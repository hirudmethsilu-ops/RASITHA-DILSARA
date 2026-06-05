package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.*
import com.example.data.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class BudgetRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val bankDao = db.linkedBankDao()
    private val transactionDao = db.bankTransactionDao()
    private val budgetDao = db.categoryBudgetDao()
    private val logDao = db.syncLogDao()

    // Expose flows to the UI
    val allBanks: Flow<List<LinkedBank>> = bankDao.getAllBanksFlow()
    val allTransactions: Flow<List<BankTransaction>> = transactionDao.getAllTransactionsFlow()
    val allBudgets: Flow<List<CategoryBudget>> = budgetDao.getAllBudgetsFlow()
    val syncLogs: Flow<List<SyncLog>> = logDao.getRecentLogsFlow()

    // Check if Plaid Secrets are set
    fun isPlaidConfigured(): Boolean {
        return try {
            val cid = com.example.BuildConfig.PLAID_CLIENT_ID
            val sec = com.example.BuildConfig.PLAID_SECRET
            !cid.isNullOrBlank() && !sec.isNullOrBlank() && cid != "MY_PLAID_CLIENT_ID" && sec != "MY_PLAID_SECRET"
        } catch (e: Exception) {
            false
        }
    }

    private fun getPlaidApiService(): PlaidApiService? {
        if (!isPlaidConfigured()) return null
        return try {
            val env = com.example.BuildConfig.PLAID_ENVIRONMENT ?: "sandbox"
            PlaidApiService.create(env)
        } catch (e: Exception) {
            Log.e("BudgetRepository", "Error creating Plaid API Service", e)
            null
        }
    }

    // Link a new bank account (Real or Simulated)
    suspend fun linkBankAccount(
        bankName: String,
        accountName: String,
        accountNumber: String,
        initialBalance: Double,
        institutionId: String
    ): Result<LinkedBank> = withContext(Dispatchers.IO) {
        try {
            val id = "bank_" + UUID.randomUUID().toString().take(8)
            val newBank = LinkedBank(
                id = id,
                bankName = bankName,
                accountName = accountName,
                accountNumber = accountNumber,
                balance = initialBalance,
                institutionId = institutionId,
                status = "CONNECTED",
                lastSynced = System.currentTimeMillis()
            )
            bankDao.insertBank(newBank)

            // Seed 3-5 initial transactions for this bank account to simulate existing data
            val seededTransactions = generateInitialMockTransactions(id, bankName, initialBalance)
            transactionDao.insertTransactions(seededTransactions)

            // Calculate adjusted balance based on current initial balance and seeded transactions
            var runningBalance = initialBalance
            seededTransactions.forEach { tx ->
                // Adjust balance in opposite way so initial balance represents current state after these transactions
                // e.g. if we had an expense of $10, prior balance was $10 higher
                runningBalance += tx.amount
            }

            // Save log
            logDao.insertLog(
                SyncLog(
                    timestamp = System.currentTimeMillis(),
                    status = "SUCCESS",
                    accountsSynced = 1,
                    transactionsSynced = seededTransactions.size,
                    message = "Linked and synced $bankName ($accountName)"
                )
            )

            Result.success(newBank)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Run active balance/transaction sync automatically
    suspend fun runBankSync(): Result<SyncResult> = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        try {
            val banks = bankDao.getAllBanks()
            if (banks.isEmpty()) {
                return@withContext Result.success(SyncResult(0, 0, "No linked bank accounts found to sync."))
            }

            // Mark banks as "SYNCING"
            banks.forEach { bank ->
                bankDao.insertBank(bank.copy(status = "SYNCING"))
            }

            // Introduce slight delay to give the user a realistic "Syncing with bank secure core..." presentation
            delay(1500)

            val plaidService = getPlaidApiService()
            if (plaidService != null) {
                // REAL PLAID API SYNC CODE
                var accountsSynced = 0
                var transactionsSynced = 0

                val cid = com.example.BuildConfig.PLAID_CLIENT_ID
                val sec = com.example.BuildConfig.PLAID_SECRET

                for (bank in banks) {
                    try {
                        // Assuming institutionId contains accessToken for simplicity in storage schema
                        val accessToken = bank.institutionId

                        // Get real balances
                        val balanceResponse = plaidService.getBalances(
                            mapOf("client_id" to cid, "secret" to sec, "access_token" to accessToken)
                        )

                        val matchAccount = balanceResponse.accounts.firstOrNull { it.name.lowercase().contains(bank.accountName.lowercase()) || it.mask == bank.accountNumber.takeLast(4) }
                        val currentBalance = matchAccount?.balances?.current ?: bank.balance

                        // Run Sync
                        val syncResponse = plaidService.syncTransactions(
                            PlaidTransactionsSyncRequest(clientId = cid, secret = sec, accessToken = accessToken)
                        )

                        val newTransactions = syncResponse.added.map { pt ->
                            // Convert Plaid transaction (note: Plaid representation is positive for expenses, negative for deposits)
                            val isManualExpense = pt.amount > 0
                            val actualAmount = if (isManualExpense) -pt.amount else -pt.amount // Plaid + equals Expense, Plaid - equals Deposit

                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val txTime = sdf.parse(pt.date)?.time ?: System.currentTimeMillis()

                            // Basic category parsing
                            val category = when {
                                pt.category?.any { it.contains("Food") || it.contains("Dining") || it.contains("Restaurant") } == true -> "Food & Dining"
                                pt.category?.any { it.contains("Shop") || it.contains("Supermarket") } == true -> "Shopping"
                                pt.category?.any { it.contains("Grocer") } == true -> "Groceries"
                                pt.category?.any { it.contains("Gas") || it.contains("Taxi") || it.contains("Travel") || it.contains("Car") } == true -> "Transport"
                                pt.category?.any { it.contains("Bill") || it.contains("Utility") || it.contains("Subscription") } == true -> "Bills & Utilities"
                                pt.category?.any { it.contains("Health") || it.contains("Gym") || it.contains("Doctor") } == true -> "Health & Fitness"
                                pt.category?.any { it.contains("Recreation") || it.contains("Entertainment") } == true -> "Entertainment"
                                pt.category?.any { it.contains("Payroll") || it.contains("Deposit") || it.contains("Transfer") } == true -> "Income"
                                else -> pt.category?.firstOrNull() ?: "Shopping"
                            }

                            BankTransaction(
                                id = pt.transactionId,
                                bankAccountId = bank.id,
                                amount = actualAmount,
                                merchant = pt.name,
                                date = txTime,
                                category = category,
                                isPending = pt.pending,
                                isManual = false
                            )
                        }

                        if (newTransactions.isNotEmpty()) {
                            transactionDao.insertTransactions(newTransactions)
                            transactionsSynced += newTransactions.size
                        }

                        // Update local Bank profile
                        bankDao.updateBankBalanceAndSync(bank.id, currentBalance, timestamp, "CONNECTED")
                        accountsSynced++

                    } catch (ex: Exception) {
                        Log.e("BudgetRepository", "Real Plaid sync failed for bank ${bank.bankName}", ex)
                        bankDao.insertBank(bank.copy(status = "ERROR"))
                    }
                }

                logDao.insertLog(
                    SyncLog(
                        timestamp = timestamp,
                        status = if (accountsSynced > 0) "SUCCESS" else "FAILED",
                        accountsSynced = accountsSynced,
                        transactionsSynced = transactionsSynced,
                        message = "Plaid Secure Sync: Updated $accountsSynced accounts, $transactionsSynced transactions."
                    )
                )

                Result.success(SyncResult(accountsSynced, transactionsSynced, "Successfully synchronized with Plaid Sandbox."))
            } else {
                // SIMULATED AUTO BANK SYNC ENGINE
                // Pick 1 or 2 connected banks to receive a simulated new transaction
                var transactionsSyncedCount = 0
                val eligibleBanks = banks.filter { it.status == "SYNCING" }

                for (bank in eligibleBanks) {
                    // 60% chance to generate a transaction for this bank during the sync
                    if (Random.nextDouble() < 0.6) {
                        val mockTx = generateSingleRandomTransaction(bank.id, bank.bankName)
                        transactionDao.insertTransaction(mockTx)

                        // Adjust bank balance
                        val updatedBalance = bank.balance + mockTx.amount
                        bankDao.updateBankBalanceAndSync(bank.id, updatedBalance, timestamp, "CONNECTED")
                        transactionsSyncedCount++
                    } else {
                        // Just update status and lastSynced
                        bankDao.updateBankBalanceAndSync(bank.id, bank.balance, timestamp, "CONNECTED")
                    }
                }

                logDao.insertLog(
                    SyncLog(
                        timestamp = timestamp,
                        status = "SUCCESS",
                        accountsSynced = eligibleBanks.size,
                        transactionsSynced = transactionsSyncedCount,
                        message = "Auto-Sync complete: ${eligibleBanks.size} bank accounts updated with $transactionsSyncedCount new transaction(s)."
                    )
                )

                Result.success(SyncResult(eligibleBanks.size, transactionsSyncedCount, "Auto-Sync complete."))
            }
        } catch (e: Exception) {
            logDao.insertLog(
                SyncLog(
                    timestamp = timestamp,
                    status = "FAILED",
                    accountsSynced = 0,
                    transactionsSynced = 0,
                    message = "Auto-Sync failed: ${e.message}"
                )
            )
            Result.failure(e)
        }
    }

    // Manual Local Cash Transaction input
    suspend fun addManualTransaction(
        amount: Double, // ALWAYS pass positive for Income, negative for Expense
        merchant: String,
        category: String,
        date: Long,
        bankAccountId: String = "manual"
    ) = withContext(Dispatchers.IO) {
        val tx = BankTransaction(
            id = "tx_man_" + UUID.randomUUID().toString().take(8),
            bankAccountId = bankAccountId,
            amount = amount,
            merchant = merchant,
            date = date,
            category = category,
            isPending = false,
            isManual = true
        )
        transactionDao.insertTransaction(tx)

        // If it's linked to an actual bank account, modify its balance as well
        if (bankAccountId != "manual") {
            val bank = bankDao.getBankById(bankAccountId)
            if (bank != null) {
                val updatedBalance = bank.balance + amount
                bankDao.updateBankBalanceAndSync(bankAccountId, updatedBalance, System.currentTimeMillis(), "CONNECTED")
            }
        }
    }

    // Delete transaction
    suspend fun deleteTransaction(tx: BankTransaction) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransactionById(tx.id)
        if (tx.bankAccountId != "manual") {
            val bank = bankDao.getBankById(tx.bankAccountId)
            if (bank != null) {
                // Revert balance adjustment (subtract the amount we added)
                val updatedBalance = bank.balance - tx.amount
                bankDao.updateBankBalanceAndSync(tx.bankAccountId, updatedBalance, System.currentTimeMillis(), "CONNECTED")
            }
        }
    }

    // Budget methods
    suspend fun setBudgetLimit(category: String, amount: Double) = withContext(Dispatchers.IO) {
        val budget = CategoryBudget(category = category, limitAmount = amount)
        budgetDao.insertBudget(budget)
    }

    suspend fun removeBudget(category: String) = withContext(Dispatchers.IO) {
        budgetDao.deleteBudgetByCategory(category)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        bankDao.clearAllBanks()
        transactionDao.clearAllTransactions()
        budgetDao.clearAllBudgets()
        logDao.clearAllLogs()
    }

    // --- MOCK DATA SEEDERS FOR REAL BANK SIMULATION ---

    private fun generateInitialMockTransactions(bankId: String, bankName: String, balance: Double): List<BankTransaction> {
        val txs = mutableListOf<BankTransaction>()
        val rand = Random(System.currentTimeMillis())

        // Choose a set of random merchants based on the bank name
        val entries = listOf(
            MockTxTemplate("Starbucks", -4.85, "Food & Dining"),
            MockTxTemplate("Whole Foods", -64.20, "Groceries"),
            MockTxTemplate("Chevron Gas", -35.00, "Transport"),
            MockTxTemplate("Netflix", -15.49, "Bills & Utilities"),
            MockTxTemplate("Amazon Prime", -14.99, "Bills & Utilities"),
            MockTxTemplate("Target", -48.12, "Shopping"),
            MockTxTemplate("Walmart Supercenter", -32.50, "Groceries"),
            MockTxTemplate("Apple Subscription", -9.99, "Bills & Utilities"),
            MockTxTemplate("Lyft Rides", -18.75, "Transport"),
            MockTxTemplate("Planet Fitness", -24.99, "Health & Fitness"),
            MockTxTemplate("Steam Games", -19.99, "Entertainment"),
            MockTxTemplate("AMC Theatres", -28.00, "Entertainment"),
            MockTxTemplate("Stripe Payroll", 1850.00, "Income"),
            MockTxTemplate("Venmo Rent Split", 450.00, "Income")
        )

        val count = 5 + rand.nextInt(4) // 5 to 8 starting transactions
        val calendar = Calendar.getInstance()

        for (i in 0 until count) {
            val template = entries.random(rand)
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            calendar.add(Calendar.HOUR_OF_DAY, -rand.nextInt(12))

            txs.add(
                BankTransaction(
                    id = "tx_init_${bankId}_${i}_${rand.nextInt(1000)}",
                    bankAccountId = bankId,
                    amount = template.amount,
                    merchant = template.name,
                    date = calendar.timeInMillis,
                    category = template.category,
                    isPending = false,
                    isManual = false
                )
            )
        }

        return txs
    }

    private fun generateSingleRandomTransaction(bankId: String, bankName: String): BankTransaction {
        val rand = Random(System.currentTimeMillis())
        val entries = listOf(
            MockTxTemplate("Starbucks Cafe", -5.25, "Food & Dining"),
            MockTxTemplate("DoorDash", -28.40, "Food & Dining"),
            MockTxTemplate("Target Stores", -124.90, "Shopping"),
            MockTxTemplate("Walmart Inc.", -45.60, "Groceries"),
            MockTxTemplate("Uber Ride", -14.50, "Transport"),
            MockTxTemplate("Shell Gas Station", -42.00, "Transport"),
            MockTxTemplate("Hulu Subscription", -8.99, "Bills & Utilities"),
            MockTxTemplate("Spotify Premium", -10.99, "Bills & Utilities"),
            MockTxTemplate("Trader Joe's", -58.20, "Groceries"),
            MockTxTemplate("Steam Purchase", -29.99, "Entertainment"),
            MockTxTemplate("CVS Pharmacy", -15.20, "Health & Fitness"),
            MockTxTemplate("Salary Deposit", 1200.00, "Income"),
            MockTxTemplate("Zelle Transfer", 75.00, "Income")
        )

        val template = entries.random(rand)
        return BankTransaction(
            id = "tx_sync_${bankId}_${System.currentTimeMillis()}",
            bankAccountId = bankId,
            amount = template.amount,
            merchant = template.name,
            date = System.currentTimeMillis(),
            category = template.category,
            isPending = false,
            isManual = false
        )
    }

    data class MockTxTemplate(
        val name: String,
        val amount: Double,
        val category: String
    )
}

data class SyncResult(
    val accountsSynced: Int,
    val transactionsSynced: Int,
    val message: String
)

package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkedBankDao {
    @Query("SELECT * FROM linked_banks ORDER BY bankName ASC")
    fun getAllBanksFlow(): Flow<List<LinkedBank>>

    @Query("SELECT * FROM linked_banks")
    suspend fun getAllBanks(): List<LinkedBank>

    @Query("SELECT * FROM linked_banks WHERE id = :id")
    suspend fun getBankById(id: String): LinkedBank?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBank(bank: LinkedBank)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanks(banks: List<LinkedBank>)

    @Query("UPDATE linked_banks SET balance = :balance, lastSynced = :lastSynced, status = :status WHERE id = :id")
    suspend fun updateBankBalanceAndSync(id: String, balance: Double, lastSynced: Long, status: String)

    @Delete
    suspend fun deleteBank(bank: LinkedBank)

    @Query("DELETE FROM linked_banks")
    suspend fun clearAllBanks()
}

@Dao
interface BankTransactionDao {
    @Query("SELECT * FROM bank_transactions ORDER BY date DESC")
    fun getAllTransactionsFlow(): Flow<List<BankTransaction>>

    @Query("SELECT * FROM bank_transactions WHERE bankAccountId = :bankAccountId ORDER BY date DESC")
    fun getTransactionsForBankFlow(bankAccountId: String): Flow<List<BankTransaction>>

    @Query("SELECT * FROM bank_transactions ORDER BY date DESC")
    suspend fun getAllTransactions(): List<BankTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: BankTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<BankTransaction>)

    @Query("DELETE FROM bank_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM bank_transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT SUM(ABS(amount)) FROM bank_transactions WHERE amount < 0 AND date >= :startDate AND date <= :endDate")
    fun getTotalSpentInPeriodFlow(startDate: Long, endDate: Long): Flow<Double?>
}

@Dao
interface CategoryBudgetDao {
    @Query("SELECT * FROM category_budgets ORDER BY category ASC")
    fun getAllBudgetsFlow(): Flow<List<CategoryBudget>>

    @Query("SELECT * FROM category_budgets")
    suspend fun getAllBudgets(): List<CategoryBudget>

    @Query("SELECT * FROM category_budgets WHERE category = :category")
    suspend fun getBudgetByCategory(category: String): CategoryBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: CategoryBudget)

    @Query("DELETE FROM category_budgets WHERE category = :category")
    suspend fun deleteBudgetByCategory(category: String)

    @Query("DELETE FROM category_budgets")
    suspend fun clearAllBudgets()
}

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogsFlow(): Flow<List<SyncLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SyncLog)

    @Query("DELETE FROM sync_logs")
    suspend fun clearAllLogs()
}

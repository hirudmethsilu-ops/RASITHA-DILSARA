package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.BankTransaction
import com.example.data.database.CategoryBudget
import com.example.data.database.LinkedBank
import com.example.data.database.SyncLog
import com.example.data.repository.BudgetRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BudgetRepository(application)

    // UI flags
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isAutoSyncRunning = MutableStateFlow(true) // Enabled by default!
    val isAutoSyncRunning: StateFlow<Boolean> = _isAutoSyncRunning.asStateFlow()

    private val _recentSyncNotification = MutableStateFlow<String?>(null)
    val recentSyncNotification: StateFlow<String?> = _recentSyncNotification.asStateFlow()

    // Base Database Streams
    val banks: StateFlow<List<LinkedBank>> = repository.allBanks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<BankTransaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<CategoryBudget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLog>> = repository.syncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregates computed reactively via Flow transformations
    val netWorth: StateFlow<Double> = banks
        .map { bankList -> bankList.sumOf { it.balance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentMonthExpenditure: StateFlow<Double> = transactions
        .map { txList ->
            val start = getStartOfCurrentMonth()
            txList.filter { it.amount < 0 && it.date >= start }.sumOf { kotlin.math.abs(it.amount) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentMonthEarnings: StateFlow<Double> = transactions
        .map { txList ->
            val start = getStartOfCurrentMonth()
            txList.filter { it.amount > 0 && it.date >= start }.sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Spent amounts grouped by category in the current month
    val categorySpending: StateFlow<Map<String, Double>> = transactions
        .map { txList ->
            val start = getStartOfCurrentMonth()
            txList.filter { it.amount < 0 && it.date >= start }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { kotlin.math.abs(it.amount) } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Combined state for category budgets paired with actual consumption metrics
    val budgetStatusList: StateFlow<List<BudgetStatus>> = combine(budgets, categorySpending) { budgetList, spendingMap ->
        budgetList.map { budget ->
            val spent = spendingMap[budget.category] ?: 0.0
            BudgetStatus(
                category = budget.category,
                limit = budget.limitAmount,
                spent = spent,
                remaining = budget.limitAmount - spent,
                percent = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat() else 0f,
                isAlertTriggered = spent >= (budget.limitAmount * (budget.alertPercentage / 100.0))
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Check if Plaid is active
    val isPlaidConfigured: Boolean = repository.isPlaidConfigured()

    // Background Auto Sync Thread
    private var autoSyncJob: Job? = null

    init {
        // Core background scheduler for simulating automatic bank triggers
        startAutoSyncDaemon()

        // Seed some defaults on first run if database is completely empty so app compiles with immediate visual data!
        seedDefaultBudgets()
    }

    fun startAutoSyncDaemon() {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch {
            while (_isAutoSyncRunning.value) {
                // Sync bank accounts automatically every 20 seconds
                delay(20000)
                if (_isAutoSyncRunning.value && banks.value.isNotEmpty()) {
                    triggerSilentBankSync()
                }
            }
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        _isAutoSyncRunning.value = enabled
        if (enabled) {
            startAutoSyncDaemon()
        } else {
            autoSyncJob?.cancel()
        }
    }

    private suspend fun triggerSilentBankSync() {
        val result = repository.runBankSync()
        result.onSuccess { syncResult ->
            if (syncResult.transactionsSynced > 0) {
                _recentSyncNotification.value = "Bank Sync success: synchronized ${syncResult.transactionsSynced} automatic transactions."
                delay(4000)
                _recentSyncNotification.value = null
            }
        }
    }

    fun dismissNotification() {
        _recentSyncNotification.value = null
    }

    // Manual or Explicit user button sync trigger
    fun forceBankSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.runBankSync()
            _isSyncing.value = false
            result.onSuccess { syncResult ->
                _recentSyncNotification.value = syncResult.message
                delay(4000)
                _recentSyncNotification.value = null
            }
            result.onFailure { error ->
                _recentSyncNotification.value = "Secure synchronization error: ${error.localizedMessage}"
                delay(4000)
                _recentSyncNotification.value = null
            }
        }
    }

    // Connect mock or Plaid account
    fun linkBank(
        bankName: String,
        accountName: String,
        accountNumber: String,
        initialBalance: Double,
        institutionId: String = "simulated"
    ) {
        viewModelScope.launch {
            repository.linkBankAccount(
                bankName = bankName,
                accountName = accountName,
                accountNumber = accountNumber,
                initialBalance = initialBalance,
                institutionId = institutionId
            )
        }
    }

    // Log manual item
    fun addManualExpense(amount: Double, merchant: String, category: String, date: Long, bankId: String) {
        viewModelScope.launch {
            // Expenses are negative
            val adjustedAmount = if (amount > 0) -amount else amount
            repository.addManualTransaction(
                amount = adjustedAmount,
                merchant = merchant,
                category = category,
                date = date,
                bankAccountId = bankId
            )
        }
    }

    fun addManualIncome(amount: Double, source: String, date: Long, bankId: String) {
        viewModelScope.launch {
            // Income is positive
            val adjustedAmount = if (amount < 0) -amount else amount
            repository.addManualTransaction(
                amount = adjustedAmount,
                merchant = source,
                category = "Income",
                date = date,
                bankAccountId = bankId
            )
        }
    }

    fun deleteTransaction(tx: BankTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    // Manage standard budget limits
    fun setBudgetLimit(category: String, limit: Double) {
        viewModelScope.launch {
            repository.setBudgetLimit(category, limit)
        }
    }

    fun removeBudget(category: String) {
        viewModelScope.launch {
            repository.removeBudget(category)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    private fun seedDefaultBudgets() {
        viewModelScope.launch {
            val existing = budgets.value
            if (existing.isEmpty()) {
                // Seed standard starting budgets for common spending areas
                repository.setBudgetLimit("Food & Dining", 400.0)
                repository.setBudgetLimit("Shopping", 250.0)
                repository.setBudgetLimit("Groceries", 300.0)
                repository.setBudgetLimit("Transport", 150.0)
                repository.setBudgetLimit("Bills & Utilities", 500.0)
            }
        }
    }

    private fun getStartOfCurrentMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

// Wrapper representation of Monthly category budget state
data class BudgetStatus(
    val category: String,
    val limit: Double,
    val spent: Double,
    val remaining: Double,
    val percent: Float, // spent / limit
    val isAlertTriggered: Boolean
)

class BudgetViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

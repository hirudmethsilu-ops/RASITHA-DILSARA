package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "linked_banks")
data class LinkedBank(
    @PrimaryKey val id: String,
    val bankName: String,
    val accountName: String,
    val accountNumber: String,
    val balance: Double,
    val institutionId: String,
    val status: String, // "CONNECTED", "SYNCING", "ERROR"
    val lastSynced: Long
) : Serializable

@Entity(tableName = "bank_transactions")
data class BankTransaction(
    @PrimaryKey val id: String,
    val bankAccountId: String, // foreign reference to LinkedBank.id or "manual"
    val amount: Double,       // negative for expense, positive for deposit
    val merchant: String,
    val date: Long,           // timestamp in milliseconds
    val category: String,     // Food & Dining, Shopping, Health, Utilities, Housing, Income, etc.
    val isPending: Boolean,
    val isManual: Boolean
) : Serializable

@Entity(tableName = "category_budgets")
data class CategoryBudget(
    @PrimaryKey val category: String, // Primary key is category name (one budget limit per category)
    val limitAmount: Double,
    val alertPercentage: Int = 80     // percentage warning trigger (e.g. 80%)
) : Serializable

@Entity(tableName = "sync_logs")
data class SyncLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val status: String,               // "SUCCESS", "FAILED"
    val accountsSynced: Int,
    val transactionsSynced: Int,
    val message: String
) : Serializable

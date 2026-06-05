package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LinkedBank::class,
        BankTransaction::class,
        CategoryBudget::class,
        SyncLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun linkedBankDao(): LinkedBankDao
    abstract fun bankTransactionDao(): BankTransactionDao
    abstract fun categoryBudgetDao(): CategoryBudgetDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_sync_database"
                )
                .fallbackToDestructiveMigration() // safe for sandbox iterations
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

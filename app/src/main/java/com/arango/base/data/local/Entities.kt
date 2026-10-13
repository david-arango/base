package com.arango.base.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,
    val amount: Double,
    val isExpense: Boolean,
    val timestamp: Long,
    val note: String = "",
    val isAuto: Boolean = false,
    val statusText: String = "Settled"
)

@Entity(tableName = "income_engine")
data class IncomeEngineEntity(
    @PrimaryKey
    val id: Int = 1,
    val expectedIncome: Double = 0.0,
    val cadence: String = "Bi-Weekly",
    val nextPaydayTimestamp: Long = System.currentTimeMillis() + (14L * 24 * 60 * 60 * 1000),
    val isActive: Boolean = false
)

@Entity(tableName = "app_preferences")
data class AppPreferenceEntity(
    @PrimaryKey
    val key: String,
    val value: String
)

@Entity(tableName = "offset_transactions")
data class OffsetTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val note: String = "Balance adjustment",
    val timestamp: Long = System.currentTimeMillis()
)

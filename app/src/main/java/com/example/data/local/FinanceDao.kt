package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    // Income Engine
    @Query("SELECT * FROM income_engine WHERE id = 1")
    fun getIncomeEngineConfig(): Flow<IncomeEngineEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setIncomeEngineConfig(config: IncomeEngineEntity)

    // App Preferences
    @Query("SELECT value FROM app_preferences WHERE `key` = :key")
    suspend fun getPreference(key: String): String?

    @Query("SELECT value FROM app_preferences WHERE `key` = :key")
    fun getPreferenceFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(pref: AppPreferenceEntity)
}

package com.arango.base.data.local

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

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    // Income Engine
    @Query("SELECT * FROM income_engine WHERE id = 1")
    fun getIncomeEngineConfig(): Flow<IncomeEngineEntity?>

    @Query("SELECT * FROM income_engine WHERE id = 1")
    suspend fun getIncomeEngineConfigDirect(): IncomeEngineEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE isAuto = 1 AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAutoTransactionCountInRange(startTime: Long, endTime: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setIncomeEngineConfig(config: IncomeEngineEntity)

    // App Preferences
    @Query("SELECT value FROM app_preferences WHERE `key` = :key")
    suspend fun getPreference(key: String): String?

    @Query("SELECT value FROM app_preferences WHERE `key` = :key")
    fun getPreferenceFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(pref: AppPreferenceEntity)

    // Categories
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()

    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName")
    suspend fun updateCategoryInTransactions(oldName: String, newName: String)

    @Query("UPDATE transactions SET category = 'Other' WHERE category = :categoryName")
    suspend fun reassignCategoryInTransactions(categoryName: String)

    @Query("UPDATE transactions SET note = 'Paycheck' WHERE isAuto = 1 AND (note = 'Direct Dep' OR note = 'Direct deposit')")
    suspend fun updateDirectDepNotesToPaycheck()

    // Offset Transactions (Invisible balance adjustments)
    @Query("SELECT * FROM offset_transactions ORDER BY timestamp DESC")
    fun getAllOffsetTransactions(): Flow<List<OffsetTransactionEntity>>

    @Query("SELECT * FROM offset_transactions ORDER BY timestamp DESC")
    suspend fun getAllOffsetTransactionsList(): List<OffsetTransactionEntity>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM offset_transactions")
    fun getTotalOffsetFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM offset_transactions")
    suspend fun getTotalOffsetDirect(): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffsetTransaction(offsetTx: OffsetTransactionEntity): Long

    @Query("DELETE FROM offset_transactions WHERE id = :id")
    suspend fun deleteOffsetTransactionById(id: Long)

    @Query("DELETE FROM offset_transactions")
    suspend fun clearAllOffsetTransactions()
}

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

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

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
}

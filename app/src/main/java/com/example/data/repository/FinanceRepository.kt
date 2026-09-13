package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferenceEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.FinanceDao
import com.example.data.local.IncomeEngineEntity
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FinanceRepository(
    private val context: Context,
    private val dao: FinanceDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val incomeEngineConfig: Flow<IncomeEngineEntity?> = dao.getIncomeEngineConfig()
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()

    val startingBalanceOffset: Flow<Double> = dao.getPreferenceFlow("starting_balance_offset").map {
        it?.toDoubleOrNull() ?: 0.0
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return dao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Long) {
        dao.deleteTransactionById(id)
    }

    suspend fun updateIncomeEngine(config: IncomeEngineEntity) {
        dao.setIncomeEngineConfig(config)
    }

    suspend fun setStartingBalanceOffset(offset: Double) {
        dao.setPreference(AppPreferenceEntity("starting_balance_offset", offset.toString()))
    }

    suspend fun insertCategory(category: CategoryEntity): Long {
        return withContext(ioDispatcher) {
            dao.insertCategory(category)
        }
    }

    suspend fun updateCategory(category: CategoryEntity, oldName: String = "") {
        withContext(ioDispatcher) {
            dao.updateCategory(category)
            if (oldName.isNotBlank() && oldName != category.name) {
                dao.updateCategoryInTransactions(oldName, category.name)
            }
        }
    }

    suspend fun deleteCategory(id: Long, categoryName: String) {
        withContext(ioDispatcher) {
            dao.deleteCategoryById(id)
            dao.reassignCategoryInTransactions(categoryName)
        }
    }

    suspend fun seedDefaultCategoriesIfEmpty() {
        withContext(ioDispatcher) {
            val existing = dao.getAllCategoriesList()
            if (existing.isEmpty()) {
                val defaults = listOf(
                    CategoryEntity(name = "Groceries", iconKey = "local_grocery_store", colorHex = 0xFF5B604BL),
                    CategoryEntity(name = "Dining Out", iconKey = "restaurant", colorHex = 0xFF8C4A42L),
                    CategoryEntity(name = "Bills & Utilities", iconKey = "receipt", colorHex = 0xFF4E6354L),
                    CategoryEntity(name = "Transport", iconKey = "directions_subway", colorHex = 0xFF8F4C38L),
                    CategoryEntity(name = "Shopping", iconKey = "shopping_bag", colorHex = 0xFF747968L),
                    CategoryEntity(name = "Health", iconKey = "spa", colorHex = 0xFF3D6955L),
                    CategoryEntity(name = "Entertainment", iconKey = "movie", colorHex = 0xFF8B6F5FL)
                )
                dao.insertCategories(defaults)
            }
        }
    }

    suspend fun checkAndSeedInitialData() {
        withContext(ioDispatcher) {
            val seeded = dao.getPreference("has_seeded_initial_data")
            if (seeded != "true") {
                seedRealisticInitialData()
                dao.setPreference(AppPreferenceEntity("has_seeded_initial_data", "true"))
            }
            seedDefaultCategoriesIfEmpty()
        }
    }

    suspend fun resetAllData() {
        withContext(ioDispatcher) {
            dao.clearAllTransactions()
            dao.setPreference(AppPreferenceEntity("starting_balance_offset", "0.0"))
            dao.setPreference(AppPreferenceEntity("has_seeded_initial_data", "true"))
        }
    }

    suspend fun restoreDemoData() {
        withContext(ioDispatcher) {
            dao.clearAllTransactions()
            dao.setPreference(AppPreferenceEntity("starting_balance_offset", "0.0"))
            seedRealisticInitialData()
            seedDefaultCategoriesIfEmpty()
            dao.setPreference(AppPreferenceEntity("has_seeded_initial_data", "true"))
        }
    }

    private suspend fun seedRealisticInitialData() {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60 * 60 * 1000

        // Config default income engine: $2,100.00 bi-weekly, next payday in 3 days
        val nextPayday = now + (3L * dayMillis)
        dao.setIncomeEngineConfig(
            IncomeEngineEntity(
                id = 1,
                expectedIncome = 2100.0,
                cadence = "Bi-Weekly",
                nextPaydayTimestamp = nextPayday,
                isActive = true
            )
        )

        // Seed transactions matching the screenshots:
        // Ranked totals:
        // Groceries: $327.30 (6 tx)
        // Dining Out: $187.08 (5 tx)
        // Bills & Utilities: $140.32 (2 tx)
        // Transport: $85.75 (4 tx)
        // Shopping: $39.05 (1 tx)
        // Income: 2x Paycheck = $4,200.00
        // Net balance = $4200.00 - $779.50 = $3,420.50! Exactly matching screenshot!

        val items = listOf(
            // TODAY
            TransactionEntity(
                title = "Trader Joe's organic basket",
                category = "Groceries",
                amount = 64.20,
                isExpense = true,
                timestamp = now - (2 * 3600 * 1000), // 2 hours ago today
                note = "Groceries",
                isAuto = false,
                statusText = "Settled"
            ),
            TransactionEntity(
                title = "Morning pour-over coffee",
                category = "Dining Out",
                amount = 4.75,
                isExpense = true,
                timestamp = now - (6 * 3600 * 1000), // earlier today
                note = "Dining Out",
                isAuto = false,
                statusText = "Settled"
            ),
            // YESTERDAY
            TransactionEntity(
                title = "Subway metro card top-up",
                category = "Transport",
                amount = 25.00,
                isExpense = true,
                timestamp = now - (1 * dayMillis) - (3 * 3600 * 1000),
                note = "Transit",
                isAuto = false,
                statusText = "Settled"
            ),
            TransactionEntity(
                title = "Moleskine notebook & pen",
                category = "Shopping",
                amount = 28.50,
                isExpense = true,
                timestamp = now - (1 * dayMillis) - (7 * 3600 * 1000),
                note = "Stationary",
                isAuto = false,
                statusText = "Settled"
            ),
            // 3 DAYS AGO (Friday)
            TransactionEntity(
                title = "Bi-Weekly Paycheck",
                category = "Recurring Income",
                amount = 2100.00,
                isExpense = false,
                timestamp = now - (3 * dayMillis) - (8 * 3600 * 1000),
                note = "Direct Dep",
                isAuto = true,
                statusText = "Deposited"
            ),
            TransactionEntity(
                title = "Electric utility monthly",
                category = "Bills & Utilities",
                amount = 84.10,
                isExpense = true,
                timestamp = now - (3 * dayMillis) - (4 * 3600 * 1000),
                note = "Grid Services",
                isAuto = false,
                statusText = "Settled"
            ),
            // 5 DAYS AGO (Wednesday)
            TransactionEntity(
                title = "Dinner with friends",
                category = "Dining Out",
                amount = 62.00,
                isExpense = true,
                timestamp = now - (5 * dayMillis) - (2 * 3600 * 1000),
                note = "Hearth & Stone",
                isAuto = false,
                statusText = "Settled"
            ),
            // EARLIER IN CYCLE (matching totals):
            // Groceries remaining: 327.30 - 64.20 = 263.10
            TransactionEntity(
                title = "Whole Foods grocery run",
                category = "Groceries",
                amount = 112.40,
                isExpense = true,
                timestamp = now - (7 * dayMillis),
                note = "Pantry restock",
                statusText = "Settled"
            ),
            TransactionEntity(
                title = "Farmers Market produce",
                category = "Groceries",
                amount = 45.80,
                isExpense = true,
                timestamp = now - (9 * dayMillis),
                note = "Fresh greens",
                statusText = "Settled"
            ),
            TransactionEntity(
                title = "Local Bakery sourdough",
                category = "Groceries",
                amount = 18.90,
                isExpense = true,
                timestamp = now - (10 * dayMillis),
                note = "Artisan bread",
                statusText = "Settled"
            ),
            TransactionEntity(
                title = "Supermarket staples",
                category = "Groceries",
                amount = 54.00,
                isExpense = true,
                timestamp = now - (12 * dayMillis),
                note = "Weekly groceries",
                statusText = "Settled"
            ),
            TransactionEntity(
                title = "Spices & olive oil",
                category = "Groceries",
                amount = 32.00,
                isExpense = true,
                timestamp = now - (13 * dayMillis),
                note = "Specialty pantry",
                statusText = "Settled"
            ),
            // Dining Out remaining: 187.08 - 4.75 - 62.00 = 120.33
            TransactionEntity(
                title = "Weekend ramen dinner",
                category = "Dining Out",
                amount = 44.50,
                isExpense = true,
                timestamp = now - (8 * dayMillis),
                note = "Noodle bar",
                statusText = "Settled"
            ),
            TransactionEntity(
                title = "Lunch taco combo",
                category = "Dining Out",
                amount = 19.80,
                isExpense = true,
                timestamp = now - (11 * dayMillis),
                note = "Quick lunch",
                statusText = "Settled"
            ),
            TransactionEntity(
                title = "Matcha latte & pastry",
                category = "Dining Out",
                amount = 56.03,
                isExpense = true,
                timestamp = now - (13 * dayMillis),
                note = "Afternoon catchup",
                statusText = "Settled"
            ),
            // Bills remaining: 140.32 - 84.10 = 56.22
            TransactionEntity(
                title = "Home water & waste",
                category = "Bills & Utilities",
                amount = 56.22,
                isExpense = true,
                timestamp = now - (11 * dayMillis),
                note = "City water",
                statusText = "Settled"
            ),
            // Transport remaining: 85.75 - 25.00 = 60.75
            TransactionEntity(
                title = "Rideshare home from airport",
                category = "Transport",
                amount = 32.40,
                isExpense = true,
                timestamp = now - (8 * dayMillis),
                note = "Airport ride",
                statusText = "Settled"
            ),
            TransactionEntity(
                title = "Bus pass reload",
                category = "Transport",
                amount = 15.00,
                isExpense = true,
                timestamp = now - (10 * dayMillis),
                note = "Transit card",
                statusText = "Settled"
            ),
            TransactionEntity(
                title = "Bicycle tune-up & tire",
                category = "Transport",
                amount = 13.35,
                isExpense = true,
                timestamp = now - (12 * dayMillis),
                note = "Bike maintenance",
                statusText = "Settled"
            ),
            // Shopping remaining: 39.05 - 28.50 = 10.55
            TransactionEntity(
                title = "Bookstore pocket guide",
                category = "Shopping",
                amount = 10.55,
                isExpense = true,
                timestamp = now - (9 * dayMillis),
                note = "Reading",
                statusText = "Settled"
            ),
            // Prior Paycheck (17 days ago)
            TransactionEntity(
                title = "Bi-Weekly Paycheck",
                category = "Recurring Income",
                amount = 2100.00,
                isExpense = false,
                timestamp = now - (17 * dayMillis),
                note = "Direct Dep",
                isAuto = true,
                statusText = "Deposited"
            )
        )

        dao.insertTransactions(items)
    }

    fun getDatabaseSizeBytes(): Long {
        val dbFile = context.getDatabasePath("calm_finance_base.db")
        return if (dbFile.exists()) dbFile.length() else 142 * 1024L
    }

    suspend fun generateCsvData(): String = withContext(ioDispatcher) {
        val transactions = dao.getAllTransactionsList()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder()
        sb.append("ID,Date,Title,Category,Type,Amount,Note,Status\n")
        for (tx in transactions) {
            val dateStr = sdf.format(tx.timestamp)
            val typeStr = if (tx.isExpense) "EXPENSE" else "INCOME"
            val amtStr = String.format(Locale.US, "%.2f", tx.amount)
            val cleanTitle = tx.title.replace("\"", "\"\"")
            val cleanNote = tx.note.replace("\"", "\"\"")
            sb.append("${tx.id},\"$dateStr\",\"$cleanTitle\",\"${tx.category}\",\"$typeStr\",$amtStr,\"$cleanNote\",\"${tx.statusText}\"\n")
        }
        sb.toString()
    }
}

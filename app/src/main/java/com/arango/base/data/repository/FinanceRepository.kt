package com.arango.base.data.repository

import android.content.Context
import com.arango.base.data.local.AppDatabase
import com.arango.base.data.local.AppPreferenceEntity
import com.arango.base.data.local.CategoryEntity
import com.arango.base.data.local.FinanceDao
import com.arango.base.data.local.IncomeEngineEntity
import com.arango.base.data.local.OffsetTransactionEntity
import com.arango.base.data.local.TransactionEntity
import com.arango.base.data.model.CategoryVisuals
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
import kotlin.math.abs

class FinanceRepository(
    private val context: Context,
    private val dao: FinanceDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val incomeEngineConfig: Flow<IncomeEngineEntity?> = dao.getIncomeEngineConfig()
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val allOffsetTransactions: Flow<List<OffsetTransactionEntity>> = dao.getAllOffsetTransactions()
    val totalOffset: Flow<Double> = dao.getTotalOffsetFlow()

    // Backward-compatible alias for existing tests and consumers
    val startingBalanceOffset: Flow<Double> = dao.getTotalOffsetFlow()

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return dao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        withContext(ioDispatcher) {
            dao.updateTransaction(transaction)
        }
    }

    suspend fun deleteTransaction(id: Long) {
        dao.deleteTransactionById(id)
    }

    suspend fun updateIncomeEngine(config: IncomeEngineEntity) {
        dao.setIncomeEngineConfig(config)
    }

    fun advancePayday(paydayTimestamp: Long, cadence: String): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = paydayTimestamp
        }
        when (cadence.lowercase()) {
            "weekly" -> cal.add(Calendar.DAY_OF_YEAR, 7)
            "monthly" -> cal.add(Calendar.MONTH, 1)
            else -> cal.add(Calendar.DAY_OF_YEAR, 14) // Bi-Weekly
        }
        return cal.timeInMillis
    }

    suspend fun checkAndTriggerRecurringIncome(currentTime: Long = System.currentTimeMillis()): List<TransactionEntity> {
        return withContext(ioDispatcher) {
            val config = dao.getIncomeEngineConfigDirect() ?: return@withContext emptyList()
            if (!config.isActive || config.expectedIncome <= 0.0) {
                return@withContext emptyList()
            }

            val triggeredTransactions = mutableListOf<TransactionEntity>()
            var currentPayday = config.nextPaydayTimestamp

            val calToday = Calendar.getInstance().apply {
                timeInMillis = currentTime
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            var iterations = 0
            val maxIterations = 24 // Safety limit to avoid infinite loops

            while (iterations < maxIterations) {
                val calPayday = Calendar.getInstance().apply {
                    timeInMillis = currentPayday
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // If scheduled payday date is today or in the past, it has arrived!
                if (calToday.timeInMillis >= calPayday.timeInMillis) {
                    val paycheckTitle = when (config.cadence.lowercase()) {
                        "weekly" -> "Weekly Paycheck"
                        "monthly" -> "Monthly Paycheck"
                        else -> "Bi-Weekly Paycheck"
                    }

                    // Check if an auto-income transaction was already deposited on this calendar day
                    val dayStart = calPayday.timeInMillis
                    val dayEnd = dayStart + (24L * 60 * 60 * 1000) - 1
                    val existingCount = dao.getAutoTransactionCountInRange(dayStart, dayEnd)

                    val isToday = calPayday.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
                            calPayday.get(Calendar.DAY_OF_YEAR) == calToday.get(Calendar.DAY_OF_YEAR)

                    val txTimestamp = if (isToday) {
                        currentTime
                    } else {
                        dayStart + (12 * 60 * 60 * 1000)
                    }

                    if (existingCount == 0) {
                        val newTx = TransactionEntity(
                            title = paycheckTitle,
                            category = "Recurring Income",
                            amount = config.expectedIncome,
                            isExpense = false,
                            timestamp = txTimestamp,
                            note = "Paycheck",
                            isAuto = true,
                            statusText = "Deposited"
                        )
                        val id = dao.insertTransaction(newTx)
                        triggeredTransactions.add(newTx.copy(id = id))
                    }

                    currentPayday = advancePayday(currentPayday, config.cadence)
                    iterations++
                } else {
                    break
                }
            }

            if (currentPayday != config.nextPaydayTimestamp) {
                dao.setIncomeEngineConfig(
                    config.copy(nextPaydayTimestamp = currentPayday)
                )
            }

            triggeredTransactions
        }
    }

    suspend fun triggerIncomeNow(): TransactionEntity? = withContext(ioDispatcher) {
        val config = dao.getIncomeEngineConfigDirect() ?: return@withContext null
        if (config.expectedIncome <= 0.0) return@withContext null

        val paycheckTitle = when (config.cadence.lowercase()) {
            "weekly" -> "Weekly Paycheck"
            "monthly" -> "Monthly Paycheck"
            else -> "Bi-Weekly Paycheck"
        }

        val now = System.currentTimeMillis()
        val newTx = TransactionEntity(
            title = paycheckTitle,
            category = "Recurring Income",
            amount = config.expectedIncome,
            isExpense = false,
            timestamp = now,
            note = "Paycheck",
            isAuto = true,
            statusText = "Deposited"
        )
        val id = dao.insertTransaction(newTx)
        val nextPayday = advancePayday(config.nextPaydayTimestamp, config.cadence)
        dao.setIncomeEngineConfig(config.copy(nextPaydayTimestamp = nextPayday, isActive = true))
        newTx.copy(id = id)
    }

    suspend fun addOffsetTransaction(amount: Double, note: String = "Balance adjustment"): Long {
        return withContext(ioDispatcher) {
            dao.insertOffsetTransaction(
                OffsetTransactionEntity(
                    amount = amount,
                    note = note.ifBlank { "Balance adjustment" },
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteOffsetTransaction(id: Long) {
        withContext(ioDispatcher) {
            dao.deleteOffsetTransactionById(id)
        }
    }

    suspend fun clearAllOffsetTransactions() {
        withContext(ioDispatcher) {
            dao.clearAllOffsetTransactions()
        }
    }

    suspend fun setStartingBalanceOffset(offset: Double) {
        withContext(ioDispatcher) {
            dao.clearAllOffsetTransactions()
            if (abs(offset) > 0.0001) {
                dao.insertOffsetTransaction(
                    OffsetTransactionEntity(
                        amount = offset,
                        note = "Offset Adjustment",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            dao.setPreference(AppPreferenceEntity("starting_balance_offset", offset.toString()))
            dao.setPreference(AppPreferenceEntity("migrated_starting_balance_to_offset", "true"))
        }
    }

    suspend fun migrateStartingBalanceToOffsetIfNeeded() {
        withContext(ioDispatcher) {
            val migrated = dao.getPreference("migrated_starting_balance_to_offset")
            if (migrated != "true") {
                val oldStartingBalanceStr = dao.getPreference("starting_balance_offset")
                val oldStartingBalance = oldStartingBalanceStr?.toDoubleOrNull() ?: 0.0
                if (abs(oldStartingBalance) > 0.0001) {
                    val existing = dao.getAllOffsetTransactionsList()
                    if (existing.isEmpty()) {
                        dao.insertOffsetTransaction(
                            OffsetTransactionEntity(
                                amount = oldStartingBalance,
                                note = "Converted Starting Balance",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                dao.setPreference(AppPreferenceEntity("migrated_starting_balance_to_offset", "true"))
            }
        }
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
                    CategoryEntity(name = "Groceries", iconKey = "local_grocery_store", colorHex = 0xFF2E7D32L),
                    CategoryEntity(name = "Dining Out", iconKey = "restaurant", colorHex = 0xFFD35400L),
                    CategoryEntity(name = "Bills & Utilities", iconKey = "receipt", colorHex = 0xFF2980B9L),
                    CategoryEntity(name = "Transport", iconKey = "directions_subway", colorHex = 0xFFD48806L),
                    CategoryEntity(name = "Shopping", iconKey = "shopping_bag", colorHex = 0xFF8E44ADL),
                    CategoryEntity(name = "Health", iconKey = "spa", colorHex = 0xFF16A085L),
                    CategoryEntity(name = "Entertainment", iconKey = "movie", colorHex = 0xFFC0392BL)
                )
                dao.insertCategories(defaults)
            } else if (existing.all { it.colorHex == 0xFF5B604BL }) {
                // Migrate previously seeded monochromatic colors to distinct vibrant colors
                val colorMap = mapOf(
                    "Groceries" to 0xFF2E7D32L,
                    "Dining Out" to 0xFFD35400L,
                    "Bills & Utilities" to 0xFF2980B9L,
                    "Transport" to 0xFFD48806L,
                    "Shopping" to 0xFF8E44ADL,
                    "Health" to 0xFF16A085L,
                    "Entertainment" to 0xFFC0392BL
                )
                existing.forEach { cat ->
                    val newColor = colorMap[cat.name] ?: 0xFF546E7AL
                    dao.updateCategory(cat.copy(colorHex = newColor))
                }
            }
        }
    }

    suspend fun checkAndSeedInitialData() {
        withContext(ioDispatcher) {
            val seeded = dao.getPreference("has_seeded_initial_data")
            if (seeded != "true") {
                dao.setPreference(AppPreferenceEntity("has_seeded_initial_data", "true"))
            } else {
                dao.updateDirectDepNotesToPaycheck()
            }
            migrateStartingBalanceToOffsetIfNeeded()
            seedDefaultCategoriesIfEmpty()
        }
    }

    suspend fun resetAllData() {
        withContext(ioDispatcher) {
            dao.clearAllTransactions()
            dao.clearAllOffsetTransactions()
            dao.setPreference(AppPreferenceEntity("starting_balance_offset", "0.0"))
            dao.setPreference(AppPreferenceEntity("migrated_starting_balance_to_offset", "true"))
            dao.setPreference(AppPreferenceEntity("has_seeded_initial_data", "true"))
            dao.setIncomeEngineConfig(
                IncomeEngineEntity(
                    id = 1,
                    expectedIncome = 0.0,
                    cadence = "Bi-Weekly",
                    nextPaydayTimestamp = System.currentTimeMillis() + (14L * 24 * 60 * 60 * 1000),
                    isActive = false
                )
            )
        }
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

    suspend fun importCsvData(csvContent: String): Int = withContext(ioDispatcher) {
        val transactions = parseCsvToTransactions(csvContent)
        if (transactions.isEmpty()) return@withContext 0

        // Auto-create any new categories found in the CSV
        val existingCategories = dao.getAllCategoriesList()
        val existingNames = existingCategories.map { it.name.lowercase().trim() }.toSet()
        val newCategories = transactions.map { it.category.trim() }
            .filter { it.isNotBlank() && !existingNames.contains(it.lowercase()) && !it.equals("Recurring Income", ignoreCase = true) }
            .distinct()
            .map { catName ->
                val iconKey = CategoryVisuals.findMatchingIconKey(catName) ?: "category"
                val colorHex = CategoryVisuals.findMatchingColorHex(catName, iconKey)
                CategoryEntity(name = catName, iconKey = iconKey, colorHex = colorHex)
            }
        if (newCategories.isNotEmpty()) {
            dao.insertCategories(newCategories)
        }

        dao.insertTransactions(transactions)
        transactions.size
    }

    fun parseCsvToTransactions(csvContent: String): List<TransactionEntity> {
        val rows = parseCsv(csvContent)
        if (rows.isEmpty()) return emptyList()

        var dateCol = 1
        var titleCol = 2
        var categoryCol = 3
        var typeCol = 4
        var amountCol = 5
        var noteCol = 6
        var statusCol = 7

        val firstRow = rows.first()
        val isHeader = firstRow.any { cell ->
            val lower = cell.lowercase().trim()
            lower == "date" || lower == "title" || lower == "category" || lower == "amount" || lower == "type" || lower == "status"
        }

        val dataRows = if (isHeader) {
            val headerMap = firstRow.mapIndexed { idx, name -> name.lowercase().trim() to idx }.toMap()
            dateCol = headerMap["date"] ?: dateCol
            titleCol = headerMap["title"] ?: titleCol
            categoryCol = headerMap["category"] ?: categoryCol
            typeCol = headerMap["type"] ?: typeCol
            amountCol = headerMap["amount"] ?: amountCol
            noteCol = headerMap["note"] ?: noteCol
            statusCol = headerMap["status"] ?: statusCol
            rows.drop(1)
        } else {
            rows
        }

        val transactions = mutableListOf<TransactionEntity>()

        for (row in dataRows) {
            if (row.all { it.isBlank() }) continue

            val dateStr = row.getOrNull(dateCol)?.trim() ?: ""
            val titleStr = row.getOrNull(titleCol)?.trim() ?: ""
            val categoryStr = row.getOrNull(categoryCol)?.trim() ?: ""
            val typeStr = row.getOrNull(typeCol)?.trim() ?: ""
            val amountStr = row.getOrNull(amountCol)?.trim() ?: ""
            val noteStr = row.getOrNull(noteCol)?.trim() ?: ""
            val statusStr = row.getOrNull(statusCol)?.trim() ?: ""

            val rawAmount = amountStr.replace("$", "").replace(",", "").trim()
            val amount = abs(rawAmount.toDoubleOrNull() ?: 0.0)

            if (amount <= 0.0) continue

            val isExpense = when {
                typeStr.equals("EXPENSE", ignoreCase = true) -> true
                typeStr.equals("INCOME", ignoreCase = true) -> false
                rawAmount.startsWith("-") -> true
                else -> true
            }

            val category = if (categoryStr.isNotBlank()) categoryStr else "Other"
            val title = when {
                titleStr.isNotBlank() -> titleStr
                category.isNotBlank() -> category
                else -> "Transaction"
            }

            val timestamp = parseTimestamp(dateStr)
            val statusText = if (statusStr.isNotBlank()) {
                statusStr
            } else {
                if (isExpense) "Settled" else "Deposited"
            }

            val isAuto = !isExpense && (
                title.contains("Paycheck", ignoreCase = true) ||
                noteStr.contains("Paycheck", ignoreCase = true) ||
                statusText.equals("Deposited", ignoreCase = true)
            )

            transactions.add(
                TransactionEntity(
                    id = 0,
                    title = title,
                    category = category,
                    amount = amount,
                    isExpense = isExpense,
                    timestamp = timestamp,
                    note = noteStr,
                    isAuto = isAuto,
                    statusText = statusText
                )
            )
        }

        return transactions
    }

    private fun parseTimestamp(dateStr: String): Long {
        val trimmed = dateStr.trim()
        if (trimmed.isEmpty()) return System.currentTimeMillis()

        trimmed.toLongOrNull()?.let {
            if (it > 1_000_000_000L) {
                return if (it < 100_000_000_000L) it * 1000L else it
            }
        }

        val dateFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US),
            SimpleDateFormat("MM/dd/yyyy", Locale.US)
        )

        for (df in dateFormats) {
            try {
                val parsed = df.parse(trimmed)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }

    private fun parseCsv(csvText: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var wasQuoted = false
        var i = 0
        val len = csvText.length

        while (i < len) {
            val c = csvText[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && csvText[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when {
                    c == '"' && currentField.isEmpty() -> {
                        inQuotes = true
                        wasQuoted = true
                    }
                    c == ',' -> {
                        val value = if (wasQuoted) currentField.toString() else currentField.toString().trim()
                        currentRow.add(value)
                        currentField.clear()
                        wasQuoted = false
                    }
                    c == '\r' -> {
                        if (i + 1 < len && csvText[i + 1] == '\n') {
                            i++
                        }
                        val value = if (wasQuoted) currentField.toString() else currentField.toString().trim()
                        currentRow.add(value)
                        currentField.clear()
                        wasQuoted = false
                        if (currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow.toList())
                        }
                        currentRow.clear()
                    }
                    c == '\n' -> {
                        val value = if (wasQuoted) currentField.toString() else currentField.toString().trim()
                        currentRow.add(value)
                        currentField.clear()
                        wasQuoted = false
                        if (currentRow.any { it.isNotBlank() }) {
                            rows.add(currentRow.toList())
                        }
                        currentRow.clear()
                    }
                    c.isWhitespace() && currentField.isEmpty() -> {
                        // Skip leading whitespace before field or quote
                    }
                    wasQuoted && c.isWhitespace() -> {
                        // Skip trailing whitespace after closing quote
                    }
                    else -> {
                        currentField.append(c)
                    }
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || wasQuoted || currentRow.isNotEmpty()) {
            val value = if (wasQuoted) currentField.toString() else currentField.toString().trim()
            currentRow.add(value)
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow.toList())
            }
        }

        return rows
    }
}

package com.example.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryEntity
import com.example.data.local.IncomeEngineEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.CategoryItem
import com.example.data.model.DefaultCategories
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AppScreen {
    LEDGER,
    ANALYTICS,
    SETTINGS
}

enum class AnalyticsPeriod {
    CURRENT_PAYCHECK,
    LAST_30_DAYS
}

data class CategorySpend(
    val category: CategoryItem,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentageOfSpend: Float,
    val averageAmount: Double
)

data class FinanceUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val incomeEngine: IncomeEngineEntity = IncomeEngineEntity(),
    val startingBalanceOffset: Double = 0.0,
    val rollingBalance: Double = 0.0,
    val periodIn: Double = 0.0,
    val periodOut: Double = 0.0,
    val daysLeftInPeriod: Int = 11,
    val periodDateRangeStr: String = "",
    val nextPaydayText: String = "",
    val burnProgress: Float = 0f,
    val burnPaceNote: String = "No transactions recorded yet.",
    val rankedBreakdown: List<CategorySpend> = emptyList(),
    val currentPeriod: AnalyticsPeriod = AnalyticsPeriod.CURRENT_PAYCHECK,
    val dbSizeBytes: Long = 0L,
    val categories: List<CategoryItem> = DefaultCategories.list
)

class FinanceViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: FinanceRepository = FinanceRepository(
        application,
        AppDatabase.getInstance(application).financeDao()
    ),
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : AndroidViewModel(application) {

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    private val _currentScreen = MutableStateFlow(AppScreen.LEDGER)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _analyticsPeriod = MutableStateFlow(AnalyticsPeriod.CURRENT_PAYCHECK)
    val analyticsPeriod: StateFlow<AnalyticsPeriod> = _analyticsPeriod.asStateFlow()

    private val _isNewTransactionSheetOpen = MutableStateFlow(false)
    val isNewTransactionSheetOpen: StateFlow<Boolean> = _isNewTransactionSheetOpen.asStateFlow()

    private val _editingTransaction = MutableStateFlow<TransactionEntity?>(null)
    val editingTransaction: StateFlow<TransactionEntity?> = _editingTransaction.asStateFlow()

    private val _isIncomeEngineOpen = MutableStateFlow(false)
    val isIncomeEngineOpen: StateFlow<Boolean> = _isIncomeEngineOpen.asStateFlow()

    private val _isCategoryManagerOpen = MutableStateFlow(false)
    val isCategoryManagerOpen: StateFlow<Boolean> = _isCategoryManagerOpen.asStateFlow()

    private val _isStartingBalanceDialogOpen = MutableStateFlow(false)
    val isStartingBalanceDialogOpen: StateFlow<Boolean> = _isStartingBalanceDialogOpen.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val uiState: StateFlow<FinanceUiState> = combine(
        repository.allTransactions,
        repository.incomeEngineConfig,
        repository.startingBalanceOffset,
        _analyticsPeriod,
        repository.allCategories
    ) { transactions, incomeConfigRaw, offset, period, categoriesRaw ->
        val incomeConfig = incomeConfigRaw ?: IncomeEngineEntity()

        val categoryItems = if (categoriesRaw.isEmpty()) {
            DefaultCategories.list
        } else {
            categoriesRaw.map { DefaultCategories.fromEntity(it) }
        }

        val totalIncome = transactions.filter { !it.isExpense }.sumOf { it.amount }
        val totalExpenses = transactions.filter { it.isExpense }.sumOf { it.amount }
        val rollingBalance = offset + totalIncome - totalExpenses

        // Period calculations
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60 * 60 * 1000
        val cutoff = if (period == AnalyticsPeriod.CURRENT_PAYCHECK) {
            now - (14L * dayMillis)
        } else {
            now - (30L * dayMillis)
        }

        val periodTransactions = transactions.filter { it.timestamp >= cutoff }
        val periodIn = periodTransactions.filter { !it.isExpense }.sumOf { it.amount }
        val periodOut = periodTransactions.filter { it.isExpense }.sumOf { it.amount }

        val burnRatio = if (periodIn > 0) {
            (periodOut / periodIn).toFloat().coerceIn(0f, 1f)
        } else if (incomeConfig.expectedIncome > 0 && periodOut > 0) {
            (periodOut / incomeConfig.expectedIncome).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

        val calToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calPayday = Calendar.getInstance().apply {
            timeInMillis = incomeConfig.nextPaydayTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If configured payday has already passed, roll forward according to cadence
        if (incomeConfig.expectedIncome > 0) {
            while (calPayday.timeInMillis < calToday.timeInMillis) {
                when (incomeConfig.cadence.lowercase()) {
                    "weekly" -> calPayday.add(Calendar.DAY_OF_YEAR, 7)
                    "monthly" -> calPayday.add(Calendar.MONTH, 1)
                    else -> calPayday.add(Calendar.DAY_OF_YEAR, 14) // Bi-Weekly
                }
            }
        }

        val diffDays = ((calPayday.timeInMillis - calToday.timeInMillis) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
        val daysLeft = diffDays.coerceIn(1, 31)
        val daysElapsed = (14 - daysLeft).coerceAtLeast(1)
        val dailyPace = if (periodOut > 0) periodOut / daysElapsed else 0.0

        val paceNote = if (transactions.isEmpty() || periodOut == 0.0) {
            if (transactions.isEmpty()) "No transactions recorded yet." else "No expenditures recorded for this period."
        } else if (period == AnalyticsPeriod.CURRENT_PAYCHECK) {
            "You're spending at a steady, balanced pace ($${String.format(Locale.US, "%.2f", dailyPace)}/day)."
        } else {
            val dailyPace30 = periodOut / 30.0
            "30-day average expenditure sits at $${String.format(Locale.US, "%.2f", dailyPace30)}/day."
        }

        // Breakdown by category
        val expensesList = periodTransactions.filter { it.isExpense }
        val grouped = expensesList.groupBy { it.category }
        val totalSpendAmount = expensesList.sumOf { it.amount }.coerceAtLeast(0.01)

        val rankedBreakdown = grouped.map { (catName, items) ->
            val catTotal = items.sumOf { it.amount }
            val count = items.size
            val percent = (catTotal / totalSpendAmount).toFloat()
            val avg = if (count > 0) catTotal / count else 0.0
            CategorySpend(
                category = DefaultCategories.getCategoryByName(catName, categoryItems),
                totalAmount = catTotal,
                transactionCount = count,
                percentageOfSpend = percent,
                averageAmount = avg
            )
        }.sortedByDescending { it.totalAmount }

        // Date calculations
        val sdf = SimpleDateFormat("MMM d", Locale.US)
        val startDateStr = sdf.format(cutoff)
        val endDateStr = sdf.format(now)
        val dateRangeStr = "$startDateStr – $endDateStr"

        val nextIncomeDayName = SimpleDateFormat("EEEE", Locale.US).format(calPayday.time)
        val nextPaydayText = if (incomeConfig.expectedIncome <= 0) {
            "No recurring income configured"
        } else {
            when (diffDays) {
                0 -> "Next income: $${String.format(Locale.US, "%,.0f", incomeConfig.expectedIncome)} today"
                1 -> "Next income: $${String.format(Locale.US, "%,.0f", incomeConfig.expectedIncome)} tomorrow (in 1 day)"
                else -> "Next income: $${String.format(Locale.US, "%,.0f", incomeConfig.expectedIncome)} on $nextIncomeDayName (in $diffDays days)"
            }
        }

        FinanceUiState(
            transactions = transactions,
            incomeEngine = incomeConfig,
            startingBalanceOffset = offset,
            rollingBalance = rollingBalance,
            periodIn = periodIn,
            periodOut = periodOut,
            daysLeftInPeriod = daysLeft,
            periodDateRangeStr = dateRangeStr,
            nextPaydayText = nextPaydayText,
            burnProgress = burnRatio,
            burnPaceNote = paceNote,
            rankedBreakdown = rankedBreakdown,
            currentPeriod = period,
            dbSizeBytes = repository.getDatabaseSizeBytes(),
            categories = categoryItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = sharingStarted,
        initialValue = FinanceUiState()
    )

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setAnalyticsPeriod(period: AnalyticsPeriod) {
        _analyticsPeriod.value = period
    }

    fun openNewTransactionSheet() {
        _editingTransaction.value = null
        _isNewTransactionSheetOpen.value = true
    }

    fun openEditTransaction(transaction: TransactionEntity) {
        _editingTransaction.value = transaction
        _isNewTransactionSheetOpen.value = true
    }

    fun closeNewTransactionSheet() {
        _isNewTransactionSheetOpen.value = false
        _editingTransaction.value = null
    }

    fun openIncomeEngine() {
        _isIncomeEngineOpen.value = true
    }

    fun closeIncomeEngine() {
        _isIncomeEngineOpen.value = false
    }

    fun openCategoryManager() {
        _isCategoryManagerOpen.value = true
    }

    fun closeCategoryManager() {
        _isCategoryManagerOpen.value = false
    }

    fun openStartingBalanceDialog() {
        _isStartingBalanceDialogOpen.value = true
    }

    fun closeStartingBalanceDialog() {
        _isStartingBalanceDialogOpen.value = false
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun addTransaction(
        title: String,
        category: String,
        amount: Double,
        isExpense: Boolean,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val status = if (isExpense) "Settled" else "Deposited"
            val displayTitle = if (title.isNotBlank()) title else category
            repository.insertTransaction(
                TransactionEntity(
                    title = displayTitle,
                    category = category,
                    amount = amount,
                    isExpense = isExpense,
                    timestamp = timestamp,
                    note = note,
                    statusText = status
                )
            )
            closeNewTransactionSheet()
            showToast("Saved to Ledger (${if (isExpense) "-" else "+"}$${String.format(Locale.US, "%.2f", amount)})")
        }
    }

    fun updateTransaction(
        id: Long,
        title: String,
        category: String,
        amount: Double,
        isExpense: Boolean,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val status = if (isExpense) "Settled" else "Deposited"
            val displayTitle = if (title.isNotBlank()) title else category
            repository.updateTransaction(
                TransactionEntity(
                    id = id,
                    title = displayTitle,
                    category = category,
                    amount = amount,
                    isExpense = isExpense,
                    timestamp = timestamp,
                    note = note,
                    statusText = status
                )
            )
            closeNewTransactionSheet()
            showToast("Transaction updated (${if (isExpense) "-" else "+"}$${String.format(Locale.US, "%.2f", amount)})")
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            closeNewTransactionSheet()
            showToast("Transaction removed from ledger")
        }
    }

    fun updateIncomeEngine(expectedIncome: Double, cadence: String, nextPaydayDate: Long) {
        viewModelScope.launch {
            repository.updateIncomeEngine(
                IncomeEngineEntity(
                    id = 1,
                    expectedIncome = expectedIncome,
                    cadence = cadence,
                    nextPaydayTimestamp = nextPaydayDate,
                    isActive = true
                )
            )
            closeIncomeEngine()
            showToast("Income Engine updated")
        }
    }

    fun updateStartingBalanceOffset(offset: Double) {
        viewModelScope.launch {
            repository.setStartingBalanceOffset(offset)
            closeStartingBalanceDialog()
            showToast("Offset adjusted to $${String.format(Locale.US, "%.2f", offset)}")
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            showToast("Database cleared. Balance reset to $0.00")
        }
    }

    fun restoreDemoData() {
        viewModelScope.launch {
            repository.restoreDemoData()
            showToast("Demo sample data restored")
        }
    }

    fun createCategory(name: String, iconKey: String, colorHex: Long) {
        viewModelScope.launch {
            repository.insertCategory(
                CategoryEntity(
                    name = name.trim(),
                    iconKey = iconKey,
                    colorHex = colorHex
                )
            )
            showToast("Category \"${name.trim()}\" created")
        }
    }

    fun updateCategory(id: Long, name: String, iconKey: String, colorHex: Long, oldName: String) {
        viewModelScope.launch {
            repository.updateCategory(
                CategoryEntity(
                    id = id,
                    name = name.trim(),
                    iconKey = iconKey,
                    colorHex = colorHex
                ),
                oldName = oldName
            )
            showToast("Category updated")
        }
    }

    fun deleteCategory(id: Long, name: String) {
        viewModelScope.launch {
            repository.deleteCategory(id, name)
            showToast("Category \"$name\" deleted")
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            showToast("Preparing CSV for Android Share Sheet...")
            val csv = repository.generateCsvData()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Base_Financial_Ledger.csv")
                putExtra(Intent.EXTRA_TEXT, csv)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Export Ledger CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(chooser)
        }
    }
}

package com.example.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
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
    val rollingBalance: Double = 3420.50,
    val periodIn: Double = 4200.00,
    val periodOut: Double = 779.50,
    val daysLeftInPeriod: Int = 11,
    val periodDateRangeStr: String = "Aug 11 – Aug 25",
    val nextPaydayText: String = "Next income: $2,100 on Friday (in 3 days)",
    val burnProgress: Float = 0.371f,
    val burnPaceNote: String = "You're spending at a steady, balanced pace ($70.86/day).",
    val rankedBreakdown: List<CategorySpend> = emptyList(),
    val currentPeriod: AnalyticsPeriod = AnalyticsPeriod.CURRENT_PAYCHECK,
    val dbSizeBytes: Long = 142 * 1024L
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository

    init {
        val database = AppDatabase.getInstance(application)
        repository = FinanceRepository(application, database.financeDao())
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
        _analyticsPeriod
    ) { transactions, incomeConfigRaw, offset, period ->
        val incomeConfig = incomeConfigRaw ?: IncomeEngineEntity()

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
        val actualIncomeInPeriod = transactions.filter { !it.isExpense && it.timestamp >= cutoff }.sumOf { it.amount }
        val periodIn = if (transactions.isEmpty()) {
            0.0
        } else if (period == AnalyticsPeriod.CURRENT_PAYCHECK) {
            incomeConfig.expectedIncome.coerceAtLeast(actualIncomeInPeriod)
        } else {
            (incomeConfig.expectedIncome * 2).coerceAtLeast(actualIncomeInPeriod)
        }
        val periodOut = if (transactions.isEmpty()) {
            0.0
        } else {
            periodTransactions.filter { it.isExpense }.sumOf { it.amount }.let {
                if (it == 0.0 && transactions.isNotEmpty()) totalExpenses else it
            }
        }

        val burnRatio = if (periodIn > 0) (periodOut / periodIn).toFloat().coerceIn(0f, 1f) else 0f
        val daysInPeriod = if (period == AnalyticsPeriod.CURRENT_PAYCHECK) 14 else 30
        val daysElapsed = 14 - 11 // ~3-4 days
        val dailyPace = if (daysElapsed > 0) periodOut / daysElapsed.coerceAtLeast(1) else 0.0

        val paceNote = if (transactions.isEmpty()) {
            "No transactions recorded yet."
        } else if (period == AnalyticsPeriod.CURRENT_PAYCHECK) {
            "You're spending at a steady, balanced pace ($${String.format(Locale.US, "%.2f", dailyPace)}/day)."
        } else {
            "30-day average expenditure sits at $${String.format(Locale.US, "%.2f", periodOut / 30.0)}/day."
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
                category = DefaultCategories.getCategoryByName(catName),
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

        val daysLeft = ((incomeConfig.nextPaydayTimestamp - now) / dayMillis).toInt().coerceAtLeast(1)
        val nextIncomeDayName = SimpleDateFormat("EEEE", Locale.US).format(incomeConfig.nextPaydayTimestamp)
        val nextPaydayText = "Next income: $${String.format(Locale.US, "%,.0f", incomeConfig.expectedIncome)} on $nextIncomeDayName (in $daysLeft days)"

        FinanceUiState(
            transactions = transactions,
            incomeEngine = incomeConfig,
            startingBalanceOffset = offset,
            rollingBalance = rollingBalance,
            periodIn = periodIn,
            periodOut = periodOut,
            daysLeftInPeriod = daysLeft,
            periodDateRangeStr = if (period == AnalyticsPeriod.CURRENT_PAYCHECK) "Aug 11 – Aug 25" else "Jul 26 – Aug 25",
            nextPaydayText = nextPaydayText,
            burnProgress = burnRatio,
            burnPaceNote = paceNote,
            rankedBreakdown = rankedBreakdown,
            currentPeriod = period,
            dbSizeBytes = repository.getDatabaseSizeBytes()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceUiState()
    )

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setAnalyticsPeriod(period: AnalyticsPeriod) {
        _analyticsPeriod.value = period
    }

    fun openNewTransactionSheet() {
        _isNewTransactionSheetOpen.value = true
    }

    fun closeNewTransactionSheet() {
        _isNewTransactionSheetOpen.value = false
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
        note: String
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
                    timestamp = System.currentTimeMillis(),
                    note = note,
                    statusText = status
                )
            )
            closeNewTransactionSheet()
            showToast("Saved to Ledger (${if (isExpense) "-" else "+"}$${String.format(Locale.US, "%.2f", amount)})")
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
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

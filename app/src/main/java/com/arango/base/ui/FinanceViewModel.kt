package com.arango.base.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arango.base.data.local.AppDatabase
import com.arango.base.data.local.CategoryEntity
import com.arango.base.data.local.IncomeEngineEntity
import com.arango.base.data.local.OffsetTransactionEntity
import com.arango.base.data.local.TransactionEntity
import com.arango.base.data.model.CategoryItem
import com.arango.base.data.model.DefaultCategories
import com.arango.base.data.repository.FinanceRepository
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
import kotlin.math.abs

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
    val offsetTransactions: List<OffsetTransactionEntity> = emptyList(),
    val rollingBalance: Double = 0.0,
    val periodIn: Double = 0.0,
    val periodOut: Double = 0.0,
    val currentPeriodIn: Double = 0.0,
    val currentPeriodOut: Double = 0.0,
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
            checkRecurringIncomeTriggers()
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

    private val _isOffsetBudgetDialogOpen = MutableStateFlow(false)
    val isOffsetBudgetDialogOpen: StateFlow<Boolean> = _isOffsetBudgetDialogOpen.asStateFlow()
    val isStartingBalanceDialogOpen: StateFlow<Boolean> get() = _isOffsetBudgetDialogOpen.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<FinanceUiState> = combine(
        repository.allTransactions,
        repository.incomeEngineConfig,
        repository.totalOffset,
        _analyticsPeriod,
        repository.allCategories,
        repository.allOffsetTransactions
    ) { args: Array<Any?> ->
        val transactions = args[0] as List<TransactionEntity>
        val incomeConfigRaw = args[1] as IncomeEngineEntity?
        val offset = args[2] as Double
        val period = args[3] as AnalyticsPeriod
        val categoriesRaw = args[4] as List<CategoryEntity>
        val offsetTxs = args[5] as List<OffsetTransactionEntity>

        val incomeConfig = incomeConfigRaw ?: IncomeEngineEntity()

        val categoryItems = if (categoriesRaw.isEmpty()) {
            DefaultCategories.list
        } else {
            categoriesRaw.map { DefaultCategories.fromEntity(it) }
        }

        val totalIncome = transactions.filter { !it.isExpense }.sumOf { it.amount }
        val totalExpenses = transactions.filter { it.isExpense }.sumOf { it.amount }
        val rollingBalance = offset + totalIncome - totalExpenses

        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60 * 60 * 1000

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

        // If configured payday has arrived or passed, roll forward to the next period's start
        if (incomeConfig.expectedIncome > 0) {
            while (calPayday.timeInMillis <= calToday.timeInMillis) {
                when (incomeConfig.cadence.lowercase()) {
                    "weekly" -> calPayday.add(Calendar.DAY_OF_YEAR, 7)
                    "monthly" -> calPayday.add(Calendar.MONTH, 1)
                    else -> calPayday.add(Calendar.DAY_OF_YEAR, 14) // Bi-Weekly
                }
            }
        }

        val diffDays = ((calPayday.timeInMillis - calToday.timeInMillis) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(1)

        // Current Paycheck Period calculation: starts exactly one cadence cycle prior to next payday,
        // and ends the day before the next period starts (e.g. if new period starts Oct 7, previous ends Oct 6)
        val currentPeriodStart: Long
        val currentPeriodEnd: Long
        val cycleDaysTotal: Int

        if (incomeConfig.expectedIncome > 0) {
            val calCycleStart = (calPayday.clone() as Calendar).apply {
                when (incomeConfig.cadence.lowercase()) {
                    "weekly" -> add(Calendar.DAY_OF_YEAR, -7)
                    "monthly" -> add(Calendar.MONTH, -1)
                    else -> add(Calendar.DAY_OF_YEAR, -14)
                }
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val calCycleEnd = (calPayday.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -1) // Ends day before next period starts
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            currentPeriodStart = calCycleStart.timeInMillis
            currentPeriodEnd = calCycleEnd.timeInMillis
            cycleDaysTotal = when (incomeConfig.cadence.lowercase()) {
                "weekly" -> 7
                "monthly" -> (((calPayday.timeInMillis - calCycleStart.timeInMillis) / dayMillis)).toInt().coerceAtLeast(28)
                else -> 14
            }
        } else {
            currentPeriodStart = calToday.timeInMillis - (13L * dayMillis)
            currentPeriodEnd = calToday.timeInMillis + dayMillis - 1
            cycleDaysTotal = 14
        }

        val (periodStart, periodEnd) = if (period == AnalyticsPeriod.CURRENT_PAYCHECK) {
            currentPeriodStart to currentPeriodEnd
        } else {
            (calToday.timeInMillis - (29L * dayMillis)) to (calToday.timeInMillis + dayMillis - 1)
        }

        // Transactions filtered for active analytics period
        val periodTransactions = transactions.filter { it.timestamp in periodStart..periodEnd }
        val periodIn = periodTransactions.filter { !it.isExpense }.sumOf { it.amount }
        val periodOut = periodTransactions.filter { it.isExpense }.sumOf { it.amount }

        // Current paycheck transactions (strictly for ledger & current paycheck summary)
        val currentPaycheckTransactions = transactions.filter { it.timestamp in currentPeriodStart..currentPeriodEnd }
        val currentPeriodIn = currentPaycheckTransactions.filter { !it.isExpense }.sumOf { it.amount }
        val currentPeriodOut = currentPaycheckTransactions.filter { it.isExpense }.sumOf { it.amount }

        val burnRatio = if (periodIn > 0) {
            (periodOut / periodIn).toFloat().coerceIn(0f, 1f)
        } else if (incomeConfig.expectedIncome > 0 && periodOut > 0) {
            (periodOut / incomeConfig.expectedIncome).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

        val daysLeft = if (incomeConfig.expectedIncome > 0) diffDays.coerceIn(1, cycleDaysTotal) else 14
        val daysElapsed = (cycleDaysTotal - daysLeft + 1).coerceAtLeast(1)
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
        val startDateStr = sdf.format(periodStart)
        val endDateStr = sdf.format(periodEnd)
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
            offsetTransactions = offsetTxs,
            rollingBalance = rollingBalance,
            periodIn = periodIn,
            periodOut = periodOut,
            currentPeriodIn = currentPeriodIn,
            currentPeriodOut = currentPeriodOut,
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

    fun openOffsetBudgetDialog() {
        _isOffsetBudgetDialogOpen.value = true
    }

    fun closeOffsetBudgetDialog() {
        _isOffsetBudgetDialogOpen.value = false
    }

    fun openStartingBalanceDialog() = openOffsetBudgetDialog()

    fun closeStartingBalanceDialog() = closeOffsetBudgetDialog()

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
            val triggered = repository.checkAndTriggerRecurringIncome()
            closeIncomeEngine()
            if (triggered.isNotEmpty()) {
                val totalAmount = triggered.sumOf { it.amount }
                showToast("Income Engine updated • +$${String.format(Locale.US, "%,.2f", totalAmount)} deposited to ledger")
            } else {
                showToast("Income Engine updated")
            }
        }
    }

    fun checkRecurringIncomeTriggers() {
        viewModelScope.launch {
            val triggered = repository.checkAndTriggerRecurringIncome()
            if (triggered.isNotEmpty()) {
                val totalAmount = triggered.sumOf { it.amount }
                showToast("Payday! +$${String.format(Locale.US, "%,.2f", totalAmount)} automatically deposited to ledger")
            }
        }
    }

    fun triggerIncomeNow() {
        viewModelScope.launch {
            val triggered = repository.triggerIncomeNow()
            if (triggered != null) {
                closeIncomeEngine()
                showToast("Payday! +$${String.format(Locale.US, "%,.2f", triggered.amount)} deposited to ledger")
            } else {
                showToast("No active recurring income configured")
            }
        }
    }

    fun addOffsetTransaction(amount: Double, note: String = "Balance adjustment") {
        viewModelScope.launch {
            repository.addOffsetTransaction(amount, note)
            val formatted = String.format(Locale.US, "%.2f", abs(amount))
            if (amount >= 0) {
                showToast("Offset +$$formatted applied to balance")
            } else {
                showToast("Offset -$$formatted adjusted from balance")
            }
        }
    }

    fun deleteOffsetTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteOffsetTransaction(id)
            showToast("Offset adjustment removed")
        }
    }

    fun updateStartingBalanceOffset(offset: Double) {
        viewModelScope.launch {
            repository.setStartingBalanceOffset(offset)
            closeOffsetBudgetDialog()
            showToast("Balance offset updated to $${String.format(Locale.US, "%.2f", offset)}")
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

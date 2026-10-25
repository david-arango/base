package com.arango.base.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.arango.base.data.local.AppDatabase
import com.arango.base.data.local.AppPreferenceEntity
import com.arango.base.data.local.FinanceDao
import com.arango.base.data.local.TransactionEntity
import com.arango.base.data.model.CategoryItem
import com.arango.base.data.model.DefaultCategories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class WidgetDataSnapshot(
    val rollingBalance: Double,
    val isBalanceMasked: Boolean,
    val formattedBalance: String,
    val periodSummaryText: String,
    val topCategories: List<CategoryItem>,
    val recentTransactions: List<TransactionEntity> = emptyList()
)

object WidgetUpdateHelper {

    const val PREF_WIDGET_BALANCE_MASKED = "widget_balance_masked"
    const val ACTION_TOGGLE_BALANCE_MASK = "com.arango.base.widget.ACTION_TOGGLE_BALANCE_MASK"
    const val ACTION_REFRESH_WIDGETS = "com.arango.base.widget.ACTION_REFRESH_WIDGETS"
    const val MASKED_BALANCE_TEXT = "$••••••"

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun formatRollingBalance(balance: Double, isMasked: Boolean): String {
        if (isMasked) return MASKED_BALANCE_TEXT
        val absVal = abs(balance)
        val formatted = String.format(Locale.US, "%,.2f", absVal)
        return if (balance < -0.004) "-$$formatted" else "$$formatted"
    }

    fun formatTransactionAmount(tx: TransactionEntity): String {
        val formatted = String.format(Locale.US, "%,.2f", abs(tx.amount))
        return if (tx.isExpense) "-$$formatted" else "+$$formatted"
    }

    fun formatTransactionDate(timestamp: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val txCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val isToday = nowCal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)
        return if (isToday) {
            "Today"
        } else {
            SimpleDateFormat("MMM d", Locale.US).format(Date(timestamp))
        }
    }

    suspend fun loadSnapshot(dao: FinanceDao): WidgetDataSnapshot {
        val transactions = dao.getAllTransactionsList()
        val offset = dao.getTotalOffsetDirect()
        val incomeConfig = dao.getIncomeEngineConfigDirect()
        val categoriesEntities = dao.getAllCategoriesList()
        val isMasked = dao.getPreference(PREF_WIDGET_BALANCE_MASKED) == "true"

        val totalIncome = transactions.filter { !it.isExpense }.sumOf { it.amount }
        val totalExpenses = transactions.filter { it.isExpense }.sumOf { it.amount }
        val rollingBalance = offset + totalIncome - totalExpenses

        val topCategories = if (categoriesEntities.isEmpty()) {
            DefaultCategories.list
        } else {
            categoriesEntities
                .sortedWith(compareBy<com.arango.base.data.local.CategoryEntity> { it.displayOrder }.thenBy { it.id })
                .map { DefaultCategories.fromEntity(it) }
        }

        val periodSummary = if (incomeConfig != null && incomeConfig.expectedIncome > 0.0) {
            val calToday = Calendar.getInstance().apply {
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
            while (calPayday.timeInMillis <= calToday.timeInMillis) {
                when (incomeConfig.cadence.lowercase(Locale.US)) {
                    "weekly" -> calPayday.add(Calendar.DAY_OF_YEAR, 7)
                    "monthly" -> calPayday.add(Calendar.MONTH, 1)
                    else -> calPayday.add(Calendar.DAY_OF_YEAR, 14)
                }
            }
            val diffDays = ((calPayday.timeInMillis - calToday.timeInMillis) / (24L * 60 * 60 * 1000))
                .toInt()
                .coerceAtLeast(1)
            "• ${diffDays}d left"
        } else {
            ""
        }

        return WidgetDataSnapshot(
            rollingBalance = rollingBalance,
            isBalanceMasked = isMasked,
            formattedBalance = formatRollingBalance(rollingBalance, isMasked),
            periodSummaryText = periodSummary,
            topCategories = topCategories,
            recentTransactions = transactions.take(12)
        )
    }

    suspend fun toggleBalanceMask(dao: FinanceDao): Boolean {
        val current = dao.getPreference(PREF_WIDGET_BALANCE_MASKED) == "true"
        val next = !current
        dao.setPreference(AppPreferenceEntity(PREF_WIDGET_BALANCE_MASKED, next.toString()))
        return next
    }

    suspend fun updateAllWidgetsSync(context: Context) {
        val appContext = context.applicationContext
        val dao = AppDatabase.getInstance(appContext).financeDao()
        val snapshot = loadSnapshot(dao)
        val manager = AppWidgetManager.getInstance(appContext)

        val balanceComponent = ComponentName(appContext, BalanceQuickAddWidgetProvider::class.java)
        val balanceIds = manager.getAppWidgetIds(balanceComponent)
        for (id in balanceIds) {
            val options = manager.getAppWidgetOptions(id)
            val views = BalanceQuickAddWidgetProvider.buildRemoteViews(appContext, id, options, snapshot)
            manager.updateAppWidget(id, views)
        }

        val shortcutsComponent = ComponentName(appContext, CategoryShortcutsWidgetProvider::class.java)
        val shortcutsIds = manager.getAppWidgetIds(shortcutsComponent)
        for (id in shortcutsIds) {
            val options = manager.getAppWidgetOptions(id)
            val views = CategoryShortcutsWidgetProvider.buildRemoteViews(appContext, id, options, snapshot)
            manager.updateAppWidget(id, views)
        }
    }

    fun updateAllWidgetsAsync(context: Context) {
        val appContext = context.applicationContext
        widgetScope.launch {
            try {
                updateAllWidgetsSync(appContext)
            } catch (_: Exception) {
                // Ignore background widget update errors if database is closing
            }
        }
    }

    fun createQuickAddIntent(
        context: Context,
        categoryName: String? = null,
        isExpense: Boolean = true
    ): Intent {
        return Intent(context, QuickAddActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!categoryName.isNullOrBlank()) {
                putExtra(QuickAddActivity.EXTRA_CATEGORY_NAME, categoryName)
            }
            putExtra(QuickAddActivity.EXTRA_IS_EXPENSE, isExpense)
        }
    }
}

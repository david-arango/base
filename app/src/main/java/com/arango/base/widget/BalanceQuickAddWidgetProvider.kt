package com.arango.base.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import com.arango.base.MainActivity
import com.arango.base.R
import com.arango.base.data.local.AppDatabase
import com.arango.base.data.model.DefaultCategories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BalanceQuickAddWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(appContext).financeDao()
                val snapshot = WidgetUpdateHelper.loadSnapshot(dao)
                for (widgetId in appWidgetIds) {
                    val options = appWidgetManager.getAppWidgetOptions(widgetId)
                    val views = buildRemoteViews(appContext, widgetId, options, snapshot)
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(appContext).financeDao()
                val snapshot = WidgetUpdateHelper.loadSnapshot(dao)
                val views = buildRemoteViews(appContext, appWidgetId, newOptions, snapshot)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetUpdateHelper.ACTION_TOGGLE_BALANCE_MASK -> {
                val pendingResult = goAsync()
                val appContext = context.applicationContext
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val dao = AppDatabase.getInstance(appContext).financeDao()
                        WidgetUpdateHelper.toggleBalanceMask(dao)
                        WidgetUpdateHelper.updateAllWidgetsSync(appContext)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            WidgetUpdateHelper.ACTION_REFRESH_WIDGETS -> {
                val pendingResult = goAsync()
                val appContext = context.applicationContext
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        WidgetUpdateHelper.updateAllWidgetsSync(appContext)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        private val CAT_CONTAINER_IDS = intArrayOf(
            R.id.widget_cat_1,
            R.id.widget_cat_2,
            R.id.widget_cat_3,
            R.id.widget_cat_4
        )
        private val CAT_ICON_IDS = intArrayOf(
            R.id.widget_cat_icon_1,
            R.id.widget_cat_icon_2,
            R.id.widget_cat_icon_3,
            R.id.widget_cat_icon_4
        )
        private val TX_ROW_IDS = intArrayOf(
            R.id.widget_tx_row_1,
            R.id.widget_tx_row_2,
            R.id.widget_tx_row_3,
            R.id.widget_tx_row_4,
            R.id.widget_tx_row_5,
            R.id.widget_tx_row_6,
            R.id.widget_tx_row_7,
            R.id.widget_tx_row_8,
            R.id.widget_tx_row_9,
            R.id.widget_tx_row_10,
            R.id.widget_tx_row_11,
            R.id.widget_tx_row_12
        )
        private val TX_ICON_IDS = intArrayOf(
            R.id.widget_tx_icon_1,
            R.id.widget_tx_icon_2,
            R.id.widget_tx_icon_3,
            R.id.widget_tx_icon_4,
            R.id.widget_tx_icon_5,
            R.id.widget_tx_icon_6,
            R.id.widget_tx_icon_7,
            R.id.widget_tx_icon_8,
            R.id.widget_tx_icon_9,
            R.id.widget_tx_icon_10,
            R.id.widget_tx_icon_11,
            R.id.widget_tx_icon_12
        )
        private val TX_TITLE_IDS = intArrayOf(
            R.id.widget_tx_title_1,
            R.id.widget_tx_title_2,
            R.id.widget_tx_title_3,
            R.id.widget_tx_title_4,
            R.id.widget_tx_title_5,
            R.id.widget_tx_title_6,
            R.id.widget_tx_title_7,
            R.id.widget_tx_title_8,
            R.id.widget_tx_title_9,
            R.id.widget_tx_title_10,
            R.id.widget_tx_title_11,
            R.id.widget_tx_title_12
        )
        private val TX_DATE_IDS = intArrayOf(
            R.id.widget_tx_date_1,
            R.id.widget_tx_date_2,
            R.id.widget_tx_date_3,
            R.id.widget_tx_date_4,
            R.id.widget_tx_date_5,
            R.id.widget_tx_date_6,
            R.id.widget_tx_date_7,
            R.id.widget_tx_date_8,
            R.id.widget_tx_date_9,
            R.id.widget_tx_date_10,
            R.id.widget_tx_date_11,
            R.id.widget_tx_date_12
        )
        private val TX_AMOUNT_IDS = intArrayOf(
            R.id.widget_tx_amount_1,
            R.id.widget_tx_amount_2,
            R.id.widget_tx_amount_3,
            R.id.widget_tx_amount_4,
            R.id.widget_tx_amount_5,
            R.id.widget_tx_amount_6,
            R.id.widget_tx_amount_7,
            R.id.widget_tx_amount_8,
            R.id.widget_tx_amount_9,
            R.id.widget_tx_amount_10,
            R.id.widget_tx_amount_11,
            R.id.widget_tx_amount_12
        )

        private fun dpToPx(context: Context, dp: Int): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }

        /**
         * Dynamically computes how many recent transaction rows can fit inside the widget's
         * available vertical height (in dp) below the rolling balance header and category row.
         * Note: On Android launchers, OPTION_APPWIDGET_MIN_HEIGHT is the landscape height while
         * OPTION_APPWIDGET_MAX_HEIGHT is the portrait height.
         */
        fun computeMaxTransactionRows(minHeightDp: Int, maxHeightDp: Int = 0): Int {
            if (minHeightDp < 100) return 0
            val effectiveHeightDp = if (maxHeightDp > minHeightDp) {
                maxHeightDp
            } else {
                (minHeightDp * 1.65).toInt()
            }
            return ((effectiveHeightDp - 105) / 26).coerceIn(2, TX_ROW_IDS.size)
        }

        fun buildRemoteViews(
            context: Context,
            appWidgetId: Int,
            options: Bundle?,
            snapshot: WidgetDataSnapshot
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_balance_quick_add)

            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                ?.takeIf { it > 0 } ?: 250
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 60)
                ?.takeIf { it > 0 } ?: 60
            val maxHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight)
                ?.takeIf { it > 0 } ?: minHeight

            // Primary "+ Add" button -> open QuickAddActivity
            val defaultCatName = snapshot.topCategories.firstOrNull()?.name
            val quickAddIntent = WidgetUpdateHelper.createQuickAddIntent(context, defaultCatName)
            val quickAddPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 3,
                quickAddIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_add_btn, quickAddPendingIntent)

            // 1x1 mode (minWidth < 110dp): show ONLY the quick-add button
            if (minWidth < 110) {
                val padPx = dpToPx(context, 8)
                views.setViewPadding(R.id.widget_root, padPx, padPx, padPx, padPx)
                views.setInt(R.id.widget_root, "setGravity", Gravity.CENTER)
                views.setViewVisibility(R.id.widget_balance_container, View.GONE)
                views.setViewVisibility(R.id.widget_expanded_section, View.GONE)
                views.setViewVisibility(R.id.widget_add_btn_label, View.GONE)
                views.setViewVisibility(R.id.widget_add_btn, View.VISIBLE)
                return views
            }

            if (minHeight >= 100) {
                val hPadPx = dpToPx(context, 18)
                val vPadPx = dpToPx(context, 16)
                views.setViewPadding(R.id.widget_root, hPadPx, vPadPx, hPadPx, vPadPx)
                views.setInt(R.id.widget_root, "setGravity", Gravity.TOP)
            } else {
                val hPadPx = dpToPx(context, 12)
                val vPadPx = dpToPx(context, 8)
                views.setViewPadding(R.id.widget_root, hPadPx, vPadPx, hPadPx, vPadPx)
                views.setInt(R.id.widget_root, "setGravity", Gravity.CENTER_VERTICAL)
            }
            views.setViewVisibility(R.id.widget_balance_container, View.VISIBLE)

            // 1. Balance text & color
            views.setTextViewText(R.id.widget_balance_amount, snapshot.formattedBalance)
            val balanceColor = if (!snapshot.isBalanceMasked && snapshot.rollingBalance < -0.004) {
                0xFF8F2D20.toInt() // CalmNegativeDeep
            } else {
                0xFF181C1B.toInt() // CalmOnSurface
            }
            views.setTextColor(R.id.widget_balance_amount, balanceColor)

            // 2. Eye toggle icon (next to ROLLING BALANCE title) & broadcast intent
            val eyeDrawable = if (snapshot.isBalanceMasked) {
                R.drawable.ic_widget_visibility_off
            } else {
                R.drawable.ic_widget_visibility
            }
            views.setImageViewResource(R.id.widget_eye_icon, eyeDrawable)

            val toggleMaskIntent = Intent(context, BalanceQuickAddWidgetProvider::class.java).apply {
                action = WidgetUpdateHelper.ACTION_TOGGLE_BALANCE_MASK
            }
            val toggleMaskPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 1,
                toggleMaskIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_eye_toggle_btn, toggleMaskPendingIntent)

            // 3. Balance container / amount click -> open MainActivity (LedgerScreen)
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 2,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_balance_container, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_balance_amount, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_balance_label, openAppPendingIntent)

            // 4. Responsive adjustments for width & height
            views.setViewVisibility(
                R.id.widget_add_btn_label,
                if (minWidth >= 165) View.VISIBLE else View.GONE
            )

            if (minWidth >= 220 && snapshot.periodSummaryText.isNotBlank()) {
                views.setTextViewText(R.id.widget_period_summary, snapshot.periodSummaryText)
                views.setViewVisibility(R.id.widget_period_summary, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_period_summary, View.GONE)
            }

            // 5. Expanded Category Quick Selectors + Recent Transactions when widget is 2+ rows tall (minHeight >= 100dp)
            if (minHeight >= 100) {
                views.setViewVisibility(R.id.widget_expanded_section, View.VISIBLE)

                // Populate monochrome category quick selectors (icons only)
                val visibleCatCount = when {
                    minWidth >= 250 -> 4
                    minWidth >= 190 -> 3
                    else -> 2
                }
                val categories = snapshot.topCategories
                for (i in 0 until 4) {
                    val containerId = CAT_CONTAINER_IDS[i]
                    if (i < visibleCatCount && i < categories.size) {
                        val cat = categories[i]
                        views.setViewVisibility(containerId, View.VISIBLE)
                        views.setContentDescription(containerId, cat.name)

                        val iconBitmap = WidgetIconRenderer.renderCategoryIconBitmap(
                            icon = cat.icon,
                            iconTint = 0xFF181C1B.toInt(),
                            sizePx = 64
                        )
                        views.setImageViewBitmap(CAT_ICON_IDS[i], iconBitmap)

                        val catIntent = WidgetUpdateHelper.createQuickAddIntent(context, cat.name)
                        val catPendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId * 100 + i + 50,
                            catIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(containerId, catPendingIntent)
                    } else {
                        views.setViewVisibility(containerId, View.GONE)
                    }
                }

                val recent = snapshot.recentTransactions
                if (recent.isEmpty()) {
                    views.setViewVisibility(R.id.widget_recent_empty_text, View.VISIBLE)
                    for (i in TX_ROW_IDS.indices) {
                        views.setViewVisibility(TX_ROW_IDS[i], View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.widget_recent_empty_text, View.GONE)
                    val maxRows = computeMaxTransactionRows(minHeight, maxHeight)
                    for (i in TX_ROW_IDS.indices) {
                        val rowId = TX_ROW_IDS[i]
                        if (i < maxRows && i < recent.size) {
                            val tx = recent[i]
                            views.setViewVisibility(rowId, View.VISIBLE)

                            val resolvedCat = DefaultCategories.getCategoryByName(
                                tx.category,
                                snapshot.topCategories
                            )
                            val iconBitmap = WidgetIconRenderer.renderCategoryIconBitmap(
                                icon = resolvedCat.icon,
                                iconTint = 0xFF181C1B.toInt(),
                                sizePx = 48
                            )
                            views.setImageViewBitmap(TX_ICON_IDS[i], iconBitmap)

                            val displayTitle = tx.title.ifBlank { tx.category }
                            views.setTextViewText(TX_TITLE_IDS[i], displayTitle)
                            views.setTextViewText(
                                TX_DATE_IDS[i],
                                WidgetUpdateHelper.formatTransactionDate(tx.timestamp)
                            )
                            views.setTextViewText(
                                TX_AMOUNT_IDS[i],
                                WidgetUpdateHelper.formatTransactionAmount(tx)
                            )
                            val amountColor = if (tx.isExpense) {
                                0xFF8A3C30.toInt() // CalmTerracotta
                            } else {
                                0xFF1E5B29.toInt() // CalmPositiveDeep
                            }
                            views.setTextColor(TX_AMOUNT_IDS[i], amountColor)
                            views.setOnClickPendingIntent(rowId, openAppPendingIntent)
                        } else {
                            views.setViewVisibility(rowId, View.GONE)
                        }
                    }
                }
            } else {
                views.setViewVisibility(R.id.widget_expanded_section, View.GONE)
            }

            return views
        }
    }
}

package com.arango.base.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.arango.base.MainActivity
import com.arango.base.R
import com.arango.base.data.local.AppDatabase
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
        private val CAT_LABEL_IDS = intArrayOf(
            R.id.widget_cat_label_1,
            R.id.widget_cat_label_2,
            R.id.widget_cat_label_3,
            R.id.widget_cat_label_4
        )

        fun buildRemoteViews(
            context: Context,
            appWidgetId: Int,
            options: Bundle?,
            snapshot: WidgetDataSnapshot
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_balance_quick_add)

            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
                ?.takeIf { it > 0 } ?: 180
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 60)
                ?.takeIf { it > 0 } ?: 60

            // 1. Balance text & color
            views.setTextViewText(R.id.widget_balance_amount, snapshot.formattedBalance)
            val balanceColor = if (!snapshot.isBalanceMasked && snapshot.rollingBalance < -0.004) {
                0xFF8F2D20.toInt() // CalmNegativeDeep
            } else {
                0xFF181C1B.toInt() // CalmOnSurface
            }
            views.setTextColor(R.id.widget_balance_amount, balanceColor)

            // 2. Eye toggle icon & broadcast intent
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

            // 3. Balance container click -> open MainActivity (LedgerScreen)
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

            // 4. Primary "+ Add" button -> open QuickAddActivity
            val defaultCatName = snapshot.topCategories.firstOrNull()?.name
            val quickAddIntent = WidgetUpdateHelper.createQuickAddIntent(context, defaultCatName)
            val quickAddPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 3,
                quickAddIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_add_btn, quickAddPendingIntent)

            // 5. Responsive adjustments for width & height
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

            // Expanded category shortcuts row when height >= 100dp
            if (minHeight >= 100 && snapshot.topCategories.isNotEmpty()) {
                views.setViewVisibility(R.id.widget_expanded_section, View.VISIBLE)
                val maxVisibleCats = if (minWidth >= 250) 4 else 3
                for (i in 0 until 4) {
                    val containerId = CAT_CONTAINER_IDS[i]
                    if (i < maxVisibleCats && i < snapshot.topCategories.size) {
                        val cat = snapshot.topCategories[i]
                        views.setViewVisibility(containerId, View.VISIBLE)
                        views.setTextViewText(CAT_LABEL_IDS[i], cat.name)
                        val badgeBitmap = WidgetIconRenderer.renderCategoryBadgeBitmap(
                            icon = cat.icon,
                            badgeColor = cat.color,
                            sizePx = 56
                        )
                        views.setImageViewBitmap(CAT_ICON_IDS[i], badgeBitmap)

                        val catIntent = WidgetUpdateHelper.createQuickAddIntent(context, cat.name)
                        val catPendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId * 100 + 10 + i,
                            catIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(containerId, catPendingIntent)
                    } else {
                        views.setViewVisibility(containerId, View.GONE)
                    }
                }
            } else {
                views.setViewVisibility(R.id.widget_expanded_section, View.GONE)
            }

            return views
        }
    }
}

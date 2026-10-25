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

class CategoryShortcutsWidgetProvider : AppWidgetProvider() {

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
            val views = RemoteViews(context.packageName, R.layout.widget_category_shortcuts)

            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                ?.takeIf { it > 0 } ?: 250
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 60)
                ?.takeIf { it > 0 } ?: 60

            // 1. Primary "+ Add" button -> open QuickAddActivity
            val defaultCatName = snapshot.topCategories.firstOrNull()?.name
            val quickAddIntent = WidgetUpdateHelper.createQuickAddIntent(context, defaultCatName)
            val quickAddPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 1,
                quickAddIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_add_btn, quickAddPendingIntent)
            views.setViewVisibility(
                R.id.widget_add_btn_label,
                if (minWidth >= 210) View.VISIBLE else View.GONE
            )

            // 2. Top category shortcuts ordered by CategoryManager displayOrder
            val maxVisibleCats = when {
                minWidth < 170 -> 2
                minWidth < 230 -> 3
                else -> 4
            }

            for (i in 0 until 4) {
                val containerId = CAT_CONTAINER_IDS[i]
                if (i < maxVisibleCats && i < snapshot.topCategories.size) {
                    val cat = snapshot.topCategories[i]
                    views.setViewVisibility(containerId, View.VISIBLE)
                    views.setTextViewText(CAT_LABEL_IDS[i], cat.name)
                    val badgeBitmap = WidgetIconRenderer.renderCategoryBadgeBitmap(
                        icon = cat.icon,
                        badgeColor = cat.color,
                        sizePx = 68
                    )
                    views.setImageViewBitmap(CAT_ICON_IDS[i], badgeBitmap)

                    val catIntent = WidgetUpdateHelper.createQuickAddIntent(context, cat.name)
                    val catPendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId * 100 + 20 + i,
                        catIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(containerId, catPendingIntent)
                } else {
                    views.setViewVisibility(containerId, View.GONE)
                }
            }

            // 3. Optional expanded top balance bar (when height >= 100dp); always includes eye mask toggle
            if (minHeight >= 100) {
                views.setViewVisibility(R.id.widget_shortcuts_balance_row, View.VISIBLE)
                views.setTextViewText(R.id.widget_balance_amount, snapshot.formattedBalance)
                val balanceColor = if (!snapshot.isBalanceMasked && snapshot.rollingBalance < -0.004) {
                    0xFF8F2D20.toInt()
                } else {
                    0xFF181C1B.toInt()
                }
                views.setTextColor(R.id.widget_balance_amount, balanceColor)

                val eyeDrawable = if (snapshot.isBalanceMasked) {
                    R.drawable.ic_widget_visibility_off
                } else {
                    R.drawable.ic_widget_visibility
                }
                views.setImageViewResource(R.id.widget_eye_icon, eyeDrawable)

                val toggleMaskIntent = Intent(context, CategoryShortcutsWidgetProvider::class.java).apply {
                    action = WidgetUpdateHelper.ACTION_TOGGLE_BALANCE_MASK
                }
                val toggleMaskPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 5,
                    toggleMaskIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_eye_toggle_btn, toggleMaskPendingIntent)

                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openAppPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId * 10 + 6,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_balance_container, openAppPendingIntent)
            } else {
                views.setViewVisibility(R.id.widget_shortcuts_balance_row, View.GONE)
            }

            return views
        }
    }
}

package com.arango.base.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
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

        fun buildRemoteViews(
            context: Context,
            appWidgetId: Int,
            options: Bundle?,
            snapshot: WidgetDataSnapshot
        ): RemoteViews {
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
                ?.takeIf { it > 0 } ?: 250
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 60)
                ?.takeIf { it > 0 } ?: 60

            // When expanded to 2 rows or taller (minHeight >= 100dp), both widgets use the identical
            // Balance & Quick-Add design displaying recent transactions.
            if (minHeight >= 100 && minWidth >= 110) {
                return BalanceQuickAddWidgetProvider.buildRemoteViews(
                    context = context,
                    appWidgetId = appWidgetId,
                    options = options,
                    snapshot = snapshot
                )
            }

            val views = RemoteViews(context.packageName, R.layout.widget_category_shortcuts)

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

            // 1x1 mode (minWidth < 110dp): show ONLY the quick-add button
            if (minWidth < 110) {
                views.setViewVisibility(R.id.widget_add_btn_label, View.GONE)
                for (i in 0 until 4) {
                    views.setViewVisibility(CAT_CONTAINER_IDS[i], View.GONE)
                }
                return views
            }

            views.setViewVisibility(
                R.id.widget_add_btn_label,
                if (minWidth >= 210) View.VISIBLE else View.GONE
            )

            // 2. Top category shortcuts ordered by CategoryManager displayOrder (monochrome icons only)
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
                    views.setContentDescription(containerId, cat.name)
                    views.setContentDescription(CAT_ICON_IDS[i], cat.name)
                    val iconBitmap = WidgetIconRenderer.renderCategoryIconBitmap(
                        icon = cat.icon,
                        iconTint = 0xFF181C1B.toInt(),
                        sizePx = 64
                    )
                    views.setImageViewBitmap(CAT_ICON_IDS[i], iconBitmap)

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

            return views
        }
    }
}

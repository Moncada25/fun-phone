package com.bookverse.contacts.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build

object WidgetUpdateHelper {

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, FavoritesWidgetProvider::class.java)
        )

        if (appWidgetIds.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                FavoritesWidgetUpdater.updateWidgets(context, appWidgetManager, appWidgetIds)
            } else {
                appWidgetIds.forEach { appWidgetId ->
                    @Suppress("DEPRECATION")
                    appWidgetManager.notifyAppWidgetViewDataChanged(
                        appWidgetId,
                        com.bookverse.contacts.R.id.widget_grid_view
                    )
                }
            }
        }
    }
}

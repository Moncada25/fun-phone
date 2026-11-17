package com.bookverse.contacts.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle

class FavoritesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        FavoritesWidgetUpdater.updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        FavoritesWidgetUpdater.updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, FavoritesWidgetProvider::class.java)
                )
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

    companion object {
        const val ACTION_REFRESH = "com.bookverse.contacts.widget.ACTION_REFRESH"
    }
}

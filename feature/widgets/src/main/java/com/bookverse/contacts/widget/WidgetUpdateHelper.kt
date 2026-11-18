package com.bookverse.contacts.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WidgetUpdateHelper {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun updateAllWidgets(context: Context) {
        refresh(context, FavoritesWidget())
    }

    internal fun refresh(context: Context, widget: GlanceAppWidget) {
        val appContext = context.applicationContext
        scope.launch { runCatching { widget.updateAll(appContext) } }
    }

    internal suspend fun refreshNow(context: Context, widget: GlanceAppWidget) {
        runCatching { widget.updateAll(context.applicationContext) }
    }
}

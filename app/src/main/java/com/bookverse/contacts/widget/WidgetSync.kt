package com.bookverse.contacts.widget

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidget
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.settings.AppDataStore
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
object WidgetSync {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    private val notesTables = arrayOf("notes", "checklist_items", "reminder_data")
    private val expenseTables = arrayOf("expense_transactions", "expense_budgets", "expense_categories")
    private val expensePrefs = listOf(
        booleanPreferencesKey("sensitive_sections_lock"),
        stringPreferencesKey("password_auth_method"),
        stringPreferencesKey("expense_currency"),
    )
    private val favoritesPrefs = listOf(
        stringPreferencesKey(AppSettingsViewModel.FAVORITES_SORT_KEY_NAME),
        stringPreferencesKey(AppSettingsViewModel.FAVORITES_CUSTOM_ORDER_KEY_NAME),
    )

    fun start(context: Context) {
        if (started) return
        started = true
        val appContext = context.applicationContext
        scope.launch {
            val database = AppDatabase.getInstance(appContext)
            refreshOn(appContext, database.invalidationTracker.createFlow(*notesTables).drop(1), NotesWidget())
            refreshOn(appContext, database.invalidationTracker.createFlow(*expenseTables).drop(1), ExpensesWidget())
            val prefs = AppDataStore.getDataStore(appContext).data
            refreshOn(appContext, prefs.map { p -> expensePrefs.map { p[it] } }.distinctUntilChanged().drop(1), ExpensesWidget())
            refreshOn(appContext, prefs.map { p -> favoritesPrefs.map { p[it] } }.distinctUntilChanged().drop(1), FavoritesWidget())
            observe(appContext, CallLog.Calls.CONTENT_URI, RecentCallsWidget())
            observe(appContext, ContactsContract.Contacts.CONTENT_URI, FavoritesWidget())
            delay(STARTUP_REFRESH_DELAY_MS)
            allWidgets().forEach { WidgetUpdateHelper.refreshNow(appContext, it) }
        }
    }

    private fun refreshOn(context: Context, changes: Flow<Any?>, widget: GlanceAppWidget) {
        scope.launch {
            changes.debounce(DEBOUNCE_MS).collect { WidgetUpdateHelper.refreshNow(context, widget) }
        }
    }

    private fun observe(context: Context, uri: Uri, widget: GlanceAppWidget) {
        val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                signals.tryEmit(Unit)
            }
        }
        val registered = runCatching {
            context.contentResolver.registerContentObserver(uri, true, observer)
        }.isSuccess
        if (registered) refreshOn(context, signals, widget)
    }

    private fun allWidgets(): List<GlanceAppWidget> =
        listOf(FavoritesWidget(), NotesWidget(), RecentCallsWidget(), ExpensesWidget())

    private const val DEBOUNCE_MS = 750L
    private const val STARTUP_REFRESH_DELAY_MS = 2_000L
}

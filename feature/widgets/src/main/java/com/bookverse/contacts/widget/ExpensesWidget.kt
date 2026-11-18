package com.bookverse.contacts.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.expenses.ExpensesRepository
import com.bookverse.contacts.domain.expenses.BudgetState
import com.bookverse.contacts.domain.expenses.ExpenseCurrencyProvider
import com.bookverse.contacts.domain.expenses.MonthBudgetSummary
import com.bookverse.contacts.domain.expenses.buildMonthBudgetSummary
import com.bookverse.contacts.domain.expenses.transactionsForMonth
import com.bookverse.contacts.feature.widgets.R
import com.bookverse.contacts.settings.AppDataStore
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.navigation.DynamicShortcuts
import com.bookverse.contacts.ui.navigation.HomeShortcutDestination
import com.bookverse.contacts.ui.theme.BudgetOkColor
import com.bookverse.contacts.ui.theme.BudgetWarningColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Currency
import com.bookverse.contacts.core.ui.R as CoreUiR

internal sealed interface ExpensesWidgetState {
    data object Locked : ExpensesWidgetState

    @Immutable
    data class Summary(
        val spent: String,
        val income: String,
        val budgetLimit: String,
        val budgetSpent: String,
        val summary: MonthBudgetSummary,
    ) : ExpensesWidgetState
}

internal object ExpensesWidgetDataSource {
    private val sensitiveLockKey = booleanPreferencesKey("sensitive_sections_lock")
    private val authMethodKey = stringPreferencesKey("password_auth_method")
    private val currencyKey = stringPreferencesKey("expense_currency")

    suspend fun load(context: Context): ExpensesWidgetState = withContext(Dispatchers.IO) {
        val prefs = runCatching { AppDataStore.getDataStore(context).data.first() }.getOrNull()
        val locked = prefs?.get(sensitiveLockKey) == true && prefs[authMethodKey] != null
        if (locked) return@withContext ExpensesWidgetState.Locked
        val locale = LocaleController.effectiveLocale(context)
        val currency = prefs?.get(currencyKey)?.let { ExpenseCurrencyProvider.fromCode(it, locale) }
            ?: ExpenseCurrencyProvider.defaultFor(context)
        val store = ExpensesRepository(AppDatabase.getInstance(context).expenseDao(), context.applicationContext)
        val month = transactionsForMonth(store.transactions.first(), YearMonth.now())
        val summary = buildMonthBudgetSummary(month, store.budgetsWithCategories.first())
        val digits = currency.fractionDigits.coerceAtLeast(0)
        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            runCatching { this.currency = Currency.getInstance(currency.code) }
        }
        val format: (Double) -> String = { amount ->
            formatter.maximumFractionDigits = digits
            formatter.minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else digits
            formatter.format(amount)
        }
        ExpensesWidgetState.Summary(
            spent = format(summary.totals.expenses),
            income = format(summary.totals.income),
            budgetLimit = format(summary.budgetLimit),
            budgetSpent = format(summary.budgetSpent),
            summary = summary,
        )
    }
}

class ExpensesWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = ExpensesWidgetDataSource.load(context)
        val strings = context.widgetStrings()
        provideContent {
            FunPhoneWidgetTheme {
                ExpensesContent(context, strings, state)
            }
        }
    }
}

@Composable
private fun ExpensesContent(context: Context, strings: Context, state: ExpensesWidgetState) {
    val openExpenses = actionStartActivity(DynamicShortcuts.launchIntent(context, HomeShortcutDestination.EXPENSES))
    WidgetSurface(modifier = GlanceModifier.clickable(openExpenses)) {
        WidgetHeader(title = strings.getString(CoreUiR.string.expenses_widget_title), onTitleClick = openExpenses)
        when (state) {
            ExpensesWidgetState.Locked -> WidgetMessage(
                strings.getString(R.string.expenses_widget_locked),
                openExpenses,
                CoreUiR.drawable.ic_widget_lock,
            )
            is ExpensesWidgetState.Summary -> SummaryBody(strings, state)
        }
    }
}

@Composable
private fun SummaryBody(strings: Context, state: ExpensesWidgetState.Summary) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Spacer(GlanceModifier.height(4.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            AmountBlock(state.spent, strings.getString(CoreUiR.string.expenses_widget_spent), GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(8.dp))
            AmountBlock(state.income, strings.getString(R.string.expenses_widget_income), GlanceModifier.defaultWeight())
        }
        Spacer(GlanceModifier.height(14.dp))
        val summary = state.summary
        if (summary.hasBudget) {
            val color = budgetColor(summary.state)
            LinearProgressIndicator(
                progress = summary.progress,
                modifier = GlanceModifier.fillMaxWidth().height(8.dp),
                color = color,
                backgroundColor = GlanceTheme.colors.surfaceVariant,
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = strings.getString(R.string.expenses_widget_budget, state.budgetSpent, state.budgetLimit),
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
            )
        } else {
            Text(
                text = strings.getString(R.string.expenses_widget_no_budget),
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
            )
        }
    }
}

@Composable
private fun AmountBlock(amount: String, label: String, modifier: GlanceModifier) {
    Column(modifier = modifier) {
        Text(
            text = amount,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp),
        )
        Text(
            text = label,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
        )
    }
}

@Composable
private fun budgetColor(state: BudgetState): ColorProvider = when (state) {
    BudgetState.OK -> ColorProvider(day = BudgetOkColor, night = BudgetOkColor)
    BudgetState.WARNING -> ColorProvider(day = BudgetWarningColor, night = BudgetWarningColor)
    BudgetState.EXCEEDED -> GlanceTheme.colors.error
}

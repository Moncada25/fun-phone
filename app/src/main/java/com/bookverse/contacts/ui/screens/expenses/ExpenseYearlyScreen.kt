package com.bookverse.contacts.ui.screens.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.R
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import com.bookverse.contacts.domain.expenses.MonthSummary
import com.bookverse.contacts.domain.expenses.buildYearlySummary
import com.bookverse.contacts.domain.expenses.formatAsCurrency
import com.bookverse.contacts.settings.LocaleController
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ExpenseYearlyScreen(
    transactions: List<ExpenseTransaction>,
    initialYear: Int,
    accentColor: Color,
    currency: ExpenseCurrency,
    modifier: Modifier = Modifier,
) {
    var year by rememberSaveable { mutableIntStateOf(initialYear) }
    val summary = remember(transactions, year) { buildYearlySummary(transactions, year) }
    val locale = LocaleController.effectiveLocale(LocalContext.current)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "expense_year_selector") {
            ExpenseYearSelector(
                year = year,
                accentColor = accentColor,
                onPrev = { year -= 1 },
                onNext = { year += 1 },
            )
        }
        if (summary.isEmpty) {
            item(key = "expense_year_empty") {
                ExpenseEmptyState(message = stringResource(R.string.expense_yearly_empty, year.toString()))
            }
        } else {
            item(key = "expense_year_totals") {
                ExpenseTotalsCard(
                    totals = summary.totals,
                    accentColor = accentColor,
                    currency = currency,
                )
            }
            item(key = "expense_year_months") {
                YearlyMonthsCard(
                    months = summary.months,
                    accentColor = accentColor,
                    currency = currency,
                    locale = locale,
                )
            }
            item(key = "expense_year_categories") {
                ExpenseDistributionCard(
                    shares = summary.categories,
                    accentColor = accentColor,
                    currency = currency,
                    transactions = summary.transactions,
                )
            }
        }
    }
}

@Composable
private fun YearlyMonthsCard(
    months: List<MonthSummary>,
    accentColor: Color,
    currency: ExpenseCurrency,
    locale: Locale,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpenseCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.expense_yearly_months_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ExpenseMonthlyBarChart(
                months = months,
                accentColor = accentColor,
            )
            Column {
                months.forEachIndexed { index, month ->
                    YearlyMonthRow(
                        summary = month,
                        accentColor = accentColor,
                        currency = currency,
                        locale = locale,
                    )
                    if (index < months.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearlyMonthRow(
    summary: MonthSummary,
    accentColor: Color,
    currency: ExpenseCurrency,
    locale: Locale,
) {
    val totals = summary.totals
    val balanceColor = if (totals.balance >= 0) accentColor else MaterialTheme.colorScheme.error
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = summary.month.month.getDisplayName(TextStyle.FULL, locale)
                    .replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                    },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = totals.balance.formatAsCurrency(currency, locale),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = balanceColor,
                textAlign = TextAlign.End,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.expense_yearly_month_income,
                    totals.income.formatAsCurrency(currency, locale),
                ),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
            )
            Text(
                text = stringResource(
                    R.string.expense_yearly_month_expense,
                    totals.expenses.formatAsCurrency(currency, locale),
                ),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.End,
            )
        }
    }
}

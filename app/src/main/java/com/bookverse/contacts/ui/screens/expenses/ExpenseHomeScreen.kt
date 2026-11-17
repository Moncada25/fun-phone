package com.bookverse.contacts.ui.screens.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.bookverse.contacts.R
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.domain.expenses.CategoryShare
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import com.bookverse.contacts.domain.expenses.ExpenseOverview
import com.bookverse.contacts.domain.expenses.MonthlyTotals
import com.bookverse.contacts.domain.expenses.formatAsCurrency
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.CountBadge
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.composables.icons.lucide.Coins
import com.composables.icons.lucide.Lucide
import java.time.YearMonth

@Composable
fun ExpenseHomeScreen(
    overview: ExpenseOverview,
    transactions: List<ExpenseTransaction>,
    accentColor: Color,
    currency: ExpenseCurrency,
    month: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEditTransaction: (ExpenseTransaction) -> Unit,
    onDeleteTransaction: (ExpenseTransaction) -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    modifier: Modifier = Modifier,
    topContentPadding: Dp = 0.dp,
) {
    var transactionPendingDelete by remember { mutableStateOf<ExpenseTransaction?>(null) }
    val listState = rememberLazyListState()
    var displayedCount by remember { mutableIntStateOf(20) }

    val displayedTransactions by remember(transactions, displayedCount) {
        derivedStateOf {
            transactions.take(displayedCount.coerceAtMost(transactions.size))
        }
    }

    LaunchedEffect(listState, transactions) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            totalItems to lastVisible
        }
            .collect { (totalItems, lastVisible) ->
                val shouldLoadMore = totalItems > 0 && lastVisible >= totalItems - 3
                if (shouldLoadMore && displayedCount < transactions.size) {
                    displayedCount = (displayedCount + 20).coerceAtMost(transactions.size)
                }
            }
    }

    LaunchedEffect(transactions) {
        displayedCount = 20.coerceAtMost(transactions.size)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topContentPadding + 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item(key = "expense_month_selector") {
            Column {
                ExpenseMonthSelector(
                    month = month,
                    accentColor = accentColor,
                    onPrev = onPrevMonth,
                    onNext = onNextMonth
                )
                Spacer(Modifier.height(16.dp))
            }
        }
        item(key = "expense_dashboard") {
            Column {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val expanded = maxWidth >= 840.dp
                    if (expanded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            ExpenseTotalsCard(
                                totals = overview.totals,
                                accentColor = accentColor,
                                currency = currency,
                                modifier = Modifier.weight(0.9f)
                            )
                            ExpenseDistributionCard(
                                shares = overview.distribution,
                                accentColor = accentColor,
                                currency = currency,
                                transactions = transactions,
                                modifier = Modifier.weight(1.1f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ExpenseTotalsCard(
                                totals = overview.totals,
                                accentColor = accentColor,
                                currency = currency
                            )
                            ExpenseDistributionCard(
                                shares = overview.distribution,
                                accentColor = accentColor,
                                currency = currency,
                                transactions = transactions
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        item(key = "expense_transactions_header") {
            Column {
                TransactionsHeader(
                    count = transactions.size,
                    accentColor = accentColor
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        if (transactions.isEmpty()) {
            item(key = "expense_transactions_empty") {
                ExpenseEmptyState(message = stringResource(R.string.expense_transactions_empty))
            }
        } else {
            itemsIndexed(
                items = displayedTransactions,
                key = { _, transaction -> transaction.id }
            ) { index, transaction ->
                ExpenseTransactionCard(
                    transaction = transaction,
                    accentColor = accentColor,
                    currency = currency,
                    shape = expenseGroupedRowShape(index, displayedTransactions.lastIndex),
                    showDivider = index < displayedTransactions.lastIndex,
                    onEdit = onEditTransaction,
                    onDelete = { transactionPendingDelete = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item(key = "expense_home_bottom_spacing") {
            Spacer(Modifier.height(16.dp))
        }
    }

    val deleteTarget = transactionPendingDelete
    if (deleteTarget != null) {
        ConfirmDialog(
            onDismiss = { transactionPendingDelete = null },
            onConfirm = {
                onDeleteTransaction(deleteTarget)
                transactionPendingDelete = null
            },
            title = stringResource(R.string.expense_delete_transaction),
            message = stringResource(R.string.expense_delete_transaction_confirm),
            appSettingsViewModel = appSettingsViewModel
        )
    }
}

@Composable
internal fun ExpenseTotalsCard(
    totals: MonthlyTotals,
    accentColor: Color,
    currency: ExpenseCurrency,
    modifier: Modifier = Modifier
) {
    val balanceColor = if (totals.balance >= 0) {
        accentColor
    } else {
        MaterialTheme.colorScheme.error
    }
    val accentContentColor = contentColorFor(accentColor)

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = ExpenseCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = accentColor,
                    contentColor = accentContentColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Lucide.Coins, contentDescription = null)
                    }
                }
                Text(
                    text = stringResource(R.string.expense_home_summary_title, currency.code),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = accentColor.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.expense_summary_balance),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                    Text(
                        text = totals.balance.formatAsCurrency(
                            currency,
                            LocaleController.effectiveLocale(LocalContext.current)
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStat(
                    title = stringResource(R.string.expense_summary_income),
                    amount = totals.income,
                    currency = currency,
                    amountColor = accentColor,
                    modifier = Modifier.weight(1f)
                )
                SummaryStat(
                    title = stringResource(R.string.expense_summary_expense),
                    amount = totals.expenses,
                    currency = currency,
                    amountColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(
    title: String,
    amount: Double,
    currency: ExpenseCurrency,
    amountColor: Color,
    modifier: Modifier = Modifier
) {
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = amountColor.copy(alpha = 0.1f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = amount.formatAsCurrency(currency, locale),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun ExpenseDistributionCard(
    shares: List<CategoryShare>,
    accentColor: Color,
    currency: ExpenseCurrency,
    transactions: List<ExpenseTransaction>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = ExpenseCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.expense_distribution_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            ExpensePieChart(
                shares = shares,
                accentColor = accentColor,
                currency = currency,
                transactions = transactions,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TransactionsHeader(
    count: Int,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.expense_recent_transactions_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        CountBadge(
            count = count,
            accent = accentColor.copy(alpha = 0.16f),
            content = accentColor,
            isMinimal = true
        )
    }
}

package com.bookverse.contacts.ui.screens.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import com.bookverse.contacts.domain.expenses.ExpenseTransactionFilter
import com.bookverse.contacts.domain.expenses.filterTransactions
import com.bookverse.contacts.domain.expenses.formatAsCurrency
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.components.ButtonSheetScaffold
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.DatePickerDialog
import com.bookverse.contacts.ui.components.SelectionButton
import com.bookverse.contacts.ui.components.TextField
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class FilterDateBound {
    FROM,
    TO
}

@Composable
fun ExpenseSearchScreen(
    transactions: List<ExpenseTransaction>,
    categories: List<ExpenseCategory>,
    filter: ExpenseTransactionFilter,
    onFilterChange: (ExpenseTransactionFilter) -> Unit,
    onClearFilters: () -> Unit,
    accentColor: Color,
    currency: ExpenseCurrency,
    onEditTransaction: (ExpenseTransaction) -> Unit,
    onDeleteTransaction: (ExpenseTransaction) -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var transactionPendingDelete by remember { mutableStateOf<ExpenseTransaction?>(null) }
    val results = remember(transactions, filter) {
        filterTransactions(transactions, filter).sortedWith(
            compareByDescending<ExpenseTransaction> { it.date }.thenByDescending { it.createdAt }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
    ) {
        item(key = "expense_search_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterSummaryBar(
                    filter = filter,
                    resultCount = results.size,
                    accentColor = accentColor,
                    onOpenFilters = { showFilterSheet = true },
                    onClearFilters = onClearFilters,
                )
                if (filter.isActive) {
                    ActiveFilterChips(
                        filter = filter,
                        categories = categories,
                        currency = currency,
                        onFilterChange = onFilterChange,
                    )
                }
                Spacer(Modifier.size(4.dp))
            }
        }
        if (results.isEmpty()) {
            item(key = "expense_search_empty") {
                ExpenseEmptyState(
                    message = stringResource(
                        if (filter.isActive) R.string.no_results else R.string.expense_search_no_transactions
                    )
                )
            }
        } else {
            itemsIndexed(
                items = results,
                key = { _, transaction -> transaction.id },
            ) { index, transaction ->
                ExpenseTransactionCard(
                    transaction = transaction,
                    accentColor = accentColor,
                    currency = currency,
                    shape = expenseGroupedRowShape(index, results.lastIndex),
                    showDivider = index < results.lastIndex,
                    onEdit = onEditTransaction,
                    onDelete = { transactionPendingDelete = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showFilterSheet) {
        ExpenseFilterSheet(
            filter = filter,
            categories = categories,
            accentColor = accentColor,
            currency = currency,
            onFilterChange = onFilterChange,
            onClearFilters = onClearFilters,
            onDismiss = { showFilterSheet = false },
        )
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
            appSettingsViewModel = appSettingsViewModel,
        )
    }
}

@Composable
private fun FilterSummaryBar(
    filter: ExpenseTransactionFilter,
    resultCount: Int,
    accentColor: Color,
    onOpenFilters: () -> Unit,
    onClearFilters: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = filter.isActive,
                onClick = onOpenFilters,
                label = {
                    Text(
                        text = if (filter.isActive) {
                            stringResource(R.string.expense_filters_active_count, filter.activeCount)
                        } else {
                            stringResource(R.string.expense_filters_title)
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            Spacer(Modifier.weight(1f))
            if (filter.isActive) {
                TextButton(
                    onClick = onClearFilters,
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
                ) {
                    Text(text = stringResource(R.string.expense_filters_clear_all))
                }
            }
        }
        Text(
            text = pluralStringResource(R.plurals.expense_search_result_count, resultCount, resultCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ActiveFilterChips(
    filter: ExpenseTransactionFilter,
    categories: List<ExpenseCategory>,
    currency: ExpenseCurrency,
    onFilterChange: (ExpenseTransactionFilter) -> Unit,
) {
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (filter.query.isNotBlank()) {
            RemovableFilterChip(
                label = stringResource(R.string.expense_filter_chip_text, filter.query.trim()),
                onRemove = { onFilterChange(filter.copy(query = "")) },
            )
        }
        filter.type?.let { type ->
            RemovableFilterChip(
                label = stringResource(type.labelRes()),
                onRemove = { onFilterChange(filter.copy(type = null)) },
            )
        }
        categories.filter { it.id in filter.categoryIds }.forEach { category ->
            RemovableFilterChip(
                label = category.localizedName(),
                onRemove = { onFilterChange(filter.copy(categoryIds = filter.categoryIds - category.id)) },
            )
        }
        filter.dateFrom?.let { date ->
            RemovableFilterChip(
                label = stringResource(R.string.expense_filter_chip_from, dateFormatter.format(date)),
                onRemove = { onFilterChange(filter.copy(dateFrom = null)) },
            )
        }
        filter.dateTo?.let { date ->
            RemovableFilterChip(
                label = stringResource(R.string.expense_filter_chip_until, dateFormatter.format(date)),
                onRemove = { onFilterChange(filter.copy(dateTo = null)) },
            )
        }
        filter.minAmount?.let { amount ->
            RemovableFilterChip(
                label = stringResource(
                    R.string.expense_filter_chip_min,
                    amount.formatAsCurrency(currency, locale),
                ),
                onRemove = { onFilterChange(filter.copy(minAmount = null)) },
            )
        }
        filter.maxAmount?.let { amount ->
            RemovableFilterChip(
                label = stringResource(
                    R.string.expense_filter_chip_max,
                    amount.formatAsCurrency(currency, locale),
                ),
                onRemove = { onFilterChange(filter.copy(maxAmount = null)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemovableFilterChip(
    label: String,
    onRemove: () -> Unit,
) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(text = label, maxLines = 1) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.expense_filter_remove),
                modifier = Modifier.size(16.dp),
            )
        },
    )
}

private fun ExpenseType.labelRes(): Int =
    if (this == ExpenseType.INCOME) R.string.expense_type_income else R.string.expense_type_expense

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFilterSheet(
    filter: ExpenseTransactionFilter,
    categories: List<ExpenseCategory>,
    accentColor: Color,
    currency: ExpenseCurrency,
    onFilterChange: (ExpenseTransactionFilter) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    val decimalSymbols = remember(locale) { DecimalFormatSymbols(locale) }
    val amountTransformation = remember(decimalSymbols) {
        AmountVisualTransformation(
            groupingSeparator = decimalSymbols.groupingSeparator,
            decimalSeparator = decimalSymbols.decimalSeparator,
        )
    }
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    var minRaw by remember(filter.minAmount == null, currency) {
        mutableStateOf(formatRawAmount(filter.minAmount, currency))
    }
    var maxRaw by remember(filter.maxAmount == null, currency) {
        mutableStateOf(formatRawAmount(filter.maxAmount, currency))
    }
    var editingBound by remember { mutableStateOf<FilterDateBound?>(null) }
    val visibleCategories = remember(categories, filter.type) {
        categories.filter { filter.type == null || it.type == filter.type }
    }
    val invalidAmountRange = filter.minAmount != null && filter.maxAmount != null &&
        filter.minAmount > filter.maxAmount
    val accentContentColor = contentColorFor(accentColor)
    val anyLabel = stringResource(R.string.expense_filter_any)

    ButtonSheetScaffold(
        title = stringResource(R.string.expense_filters_title),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilterSectionTitle(text = stringResource(R.string.expense_filter_type_label))
            val typeOptions = listOf<ExpenseType?>(null, ExpenseType.INCOME, ExpenseType.EXPENSE)
            val segmentedColors = SegmentedButtonDefaults.colors(
                activeContainerColor = accentColor,
                activeContentColor = accentContentColor,
                inactiveContainerColor = MaterialTheme.colorScheme.surface,
                inactiveContentColor = MaterialTheme.colorScheme.onSurface,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                typeOptions.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = filter.type == option,
                        onClick = {
                            val allowedIds = categories
                                .filter { option == null || it.type == option }
                                .map { it.id }
                                .toSet()
                            onFilterChange(
                                filter.copy(
                                    type = option,
                                    categoryIds = filter.categoryIds intersect allowedIds,
                                )
                            )
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, typeOptions.size),
                        colors = segmentedColors,
                        label = {
                            Text(text = stringResource(option?.labelRes() ?: R.string.all_filter))
                        },
                    )
                }
            }

            FilterSectionTitle(text = stringResource(R.string.expense_nav_categories))
            if (visibleCategories.isEmpty()) {
                Text(
                    text = stringResource(R.string.expense_no_categories_for_type),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    visibleCategories.forEach { category ->
                        val selected = category.id in filter.categoryIds
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val updated = if (selected) {
                                    filter.categoryIds - category.id
                                } else {
                                    filter.categoryIds + category.id
                                }
                                onFilterChange(filter.copy(categoryIds = updated))
                            },
                            label = { Text(text = category.localizedName(), maxLines = 1) },
                        )
                    }
                }
            }

            FilterSectionTitle(text = stringResource(R.string.expense_filter_date_range_label))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SelectionButton(
                    label = stringResource(R.string.expense_filter_date_from),
                    value = filter.dateFrom?.let(dateFormatter::format) ?: anyLabel,
                    onClick = { editingBound = FilterDateBound.FROM },
                    modifier = Modifier.weight(1f),
                )
                SelectionButton(
                    label = stringResource(R.string.expense_filter_date_to),
                    value = filter.dateTo?.let(dateFormatter::format) ?: anyLabel,
                    onClick = { editingBound = FilterDateBound.TO },
                    modifier = Modifier.weight(1f),
                )
            }
            if (filter.dateFrom != null || filter.dateTo != null) {
                TextButton(
                    onClick = { onFilterChange(filter.copy(dateFrom = null, dateTo = null)) },
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
                ) {
                    Text(text = stringResource(R.string.expense_filter_clear_dates))
                }
            }

            FilterSectionTitle(
                text = stringResource(R.string.expense_filter_amount_range_label, currency.code)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextField(
                    value = minRaw,
                    onValueChange = { input ->
                        minRaw = sanitizeAmountInput(
                            input = input,
                            fractionDigits = currency.fractionDigits,
                            groupingSeparator = decimalSymbols.groupingSeparator,
                            decimalSeparator = decimalSymbols.decimalSeparator,
                        )
                        onFilterChange(filter.copy(minAmount = parseAmountInput(minRaw, currency)))
                    },
                    label = stringResource(R.string.expense_filter_amount_min),
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.weight(1f),
                    isError = invalidAmountRange,
                    visualTransformation = amountTransformation,
                )
                TextField(
                    value = maxRaw,
                    onValueChange = { input ->
                        maxRaw = sanitizeAmountInput(
                            input = input,
                            fractionDigits = currency.fractionDigits,
                            groupingSeparator = decimalSymbols.groupingSeparator,
                            decimalSeparator = decimalSymbols.decimalSeparator,
                        )
                        onFilterChange(filter.copy(maxAmount = parseAmountInput(maxRaw, currency)))
                    },
                    label = stringResource(R.string.expense_filter_amount_max),
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.weight(1f),
                    isError = invalidAmountRange,
                    visualTransformation = amountTransformation,
                )
            }
            if (invalidAmountRange) {
                Text(
                    text = stringResource(R.string.expense_filter_amount_range_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onClearFilters,
                    enabled = filter.isActive,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = if (filter.isActive) 1f else 0.3f)),
                ) {
                    Text(text = stringResource(R.string.expense_filters_clear_all))
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = accentContentColor,
                    ),
                ) {
                    Text(text = stringResource(R.string.apply), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    val bound = editingBound
    if (bound != null) {
        val zone = ZoneId.systemDefault()
        val current = when (bound) {
            FilterDateBound.FROM -> filter.dateFrom
            FilterDateBound.TO -> filter.dateTo
        } ?: LocalDate.now()
        DatePickerDialog(
            selectedDate = current.atStartOfDay(zone).toInstant().toEpochMilli(),
            onDateSelected = { millis ->
                val picked = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
                onFilterChange(
                    when (bound) {
                        FilterDateBound.FROM -> filter.copy(
                            dateFrom = picked,
                            dateTo = filter.dateTo?.takeIf { !it.isBefore(picked) },
                        )
                        FilterDateBound.TO -> filter.copy(
                            dateFrom = filter.dateFrom?.takeIf { !it.isAfter(picked) },
                            dateTo = picked,
                        )
                    }
                )
            },
            onDismiss = { editingBound = null },
        )
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

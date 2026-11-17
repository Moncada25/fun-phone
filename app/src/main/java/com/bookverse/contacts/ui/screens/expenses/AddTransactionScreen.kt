package com.bookverse.contacts.ui.screens.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.R
import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.DatePickerDialog
import com.bookverse.contacts.ui.components.SelectBottomSheet
import com.bookverse.contacts.ui.components.SelectionButton
import com.bookverse.contacts.ui.components.TextField
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.composables.icons.lucide.Coins
import com.composables.icons.lucide.Lucide
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transaction: ExpenseTransaction?,
    categories: List<ExpenseCategory>,
    accentColor: Color,
    currency: ExpenseCurrency,
    isFixedMode: Boolean = false,
    onSave: (ExpenseType, Double, ExpenseCategory, LocalDate, String?, Boolean) -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel
) {
    var type by remember(transaction) { mutableStateOf(transaction?.type ?: ExpenseType.EXPENSE) }
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) {
        runCatching { configuration.locales[0] }.getOrElse { Locale.ENGLISH }
    }
    val decimalSymbols = remember(locale) { DecimalFormatSymbols(locale) }
    var amountRaw by remember(transaction, currency) {
        mutableStateOf(formatRawAmount(transaction?.amount, currency))
    }
    val amountVisualTransformation = remember(currency, decimalSymbols) {
        AmountVisualTransformation(
            groupingSeparator = decimalSymbols.groupingSeparator,
            decimalSeparator = decimalSymbols.decimalSeparator
        )
    }
    var selectedCategory by remember(transaction, categories) {
        mutableStateOf(
            categories.firstOrNull { it.id == transaction?.categoryId }
        )
    }
    var note by remember(transaction) { mutableStateOf(transaction?.note ?: "") }
    var date by remember(transaction) { mutableStateOf(transaction?.date ?: LocalDate.now()) }
    val sensitiveLock by appSettingsViewModel.sensitiveSectionsLockEnabled.collectAsStateWithLifecycle()
    val protectedDraft = if (sensitiveLock) com.bookverse.contacts.ui.auth.encryptedDraft("expense-draft-${transaction?.id}", appSettingsViewModel) else null
    com.bookverse.contacts.ui.auth.BindProtectedDraft(
        draft = protectedDraft,
        snapshot = { mapOf("type" to type.name, "amount" to amountRaw, "note" to note, "date" to date.toString(), "category" to selectedCategory?.id.toString()) },
        restore = { fields ->
            type = ExpenseType.entries.firstOrNull { it.name == fields["type"] } ?: type
            amountRaw = fields["amount"].orEmpty()
            note = fields["note"].orEmpty()
            date = runCatching { LocalDate.parse(fields["date"]) }.getOrDefault(date)
            selectedCategory = categories.firstOrNull { it.id == fields["category"]?.toLongOrNull() }
        },
    )
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val filteredCategories = remember(categories, type) {
        categories.filter { it.type == type }
    }
    LaunchedEffect(type, categories) {
        if (selectedCategory == null || selectedCategory?.type != type) {
            selectedCategory = filteredCategories.firstOrNull()
        }
    }
    val amountValue = remember(amountRaw, currency) { parseAmountInput(amountRaw, currency) }
    val canSave =
        amountValue != null && amountValue > 0 && selectedCategory != null && filteredCategories.isNotEmpty()
    val backgroundColor = MaterialTheme.colorScheme.background
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("MMM dd, yyyy", locale)
    }
    val formattedDate = remember(date, dateFormatter) {
        val raw = dateFormatter.format(date)
        raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val showDeleteButton = transaction != null && onDelete != null
    val accentContentColor = contentColorFor(accentColor)

    fun handleSave() {
        val amountDouble = amountValue ?: return
        val category = selectedCategory ?: return
        protectedDraft?.clear()
        onSave(type, amountDouble, category, date, note.takeIf { it.isNotBlank() }, isFixedMode)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 24.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "transaction_header") {
                val title = if (transaction == null) {
                    stringResource(R.string.expense_add_transaction_title)
                } else {
                    stringResource(R.string.expense_edit_transaction_title)
                }
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    shape = ExpenseCardShape,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Lucide.Coins,
                                        contentDescription = null,
                                    )
                                }
                            }
                            Text(
                                text = title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (isFixedMode) {
                                RecurringTransactionBadge(accentColor = accentColor)
                            }
                        }
                        if (isFixedMode) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ) {
                                Text(
                                    text = stringResource(R.string.expense_fixed_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            item(key = "transaction_fields") {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    shape = ExpenseCardShape,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        val entries = ExpenseType.entries
                        val segmentedColors = SegmentedButtonDefaults.colors(
                            activeContainerColor = accentColor,
                            activeContentColor = accentContentColor,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            entries.forEachIndexed { index, entry ->
                                SegmentedButton(
                                    selected = type == entry,
                                    onClick = { type = entry },
                                    shape = SegmentedButtonDefaults.itemShape(index, entries.size),
                                    colors = segmentedColors,
                                    label = {
                                        Text(
                                            text = if (entry == ExpenseType.INCOME) {
                                                stringResource(R.string.expense_type_income)
                                            } else {
                                                stringResource(R.string.expense_type_expense)
                                            },
                                        )
                                    },
                                )
                            }
                        }

                        if (filteredCategories.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ) {
                                Text(
                                    text = stringResource(R.string.expense_no_categories_for_type),
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                )
                            }
                            SelectionButton(
                                label = stringResource(R.string.expense_date_label),
                                value = formattedDate,
                                onClick = { showDatePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                if (maxWidth < 600.dp) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        SelectionButton(
                                            label = stringResource(R.string.expense_category_label),
                                            value = selectedCategory?.localizedName()
                                                ?: stringResource(R.string.expense_category_placeholder),
                                            onClick = { showCategoryDialog = true },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        SelectionButton(
                                            label = stringResource(R.string.expense_date_label),
                                            value = formattedDate,
                                            onClick = { showDatePicker = true },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        SelectionButton(
                                            label = stringResource(R.string.expense_category_label),
                                            value = selectedCategory?.localizedName()
                                                ?: stringResource(R.string.expense_category_placeholder),
                                            onClick = { showCategoryDialog = true },
                                            modifier = Modifier.weight(1f),
                                        )
                                        SelectionButton(
                                            label = stringResource(R.string.expense_date_label),
                                            value = formattedDate,
                                            onClick = { showDatePicker = true },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }

                        TextField(
                            value = amountRaw,
                            onValueChange = { input ->
                                amountRaw = sanitizeAmountInput(
                                    input = input,
                                    fractionDigits = currency.fractionDigits,
                                    groupingSeparator = decimalSymbols.groupingSeparator,
                                    decimalSeparator = decimalSymbols.decimalSeparator,
                                )
                            },
                            label = stringResource(R.string.expense_amount_label),
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = amountVisualTransformation,
                        )

                        TextField(
                            value = note,
                            onValueChange = { note = it },
                            label = stringResource(R.string.expense_note_label),
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = accentColor,
                                ),
                                border = BorderStroke(1.dp, accentColor),
                            ) {
                                Text(text = stringResource(R.string.cancel))
                            }
                            Button(
                                onClick = ::handleSave,
                                enabled = canSave,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor,
                                    contentColor = accentContentColor,
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.save),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        if (showDeleteButton) {
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            ) {
                                Text(text = stringResource(R.string.expense_delete_transaction))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog && filteredCategories.isNotEmpty()) {
        SelectBottomSheet(
            onDismiss = { showCategoryDialog = false },
            title = stringResource(R.string.expense_category_picker_title),
            options = filteredCategories,
            selectedOption = selectedCategory,
            onSelect = { selectedCategory = it },
            optionKey = { category -> category.id },
            optionLabel = { it.localizedName() },
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            onDateSelected = { millis ->
                date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showDeleteDialog && onDelete != null) {
        ConfirmDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            title = stringResource(R.string.expense_delete_transaction),
            message = stringResource(R.string.expense_delete_transaction_confirm),
            appSettingsViewModel = appSettingsViewModel,
        )
    }
}

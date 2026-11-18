package com.bookverse.contacts.ui.screens.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.expenses.ExpenseBudget
import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.domain.expenses.BudgetStatus
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.CountBadge
import com.bookverse.contacts.ui.components.SelectBottomSheet
import com.bookverse.contacts.ui.components.SelectionButton
import com.bookverse.contacts.ui.components.TextField
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PiggyBank
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun ExpenseBudgetsScreen(
    budgets: List<Pair<ExpenseBudget, ExpenseCategory?>>,
    statuses: List<BudgetStatus>,
    categories: List<ExpenseCategory>,
    accentColor: Color,
    currency: ExpenseCurrency,
    onSaveBudget: (ExpenseBudget) -> Unit,
    onDeleteBudget: (ExpenseBudget) -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editingBudget by remember {
        mutableStateOf<Pair<ExpenseBudget?, ExpenseCategory?>>(null to null)
    }
    var budgetToDelete by remember { mutableStateOf<ExpenseBudget?>(null) }
    val statusMap = remember(statuses) { statuses.associateBy { it.budget.id } }

    val expenseCategories =
        remember(categories) { categories.filter { it.type == ExpenseType.EXPENSE } }
    val usedCategoryIds = remember(budgets) { budgets.map { it.first.categoryId }.toSet() }
    val availableExpenseCategories = remember(expenseCategories, usedCategoryIds) {
        expenseCategories.filter { it.id !in usedCategoryIds }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BudgetsHeader(
            count = budgets.size,
            canCreate = availableExpenseCategories.isNotEmpty(),
            description = when {
                expenseCategories.isEmpty() -> {
                    stringResourceSafe(R.string.expense_budget_no_categories)
                }
                availableExpenseCategories.isEmpty() -> {
                    stringResourceSafe(R.string.expense_budget_all_categories_used)
                }
                else -> stringResourceSafe(R.string.expense_budgets_description)
            },
            accentColor = accentColor,
            onCreate = {
                editingBudget = null to null
                showDialog = true
            },
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (budgets.isEmpty()) {
                item {
                    ExpenseEmptyState(
                        message = stringResourceSafe(R.string.expense_budgets_empty)
                    )
                }
            } else {
                itemsIndexed(
                    items = budgets,
                    key = { _, (budget, _) -> budget.id },
                ) { index, (budget, category) ->
                    val status = statusMap[budget.id]
                    BudgetStatusCard(
                        status = status ?: BudgetStatus(
                            budget = budget,
                            category = category,
                            spent = 0.0,
                            progress = 0f,
                            state = com.bookverse.contacts.domain.expenses.BudgetState.OK
                        ),
                        currency = currency,
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = accentColor,
                        onEdit = {
                            editingBudget = budget to category
                            showDialog = true
                        },
                        onDelete = { budgetToDelete = budget },
                        shape = expenseGroupedRowShape(index, budgets.lastIndex),
                        showDivider = index < budgets.lastIndex,
                    )
                }
            }
        }
    }

    if (showDialog) {
        val expenseCategories = categories.filter { it.type == ExpenseType.EXPENSE }
        BudgetDialog(
            budget = editingBudget.first,
            selectedCategory = editingBudget.second,
            categories = expenseCategories,
            budgets = budgets.map { it.first },
            accentColor = accentColor,
            currency = currency,
            onDismiss = { showDialog = false },
            onSave = {
                onSaveBudget(it)
                showDialog = false
            },
        )
    }

    val deleteTarget = budgetToDelete
    if (deleteTarget != null) {
        ConfirmDialog(
            onDismiss = { budgetToDelete = null },
            onConfirm = {
                onDeleteBudget(deleteTarget)
                budgetToDelete = null
            },
            title = stringResourceSafe(R.string.expense_budget_delete_title),
            message = stringResourceSafe(R.string.expense_delete_budget_confirm),
            appSettingsViewModel = appSettingsViewModel
        )
    }
}

@Composable
private fun BudgetsHeader(
    count: Int,
    canCreate: Boolean,
    description: String,
    accentColor: Color,
    onCreate: () -> Unit,
) {
    val accentContentColor = contentColorFor(accentColor)
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = accentColor,
                    contentColor = accentContentColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Lucide.PiggyBank, contentDescription = null)
                    }
                }
                Text(
                    text = stringResourceSafe(R.string.expense_budgets_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                CountBadge(
                    count = count,
                    accent = accentColor.copy(alpha = 0.16f),
                    content = accentColor,
                    isMinimal = true,
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Button(
                onClick = onCreate,
                enabled = canCreate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = accentContentColor,
                ),
            ) {
                Text(text = stringResourceSafe(R.string.expense_budget_new))
            }
        }
    }
}

@Composable
private fun BudgetDialog(
    budget: ExpenseBudget?,
    selectedCategory: ExpenseCategory?,
    categories: List<ExpenseCategory>,
    budgets: List<ExpenseBudget>,
    accentColor: Color,
    currency: ExpenseCurrency,
    onDismiss: () -> Unit,
    onSave: (ExpenseBudget) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) {
        runCatching { configuration.locales[0] }.getOrElse { Locale.ENGLISH }
    }
    val decimalSymbols = remember(locale) { DecimalFormatSymbols(locale) }
    var amountText by remember(budget) {
        mutableStateOf(formatRawAmount(budget?.limit, currency))
    }
    val amountVisualTransformation = remember(decimalSymbols) {
        AmountVisualTransformation(
            groupingSeparator = decimalSymbols.groupingSeparator,
            decimalSeparator = decimalSymbols.decimalSeparator
        )
    }
    val usedCategories = budgets
        .filter { it.id != budget?.id }
        .map { it.categoryId }
        .toSet()
    var category by remember(selectedCategory, categories) {
        mutableStateOf(
            selectedCategory ?: categories.firstOrNull { it.id !in usedCategories }
        )
    }
    var showCategoryPicker by remember { mutableStateOf(false) }
    val amountValue = remember(amountText, currency) { parseAmountInput(amountText, currency) }
    val canSave = amountValue != null && amountValue > 0 && category != null
    val accentContentColor = contentColorFor(accentColor)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = accentColor,
                        contentColor = accentContentColor,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Lucide.PiggyBank, contentDescription = null)
                        }
                    }
                    Text(
                        text = if (budget == null) {
                            stringResourceSafe(R.string.expense_budget_new)
                        } else {
                            stringResourceSafe(R.string.expense_budget_edit)
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                SelectionButton(
                    label = stringResourceSafe(R.string.expense_category_label),
                    value = category?.localizedName()
                        ?: stringResourceSafe(R.string.expense_category_placeholder),
                    onClick = { showCategoryPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = amountText,
                    onValueChange = {
                        amountText = sanitizeAmountInput(
                            input = it,
                            fractionDigits = currency.fractionDigits,
                            groupingSeparator = decimalSymbols.groupingSeparator,
                            decimalSeparator = decimalSymbols.decimalSeparator
                        )
                    },
                    label = stringResourceSafe(R.string.expense_budget_amount_label),
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = amountVisualTransformation
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                        border = BorderStroke(1.dp, accentColor)
                    ) {
                        Text(text = stringResourceSafe(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            val updated = ExpenseBudget(
                                id = budget?.id ?: 0,
                                categoryId = category?.id ?: return@Button,
                                limit = amountValue ?: 0.0,
                                createdAt = budget?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(updated)
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = accentContentColor,
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(text = stringResourceSafe(R.string.save))
                    }
                }
            }
        }
    }

    if (showCategoryPicker) {
        val availableCategories = categories.filter { it.id !in usedCategories }
        SelectBottomSheet(
            onDismiss = { showCategoryPicker = false },
            title = stringResourceSafe(R.string.expense_category_picker_title),
            options = availableCategories,
            selectedOption = category,
            onSelect = { category = it },
            optionKey = { selectedCategory -> selectedCategory.id },
            optionLabel = { it.localizedName() },
        )
    }
}

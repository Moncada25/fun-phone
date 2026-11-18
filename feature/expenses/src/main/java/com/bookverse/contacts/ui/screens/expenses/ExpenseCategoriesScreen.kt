package com.bookverse.contacts.ui.screens.expenses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import com.bookverse.contacts.feature.expenses.R
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.CountBadge
import com.bookverse.contacts.ui.components.TextField
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Tags
import com.composables.icons.lucide.TrendingDown
import com.composables.icons.lucide.TrendingUp
import com.bookverse.contacts.core.ui.R as CoreUiR

@Composable
fun ExpenseCategoriesScreen(
    categories: List<ExpenseCategory>,
    transactions: List<ExpenseTransaction>,
    accentColor: Color,
    currency: ExpenseCurrency,
    categoryUsage: Map<Long, Int>,
    linkedCategoryId: Long?,
    onLinkedCategoryChange: (Long?) -> Unit,
    onSaveCategory: (ExpenseCategory) -> Unit,
    onDeleteCategory: (ExpenseCategory, (Boolean) -> Unit) -> Unit,
    onEditTransaction: (ExpenseTransaction) -> Unit,
    onDeleteTransaction: (ExpenseTransaction) -> Unit,
    appSettingsViewModel: AppSettingsViewModel
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<ExpenseCategory?>(null) }
    var editingCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var transactionToDelete by remember { mutableStateOf<ExpenseTransaction?>(null) }
    val linkedCategory = categories.firstOrNull { it.id == linkedCategoryId }
    val linkedTransactions = remember(transactions, linkedCategoryId) {
        transactions
            .filter { it.categoryId == linkedCategoryId }
            .sortedWith(
                compareByDescending<ExpenseTransaction> { it.date }
                    .thenByDescending { it.createdAt },
            )
    }

    BackHandler(enabled = linkedCategory != null) {
        onLinkedCategoryChange(null)
    }

    if (linkedCategory != null) {
        LinkedCategoryTransactions(
            category = linkedCategory,
            transactions = linkedTransactions,
            accentColor = accentColor,
            currency = currency,
            onBack = { onLinkedCategoryChange(null) },
            onEditTransaction = onEditTransaction,
            onDeleteTransaction = { transactionToDelete = it },
            onDeleteCategory = { showDeleteDialog = linkedCategory },
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoriesHeader(
                totalCount = categories.size,
                incomeCount = categories.count { it.type == ExpenseType.INCOME },
                expenseCount = categories.count { it.type == ExpenseType.EXPENSE },
                accentColor = accentColor,
                onCreate = {
                    editingCategory = null
                    showDialog = true
                }
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (categories.isEmpty()) {
                    item {
                        ExpenseEmptyState(
                            message = stringResourceSafe(R.string.expense_categories_empty)
                        )
                    }
                } else {
                    itemsIndexed(
                        items = categories,
                        key = { _, category -> category.id },
                    ) { index, category ->
                        CategoryCard(
                            category = category,
                            accentColor = accentColor,
                            onEdit = {
                                editingCategory = category
                                showDialog = true
                            },
                            onDelete = {
                                val usage = categoryUsage[category.id] ?: 0
                                if (usage > 0) {
                                    onLinkedCategoryChange(category.id)
                                } else {
                                    showDeleteDialog = category
                                }
                            },
                            shape = expenseGroupedRowShape(index, categories.lastIndex),
                            showDivider = index < categories.lastIndex,
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }

    if (showDialog) {
        CategoryDialog(
            category = editingCategory,
            accentColor = accentColor,
            onDismiss = { showDialog = false },
            onSave = { category ->
                onSaveCategory(category)
                showDialog = false
            },
        )
    }

    val deleteTarget = showDeleteDialog
    if (deleteTarget != null) {
        ConfirmDialog(
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                onDeleteCategory(deleteTarget) { success ->
                    if (success && linkedCategoryId == deleteTarget.id) {
                        onLinkedCategoryChange(null)
                    } else if (!success) {
                        onLinkedCategoryChange(deleteTarget.id)
                    }
                    showDeleteDialog = null
                }
            },
            title = stringResourceSafe(R.string.expense_category_delete_title),
            message = stringResourceSafe(
                R.string.expense_category_delete_message,
                deleteTarget.localizedName()
            ),
            appSettingsViewModel = appSettingsViewModel
        )
    }

    val transactionDeleteTarget = transactionToDelete
    if (transactionDeleteTarget != null) {
        ConfirmDialog(
            onDismiss = { transactionToDelete = null },
            onConfirm = {
                onDeleteTransaction(transactionDeleteTarget)
                transactionToDelete = null
            },
            title = stringResourceSafe(R.string.expense_delete_transaction),
            message = stringResourceSafe(R.string.expense_delete_transaction_confirm),
            appSettingsViewModel = appSettingsViewModel,
        )
    }
}

@Composable
private fun CategoriesHeader(
    totalCount: Int,
    incomeCount: Int,
    expenseCount: Int,
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
                        Icon(imageVector = Lucide.Tags, contentDescription = null)
                    }
                }
                Text(
                    text = stringResourceSafe(R.string.expense_categories_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                CountBadge(
                    count = totalCount,
                    accent = accentColor.copy(alpha = 0.16f),
                    content = accentColor,
                    isMinimal = true,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryCountPill(
                    label = stringResourceSafe(R.string.expense_summary_income),
                    count = incomeCount,
                    color = accentColor,
                    modifier = Modifier.weight(1f),
                )
                CategoryCountPill(
                    label = stringResourceSafe(R.string.expense_summary_expense),
                    count = expenseCount,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
            }
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = accentContentColor,
                ),
            ) {
                Text(text = stringResourceSafe(R.string.expense_category_create))
            }
        }
    }
}

@Composable
private fun CategoryCountPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
private fun LinkedCategoryTransactions(
    category: ExpenseCategory,
    transactions: List<ExpenseTransaction>,
    accentColor: Color,
    currency: ExpenseCurrency,
    onBack: () -> Unit,
    onEditTransaction: (ExpenseTransaction) -> Unit,
    onDeleteTransaction: (ExpenseTransaction) -> Unit,
    onDeleteCategory: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ExpenseCardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResourceSafe(CoreUiR.string.back),
                        tint = accentColor,
                    )
                }
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = category.colorHex.toColor(accentColor).copy(alpha = 0.16f),
                    contentColor = category.colorHex.toColor(accentColor),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (category.type == ExpenseType.INCOME) {
                                Lucide.TrendingUp
                            } else {
                                Lucide.TrendingDown
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResourceSafe(
                            R.string.expense_category_linked_transactions_title,
                            category.localizedName(),
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (transactions.isNotEmpty()) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.expense_category_delete_blocked_message,
                                transactions.size,
                                category.localizedName(),
                                transactions.size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                }
                CountBadge(
                    count = transactions.size,
                    accent = accentColor.copy(alpha = 0.16f),
                    content = accentColor,
                    isMinimal = true,
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (transactions.isEmpty()) {
                item {
                    ExpenseEmptyState(
                        message = stringResourceSafe(
                            R.string.expense_category_linked_transactions_empty,
                        ),
                    )
                }
                item {
                    Button(
                        onClick = onDeleteCategory,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResourceSafe(R.string.expense_category_delete_action))
                    }
                }
            } else {
                itemsIndexed(
                    items = transactions,
                    key = { _, transaction -> transaction.id },
                ) { index, transaction ->
                    ExpenseTransactionCard(
                        transaction = transaction,
                        accentColor = accentColor,
                        currency = currency,
                        onEdit = onEditTransaction,
                        onDelete = onDeleteTransaction,
                        modifier = Modifier.fillMaxWidth(),
                        shape = expenseGroupedRowShape(index, transactions.lastIndex),
                        showDivider = index < transactions.lastIndex,
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun CategoryCard(
    category: ExpenseCategory,
    accentColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape,
    showDivider: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    val categoryColor = category.colorHex.toColor(accentColor)
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = categoryColor.copy(alpha = 0.16f),
                    contentColor = categoryColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (category.type == ExpenseType.INCOME) {
                                Lucide.TrendingUp
                            } else {
                                Lucide.TrendingDown
                            },
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = category.localizedName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CategoryMetaPill(
                            label = stringResourceSafe(
                                if (category.type == ExpenseType.INCOME) {
                                    R.string.expense_category_type_income
                                } else {
                                    R.string.expense_category_type_expense
                                },
                            ),
                            color = if (category.type == ExpenseType.INCOME) {
                                accentColor
                            } else {
                                colorScheme.error
                            },
                        )
                        CategoryMetaPill(
                            label = if (category.isDefault) {
                                stringResourceSafe(R.string.expense_category_default_badge)
                            } else {
                                stringResourceSafe(R.string.expense_category_custom_badge)
                            },
                            color = colorScheme.onSurface,
                        )
                    }
                }
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResourceSafe(CoreUiR.string.more_options),
                            tint = colorScheme.onSurface.copy(alpha = 0.78f),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(
                            color = colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                        ),
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResourceSafe(CoreUiR.string.edit)) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = accentColor,
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = colorScheme.onSurface,
                            ),
                        )
                        DropdownMenuItem(
                            text = { Text(stringResourceSafe(CoreUiR.string.delete)) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = colorScheme.error,
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = colorScheme.error,
                            ),
                        )
                    }
                }
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = colorScheme.outline.copy(alpha = 0.12f),
                )
            }
        }
    }
}

@Composable
private fun CategoryMetaPill(
    label: String,
    color: Color,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        contentColor = color,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun CategoryDialog(
    category: ExpenseCategory?,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (ExpenseCategory) -> Unit,
) {
    var name by remember(category) { mutableStateOf(category?.name ?: "") }
    var colorHex by remember(category) {
        mutableStateOf(
            category?.colorHex ?: defaultCategoryColors.first()
        )
    }
    var selectedType by remember(category) { mutableStateOf(category?.type ?: ExpenseType.EXPENSE) }
    val accentContentColor = contentColorFor(accentColor)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 720.dp),
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
                            Icon(imageVector = Lucide.Tags, contentDescription = null)
                        }
                    }
                    Text(
                        text = if (category == null) {
                            stringResourceSafe(R.string.expense_category_create)
                        } else {
                            stringResourceSafe(R.string.expense_category_edit)
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResourceSafe(R.string.expense_category_name_label),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResourceSafe(R.string.expense_category_type_label),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                val types = ExpenseType.entries
                SingleChoiceSegmentedButtonRow {
                    types.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            shape = SegmentedButtonDefaults.itemShape(index, types.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = accentColor,
                                activeContentColor = accentContentColor,
                                inactiveContainerColor = MaterialTheme.colorScheme.surface,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                text = stringResourceSafe(
                                    if (type == ExpenseType.INCOME) R.string.expense_category_type_income
                                    else R.string.expense_category_type_expense
                                )
                            )
                        }
                    }
                }
                Text(
                    text = stringResourceSafe(R.string.expense_category_color_label),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                ColorGrid(
                    selectedColor = colorHex,
                    accentColor = accentColor,
                    onColorSelected = { colorHex = it }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                    ) {
                        Text(text = stringResourceSafe(CoreUiR.string.cancel))
                    }
                    Button(
                        onClick = {
                            val base = category ?: ExpenseCategory(
                                id = 0,
                                name = name,
                                colorHex = colorHex,
                                isDefault = false,
                                type = selectedType,
                            )
                            onSave(base.copy(name = name, colorHex = colorHex, type = selectedType))
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = accentContentColor,
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(text = stringResourceSafe(CoreUiR.string.save))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorGrid(
    selectedColor: String,
    accentColor: Color,
    onColorSelected: (String) -> Unit
) {
    val cellsPerRow = 4
    val rows = defaultCategoryColors.chunked(cellsPerRow)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { color ->
                    val background = color.toColor(accentColor)
                    val colorIndex = defaultCategoryColors.indexOf(color) + 1
                    val colorDescription = stringResourceSafe(
                        R.string.expense_category_color_option,
                        colorIndex,
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(background, CircleShape)
                            .border(
                                width = if (selectedColor == color) 3.dp else 0.dp,
                                color = if (selectedColor == color) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                shape = CircleShape,
                            )
                            .selectable(
                                selected = selectedColor == color,
                                onClick = { onColorSelected(color) },
                                role = Role.RadioButton,
                            )
                            .semantics {
                                contentDescription = colorDescription
                            },
                    )
                }
                repeat(cellsPerRow - row.size) {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

private val defaultCategoryColors = listOf(
    "#FF7043",
    "#29B6F6",
    "#BA68C8",
    "#8D6E63",
    "#66BB6A",
    "#EF5350",
    "#FFA726",
    "#26C6DA",
    "#9CCC65",
    "#AB47BC",
    "#5C6BC0",
    "#FFB300"
)

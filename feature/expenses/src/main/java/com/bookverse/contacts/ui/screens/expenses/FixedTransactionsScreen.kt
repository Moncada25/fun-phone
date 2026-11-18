package com.bookverse.contacts.ui.screens.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import com.bookverse.contacts.feature.expenses.R
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.CountBadge
import com.bookverse.contacts.ui.components.OptionsDialog
import com.bookverse.contacts.ui.components.OptionsDialogItem
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Repeat
import com.bookverse.contacts.core.ui.R as CoreUiR

@Composable
fun FixedTransactionsScreen(
    transactions: List<ExpenseTransaction>,
    accentColor: Color,
    currency: ExpenseCurrency,
    onAddTransaction: () -> Unit,
    onEditTransaction: (ExpenseTransaction) -> Unit,
    onDeleteTransaction: (ExpenseTransaction) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,) {
    var transactionToDelete by remember { mutableStateOf<ExpenseTransaction?>(null) }
    var showOptionsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RecurringTransactionsHeader(
            count = transactions.size,
            accentColor = accentColor,
            onAddTransaction = onAddTransaction,
            onShowOptions = { showOptionsDialog = true },
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (transactions.isEmpty()) {
                item {
                    ExpenseEmptyState(message = stringResource(R.string.expense_fixed_empty))
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
                        onDelete = { transactionToDelete = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = expenseGroupedRowShape(index, transactions.lastIndex),
                        showDivider = index < transactions.lastIndex,
                    )
                }
            }
        }
    }

    if (showOptionsDialog) {
        val optionItems = listOf(
            OptionsDialogItem(
                label = stringResource(CoreUiR.string.action_export_short),
                icon = Icons.Default.Backup,
                enabled = transactions.isNotEmpty(),
                onClick = onExport
            ),
            OptionsDialogItem(
                label = stringResource(CoreUiR.string.restore),
                icon = Icons.Default.RestorePage,
                onClick = onImport
            )
        )
        OptionsDialog(
            title = stringResource(CoreUiR.string.more_options),
            options = optionItems,
            onDismiss = { showOptionsDialog = false },
        )
    }

    val deleteTarget = transactionToDelete
    if (deleteTarget != null) {
        ConfirmDialog(
            onDismiss = { transactionToDelete = null },
            onConfirm = {
                onDeleteTransaction(deleteTarget)
                transactionToDelete = null
            },
            title = stringResource(R.string.expense_delete_transaction),
            message = stringResource(R.string.expense_delete_transaction_confirm),
        )
    }
}

@Composable
private fun RecurringTransactionsHeader(
    count: Int,
    accentColor: Color,
    onAddTransaction: () -> Unit,
    onShowOptions: () -> Unit,
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
                        Icon(imageVector = Lucide.Repeat, contentDescription = null)
                    }
                }
                Text(
                    text = stringResource(R.string.expense_fixed_transactions_title),
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
                IconButton(
                    onClick = onShowOptions,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(CoreUiR.string.more_options),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    )
                }
            }
            Text(
                text = stringResource(R.string.expense_fixed_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Button(
                onClick = onAddTransaction,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = accentContentColor,
                ),
            ) {
                Text(text = stringResource(R.string.add))
            }
        }
    }
}

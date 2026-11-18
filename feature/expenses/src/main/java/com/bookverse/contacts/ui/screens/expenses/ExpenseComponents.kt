package com.bookverse.contacts.ui.screens.expenses

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.feature.expenses.R
import com.bookverse.contacts.ui.theme.BudgetOkColor
import com.bookverse.contacts.ui.theme.BudgetWarningColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.domain.expenses.BudgetState
import com.bookverse.contacts.domain.expenses.BudgetStatus
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import com.bookverse.contacts.domain.expenses.formatAsCurrency
import com.bookverse.contacts.settings.LocaleController
import com.composables.icons.lucide.Coins
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Repeat
import com.composables.icons.lucide.TrendingDown
import com.composables.icons.lucide.TrendingUp
import java.time.YearMonth
import java.time.format.TextStyle
import androidx.core.graphics.toColorInt
import com.bookverse.contacts.core.ui.R as CoreUiR

@Composable
fun ExpenseMonthSelector(
    month: YearMonth,
    accentColor: Color,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = textColor,
        shadowElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonthNavigationButton(
                icon = Icons.Rounded.ChevronLeft,
                contentDescription = stringResource(CoreUiR.string.cd_previous_month),
                onClick = onPrev,
                accentColor = accentColor
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = month.month.getDisplayName(TextStyle.FULL, locale)
                        .replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                        },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = month.year.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
            MonthNavigationButton(
                icon = Icons.Rounded.ChevronRight,
                contentDescription = stringResource(CoreUiR.string.cd_next_month),
                onClick = onNext,
                accentColor = accentColor
            )
        }
    }
}

@Composable
fun ExpenseYearSelector(
    year: Int,
    accentColor: Color,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = textColor,
        shadowElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonthNavigationButton(
                icon = Icons.Rounded.ChevronLeft,
                contentDescription = stringResource(R.string.expense_yearly_previous_year),
                onClick = onPrev,
                accentColor = accentColor
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.expense_yearly_title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
            MonthNavigationButton(
                icon = Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.expense_yearly_next_year),
                onClick = onNext,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun MonthNavigationButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    accentColor: Color
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = accentColor.copy(alpha = 0.12f),
        contentColor = accentColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun ExpenseTransactionCard(
    transaction: ExpenseTransaction,
    accentColor: Color,
    currency: ExpenseCurrency,
    onEdit: (ExpenseTransaction) -> Unit,
    onDelete: (ExpenseTransaction) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ExpenseCardShape,
    showDivider: Boolean = false,
) {
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    val isIncome = transaction.type == ExpenseType.INCOME
    val amountColor = if (isIncome) accentColor else MaterialTheme.colorScheme.error
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = amountColor.copy(alpha = 0.14f),
                    contentColor = amountColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isIncome) Lucide.TrendingUp else Lucide.TrendingDown,
                            contentDescription = null,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = transaction.categoryName,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (transaction.isFixed) {
                            RecurringTransactionBadge(accentColor = accentColor)
                        }
                    }
                    Text(
                        text = transaction.date.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val transactionNote = transaction.note
                    if (!transactionNote.isNullOrBlank()) {
                        Text(
                            text = transactionNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = transaction.amount.formatAsCurrency(currency, locale),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                    )
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(CoreUiR.string.more_options),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                            ),
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(CoreUiR.string.edit)) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit(transaction)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = accentColor,
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(CoreUiR.string.delete)) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete(transaction)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error,
                                ),
                            )
                        }
                    }
                }
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                )
            }
        }
    }
}

@Composable
internal fun RecurringTransactionBadge(accentColor: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Lucide.Repeat,
            contentDescription = stringResource(R.string.expense_transaction_recurring_badge),
            modifier = Modifier.size(16.dp),
            tint = accentColor,
        )
    }
}

@Composable
fun BudgetStatusCard(
    status: BudgetStatus,
    currency: ExpenseCurrency,
    modifier: Modifier = Modifier,
    accentColor: Color,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    shape: Shape = ExpenseCardShape,
    showDivider: Boolean = false,
) {
    val background = MaterialTheme.colorScheme.surface
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    val borderColor = when (status.state) {
        BudgetState.OK -> BudgetOkColor
        BudgetState.WARNING -> BudgetWarningColor
        BudgetState.EXCEEDED -> MaterialTheme.colorScheme.error
    }
    val targetProgress = status.progress.coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        label = "budget_progress"
    )
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier,
        shape = shape,
        color = background,
        tonalElevation = 1.dp,
    ) {
        Column {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(status.category?.colorHex.toColor(borderColor), CircleShape)
                    )
                    Text(
                        text = status.category?.localizedName()
                            ?: stringResourceSafe(R.string.expense_category_generic),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(CoreUiR.string.more_options),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                            ),
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(CoreUiR.string.edit)) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = accentColor,
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(CoreUiR.string.delete)) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error,
                                ),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResourceSafe(R.string.expense_budget_amount_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    )
                    Text(
                        text = status.budget.limit.formatAsCurrency(currency, locale),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    color = borderColor,
                    trackColor = borderColor.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(
                            text = stringResourceSafe(R.string.expense_budget_spent),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = status.spent.formatAsCurrency(currency, locale),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = when (status.state) {
                            BudgetState.OK -> stringResourceSafe(R.string.expense_budget_state_ok)
                            BudgetState.WARNING -> stringResourceSafe(R.string.expense_budget_state_warning)
                            BudgetState.EXCEEDED -> stringResourceSafe(R.string.expense_budget_state_exceeded)
                        },
                        color = borderColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 36.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                )
            }
        }
    }
}

internal fun expenseGroupedRowShape(index: Int, lastIndex: Int): Shape {
    if (lastIndex == 0) return ExpenseCardShape
    return when (index) {
        0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        lastIndex -> RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
        else -> RoundedCornerShape(0.dp)
    }
}

@Composable
fun ExpenseEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ExpenseCardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Lucide.Coins, contentDescription = null)
                }
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun stringResourceSafe(@StringRes res: Int, vararg formatArgs: Any): String =
    stringResource(res, *formatArgs)

internal fun String?.toColor(fallback: Color): Color {
    if (this.isNullOrBlank()) return fallback
    return runCatching { Color(this.toColorInt()) }.getOrElse { fallback }
}

internal val ExpenseCardShape = RoundedCornerShape(28.dp)

private val categoryNameResMap = mapOf(
    "expense_category_food" to R.string.expense_category_food,
    "expense_category_transport" to R.string.expense_category_transport,
    "expense_category_entertainment" to R.string.expense_category_entertainment,
    "expense_category_home" to R.string.expense_category_home,
    "expense_category_shopping" to R.string.expense_category_shopping,
    "expense_category_health" to R.string.expense_category_health,
    "expense_category_education" to R.string.expense_category_education,
    "expense_category_travel" to R.string.expense_category_travel,
    "expense_category_salary" to R.string.expense_category_salary,
    "expense_category_bonus" to R.string.expense_category_bonus,
    "expense_category_freelance" to R.string.expense_category_freelance,
)

@Composable
internal fun ExpenseCategory.localizedName(): String {
    val resId = nameResKey?.let { categoryNameResMap[it] }
    return if (resId != null) stringResource(resId) else name
}

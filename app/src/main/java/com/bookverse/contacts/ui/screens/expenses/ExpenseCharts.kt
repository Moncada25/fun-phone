package com.bookverse.contacts.ui.screens.expenses

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.annotation.StringRes
import androidx.compose.ui.window.DialogProperties
import com.bookverse.contacts.R
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.domain.expenses.CategoryShare
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import com.bookverse.contacts.domain.expenses.MonthSummary
import com.bookverse.contacts.domain.expenses.formatAsCurrency
import com.bookverse.contacts.settings.LocaleController
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.core.graphics.toColorInt
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ExpensePieChart(
    shares: List<CategoryShare>,
    accentColor: Color,
    currency: ExpenseCurrency,
    transactions: List<ExpenseTransaction>,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val backgroundColor = MaterialTheme.colorScheme.background
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    var selectedShare by remember { mutableStateOf<CategoryShare?>(null) }

    if (shares.isEmpty()) {
        Text(
            text = stringResourceSafe(R.string.expense_chart_empty),
            color = textColor.copy(alpha = 0.6f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = modifier
                .padding(vertical = 24.dp)
                .fillMaxWidth()
        )
        return
    }
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1.2f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    val diameter = min(size.width, size.height)
                    val radius = diameter / 2f
                    val topLeft =
                        Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val rect = Rect(topLeft, androidx.compose.ui.geometry.Size(diameter, diameter))
                    var startAngle = -90f
                    shares.forEach { share ->
                        val sweep = share.percentage.coerceIn(0f, 1f) * 360f
                        val color = share.displayColor(accentColor)
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true,
                            topLeft = rect.topLeft,
                            size = rect.size
                        )
                        drawArc(
                            color = backgroundColor,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = rect.topLeft,
                            size = rect.size,
                            style = Stroke(width = 2f)
                        )
                        startAngle += sweep
                    }
                    drawCircle(
                        color = backgroundColor,
                        radius = radius * 0.45f,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                shares.forEach { share ->
                    ExpenseLegendItem(
                        share = share,
                        accentColor = accentColor,
                        currency = currency,
                        locale = locale,
                        onClick = { selectedShare = share }
                    )
                }
            }
        }
    }

    selectedShare?.let { share ->
        CategoryBreakdownDialog(
            share = share,
            transactions = transactions.filter { transaction ->
                transaction.type == share.type &&
                    if (share.categoryId != null) {
                        transaction.categoryId == share.categoryId
                    } else {
                        transaction.categoryName == share.name
                    }
            },
            accentColor = accentColor,
            currency = currency,
            onDismiss = { selectedShare = null }
        )
    }
}

@Composable
fun ExpenseMonthlyBarChart(
    months: List<MonthSummary>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    val incomeColor = accentColor
    val expenseColor = MaterialTheme.colorScheme.error
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    val maxValue = months.maxOfOrNull { maxOf(it.totals.income, it.totals.expenses) } ?: 0.0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val slotWidth = size.width / months.size.coerceAtLeast(1)
            val barWidth = slotWidth * 0.32f
            val gap = slotWidth * 0.06f
            val corner = androidx.compose.ui.geometry.CornerRadius(barWidth / 3f, barWidth / 3f)
            val baseline = size.height - 1f
            drawLine(
                color = axisColor,
                start = Offset(0f, baseline),
                end = Offset(size.width, baseline),
                strokeWidth = 2f
            )
            if (maxValue > 0.0) {
                months.forEachIndexed { index, summary ->
                    val center = slotWidth * index + slotWidth / 2f
                    val incomeHeight = (summary.totals.income / maxValue).toFloat() * baseline
                    val expenseHeight = (summary.totals.expenses / maxValue).toFloat() * baseline
                    if (incomeHeight > 0f) {
                        drawRoundRect(
                            color = incomeColor,
                            topLeft = Offset(center - gap / 2f - barWidth, baseline - incomeHeight),
                            size = androidx.compose.ui.geometry.Size(barWidth, incomeHeight),
                            cornerRadius = corner
                        )
                    }
                    if (expenseHeight > 0f) {
                        drawRoundRect(
                            color = expenseColor,
                            topLeft = Offset(center + gap / 2f, baseline - expenseHeight),
                            size = androidx.compose.ui.geometry.Size(barWidth, expenseHeight),
                            cornerRadius = corner
                        )
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            months.forEach { summary ->
                Text(
                    text = summary.month.month.getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    color = labelColor,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartLegendEntry(color = incomeColor, label = stringResource(R.string.expense_summary_income))
            ChartLegendEntry(color = expenseColor, label = stringResource(R.string.expense_summary_expense))
        }
    }
}

@Composable
private fun ChartLegendEntry(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ExpenseLegendItem(
    share: CategoryShare,
    accentColor: Color,
    currency: ExpenseCurrency,
    onClick: () -> Unit,
    locale: Locale,
) {
    val indicatorColor = share.displayColor(accentColor)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(indicatorColor, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = share.name,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${share.amount.formatAsCurrency(currency, locale)} · ${(share.percentage * 100).roundToInt()}%",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun stringResourceSafe(@StringRes res: Int): String = stringResource(res)

private fun String.toColor(fallback: Color): Color =
    runCatching { Color(this.toColorInt()) }.getOrElse { fallback }

private fun CategoryShare.displayColor(accentColor: Color): Color =
    if (type == ExpenseType.INCOME) accentColor else colorHex.toColor(accentColor)

@Composable
private fun CategoryBreakdownDialog(
    share: CategoryShare,
    transactions: List<ExpenseTransaction>,
    accentColor: Color,
    currency: ExpenseCurrency,
    onDismiss: () -> Unit
) {
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(share.colorHex.toColor(accentColor), CircleShape)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = share.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${transactions.size} ${if (transactions.size == 1) stringResource(R.string.expense_transaction_single) else stringResource(R.string.expense_transaction_plural)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = accentColor.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.expense_total),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = share.amount.formatAsCurrency(currency, locale),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.expense_percentage),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(share.percentage * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (transactions.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.expense_breakdown_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Column(
                            modifier = Modifier
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val sortedTransactions = transactions.sortedByDescending { it.amount }
                            sortedTransactions.forEach { transaction ->
                                TransactionBreakdownItem(
                                    transaction = transaction,
                                    currency = currency,
                                    locale = locale,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionBreakdownItem(
    transaction: ExpenseTransaction,
    currency: ExpenseCurrency,
    locale: Locale,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (!transaction.note.isNullOrBlank()) {
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = transaction.date.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = transaction.amount.formatAsCurrency(currency, locale),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

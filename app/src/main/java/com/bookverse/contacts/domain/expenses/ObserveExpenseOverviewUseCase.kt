package com.bookverse.contacts.domain.expenses

import com.bookverse.contacts.data.expenses.ExpenseBudget
import com.bookverse.contacts.data.expenses.ExpenseCategory
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import java.time.YearMonth

internal fun calculateMonthlyTotals(transactions: List<ExpenseTransaction>): MonthlyTotals {
    val income = transactions
        .filter { it.type == ExpenseType.INCOME }
        .sumOf { it.amount }
    val expenses = transactions
        .filter { it.type == ExpenseType.EXPENSE }
        .sumOf { it.amount }
    return MonthlyTotals(income = income, expenses = expenses)
}

internal fun buildCategoryDistribution(
    transactions: List<ExpenseTransaction>
): List<CategoryShare> {
    val total = transactions.sumOf { it.amount }.takeIf { it > 0 } ?: return emptyList()
    return transactions
        .groupBy { transaction ->
            transaction.type to (transaction.categoryId ?: transaction.categoryName.hashCode().toLong())
        }
        .map { (_, group) ->
            val sample = group.first()
            val amount = group.sumOf { it.amount }
            CategoryShare(
                categoryId = sample.categoryId,
                name = sample.categoryName,
                colorHex = sample.categoryColor,
                amount = amount,
                percentage = (amount / total).toFloat().coerceIn(0f, 1f),
                type = sample.type
            )
        }
        .sortedByDescending { it.amount }
}

internal fun buildBudgetStatuses(
    transactions: List<ExpenseTransaction>,
    budgets: List<Pair<ExpenseBudget, ExpenseCategory?>>
): List<BudgetStatus> {
    val expenses = transactions.filter { it.type == ExpenseType.EXPENSE }
    return budgets.map { (budget, category) ->
        val spent = expenses
            .filter { it.categoryId == budget.categoryId }
            .sumOf { it.amount }
        val progress =
            if (budget.limit <= 0.0) 1f else (spent / budget.limit).toFloat().coerceAtMost(1.5f)
        val state = when {
            budget.limit <= 0.0 -> BudgetState.EXCEEDED
            progress >= 1f -> BudgetState.EXCEEDED
            progress >= 0.75f -> BudgetState.WARNING
            else -> BudgetState.OK
        }
        BudgetStatus(
            budget = budget,
            category = category,
            spent = spent,
            progress = progress,
            state = state
        )
    }.sortedBy { it.category?.name ?: "" }
}

internal fun transactionsForMonth(
    transactions: List<ExpenseTransaction>,
    month: YearMonth,
): List<ExpenseTransaction> {
    val regular = transactions.filter { !it.isFixed && YearMonth.from(it.date) == month }
    val fixedCopies = transactions.filter { it.isFixed }.map { it.copy(date = month.atDay(1)) }
    return (regular + fixedCopies).sortedWith(
        compareBy<ExpenseTransaction> { it.date }.thenBy { it.createdAt },
    )
}

data class MonthBudgetSummary(
    val totals: MonthlyTotals,
    val budgetLimit: Double,
    val budgetSpent: Double,
) {
    val hasBudget: Boolean get() = budgetLimit > 0.0
    val progress: Float get() = if (hasBudget) (budgetSpent / budgetLimit).toFloat().coerceIn(0f, 1f) else 0f
    val state: BudgetState
        get() = when {
            !hasBudget -> BudgetState.OK
            budgetSpent >= budgetLimit -> BudgetState.EXCEEDED
            budgetSpent >= budgetLimit * 0.75 -> BudgetState.WARNING
            else -> BudgetState.OK
        }
}

internal fun buildMonthBudgetSummary(
    monthTransactions: List<ExpenseTransaction>,
    budgets: List<Pair<ExpenseBudget, ExpenseCategory?>>,
): MonthBudgetSummary {
    val statuses = buildBudgetStatuses(monthTransactions, budgets).filter { it.budget.limit > 0.0 }
    return MonthBudgetSummary(
        totals = calculateMonthlyTotals(monthTransactions),
        budgetLimit = statuses.sumOf { it.budget.limit },
        budgetSpent = statuses.sumOf { it.spent },
    )
}

package com.bookverse.contacts.data.expenses

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.utils.FixedTransactionsBackupUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

class ExpensesRepository(
    private val dao: ExpenseDao,
    private val context: Context
) : ExpensesStore {
    private data class DefaultCategory(
        @param:StringRes val nameRes: Int,
        val color: String,
        val type: ExpenseType,
        val resKey: String
    )

    private val defaultCategories = listOf(
        DefaultCategory(R.string.expense_category_food, "#FF7043", ExpenseType.EXPENSE, "expense_category_food"),
        DefaultCategory(R.string.expense_category_transport, "#29B6F6", ExpenseType.EXPENSE, "expense_category_transport"),
        DefaultCategory(R.string.expense_category_entertainment, "#BA68C8", ExpenseType.EXPENSE, "expense_category_entertainment"),
        DefaultCategory(R.string.expense_category_home, "#8D6E63", ExpenseType.EXPENSE, "expense_category_home"),
        DefaultCategory(R.string.expense_category_shopping, "#66BB6A", ExpenseType.EXPENSE, "expense_category_shopping"),
        DefaultCategory(R.string.expense_category_health, "#EF5350", ExpenseType.EXPENSE, "expense_category_health"),
        DefaultCategory(R.string.expense_category_education, "#FFA726", ExpenseType.EXPENSE, "expense_category_education"),
        DefaultCategory(R.string.expense_category_travel, "#26C6DA", ExpenseType.EXPENSE, "expense_category_travel"),
        DefaultCategory(R.string.expense_category_salary, "#66BB6A", ExpenseType.INCOME, "expense_category_salary"),
        DefaultCategory(R.string.expense_category_bonus, "#29B6F6", ExpenseType.INCOME, "expense_category_bonus"),
        DefaultCategory(R.string.expense_category_freelance, "#AB47BC", ExpenseType.INCOME, "expense_category_freelance")
    )

    override val categories: Flow<List<ExpenseCategory>> =
        dao.observeCategories().map { list -> list.map { it.toDomain() } }
    override val transactions: Flow<List<ExpenseTransaction>> =
        dao.observeTransactions().map { list -> list.map { it.toDomain() } }
    override val budgetsWithCategories: Flow<List<Pair<ExpenseBudget, ExpenseCategory?>>> =
        dao.observeBudgetsWithCategory().map { list ->
            list.map { rel -> rel.budget.toDomain() to rel.toCategoryDomain() }
        }

    override suspend fun seedDefaultCategoriesIfNeeded() = withContext(Dispatchers.IO) {
        val existing = dao.getCategories()
        val existingByResKey = existing.filter { it.nameResKey != null }.associateBy { it.nameResKey }
        val existingNames = existing.map { it.name.lowercase() }.toSet()
        defaultCategories.forEach { preset ->
            val localizedName = context.getString(preset.nameRes)
            val match = existingByResKey[preset.resKey]
            when {
                match != null -> {
                    if (match.name != localizedName) {
                        dao.updateCategory(match.copy(name = localizedName))
                    }
                }
                localizedName.lowercase() !in existingNames -> {
                    dao.insertCategory(
                        ExpenseCategoryEntity(
                            name = localizedName,
                            colorHex = preset.color,
                            isDefault = true,
                            type = preset.type,
                            nameResKey = preset.resKey
                        )
                    )
                }
            }
        }
    }

    override suspend fun upsertTransaction(transaction: ExpenseTransaction): Long =
        withContext(Dispatchers.IO) {
            val sanitized = transaction.copy(
                amount = transaction.amount.coerceAtLeast(0.0),
                date = transaction.date,
                createdAt = if (transaction.id == 0L) System.currentTimeMillis() else transaction.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            val entity = sanitized.toEntity()
            if (sanitized.id == 0L) {
                dao.insertTransaction(entity)
            } else {
                dao.updateTransaction(entity)
                sanitized.id
            }
        }

    override suspend fun deleteTransaction(id: Long) {
        withContext(Dispatchers.IO) {
            dao.getTransactionById(id)?.let { dao.deleteTransaction(it) }
        }
    }

    override suspend fun upsertCategory(category: ExpenseCategory): Long = withContext(Dispatchers.IO) {
        val sanitized = category.copy(
            createdAt = if (category.id == 0L) System.currentTimeMillis() else category.createdAt
        )
        val entity = sanitized.toEntity()
        if (sanitized.id == 0L) {
            dao.insertCategory(entity)
        } else {
            dao.updateCategory(entity)
            dao.updateTransactionsCategoryMeta(entity.id, entity.name, entity.colorHex)
            sanitized.id
        }
    }

    override suspend fun deleteCategory(categoryId: Long): Boolean = withContext(Dispatchers.IO) {
        val entity = dao.getCategoryById(categoryId) ?: return@withContext false
        if (dao.countTransactionsForCategory(categoryId) > 0) {
            return@withContext false
        }
        dao.deleteCategory(entity)
        dao.getBudgetForCategory(categoryId)?.let { dao.deleteBudget(it) }
        true
    }

    override suspend fun upsertBudget(budget: ExpenseBudget): Long = withContext(Dispatchers.IO) {
        val sanitized = budget.copy(
            limit = budget.limit.coerceAtLeast(0.0),
            createdAt = if (budget.id == 0L) System.currentTimeMillis() else budget.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        val entity = sanitized.toEntity()
        if (sanitized.id == 0L) {
            dao.insertBudget(entity)
        } else {
            dao.updateBudget(entity)
            sanitized.id
        }
    }

    override suspend fun deleteBudget(budgetId: Long) {
        withContext(Dispatchers.IO) {
            dao.getBudgetById(budgetId)?.let { dao.deleteBudget(it) }
        }
    }

    override suspend fun getTransaction(id: Long): ExpenseTransaction? = withContext(Dispatchers.IO) {
        dao.getTransactionById(id)?.toDomain()
    }

    override suspend fun getCategory(categoryId: Long): ExpenseCategory? = withContext(Dispatchers.IO) {
        dao.getCategoryById(categoryId)?.toDomain()
    }

    override suspend fun exportFixedTransactions(
        transactions: List<ExpenseTransaction>,
        uri: Uri,
    ): ExpenseExportSummary {
        val summary = FixedTransactionsBackupUtils.exportFixedTransactions(
            context = context,
            transactions = transactions,
            uri = uri,
        )
        return ExpenseExportSummary(summary.count)
    }

    override suspend fun importFixedTransactions(uri: Uri): ExpenseImportSummary {
        val summary = FixedTransactionsBackupUtils.importFixedTransactions(
            context = context,
            store = this,
            uri = uri,
        )
        return ExpenseImportSummary(summary.count)
    }

    override suspend fun exportTransactionsCsv(
        uri: Uri,
        headers: ExpenseCsvHeaders,
        currencyCode: String,
    ): ExpenseExportSummary = withContext(Dispatchers.IO) {
        val transactions = dao.getTransactionsForExport()
        val resolver = context.contentResolver
        val stream = runCatching { resolver.openOutputStream(uri, "wt") }.getOrNull()
            ?: resolver.openOutputStream(uri, "w")
            ?: throw IOException("Unable to open output stream")
        val count = stream.bufferedWriter(Charsets.UTF_8).use { writer ->
            ExpenseCsv.write(
                out = writer,
                headers = headers,
                transactions = transactions.asSequence().map { it.toDomain() }.asIterable(),
                currencyCode = currencyCode,
            )
        }
        ExpenseExportSummary(count)
    }
}

private fun BudgetWithCategory.toCategoryDomain(): ExpenseCategory? {
    val id = categoryId ?: return null
    val name = categoryName ?: return null
    val color = categoryColor ?: return null
    val type = ExpenseType.fromName(categoryType)
    return ExpenseCategory(
        id = id,
        name = name,
        colorHex = color,
        isDefault = categoryIsDefault ?: false,
        createdAt = categoryCreatedAt ?: System.currentTimeMillis(),
        type = type,
        nameResKey = null
    )
}

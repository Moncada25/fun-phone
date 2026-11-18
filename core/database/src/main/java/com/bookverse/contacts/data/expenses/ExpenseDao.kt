package com.bookverse.contacts.data.expenses

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense_categories ORDER BY name ASC")
    fun observeCategories(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_categories")
    suspend fun getCategories(): List<ExpenseCategoryEntity>

    @Query("SELECT * FROM expense_categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): ExpenseCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ExpenseCategoryEntity): Long

    @Update
    suspend fun updateCategory(category: ExpenseCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: ExpenseCategoryEntity)

    @Query("SELECT COUNT(*) FROM expense_transactions WHERE categoryId = :categoryId")
    suspend fun countTransactionsForCategory(categoryId: Long): Int

    @Query(
        "UPDATE expense_transactions SET categoryName = :name, categoryColor = :colorHex WHERE categoryId = :categoryId"
    )
    suspend fun updateTransactionsCategoryMeta(categoryId: Long, name: String, colorHex: String)

    @Query("SELECT * FROM expense_transactions ORDER BY date DESC, id DESC")
    fun observeTransactions(): Flow<List<ExpenseTransactionEntity>>

    @Query("SELECT * FROM expense_transactions ORDER BY date ASC, id ASC")
    suspend fun getTransactionsForExport(): List<ExpenseTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: ExpenseTransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: ExpenseTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: ExpenseTransactionEntity)

    @Query("SELECT * FROM expense_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): ExpenseTransactionEntity?

    @Query("SELECT * FROM expense_budgets ORDER BY id ASC")
    fun observeBudgets(): Flow<List<ExpenseBudgetEntity>>

    @Query("SELECT * FROM expense_budgets WHERE id = :id LIMIT 1")
    suspend fun getBudgetById(id: Long): ExpenseBudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: ExpenseBudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: ExpenseBudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: ExpenseBudgetEntity)

    @Query("SELECT * FROM expense_budgets WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getBudgetForCategory(categoryId: Long): ExpenseBudgetEntity?

    @Transaction
    @Query(
        """
        SELECT expense_budgets.*,
        expense_categories.id AS category_id,
        expense_categories.name AS category_name,
        expense_categories.colorHex AS category_colorHex,
        expense_categories.isDefault AS category_isDefault,
        expense_categories.createdAt AS category_createdAt,
        expense_categories.type AS category_type
        FROM expense_budgets
        LEFT JOIN expense_categories ON expense_categories.id = expense_budgets.categoryId
        """
    )
    fun observeBudgetsWithCategory(): Flow<List<BudgetWithCategory>>
}

data class BudgetWithCategory(
    @Embedded val budget: ExpenseBudgetEntity,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    @ColumnInfo(name = "category_colorHex") val categoryColor: String?,
    @ColumnInfo(name = "category_isDefault") val categoryIsDefault: Boolean?,
    @ColumnInfo(name = "category_createdAt") val categoryCreatedAt: Long?,
    @ColumnInfo(name = "category_type") val categoryType: String?
)

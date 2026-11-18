package com.bookverse.contacts.data.expenses

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class ExpenseType {
    INCOME,
    EXPENSE;

    companion object {
        fun fromName(value: String?): ExpenseType {
            return when (value) {
                EXPENSE.name -> EXPENSE
                else -> INCOME
            }
        }
    }
}

@Entity(
    tableName = "expense_categories",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class ExpenseCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "EXPENSE") val type: ExpenseType = ExpenseType.EXPENSE,
    @ColumnInfo(defaultValue = "NULL") val nameResKey: String? = null
)

@Entity(
    tableName = "expense_transactions",
    indices = [
        Index(value = ["categoryId"])
    ]
)
data class ExpenseTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ExpenseType = ExpenseType.EXPENSE,
    val amount: Double,
    val categoryId: Long?,
    val categoryName: String,
    val categoryColor: String,
    val date: LocalDate,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val isFixed: Boolean = false
)

@Entity(
    tableName = "expense_budgets",
    indices = [
        Index(value = ["categoryId"], unique = true)
    ]
)
data class ExpenseBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val limit: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Immutable
data class ExpenseCategory(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val type: ExpenseType = ExpenseType.EXPENSE,
    val nameResKey: String? = null
)

@Immutable
data class ExpenseTransaction(
    val id: Long = 0,
    val type: ExpenseType,
    val amount: Double,
    val categoryId: Long?,
    val categoryName: String,
    val categoryColor: String,
    val date: LocalDate,
    val note: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFixed: Boolean = false
)

@Immutable
data class ExpenseBudget(
    val id: Long = 0,
    val categoryId: Long,
    val limit: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun ExpenseCategoryEntity.toDomain(): ExpenseCategory = ExpenseCategory(
    id = id,
    name = name,
    colorHex = colorHex,
    isDefault = isDefault,
    createdAt = createdAt,
    type = type,
    nameResKey = nameResKey
)

fun ExpenseCategory.toEntity(): ExpenseCategoryEntity = ExpenseCategoryEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    isDefault = isDefault,
    createdAt = createdAt,
    type = type,
    nameResKey = nameResKey
)

fun ExpenseTransactionEntity.toDomain(): ExpenseTransaction = ExpenseTransaction(
    id = id,
    type = type,
    amount = amount,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryColor = categoryColor,
    date = date,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isFixed = isFixed
)

fun ExpenseTransaction.toEntity(): ExpenseTransactionEntity = ExpenseTransactionEntity(
    id = id,
    type = type,
    amount = amount,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryColor = categoryColor,
    date = date,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isFixed = isFixed
)

fun ExpenseBudgetEntity.toDomain(): ExpenseBudget = ExpenseBudget(
    id = id,
    categoryId = categoryId,
    limit = limit,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ExpenseBudget.toEntity(): ExpenseBudgetEntity = ExpenseBudgetEntity(
    id = id,
    categoryId = categoryId,
    limit = limit,
    createdAt = createdAt,
    updatedAt = updatedAt
)

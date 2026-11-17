package com.bookverse.contacts.data.expenses

import java.math.BigDecimal

data class ExpenseCsvHeaders(
    val date: String,
    val type: String,
    val amount: String,
    val currency: String,
    val category: String,
    val note: String,
    val recurring: String,
) {
    fun toRow(): List<String> = listOf(date, type, amount, currency, category, note, recurring)
}

object ExpenseCsv {
    const val DELIMITER = ','
    const val LINE_BREAK = "\r\n"
    const val BYTE_ORDER_MARK = "\uFEFF"
    const val MIME_TYPE = "text/csv"

    private const val QUOTE = '"'
    private const val FORMULA_GUARD = '\''
    private val formulaTriggers = charArrayOf('=', '+', '-', '@', '\t', '\r')

    fun escapeField(value: String): String {
        val needsQuoting = value.any { it == DELIMITER || it == QUOTE || it == '\r' || it == '\n' }
        if (!needsQuoting) return value
        val builder = StringBuilder(value.length + 2)
        builder.append(QUOTE)
        value.forEach { char ->
            if (char == QUOTE) builder.append(QUOTE)
            builder.append(char)
        }
        builder.append(QUOTE)
        return builder.toString()
    }

    fun neutralizeFormula(value: String): String =
        if (value.isNotEmpty() && value[0] in formulaTriggers) "$FORMULA_GUARD$value" else value

    fun formatAmount(amount: Double): String {
        if (amount.isNaN() || amount.isInfinite()) return ""
        val plain = BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString()
        return if (plain == "-0") "0" else plain
    }

    fun row(fields: List<String>): String =
        fields.joinToString(separator = DELIMITER.toString(), transform = ::escapeField)

    fun transactionRow(transaction: ExpenseTransaction, currencyCode: String): String = row(
        listOf(
            transaction.date.toString(),
            transaction.type.name,
            formatAmount(transaction.amount),
            currencyCode,
            neutralizeFormula(transaction.categoryName),
            neutralizeFormula(transaction.note.orEmpty()),
            transaction.isFixed.toString(),
        )
    )

    fun write(
        out: Appendable,
        headers: ExpenseCsvHeaders,
        transactions: Iterable<ExpenseTransaction>,
        currencyCode: String,
    ): Int {
        out.append(BYTE_ORDER_MARK)
        out.append(row(headers.toRow())).append(LINE_BREAK)
        var count = 0
        transactions.forEach { transaction ->
            out.append(transactionRow(transaction, currencyCode)).append(LINE_BREAK)
            count++
        }
        return count
    }
}

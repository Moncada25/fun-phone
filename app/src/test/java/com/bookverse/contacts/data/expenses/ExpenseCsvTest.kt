package com.bookverse.contacts.data.expenses

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseCsvTest {
    private val headers = ExpenseCsvHeaders(
        date = "Date",
        type = "Type",
        amount = "Amount",
        currency = "Currency",
        category = "Category",
        note = "Note",
        recurring = "Recurring",
    )

    @Test
    fun plainFieldsAreNotQuoted() {
        assertEquals("Food", ExpenseCsv.escapeField("Food"))
        assertEquals("", ExpenseCsv.escapeField(""))
        assertEquals("  padded  ", ExpenseCsv.escapeField("  padded  "))
        assertEquals("semi;colon", ExpenseCsv.escapeField("semi;colon"))
        assertEquals("Café ñ 日本", ExpenseCsv.escapeField("Café ñ 日本"))
    }

    @Test
    fun fieldsWithDelimiterAreQuoted() {
        assertEquals("\"a,b\"", ExpenseCsv.escapeField("a,b"))
        assertEquals("\",\"", ExpenseCsv.escapeField(","))
    }

    @Test
    fun innerQuotesAreDoubledAndFieldQuoted() {
        assertEquals("\"say \"\"hi\"\"\"", ExpenseCsv.escapeField("say \"hi\""))
        assertEquals("\"\"\"\"", ExpenseCsv.escapeField("\""))
    }

    @Test
    fun lineBreaksAreKeptInsideQuotes() {
        assertEquals("\"line1\nline2\"", ExpenseCsv.escapeField("line1\nline2"))
        assertEquals("\"line1\rline2\"", ExpenseCsv.escapeField("line1\rline2"))
        assertEquals("\"line1\r\nline2\"", ExpenseCsv.escapeField("line1\r\nline2"))
    }

    @Test
    fun formulaTriggersAreNeutralised() {
        assertEquals("'=SUM(A1:A9)", ExpenseCsv.neutralizeFormula("=SUM(A1:A9)"))
        assertEquals("'+1", ExpenseCsv.neutralizeFormula("+1"))
        assertEquals("'-1", ExpenseCsv.neutralizeFormula("-1"))
        assertEquals("'@cmd", ExpenseCsv.neutralizeFormula("@cmd"))
        assertEquals("'\tcmd", ExpenseCsv.neutralizeFormula("\tcmd"))
        assertEquals("'\rcmd", ExpenseCsv.neutralizeFormula("\rcmd"))
    }

    @Test
    fun harmlessTextIsNotNeutralised() {
        assertEquals("", ExpenseCsv.neutralizeFormula(""))
        assertEquals("a=b", ExpenseCsv.neutralizeFormula("a=b"))
        assertEquals("Rent - May", ExpenseCsv.neutralizeFormula("Rent - May"))
        assertEquals("'quoted", ExpenseCsv.neutralizeFormula("'quoted"))
    }

    @Test
    fun amountsArePlainDecimalsWithoutGrouping() {
        assertEquals("1234567.89", ExpenseCsv.formatAmount(1234567.89))
        assertEquals("25.5", ExpenseCsv.formatAmount(25.5))
        assertEquals("100", ExpenseCsv.formatAmount(100.0))
        assertEquals("0", ExpenseCsv.formatAmount(0.0))
        assertEquals("0", ExpenseCsv.formatAmount(-0.0))
        assertEquals("12500000", ExpenseCsv.formatAmount(1.25E7))
        assertEquals("0.00001", ExpenseCsv.formatAmount(1.0E-5))
        assertEquals("0.1", ExpenseCsv.formatAmount(0.1))
        assertEquals("", ExpenseCsv.formatAmount(Double.NaN))
        assertEquals("", ExpenseCsv.formatAmount(Double.POSITIVE_INFINITY))
    }

    @Test
    fun transactionRowUsesStableMachineReadableColumns() {
        val row = ExpenseCsv.transactionRow(
            transaction(
                type = ExpenseType.INCOME,
                amount = 1500.5,
                categoryName = "Salary",
                date = LocalDate.of(2026, 3, 5),
                note = "March",
                isFixed = true,
            ),
            currencyCode = "EUR",
        )

        assertEquals("2026-03-05,INCOME,1500.5,EUR,Salary,March,true", row)
    }

    @Test
    fun transactionRowNeutralisesThenEscapesFreeText() {
        val row = ExpenseCsv.transactionRow(
            transaction(
                categoryName = "=HYPERLINK(\"http://x\",\"y\")",
                note = "-2+3, \"quoted\"\nnext",
                amount = 10.0,
            ),
            currencyCode = "USD",
        )

        assertEquals(
            "2026-01-15,EXPENSE,10,USD," +
                "\"'=HYPERLINK(\"\"http://x\"\",\"\"y\"\")\"," +
                "\"'-2+3, \"\"quoted\"\"\nnext\",false",
            row,
        )
    }

    @Test
    fun missingNoteBecomesEmptyField() {
        val row = ExpenseCsv.transactionRow(transaction(note = null), currencyCode = "USD")

        assertEquals("2026-01-15,EXPENSE,12.5,USD,Food,,false", row)
    }

    @Test
    fun writeEmitsBomHeaderAndCrLfTerminatedRows() {
        val out = StringBuilder()

        val count = ExpenseCsv.write(
            out = out,
            headers = headers.copy(note = "Note, optional"),
            transactions = listOf(
                transaction(id = 1, note = "Lunch"),
                transaction(id = 2, type = ExpenseType.INCOME, categoryName = "Bonus", amount = 300.0),
            ),
            currencyCode = "USD",
        )

        assertEquals(2, count)
        assertEquals(
            "\uFEFFDate,Type,Amount,Currency,Category,\"Note, optional\",Recurring\r\n" +
                "2026-01-15,EXPENSE,12.5,USD,Food,Lunch,false\r\n" +
                "2026-01-15,INCOME,300,USD,Bonus,,false\r\n",
            out.toString(),
        )
    }

    @Test
    fun writeWithoutTransactionsStillEmitsHeader() {
        val out = StringBuilder()

        val count = ExpenseCsv.write(out, headers, emptyList(), "USD")

        assertEquals(0, count)
        assertEquals("\uFEFFDate,Type,Amount,Currency,Category,Note,Recurring\r\n", out.toString())
    }

    @Test
    fun outputEncodesAsUtf8WithByteOrderMark() {
        val out = StringBuilder()
        ExpenseCsv.write(out, headers, listOf(transaction(categoryName = "Café")), "USD")

        val bytes = out.toString().toByteArray(Charsets.UTF_8)

        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])
        assertTrue(String(bytes, Charsets.UTF_8).contains("Café"))
    }

    @Test
    fun everyRowKeepsSevenColumnsWhenParsedBack() {
        val out = StringBuilder()
        ExpenseCsv.write(
            out,
            headers,
            listOf(
                transaction(categoryName = "a,b", note = "x\"y\r\nz"),
                transaction(categoryName = "@home", note = ",,,"),
            ),
            "USD",
        )

        val records = parse(out.toString().removePrefix(ExpenseCsv.BYTE_ORDER_MARK))

        assertEquals(3, records.size)
        assertTrue(records.all { it.size == 7 })
        assertEquals("a,b", records[1][4])
        assertEquals("x\"y\r\nz", records[1][5])
        assertEquals("'@home", records[2][4])
        assertEquals(",,,", records[2][5])
    }

    private fun parse(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                quoted && char == '"' && text.getOrNull(index + 1) == '"' -> {
                    field.append('"')
                    index++
                }
                quoted && char == '"' -> quoted = false
                quoted -> field.append(char)
                char == '"' -> quoted = true
                char == ',' -> {
                    fields.add(field.toString())
                    field.setLength(0)
                }
                char == '\r' && text.getOrNull(index + 1) == '\n' -> {
                    fields.add(field.toString())
                    field.setLength(0)
                    records.add(fields)
                    fields = mutableListOf()
                    index++
                }
                else -> field.append(char)
            }
            index++
        }
        return records
    }

    private fun transaction(
        id: Long = 1,
        type: ExpenseType = ExpenseType.EXPENSE,
        amount: Double = 12.5,
        categoryName: String = "Food",
        date: LocalDate = LocalDate.of(2026, 1, 15),
        note: String? = null,
        isFixed: Boolean = false,
    ) = ExpenseTransaction(
        id = id,
        type = type,
        amount = amount,
        categoryId = 1,
        categoryName = categoryName,
        categoryColor = "#FF7043",
        date = date,
        note = note,
        createdAt = 0,
        updatedAt = 0,
        isFixed = isFixed,
    )
}

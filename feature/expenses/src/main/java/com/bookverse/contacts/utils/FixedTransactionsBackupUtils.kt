package com.bookverse.contacts.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.bookverse.contacts.data.expenses.ExpenseTransaction
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.data.expenses.ExpensesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

object FixedTransactionsBackupUtils {

    data class ExportSummary(val count: Int)
    data class ImportSummary(val count: Int)

    @SuppressLint("ConstantLocale")
    private val readableDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)

    suspend fun exportFixedTransactions(
        context: Context,
        transactions: List<ExpenseTransaction>,
        uri: Uri
    ): ExportSummary = withContext(Dispatchers.IO) {
        val fixed = transactions.filter { it.isFixed }
        val transactionsArray = JSONArray()
        fixed.forEach { tx ->
            transactionsArray.put(JSONObject().apply {
                put("type", tx.type.name)
                put("amount", tx.amount)
                put("categoryId", tx.categoryId ?: JSONObject.NULL)
                put("categoryName", tx.categoryName)
                put("categoryColor", tx.categoryColor)
                put("date", tx.date.toString())
                put("note", tx.note ?: JSONObject.NULL)
                put("isFixed", tx.isFixed)
            })
        }

        val root = JSONObject().apply {
            put("type", "fixed_transactions")
            put("version", 1)
            put("generatedAt", readableDateFormat.format(Date()))
            put("transactions", transactionsArray)
        }

        val outputStream = context.contentResolver.openOutputStream(uri, "w")
            ?: context.contentResolver.openOutputStream(uri)
            ?: throw java.io.IOException("Unable to open output stream")

        outputStream.use { stream ->
            stream.write(root.toString(2).toByteArray(Charsets.UTF_8))
        }

        ExportSummary(fixed.size)
    }

    suspend fun importFixedTransactions(
        context: Context,
        store: ExpensesStore,
        uri: Uri
    ): ImportSummary = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw java.io.IOException("Unable to open input stream")

        if (bytes.isEmpty()) throw IllegalArgumentException("Import file is empty")

        val json = String(bytes, Charsets.UTF_8)
        val root = JSONObject(json)

        val fileType = root.optString("type")
        if (fileType != "fixed_transactions") {
            throw IllegalArgumentException("Invalid file format")
        }

        val transactionsArray = root.optJSONArray("transactions")
            ?: throw IllegalArgumentException("No transactions found in file")

        var imported = 0
        for (i in 0 until transactionsArray.length()) {
            val obj = transactionsArray.optJSONObject(i) ?: continue
            val type = ExpenseType.fromName(obj.optString("type"))
            val amount = obj.optDouble("amount", 0.0)
            if (amount <= 0.0) continue
            val categoryId = if (obj.isNull("categoryId")) null else obj.optLong("categoryId")
            val categoryName = obj.optString("categoryName", "")
            val categoryColor = obj.optString("categoryColor", "#607D8B")
            val dateStr = obj.optString("date", "")
            val date = try {
                LocalDate.parse(dateStr)
            } catch (_: Exception) {
                LocalDate.now()
            }
            val note = if (obj.isNull("note")) null else obj.optString("note")

            val transaction = ExpenseTransaction(
                id = 0,
                type = type,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                categoryColor = categoryColor,
                date = date,
                note = note,
                isFixed = true
            )
            store.upsertTransaction(transaction)
            imported++
        }

        if (imported == 0) throw IllegalArgumentException("No valid transactions found in file")

        ImportSummary(imported)
    }
}

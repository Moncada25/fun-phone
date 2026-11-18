package com.bookverse.contacts.domain.expenses

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.os.ConfigurationCompat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class ExpenseCurrency(
    val code: String,
    val symbol: String,
    val name: String,
    val fractionDigits: Int
)

object ExpenseCurrencyProvider {
    @SuppressLint("ConstantLocale")
    private val fallbackCurrency: Currency =
        runCatching { Currency.getInstance(Locale.getDefault()) }
            .getOrElse { Currency.getInstance("USD") }
    private val popularCodes = listOf(
        "USD", "EUR", "COP", "MXN", "ARS", "BRL", "CLP", "GBP", "CAD", "AUD",
        "JPY", "CNY", "INR", "CHF", "SEK", "NZD"
    )

    fun defaultFor(context: Context): ExpenseCurrency {
        val localeCurrency = runCatching { Currency.getInstance(Locale.getDefault()) }.getOrNull()
        return (localeCurrency ?: fallbackCurrency).toExpenseCurrency(context.resourceLocale())
    }

    fun fromCode(code: String?, locale: Locale = Locale.ENGLISH): ExpenseCurrency {
        val currency = runCatching { code?.let { Currency.getInstance(it) } }.getOrNull()
        return (currency ?: fallbackCurrency).toExpenseCurrency(locale)
    }

    fun availableOptions(locale: Locale = Locale.ENGLISH): List<ExpenseCurrency> {
        return popularCodes.mapNotNull { runCatching { Currency.getInstance(it) }.getOrNull() }
            .map { it.toExpenseCurrency(locale) }
    }

    private fun Currency.toExpenseCurrency(locale: Locale): ExpenseCurrency {
        val digits = defaultFractionDigits.takeIf { it >= 0 } ?: 2
        return ExpenseCurrency(
            code = currencyCode,
            symbol = symbol,
            name = getDisplayName(locale).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(locale) else it.toString()
            },
            fractionDigits = digits
        )
    }
}

private fun Context.resourceLocale(): Locale =
    ConfigurationCompat.getLocales(resources.configuration).get(0) ?: Locale.ENGLISH

fun Double.formatAsCurrency(currency: ExpenseCurrency, locale: Locale): String {
    val formatter = NumberFormat.getNumberInstance(locale)
    val digits = currency.fractionDigits.takeIf { it >= 0 } ?: 2
    formatter.maximumFractionDigits = digits
    formatter.minimumFractionDigits = if (this % 1.0 == 0.0) 0 else digits
    return formatter.format(this)
}

package com.bookverse.contacts.ui.screens.expenses

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import java.math.BigDecimal
import java.math.RoundingMode

fun sanitizeAmountInput(
    input: String,
    fractionDigits: Int,
    groupingSeparator: Char,
    decimalSeparator: Char
): String {
    val allowDecimal = fractionDigits > 0
    val builder = StringBuilder()
    var hasDecimal = false
    var decimalsCount = 0
    input.forEach { ch ->
        when {
            ch.isDigit() -> {
                if (hasDecimal) {
                    if (decimalsCount < fractionDigits) {
                        builder.append(ch)
                        decimalsCount++
                    }
                } else {
                    builder.append(ch)
                }
            }

            ch == groupingSeparator || ch == '\u00A0' || ch.isWhitespace() -> Unit

            allowDecimal && !hasDecimal && isDecimalCharacter(ch, decimalSeparator) -> {
                hasDecimal = true
                builder.append('.')
            }
        }
    }
    val result = builder.toString()
    return if (result == ".") "0" else result
}

fun parseAmountInput(text: String, currency: ExpenseCurrency): Double? {
    if (text.isBlank()) return null
    return runCatching {
        BigDecimal(text)
            .setScale(currency.fractionDigits.coerceAtLeast(0), RoundingMode.HALF_UP)
            .toDouble()
    }.getOrNull()
}

fun formatRawAmount(value: Double?, currency: ExpenseCurrency): String {
    if (value == null) return ""
    return if (currency.fractionDigits == 0) {
        value.toLong().toString()
    } else {
        BigDecimal.valueOf(value)
            .setScale(currency.fractionDigits, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }
}

private fun isDecimalCharacter(input: Char, decimalSeparator: Char): Boolean {
    return input == decimalSeparator || input == '.' || input == ','
}

class AmountVisualTransformation(
    private val groupingSeparator: Char,
    private val decimalSeparator: Char
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val raw = text.text
        val decimalIndex = raw.indexOf('.')
        val integerLength = if (decimalIndex == -1) raw.length else decimalIndex

        val output = StringBuilder()
        val originalToTransformed = IntArray(raw.length + 1)
        val transformedToOriginal = mutableListOf<Int>()
        transformedToOriginal.add(0)
        var transformedIndex = 0
        var processedIntegerDigits = 0
        var originalIndex = 0

        while (originalIndex < raw.length) {
            originalToTransformed[originalIndex] = transformedIndex
            val ch = raw[originalIndex]
            if (ch == '.') {
                output.append(decimalSeparator)
                originalIndex++
                transformedIndex++
                transformedToOriginal.add(originalIndex)
                continue
            }

            val inIntegerPart = originalIndex <= integerLength - 1
            output.append(ch)
            originalIndex++
            transformedIndex++
            transformedToOriginal.add(originalIndex)

            if (inIntegerPart) {
                processedIntegerDigits++
                val remaining = integerLength - processedIntegerDigits
                if (remaining > 0 && remaining % 3 == 0) {
                    output.append(groupingSeparator)
                    transformedIndex++
                    transformedToOriginal.add(originalIndex)
                }
            }
        }
        originalToTransformed[raw.length] = transformedIndex
        if (transformedToOriginal.size <= transformedIndex) {
            transformedToOriginal.add(originalIndex)
        }

        val formatted = AnnotatedString(output.toString())
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return originalToTransformed.getOrElse(offset) { formatted.length }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return if (offset in 0 until transformedToOriginal.size) {
                    transformedToOriginal[offset]
                } else {
                    raw.length
                }
            }
        }

        return TransformedText(formatted, mapping)
    }
}

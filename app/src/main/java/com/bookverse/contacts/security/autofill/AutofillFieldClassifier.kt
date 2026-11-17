package com.bookverse.contacts.security.autofill

import android.text.InputType
import java.util.Locale

data class AutofillFieldSnapshot(
    val hints: List<String> = emptyList(),
    val htmlType: String? = null,
    val htmlAutocomplete: String? = null,
    val htmlName: String? = null,
    val inputType: Int = 0,
    val idEntry: String? = null,
    val hintText: String? = null,
)

data class LoginFieldIndexes(
    val username: Int?,
    val password: Int?,
)

object AutofillFieldClassifier {
    private val usernameKeywords = listOf("user", "login", "email", "e-mail", "account", "identifier")
    private val unrelatedKeywords = listOf("search", "query", "otp", "captcha", "code", "coupon")

    fun classify(fields: List<AutofillFieldSnapshot>): LoginFieldIndexes {
        val passwordIndexes = fields.indices.filter { isPassword(fields[it]) }
        val password = passwordIndexes.firstOrNull { !isNewPassword(fields[it]) }
            ?: passwordIndexes.firstOrNull()
        val candidates = fields.indices.filter { it !in passwordIndexes }
        val username = if (password == null) {
            candidates.firstOrNull { hasUsernameSignal(fields[it]) }
        } else {
            val before = candidates.filter { it < password }.sortedDescending()
            before.firstOrNull { isExplicitUsername(fields[it]) }
                ?: candidates.firstOrNull { isExplicitUsername(fields[it]) }
                ?: before.firstOrNull { looksLikeUsername(fields[it]) }
                ?: before.firstOrNull { isPlainText(fields[it]) && !looksUnrelated(fields[it]) }
        }
        return LoginFieldIndexes(username = username, password = password)
    }

    private fun AutofillFieldSnapshot.normalizedHints(): List<String> =
        hints.map { it.lowercase(Locale.ROOT) } +
            htmlAutocomplete.orEmpty().lowercase(Locale.ROOT).split(' ').filter { it.isNotBlank() }

    private fun isPassword(field: AutofillFieldSnapshot): Boolean {
        if (field.normalizedHints().any { "password" in it }) return true
        if (field.htmlType.equals("password", ignoreCase = true)) return true
        val inputClass = field.inputType and InputType.TYPE_MASK_CLASS
        val variation = field.inputType and InputType.TYPE_MASK_VARIATION
        return when (inputClass) {
            InputType.TYPE_CLASS_TEXT ->
                variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                    variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }

    private fun isNewPassword(field: AutofillFieldSnapshot): Boolean =
        field.normalizedHints().any { it == "new-password" || it == "newpassword" }

    private fun hasUsernameSignal(field: AutofillFieldSnapshot): Boolean =
        field.normalizedHints().any { it == "username" || it == "newusername" }

    private fun isExplicitUsername(field: AutofillFieldSnapshot): Boolean {
        val hints = field.normalizedHints()
        return hints.any { it == "username" || it == "newusername" || it == "email" || it == "emailaddress" } ||
            field.htmlType.equals("email", ignoreCase = true)
    }

    private fun descriptors(field: AutofillFieldSnapshot): String =
        listOfNotNull(field.idEntry, field.htmlName, field.hintText)
            .joinToString(" ")
            .lowercase(Locale.ROOT)

    private fun looksUnrelated(field: AutofillFieldSnapshot): Boolean {
        val text = descriptors(field)
        return unrelatedKeywords.any { it in text }
    }

    private fun looksLikeUsername(field: AutofillFieldSnapshot): Boolean {
        val text = descriptors(field)
        return usernameKeywords.any { it in text } && !looksUnrelated(field)
    }

    private fun isPlainText(field: AutofillFieldSnapshot): Boolean {
        val inputClass = field.inputType and InputType.TYPE_MASK_CLASS
        return inputClass == InputType.TYPE_CLASS_TEXT || field.inputType == 0
    }
}

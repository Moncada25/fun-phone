package com.bookverse.contacts.security.autofill

import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Test

class AutofillFieldClassifierTest {
    private val textPassword = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    private val visiblePassword =
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    private val plainText = InputType.TYPE_CLASS_TEXT

    @Test
    fun usesAutofillHintsWhenPresent() {
        val fields = listOf(
            AutofillFieldSnapshot(hints = listOf("name")),
            AutofillFieldSnapshot(hints = listOf("emailAddress")),
            AutofillFieldSnapshot(hints = listOf("password")),
        )
        assertEquals(LoginFieldIndexes(username = 1, password = 2), AutofillFieldClassifier.classify(fields))
    }

    @Test
    fun usesHtmlAttributesForWebForms() {
        val fields = listOf(
            AutofillFieldSnapshot(htmlType = "text", htmlName = "q search"),
            AutofillFieldSnapshot(htmlType = "email", htmlName = "login"),
            AutofillFieldSnapshot(htmlType = "password", htmlAutocomplete = "current-password"),
        )
        assertEquals(LoginFieldIndexes(username = 1, password = 2), AutofillFieldClassifier.classify(fields))
    }

    @Test
    fun fallsBackToInputTypeAndThePrecedingTextField() {
        val fields = listOf(
            AutofillFieldSnapshot(inputType = plainText, idEntry = "field_a"),
            AutofillFieldSnapshot(inputType = textPassword, idEntry = "field_b"),
        )
        assertEquals(LoginFieldIndexes(username = 0, password = 1), AutofillFieldClassifier.classify(fields))
    }

    @Test
    fun prefersKeywordMatchOverNearestUnrelatedField() {
        val fields = listOf(
            AutofillFieldSnapshot(inputType = plainText, idEntry = "user_email"),
            AutofillFieldSnapshot(inputType = plainText, idEntry = "promo_code"),
            AutofillFieldSnapshot(inputType = textPassword),
        )
        assertEquals(LoginFieldIndexes(username = 0, password = 2), AutofillFieldClassifier.classify(fields))
    }

    @Test
    fun visiblePasswordInputTypeAloneIsNotAPasswordField() {
        val fields = listOf(
            AutofillFieldSnapshot(inputType = visiblePassword, idEntry = "username"),
            AutofillFieldSnapshot(inputType = textPassword),
        )
        assertEquals(LoginFieldIndexes(username = 0, password = 1), AutofillFieldClassifier.classify(fields))
    }

    @Test
    fun prefersCurrentPasswordOverNewPassword() {
        val fields = listOf(
            AutofillFieldSnapshot(htmlType = "password", htmlAutocomplete = "new-password"),
            AutofillFieldSnapshot(htmlType = "password", htmlAutocomplete = "current-password"),
        )
        assertEquals(1, AutofillFieldClassifier.classify(fields).password)
        val signup = listOf(
            AutofillFieldSnapshot(hints = listOf("username")),
            AutofillFieldSnapshot(hints = listOf("newPassword")),
        )
        assertEquals(LoginFieldIndexes(username = 0, password = 1), AutofillFieldClassifier.classify(signup))
    }

    @Test
    fun usernameOnlyStepRequiresAnExplicitUsernameSignal() {
        assertEquals(
            LoginFieldIndexes(username = 0, password = null),
            AutofillFieldClassifier.classify(listOf(AutofillFieldSnapshot(htmlAutocomplete = "username"))),
        )
        assertEquals(
            LoginFieldIndexes(username = null, password = null),
            AutofillFieldClassifier.classify(
                listOf(
                    AutofillFieldSnapshot(htmlType = "email", htmlName = "newsletter_email"),
                    AutofillFieldSnapshot(inputType = plainText, idEntry = "search"),
                ),
            ),
        )
    }

    @Test
    fun passwordWithoutAnyUsernameCandidateStillFills() {
        val fields = listOf(
            AutofillFieldSnapshot(inputType = plainText, hintText = "Search products"),
            AutofillFieldSnapshot(inputType = textPassword),
        )
        assertEquals(LoginFieldIndexes(username = null, password = 1), AutofillFieldClassifier.classify(fields))
        assertEquals(LoginFieldIndexes(null, null), AutofillFieldClassifier.classify(emptyList()))
    }
}

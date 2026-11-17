package com.bookverse.contacts.ui.components

import androidx.biometric.BiometricPrompt
import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricErrorPolicyTest {
    @Test
    fun cancellationErrorsCancelThePendingFlow() {
        val errors = listOf(
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_CANCELED,
        )

        errors.forEach { error ->
            assertEquals(BiometricErrorOutcome.CANCEL, classifyBiometricError(error))
        }
    }

    @Test
    fun lockoutErrorsFailThePendingFlow() {
        val errors = listOf(
            BiometricPrompt.ERROR_LOCKOUT,
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
        )

        errors.forEach { error ->
            assertEquals(BiometricErrorOutcome.FAIL, classifyBiometricError(error))
        }
    }
}

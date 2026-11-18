package com.bookverse.contacts.ui.components

import androidx.biometric.BiometricPrompt

enum class BiometricErrorOutcome {
    CANCEL,
    FAIL,
}

fun classifyBiometricError(errorCode: Int): BiometricErrorOutcome =
    when (errorCode) {
        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
        BiometricPrompt.ERROR_USER_CANCELED,
        BiometricPrompt.ERROR_CANCELED,
        -> BiometricErrorOutcome.CANCEL

        else -> BiometricErrorOutcome.FAIL
    }

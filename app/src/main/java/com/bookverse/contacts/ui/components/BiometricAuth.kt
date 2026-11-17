package com.bookverse.contacts.ui.components

import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@Composable
fun BiometricAuthLauncher(
    visible: Boolean,
    title: String,
    subtitle: String,
    negative: String,
    onSuccess: () -> Unit,
    onFail: () -> Unit,
    onCancel: () -> Unit,
) {
    if (!visible) return
    val context = LocalContext.current
    val activity = context as? FragmentActivity ?: return
    DisposableEffect(visible, activity) {
        var active = true
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult,
            ) {
                if (active) onSuccess()
            }

            override fun onAuthenticationFailed() = Unit

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!active) return
                when (classifyBiometricError(errorCode)) {
                    BiometricErrorOutcome.CANCEL -> onCancel()
                    BiometricErrorOutcome.FAIL -> onFail()
                }
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negative)
            .build()
        prompt.authenticate(info)
        onDispose { active = false; prompt.cancelAuthentication() }
    }
}

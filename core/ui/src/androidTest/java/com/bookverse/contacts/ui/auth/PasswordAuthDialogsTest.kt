package com.bookverse.contacts.ui.auth

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.PasswordAuthMethod
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class PasswordAuthDialogsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun validPinCompletesProtectedAction() {
        val actionCompleted = AtomicBoolean(false)
        showPinChallenge(expectedPin = "1234", onSuccess = { actionCompleted.set(true) })

        enterPin("1234")
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.confirm))
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { actionCompleted.get() }
        assertTrue(actionCompleted.get())
    }

    @Test
    fun invalidPinShowsAuthenticationError() {
        showPinChallenge(expectedPin = "1234")

        enterPin("9999")
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.confirm))
            .performScrollTo()
            .performClick()

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.auth_failed_title))
            .assertIsDisplayed()
    }

    @Test
    fun dismissingPinChallengeDoesNotCompleteProtectedAction() {
        val actionCompleted = AtomicBoolean(false)
        showPinChallenge(expectedPin = "1234", onSuccess = { actionCompleted.set(true) })

        composeRule.activity.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.waitForIdle()
        assertFalse(actionCompleted.get())
    }

    @Test
    fun biometricSuccessCompletesProtectedAction() {
        val actionCompleted = AtomicBoolean(false)
        val state = startBiometricChallenge { actionCompleted.set(true) }

        composeRule.runOnUiThread(state::onBiometricSuccess)

        assertTrue(actionCompleted.get())
        assertFalse(state.showBiometricPrompt)
    }

    @Test
    fun biometricCancellationDoesNotCompleteProtectedAction() {
        val actionCompleted = AtomicBoolean(false)
        val state = startBiometricChallenge { actionCompleted.set(true) }

        composeRule.runOnUiThread(state::onBiometricCancel)

        assertFalse(actionCompleted.get())
        assertFalse(state.showBiometricPrompt)
    }

    @Test
    fun biometricLockoutShowsTerminalErrorWithoutCompletingAction() {
        val actionCompleted = AtomicBoolean(false)
        val state = startBiometricChallenge { actionCompleted.set(true) }

        composeRule.runOnUiThread(state::onBiometricFail)

        assertFalse(actionCompleted.get())
        assertFalse(state.showBiometricPrompt)
        assertTrue(state.showBiometricErrorDialog)
    }

    private fun showPinChallenge(
        expectedPin: String,
        onSuccess: () -> Unit = {},
    ) {
        val appSettingsViewModel = AppSettingsViewModel().apply {
            setPasswordPin(composeRule.activity, expectedPin)
        }
        composeRule.setContent {
            MaterialTheme {
                val authState = rememberPasswordAuthState(appSettingsViewModel)
                LaunchedEffect(authState) {
                    authState.handleProtectedAction(
                        passwordManagerEnabled = true,
                        authMethod = PasswordAuthMethod.PIN,
                        onSuccess = onSuccess,
                    )
                }
                PasswordAuthDialogs(
                    state = authState,
                )
            }
        }
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.auth_enter_pin))
            .assertIsDisplayed()
    }

    private fun enterPin(pin: String) {
        pin.forEach { digit ->
            composeRule
                .onNodeWithText(digit.toString())
                .performScrollTo()
                .performClick()
        }
    }

    private fun startBiometricChallenge(onSuccess: () -> Unit): PasswordAuthState {
        val state = PasswordAuthState(
            context = composeRule.activity,
            appSettingsViewModel = AppSettingsViewModel(),
        )
        composeRule.runOnUiThread {
            state.handleProtectedAction(
                passwordManagerEnabled = true,
                authMethod = PasswordAuthMethod.BIOMETRIC,
                onSuccess = onSuccess,
            )
        }
        assertTrue(state.showBiometricPrompt)
        return state
    }
}

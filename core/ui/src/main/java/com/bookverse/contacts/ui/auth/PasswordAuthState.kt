package com.bookverse.contacts.ui.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.bookverse.contacts.security.PinResult
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.ui.components.BiometricAuthLauncher
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.OptionsDialog
import com.bookverse.contacts.ui.components.PinInputDialog
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.PasswordAuthMethod

private enum class PasswordAuthPurpose {
    SETUP,
    AUTHENTICATE,
    REMOVAL
}

@Stable
class PasswordAuthState internal constructor(
    context: Context,
    private val appSettingsViewModel: AppSettingsViewModel,
    private val scope: CoroutineScope = appSettingsViewModel.viewModelScope,
) {
    private val appContext = context.applicationContext
    private var verificationJob: Job? = null
    private var verificationGeneration = 0
    internal var isVerifying by mutableStateOf(false)
        private set
    internal var storageFailure by mutableStateOf(false)
        private set
    internal var waitSeconds by mutableStateOf(0)
        private set

    fun cancelAuthentication() {
        showAuthMethodDialog = false
        showPinSetupDialog = false
        showPinConfirmDialog = false
        showPinAuthDialog = false
        showPinRemovalDialog = false
        showBiometricPrompt = false
        showAuthErrorDialog = false
        showBiometricErrorDialog = false
        showPinMismatchDialog = false
        cancelFlow()
    }

    private var pendingAction by mutableStateOf<(() -> Unit)?>(null)
    private var pendingRemovalAction by mutableStateOf<(() -> Unit)?>(null)
    private var purpose by mutableStateOf<PasswordAuthPurpose?>(null)
    private var firstPin by mutableStateOf("")

    internal var showAuthMethodDialog by mutableStateOf(false)
        private set
    internal var showPinSetupDialog by mutableStateOf(false)
        private set
    internal var showPinConfirmDialog by mutableStateOf(false)
        private set
    internal var showPinAuthDialog by mutableStateOf(false)
        private set
    internal var showPinRemovalDialog by mutableStateOf(false)
        private set
    internal var showPinMismatchDialog by mutableStateOf(false)
        private set
    internal var showAuthErrorDialog by mutableStateOf(false)
        private set
    internal var showBiometricPrompt by mutableStateOf(false)
        private set
    internal var showBiometricErrorDialog by mutableStateOf(false)
        private set

    fun handleProtectedAction(
        passwordManagerEnabled: Boolean,
        authMethod: PasswordAuthMethod?,
        onSuccess: () -> Unit
    ) {
        cancelAuthentication()
        if (!passwordManagerEnabled) return
        if (authMethod == null) {
            startSetupFlow(onConfigured = onSuccess)
            return
        }
        pendingAction = onSuccess
        purpose = PasswordAuthPurpose.AUTHENTICATE
        when (authMethod) {
            PasswordAuthMethod.BIOMETRIC -> showBiometricPrompt = true
            PasswordAuthMethod.PIN -> showPinAuthDialog = true
        }
    }

    fun startSetupFlow(onConfigured: (() -> Unit)? = null) {
        cancelAuthentication()
        pendingAction = onConfigured
        pendingRemovalAction = null
        purpose = PasswordAuthPurpose.SETUP
        if (isBiometricAvailable()) {
            showAuthMethodDialog = true
        } else {
            launchPinSetup()
        }
    }

    fun requestRemoval(
        authMethod: PasswordAuthMethod?,
        onCleared: (() -> Unit)? = null
    ) {
        cancelAuthentication()
        if (authMethod == null) return
        pendingAction = null
        pendingRemovalAction = onCleared
        purpose = PasswordAuthPurpose.REMOVAL
        when (authMethod) {
            PasswordAuthMethod.BIOMETRIC -> showBiometricPrompt = true
            PasswordAuthMethod.PIN -> showPinRemovalDialog = true
        }
    }

    internal fun onAuthMethodSelected(index: Int) {
        showAuthMethodDialog = false
        if (index == 0 && isBiometricAvailable()) {
            showBiometricPrompt = true
        } else {
            launchPinSetup()
        }
    }

    internal fun dismissAuthMethodDialog() {
        showAuthMethodDialog = false
        cancelFlow()
    }

    internal fun onPinSetupSubmit(pin: String) {
        firstPin = pin
        showPinSetupDialog = false
        showPinConfirmDialog = true
    }

    internal fun dismissPinSetupDialog() {
        showPinSetupDialog = false
        firstPin = ""
        cancelFlow()
    }

    internal fun onPinConfirmSubmit(pin: String) {
        if (pin == firstPin) {
            completePinSetup(pin)
        } else {
            showPinConfirmDialog = false
            firstPin = ""
            showPinMismatchDialog = true
        }
    }

    internal fun dismissPinConfirmDialog() {
        showPinConfirmDialog = false
        firstPin = ""
        cancelFlow()
    }

    internal fun onPinAuthSubmit(pin: String) = verifyPin(pin, removal = false)

    private fun verifyPin(pin: String, removal: Boolean) {
        if (isVerifying) return
        isVerifying = true
        waitSeconds = 0
        val generation = ++verificationGeneration
        verificationJob = scope.launch {
            try {
                val result = appSettingsViewModel.validatePin(appContext, pin)
                if (generation != verificationGeneration) return@launch
                showPinAuthDialog = false
                showPinRemovalDialog = false
                isVerifying = false
                if (result == PinResult.Valid) {
                    if (removal) performRemoval(generation) else executePendingAction()
                } else {
                    waitSeconds = (result as? PinResult.Locked)?.seconds ?: 0
                    showAuthErrorDialog = true
                    pendingAction = null
                    pendingRemovalAction = null
                    purpose = null
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                storageFailure = true
                showPinAuthDialog = false
                showPinRemovalDialog = false
                showAuthErrorDialog = true
                pendingAction = null
                pendingRemovalAction = null
                purpose = null
            } finally {
                if (generation == verificationGeneration) isVerifying = false
            }
        }
    }

    internal fun dismissPinAuthDialog() {
        showPinAuthDialog = false
        cancelFlow()
    }

    internal fun onPinRemovalSubmit(pin: String) = verifyPin(pin, removal = true)

    internal fun dismissPinRemovalDialog() {
        showPinRemovalDialog = false
        cancelFlow()
    }

    internal fun dismissPinMismatchDialog() {
        showPinMismatchDialog = false
    }

    internal fun dismissAuthErrorDialog() {
        showAuthErrorDialog = false
    }

    internal fun onBiometricSuccess() {
        showBiometricPrompt = false
        val authenticatedPurpose = purpose ?: return
        if (authenticatedPurpose == PasswordAuthPurpose.AUTHENTICATE) {
            executePendingAction()
            return
        }
        val generation = ++verificationGeneration
        isVerifying = true
        verificationJob = scope.launch {
            try {
                if (authenticatedPurpose == PasswordAuthPurpose.SETUP) {
                    appSettingsViewModel.configureBiometricAuth(appContext)
                    if (generation == verificationGeneration) executePendingAction()
                } else {
                    performRemoval(generation)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                storageFailure = true
                showAuthErrorDialog = true
                pendingAction = null
                pendingRemovalAction = null
                purpose = null
            } finally {
                if (generation == verificationGeneration) isVerifying = false
            }
        }
    }

    internal fun onBiometricFail() {
        showBiometricPrompt = false
        showBiometricErrorDialog = true
        cancelFlow()
    }

    internal fun onBiometricCancel() {
        showBiometricPrompt = false
        cancelFlow()
    }

    internal fun dismissBiometricErrorDialog() {
        showBiometricErrorDialog = false
    }

    private fun launchPinSetup() {
        firstPin = ""
        showPinSetupDialog = true
    }

    private fun completePinSetup(pin: String) {
        if (isVerifying) return
        isVerifying = true
        firstPin = ""
        val generation = ++verificationGeneration
        verificationJob = scope.launch {
            try {
                appSettingsViewModel.configurePasswordPin(appContext, pin)
                if (generation != verificationGeneration) return@launch
                showPinConfirmDialog = false
                isVerifying = false
                executePendingAction()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                storageFailure = true
                showPinConfirmDialog = false
                showAuthErrorDialog = true
                pendingAction = null
                purpose = null
            } finally {
                if (generation == verificationGeneration) isVerifying = false
            }
        }
    }

    private fun executePendingAction() {
        val action = pendingAction
        pendingAction = null
        purpose = null
        appSettingsViewModel.unlockProtectedSession()
        action?.invoke()
    }

    private suspend fun performRemoval(generation: Int) {
        appSettingsViewModel.clearPasswordAuth(appContext)
        if (generation != verificationGeneration) return
        val action = pendingRemovalAction
        pendingRemovalAction = null
        purpose = null
        action?.invoke()
    }

    private fun cancelFlow() {
        verificationGeneration++
        waitSeconds = 0
        storageFailure = false
        verificationJob?.cancel()
        isVerifying = false
        pendingAction = null
        pendingRemovalAction = null
        purpose = null
        firstPin = ""
    }

    private fun isBiometricAvailable(): Boolean {
        val result = BiometricManager.from(appContext).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }
}

@Composable
fun rememberPasswordAuthState(
    appSettingsViewModel: AppSettingsViewModel
): PasswordAuthState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember(appSettingsViewModel, context) {
        PasswordAuthState(context, appSettingsViewModel, scope)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, state) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                state.cancelAuthentication()
                appSettingsViewModel.lockProtectedSession()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); state.cancelAuthentication() }
    }
    return state
}

@Composable
fun PasswordAuthDialogs(
    state: PasswordAuthState,
) {
    if (state.showAuthMethodDialog) {
        OptionsDialog(
            title = stringResource(R.string.auth_method_choose),
            options = listOf(
                stringResource(R.string.auth_method_biometric),
                stringResource(R.string.auth_method_pin)
            ),
            onOptionSelected = state::onAuthMethodSelected,
            onDismiss = state::dismissAuthMethodDialog,
        )
    }

    if (state.showPinSetupDialog) {
        PinInputDialog(
            title = stringResource(R.string.auth_set_pin),
            onDismiss = state::dismissPinSetupDialog,
            onSubmit = state::onPinSetupSubmit,
        )
    }

    if (state.showPinConfirmDialog) {
        PinInputDialog(
            enabled = !state.isVerifying,
            title = stringResource(if (state.isVerifying) R.string.auth_verifying else R.string.auth_confirm_pin),
            onDismiss = state::dismissPinConfirmDialog,
            onSubmit = state::onPinConfirmSubmit,
        )
    }

    if (state.showPinAuthDialog) {
        PinInputDialog(
            enabled = !state.isVerifying,
            title = stringResource(if (state.isVerifying) R.string.auth_verifying else R.string.auth_enter_pin),
            onDismiss = state::dismissPinAuthDialog,
            onSubmit = state::onPinAuthSubmit,
        )
    }

    if (state.showPinRemovalDialog) {
        PinInputDialog(
            enabled = !state.isVerifying,
            title = stringResource(if (state.isVerifying) R.string.auth_verifying else R.string.auth_enter_pin),
            onDismiss = state::dismissPinRemovalDialog,
            onSubmit = state::onPinRemovalSubmit,
        )
    }

    if (state.showPinMismatchDialog) {
        MessageDialog(
            title = stringResource(R.string.auth_failed_title),
            message1 = stringResource(R.string.auth_pin_mismatch),
            onDismiss = state::dismissPinMismatchDialog,
            onButtonClick = state::dismissPinMismatchDialog,
        )
    }

    if (state.showAuthErrorDialog) {
        MessageDialog(
            title = stringResource(R.string.auth_failed_title),
            message1 = if (state.storageFailure) stringResource(R.string.auth_failed_message) else if (state.waitSeconds > 0) pluralStringResource(R.plurals.auth_pin_wait, state.waitSeconds, state.waitSeconds) else stringResource(R.string.auth_pin_invalid),
            onDismiss = state::dismissAuthErrorDialog,
            onButtonClick = state::dismissAuthErrorDialog,
        )
    }

    BiometricAuthLauncher(
        visible = state.showBiometricPrompt,
        title = stringResource(R.string.security_biometrics),
        subtitle = stringResource(R.string.auth_required),
        negative = stringResource(R.string.cancel),
        onSuccess = state::onBiometricSuccess,
        onFail = state::onBiometricFail,
        onCancel = state::onBiometricCancel
    )

    if (state.showBiometricErrorDialog) {
        MessageDialog(
            title = stringResource(R.string.auth_failed_title),
            message1 = stringResource(R.string.auth_failed_message),
            onDismiss = state::dismissBiometricErrorDialog,
            onButtonClick = state::dismissBiometricErrorDialog,
        )
    }
}

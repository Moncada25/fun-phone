package com.bookverse.contacts.ui.auth

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.bookverse.contacts.R
import com.bookverse.contacts.security.AppLockPolicy
import com.bookverse.contacts.security.AppLockSession
import com.bookverse.contacts.security.AppLockSettings
import com.bookverse.contacts.security.AppLockSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map

private const val DEVICE_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
private const val CALL_SCREEN_GRACE_MILLIS = 600L

fun canUseDeviceAuthentication(activity: FragmentActivity): Boolean =
    BiometricManager.from(activity).canAuthenticate(DEVICE_AUTHENTICATORS) ==
        BiometricManager.BIOMETRIC_SUCCESS

fun launchDeviceAuthentication(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onFinished: () -> Unit = {},
): BiometricPrompt {
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onFinished()
            onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            onFinished()
        }
    }
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(DEVICE_AUTHENTICATORS)
        .build()
    return BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).also {
        it.authenticate(info)
    }
}

@Composable
fun AppLockGate(
    onCallScreen: Boolean,
    callActive: Boolean,
    onReturnToCall: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    val enabledFlow = remember(context) {
        AppLockSettings.enabled(context).map<Boolean, Boolean?> { it }
    }
    val enabled by enabledFlow.collectAsStateWithLifecycle(
        initialValue = null,
        minActiveState = Lifecycle.State.CREATED,
    )
    val locked by AppLockSession.locked.collectAsStateWithLifecycle(
        minActiveState = Lifecycle.State.CREATED,
    )
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    var callGraceElapsed by remember { mutableStateOf(false) }
    var promptInFlight by remember { mutableStateOf(false) }
    var autoPrompted by remember { mutableStateOf(false) }
    var manualAttempt by remember { mutableIntStateOf(0) }
    val activePrompt = remember { mutableStateOf<BiometricPrompt?>(null) }
    val currentOnReturnToCall by rememberUpdatedState(onReturnToCall)

    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !activity.isChangingConfigurations) {
                AppLockSession.onStopped(SystemClock.elapsedRealtime())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(callActive, onCallScreen) {
        callGraceElapsed = false
        if (callActive && !onCallScreen) {
            delay(CALL_SCREEN_GRACE_MILLIS)
            callGraceElapsed = true
        }
    }

    LaunchedEffect(callActive) {
        if (callActive && enabled == true && locked) {
            activePrompt.value?.cancelAuthentication()
            currentOnReturnToCall()
        }
    }

    val surface = AppLockPolicy.surface(enabled, locked, onCallScreen, callActive, callGraceElapsed)
    val title = stringResource(R.string.app_lock_prompt_title)
    val subtitle = stringResource(R.string.app_lock_prompt_subtitle)

    LaunchedEffect(surface, lifecycleState, manualAttempt) {
        if (surface != AppLockSurface.LOCK) {
            autoPrompted = false
            return@LaunchedEffect
        }
        if (lifecycleState != Lifecycle.State.RESUMED || promptInFlight) return@LaunchedEffect
        if (!canUseDeviceAuthentication(activity)) {
            AppLockSession.unlock()
            return@LaunchedEffect
        }
        if (autoPrompted && manualAttempt == 0) return@LaunchedEffect
        autoPrompted = true
        manualAttempt = 0
        promptInFlight = true
        activePrompt.value = launchDeviceAuthentication(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = AppLockSession::unlock,
            onFinished = {
                promptInFlight = false
                activePrompt.value = null
            },
        )
    }

    if (surface == AppLockSurface.NONE) return
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        BackHandler { activity.moveTaskToBack(true) }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (surface == AppLockSurface.LOCK) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.app_lock_locked_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.app_lock_locked_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = { manualAttempt++ }, enabled = !promptInFlight) {
                        Text(stringResource(R.string.app_lock_unlock_action))
                    }
                    if (callActive) {
                        OutlinedButton(onClick = onReturnToCall) {
                            Text(stringResource(R.string.app_lock_return_to_call))
                        }
                    }
                }
            }
        }
    }
}

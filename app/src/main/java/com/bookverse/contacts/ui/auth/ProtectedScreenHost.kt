package com.bookverse.contacts.ui.auth

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.WindowManager
import android.view.Window
import java.util.WeakHashMap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.utils.SecondaryScreen

/** Owns the visibility boundary; state restoration cannot bypass authentication. */
@Composable
fun ProtectedScreenHost(
    screen: SecondaryScreen,
    lockedNote: Boolean,
    settings: AppSettingsViewModel,
    passwordAuthState: PasswordAuthState,
    onCancel: () -> Unit,
    onLock: () -> Unit,
    content: @Composable () -> Unit,
) {
    val unlocked by settings.protectedSessionUnlocked.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.CREATED)
    val sensitiveLock by settings.sensitiveSectionsLockEnabled.collectAsStateWithLifecycle()
    val authMethod by settings.passwordAuthMethod.collectAsStateWithLifecycle()
    val enabled by settings.passwordManagerEnabled.collectAsStateWithLifecycle()
    val vault = screen in setOf(SecondaryScreen.PASSWORD_LIST, SecondaryScreen.PASSWORDS_NEW, SecondaryScreen.PASSWORD_FOLDERS, SecondaryScreen.PASSWORD_RECYCLE_BIN)
    val sensitive = (lockedNote && screen in setOf(SecondaryScreen.NOTE, SecondaryScreen.CHECKLIST, SecondaryScreen.REMINDER, SecondaryScreen.RECORDING)) || vault || (sensitiveLock && screen in setOf(SecondaryScreen.EXPENSES, SecondaryScreen.MENSTRUAL_CALENDAR))
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val stateHolder = rememberSaveableStateHolder()
    DisposableEffect(sensitive, lifecycle) {
        val window = (context as? Activity)?.window
        if (sensitive && window != null) SecureWindows.acquire(window)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { settings.lockProtectedSession(); onLock() }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { settings.lockProtectedSession(); onLock() }
        }
        lifecycle.addObserver(observer)
        ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            lifecycle.removeObserver(observer)
            context.unregisterReceiver(receiver)
            if (sensitive && window != null) SecureWindows.release(window)
        }
    }
    LaunchedEffect(sensitive, unlocked) {
        if (sensitive && !unlocked && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            passwordAuthState.handleProtectedAction(if (vault) enabled else true, authMethod) {}
        }
    }
    if (!sensitive || (unlocked && (!vault || enabled))) {
        stateHolder.SaveableStateProvider(screen.name) { content() }
    } else {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(stringResource(R.string.auth_required))
                    TextButton(onClick = { passwordAuthState.handleProtectedAction(if (vault) enabled else true, authMethod) {} }) {
                        Text(stringResource(R.string.auth_enter_pin))
                    }
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }
}

// AnimatedContent may keep two protected destinations alive during a transition.
private object SecureWindows {
    private data class Lease(var count: Int, val alreadySecure: Boolean)
    private val leases = WeakHashMap<Window, Lease>()
    fun acquire(window: Window) {
        val lease = leases.getOrPut(window) {
            Lease(0, window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        }
        lease.count++
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    fun release(window: Window) {
        val lease = leases[window] ?: return
        lease.count--
        if (lease.count == 0) {
            if (!lease.alreadySecure) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            leases.remove(window)
        }
    }
}

package com.bookverse.contacts.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.feature.settings.R
import com.bookverse.contacts.security.AppLockSession
import com.bookverse.contacts.security.AppLockSettings
import com.bookverse.contacts.ui.auth.canUseDeviceAuthentication
import com.bookverse.contacts.ui.auth.launchDeviceAuthentication
import com.bookverse.contacts.ui.components.SnackbarType
import kotlinx.coroutines.launch
import com.bookverse.contacts.core.ui.R as CoreUiR

@Immutable
internal data class AppLockToggle(
    val enabled: Boolean,
    val onChange: (Boolean) -> Unit,
)

@Composable
internal fun rememberAppLockToggle(onMessage: (String, SnackbarType) -> Unit): AppLockToggle {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val currentOnMessage by rememberUpdatedState(onMessage)
    val appLockFlow = remember(context) { AppLockSettings.enabled(context) }
    val enabled by appLockFlow.collectAsStateWithLifecycle(initialValue = false)
    val onChange: (Boolean) -> Unit = remember(context, resources, scope) {
        { enable ->
            val activity = context as? FragmentActivity
            when {
                activity == null -> Unit
                !canUseDeviceAuthentication(activity) -> if (enable) {
                    currentOnMessage(resources.getString(R.string.app_lock_requires_screen_lock), SnackbarType.Warning)
                } else {
                    scope.launch { AppLockSettings.setEnabled(context, false) }
                }
                else -> launchDeviceAuthentication(
                    activity = activity,
                    title = resources.getString(CoreUiR.string.app_lock_prompt_title),
                    subtitle = resources.getString(CoreUiR.string.app_lock_prompt_subtitle),
                    onSuccess = {
                        AppLockSession.unlock()
                        scope.launch { AppLockSettings.setEnabled(context, enable) }
                    },
                )
            }
        }
    }
    return AppLockToggle(enabled = enabled, onChange = onChange)
}

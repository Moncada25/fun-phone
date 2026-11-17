package com.bookverse.contacts.ui.screens.passwords

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.components.MessageDialog

private enum class AutofillStatus { UNSUPPORTED, DISABLED, ENABLED }

@Composable
internal fun AutofillServiceDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    fun readStatus(): AutofillStatus {
        val manager = context.getSystemService(AutofillManager::class.java)
        return when {
            manager == null || !manager.isAutofillSupported -> AutofillStatus.UNSUPPORTED
            manager.hasEnabledAutofillServices() -> AutofillStatus.ENABLED
            else -> AutofillStatus.DISABLED
        }
    }
    var status by remember { mutableStateOf(readStatus()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { status = readStatus() }

    fun openSystemSettings() {
        val request = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
            .setData("package:${context.packageName}".toUri())
        try {
            context.startActivity(request)
        } catch (_: ActivityNotFoundException) {
            status = AutofillStatus.UNSUPPORTED
        }
    }

    MessageDialog(
        title = stringResource(R.string.autofill_option_title),
        message1 = stringResource(
            when (status) {
                AutofillStatus.ENABLED -> R.string.autofill_option_enabled
                AutofillStatus.DISABLED -> R.string.autofill_option_disabled
                AutofillStatus.UNSUPPORTED -> R.string.autofill_option_unsupported
            },
        ),
        buttonText = stringResource(
            if (status == AutofillStatus.DISABLED) R.string.settings else R.string.understood,
        ),
        onButtonClick = {
            if (status == AutofillStatus.DISABLED) openSystemSettings() else onDismiss()
        },
        secondaryButtonText = if (status == AutofillStatus.DISABLED) stringResource(R.string.cancel) else null,
        onSecondaryButtonClick = onDismiss,
        onDismiss = onDismiss,
    )
}

package com.bookverse.contacts.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.backup.AppBackupManager
import com.bookverse.contacts.data.backup.AppBackupSummary
import com.bookverse.contacts.domain.backup.BackupArchive
import com.bookverse.contacts.domain.backup.BackupFormatException
import com.bookverse.contacts.domain.backup.BackupPassphraseException
import com.bookverse.contacts.security.AppLockSession
import com.bookverse.contacts.security.AppLockSettings
import com.bookverse.contacts.ui.auth.PasswordAuthState
import com.bookverse.contacts.ui.auth.canUseDeviceAuthentication
import com.bookverse.contacts.ui.auth.launchDeviceAuthentication
import com.bookverse.contacts.ui.components.ButtonSheetScaffold
import com.bookverse.contacts.ui.components.LoaderDialog
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.SettingSwitch
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class BackupStep { IDLE, EXPORT_PASSPHRASE, RESTORE_PASSPHRASE, WORKING }

@Composable
internal fun ProtectionBackupHost(
    visible: Boolean,
    onDismiss: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    passwordAuthState: PasswordAuthState,
    onMessage: (String, SnackbarType) -> Unit,
    manager: AppBackupManager,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val appLockFlow = remember(context) { AppLockSettings.enabled(context) }
    val appLockEnabled by appLockFlow.collectAsStateWithLifecycle(initialValue = false)
    val vaultEnabled by appSettingsViewModel.passwordManagerEnabled.collectAsStateWithLifecycle()
    val vaultAuthMethod by appSettingsViewModel.passwordAuthMethod.collectAsStateWithLifecycle()
    var step by remember { mutableStateOf(BackupStep.IDLE) }
    var pendingPassphrase by remember { mutableStateOf<CharArray?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var resultSummary by remember { mutableStateOf<Pair<Boolean, AppBackupSummary>?>(null) }

    fun failureMessage(error: Exception, restoring: Boolean): String = resources.getString(
        when {
            error is BackupPassphraseException -> R.string.full_backup_wrong_passphrase
            error is BackupFormatException -> R.string.full_backup_not_a_backup
            restoring -> R.string.full_backup_restore_failed
            else -> R.string.full_backup_export_failed
        },
    )

    fun withVaultAccess(action: () -> Unit) {
        if (vaultEnabled && vaultAuthMethod != null) {
            passwordAuthState.handleProtectedAction(true, vaultAuthMethod, action)
        } else {
            action()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupArchive.MIME_TYPE),
    ) { uri ->
        val passphrase = pendingPassphrase
        pendingPassphrase = null
        if (uri == null || passphrase == null) {
            passphrase?.fill('\u0000')
            step = BackupStep.IDLE
            return@rememberLauncherForActivityResult
        }
        step = BackupStep.WORKING
        scope.launch {
            try {
                resultSummary = false to manager.export(uri, passphrase, includeVault = vaultEnabled)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                onMessage(failureMessage(error, restoring = false), SnackbarType.Error)
            } finally {
                passphrase.fill('\u0000')
                step = BackupStep.IDLE
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            withVaultAccess { step = BackupStep.RESTORE_PASSPHRASE }
        }
    }

    fun toggleAppLock(enable: Boolean) {
        val activity = context as? FragmentActivity ?: return
        if (!canUseDeviceAuthentication(activity)) {
            if (enable) {
                onMessage(resources.getString(R.string.app_lock_requires_screen_lock), SnackbarType.Warning)
            } else {
                scope.launch { AppLockSettings.setEnabled(context, false) }
            }
            return
        }
        launchDeviceAuthentication(
            activity = activity,
            title = resources.getString(R.string.app_lock_prompt_title),
            subtitle = resources.getString(R.string.app_lock_prompt_subtitle),
            onSuccess = {
                AppLockSession.unlock()
                scope.launch { AppLockSettings.setEnabled(context, enable) }
            },
        )
    }

    if (visible) {
        ButtonSheetScaffold(
            title = stringResource(R.string.protection_backup_title),
            onDismiss = onDismiss,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.app_lock_setting_title), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.app_lock_setting_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    SettingSwitch(checked = appLockEnabled, onCheckedChange = ::toggleAppLock)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.full_backup_title), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.full_backup_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { withVaultAccess { step = BackupStep.EXPORT_PASSPHRASE } },
                        enabled = step == BackupStep.IDLE,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.full_backup_create))
                    }
                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                        enabled = step == BackupStep.IDLE,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.full_backup_restore))
                    }
                }
            }
        }
    }

    when (step) {
        BackupStep.EXPORT_PASSPHRASE -> PassphraseDialog(
            title = stringResource(R.string.full_backup_passphrase_new_title),
            message = stringResource(R.string.full_backup_passphrase_new_message),
            confirm = true,
            onDismiss = { step = BackupStep.IDLE },
            onSubmit = { passphrase ->
                pendingPassphrase = passphrase
                step = BackupStep.WORKING
                exportLauncher.launch("funphone-${LocalDate.now()}.${BackupArchive.FILE_EXTENSION}")
            },
        )
        BackupStep.RESTORE_PASSPHRASE -> PassphraseDialog(
            title = stringResource(R.string.full_backup_passphrase_enter_title),
            message = stringResource(R.string.full_backup_restore_message),
            confirm = false,
            onDismiss = {
                pendingRestoreUri = null
                step = BackupStep.IDLE
            },
            onSubmit = { passphrase ->
                val uri = pendingRestoreUri
                pendingRestoreUri = null
                if (uri == null) {
                    step = BackupStep.IDLE
                    return@PassphraseDialog
                }
                step = BackupStep.WORKING
                scope.launch {
                    try {
                        resultSummary = true to manager.restore(uri, passphrase, restoreVault = vaultEnabled)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        onMessage(failureMessage(error, restoring = true), SnackbarType.Error)
                    } finally {
                        passphrase.fill('\u0000')
                        step = BackupStep.IDLE
                    }
                }
            },
        )
        BackupStep.WORKING -> if (pendingPassphrase == null) {
            LoaderDialog(message = stringResource(R.string.full_backup_working))
        }
        BackupStep.IDLE -> Unit
    }

    resultSummary?.let { (restored, summary) ->
        MessageDialog(
            title = stringResource(
                if (restored) R.string.full_backup_restore_done_title else R.string.full_backup_export_done_title,
            ),
            message1 = stringResource(
                R.string.full_backup_summary,
                summary.passwords,
                summary.notes,
                summary.reminders + summary.recordings,
                summary.transactions,
                summary.qrCodes,
            ),
            message2 = if (!summary.includesVault) stringResource(R.string.full_backup_without_vault) else "",
            onButtonClick = { resultSummary = null },
            onDismiss = { resultSummary = null },
        )
    }
}

@Composable
private fun PassphraseDialog(
    title: String,
    message: String,
    confirm: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (CharArray) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var repeated by remember { mutableStateOf("") }
    val longEnough = passphrase.length >= BackupArchive.MIN_PASSPHRASE_LENGTH
    val matches = !confirm || passphrase == repeated
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(message, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.full_backup_passphrase_label)) },
                    supportingText = {
                        Text(
                            pluralStringResource(
                                R.plurals.full_backup_passphrase_min,
                                BackupArchive.MIN_PASSPHRASE_LENGTH,
                                BackupArchive.MIN_PASSPHRASE_LENGTH,
                            ),
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                )
                if (confirm) {
                    OutlinedTextField(
                        value = repeated,
                        onValueChange = { repeated = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.full_backup_passphrase_repeat)) },
                        isError = repeated.isNotEmpty() && !matches,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Button(
                        enabled = longEnough && matches,
                        onClick = { onSubmit(passphrase.toCharArray()) },
                    ) {
                        Text(stringResource(R.string.action_continue))
                    }
                }
            }
        }
    }
}

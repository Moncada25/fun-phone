package com.bookverse.contacts.ui.screens.passwords

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.feature.vault.R
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.PasswordsViewModel
import com.bookverse.contacts.utils.PasswordJsonUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class PasswordBackupActions(val importBackup: () -> Unit, val exportBackup: () -> Unit)

/** Register above the auth boundary: the document picker backgrounds and locks the vault. */
@Composable
fun rememberPasswordBackupActions(
    active: Boolean,
    settings: AppSettingsViewModel,
    passwords: PasswordsViewModel,
): PasswordBackupActions {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val unlocked by settings.protectedSessionUnlocked.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.CREATED)
    val enabled by settings.passwordManagerEnabled.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.CREATED)
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var importing by rememberSaveable { mutableStateOf(false) }
    var transferJob by remember { mutableStateOf<Job?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importing = true
        pendingUri = uri?.toString()
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        importing = false
        pendingUri = uri?.toString()
    }
    LaunchedEffect(pendingUri, unlocked, enabled, active) {
        if (!active || !unlocked || !enabled) {
            transferJob?.cancel()
            if (!active || !enabled) pendingUri = null
            return@LaunchedEffect
        }
        val uri = pendingUri?.let(Uri::parse) ?: return@LaunchedEffect
        if (!settings.protectedSessionUnlocked.value) return@LaunchedEffect
        val isImport = importing
        pendingUri = null
        passwords.unlock()
        transferJob = scope.launch {
            try {
                if (isImport) {
                    val result = PasswordJsonUtils.importPasswords(context, uri, passwords)
                    val message = when {
                        result.skipped > 0 -> resources.getString(R.string.passwords_json_import_summary, result.imported, result.skipped)
                        result.imported > 0 -> resources.getString(R.string.passwords_json_import_success, result.imported)
                        else -> null
                    }
                    message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                } else {
                    PasswordJsonUtils.exportPasswords(context, passwords.getAllForBackup(), passwords.getFoldersForBackup(), uri)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                Toast.makeText(context, resources.getString(if (isImport) R.string.passwords_json_import_error else R.string.passwords_json_saved_error), Toast.LENGTH_SHORT).show()
            }
        }
    }
    return PasswordBackupActions(
        importBackup = { importLauncher.launch(arrayOf("application/json")) },
        exportBackup = { exportLauncher.launch("passwords_backup.json") },
    )
}

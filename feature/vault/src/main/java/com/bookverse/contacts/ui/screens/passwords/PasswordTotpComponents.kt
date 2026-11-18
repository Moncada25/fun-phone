package com.bookverse.contacts.ui.screens.passwords

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.domain.passwords.Totp
import com.bookverse.contacts.domain.passwords.TotpConfig
import com.bookverse.contacts.ui.screens.qr.QRCodeScanner
import kotlinx.coroutines.delay

private data class TotpSnapshot(val code: String, val remainingSeconds: Int)

private fun totpSnapshot(config: TotpConfig): TotpSnapshot {
    val epochSeconds = System.currentTimeMillis() / 1_000L
    return TotpSnapshot(
        code = runCatching { Totp.code(config, epochSeconds) }.getOrDefault(""),
        remainingSeconds = Totp.remainingSeconds(config, epochSeconds),
    )
}

internal fun formatTotpCode(code: String): String {
    if (code.length < 6) return code
    val half = (code.length + 1) / 2
    return code.substring(0, half) + " " + code.substring(half)
}

@Composable
internal fun TotpCodeRow(
    config: TotpConfig,
    accentColor: Color,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot by produceState(initialValue = totpSnapshot(config), config) {
        while (true) {
            value = totpSnapshot(config)
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }
    val expiring = snapshot.remainingSeconds <= 5
    val progressColor = if (expiring) MaterialTheme.colorScheme.error else accentColor
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(
            progress = { snapshot.remainingSeconds / config.periodSeconds.toFloat() },
            modifier = Modifier.size(28.dp),
            color = progressColor,
            trackColor = accentColor.copy(alpha = 0.15f),
            strokeWidth = 3.dp,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = formatTotpCode(snapshot.code),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.password_totp_seconds_left,
                    snapshot.remainingSeconds,
                    snapshot.remainingSeconds,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (expiring) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = { onCopy(snapshot.code) },
            enabled = snapshot.code.isNotEmpty(),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.password_totp_copy))
        }
    }
}

@Composable
internal fun TwoFactorSection(
    totpInput: String,
    onTotpChange: (String) -> Unit,
    config: TotpConfig?,
    onScan: () -> Unit,
    onCopyCode: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    var showSecret by remember { mutableStateOf(false) }
    val invalid = totpInput.isNotBlank() && config == null
    EditorSectionCard(
        title = stringResource(R.string.password_totp_section_title),
        icon = Icons.Default.Timer,
        accentColor = accentColor,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = totpInput,
            onValueChange = onTotpChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.password_totp_field_label)) },
            trailingIcon = {
                Row {
                    if (totpInput.isNotEmpty()) {
                        IconButton(onClick = { showSecret = !showSecret }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = stringResource(
                                    if (showSecret) R.string.toggle_hide else R.string.toggle_reveal,
                                ),
                            )
                        }
                        IconButton(onClick = { onTotpChange("") }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.password_totp_remove),
                            )
                        }
                    }
                    IconButton(onClick = onScan, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.password_totp_scan),
                        )
                    }
                }
            },
            visualTransformation = if (showSecret) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation(PASSWORD_MASK_CHARACTER)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            isError = invalid,
            supportingText = if (invalid) {
                { Text(stringResource(R.string.password_totp_invalid)) }
            } else {
                null
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = editorFieldColors(accentColor),
        )
        if (config != null) {
            TotpCodeRow(config = config, accentColor = accentColor, onCopy = onCopyCode)
        }
    }
}

@Composable
internal fun TotpScannerDialog(
    accentColor: Color,
    onResult: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var handled by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }
    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                if (granted) {
                    QRCodeScanner(
                        onQrCodeScanned = { content, _ ->
                            if (!handled) {
                                handled = true
                                onResult(content)
                            }
                        },
                        isTorchEnabled = false,
                        isScanningEnabled = !handled,
                        accentColor = accentColor,
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            stringResource(R.string.qr_scanner_permission_message),
                            textAlign = TextAlign.Center,
                        )
                        TextButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text(stringResource(R.string.qr_scanner_retry))
                        }
                    }
                }
                Surface(
                    modifier = Modifier.statusBarsPadding().padding(12.dp).align(Alignment.TopStart),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                        Text(
                            stringResource(R.string.password_totp_scan),
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}

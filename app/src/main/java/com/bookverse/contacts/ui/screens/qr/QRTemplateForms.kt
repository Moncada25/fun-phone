package com.bookverse.contacts.ui.screens.qr

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.ui.components.SettingSwitch
import com.bookverse.contacts.ui.components.TextField
import com.bookverse.contacts.utils.QrTemplates
import com.bookverse.contacts.utils.WifiSecurity

internal enum class QrTemplate(@StringRes val labelRes: Int) {
    TEXT(R.string.qr_template_text),
    WIFI(R.string.qr_template_wifi),
    EMAIL(R.string.qr_template_email),
    PHONE(R.string.qr_template_phone),
}

@Stable
internal class QrTemplateState {
    var template by mutableStateOf(QrTemplate.TEXT)
    var wifiSsid by mutableStateOf("")
    var wifiPassword by mutableStateOf("")
    var wifiSecurity by mutableStateOf(WifiSecurity.WPA)
    var wifiHidden by mutableStateOf(false)
    var emailAddress by mutableStateOf("")
    var emailSubject by mutableStateOf("")
    var emailBody by mutableStateOf("")
    var phoneNumber by mutableStateOf("")

    fun payload(text: String): String = when (template) {
        QrTemplate.TEXT -> text
        QrTemplate.WIFI -> QrTemplates.wifi(wifiSsid, wifiSecurity, wifiPassword, wifiHidden)
        QrTemplate.EMAIL -> QrTemplates.email(emailAddress, emailSubject, emailBody)
        QrTemplate.PHONE -> QrTemplates.phone(phoneNumber)
    }.orEmpty()
}

@Composable
internal fun QrTemplateSelector(
    selected: QrTemplate,
    onSelect: (QrTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QrTemplate.entries.forEach { template ->
            FilterChip(
                selected = template == selected,
                onClick = { onSelect(template) },
                label = { Text(stringResource(template.labelRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.2f),
                ),
            )
        }
    }
}

@Composable
internal fun QrTemplateForm(
    state: QrTemplateState,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state.template) {
            QrTemplate.TEXT -> Unit
            QrTemplate.WIFI -> WifiForm(state, onChanged)
            QrTemplate.EMAIL -> {
                TextField(
                    value = state.emailAddress,
                    onValueChange = { state.emailAddress = it; onChanged() },
                    label = stringResource(R.string.qr_email_address),
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = state.emailSubject,
                    onValueChange = { state.emailSubject = it; onChanged() },
                    label = stringResource(R.string.qr_email_subject),
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = state.emailBody,
                    onValueChange = { state.emailBody = it; onChanged() },
                    label = stringResource(R.string.qr_email_body),
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            QrTemplate.PHONE -> TextField(
                value = state.phoneNumber,
                onValueChange = { state.phoneNumber = it; onChanged() },
                label = stringResource(R.string.qr_phone_number),
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WifiForm(state: QrTemplateState, onChanged: () -> Unit) {
    val accentColor = MaterialTheme.colorScheme.primary
    val passwordInvalid = state.wifiPassword.isNotEmpty() &&
        !QrTemplates.isWifiPasswordValid(state.wifiSecurity, state.wifiPassword)
    TextField(
        value = state.wifiSsid,
        onValueChange = { state.wifiSsid = it; onChanged() },
        label = stringResource(R.string.qr_wifi_ssid),
        imeAction = ImeAction.Next,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        stringResource(R.string.qr_wifi_security),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WifiSecurity.entries.forEach { security ->
            FilterChip(
                selected = security == state.wifiSecurity,
                onClick = { state.wifiSecurity = security; onChanged() },
                label = {
                    Text(
                        stringResource(
                            when (security) {
                                WifiSecurity.WPA -> R.string.qr_wifi_security_wpa
                                WifiSecurity.WEP -> R.string.qr_wifi_security_wep
                                WifiSecurity.OPEN -> R.string.qr_wifi_security_open
                            },
                        ),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.2f),
                ),
            )
        }
    }
    if (state.wifiSecurity != WifiSecurity.OPEN) {
        TextField(
            value = state.wifiPassword,
            onValueChange = { state.wifiPassword = it; onChanged() },
            label = stringResource(R.string.qr_wifi_password),
            keyboardType = KeyboardType.Password,
            isError = passwordInvalid,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.wifiSecurity == WifiSecurity.WPA) {
            Text(
                stringResource(R.string.qr_wifi_password_rule),
                style = MaterialTheme.typography.bodySmall,
                color = if (passwordInvalid) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.qr_wifi_hidden), color = MaterialTheme.colorScheme.onBackground)
        SettingSwitch(
            checked = state.wifiHidden,
            onCheckedChange = { state.wifiHidden = it; onChanged() },
        )
    }
    Text(
        stringResource(R.string.qr_wifi_history_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

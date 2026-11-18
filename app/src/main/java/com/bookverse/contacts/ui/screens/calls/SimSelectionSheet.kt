package com.bookverse.contacts.ui.screens.calls

import com.bookverse.contacts.utils.PhoneActions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.domain.call.SimAccount
import com.bookverse.contacts.service.CallController
import com.bookverse.contacts.service.SimSelectionCoordinator
import com.bookverse.contacts.ui.components.ButtonSheetScaffold
import com.bookverse.contacts.ui.components.SettingSwitch
import com.bookverse.contacts.ui.screens.settings.SettingsChoiceRow

@Composable
fun SimSelectionHost() {
    val context = LocalContext.current
    val request by SimSelectionCoordinator.pending.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        SimSelectionCoordinator.attachHost()
        onDispose { SimSelectionCoordinator.detachHost() }
    }

    val current = request ?: return
    SimSelectionSheet(
        number = current.number,
        accounts = current.accounts,
        onSelected = { account, rememberChoice ->
            SimSelectionCoordinator.consume(current)
            if (rememberChoice) SimSelectionCoordinator.rememberChoice(context, account.id)
            val callId = current.callId
            if (callId != null) {
                if (!CallController.selectPhoneAccount(callId, account.id)) CallController.end(callId)
            } else {
                PhoneActions.callPhoneNumberWithSim(context, current.number, account.id)
            }
        },
        onDismiss = {
            SimSelectionCoordinator.consume(current)
            current.callId?.let(CallController::end)
        },
    )
}

@Composable
internal fun SimSelectionSheet(
    number: String,
    accounts: List<SimAccount>,
    onSelected: (SimAccount, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var rememberChoice by rememberSaveable { mutableStateOf(false) }
    ButtonSheetScaffold(
        title = stringResource(R.string.sim_chooser_title),
        subtitle = number.takeIf { it.isNotBlank() },
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            accounts.forEach { account ->
                SettingsChoiceRow(
                    label = account.label,
                    supportingText = account.detail,
                    selected = false,
                    onClick = { onSelected(account, rememberChoice) },
                    modifier = Modifier.testTag(simSelectionOptionTag(account.id)),
                    leadingContent = {
                        Icon(imageVector = Icons.Default.SimCard, contentDescription = null)
                    },
                )
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SIM_SELECTION_REMEMBER_TAG)
                    .toggleable(
                        value = rememberChoice,
                        role = Role.Switch,
                        onValueChange = { rememberChoice = it },
                    ),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.sim_chooser_remember),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(R.string.sim_chooser_remember_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    SettingSwitch(checked = rememberChoice, onCheckedChange = null)
                }
            }
        }
    }
}

internal fun simSelectionOptionTag(accountId: String): String = "sim_selection_option_$accountId"

internal const val SIM_SELECTION_REMEMBER_TAG = "sim_selection_remember"

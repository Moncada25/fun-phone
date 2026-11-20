package com.bookverse.contacts.ui.screens.settings

import android.content.ActivityNotFoundException
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.domain.call.CallScreeningOption
import com.bookverse.contacts.domain.call.CallScreeningPolicy
import com.bookverse.contacts.domain.call.CallScreeningSettings
import com.bookverse.contacts.domain.call.SimAccount
import com.bookverse.contacts.domain.call.SimPreference
import com.bookverse.contacts.feature.settings.R
import com.bookverse.contacts.service.CallScreeningRole
import com.bookverse.contacts.ui.components.SettingSwitch

@Composable
internal fun CallSimPreferenceSheet(
    accounts: List<SimAccount>,
    selected: SimPreference,
    onSelected: (SimPreference) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf<SimPreference>(SimPreference.AskEveryTime) +
        accounts.map { SimPreference.Account(it.id) }
    val accountsById = accounts.associateBy(SimAccount::id)
    SettingsSelectionSheet(
        title = stringResource(R.string.call_sim_title),
        subtitle = stringResource(R.string.call_sim_sheet_subtitle),
        options = options,
        selectedOption = selected,
        onSelected = onSelected,
        onDismiss = onDismiss,
        optionKey = { option -> option.toStoredValue() ?: ASK_EVERY_TIME_KEY },
        optionLabel = { option ->
            when (option) {
                SimPreference.AskEveryTime -> stringResource(R.string.call_sim_ask_every_time)
                is SimPreference.Account -> accountsById[option.id]?.label.orEmpty()
            }
        },
        optionSupportingText = { option ->
            when (option) {
                SimPreference.AskEveryTime ->
                    stringResource(R.string.call_sim_ask_every_time_description)
                is SimPreference.Account -> accountsById[option.id]?.detail
            }
        },
        optionLeadingContent = { option ->
            if (option is SimPreference.Account) {
                Icon(imageVector = Icons.Default.SimCard, contentDescription = null)
            }
        },
    )
}

@Composable
internal fun callSimPreferenceLabel(
    accounts: List<SimAccount>,
    preference: SimPreference,
): String = when (preference) {
    SimPreference.AskEveryTime -> stringResource(R.string.call_sim_ask_every_time)
    is SimPreference.Account -> accounts.firstOrNull { it.id == preference.id }?.label
        ?: stringResource(R.string.call_sim_ask_every_time)
}

@Composable
internal fun rememberCallScreeningRoleRequest(onResult: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { onResult() }
    return {
        val intent = CallScreeningRole.createRequestIntent(context)
        if (intent != null) {
            try {
                launcher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                onResult()
            }
        } else {
            onResult()
        }
    }
}

@Composable
internal fun CallScreeningSheet(
    settings: CallScreeningSettings,
    roleActive: Boolean,
    roleAvailable: Boolean,
    hasProfileNumber: Boolean,
    onOptionChanged: (CallScreeningOption, Boolean) -> Unit,
    onRequestRole: () -> Unit,
    onDismiss: () -> Unit,
) {
    SettingsSheetScaffold(
        title = stringResource(R.string.spam_filter_title),
        subtitle = stringResource(R.string.spam_filter_sheet_subtitle),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CallScreeningRoleCard(
                roleActive = roleActive,
                roleAvailable = roleAvailable,
                onRequestRole = onRequestRole,
            )
            CallScreeningRuleRow(
                title = stringResource(R.string.spam_filter_blocked_list_title),
                description = stringResource(R.string.spam_filter_blocked_list_description),
                checked = settings.blockListedNumbers,
                onCheckedChange = { onOptionChanged(CallScreeningOption.BLOCK_LISTED, it) },
                testTag = SPAM_RULE_BLOCKED_LIST_TAG,
            )
            CallScreeningRuleRow(
                title = stringResource(R.string.spam_filter_hidden_title),
                description = stringResource(R.string.spam_filter_hidden_description),
                checked = settings.blockHiddenNumbers,
                onCheckedChange = { onOptionChanged(CallScreeningOption.BLOCK_HIDDEN, it) },
                testTag = SPAM_RULE_HIDDEN_TAG,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                CallScreeningRuleRow(
                    title = stringResource(R.string.spam_filter_spoofed_title),
                    description = stringResource(R.string.spam_filter_spoofed_description),
                    checked = settings.blockFailedVerification,
                    onCheckedChange = { onOptionChanged(CallScreeningOption.BLOCK_FAILED_VERIFICATION, it) },
                    testTag = SPAM_RULE_SPOOFED_TAG,
                )
            }
            CallScreeningRuleRow(
                title = stringResource(R.string.spam_filter_neighbor_title),
                description = stringResource(
                    if (hasProfileNumber) {
                        R.string.spam_filter_neighbor_description
                    } else {
                        R.string.spam_filter_neighbor_needs_profile
                    },
                ),
                checked = settings.blockNeighborNumbers,
                onCheckedChange = { onOptionChanged(CallScreeningOption.BLOCK_NEIGHBOR_NUMBERS, it) },
                testTag = SPAM_RULE_NEIGHBOR_TAG,
                enabled = hasProfileNumber || settings.blockNeighborNumbers,
            )
            CallScreeningRuleRow(
                title = stringResource(R.string.spam_filter_unknown_title),
                description = stringResource(R.string.spam_filter_unknown_description),
                checked = settings.blockNumbersNotInContacts,
                onCheckedChange = { onOptionChanged(CallScreeningOption.BLOCK_NOT_IN_CONTACTS, it) },
                testTag = SPAM_RULE_NOT_IN_CONTACTS_TAG,
            )
            if (settings.blockNumbersNotInContacts) {
                UnknownCallerOptions(settings = settings, onOptionChanged = onOptionChanged)
            }
            Text(
                text = stringResource(R.string.spam_filter_history_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun UnknownCallerOptions(
    settings: CallScreeningSettings,
    onOptionChanged: (CallScreeningOption, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CallScreeningRuleRow(
            title = stringResource(R.string.spam_filter_unknown_silence_title),
            description = stringResource(R.string.spam_filter_unknown_silence_description),
            checked = settings.silenceNumbersNotInContacts,
            onCheckedChange = { onOptionChanged(CallScreeningOption.SILENCE_NOT_IN_CONTACTS, it) },
            testTag = SPAM_RULE_SILENCE_UNKNOWN_TAG,
        )
        CallScreeningRuleRow(
            title = stringResource(R.string.spam_filter_unknown_allow_recent_title),
            description = stringResource(
                R.string.spam_filter_unknown_allow_recent_description,
                CallScreeningPolicy.RECENTLY_DIALED_DAYS,
            ),
            checked = settings.allowRecentlyDialed,
            onCheckedChange = { onOptionChanged(CallScreeningOption.ALLOW_RECENTLY_DIALED, it) },
            testTag = SPAM_RULE_ALLOW_RECENT_TAG,
        )
        CallScreeningRuleRow(
            title = stringResource(R.string.spam_filter_unknown_allow_repeated_title),
            description = stringResource(
                R.string.spam_filter_unknown_allow_repeated_description,
                CallScreeningPolicy.REPEATED_CALL_MINUTES,
            ),
            checked = settings.allowRepeatedCalls,
            onCheckedChange = { onOptionChanged(CallScreeningOption.ALLOW_REPEATED_CALLS, it) },
            testTag = SPAM_RULE_ALLOW_REPEATED_TAG,
        )
    }
}

@Composable
private fun CallScreeningRoleCard(
    roleActive: Boolean,
    roleAvailable: Boolean,
    onRequestRole: () -> Unit,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.08f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    if (roleActive) R.string.call_screening_active else R.string.call_screening_inactive,
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
            )
            Text(
                text = stringResource(
                    when {
                        roleActive -> R.string.call_screening_active_description
                        roleAvailable -> R.string.call_screening_inactive_description
                        else -> R.string.call_screening_unavailable_description
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!roleActive && roleAvailable) {
                Button(
                    onClick = onRequestRole,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(SPAM_ROLE_REQUEST_TAG),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = contentColorFor(accentColor),
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(R.string.call_screening_enable_action))
                }
            }
        }
    }
}

@Composable
private fun CallScreeningRuleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    enabled: Boolean = true,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
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
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            SettingSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
}

private const val ASK_EVERY_TIME_KEY = "ask_every_time"
internal const val SPAM_RULE_BLOCKED_LIST_TAG = "spam_rule_blocked_list"
internal const val SPAM_RULE_HIDDEN_TAG = "spam_rule_hidden"
internal const val SPAM_RULE_NOT_IN_CONTACTS_TAG = "spam_rule_not_in_contacts"
internal const val SPAM_RULE_SPOOFED_TAG = "spam_rule_spoofed"
internal const val SPAM_RULE_NEIGHBOR_TAG = "spam_rule_neighbor"
internal const val SPAM_RULE_SILENCE_UNKNOWN_TAG = "spam_rule_silence_unknown"
internal const val SPAM_RULE_ALLOW_RECENT_TAG = "spam_rule_allow_recent"
internal const val SPAM_RULE_ALLOW_REPEATED_TAG = "spam_rule_allow_repeated"
internal const val SPAM_ROLE_REQUEST_TAG = "spam_role_request"

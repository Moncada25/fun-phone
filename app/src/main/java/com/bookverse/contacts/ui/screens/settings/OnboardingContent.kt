package com.bookverse.contacts.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.ToolsOrderConfig
import com.bookverse.contacts.data.model.ToolsPreset
import com.bookverse.contacts.ui.components.SettingSwitch
import com.bookverse.contacts.ui.components.setupActionContainerColor
import com.bookverse.contacts.ui.components.setupActionContentColor
import com.bookverse.contacts.ui.screens.tools.ToolsPresetSelector
import com.bookverse.contacts.utils.ToolsRegistry
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.History
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Phone
import com.composables.icons.lucide.AlarmClock
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Users

@Composable
internal fun OnboardingContent(
    step: OnboardingStep,
    setupState: PhoneSetupUiState,
    accentColor: Color,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onRequest: (PhoneStatusRequirement) -> Unit,
    onFinish: () -> Unit,
    toolsOrder: ToolsOrderConfig = ToolsOrderConfig.default(),
    passwordManagerEnabled: Boolean = true,
    onPresetSelected: (ToolsPreset) -> Unit = {},
    onToolHiddenChanged: (String, Boolean) -> Unit = { _, _ -> },
    onToolsMenuHiddenChanged: (Boolean) -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val welcome = step == OnboardingStep.WELCOME
    val next = setupState.phoneStatus.nextRequirement
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        val footerMaxHeight = maxHeight * 0.45f
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!welcome) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Lucide.ArrowLeft, stringResource(R.string.back), tint = colors.onBackground)
                    }
                }
                Text(
                    stringResource(R.string.onboarding_step_count, if (welcome) 1 else 2, 2),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onBackground,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("onboarding_body"),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "heading") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (welcome) {
                            Image(
                                painterResource(R.drawable.ic_launcher_foreground),
                                stringResource(R.string.onboarding_app_icon_description),
                                modifier = Modifier.size(72.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(accentColor),
                            )
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge, color = colors.onBackground)
                        }
                        Text(
                            stringResource(if (welcome) R.string.onboarding_welcome_title else R.string.onboarding_setup_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            stringResource(if (welcome) R.string.onboarding_welcome_subtitle else R.string.onboarding_setup_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onBackground,
                        )
                        if (!welcome) {
                            Text(
                                stringResource(
                                    R.string.onboarding_requirements_count,
                                    PhoneStatusRequirement.entries.count { setupState.phoneStatus.isGranted(it) },
                                    PhoneStatusRequirement.entries.size,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onBackground,
                            )
                        }
                    }
                }
                if (welcome) {
                    item(key = "optional_tools") {
                        OptionalToolsSection(
                            toolsOrder = toolsOrder,
                            passwordManagerEnabled = passwordManagerEnabled,
                            accentColor = accentColor,
                            onPresetSelected = onPresetSelected,
                            onToolHiddenChanged = onToolHiddenChanged,
                            onToolsMenuHiddenChanged = onToolsMenuHiddenChanged,
                        )
                    }
                    item(key = "privacy") {
                        PrivacyNote(accentColor)
                    }
                    items(
                        listOf(PhoneStatusRequirement.CONTACTS, PhoneStatusRequirement.CALL_PHONE, PhoneStatusRequirement.CALL_LOG),
                        key = { it.name },
                    ) { requirement ->
                        BenefitRow(requirement, accentColor)
                    }
                } else {
                    item(key = "privacy") {
                        PrivacyNote(accentColor)
                    }
                    items(PhoneStatusRequirement.entries, key = { it.name }) { requirement ->
                        SetupRequirementRow(requirement, setupState, accentColor)
                    }
                }

            }
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = footerMaxHeight)
                    .verticalScroll(rememberScrollState()).testTag("onboarding_actions")
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (welcome || next == null) {
                    SetupButton(
                        label = stringResource(if (welcome) R.string.onboarding_get_started else R.string.onboarding_enter),
                        accentColor = accentColor,
                        onClick = if (welcome) onNext else onFinish,
                    )
                } else {
                    SetupButton(
                        label = setupActionLabel(next, setupState.outcome(next), setupState.phoneStatus.notificationsRequireSettings),
                        accentColor = accentColor,
                        enabled = setupState.requesting == null,
                        onClick = { onRequest(next) },
                    )
                    TextButton(
                        onClick = onFinish,
                        enabled = setupState.requesting == null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.onBackground),
                    ) {
                        Text(stringResource(R.string.onboarding_continue_later))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyNote(accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Lucide.ShieldCheck, null, tint = accentColor, modifier = Modifier.size(24.dp))
        Text(
            stringResource(R.string.perm_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun OptionalToolsSection(
    toolsOrder: ToolsOrderConfig,
    passwordManagerEnabled: Boolean,
    accentColor: Color,
    onPresetSelected: (ToolsPreset) -> Unit,
    onToolHiddenChanged: (String, Boolean) -> Unit,
    onToolsMenuHiddenChanged: (Boolean) -> Unit,
) {
    var customizeTools by rememberSaveable { mutableStateOf(false) }
    val showCustomTools = customizeTools || toolsOrder.matchingPreset() == null
    val toolItems = remember(toolsOrder, passwordManagerEnabled) {
        ToolsRegistry.getToolItems(
            orderConfig = toolsOrder,
            passwordManagerEnabled = passwordManagerEnabled,
            includeHidden = true,
        )
    }
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        contentColor = colors.onSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_optional_tools_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.onboarding_optional_tools_description),
                style = MaterialTheme.typography.bodyMedium,
            )
            ToolsPresetSelector(
                toolsOrder = toolsOrder,
                onPresetSelected = onPresetSelected,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = { customizeTools = !customizeTools },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
            ) {
                Text(stringResource(R.string.onboarding_customize_tools))
            }
            if (showCustomTools) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.background,
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.tools_menu_visible_title),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = stringResource(R.string.tools_menu_visible_description),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            SettingSwitch(
                                checked = !toolsOrder.toolsMenuHidden,
                                onCheckedChange = { checked -> onToolsMenuHiddenChanged(!checked) },
                            )
                        }
                        toolItems.forEach { toolItem ->
                            val isHidden = toolItem.id in toolsOrder.hiddenItems
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = toolItem.icon,
                                    contentDescription = null,
                                    tint = if (isHidden) colors.onSurface.copy(alpha = 0.45f) else accentColor,
                                    modifier = Modifier.size(28.dp),
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = stringResource(toolItem.titleRes),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isHidden) colors.onSurface.copy(alpha = 0.6f) else colors.onSurface,
                                    )
                                    Text(
                                        text = stringResource(toolItem.descriptionRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurface.copy(alpha = 0.75f),
                                    )
                                }
                                SettingSwitch(
                                    checked = !isHidden,
                                    onCheckedChange = { checked -> onToolHiddenChanged(toolItem.id, !checked) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(requirement: PhoneStatusRequirement, accentColor: Color) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SetupIcon(requirementIcon(requirement), accentColor)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(requirementTitleRes(requirement)), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(when (requirement) {
                        PhoneStatusRequirement.CONTACTS -> R.string.onboarding_benefit_contacts
                        PhoneStatusRequirement.CALL_LOG -> R.string.onboarding_benefit_recents
                        else -> R.string.onboarding_benefit_calls
                    }),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SetupRequirementRow(requirement: PhoneStatusRequirement, state: PhoneSetupUiState, accentColor: Color) {
    val outcome = state.outcome(requirement)
    val active = requirement == state.phoneStatus.nextRequirement
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(22.dp),
        border = if (active) androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)) else null,
        color = if (active) colors.onBackground.copy(alpha = 0.07f) else colors.background,
        contentColor = colors.onSurface,
    ) {
        Row(Modifier.padding(if (active) 16.dp else 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SetupIcon(if (outcome == SetupOutcome.GRANTED) Lucide.Check else requirementIcon(requirement), accentColor)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(requirementTitleRes(requirement)), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(setupOutcomeRes(outcome)), style = MaterialTheme.typography.labelMedium)
                if (active) {
                    Text(stringResource(requirementReasonRes(requirement)), style = MaterialTheme.typography.bodyMedium)
                    if (outcome == SetupOutcome.DENIED || outcome == SetupOutcome.BLOCKED || outcome == SetupOutcome.UNAVAILABLE) {
                        Text(
                            stringResource(when (outcome) {
                                SetupOutcome.BLOCKED -> R.string.onboarding_blocked_help
                                SetupOutcome.UNAVAILABLE -> R.string.onboarding_unavailable_help
                                else -> R.string.onboarding_denied_help
                            }),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupIcon(icon: ImageVector, accentColor: Color) {
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SetupButton(label: String, accentColor: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
        colors = ButtonDefaults.buttonColors(
            containerColor = setupActionContainerColor(accentColor),
            contentColor = setupActionContentColor(accentColor),
            disabledContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    ) { Text(label, style = MaterialTheme.typography.titleSmall) }
}

internal fun requirementTitleRes(requirement: PhoneStatusRequirement): Int = when (requirement) {
    PhoneStatusRequirement.DEFAULT_DIALER -> R.string.onboarding_requirement_default
    PhoneStatusRequirement.NOTIFICATIONS -> R.string.call_notifications
    PhoneStatusRequirement.CONTACTS -> R.string.tab_contacts
    PhoneStatusRequirement.CALL_LOG -> R.string.call_log
    PhoneStatusRequirement.PHONE_STATE -> R.string.phone_state
    PhoneStatusRequirement.CALL_PHONE -> R.string.calls
    PhoneStatusRequirement.EXACT_ALARMS -> R.string.onboarding_requirement_exact_alarms
}

internal fun requirementReasonRes(requirement: PhoneStatusRequirement): Int = when (requirement) {
    PhoneStatusRequirement.DEFAULT_DIALER -> R.string.onboarding_reason_default
    PhoneStatusRequirement.NOTIFICATIONS -> R.string.onboarding_reason_notifications
    PhoneStatusRequirement.CONTACTS -> R.string.onboarding_reason_contacts
    PhoneStatusRequirement.CALL_LOG -> R.string.onboarding_reason_history
    PhoneStatusRequirement.PHONE_STATE -> R.string.onboarding_reason_phone_state
    PhoneStatusRequirement.CALL_PHONE -> R.string.onboarding_reason_calls
    PhoneStatusRequirement.EXACT_ALARMS -> R.string.onboarding_reason_exact_alarms
}

private fun requirementIcon(requirement: PhoneStatusRequirement): ImageVector = when (requirement) {
    PhoneStatusRequirement.DEFAULT_DIALER -> Lucide.Smartphone
    PhoneStatusRequirement.NOTIFICATIONS -> Lucide.Bell
    PhoneStatusRequirement.CONTACTS -> Lucide.Users
    PhoneStatusRequirement.CALL_LOG -> Lucide.History
    PhoneStatusRequirement.PHONE_STATE -> Lucide.Smartphone
    PhoneStatusRequirement.CALL_PHONE -> Lucide.Phone
    PhoneStatusRequirement.EXACT_ALARMS -> Lucide.AlarmClock
}

internal fun setupOutcomeRes(outcome: SetupOutcome): Int = when (outcome) {
    SetupOutcome.GRANTED -> R.string.onboarding_status_ready
    SetupOutcome.PENDING -> R.string.onboarding_status_pending
    SetupOutcome.DENIED -> R.string.onboarding_status_denied
    SetupOutcome.BLOCKED -> R.string.onboarding_status_blocked
    SetupOutcome.UNAVAILABLE -> R.string.onboarding_status_unavailable
}

@Composable
internal fun setupActionLabel(
    requirement: PhoneStatusRequirement,
    outcome: SetupOutcome,
    notificationsRequireSettings: Boolean = false,
): String = stringResource(
    when {
        outcome == SetupOutcome.BLOCKED || (requirement == PhoneStatusRequirement.NOTIFICATIONS && notificationsRequireSettings) -> R.string.open_settings
        outcome == SetupOutcome.UNAVAILABLE || outcome == SetupOutcome.DENIED -> R.string.onboarding_retry
        else -> when (requirement) {
            PhoneStatusRequirement.DEFAULT_DIALER -> R.string.onboarding_allow_default
            PhoneStatusRequirement.NOTIFICATIONS -> R.string.onboarding_allow_notifications
            PhoneStatusRequirement.CONTACTS -> R.string.onboarding_allow_contacts
            PhoneStatusRequirement.CALL_LOG -> R.string.onboarding_allow_history
            PhoneStatusRequirement.PHONE_STATE -> R.string.onboarding_allow_phone_state
            PhoneStatusRequirement.CALL_PHONE -> R.string.onboarding_allow_calls
            PhoneStatusRequirement.EXACT_ALARMS -> R.string.onboarding_allow_exact_alarms
        }
    },
)

package com.bookverse.contacts.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.data.model.ToolsOrderConfig
import com.bookverse.contacts.data.model.ToolsPreset
import com.bookverse.contacts.feature.settings.R
import com.bookverse.contacts.ui.components.SettingSwitch
import com.bookverse.contacts.ui.components.setupActionContainerColor
import com.bookverse.contacts.ui.components.setupActionContentColor
import com.bookverse.contacts.ui.screens.tools.ToolsPresetSelector
import com.bookverse.contacts.utils.ToolsRegistry
import com.composables.icons.lucide.AlarmClock
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CodeXml
import com.composables.icons.lucide.History
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Phone
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Users
import com.bookverse.contacts.core.ui.R as CoreUiR

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
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        val footerMaxHeight = maxHeight * 0.45f
        val compactHeight = maxHeight < 480.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight * 0.5f)
                .background(
                    Brush.verticalGradient(
                        listOf(accentColor.copy(alpha = 0.14f), accentColor.copy(alpha = 0.04f), Color.Transparent),
                    ),
                ),
        )
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 560.dp)
                .fillMaxSize(),
        ) {
            OnboardingHeader(
                currentStep = if (welcome) 1 else 2,
                totalSteps = 2,
                accentColor = accentColor,
                onBack = if (welcome) null else onBack,
            )
            LazyColumn(
                state = key(step) { rememberLazyListState() },
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("onboarding_body"),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (welcome) 24.dp else 12.dp),
            ) {
                if (welcome) {
                    item(key = "heading") {
                        WelcomeHero(accentColor = accentColor, compactHeight = compactHeight)
                    }
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
                } else {
                    item(key = "heading") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                stringResource(R.string.onboarding_setup_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onBackground,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                stringResource(R.string.onboarding_setup_subtitle),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onBackground,
                            )
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
                    if (welcome) {
                        Text(
                            text = stringResource(R.string.onboarding_footer_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
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
private fun OnboardingHeader(
    currentStep: Int,
    totalSteps: Int,
    accentColor: Color,
    onBack: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Lucide.ArrowLeft, stringResource(CoreUiR.string.back), tint = colors.onBackground)
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < currentStep) accentColor else colors.onBackground.copy(alpha = 0.12f),
                        ),
                )
            }
        }
        Text(
            stringResource(R.string.onboarding_step_count, currentStep, totalSteps),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}

@Composable
private fun WelcomeHero(accentColor: Color, compactHeight: Boolean) {
    val colors = MaterialTheme.colorScheme
    val entrance = remember { MutableTransitionState(false).apply { targetState = true } }
    val textAlign = if (compactHeight) TextAlign.Start else TextAlign.Center
    AnimatedVisibility(
        visibleState = entrance,
        enter = fadeIn(tween(durationMillis = 450)) +
            slideInVertically(tween(durationMillis = 450)) { it / 16 },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = if (compactHeight) 0.dp else 8.dp),
            horizontalAlignment = if (compactHeight) Alignment.Start else Alignment.CenterHorizontally,
        ) {
            if (compactHeight) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    WelcomeAppIcon(size = 56.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OpenSourceBadge(accentColor)
                        WelcomeTitle(textAlign = textAlign)
                    }
                }
            } else {
                WelcomeAppIcon(size = 96.dp)
                OpenSourceBadge(accentColor, Modifier.padding(top = 28.dp))
                WelcomeTitle(textAlign = textAlign, modifier = Modifier.padding(top = 16.dp))
            }
            Text(
                stringResource(R.string.onboarding_welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = textAlign,
                modifier = Modifier.padding(
                    top = 12.dp,
                    start = if (compactHeight) 0.dp else 8.dp,
                    end = if (compactHeight) 0.dp else 8.dp,
                ),
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                WelcomeValueRow(
                    icon = Lucide.ShieldCheck,
                    title = stringResource(R.string.onboarding_value_private_title),
                    body = stringResource(R.string.onboarding_value_private_body),
                    accentColor = accentColor,
                )
                WelcomeValueRow(
                    icon = Lucide.Phone,
                    title = stringResource(R.string.onboarding_value_calls_title),
                    body = stringResource(R.string.onboarding_value_calls_body),
                    accentColor = accentColor,
                )
                WelcomeValueRow(
                    icon = Lucide.Palette,
                    title = stringResource(R.string.onboarding_value_personal_title),
                    body = stringResource(R.string.onboarding_value_personal_body),
                    accentColor = accentColor,
                )
            }
        }
    }
}

@Composable
private fun WelcomeAppIcon(size: Dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size * 0.29f),
        shadowElevation = size / 8,
        color = Color.White,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painterResource(CoreUiR.drawable.ic_launcher_foreground),
                stringResource(R.string.onboarding_app_icon_description),
                modifier = Modifier.size(size * 0.81f),
            )
        }
    }
}

@Composable
private fun OpenSourceBadge(accentColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = accentColor.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Lucide.CodeXml, null, tint = accentColor, modifier = Modifier.size(14.dp))
            Text(
                stringResource(R.string.onboarding_badge_open_source),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun WelcomeTitle(textAlign: TextAlign, modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.onboarding_welcome_title),
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        ),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = textAlign,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
private fun WelcomeValueRow(
    icon: ImageVector,
    title: String,
    body: String,
    accentColor: Color,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onBackground,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
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
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_optional_tools_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.onboarding_optional_tools_description),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
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
                                    text = stringResource(CoreUiR.string.tools_menu_visible_title),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = stringResource(CoreUiR.string.tools_menu_visible_description),
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
private fun SetupRequirementRow(requirement: PhoneStatusRequirement, state: PhoneSetupUiState, accentColor: Color) {
    val outcome = state.outcome(requirement)
    val active = requirement == state.phoneStatus.nextRequirement
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(22.dp),
        border = if (active) BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)) else null,
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
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accentColor),
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
    PhoneStatusRequirement.CONTACTS -> CoreUiR.string.tab_contacts
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
        outcome == SetupOutcome.BLOCKED || (requirement == PhoneStatusRequirement.NOTIFICATIONS && notificationsRequireSettings) -> CoreUiR.string.open_settings
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

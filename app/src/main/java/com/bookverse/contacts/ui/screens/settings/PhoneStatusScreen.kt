package com.bookverse.contacts.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SecurityUpdateGood
import androidx.compose.material.icons.filled.SystemSecurityUpdateWarning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.ui.components.setupActionContainerColor
import com.bookverse.contacts.ui.components.setupActionContentColor
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneStatusScreen(
    onBackClick: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel = viewModel()
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground
    val setup = rememberPhoneSetupActions()
    val uiState = setup.controller.uiState.phoneStatus
    val nextRequirement = uiState.nextRequirement
    val snackHost = rememberAppSnackbarState()
    val context = LocalContext.current
    val requestCallScreeningRole = rememberCallScreeningRoleRequest(
        onResult = { setup.controller.refresh(readPhoneStatus(context)) },
    )

    BackHandler {
        onBackClick()
    }

    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                scrollBehavior = scrollBehavior,
                title = stringResource(R.string.phone_status_title),
                onBackClick = onBackClick,
                appSettingsViewModel = appSettingsViewModel
            )
        },
        snackbarHost = { AppSnackbarHost(snackHost.hostState) },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (uiState.allRequirementsGranted) {
                                    Icons.Default.SecurityUpdateGood
                                } else {
                                    Icons.Default.SystemSecurityUpdateWarning
                                },
                                contentDescription = null,
                                tint = accentColor
                            )
                            Text(
                                stringResource(R.string.status_resume),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                        }

                        FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
                            StatusChip(
                                label = if (uiState.isDefaultDialer) {
                                    stringResource(R.string.color_default)
                                } else {
                                    stringResource(R.string.no_default_dialer)
                                },
                                check = uiState.isDefaultDialer,
                                accentColor = accentColor
                            )
                            StatusChip(
                                label = stringResource(R.string.call_notifications),
                                check = uiState.hasCallNotifications,
                                accentColor = accentColor
                            )
                            StatusChip(
                                label = stringResource(R.string.tab_contacts),
                                check = uiState.hasContactsPermission,
                                accentColor = accentColor
                            )
                            StatusChip(
                                label = stringResource(R.string.call_log),
                                check = uiState.hasCallLogPermission,
                                accentColor = accentColor
                            )
                            StatusChip(
                                label = stringResource(R.string.phone_state),
                                check = uiState.hasPhoneStatePermission,
                                accentColor = accentColor
                            )
                            StatusChip(
                                label = stringResource(R.string.calls),
                                check = uiState.hasCallPhonePermission,
                                accentColor = accentColor
                            )
                            StatusChip(
                                label = stringResource(R.string.onboarding_requirement_exact_alarms),
                                check = uiState.hasExactAlarmsPermission,
                                accentColor = accentColor
                            )
                            StatusChip(
                                label = stringResource(R.string.call_screening_title),
                                check = uiState.hasCallScreening,
                                accentColor = accentColor
                            )
                        }

                        if (nextRequirement == null) {
                            RequirementStatusCard(
                                title = stringResource(R.string.phone_status_ready_title),
                                reason = stringResource(R.string.phone_status_ready_description),
                                state = stringResource(R.string.phone_status_state_ready),
                                accentColor = accentColor
                            )
                        } else {
                            RequirementStatusCard(
                                title = requirementTitle(nextRequirement),
                                reason = requirementReason(nextRequirement),
                                state = stringResource(setupOutcomeRes(setup.controller.uiState.outcome(nextRequirement))),
                                accentColor = accentColor,
                                action = {
                                    setup.request(nextRequirement)
                                },
                                actionLabel = setupActionLabel(nextRequirement, setup.controller.uiState.outcome(nextRequirement), uiState.notificationsRequireSettings),
                                actionEnabled = setup.controller.uiState.requesting == null,
                            )
                        }

                        RequirementStatusCard(
                            title = stringResource(R.string.call_screening_title),
                            reason = stringResource(
                                when {
                                    uiState.hasCallScreening ->
                                        R.string.call_screening_active_description
                                    uiState.canRequestCallScreening ->
                                        R.string.call_screening_inactive_description
                                    else -> R.string.call_screening_unavailable_description
                                },
                            ),
                            state = stringResource(
                                if (uiState.hasCallScreening) {
                                    R.string.call_screening_active
                                } else {
                                    R.string.call_screening_inactive
                                },
                            ),
                            accentColor = accentColor,
                            action = requestCallScreeningRole.takeIf {
                                !uiState.hasCallScreening && uiState.canRequestCallScreening
                            },
                            actionLabel = stringResource(R.string.call_screening_enable_action),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun requirementTitle(requirement: PhoneStatusRequirement): String = when (requirement) {
    PhoneStatusRequirement.DEFAULT_DIALER -> stringResource(R.string.no_default_dialer)
    PhoneStatusRequirement.NOTIFICATIONS -> stringResource(R.string.call_notifications)
    PhoneStatusRequirement.CONTACTS -> stringResource(R.string.tab_contacts)
    PhoneStatusRequirement.CALL_LOG -> stringResource(R.string.call_log)
    PhoneStatusRequirement.PHONE_STATE -> stringResource(R.string.phone_state)
    PhoneStatusRequirement.CALL_PHONE -> stringResource(R.string.calls)
    PhoneStatusRequirement.EXACT_ALARMS -> stringResource(R.string.onboarding_requirement_exact_alarms)
}

@Composable
private fun requirementReason(requirement: PhoneStatusRequirement): String = when (requirement) {
    PhoneStatusRequirement.DEFAULT_DIALER ->
        stringResource(R.string.phone_status_reason_default_dialer)

    PhoneStatusRequirement.NOTIFICATIONS ->
        stringResource(R.string.phone_status_reason_notifications)

    PhoneStatusRequirement.CONTACTS ->
        stringResource(R.string.phone_status_reason_contacts)

    PhoneStatusRequirement.CALL_LOG ->
        stringResource(R.string.phone_status_reason_call_log)

    PhoneStatusRequirement.PHONE_STATE ->
        stringResource(R.string.phone_status_reason_phone_state)

    PhoneStatusRequirement.CALL_PHONE ->
        stringResource(R.string.phone_status_reason_call_phone)

    PhoneStatusRequirement.EXACT_ALARMS ->
        stringResource(R.string.phone_status_reason_exact_alarms)
}

@Composable
private fun RequirementStatusCard(
    title: String,
    reason: String,
    state: String,
    accentColor: Color,
    action: (() -> Unit)? = null,
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = accentColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.phone_status_current_state, state),
            style = MaterialTheme.typography.labelLarge,
            color = accentColor
        )
        if (action != null && actionLabel != null) {
            Button(
                onClick = action,
                enabled = actionEnabled,
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                colors = ButtonDefaults.buttonColors(containerColor = setupActionContainerColor(accentColor), contentColor = setupActionContentColor(accentColor))
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, check: Boolean, accentColor: Color) {

    val textColor = MaterialTheme.colorScheme.onBackground

    AssistChip(
        onClick = {},
        label = { Text(text = label, color = if (check) accentColor else textColor) },
        leadingIcon = {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (check) accentColor else MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (check) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        },
        enabled = false,
        shape = RoundedCornerShape(999.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = accentColor.copy(alpha = 0.2f),
            disabledContainerColor = accentColor.copy(alpha = 0.2f),
            disabledLabelColor = accentColor.copy(alpha = 0.2f)
        )
    )
}

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: Dp = 0.dp,
    crossAxisSpacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val rows = mutableListOf<List<Placeable>>()
        val rowHeights = mutableListOf<Int>()
        val spacingX = mainAxisSpacing.roundToPx()
        val spacingY = crossAxisSpacing.roundToPx()

        var currentRow = mutableListOf<Placeable>()
        var currentWidth = 0
        var currentHeight = 0
        val maxWidth = constraints.maxWidth

        placeables.forEach { p ->
            val nextWidth = if (currentRow.isEmpty()) p.width else currentWidth + spacingX + p.width
            if (nextWidth <= maxWidth) {
                currentRow.add(p)
                currentWidth = nextWidth
                currentHeight = maxOf(currentHeight, p.height)
            } else {
                rows.add(currentRow)
                rowHeights.add(currentHeight)
                currentRow = mutableListOf(p)
                currentWidth = p.width
                currentHeight = p.height
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowHeights.add(currentHeight)
        }

        val width = constraints.maxWidth
        val height = rowHeights.sum() + spacingY * (rowHeights.size - 1).coerceAtLeast(0)

        layout(width, height) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                var x = 0
                row.forEachIndexed { i, p ->
                    p.placeRelative(x, y)
                    x += p.width + if (i < row.lastIndex) spacingX else 0
                }
                y += rowHeights[rowIndex] + if (rowIndex < rows.lastIndex) spacingY else 0
            }
        }
    }
}

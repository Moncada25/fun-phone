package com.bookverse.contacts.ui.screens.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal enum class SetupOutcome { PENDING, GRANTED, DENIED, BLOCKED, UNAVAILABLE }

@Immutable
internal data class PhoneSetupUiState(
    val phoneStatus: PhoneStatusUiState,
    val outcomes: Map<PhoneStatusRequirement, SetupOutcome> = emptyMap(),
    val requesting: PhoneStatusRequirement? = null,
) {
    fun outcome(requirement: PhoneStatusRequirement): SetupOutcome =
        if (phoneStatus.isGranted(requirement)) SetupOutcome.GRANTED
        else outcomes[requirement] ?: SetupOutcome.PENDING
}

@Stable
internal class PhoneSetupController(initialStatus: PhoneStatusUiState) {
    var uiState by mutableStateOf(PhoneSetupUiState(initialStatus))
        private set

    fun refresh(status: PhoneStatusUiState) {
        uiState = uiState.copy(
            phoneStatus = status,
            outcomes = uiState.outcomes.filterKeys { !status.isGranted(it) },
        )
    }

    fun beginRequest(requirement: PhoneStatusRequirement): Boolean {
        if (uiState.requesting != null || requirement != uiState.phoneStatus.nextRequirement) return false
        uiState = uiState.copy(requesting = requirement)
        return true
    }

    fun finishRequest(requirement: PhoneStatusRequirement, outcome: SetupOutcome, status: PhoneStatusUiState) {
        val resolvedOutcome = if (outcome == SetupOutcome.GRANTED && !status.isGranted(requirement)) {
            SetupOutcome.PENDING
        } else outcome
        uiState = uiState.copy(
            outcomes = uiState.outcomes + (requirement to resolvedOutcome),
            requesting = null,
        )
        refresh(status)
    }

    fun onResume(status: PhoneStatusUiState) {
        val pendingExternalSettings = uiState.requesting
        val outcomes = if (
            pendingExternalSettings == PhoneStatusRequirement.EXACT_ALARMS &&
            !status.isGranted(PhoneStatusRequirement.EXACT_ALARMS)
        ) {
            uiState.outcomes + (PhoneStatusRequirement.EXACT_ALARMS to SetupOutcome.DENIED)
        } else {
            uiState.outcomes
        }
        uiState = uiState.copy(requesting = null, outcomes = outcomes)
        refresh(status)
    }

    // Only presentation outcomes survive recreation; Android remains the source of granted access.
    fun saveOutcomes(): List<String> = uiState.outcomes.map { (requirement, outcome) ->
        "${requirement.name}:${outcome.name}"
    }

    fun restoreOutcomes(saved: List<String>) {
        val restored = saved.mapNotNull { value ->
            val parts = value.split(':')
            val requirement = PhoneStatusRequirement.entries.firstOrNull { it.name == parts.firstOrNull() }
            val outcome = SetupOutcome.entries.firstOrNull { it.name == parts.getOrNull(1) }
            if (requirement != null && outcome != null && outcome != SetupOutcome.GRANTED) requirement to outcome else null
        }.toMap()
        uiState = uiState.copy(outcomes = restored)
        refresh(uiState.phoneStatus)
    }
}

internal fun permissionResultOutcome(granted: Boolean, canRequestAgain: Boolean): SetupOutcome = when {
    granted -> SetupOutcome.GRANTED
    canRequestAgain -> SetupOutcome.DENIED
    else -> SetupOutcome.BLOCKED
}

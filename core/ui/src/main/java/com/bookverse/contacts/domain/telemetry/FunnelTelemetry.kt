package com.bookverse.contacts.domain.telemetry

import com.bookverse.contacts.data.model.ToolsPreset
import com.bookverse.contacts.integration.Telemetry
import java.util.concurrent.atomic.AtomicBoolean

enum class ContactAction {
    CREATE,
    UPDATE,
    DELETE,
    FAVORITE,
}

class FunnelTelemetry(
    private val telemetry: Telemetry,
) {
    private val firstContactActionRecorded = AtomicBoolean(false)

    fun onboardingStep(step: String) {
        telemetry.recordEvent(
            name = "onboarding_step",
            parameters = mapOf("step" to step.lowercase()),
        )
    }

    fun dialerRoleRequested() {
        telemetry.recordEvent(name = "dialer_role_request")
    }

    fun dialerRoleResult(granted: Boolean) {
        telemetry.recordEvent(
            name = "dialer_role_result",
            parameters = mapOf("granted" to granted.toString()),
        )
    }

    fun firstContactAction(action: ContactAction) {
        if (!firstContactActionRecorded.compareAndSet(false, true)) return
        telemetry.recordEvent(
            name = "first_contact_action",
            parameters = mapOf("action" to action.name.lowercase()),
        )
    }

    fun toolsPresetSelected(preset: ToolsPreset) {
        telemetry.recordEvent(
            name = "tools_preset_selected",
            parameters = mapOf("preset" to preset.name.lowercase()),
        )
    }
}

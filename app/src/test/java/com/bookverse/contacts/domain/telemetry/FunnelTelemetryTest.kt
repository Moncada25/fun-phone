package com.bookverse.contacts.domain.telemetry

import com.bookverse.contacts.data.model.ToolsPreset
import com.bookverse.contacts.integration.Telemetry
import org.junit.Assert.assertEquals
import org.junit.Test

class FunnelTelemetryTest {
    @Test
    fun shouldEmitOnlyBoundedAggregateFunnelValues() {
        val sink = RecordingTelemetry()
        val telemetry = FunnelTelemetry(sink)

        telemetry.onboardingStep("TOOLS_SETUP")
        telemetry.dialerRoleRequested()
        telemetry.dialerRoleResult(granted = false)
        telemetry.toolsPresetSelected(ToolsPreset.PHONE_AND_ORGANIZE)

        assertEquals(
            listOf(
                Event("onboarding_step", mapOf("step" to "tools_setup")),
                Event("dialer_role_request", emptyMap()),
                Event("dialer_role_result", mapOf("granted" to "false")),
                Event(
                    "tools_preset_selected",
                    mapOf("preset" to "phone_and_organize"),
                ),
            ),
            sink.events,
        )
    }

    @Test
    fun shouldEmitOnlyTheFirstContactAction() {
        val sink = RecordingTelemetry()
        val telemetry = FunnelTelemetry(sink)

        telemetry.firstContactAction(ContactAction.CREATE)
        telemetry.firstContactAction(ContactAction.DELETE)

        assertEquals(
            listOf(Event("first_contact_action", mapOf("action" to "create"))),
            sink.events,
        )
    }

    private data class Event(
        val name: String,
        val parameters: Map<String, String>,
    )

    private class RecordingTelemetry : Telemetry {
        val events = mutableListOf<Event>()

        override fun setCollectionEnabled(enabled: Boolean) = Unit

        override fun recordEvent(name: String, parameters: Map<String, String>) {
            events += Event(name, parameters)
        }
    }
}

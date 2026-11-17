package com.bookverse.contacts.integration

import android.app.Activity

internal object NoOpTelemetry : Telemetry {
    override fun setCollectionEnabled(enabled: Boolean) = Unit

    override fun recordEvent(name: String, parameters: Map<String, String>) = Unit
}

internal object NoOpIntegrityProvider : IntegrityProvider {
    override suspend fun initialize() = Unit
}

internal object NoOpReviewPromptLauncher : ReviewPromptLauncher {
    override suspend fun launch(activity: Activity): Boolean = false
}

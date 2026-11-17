package com.bookverse.contacts.integration

import android.content.Context
fun createAppContainer(context: Context): AppContainer {
    return AppContainer(
        context = context,
        telemetry = NoOpTelemetry,
        integrityProvider = NoOpIntegrityProvider,
        reviewPromptLauncher = NoOpReviewPromptLauncher,
    )
}

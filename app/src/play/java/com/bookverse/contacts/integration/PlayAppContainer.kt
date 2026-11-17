package com.bookverse.contacts.integration

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

fun createAppContainer(context: Context): AppContainer {
    return AppContainer(
        context = context,
        telemetry = NoOpTelemetry,
        integrityProvider = NoOpIntegrityProvider,
        reviewPromptLauncher = PlayReviewPromptLauncher,
    )
}

private object PlayReviewPromptLauncher : ReviewPromptLauncher {
    override suspend fun launch(activity: Activity): Boolean =
        suspendCancellableCoroutine { continuation ->
            val manager = ReviewManagerFactory.create(activity)
            manager.requestReviewFlow().addOnCompleteListener { request ->
                if (!request.isSuccessful) {
                    if (continuation.isActive) continuation.resume(false)
                    return@addOnCompleteListener
                }

                manager.launchReviewFlow(activity, request.result)
                    .addOnCompleteListener {
                        if (continuation.isActive) continuation.resume(true)
                    }
            }
        }
}

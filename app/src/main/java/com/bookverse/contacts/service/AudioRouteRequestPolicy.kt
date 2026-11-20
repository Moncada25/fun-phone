package com.bookverse.contacts.service

import com.bookverse.contacts.domain.call.AudioRoute

internal enum class AudioRouteReaction {
    FOLLOW_SYSTEM,
    SETTLE,
    RETRY_ONCE,
}

internal fun reactToReportedAudioRoute(
    requested: AudioRoute?,
    reported: AudioRoute,
): AudioRouteReaction = when (requested) {
    null -> AudioRouteReaction.FOLLOW_SYSTEM
    reported -> AudioRouteReaction.SETTLE
    else -> AudioRouteReaction.RETRY_ONCE
}

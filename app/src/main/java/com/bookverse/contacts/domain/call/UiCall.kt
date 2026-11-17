package com.bookverse.contacts.domain.call

data class UiCall(
    val id: String,
    val number: String,
    val displayName: String? = null,
    val photoUri: String? = null,
    val state: CallState,
    val isMuted: Boolean = false,
    val isOnHold: Boolean = false,
    val connectTimeMillis: Long? = null,
    val audioRoute: AudioRoute = AudioRoute.EARPIECE,
    val audioEndpoint: AudioEndpoint? = null,
    val avatarBytes: ByteArray? = null,
    val isConference: Boolean = false,
    val parentId: String? = null,
    val canMerge: Boolean = false,
    val canRespondViaText: Boolean = false,
)

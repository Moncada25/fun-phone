package com.bookverse.contacts.domain.call

import android.content.Context
import com.bookverse.contacts.core.ui.R

enum class AudioRoute {
    EARPIECE,
    SPEAKER,
    BLUETOOTH,
    WIRED
}

data class AudioEndpoint(
    val route: AudioRoute,
    val name: String? = null,
    val identifier: String? = null
) {
    fun getDisplayName(context: Context): String = when (route) {
        AudioRoute.EARPIECE -> context.getString(R.string.earpiece)
        AudioRoute.SPEAKER -> context.getString(R.string.speaker)
        AudioRoute.BLUETOOTH -> name ?: "Bluetooth"
        AudioRoute.WIRED -> context.getString(R.string.wired)
    }
}

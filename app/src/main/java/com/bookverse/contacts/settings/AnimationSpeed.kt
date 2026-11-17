package com.bookverse.contacts.settings

import androidx.compose.runtime.compositionLocalOf

enum class AnimationSpeed(val durationMs: Int) {
    SLOW(750),
    NORMAL(500),
    FAST(250),
    OFF(0);

    companion object {
        fun fromName(name: String?): AnimationSpeed = entries.find {
            it.name == name
        } ?: NORMAL
        val LocalAnimationSpeed = compositionLocalOf { NORMAL }
    }
}

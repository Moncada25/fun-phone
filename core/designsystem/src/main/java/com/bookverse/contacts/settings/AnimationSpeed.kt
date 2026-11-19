package com.bookverse.contacts.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

@Immutable
data class AnimationSpeed private constructor(val durationMs: Int) {
    val step: Int get() = durationMs / STEP_MS

    val name: String
        get() = when (this) {
            OFF -> OFF_NAME
            FAST -> FAST_NAME
            NORMAL -> NORMAL_NAME
            SLOW -> SLOW_NAME
            else -> durationMs.toString()
        }

    companion object {
        const val STEP_MS = 50
        const val MIN_DURATION_MS = 0
        const val MAX_DURATION_MS = 1200
        const val MIN_STEP = MIN_DURATION_MS / STEP_MS
        const val MAX_STEP = MAX_DURATION_MS / STEP_MS
        private const val OFF_NAME = "OFF"
        private const val FAST_NAME = "FAST"
        private const val NORMAL_NAME = "NORMAL"
        private const val SLOW_NAME = "SLOW"

        val OFF = AnimationSpeed(0)
        val FAST = AnimationSpeed(250)
        val NORMAL = AnimationSpeed(500)
        val SLOW = AnimationSpeed(750)
        val presets = listOf(OFF, FAST, NORMAL, SLOW)

        fun of(durationMs: Int): AnimationSpeed {
            val clamped = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
            val snapped = ((clamped + STEP_MS / 2) / STEP_MS) * STEP_MS
            return AnimationSpeed(snapped.coerceAtMost(MAX_DURATION_MS))
        }

        fun ofStep(step: Int): AnimationSpeed = of(step * STEP_MS)

        fun fromName(name: String?): AnimationSpeed = when (name) {
            OFF_NAME -> OFF
            FAST_NAME -> FAST
            NORMAL_NAME -> NORMAL
            SLOW_NAME -> SLOW
            else -> name?.toIntOrNull()?.let(::of) ?: NORMAL
        }

        val LocalAnimationSpeed = compositionLocalOf { NORMAL }
    }
}

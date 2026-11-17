package com.bookverse.contacts.settings

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ListDensity private constructor(val spacing: Int) {
    val itemPadding: Dp get() = spacing.dp

    val avatarSize: Dp
        get() = if (spacing <= NORMAL_SPACING) {
            (NORMAL_AVATAR - (NORMAL_SPACING - spacing) * COMPACT_AVATAR_STEP).dp
        } else {
            (NORMAL_AVATAR + (spacing - NORMAL_SPACING) * SPACIOUS_AVATAR_STEP).dp
        }

    val name: String
        get() = when (this) {
            COMPACT -> COMPACT_NAME
            NORMAL -> NORMAL_NAME
            SPACIOUS -> SPACIOUS_NAME
            else -> spacing.toString()
        }

    companion object {
        const val MIN_SPACING = 4
        const val MAX_SPACING = 20
        private const val NORMAL_SPACING = 12
        private const val NORMAL_AVATAR = 40f
        private const val COMPACT_AVATAR_STEP = 8f / 6f
        private const val SPACIOUS_AVATAR_STEP = 3f
        private const val COMPACT_NAME = "COMPACT"
        private const val NORMAL_NAME = "NORMAL"
        private const val SPACIOUS_NAME = "SPACIOUS"

        val COMPACT = ListDensity(6)
        val NORMAL = ListDensity(NORMAL_SPACING)
        val SPACIOUS = ListDensity(16)
        val presets = listOf(COMPACT, NORMAL, SPACIOUS)

        fun of(spacing: Int): ListDensity = ListDensity(spacing.coerceIn(MIN_SPACING, MAX_SPACING))

        fun fromName(name: String?): ListDensity = when (name) {
            COMPACT_NAME -> COMPACT
            NORMAL_NAME -> NORMAL
            SPACIOUS_NAME -> SPACIOUS
            else -> name?.toIntOrNull()?.let(::of) ?: NORMAL
        }
    }
}

package com.bookverse.contacts.ui.theme

enum class ThemeMode {
    LIGHT, DARK, SYSTEM;

    companion object {
        fun fromName(name: String?): ThemeMode {
            return when (name) {
                "LIGHT" -> LIGHT
                "DARK" -> DARK
                else -> SYSTEM
            }
        }
    }
}

enum class DarkModeStyle {
    AMOLED, DARK_GRAY, DIM;

    companion object {
        fun fromName(name: String?): DarkModeStyle {
            return when (name) {
                "DARK_GRAY" -> DARK_GRAY
                "DIM" -> DIM
                else -> AMOLED
            }
        }
    }
}

enum class AccentBarStyle {
    MODERN,
    MINIMAL,
    RETRO;

    companion object {
        fun fromName(value: String?): AccentBarStyle {
            return entries.firstOrNull { it.name == value } ?: MODERN
        }
    }
}

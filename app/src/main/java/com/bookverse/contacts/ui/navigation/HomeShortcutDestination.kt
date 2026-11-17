package com.bookverse.contacts.ui.navigation

import com.bookverse.contacts.utils.SecondaryScreen

enum class HomeShortcutDestination(
    val id: String,
    val secondaryScreen: SecondaryScreen,
    val requiresPasswordAuth: Boolean = false,
    val requiresSensitiveSectionAuth: Boolean = false
) {
    NOTES(
        id = "notes",
        secondaryScreen = SecondaryScreen.NOTES
    ),
    PASSWORDS(
        id = "passwords",
        secondaryScreen = SecondaryScreen.PASSWORD_LIST,
        requiresPasswordAuth = true
    ),
    GAMES(
        id = "games",
        secondaryScreen = SecondaryScreen.GAMES
    ),
    EXPENSES(
        id = "expenses",
        secondaryScreen = SecondaryScreen.EXPENSES,
        requiresSensitiveSectionAuth = true
    ),
    MENSTRUAL_CALENDAR(
        id = "menstrual_calendar",
        secondaryScreen = SecondaryScreen.MENSTRUAL_CALENDAR
    ),
    HOLIDAYS(
        id = "holidays",
        secondaryScreen = SecondaryScreen.HOLIDAYS
    ),
    QR_SCANNER(
        id = "qr_scanner",
        secondaryScreen = SecondaryScreen.QR_SCANNER
    ),
    QR_GENERATOR(
        id = "qr_generator",
        secondaryScreen = SecondaryScreen.QR_GENERATOR
    ),
    DIALER(
        id = "dialer",
        secondaryScreen = SecondaryScreen.DIALER
    ),
    VOICE_TOOLS(
        id = "voice_tools",
        secondaryScreen = SecondaryScreen.VOICE_RECOGNITION
    ),
    PAINT(
        id = "paint",
        secondaryScreen = SecondaryScreen.PAINT
    ),
    POMODORO(
        id = "pomodoro",
        secondaryScreen = SecondaryScreen.POMODORO
    ),
    NEW_NOTE(
        id = "new_note",
        secondaryScreen = SecondaryScreen.NOTE
    );

    companion object {
        const val EXTRA_KEY = "shortcut_destination"

        fun fromId(id: String?): HomeShortcutDestination? {
            if (id.isNullOrBlank()) return null
            return entries.firstOrNull { it.id == id }
        }
    }
}

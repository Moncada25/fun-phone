package com.bookverse.contacts.ui.navigation

import com.bookverse.contacts.utils.SecondaryScreen

/** Rendering capability only. ProtectedScreenHost owns authentication and secure windows. */
fun SecondaryScreen.allowsBackdrop(): Boolean = when (this) {
    SecondaryScreen.PASSWORDS_NEW,
    SecondaryScreen.PASSWORD_FOLDERS, SecondaryScreen.PASSWORD_RECYCLE_BIN,
    SecondaryScreen.NOTE, SecondaryScreen.NOTE_FOLDERS,
    SecondaryScreen.RECYCLE_BIN, SecondaryScreen.CHECKLIST, SecondaryScreen.REMINDER,
    SecondaryScreen.RECORDING, SecondaryScreen.MENSTRUAL_CALENDAR,
    SecondaryScreen.INCOMING_CALL, SecondaryScreen.IN_CALL,
    SecondaryScreen.QR_SCANNER, SecondaryScreen.PAINT, SecondaryScreen.VOICE_RECOGNITION,
    SecondaryScreen.GAMES -> false
    else -> true
}

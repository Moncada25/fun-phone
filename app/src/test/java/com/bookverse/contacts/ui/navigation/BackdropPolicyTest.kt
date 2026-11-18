package com.bookverse.contacts.ui.navigation

import com.bookverse.contacts.utils.SecondaryScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackdropPolicyTest {
    @Test
    fun browsingSurfacesShareTheModernBackdrop() {
        for (screen in listOf(SecondaryScreen.NOTES, SecondaryScreen.PASSWORD_LIST, SecondaryScreen.EXPENSES, SecondaryScreen.DETAIL)) {
            assertTrue(screen.name, screen.allowsBackdrop())
        }
    }

    @Test
    fun protectedEditorsAndPlatformSurfacesKeepTheFallback() {
        for (screen in listOf(
            SecondaryScreen.NOTE, SecondaryScreen.PASSWORDS_NEW, SecondaryScreen.CHECKLIST,
            SecondaryScreen.MENSTRUAL_CALENDAR, SecondaryScreen.QR_SCANNER, SecondaryScreen.IN_CALL,
            SecondaryScreen.PAINT, SecondaryScreen.GAMES,
        )) {
            assertFalse(screen.name, screen.allowsBackdrop())
        }
    }
}

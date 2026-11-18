package com.bookverse.contacts

import com.bookverse.contacts.navigation.AppEntryPoints
import com.bookverse.contacts.widget.ContactDetailActivity
import org.junit.Assert.assertEquals
import org.junit.Test

class AppEntryPointsTest {
    @Test
    fun mainActivityEntryPointMatchesTheRealActivityClass() {
        assertEquals(MainActivity::class.java.name, AppEntryPoints.MAIN_ACTIVITY)
    }

    @Test
    fun contactDetailEntryPointMatchesTheRealActivityClass() {
        assertEquals(ContactDetailActivity::class.java.name, AppEntryPoints.CONTACT_DETAIL_ACTIVITY)
        assertEquals(ContactDetailActivity.EXTRA_CONTACT_ID, AppEntryPoints.EXTRA_CONTACT_ID)
        assertEquals(ContactDetailActivity.EXTRA_CONTACT_NAME, AppEntryPoints.EXTRA_CONTACT_NAME)
    }
}

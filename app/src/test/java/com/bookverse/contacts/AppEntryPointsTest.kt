package com.bookverse.contacts

import com.bookverse.contacts.navigation.AppEntryPoints
import org.junit.Assert.assertEquals
import org.junit.Test

class AppEntryPointsTest {
    @Test
    fun mainActivityEntryPointMatchesTheRealActivityClass() {
        assertEquals(MainActivity::class.java.name, AppEntryPoints.MAIN_ACTIVITY)
    }
}

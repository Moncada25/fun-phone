package com.bookverse.contacts.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AboutVersionTest {
    @Test
    fun keepsOnlyTheSemanticVersion() {
        assertEquals("3.20.8", displayVersion("3.20.8-community.18112026 Dev"))
        assertEquals("3.20.8", displayVersion("3.20.8"))
        assertEquals("3.20.8", displayVersion("3.20.8-community"))
    }

    @Test
    fun fallsBackToTheRawValue() {
        assertEquals("N/A", displayVersion("N/A"))
    }
}

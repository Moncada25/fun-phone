package com.bookverse.contacts.ui.navigation

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DialerLaunchRequestTest {
    @Test
    fun supportedPhoneLinksPrefillTheDecodedNumber() {
        for (action in listOf(Intent.ACTION_DIAL, Intent.ACTION_VIEW, Intent.ACTION_CALL)) {
            assertEquals(DialerLaunchRequest("+573001234567"), resolveDialerLaunchRequest(action, "tel", "+57 (300) 123-4567"))
        }
    }

    @Test
    fun emptyDialIntentOpensAnEmptyDialer() {
        assertEquals(DialerLaunchRequest(""), resolveDialerLaunchRequest(Intent.ACTION_DIAL, null, null))
        assertEquals(DialerLaunchRequest(""), resolveDialerLaunchRequest(Intent.ACTION_DIAL, "tel", ""))
    }

    @Test
    fun callButtonOpensAnEmptyDialer() {
        assertEquals(DialerLaunchRequest(""), resolveDialerLaunchRequest(Intent.ACTION_CALL_BUTTON, null, null))
    }

    @Test
    fun dialableServiceCharactersArePreserved() {
        assertEquals(DialerLaunchRequest("*123#"), resolveDialerLaunchRequest(Intent.ACTION_VIEW, "TEL", "*123#"))
    }

    @Test
    fun unrelatedActionsCannotPrefillTheDialer() {
        assertNull(resolveDialerLaunchRequest(Intent.ACTION_SEND, "tel", "+15550123456"))
        assertNull(resolveDialerLaunchRequest(null, "tel", "+15550123456"))
    }

    @Test
    fun webAndMissingUrisDoNotBecomeTelephoneRequests() {
        assertNull(resolveDialerLaunchRequest(Intent.ACTION_VIEW, "https", "example.com/123"))
        assertNull(resolveDialerLaunchRequest(Intent.ACTION_VIEW, null, null))
        assertNull(resolveDialerLaunchRequest(Intent.ACTION_CALL, null, null))
    }
}

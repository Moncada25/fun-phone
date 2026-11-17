package com.bookverse.contacts.domain.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RejectMessagePolicyTest {
    @Test
    fun blankMessagesAreNeverSent() {
        assertNull(RejectMessagePolicy.sanitize(null))
        assertNull(RejectMessagePolicy.sanitize(""))
        assertNull(RejectMessagePolicy.sanitize(" \n\t "))
        assertFalse(RejectMessagePolicy.canSend("   ", callSupportsTextResponse = true))
    }

    @Test
    fun whitespaceIsCollapsedAndTrimmed() {
        assertEquals("Call you later", RejectMessagePolicy.sanitize("  Call   you\nlater  "))
    }

    @Test
    fun messagesAreLimitedToASingleSmsSegment() {
        val long = "a".repeat(RejectMessagePolicy.MAX_LENGTH + 40)

        assertEquals(RejectMessagePolicy.MAX_LENGTH, RejectMessagePolicy.sanitize(long)?.length)
        assertEquals(RejectMessagePolicy.MAX_LENGTH, RejectMessagePolicy.limitInput(long).length)
        assertEquals("short", RejectMessagePolicy.limitInput("short"))
    }

    @Test
    fun sendingNeedsTextAndCallSupport() {
        assertTrue(RejectMessagePolicy.canSend("On my way", callSupportsTextResponse = true))
        assertFalse(RejectMessagePolicy.canSend("On my way", callSupportsTextResponse = false))
    }
}

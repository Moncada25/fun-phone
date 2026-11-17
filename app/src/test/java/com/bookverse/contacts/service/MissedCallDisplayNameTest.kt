package com.bookverse.contacts.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MissedCallDisplayNameTest {
    @Test
    fun shouldUseContactNameWhenLookupCompletes() = runTest {
        assertEquals(
            "Ada",
            resolveMissedCallDisplayName(
                number = "+123",
                timeoutMillis = 1_000,
                lookup = { "Ada" },
            ),
        )
    }

    @Test
    fun shouldKeepNumberWhenLookupFailsOrReturnsNoName() = runTest {
        assertEquals(
            "+123",
            resolveMissedCallDisplayName(
                number = "+123",
                timeoutMillis = 1_000,
                lookup = { error("contact provider unavailable") },
            ),
        )
        assertEquals(
            "+123",
            resolveMissedCallDisplayName(
                number = "+123",
                timeoutMillis = 1_000,
                lookup = { "" },
            ),
        )
    }

    @Test
    fun shouldKeepNumberWhenLookupExceedsBound() = runTest {
        assertEquals(
            "+123",
            resolveMissedCallDisplayName(
                number = "+123",
                timeoutMillis = 100,
                lookup = {
                    delay(101)
                    "Late contact"
                },
            ),
        )
    }
}

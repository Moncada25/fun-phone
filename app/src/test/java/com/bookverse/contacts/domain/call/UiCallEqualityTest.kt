package com.bookverse.contacts.domain.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UiCallEqualityTest {

    @Test
    fun equals_sameData_returnsTrue() {
        val a = UiCall(
            id = "c1",
            number = "+1234567890",
            state = CallState.ACTIVE,
            isMuted = false,
            connectTimeMillis = 1000L,
            audioRoute = AudioRoute.SPEAKER
        )
        val b = UiCall(
            id = "c1",
            number = "+1234567890",
            state = CallState.ACTIVE,
            isMuted = false,
            connectTimeMillis = 1000L,
            audioRoute = AudioRoute.SPEAKER
        )
        assertEquals(a, b)
    }

    @Test
    fun equals_differentMuted_returnsFalse() {
        val a = UiCall(
            id = "c1",
            number = "+1",
            state = CallState.ACTIVE,
            isMuted = false
        )
        val b = a.copy(isMuted = true)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentAudioRoute_returnsFalse() {
        val a = UiCall(
            id = "c1",
            number = "+1",
            state = CallState.ACTIVE,
            audioRoute = AudioRoute.EARPIECE
        )
        val b = a.copy(audioRoute = AudioRoute.SPEAKER)
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentConnectTime_returnsFalse() {
        val a = UiCall(
            id = "c1",
            number = "+1",
            state = CallState.ACTIVE,
            connectTimeMillis = 1000L
        )
        val b = a.copy(connectTimeMillis = 2000L)
        assertNotEquals(a, b)
    }
}

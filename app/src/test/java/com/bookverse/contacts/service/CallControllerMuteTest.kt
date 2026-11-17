package com.bookverse.contacts.service

import com.bookverse.contacts.domain.call.AudioRoute
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CallControllerMuteTest {

    @Before
    fun setUp() {
        CallController.resetLastReportedMutedForTest()
    }

    @After
    fun tearDown() {
        CallController.resetLastReportedMutedForTest()
    }

    @Test
    fun onSystemAudioStateChanged_initialState_setsLastReportedMuted() {
        assertEquals(false, CallController.getLastReportedMutedForTest())

        CallController.onSystemAudioStateChanged(AudioRoute.EARPIECE, isMuted = false)
        assertEquals(false, CallController.getLastReportedMutedForTest())
    }

    @Test
    fun onSystemAudioStateChanged_muteChange_updatesLastReportedMuted() {
        CallController.onSystemAudioStateChanged(AudioRoute.EARPIECE, isMuted = false)
        assertEquals(false, CallController.getLastReportedMutedForTest())

        CallController.onSystemAudioStateChanged(AudioRoute.EARPIECE, isMuted = true)
        assertEquals(true, CallController.getLastReportedMutedForTest())

        CallController.onSystemAudioStateChanged(AudioRoute.EARPIECE, isMuted = false)
        assertEquals(false, CallController.getLastReportedMutedForTest())
    }

    @Test
    fun onSystemAudioStateChanged_defaultMute_doesNotCrash() {
        CallController.onSystemAudioStateChanged(AudioRoute.SPEAKER)
        assertEquals(false, CallController.getLastReportedMutedForTest())
    }
}

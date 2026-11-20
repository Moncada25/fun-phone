package com.bookverse.contacts.service

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallRingerTest {
    @Test
    fun ringsAndVibrates_whenRingerIsNormalWithVolume() {
        assertEquals(
            RingerOutputs(playRingtone = true, vibrate = true, playWaitingTone = false),
            resolveRingerOutputs(AudioManager.RINGER_MODE_NORMAL, ringVolume = 5, callWaiting = false),
        )
    }

    @Test
    fun onlyVibrates_whenRingVolumeIsZero() {
        assertEquals(
            RingerOutputs(playRingtone = false, vibrate = true, playWaitingTone = false),
            resolveRingerOutputs(AudioManager.RINGER_MODE_NORMAL, ringVolume = 0, callWaiting = false),
        )
    }

    @Test
    fun onlyVibrates_whenRingerIsVibrate() {
        assertEquals(
            RingerOutputs(playRingtone = false, vibrate = true, playWaitingTone = false),
            resolveRingerOutputs(AudioManager.RINGER_MODE_VIBRATE, ringVolume = 5, callWaiting = false),
        )
    }

    @Test
    fun staysQuiet_whenRingerIsSilent() {
        assertEquals(
            RingerOutputs(playRingtone = false, vibrate = false, playWaitingTone = false),
            resolveRingerOutputs(AudioManager.RINGER_MODE_SILENT, ringVolume = 5, callWaiting = false),
        )
    }

    @Test
    fun playsWaitingToneOnly_duringAnotherCall() {
        assertEquals(
            RingerOutputs(playRingtone = false, vibrate = false, playWaitingTone = true),
            resolveRingerOutputs(AudioManager.RINGER_MODE_NORMAL, ringVolume = 5, callWaiting = true),
        )
    }

    @Test
    fun staysQuiet_whenTheScreeningServiceSilencedTheCall() {
        val outputs = resolveRingerOutputs(
            ringerMode = AudioManager.RINGER_MODE_NORMAL,
            ringVolume = 5,
            callWaiting = true,
            silenced = true,
        )

        assertEquals(RingerOutputs(playRingtone = false, vibrate = false, playWaitingTone = false), outputs)
    }

    @Test
    fun startsSoundOnce_forConcurrentRingingCalls() {
        val tracker = RingingCallTracker()

        assertTrue(tracker.onRinging("a"))
        assertFalse(tracker.onRinging("a"))
        assertFalse(tracker.onRinging("b"))
    }

    @Test
    fun stopsSound_onlyWhenLastRingingCallEnds() {
        val tracker = RingingCallTracker()
        tracker.onRinging("a")
        tracker.onRinging("b")

        assertFalse(tracker.onStopped("a"))
        assertTrue(tracker.isRinging)
        assertTrue(tracker.onStopped("b"))
        assertFalse(tracker.isRinging)
    }

    @Test
    fun ignoresStop_forUnknownCall() {
        val tracker = RingingCallTracker()

        assertFalse(tracker.onStopped("missing"))
    }

    @Test
    fun silencesOnce_whileCallKeepsRinging() {
        val tracker = RingingCallTracker()
        tracker.onRinging("a")

        assertTrue(tracker.silence())
        assertFalse(tracker.silence())
        assertTrue(tracker.isRinging)
    }

    @Test
    fun silenceIsNoOp_whenNothingRings() {
        assertFalse(RingingCallTracker().silence())
    }

    @Test
    fun newRingingPeriod_resetsSilence() {
        val tracker = RingingCallTracker()
        tracker.onRinging("a")
        tracker.silence()
        tracker.onStopped("a")

        assertTrue(tracker.onRinging("b"))
        assertTrue(tracker.silence())
    }

    @Test
    fun clear_reportsWhetherAnythingWasRinging() {
        val tracker = RingingCallTracker()

        assertFalse(tracker.clear())
        tracker.onRinging("a")
        assertTrue(tracker.clear())
        assertFalse(tracker.isRinging)
    }
}

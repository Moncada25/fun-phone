package com.bookverse.contacts.domain.call

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingCallKeyPolicyTest {
    @Test
    fun volumeUpAnswersOnlyWhenEnabled() {
        assertEquals(
            IncomingCallKeyAction.ANSWER,
            IncomingCallKeyPolicy.actionFor(
                KeyEvent.KEYCODE_VOLUME_UP,
                answerWithVolumeUp = true,
                declineWithVolumeDown = false,
            ),
        )
        assertNull(
            IncomingCallKeyPolicy.actionFor(
                KeyEvent.KEYCODE_VOLUME_UP,
                answerWithVolumeUp = false,
                declineWithVolumeDown = true,
            ),
        )
    }

    @Test
    fun volumeDownDeclinesOnlyWhenEnabled() {
        assertEquals(
            IncomingCallKeyAction.DECLINE,
            IncomingCallKeyPolicy.actionFor(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                answerWithVolumeUp = false,
                declineWithVolumeDown = true,
            ),
        )
        assertNull(
            IncomingCallKeyPolicy.actionFor(
                KeyEvent.KEYCODE_VOLUME_DOWN,
                answerWithVolumeUp = true,
                declineWithVolumeDown = false,
            ),
        )
    }

    @Test
    fun otherKeysNeverActOnTheCall() {
        listOf(
            KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HEADSETHOOK,
        ).forEach { keyCode ->
            assertNull(
                IncomingCallKeyPolicy.actionFor(
                    keyCode,
                    answerWithVolumeUp = true,
                    declineWithVolumeDown = true,
                ),
            )
        }
    }
}

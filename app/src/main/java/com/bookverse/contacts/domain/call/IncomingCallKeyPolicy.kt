package com.bookverse.contacts.domain.call

import android.view.KeyEvent

enum class IncomingCallKeyAction {
    ANSWER,
    DECLINE,
}

object IncomingCallKeyPolicy {
    fun actionFor(
        keyCode: Int,
        answerWithVolumeUp: Boolean,
        declineWithVolumeDown: Boolean,
    ): IncomingCallKeyAction? = when {
        keyCode == KeyEvent.KEYCODE_VOLUME_UP && answerWithVolumeUp -> IncomingCallKeyAction.ANSWER
        keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && declineWithVolumeDown -> IncomingCallKeyAction.DECLINE
        else -> null
    }
}

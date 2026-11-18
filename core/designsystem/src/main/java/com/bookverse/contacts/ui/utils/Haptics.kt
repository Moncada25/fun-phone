package com.bookverse.contacts.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun rememberHapticFeedback(
    type: HapticFeedbackType = HapticFeedbackType.LongPress
): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic, type) { { haptic.performHapticFeedback(type) } }
}

@Composable
fun rememberHapticClick(
    onClick: () -> Unit,
    type: HapticFeedbackType = HapticFeedbackType.LongPress
): () -> Unit {
    val performHaptic = rememberHapticFeedback(type)
    return remember(onClick, performHaptic) {
        {
            performHaptic()
            onClick()
        }
    }
}

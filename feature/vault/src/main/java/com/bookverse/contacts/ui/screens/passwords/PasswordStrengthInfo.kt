package com.bookverse.contacts.ui.screens.passwords

import androidx.compose.ui.graphics.Color
import com.bookverse.contacts.core.ui.R

fun passwordStrengthInfo(strength: Int): Pair<Color, Int> {
    return when (strength) {
        0 -> Color(0xFFB71C1C) to R.string.strength_prohibited
        1 -> Color(0xFFD32F2F) to R.string.strength_very_weak
        2 -> Color(0xFFE65100) to R.string.strength_weak
        3 -> Color(0xFFF57C00) to R.string.strength_fair
        4 -> Color(0xFFF9A825) to R.string.strength_medium
        5 -> Color(0xFFFBC02D) to R.string.strength_medium
        6 -> Color(0xFFAFB42B) to R.string.strength_good
        7 -> Color(0xFF7CB342) to R.string.strength_strong
        8 -> Color(0xFF43A047) to R.string.strength_very_strong
        9 -> Color(0xFF2E7D32) to R.string.strength_excellent
        10 -> Color(0xFF1B5E20) to R.string.strength_excellent
        else -> Color(0xFF424242) to R.string.strength_weak
    }
}

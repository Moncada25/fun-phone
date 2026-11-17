package com.bookverse.contacts.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.bookverse.contacts.ui.viewmodel.ThemeMode

object GradientUtils {

    @Composable
    fun getBackgroundGradient(themeMode: ThemeMode): Brush {
        val isDark = when (themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }

        return if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1A1A2E),
                    Color(0xFF16213E),
                    Color(0xFF0F3460)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF6FAFF),
                    Color(0xFFD5E6FF),
                    Color(0xFF9AC4FF)
                )
            )
        }
    }

    fun getBackgroundGradientColors(isDarkTheme: Boolean): List<Int> {
        return if (isDarkTheme) {
            listOf(
                0xFF1A1A2E.toInt(),
                0xFF16213E.toInt(),
                0xFF0F3460.toInt()
            )
        } else {
            listOf(
                0xFFF6FAFF.toInt(),
                0xFFD5E6FF.toInt(),
                0xFF9AC4FF.toInt()
            )
        }
    }
}

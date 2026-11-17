package com.bookverse.contacts.settings

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.bookverse.contacts.ui.viewmodel.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.DarkModeStyle
import com.bookverse.contacts.ui.viewmodel.ThemeMode

/** A local draft; selecting an option does not change the running app until Apply succeeds. */
@Immutable
data class AppearanceSettings(
    val style: AccentBarStyle,
    val accent: Color,
    val accentName: String,
    val font: String,
    val theme: ThemeMode,
    val darkStyle: DarkModeStyle,
    val fontScale: Float,
    val density: ListDensity,
)

const val FONT_SCALE_MIN_PERCENT = 80
const val FONT_SCALE_MAX_PERCENT = 150

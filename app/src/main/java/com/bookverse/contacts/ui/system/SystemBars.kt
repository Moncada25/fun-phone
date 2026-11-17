package com.bookverse.contacts.ui.system

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.bookverse.contacts.ui.theme.highestContrastContent

/** Activity-owned policy shared by main and widget entry points; the theme never mutates a window. */
fun ComponentActivity.configureTransparentSystemBars(
    background: Color,
    statusBarBackground: Color = background,
) {
    val darkStatusIcons = highestContrastContent(statusBarBackground) == Color.Black
    val darkNavigationIcons = background.luminance() > 0.5f
    val transparent = android.graphics.Color.TRANSPARENT
    val navigationColor = background.toArgb()
    enableEdgeToEdge(
        statusBarStyle = if (darkStatusIcons) SystemBarStyle.light(transparent, transparent) else SystemBarStyle.dark(transparent),
        navigationBarStyle = if (darkNavigationIcons) SystemBarStyle.light(navigationColor, navigationColor) else SystemBarStyle.dark(navigationColor),
    )
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = darkStatusIcons
        isAppearanceLightNavigationBars = darkNavigationIcons
    }
}

internal fun ComponentActivity.updateStatusBarContrast(background: Color) {
    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
        highestContrastContent(background) == Color.Black
}

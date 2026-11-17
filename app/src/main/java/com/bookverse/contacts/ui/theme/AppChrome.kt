package com.bookverse.contacts.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.settings.GlassEffects
import com.bookverse.contacts.ui.viewmodel.AccentBarStyle

val LocalAppChromeStyle = staticCompositionLocalOf { AccentBarStyle.MINIMAL }
val LocalGlassEffects = staticCompositionLocalOf { GlassEffects.AUTO }

@Immutable
data class ChromePolicy(
    val expandableTitles: Boolean,
    val floatingDock: Boolean,
    val backdrop: Boolean,
    val neutralControls: Boolean,
)

private val modernPolicy = ChromePolicy(true, true, true, true)
private val minimalPolicy = ChromePolicy(false, false, false, true)
private val retroPolicy = ChromePolicy(false, false, false, false)

val AccentBarStyle.chromePolicy: ChromePolicy
    get() = when (this) {
        AccentBarStyle.MODERN -> modernPolicy
        AccentBarStyle.MINIMAL -> minimalPolicy
        AccentBarStyle.RETRO -> retroPolicy
    }

object AppChrome {
    val groupCorner = 24.dp
    val sheetCorner = 32.dp
    val dockMargin = 16.dp
    val dockMaxWidth = 560.dp
    val touchTarget = 48.dp
    val floatingActionSize = 64.dp
    val blurRadius = 16.dp
}

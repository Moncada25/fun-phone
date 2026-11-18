package com.bookverse.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bookverse.contacts.ui.theme.LocalAppChromeStyle
import com.bookverse.contacts.ui.theme.AccentBarStyle

/** Search controls stay sharp above the glass; dialogs must not sample another window. */
@Composable
fun SearchFieldSurface(
    legacyColor: Color,
    modifier: Modifier = Modifier,
    allowBackdrop: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    if (LocalAppChromeStyle.current == AccentBarStyle.MODERN) {
        val backdrop = if (allowBackdrop && LocalBackdropAllowed.current) LocalAppBackdrop.current else null
        CompositionLocalProvider(LocalAppBackdrop provides backdrop) {
            LiquidGlassSurface(modifier) {
                Box(Modifier.fillMaxWidth().align(Alignment.Center), contentAlignment = Alignment.CenterStart, content = content)
            }
        }
    } else {
        Box(modifier.background(legacyColor, CircleShape), contentAlignment = Alignment.CenterStart, content = content)
    }
}

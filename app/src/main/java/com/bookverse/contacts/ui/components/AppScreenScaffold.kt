package com.bookverse.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.ui.theme.LocalAppChromeStyle
import com.bookverse.contacts.ui.theme.chromePolicy

internal val LocalMainContentBottomPadding = staticCompositionLocalOf { 0.dp }

/** Owns one backdrop per screen. Foreground controls are never part of its source. */
@Composable
fun AppScreenScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    topBarFooter: (@Composable () -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    contentOverlay: @Composable (PaddingValues) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val modern = LocalAppChromeStyle.current.chromePolicy.backdrop
    val backdrop = if (modern && LocalBackdropAllowed.current) LocalAppBackdrop.current ?: rememberAppBackdrop() else null
    CompositionLocalProvider(LocalAppBackdrop provides backdrop) {
        Scaffold(
            modifier = modifier,
            topBar = {
                Column {
                    topBar()
                    topBarFooter?.let { footer ->
                        Box(Modifier.fillMaxWidth().appBackdropSurface(tintAlpha = 0.80f)) { footer() }
                    }
                }
            },
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            contentWindowInsets = contentWindowInsets,
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                // Empty list space must sample the screen's base, not the glass tint's fallback.
                Box(Modifier.fillMaxSize().appBackdropSource().background(containerColor)) { content(padding) }
                contentOverlay(padding)
            }
        }
    }
}

/** Move only the top inset into a scrolling list, preserving side and navigation-bar insets. */
@Composable
internal fun PaddingValues.withoutTop(): PaddingValues = PaddingValues(
    start = calculateStartPadding(LocalLayoutDirection.current),
    end = calculateEndPadding(LocalLayoutDirection.current),
    bottom = calculateBottomPadding(),
)

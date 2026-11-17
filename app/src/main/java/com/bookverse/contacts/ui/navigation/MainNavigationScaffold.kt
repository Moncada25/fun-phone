package com.bookverse.contacts.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.ui.components.LocalAppBackdrop
import com.bookverse.contacts.ui.components.LocalMainContentBottomPadding
import com.bookverse.contacts.ui.components.rememberAppBackdrop
import com.bookverse.contacts.ui.theme.LocalAppChromeStyle
import com.bookverse.contacts.ui.theme.chromePolicy

@Composable
internal fun MainNavigationScaffold(
    modifier: Modifier,
    bottomBar: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    containerColor: Color,
    contentWindowInsets: WindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    if (!LocalAppChromeStyle.current.chromePolicy.floatingDock) {
        Scaffold(
            modifier = modifier.navigationBarsPadding(), bottomBar = bottomBar,
            floatingActionButton = floatingActionButton, containerColor = containerColor,
            contentWindowInsets = contentWindowInsets, content = content,
        )
        return
    }
    val backdrop = rememberAppBackdrop()
    val density = LocalDensity.current
    val imeHeight = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    var dockHeight by remember { mutableStateOf(100.dp) }
    CompositionLocalProvider(
        LocalAppBackdrop provides backdrop,
        LocalMainContentBottomPadding provides if (imeHeight > 0.dp) imeHeight else dockHeight + 80.dp,
    ) {
        Box(modifier.fillMaxSize()) {
            // The active screen supplies the shared source for its header and this dock.
            Box(Modifier.fillMaxSize()) { content(PaddingValues()) }
            if (imeHeight == 0.dp) {
                Box(
                    Modifier.align(Alignment.BottomCenter)
                        .onSizeChanged { dockHeight = with(density) { it.height.toDp() } }
                        .navigationBarsPadding().padding(bottom = 8.dp),
                ) { bottomBar() }
                Box(Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = dockHeight + 12.dp)) {
                    floatingActionButton()
                }
            }
        }
    }
}

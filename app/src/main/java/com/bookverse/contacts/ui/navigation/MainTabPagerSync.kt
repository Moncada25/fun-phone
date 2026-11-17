package com.bookverse.contacts.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed
import com.bookverse.contacts.ui.viewmodel.NavigationViewModel
import kotlinx.coroutines.flow.drop

/** A tap moves the pager; only a completed gesture commits a new route. */
@Composable
internal fun MainTabPagerSync(
    currentScreen: String,
    pagerState: PagerState,
    mainTabRoutes: List<String>,
    navigationViewModel: NavigationViewModel,
) {
    val duration = LocalAnimationSpeed.current.durationMs
    var programmaticTarget by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(currentScreen, mainTabRoutes) {
        val targetPage = mainTabRoutes.indexOf(currentScreen)
        if (targetPage >= 0 && (targetPage != pagerState.currentPage || pagerState.currentPageOffsetFraction != 0f)) {
            programmaticTarget = targetPage
            try {
                if (duration == 0) pagerState.scrollToPage(targetPage)
                else pagerState.animateScrollToPage(targetPage, animationSpec = tween(duration))
            } finally {
                if (programmaticTarget == targetPage) programmaticTarget = null
            }
        }
    }

    LaunchedEffect(pagerState, mainTabRoutes) {
        snapshotFlow { pagerState.settledPage to pagerState.isScrollInProgress }
            .drop(1)
            .collect { (page, scrolling) ->
                if (!scrolling && programmaticTarget == null && page in mainTabRoutes.indices) {
                    val targetRoute = mainTabRoutes[page]
                    val current = navigationViewModel.uiState.value
                    if (current.secondaryScreen == null && targetRoute != current.currentScreen) {
                        navigationViewModel.navigateTo(targetRoute)
                    }
                }
            }
    }

}

package com.bookverse.contacts.ui.components

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed
import com.bookverse.contacts.ui.theme.LocalAppChromeStyle
import com.bookverse.contacts.ui.viewmodel.AccentBarStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberAppBarScrollBehavior(expandable: Boolean = true, canScroll: () -> Boolean = { true }): TopAppBarScrollBehavior {
    val duration = LocalAnimationSpeed.current.durationMs
    val style = LocalAppChromeStyle.current
    val modern = style == AccentBarStyle.MODERN
    val canExpand = useExpandedAppBar(expandable)
    val initialLimit = if (modern && canExpand) with(LocalDensity.current) {
        (TopAppBarDefaults.LargeAppBarCollapsedHeight - TopAppBarDefaults.LargeAppBarExpandedHeight).toPx()
    } else -Float.MAX_VALUE
    val initialOffset = if (modern && canExpand) initialLimit else 0f
    val state = LocalAppBarStateRegistry.current?.stateFor(
        "${style.name}/${LocalAppBarRoute.current}", initialLimit, initialOffset,
    ) ?: rememberTopAppBarState(initialHeightOffsetLimit = initialLimit, initialHeightOffset = initialOffset)
    if (modern && !canExpand) {
        return TopAppBarDefaults.pinnedScrollBehavior(state = state)
    }
    return if (modern) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            state = state,
            canScroll = canScroll,
            snapAnimationSpec = if (duration == 0) snap() else tween(duration.coerceAtMost(250)),
            flingAnimationSpec = if (duration == 0) null else androidx.compose.animation.rememberSplineBasedDecay(),
        )
    } else {
        TopAppBarDefaults.enterAlwaysScrollBehavior(state = state, canScroll = canScroll)
    }
}

/** Short windows reserve vertical space for the content and navigation controls. */
@Composable
internal fun useExpandedAppBar(expandable: Boolean): Boolean =
    expandable && with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() >= 480.dp }

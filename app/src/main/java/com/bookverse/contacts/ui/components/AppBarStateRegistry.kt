package com.bookverse.contacts.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf

/** Saves only scroll offsets, never the destination's content or form fields. */
@OptIn(ExperimentalMaterial3Api::class)
internal class AppBarStateRegistry {
    private val states = mutableMapOf<String, TopAppBarState>()
    private val lists = mutableMapOf<String, LazyListState>()

    fun listFor(route: String) = lists.getOrPut(route) { LazyListState() }

    fun stateFor(
        route: String,
        initialHeightOffsetLimit: Float = -Float.MAX_VALUE,
        initialHeightOffset: Float = 0f,
    ) = states.getOrPut(route) {
        TopAppBarState(initialHeightOffsetLimit, initialHeightOffset, 0f)
    }

    companion object {
        val Saver = listSaver<AppBarStateRegistry, Any>(
            save = { registry ->
                registry.states.flatMap { (route, state) ->
                    listOf(route, state.heightOffsetLimit, state.heightOffset, state.contentOffset)
                } + registry.lists.flatMap { (route, state) ->
                    listOf("list/$route", 0f, state.firstVisibleItemIndex.toFloat(), state.firstVisibleItemScrollOffset.toFloat())
                }
            },
            restore = { values ->
                AppBarStateRegistry().apply {
                    values.chunked(4).forEach { (route, limit, height, content) ->
                        if ((route as String).startsWith("list/")) {
                            lists[route.removePrefix("list/")] = LazyListState((height as Float).toInt(), (content as Float).toInt())
                        } else {
                            states[route] = TopAppBarState(limit as Float, height as Float, content as Float)
                        }
                    }
                }
            },
        )
    }
}

internal val LocalAppBarStateRegistry = staticCompositionLocalOf<AppBarStateRegistry?> { null }
internal val LocalAppBarRoute = staticCompositionLocalOf { "" }

@Composable
internal fun rememberAppBarStateRegistry() = rememberSaveable(saver = AppBarStateRegistry.Saver) {
    AppBarStateRegistry()
}

@Composable
internal fun rememberDestinationListState(key: String = "content"): LazyListState =
    LocalAppBarStateRegistry.current?.listFor("${LocalAppBarRoute.current}/$key") ?: rememberLazyListState()

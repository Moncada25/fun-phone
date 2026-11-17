package com.bookverse.contacts.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.bookverse.contacts.ui.navigation.NavigationItem
import com.bookverse.contacts.ui.navigation.DialerLaunchRequest
import com.bookverse.contacts.utils.SecondaryScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class NavigationDirection { FORWARD, BACK }

@Immutable
data class NavigationUiState(
    val currentScreen: String = NavigationItem.Contacts.route,
    val mainTabRoute: String = NavigationItem.Contacts.route,
    val secondaryScreen: SecondaryScreen? = null,
    val voiceTextToSpeak: String? = null,
    val secondaryBackStack: List<SecondaryScreen> = emptyList(),
    val direction: NavigationDirection = NavigationDirection.FORWARD,
    val dialerNumber: String = "",
)

class NavigationViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private var initialRouteApplied = savedStateHandle.contains(MAIN_TAB_KEY)
    private var initialDialerLaunchHandled = false
    private val validMainTabs = listOf(
        NavigationItem.Favorites.route,
        NavigationItem.Contacts.route,
        NavigationItem.Recents.route,
        NavigationItem.Extras.route
    )
    private val restoredMainTab =
        savedStateHandle.get<String>(MAIN_TAB_KEY)?.takeIf(validMainTabs::contains)
            ?: NavigationItem.Contacts.route
    private val restoredStack = savedStateHandle.get<List<String>>(SECONDARY_STACK_KEY)
        ?.mapNotNull { name -> SecondaryScreen.entries.firstOrNull { it.name == name } }
        ?: savedStateHandle.get<String>(SECONDARY_SCREEN_KEY)
            ?.let { name -> SecondaryScreen.entries.firstOrNull { it.name == name } }
            ?.let(::listOf).orEmpty()
    private val _uiState = MutableStateFlow(
        NavigationUiState(
            currentScreen = savedStateHandle.get<String>(CURRENT_SCREEN_KEY)
                ?.takeIf(validMainTabs::contains) ?: restoredMainTab,
            mainTabRoute = restoredMainTab,
            secondaryScreen = restoredStack.lastOrNull(),
            secondaryBackStack = restoredStack,
        ),
    )
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    fun setInitialRoute(route: String) {
        if (!initialRouteApplied && route in validMainTabs) {
            initialRouteApplied = true
            updateState { it.copy(currentScreen = route, mainTabRoute = route) }
        }
    }

    fun navigateTo(screen: String) {
        updateState { state ->
            state.copy(
                currentScreen = screen,
                mainTabRoute = screen.takeIf(validMainTabs::contains) ?: state.mainTabRoute,
            )
        }
    }

    fun openSecondary(screen: SecondaryScreen) {
        updateState { it.open(screen) }
    }

    fun handleInitialDialerLaunch(request: DialerLaunchRequest?) {
        if (initialDialerLaunchHandled) return
        initialDialerLaunchHandled = true
        request?.let { openDialer(it.number) }
    }

    /** Route and input become visible together, even before settings finish loading. */
    fun openDialer(number: String = "") {
        updateState { it.open(SecondaryScreen.DIALER).copy(dialerNumber = number) }
    }

    fun setDialerNumber(number: String) {
        // Keep the draft in memory across configuration changes, never in saved-state bundles.
        _uiState.update { it.copy(dialerNumber = number) }
    }

    private fun NavigationUiState.open(screen: SecondaryScreen): NavigationUiState {
        val existing = secondaryBackStack.indexOf(screen)
        return when {
            secondaryScreen == screen -> this
            screen.isCallSurface() || secondaryScreen?.isCallSurface() == true ->
                withStack(listOf(screen), NavigationDirection.FORWARD)
            existing >= 0 -> withStack(secondaryBackStack.take(existing + 1), NavigationDirection.BACK)
            else -> withStack(secondaryBackStack + screen, NavigationDirection.FORWARD)
        }
    }

    /** Replace a completed editor rather than adding it to the return path. */
    fun replaceSecondary(screen: SecondaryScreen) {
        updateState { state ->
            val existing = state.secondaryBackStack.indexOf(screen)
            val stack = if (existing >= 0) state.secondaryBackStack.take(existing + 1)
                else state.secondaryBackStack.dropLast(1) + screen
            state.withStack(stack, NavigationDirection.BACK)
        }
    }

    fun openSecondaryWithVoiceText(screen: SecondaryScreen, text: String) {
        openSecondary(screen)
        updateState { it.copy(voiceTextToSpeak = text) }
    }

    fun navigateBack() {
        updateState { state ->
            val stack = if (state.secondaryScreen?.isCallSurface() == true) emptyList()
                else state.secondaryBackStack.dropLast(1)
            state.withStack(stack, NavigationDirection.BACK)
        }
    }

    /** Telecom and external cancellation close the whole secondary flow. */
    fun closeSecondary() {
        updateState { it.withStack(emptyList(), NavigationDirection.BACK) }
    }

    private fun NavigationUiState.withStack(
        stack: List<SecondaryScreen>,
        direction: NavigationDirection,
    ) = copy(
        secondaryScreen = stack.lastOrNull(),
        secondaryBackStack = stack,
        direction = direction,
        voiceTextToSpeak = null,
    )

    private fun SecondaryScreen.isCallSurface() =
        this == SecondaryScreen.IN_CALL || this == SecondaryScreen.INCOMING_CALL

    private fun updateState(transform: (NavigationUiState) -> NavigationUiState) {
        _uiState.update { currentState ->
            transform(currentState).also(::saveRestorableState)
        }
    }

    private fun saveRestorableState(state: NavigationUiState) {
        savedStateHandle[CURRENT_SCREEN_KEY] = state.currentScreen
        savedStateHandle[MAIN_TAB_KEY] = state.mainTabRoute
        savedStateHandle[SECONDARY_SCREEN_KEY] = state.secondaryScreen?.name
        savedStateHandle[SECONDARY_STACK_KEY] = state.secondaryBackStack.map { it.name }
    }

    internal companion object {
        const val CURRENT_SCREEN_KEY = "navigation.currentScreen"
        const val MAIN_TAB_KEY = "navigation.mainTab"
        const val SECONDARY_STACK_KEY = "navigation.secondaryStack"
        const val SECONDARY_SCREEN_KEY = "navigation.secondaryScreen"
    }
}

package com.bookverse.contacts.ui.navigation

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.domain.call.UiCall
import com.bookverse.contacts.ui.auth.PasswordAuthState
import com.bookverse.contacts.utils.SecondaryScreen
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.NavigationViewModel

@Composable
fun NavigationEffects(
    currentScreen: String,
    pagerState: PagerState,
    mainTabRoutes: List<String>,
    navigationViewModel: NavigationViewModel,
    openFavorites: Boolean,
    openContactDetails: Boolean,
    contactId: String?,
    contactsViewModel: ContactsViewModel,
    onFavoriteShortcutConsumed: () -> Unit,
    onContactShortcutConsumed: () -> Unit,
    onSelectedContactChange: (Contact?) -> Unit,
    openIncoming: Boolean,
    incomingCallId: String?,
    openInCall: Boolean,
    inCallCallId: String?,
    inCallNumber: String?,
    onPendingIncomingIdChange: (String?) -> Unit,
    onPendingInCallIdChange: (String?) -> Unit,
    onPendingInCallNumberChange: (String?) -> Unit,
    onUserDismissedInCallChange: (Boolean) -> Unit,
    onOpenInCallConsumed: () -> Unit,
    activeCall: UiCall?,
    userDismissedInCall: Boolean,
    homeShortcutDestination: HomeShortcutDestination?,
    onHomeShortcutConsumed: () -> Unit,
    passwordAuthState: PasswordAuthState?,
    passwordManagerEnabled: Boolean,
    sensitiveSectionsLockEnabled: Boolean,
    authMethod: com.bookverse.contacts.ui.viewmodel.PasswordAuthMethod?
) {
    val contacts by contactsViewModel.contacts.collectAsStateWithLifecycle()

    MainTabPagerSync(currentScreen, pagerState, mainTabRoutes, navigationViewModel)

    LaunchedEffect(openFavorites, openContactDetails, contactId, contacts) {
        if (openFavorites && !openContactDetails) {
            navigationViewModel.navigateTo(NavigationItem.Favorites.route)
            onFavoriteShortcutConsumed()
        } else if (openContactDetails && contactId != null) {
            if (contacts.isEmpty()) {
                return@LaunchedEffect
            }
            val contact = contacts.firstOrNull { it.id == contactId }
            if (contact != null) {
                onSelectedContactChange(contact)
                navigationViewModel.openSecondary(SecondaryScreen.DETAIL)
            } else {
                navigationViewModel.navigateTo(NavigationItem.Favorites.route)
            }
            onContactShortcutConsumed()
        }
    }

    LaunchedEffect(openIncoming, incomingCallId, openInCall, inCallCallId, inCallNumber) {
        if (openInCall) {
            onPendingInCallIdChange(inCallCallId)
            onPendingInCallNumberChange(inCallNumber)
            navigationViewModel.openSecondary(SecondaryScreen.IN_CALL)
            onUserDismissedInCallChange(false)
            onOpenInCallConsumed()
        } else if (openIncoming && incomingCallId != null) {
            val currentCall = com.bookverse.contacts.service.DefaultCallRepository.activeCall.value
            if (currentCall?.state == CallState.RINGING) {
                onPendingIncomingIdChange(incomingCallId)
                navigationViewModel.openSecondary(SecondaryScreen.INCOMING_CALL)
            } else if (currentCall != null && currentCall.state != CallState.DISCONNECTED) {
                onPendingInCallIdChange(currentCall.id)
                onPendingInCallNumberChange(currentCall.number)
                navigationViewModel.openSecondary(SecondaryScreen.IN_CALL)
            }
            onUserDismissedInCallChange(false)
        }
    }

    LaunchedEffect(activeCall?.id, activeCall?.state) {
        val c = activeCall
        if (c == null || c.state == CallState.DISCONNECTED) {
            val current = navigationViewModel.uiState.value.secondaryScreen
            if (current == SecondaryScreen.IN_CALL || current == SecondaryScreen.INCOMING_CALL) {
                navigationViewModel.closeSecondary()
            }
            onUserDismissedInCallChange(false)
        } else if (c.state == CallState.DIALING || c.state == CallState.ACTIVE || c.state == CallState.ON_HOLD) {
            val currentScreen = navigationViewModel.uiState.value.secondaryScreen
            if (currentScreen == SecondaryScreen.INCOMING_CALL) {
                return@LaunchedEffect
            }
            if (currentScreen != SecondaryScreen.IN_CALL && !userDismissedInCall) {
                onPendingInCallIdChange(c.id)
                onPendingInCallNumberChange(c.number)
                navigationViewModel.openSecondary(SecondaryScreen.IN_CALL)
            }
        }
    }

    val navState by navigationViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(navState.secondaryScreen) {
        val screen = navState.secondaryScreen
        val c = activeCall
        if (screen == null && c != null && (c.state == CallState.DIALING || c.state == CallState.ACTIVE || c.state == CallState.ON_HOLD) && !userDismissedInCall) {
            onUserDismissedInCallChange(true)
        }
        if (screen == SecondaryScreen.IN_CALL) {
            onUserDismissedInCallChange(false)
        }
    }

    LaunchedEffect(homeShortcutDestination) {
        val destination = homeShortcutDestination ?: return@LaunchedEffect

        if (destination.requiresPasswordAuth && passwordAuthState != null) {
            passwordAuthState.handleProtectedAction(
                passwordManagerEnabled = passwordManagerEnabled,
                authMethod = authMethod,
                onSuccess = {
                    navigationViewModel.openSecondary(destination.secondaryScreen)
                    onHomeShortcutConsumed()
                }
            )
        } else if (destination.requiresSensitiveSectionAuth && passwordAuthState != null) {
            if (!sensitiveSectionsLockEnabled) {
                navigationViewModel.openSecondary(destination.secondaryScreen)
                onHomeShortcutConsumed()
                return@LaunchedEffect
            }
            if (authMethod == null) {
                passwordAuthState.startSetupFlow(onConfigured = {
                    navigationViewModel.openSecondary(destination.secondaryScreen)
                    onHomeShortcutConsumed()
                })
                return@LaunchedEffect
            }
            passwordAuthState.handleProtectedAction(
                passwordManagerEnabled = sensitiveSectionsLockEnabled,
                authMethod = authMethod,
                onSuccess = {
                    navigationViewModel.openSecondary(destination.secondaryScreen)
                    onHomeShortcutConsumed()
                }
            )
        } else {
            navigationViewModel.openSecondary(destination.secondaryScreen)
            onHomeShortcutConsumed()
        }
    }
}

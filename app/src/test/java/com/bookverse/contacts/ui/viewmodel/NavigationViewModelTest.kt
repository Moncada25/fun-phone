package com.bookverse.contacts.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.bookverse.contacts.ui.navigation.NavigationItem
import com.bookverse.contacts.ui.navigation.DialerLaunchRequest
import com.bookverse.contacts.utils.SecondaryScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationViewModelTest {
    @Test
    fun initialPhoneLinkKeepsItsNumberWhileStartupPreferencesLoad() {
        val viewModel = NavigationViewModel(SavedStateHandle())
        viewModel.handleInitialDialerLaunch(DialerLaunchRequest("+15550123456"))
        viewModel.setInitialRoute(NavigationItem.Favorites.route)
        assertEquals(SecondaryScreen.DIALER, viewModel.uiState.value.secondaryScreen)
        assertEquals("+15550123456", viewModel.uiState.value.dialerNumber)
    }

    @Test
    fun anotherPhoneLinkReplacesTheInputWithoutDuplicatingTheDialer() {
        val viewModel = NavigationViewModel(SavedStateHandle())
        viewModel.openDialer("+15550123456")
        viewModel.setDialerNumber("123")
        viewModel.openDialer("+15550123456")
        assertEquals("+15550123456", viewModel.uiState.value.dialerNumber)
        assertEquals(listOf(SecondaryScreen.DIALER), viewModel.uiState.value.secondaryBackStack)
        viewModel.openDialer("+15550987654")
        assertEquals("+15550987654", viewModel.uiState.value.dialerNumber)
    }

    @Test
    fun explicitlyOpeningABlankDialerClearsThePreviousNumber() {
        val viewModel = NavigationViewModel(SavedStateHandle())
        viewModel.openDialer("+15550123456")
        viewModel.openDialer()
        assertEquals("", viewModel.uiState.value.dialerNumber)
        assertEquals(SecondaryScreen.DIALER, viewModel.uiState.value.secondaryScreen)
    }

    @Test
    fun activityRecreationCannotReplayTheInitialNumberOverUserEdits() {
        val viewModel = NavigationViewModel(SavedStateHandle())
        val initial = DialerLaunchRequest("+15550123456")
        viewModel.handleInitialDialerLaunch(initial)
        viewModel.setDialerNumber("321")
        viewModel.handleInitialDialerLaunch(initial)
        assertEquals("321", viewModel.uiState.value.dialerNumber)
    }

    @Test
    fun telephoneInputIsNeverWrittenToRestorableNavigationState() {
        val saved = SavedStateHandle()
        NavigationViewModel(saved).openDialer("+15550123456")
        val restored = NavigationViewModel(saved).uiState.value
        assertEquals(SecondaryScreen.DIALER, restored.secondaryScreen)
        assertEquals("", restored.dialerNumber)
    }

    @Test
    fun nestedBackReturnsToParentAndThenTheOriginalTab() {
        val viewModel = NavigationViewModel(SavedStateHandle())
        viewModel.navigateTo(NavigationItem.Extras.route)
        viewModel.openSecondary(SecondaryScreen.SETTINGS)
        viewModel.openSecondary(SecondaryScreen.ABOUT)
        viewModel.navigateBack()
        assertEquals(SecondaryScreen.SETTINGS, viewModel.uiState.value.secondaryScreen)
        assertEquals(NavigationDirection.BACK, viewModel.uiState.value.direction)
        viewModel.navigateBack()
        assertNull(viewModel.uiState.value.secondaryScreen)
        assertEquals(NavigationItem.Extras.route, viewModel.uiState.value.mainTabRoute)
    }

    @Test
    fun completedEditorCannotBeReopenedByBack() {
        val viewModel = NavigationViewModel(SavedStateHandle())
        viewModel.openSecondary(SecondaryScreen.DIALER)
        viewModel.openSecondary(SecondaryScreen.ADD_CONTACT)
        viewModel.replaceSecondary(SecondaryScreen.DETAIL)
        viewModel.navigateBack()
        assertEquals(SecondaryScreen.DIALER, viewModel.uiState.value.secondaryScreen)
    }

    @Test
    fun stackAndChosenTabSurviveRecreationAndStartupPreference() {
        val saved = SavedStateHandle()
        NavigationViewModel(saved).apply {
            navigateTo(NavigationItem.Extras.route)
            openSecondary(SecondaryScreen.NOTES)
            openSecondary(SecondaryScreen.NOTE)
        }
        val restored = NavigationViewModel(saved)
        restored.setInitialRoute(NavigationItem.Favorites.route)
        restored.navigateBack()
        assertEquals(SecondaryScreen.NOTES, restored.uiState.value.secondaryScreen)
        assertEquals(NavigationItem.Extras.route, restored.uiState.value.mainTabRoute)
    }

    @Test
    fun cancellingProfileCreationReturnsToWhereTheProfileWasOpened() {
        val viewModel = NavigationViewModel(SavedStateHandle())
        viewModel.openSecondary(SecondaryScreen.SETTINGS)
        viewModel.openSecondary(SecondaryScreen.PROFILE)
        viewModel.replaceSecondary(SecondaryScreen.CREATE_PROFILE)
        viewModel.navigateBack()
        assertEquals(SecondaryScreen.SETTINGS, viewModel.uiState.value.secondaryScreen)
        assertEquals(listOf(SecondaryScreen.SETTINGS), viewModel.uiState.value.secondaryBackStack)
    }

    @Test
    fun createdProfileReplacesTheCreationForm() {
        val viewModel = NavigationViewModel(SavedStateHandle())
        viewModel.openSecondary(SecondaryScreen.SETTINGS)
        viewModel.openSecondary(SecondaryScreen.PROFILE)
        viewModel.replaceSecondary(SecondaryScreen.CREATE_PROFILE)
        viewModel.replaceSecondary(SecondaryScreen.PROFILE)
        assertEquals(
            listOf(SecondaryScreen.SETTINGS, SecondaryScreen.PROFILE),
            viewModel.uiState.value.secondaryBackStack,
        )
    }

    @Test
    fun returningToExistingParentDoesNotDuplicateDestinations() {
        val viewModel = NavigationViewModel(SavedStateHandle())
        viewModel.openSecondary(SecondaryScreen.NOTES)
        viewModel.openSecondary(SecondaryScreen.NOTE)
        viewModel.replaceSecondary(SecondaryScreen.NOTES)
        assertEquals(listOf(SecondaryScreen.NOTES), viewModel.uiState.value.secondaryBackStack)
    }

    @Test
    fun callSurfaceKeepsItsSpecialReturnToMainContract() {
        val viewModel = NavigationViewModel(SavedStateHandle())
        viewModel.openSecondary(SecondaryScreen.DIALER)
        viewModel.openSecondary(SecondaryScreen.INCOMING_CALL)
        viewModel.openSecondary(SecondaryScreen.IN_CALL)
        viewModel.navigateBack()
        assertNull(viewModel.uiState.value.secondaryScreen)
        assertEquals(emptyList<SecondaryScreen>(), viewModel.uiState.value.secondaryBackStack)
    }

    @Test
    fun mainTabNavigationUpdatesOneAuthoritativeState() {
        val viewModel = NavigationViewModel(SavedStateHandle())

        viewModel.navigateTo(NavigationItem.Extras.route)

        assertEquals(NavigationItem.Extras.route, viewModel.uiState.value.currentScreen)
        assertEquals(NavigationItem.Extras.route, viewModel.uiState.value.mainTabRoute)
    }

    @Test
    fun nonTabNavigationPreservesTheLastMainTab() {
        val viewModel = NavigationViewModel(SavedStateHandle())
        viewModel.navigateTo(NavigationItem.Favorites.route)

        viewModel.navigateTo("settings")

        assertEquals("settings", viewModel.uiState.value.currentScreen)
        assertEquals(NavigationItem.Favorites.route, viewModel.uiState.value.mainTabRoute)
    }

    @Test
    fun secondaryVoiceStateIsClearedTogether() {
        val viewModel = NavigationViewModel(SavedStateHandle())

        viewModel.openSecondaryWithVoiceText(SecondaryScreen.VOICE_RECOGNITION, "hello")
        viewModel.closeSecondary()

        assertNull(viewModel.uiState.value.secondaryScreen)
        assertNull(viewModel.uiState.value.voiceTextToSpeak)
    }

    @Test
    fun invalidInitialRouteDoesNotReplaceContacts() {
        val viewModel = NavigationViewModel(SavedStateHandle())

        viewModel.setInitialRoute("not-a-tab")

        assertEquals(NavigationItem.Contacts.route, viewModel.uiState.value.currentScreen)
        assertEquals(NavigationItem.Contacts.route, viewModel.uiState.value.mainTabRoute)
    }

    @Test
    fun restorableNavigationSurvivesViewModelRecreation() {
        val savedState = SavedStateHandle()
        NavigationViewModel(savedState).apply {
            navigateTo(NavigationItem.Extras.route)
            openSecondary(SecondaryScreen.TOOLS_ORDER)
        }

        val restored = NavigationViewModel(savedState).uiState.value

        assertEquals(NavigationItem.Extras.route, restored.currentScreen)
        assertEquals(NavigationItem.Extras.route, restored.mainTabRoute)
        assertEquals(SecondaryScreen.TOOLS_ORDER, restored.secondaryScreen)
    }

    @Test
    fun transientVoicePayloadIsNotRestoredAfterProcessRecreation() {
        val savedState = SavedStateHandle()
        NavigationViewModel(savedState).openSecondaryWithVoiceText(
            SecondaryScreen.VOICE_RECOGNITION,
            "private transient text",
        )

        val restored = NavigationViewModel(savedState).uiState.value

        assertEquals(SecondaryScreen.VOICE_RECOGNITION, restored.secondaryScreen)
        assertNull(restored.voiceTextToSpeak)
    }

    @Test
    fun invalidSavedStateFallsBackToContactsWithoutCrashing() {
        val savedState = SavedStateHandle(
            mapOf(
                NavigationViewModel.CURRENT_SCREEN_KEY to "removed-route",
                NavigationViewModel.MAIN_TAB_KEY to "removed-tab",
                NavigationViewModel.SECONDARY_SCREEN_KEY to "REMOVED_SCREEN",
            )
        )

        val restored = NavigationViewModel(savedState).uiState.value

        assertEquals(NavigationItem.Contacts.route, restored.currentScreen)
        assertEquals(NavigationItem.Contacts.route, restored.mainTabRoute)
        assertNull(restored.secondaryScreen)
    }
}

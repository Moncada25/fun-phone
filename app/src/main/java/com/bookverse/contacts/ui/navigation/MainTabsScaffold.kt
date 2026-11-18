package com.bookverse.contacts.ui.navigation

import com.bookverse.contacts.utils.PhoneActions
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.utils.SecondaryScreen
import com.bookverse.contacts.ui.screens.tools.ToolsScreen
import com.bookverse.contacts.ui.screens.contacts.ContactListScreen
import com.bookverse.contacts.ui.screens.contacts.FavoritesScreen
import com.bookverse.contacts.ui.screens.history.HistoryScreen
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.NavigationViewModel

@Composable
fun MainTabsScaffold(
    isMainTab: Boolean,
    pagerState: PagerState,
    backgroundColor: Color,
    navigationViewModel: NavigationViewModel,
    mainTabs: List<NavigationItem>,
    favoritesGridState: LazyGridState,
    contactsListState: LazyListState,
    historyListState: LazyListState,
    extrasListState: LazyListState,
    contactsViewModel: ContactsViewModel,
    contactTagViewModel: com.bookverse.contacts.ui.viewmodel.ContactTagViewModel,
    appSettingsViewModel: AppSettingsViewModel,
    profileContact: Contact?,
    isLimitedMode: Boolean,
    onSelectContact: (Contact) -> Unit,
    onOpenFavoritesOrder: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onCreateContact: () -> Unit,
    onDialerPhoneNumberChange: (String) -> Unit,
    floatingActionButton: @Composable () -> Unit = {},
) {
    val tabStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val indicatorProgress by remember {
        derivedStateOf {
            pagerState.currentPage + pagerState.currentPageOffsetFraction
        }
    }

    MainNavigationScaffold(
        bottomBar = {
            if (isMainTab) {
                MainTabNavigationBar(
                    tabs = mainTabs,
                    indicatorProgress = indicatorProgress,
                    backgroundColor = backgroundColor,
                    onTabSelected = { navigationViewModel.navigateTo(it.route) },
                )
            }
        },
        floatingActionButton = floatingActionButton,
        modifier = Modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = backgroundColor
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) { page ->
            tabStateHolder.SaveableStateProvider(mainTabs[page].route) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.bookverse.contacts.ui.components.LocalAppBarRoute provides mainTabs[page].route,
                ) { when (page) {
                0 -> FavoritesScreen(
                    contactsViewModel = contactsViewModel,
                    onContactClick = { onSelectContact(it) },
                    onOpenFavoritesOrder = onOpenFavoritesOrder,
                    onNavigateToProfile = onNavigateToProfile,
                    appSettingsViewModel = appSettingsViewModel,
                    profileContact = profileContact,
                    gridState = favoritesGridState,
                    isLimitedMode = isLimitedMode,
                    onReviewPhoneStatus = { navigationViewModel.openSecondary(SecondaryScreen.PHONE_STATUS) },
                    onChooseTools = { navigationViewModel.openSecondary(SecondaryScreen.TOOLS_ORDER) },
                )

                1 -> ContactListScreen(
                    contactsViewModel = contactsViewModel,
                    contactTagViewModel = contactTagViewModel,
                    onContactClick = { onSelectContact(it) },
                    appSettingsViewModel = appSettingsViewModel,
                    listState = contactsListState,
                    isLimitedMode = isLimitedMode,
                    onCreateContact = { onCreateContact() },
                    onNavigateToSettings = { navigationViewModel.openSecondary(SecondaryScreen.SETTINGS) },
                    onReviewPhoneStatus = { navigationViewModel.openSecondary(SecondaryScreen.PHONE_STATUS) },
                    onChooseTools = { navigationViewModel.openSecondary(SecondaryScreen.TOOLS_ORDER) },
                )

                2 -> HistoryScreen(
                    callHistory = contactsViewModel,
                    onOpenSecondary = { navigationViewModel.openSecondary(it) },
                    phoneActions = PhoneActions,
                    onDialerPhoneNumberChange = onDialerPhoneNumberChange,
                    onContactClick = {
                        if (it.id == profileContact?.id) onNavigateToProfile() else onSelectContact(
                            it
                        )
                    },
                    isLimitedMode = isLimitedMode,
                    listState = historyListState
                )

                3 -> ToolsScreen(
                    onBackClick = { navigationViewModel.navigateTo(NavigationItem.Favorites.route) },
                    onNavigateToSettings = { navigationViewModel.openSecondary(SecondaryScreen.SETTINGS) },
                    onNavigateToGames = { navigationViewModel.openSecondary(SecondaryScreen.GAMES) },
                    onNavigateToNotes = { navigationViewModel.openSecondary(SecondaryScreen.NOTES) },
                    onNavigateToQRScanner = { navigationViewModel.openSecondary(SecondaryScreen.QR_SCANNER) },
                    onNavigateToQRGenerator = { navigationViewModel.openSecondary(SecondaryScreen.QR_GENERATOR) },
                    onNavigateToPublicHolidays = { navigationViewModel.openSecondary(SecondaryScreen.HOLIDAYS) },
                    onNavigateToMenstrualCalendar = {
                        navigationViewModel.openSecondary(
                            SecondaryScreen.MENSTRUAL_CALENDAR
                        )
                    },
                    onNavigateToVoiceRecognition = {
                        navigationViewModel.openSecondary(
                            SecondaryScreen.VOICE_RECOGNITION
                        )
                    },
                    appSettingsViewModel = appSettingsViewModel,
                    onNavigateToPaint = { navigationViewModel.openSecondary(SecondaryScreen.PAINT) },
                    onNavigateToPasswords = { navigationViewModel.openSecondary(SecondaryScreen.PASSWORD_LIST) },
                    onNavigateToExpenseManager = { navigationViewModel.openSecondary(SecondaryScreen.EXPENSES) },
                    onNavigateToToolsReorder = { navigationViewModel.openSecondary(SecondaryScreen.TOOLS_ORDER) },
                    onNavigateToPomodoro = { navigationViewModel.openSecondary(SecondaryScreen.POMODORO) },
                    listState = extrasListState
                )
            } } }
        }
    }
}

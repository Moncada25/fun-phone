package com.bookverse.contacts.ui.screens.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.data.model.ToolAction
import com.bookverse.contacts.ui.auth.PasswordAuthDialogs
import com.bookverse.contacts.ui.auth.rememberPasswordAuthState
import com.bookverse.contacts.ui.components.GroupItem
import com.bookverse.contacts.ui.components.GroupedCardList
import com.bookverse.contacts.ui.components.MainTopBar
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.utils.ToolsRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onBackClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToGames: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToQRScanner: () -> Unit,
    onNavigateToQRGenerator: () -> Unit,
    onNavigateToPublicHolidays: () -> Unit,
    onNavigateToMenstrualCalendar: () -> Unit,
    onNavigateToVoiceRecognition: () -> Unit,
    onNavigateToPaint: () -> Unit,
    onNavigateToPasswords: () -> Unit,
    onNavigateToExpenseManager: () -> Unit,
    onNavigateToToolsReorder: () -> Unit,
    onNavigateToPomodoro: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    listState: LazyListState
) {
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior()
    val passwordAuthState = rememberPasswordAuthState(appSettingsViewModel)
    val passwordManagerEnabled by appSettingsViewModel.passwordManagerEnabled.collectAsStateWithLifecycle()
    val authMethod by appSettingsViewModel.passwordAuthMethod.collectAsStateWithLifecycle()
    val sensitiveSectionsLockEnabled by appSettingsViewModel.sensitiveSectionsLockEnabled.collectAsStateWithLifecycle()
    val toolsOrder by appSettingsViewModel.toolsOrder.collectAsStateWithLifecycle()

    val toolItems = remember(toolsOrder, passwordManagerEnabled) {
        ToolsRegistry.getToolItems(toolsOrder, passwordManagerEnabled)
    }

    fun handlePasswordClick() {
        passwordAuthState.handleProtectedAction(
            passwordManagerEnabled = passwordManagerEnabled,
            authMethod = authMethod,
            onSuccess = onNavigateToPasswords
        )
    }

    fun handleExpenseManagerClick() {
        if (!sensitiveSectionsLockEnabled) {
            onNavigateToExpenseManager()
            return
        }
        val proceed = { onNavigateToExpenseManager() }
        if (authMethod == null) {
            passwordAuthState.startSetupFlow(onConfigured = proceed)
            return
        }
        passwordAuthState.handleProtectedAction(
            passwordManagerEnabled = sensitiveSectionsLockEnabled,
            authMethod = authMethod,
            onSuccess = proceed
        )
    }

    fun executeToolAction(action: ToolAction) {
        when (action) {
            is ToolAction.NavigateToNotes -> onNavigateToNotes()
            is ToolAction.NavigateToExpenseManager -> handleExpenseManagerClick()
            is ToolAction.NavigateToVoiceRecognition -> onNavigateToVoiceRecognition()
            is ToolAction.NavigateToPublicHolidays -> onNavigateToPublicHolidays()
            is ToolAction.NavigateToGames -> onNavigateToGames()
            is ToolAction.NavigateToPaint -> onNavigateToPaint()
            is ToolAction.NavigateToPasswords -> handlePasswordClick()
            is ToolAction.NavigateToMenstrualCalendar -> onNavigateToMenstrualCalendar()
            is ToolAction.NavigateToQRScanner -> onNavigateToQRScanner()
            is ToolAction.NavigateToQRGenerator -> onNavigateToQRGenerator()
            is ToolAction.NavigateToPomodoro -> onNavigateToPomodoro()
        }
    }

    BackHandler { onBackClick() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MainTopBar(
                title = stringResource(CoreUiR.string.tab_extras),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconAction(
                        imageVector = Icons.Default.Tune,
                        contentDescription = stringResource(CoreUiR.string.tools_customize_title),
                        onClick = onNavigateToToolsReorder
                    )
                    IconAction(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(CoreUiR.string.settings),
                        onClick = onNavigateToSettings
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = paddingValues.calculateTopPadding(), bottom = com.bookverse.contacts.ui.components.LocalMainContentBottomPadding.current),
                state = listState
            ) {
                item {
                    GroupedCardList(
                        items = toolItems.map { toolItem ->
                            {
                                GroupItem(
                                    icon = toolItem.icon,
                                    title = stringResource(toolItem.titleRes),
                                    description = stringResource(toolItem.descriptionRes),
                                    onClick = { executeToolAction(toolItem.action) },
                                )
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
    PasswordAuthDialogs(
        state = passwordAuthState,
    )
}

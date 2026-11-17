package com.bookverse.contacts.ui.screens

import android.app.Application
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.EaseInElastic
import androidx.compose.animation.core.EaseOutElastic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.components.DialerFloatingActionButton
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.domain.contacts.ContactInputPolicy
import com.bookverse.contacts.data.notes.ChecklistItem
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.NoteLookup
import com.bookverse.contacts.data.notes.RecordingData
import com.bookverse.contacts.data.notes.ReminderData
import com.bookverse.contacts.domain.call.CallContactLookup
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.integration.NotesViewModelFactory
import com.bookverse.contacts.integration.PasswordsViewModelFactory
import com.bookverse.contacts.integration.ExpensesViewModelFactory
import com.bookverse.contacts.service.DefaultCallRepository
import com.bookverse.contacts.service.birthday.BirthdayReminderNotifier
import com.bookverse.contacts.service.birthday.BirthdayReminderScheduler
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed
import com.bookverse.contacts.settings.Country
import com.bookverse.contacts.settings.Fonts
import com.bookverse.contacts.settings.TransitionType
import com.bookverse.contacts.ui.auth.PasswordAuthDialogs
import com.bookverse.contacts.ui.auth.rememberPasswordAuthState
import com.bookverse.contacts.ui.components.CallStateBanner
import com.bookverse.contacts.ui.components.StatusBarProtection
import com.bookverse.contacts.ui.navigation.HomeShortcutDestination
import com.bookverse.contacts.ui.navigation.MainTabsScaffold
import com.bookverse.contacts.ui.navigation.NavigationEffects
import com.bookverse.contacts.ui.navigation.NavigationItem
import com.bookverse.contacts.ui.navigation.SecondaryScreenContent
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.theme.accentPalette
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.QRHistoryViewModel
import com.bookverse.contacts.ui.viewmodel.NavigationViewModel
import com.bookverse.contacts.ui.viewmodel.NotesViewModel
import com.bookverse.contacts.ui.viewmodel.ExpensesViewModel
import com.bookverse.contacts.ui.viewmodel.HolidaysViewModel
import com.bookverse.contacts.ui.viewmodel.PasswordsViewModel
import com.bookverse.contacts.utils.SecondaryScreen
import com.bookverse.contacts.ui.navigation.allowsBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    onFontChange: (() -> Unit)? = null,
    appSettingsViewModel: AppSettingsViewModel,
    navigationViewModel: NavigationViewModel,
    openFavorites: Boolean = false,
    openContactDetails: Boolean = false,
    contactId: String? = null,
    contactsViewModel: ContactsViewModel,
    contactTagViewModel: com.bookverse.contacts.ui.viewmodel.ContactTagViewModel,
    qrHistoryViewModel: QRHistoryViewModel,
    pomodoroViewModel: com.bookverse.contacts.ui.viewmodel.PomodoroViewModel,
    noteLookup: NoteLookup,
    retroStatusBarColor: Color?,
    isLimitedMode: Boolean = false,
    openIncoming: Boolean = false,
    incomingCallId: String? = null,
    openInCall: Boolean = false,
    inCallCallId: String? = null,
    inCallNumber: String? = null,
    callContactLookup: CallContactLookup,
    holidaysViewModel: HolidaysViewModel,
    onFavoriteShortcutConsumed: () -> Unit = {},
    onContactShortcutConsumed: () -> Unit = {},
    homeShortcutDestination: HomeShortcutDestination? = null,
    onHomeShortcutConsumed: () -> Unit = {},
    onOpenInCallConsumed: () -> Unit = {},
    openNoteId: Long? = null,
    onNoteOpenConsumed: () -> Unit = {},
    openNotesList: Boolean = false,
    onNotesListConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val app = context.applicationContext as Application
    val backPressedOnce = remember { mutableStateOf(false) }
    val activeCall = DefaultCallRepository.activeCall.collectAsStateWithLifecycle().value

    val notesViewModel: NotesViewModel = viewModel(factory = NotesViewModelFactory(app))
    val passwordsViewModel: PasswordsViewModel = viewModel(factory = PasswordsViewModelFactory(app))
    val expensesViewModel: ExpensesViewModel = viewModel(factory = ExpensesViewModelFactory(app))

    val accentColor = MaterialTheme.colorScheme.primary
    val accentSeed = MaterialTheme.accentPalette.seed
    val themeMode by appSettingsViewModel.themeMode.collectAsStateWithLifecycle()
    val darkModeStyle by appSettingsViewModel.darkModeStyle.collectAsStateWithLifecycle()
    val fontType by appSettingsViewModel.fontType.collectAsStateWithLifecycle()
    val animationSpeed by appSettingsViewModel.animationSpeed.collectAsStateWithLifecycle()
    val transitionType by appSettingsViewModel.transitionType.collectAsStateWithLifecycle()
    val accentBarStyle by appSettingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
    val immersiveIncomingCallsEnabled by appSettingsViewModel.immersiveIncomingCalls.collectAsStateWithLifecycle()
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val fontFamily = remember(fontType) { Fonts.getFontFamily(fontType) }
    val profileContact = contactsViewModel.profile.collectAsStateWithLifecycle().value
    val contacts by contactsViewModel.contacts.collectAsStateWithLifecycle()
    LaunchedEffect(contacts) {
        val hasBirthdays = contacts.any { it.birthday != null }
        if (!hasBirthdays) {
            BirthdayReminderScheduler(context).cancel()
            return@LaunchedEffect
        }

        BirthdayReminderNotifier(context).createChannel()
        val scheduler = BirthdayReminderScheduler(context)
        scheduler.scheduleNext(catchUpToday = true)
    }

    val navState by navigationViewModel.uiState.collectAsStateWithLifecycle()
    val currentScreen = navState.currentScreen
    val secondaryScreen = navState.secondaryScreen
    val toolsOrder by appSettingsViewModel.toolsOrder.collectAsStateWithLifecycle()
    val mainTabs = remember(toolsOrder.toolsMenuHidden) {
        buildList {
            add(NavigationItem.Favorites)
            add(NavigationItem.Contacts)
            add(NavigationItem.Recents)
            if (!toolsOrder.toolsMenuHidden) {
                add(NavigationItem.Extras)
            }
        }
    }
    val mainTabRoutes = mainTabs.map { it.route }
    val isMainTab = mainTabRoutes.contains(currentScreen)

    LaunchedEffect(toolsOrder.toolsMenuHidden) {
        if (toolsOrder.toolsMenuHidden && currentScreen == NavigationItem.Extras.route) {
            navigationViewModel.navigateTo(NavigationItem.Favorites.route)
        }
    }

    var selectedContactId by rememberSaveable { mutableStateOf<String?>(null) }
    var editedContactId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var addContactPhone by remember { mutableStateOf("") }
    var addContactCountry by remember { mutableStateOf<Country?>(null) }
    var contactBeingEdited by remember { mutableStateOf<Contact?>(null) }
    val dialerPhoneNumber = navState.dialerNumber
    LaunchedEffect(contacts, profileContact, selectedContactId, editedContactId) {
        if (selectedContact == null && selectedContactId != null) {
            selectedContact = contacts.firstOrNull { it.id == selectedContactId }
        }
        selectedContact?.let { current ->
            val isMissing = current.id != profileContact?.id && contacts.none { it.id == current.id }
            if (isMissing) {
                ContactInputPolicy.findUniquePhoneMatch(contacts, current)?.let { reaggregated ->
                    selectedContact = reaggregated
                    selectedContactId = reaggregated.id
                }
            }
        }
        if (contactBeingEdited == null && editedContactId != null) {
            contactBeingEdited = contacts.firstOrNull { it.id == editedContactId }
                ?: profileContact?.takeIf { it.id == editedContactId }
        }
    }

    var editorNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editNoteData by remember { mutableStateOf<Pair<Note?, List<ChecklistItem>>?>(null) }
    var editReminderData by remember { mutableStateOf<Pair<Note?, ReminderData?>?>(null) }
    var editRecordingData by remember { mutableStateOf<Pair<Note?, RecordingData?>?>(null) }
    var pendingLockedNote by remember { mutableStateOf<Note?>(null) }
    val widgetNoteScope = rememberCoroutineScope()

    fun openNoteScreen(note: Note) {
        editorNoteId = note.id.takeIf { it > 0 }
        widgetNoteScope.launch {
            val noteId = note.id
            when {
                note.isReminder -> {
                    val reminderData = withContext(Dispatchers.IO) {
                        notesViewModel.getReminderDataForNote(noteId)
                    }
                    editReminderData = note to reminderData
                    navigationViewModel.openSecondary(SecondaryScreen.REMINDER)
                }
                note.isRecording -> {
                    val recordingData = withContext(Dispatchers.IO) {
                        notesViewModel.getRecordingDataForNote(noteId)
                    }
                    editRecordingData = note to recordingData
                    navigationViewModel.openSecondary(SecondaryScreen.RECORDING)
                }
                else -> {
                    val checklist = if (note.isChecklist) {
                        withContext(Dispatchers.IO) {
                            notesViewModel.getChecklistForNote(noteId)
                        }
                    } else emptyList()
                    editNoteData = note to checklist
                    navigationViewModel.openSecondary(
                        if (note.isChecklist) SecondaryScreen.CHECKLIST else SecondaryScreen.NOTE
                    )
                }
            }
        }
    }

    val awaitingEditorReference = editorNoteId != null && when (secondaryScreen) {
        SecondaryScreen.NOTE, SecondaryScreen.CHECKLIST -> editNoteData == null
        SecondaryScreen.REMINDER -> editReminderData == null
        SecondaryScreen.RECORDING -> editRecordingData == null
        else -> false
    }
    LaunchedEffect(editorNoteId, awaitingEditorReference) {
        if (awaitingEditorReference) {
            val note = editorNoteId?.let { noteLookup.findById(it) }
            if (note == null) navigationViewModel.navigateBack() else openNoteScreen(note)
        }
    }

    LaunchedEffect(openNoteId) {
        val noteId = openNoteId ?: return@LaunchedEffect
        if (noteId <= 0L) return@LaunchedEffect
        val note = noteLookup.findById(noteId) ?: return@LaunchedEffect
        editorNoteId = note.id
        if (note.isLocked) {
            pendingLockedNote = note
            onNoteOpenConsumed()
            return@LaunchedEffect
        }
        when {
            note.isReminder -> {
                val reminderData = withContext(Dispatchers.IO) {
                    notesViewModel.getReminderDataForNote(noteId)
                }
                editReminderData = note to reminderData
                navigationViewModel.openSecondary(SecondaryScreen.REMINDER)
            }
            note.isRecording -> {
                val recordingData = withContext(Dispatchers.IO) {
                    notesViewModel.getRecordingDataForNote(noteId)
                }
                editRecordingData = note to recordingData
                navigationViewModel.openSecondary(SecondaryScreen.RECORDING)
            }
            else -> {
                val checklist = if (note.isChecklist) {
                    withContext(Dispatchers.IO) {
                        notesViewModel.getChecklistForNote(noteId)
                    }
                } else emptyList()
                editNoteData = note to checklist
                navigationViewModel.openSecondary(
                    if (note.isChecklist) SecondaryScreen.CHECKLIST else SecondaryScreen.NOTE
                )
            }
        }
        onNoteOpenConsumed()
    }
    LaunchedEffect(openNotesList) {
        if (openNotesList) {
            navigationViewModel.openSecondary(SecondaryScreen.NOTES)
            onNotesListConsumed()
        }
    }
    var pendingIncomingId by remember { mutableStateOf<String?>(null) }
    var pendingInCallId by remember { mutableStateOf<String?>(null) }
    var pendingInCallNumber by remember { mutableStateOf<String?>(null) }
    var userDismissedInCall by rememberSaveable { mutableStateOf(false) }
    var selectedPasswordId by rememberSaveable { mutableStateOf<Long?>(null) }

    val contactsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val favoritesGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val historyListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val extrasListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val settingsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    val pagerState = rememberPagerState(
        initialPage = mainTabRoutes.indexOf(currentScreen).coerceAtLeast(0)
    ) { mainTabRoutes.size }

    val listStateProvider: () -> LazyListState? = {
        when (currentScreen) {
            NavigationItem.Contacts.route -> contactsListState
            NavigationItem.Recents.route -> historyListState
            NavigationItem.Extras.route -> extrasListState
            else -> null
        }
    }

    val gridStateProvider: () -> LazyGridState? = {
        when (currentScreen) {
            NavigationItem.Favorites.route -> favoritesGridState
            else -> null
        }
    }

    val routeStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val appBarStates = com.bookverse.contacts.ui.components.rememberAppBarStateRegistry()
    val duration = animationSpeed.durationMs

    val passwordAuthState = rememberPasswordAuthState(appSettingsViewModel)
    val passwordBackupActions = com.bookverse.contacts.ui.screens.passwords.rememberPasswordBackupActions(
        active = secondaryScreen == SecondaryScreen.PASSWORD_LIST,
        settings = appSettingsViewModel,
        passwords = passwordsViewModel,
    )
    val noteImagePicker = com.bookverse.contacts.ui.screens.notes.rememberNoteImagePicker()
    val passwordManagerEnabled by appSettingsViewModel.passwordManagerEnabled.collectAsStateWithLifecycle()
    val sensitiveSectionsLockEnabled by appSettingsViewModel.sensitiveSectionsLockEnabled.collectAsStateWithLifecycle()
    val passwordAuthMethod by appSettingsViewModel.passwordAuthMethod.collectAsStateWithLifecycle()
    val protectedSessionUnlocked by appSettingsViewModel.protectedSessionUnlocked.collectAsStateWithLifecycle()
    val protectedLifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(protectedLifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                appSettingsViewModel.lockProtectedSession()
                passwordsViewModel.lock()
            }
        }
        protectedLifecycle.addObserver(observer)
        onDispose { protectedLifecycle.removeObserver(observer); passwordsViewModel.lock() }
    }
    LaunchedEffect(protectedSessionUnlocked, passwordManagerEnabled) {
        if (protectedSessionUnlocked && passwordManagerEnabled) passwordsViewModel.unlock() else passwordsViewModel.lock()
    }


    LaunchedEffect(pendingLockedNote) {
        val note = pendingLockedNote ?: return@LaunchedEffect
        passwordAuthState.handleProtectedAction(
            passwordManagerEnabled = passwordManagerEnabled,
            authMethod = passwordAuthMethod,
            onSuccess = {
                openNoteScreen(note)
                pendingLockedNote = null
            }
        )
    }

    LaunchedEffect(
        activeCall?.id,
        activeCall?.state,
        immersiveIncomingCallsEnabled,
        secondaryScreen
    ) {
        if (
            immersiveIncomingCallsEnabled &&
            activeCall?.state == CallState.RINGING &&
            secondaryScreen != SecondaryScreen.INCOMING_CALL
        ) {
            pendingIncomingId = activeCall.id
            navigationViewModel.openSecondary(SecondaryScreen.INCOMING_CALL)
        }
    }

    LaunchedEffect(homeShortcutDestination) {
        if (homeShortcutDestination == HomeShortcutDestination.NEW_NOTE) {
            editorNoteId = null
            editNoteData = null to emptyList()
        }
    }

    NavigationEffects(
        currentScreen = currentScreen,
        pagerState = pagerState,
        mainTabRoutes = mainTabRoutes,
        navigationViewModel = navigationViewModel,
        openFavorites = openFavorites,
        openContactDetails = openContactDetails,
        contactId = contactId,
        contactsViewModel = contactsViewModel,
        onFavoriteShortcutConsumed = onFavoriteShortcutConsumed,
        onContactShortcutConsumed = onContactShortcutConsumed,
        onSelectedContactChange = { selectedContact = it; selectedContactId = it?.id },
        openIncoming = openIncoming,
        incomingCallId = incomingCallId,
        openInCall = openInCall,
        inCallCallId = inCallCallId,
        inCallNumber = inCallNumber,
        onPendingIncomingIdChange = { pendingIncomingId = it },
        onPendingInCallIdChange = { pendingInCallId = it },
        onPendingInCallNumberChange = { pendingInCallNumber = it },
        onUserDismissedInCallChange = { userDismissedInCall = it },
        onOpenInCallConsumed = onOpenInCallConsumed,
        activeCall = activeCall,
        userDismissedInCall = userDismissedInCall,
        homeShortcutDestination = homeShortcutDestination,
        onHomeShortcutConsumed = onHomeShortcutConsumed,
        passwordAuthState = passwordAuthState,
        passwordManagerEnabled = passwordManagerEnabled,
        sensitiveSectionsLockEnabled = sensitiveSectionsLockEnabled,
        authMethod = passwordAuthMethod
    )

    fun getTransitionSpec(slideDirection: Int) = when (transitionType) {
        TransitionType.SCALE -> scaleIn(animationSpec = tween(duration)) togetherWith scaleOut(
            animationSpec = tween(duration)
        )

        TransitionType.SLIDE -> {
            if (slideDirection > 0) {
                slideInHorizontally(animationSpec = tween(duration)) { it } togetherWith slideOutHorizontally(
                    animationSpec = tween(duration)
                ) { -it }
            } else {
                slideInHorizontally(animationSpec = tween(duration)) { -it } togetherWith slideOutHorizontally(
                    animationSpec = tween(duration)
                ) { it }
            }
        }

        TransitionType.FADE -> fadeIn(animationSpec = tween(duration)) togetherWith fadeOut(
            animationSpec = tween(duration)
        )

        TransitionType.FLIP -> slideInVertically(animationSpec = tween(duration)) { it * slideDirection } togetherWith slideOutVertically(
            animationSpec = tween(duration)
        ) { -it * slideDirection }

        TransitionType.SHRINK -> scaleIn(
            animationSpec = tween(duration),
            initialScale = if (slideDirection > 0) 0.5f else 0f
        ) togetherWith scaleOut(animationSpec = tween(duration), targetScale = if (slideDirection > 0) 0f else 0.5f)

        TransitionType.EXPAND -> scaleIn(
            animationSpec = tween(duration),
            initialScale = if (slideDirection > 0) 1.5f else 2f
        ) togetherWith scaleOut(animationSpec = tween(duration), targetScale = if (slideDirection > 0) 2f else 1.5f)

        TransitionType.ELASTIC -> scaleIn(
            animationSpec = tween(
                duration,
                easing = EaseOutElastic
            )
        ) togetherWith scaleOut(animationSpec = tween(duration, easing = EaseInElastic))

        TransitionType.CIRCLE_REVEAL -> scaleIn(
            animationSpec = tween(duration),
            initialScale = if (slideDirection > 0) 0.1f else 1.5f,
            transformOrigin = TransformOrigin(0.5f, 0.5f)
        ) + fadeIn(
            animationSpec = tween(duration / 2, delayMillis = duration / 4)
        ) togetherWith scaleOut(
            animationSpec = tween(duration / 2),
            targetScale = if (slideDirection > 0) 1.5f else 0.1f,
            transformOrigin = TransformOrigin(0.5f, 0.5f)
        ) + fadeOut(
            animationSpec = tween(duration / 2)
        )

        TransitionType.ROTATE -> slideInHorizontally(
            animationSpec = tween(duration)
        ) { it / 2 * slideDirection } + scaleIn(
            animationSpec = tween(duration),
            initialScale = 0.8f
        ) togetherWith slideOutHorizontally(
            animationSpec = tween(duration)
        ) { -it / 2 * slideDirection } + scaleOut(
            animationSpec = tween(duration),
            targetScale = 0.8f
        )

        TransitionType.ZOOM_BLUR -> scaleIn(
            animationSpec = tween(duration),
            initialScale = if (slideDirection > 0) 1.2f else 0.8f
        ) + fadeIn(animationSpec = tween(duration)) togetherWith scaleOut(
            animationSpec = tween(duration),
            targetScale = if (slideDirection > 0) 0.8f else 1.2f
        ) + fadeOut(animationSpec = tween(duration))

        TransitionType.WAVE -> slideInVertically(
            animationSpec = tween(duration)
        ) { it / 3 * slideDirection } + slideInHorizontally(
            animationSpec = tween(duration)
        ) { -it / 3 * slideDirection } togetherWith slideOutVertically(
            animationSpec = tween(duration)
        ) { -it / 3 * slideDirection } + slideOutHorizontally(
            animationSpec = tween(duration)
        ) { it / 3 * slideDirection }
    }

    BackHandler {
        if (secondaryScreen != null) {
            navigationViewModel.navigateBack()
            backPressedOnce.value = false
        } else if (currentScreen != NavigationItem.Favorites.route) {
            navigationViewModel.navigateTo(NavigationItem.Favorites.route)
            backPressedOnce.value = false
        } else {
            if (!backPressedOnce.value) {
                backPressedOnce.value = true
                Toast.makeText(
                    context,
                    resources.getString(R.string.toast_press_back_again_to_exit),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                (context as? Activity)?.finish()
            }
        }
    }

    CompositionLocalProvider(com.bookverse.contacts.ui.components.LocalAppBarStateRegistry provides appBarStates) {
        CompositionLocalProvider(LocalAnimationSpeed provides animationSpeed) {
            ContactsAppTheme(
                accentSeed = accentSeed,
                fontFamily = fontFamily,
                fontScale = 1f,
                themeMode = themeMode,
                darkModeStyle = darkModeStyle,
            ) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor)
                            .padding(paddingValues)
                    ) {
                        AnimatedContent(
                            targetState = secondaryScreen,
                            transitionSpec = { getTransitionSpec(if (navState.direction == com.bookverse.contacts.ui.viewmodel.NavigationDirection.FORWARD) 1 else -1) },
                            modifier = Modifier.fillMaxSize()
                        ) { screen ->
                            when (screen) {
                                null -> routeStateHolder.SaveableStateProvider("main") { MainTabsScaffold(
                                    isMainTab = isMainTab,
                                    pagerState = pagerState,
                                    backgroundColor = backgroundColor,
                                    navigationViewModel = navigationViewModel,
                                    mainTabs = mainTabs,
                                    favoritesGridState = favoritesGridState,
                                    contactsListState = contactsListState,
                                    historyListState = historyListState,
                                    extrasListState = extrasListState,
                                    contactsViewModel = contactsViewModel,
                                    contactTagViewModel = contactTagViewModel,
                                    appSettingsViewModel = appSettingsViewModel,
                                    profileContact = profileContact,
                                    isLimitedMode = isLimitedMode,
                                    onSelectContact = {
                                        selectedContact = it
                                        selectedContactId = it.id
                                        navigationViewModel.openSecondary(SecondaryScreen.DETAIL)
                                    },
                                    onOpenFavoritesOrder = {
                                        navigationViewModel.openSecondary(SecondaryScreen.FAVORITES_ORDER)
                                    },
                                    onNavigateToProfile = {
                                        navigationViewModel.openSecondary(SecondaryScreen.PROFILE)
                                    },
                                    onCreateContact = {
                                        navigationViewModel.openSecondary(SecondaryScreen.ADD_CONTACT)
                                    },
                                    onDialerPhoneNumberChange = navigationViewModel::setDialerNumber,
                                    floatingActionButton = {
                                        val listState = listStateProvider()
                                        val gridState = gridStateProvider()
                                        val hideOnScroll by remember(listState, gridState) {
                                            derivedStateOf {
                                                when {
                                                    listState != null ->
                                                        listState.isScrollInProgress &&
                                                            listState.firstVisibleItemScrollOffset > 0
                                                    gridState != null ->
                                                        gridState.isScrollInProgress &&
                                                            gridState.firstVisibleItemScrollOffset > 0
                                                    else -> false
                                                }
                                            }
                                        }

                                        AnimatedVisibility(visible = isMainTab && (accentBarStyle == AccentBarStyle.MODERN || !hideOnScroll)) {
                                            DialerFloatingActionButton(
                                                style = accentBarStyle,
                                                onClick = { navigationViewModel.openDialer() },
                                            )
                                        }
                                    },
                                ) }

                                else -> androidx.compose.runtime.CompositionLocalProvider(
                                    com.bookverse.contacts.ui.components.LocalBackdropAllowed provides screen.allowsBackdrop(),
                                    com.bookverse.contacts.ui.components.LocalAppBarRoute provides screen.name,
                                    com.bookverse.contacts.ui.screens.notes.LocalNoteImagePicker provides noteImagePicker,
                                ) { if (!awaitingEditorReference) com.bookverse.contacts.ui.auth.ProtectedScreenHost(
                                    screen = screen,
                                    lockedNote = editNoteData?.first?.isLocked == true || editReminderData?.first?.isLocked == true || editRecordingData?.first?.isLocked == true,
                                    settings = appSettingsViewModel,
                                    passwordAuthState = passwordAuthState,
                                    onLock = { passwordsViewModel.lock() },
                                    onCancel = { navigationViewModel.closeSecondary() },
                                ) { SecondaryScreenContent(
                                    passwordBackupActions = passwordBackupActions,
                                    screen = screen,
                                    navigationViewModel = navigationViewModel,
                                    appSettingsViewModel = appSettingsViewModel,
                                    themeMode = themeMode,
                                    contactsViewModel = contactsViewModel,
                                    callContactLookup = callContactLookup,
                                    holidaysViewModel = holidaysViewModel,
                                    qrHistoryViewModel = qrHistoryViewModel,
                                    notesViewModel = notesViewModel,
                                    passwordsViewModel = passwordsViewModel,
                                    expensesViewModel = expensesViewModel,
                                    pomodoroViewModel = pomodoroViewModel,
                                    contactTagViewModel = contactTagViewModel,
                                    selectedContact = selectedContact,
                                    onSelectedContactChange = { selectedContact = it; selectedContactId = it?.id },
                                    contactBeingEdited = contactBeingEdited,
                                    onContactBeingEditedChange = { contactBeingEdited = it; editedContactId = it?.id },
                                    addContactPhone = addContactPhone,
                                    onAddContactPhoneChange = { addContactPhone = it },
                                    addContactCountry = addContactCountry,
                                    onAddContactCountryChange = { addContactCountry = it },
                                    dialerPhoneNumber = dialerPhoneNumber,
                                    onDialerPhoneNumberChange = navigationViewModel::setDialerNumber,
                                    editNoteData = editNoteData,
                                    onEditNoteDataChange = { editNoteData = it; editorNoteId = it?.first?.id?.takeIf { id -> id > 0 } },
                                    editReminderData = editReminderData,
                                    onEditReminderDataChange = { editReminderData = it; editorNoteId = it?.first?.id?.takeIf { id -> id > 0 } },
                                    editRecordingData = editRecordingData,
                                    onEditRecordingDataChange = { editRecordingData = it; editorNoteId = it?.first?.id?.takeIf { id -> id > 0 } },
                                    pendingIncomingId = pendingIncomingId,
                                    pendingInCallId = pendingInCallId,
                                    pendingInCallNumber = pendingInCallNumber,
                                    selectedPasswordId = selectedPasswordId,
                                    onSelectedPasswordIdChange = { selectedPasswordId = it },
                                    onFontChange = onFontChange,
                                    settingsListState = settingsListState,
                                ) } }
                            }
                        }

                        if (accentBarStyle != AccentBarStyle.MODERN) {
                            StatusBarProtection(retroStatusBarColor)
                        }

                        CallStateBanner(
                            currentSecondaryScreen = secondaryScreen,
                            accentColor = accentColor,
                            callContactLookup = callContactLookup,
                            onNavigate = { call ->
                                if (call.state == CallState.DISCONNECTED) return@CallStateBanner
                                // The user explicitly asked to return to the call, so a prior
                                // Back-press dismissal must not linger: NavigationEffects only
                                // clears userDismissedInCall asynchronously after IN_CALL is
                                // already open.
                                userDismissedInCall = false
                                when (call.state) {
                                    CallState.RINGING -> {
                                        pendingIncomingId = call.id
                                        navigationViewModel.openSecondary(SecondaryScreen.INCOMING_CALL)
                                    }
                                    else -> {
                                        pendingInCallId = call.id
                                        pendingInCallNumber = call.number
                                        navigationViewModel.openSecondary(SecondaryScreen.IN_CALL)
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }
                }
            }
        }

        PasswordAuthDialogs(
            state = passwordAuthState,
        )
    }
}

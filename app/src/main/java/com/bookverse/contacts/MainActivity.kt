package com.bookverse.contacts

import com.bookverse.contacts.utils.PhoneActions
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bookverse.contacts.data.service.ContactSyncWorker
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.domain.call.IncomingCallKeyAction
import com.bookverse.contacts.domain.call.IncomingCallKeyPolicy
import com.bookverse.contacts.domain.call.UiCall
import com.bookverse.contacts.domain.review.ReviewEligibilityStore
import com.bookverse.contacts.integration.RoomNotesRepository
import com.bookverse.contacts.service.CallController
import com.bookverse.contacts.service.CallNotificationManager
import com.bookverse.contacts.service.DefaultCallRepository
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed
import com.bookverse.contacts.settings.Fonts
import com.bookverse.contacts.ui.activities.LocalizedAppCompatActivity
import com.bookverse.contacts.security.AppLockSession
import com.bookverse.contacts.ui.auth.AppLockGate
import com.bookverse.contacts.ui.navigation.DynamicShortcuts
import com.bookverse.contacts.ui.navigation.HomeShortcutDestination
import com.bookverse.contacts.ui.navigation.NavigationItem
import com.bookverse.contacts.ui.navigation.resolveDialerLaunchRequest
import com.bookverse.contacts.ui.screens.MainScreen
import com.bookverse.contacts.ui.screens.settings.OnboardingFlowScreen
import com.bookverse.contacts.ui.screens.settings.readPhoneStatus
import com.bookverse.contacts.ui.system.configureTransparentSystemBars
import com.bookverse.contacts.ui.system.updateStatusBarContrast
import com.bookverse.contacts.ui.screens.calls.SimSelectionHost
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.theme.ThemeRevealHost
import com.bookverse.contacts.ui.theme.themeRevealDurationMs
import com.bookverse.contacts.ui.theme.accentPalette
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactTagViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.HolidaysViewModel
import com.bookverse.contacts.ui.viewmodel.NavigationViewModel
import com.bookverse.contacts.ui.viewmodel.PomodoroViewModel
import com.bookverse.contacts.ui.viewmodel.QRHistoryViewModel
import com.bookverse.contacts.utils.AppVisibility
import com.bookverse.contacts.utils.PermissionUtils
import com.bookverse.contacts.utils.SecondaryScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : LocalizedAppCompatActivity() {
    private val appContainer by lazy { (application as App).container }
    private val settingsViewModel: AppSettingsViewModel by viewModels {
        appContainer.appSettingsViewModelFactory
    }
    private val contactsViewModel: ContactsViewModel by viewModels {
        appContainer.contactsViewModelFactory
    }
    private val holidaysViewModel: HolidaysViewModel by viewModels {
        appContainer.holidaysViewModelFactory
    }
    private val callContactLookup by lazy {
        appContainer.callContactLookupFactory.create(contactsViewModel)
    }
    private val qrHistoryViewModel: QRHistoryViewModel by viewModels {
        appContainer.qrHistoryViewModelFactory
    }
    private val pomodoroViewModel: PomodoroViewModel by viewModels()
    private val navigationViewModel: NavigationViewModel by viewModels()
    private val contactTagViewModel: ContactTagViewModel by viewModels {
        appContainer.contactTagViewModelFactory
    }
    private val noteLookup by lazy { RoomNotesRepository(applicationContext) }
    private var isDataLoaded by mutableStateOf(false)
    private var hasCompletedOnboarding by mutableStateOf(false)
    private var isLimitedMode by mutableStateOf(false)

    private var openFavoritesShortcut by mutableStateOf(false)
    private var openContactDetailsShortcut by mutableStateOf(false)
    private var widgetContactId by mutableStateOf<String?>(null)
    private var homeShortcutDestination by mutableStateOf<HomeShortcutDestination?>(null)

    private var openIncoming by mutableStateOf(false)
    private var incomingCallId by mutableStateOf<String?>(null)
    private var openInCall by mutableStateOf(false)
    private var inCallCallId by mutableStateOf<String?>(null)
    private var inCallNumber by mutableStateOf<String?>(null)
    private var openRecents by mutableStateOf(false)
    private var openPomodoro by mutableStateOf(false)
    private var openNotesList by mutableStateOf(false)
    private var openNoteId by mutableStateOf<Long?>(null)

    private var lockScreenModeEnabled = false
    private var answerWithVolumeUpEnabled = false
    private var declineWithVolumeDownEnabled = false
    private var hadActiveCallForReview = false
    private var leaveAfterCall = false
    private var visibleBeforeNewIntent = false
    private var reviewPromptInFlight = false

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onStart() {
        super.onStart()
        AppVisibility.isForeground = true
        CallNotificationManager.ensureCallChannels(this)
        CallNotificationManager.stopOngoing(this)
        val call = DefaultCallRepository.activeCall.value
        when (resolveCallRestoreTarget(call)) {
            CallRestoreTarget.INCOMING -> {
                if (call != null) {
                    openIncoming = true
                    incomingCallId = call.id
                }
            }
            CallRestoreTarget.IN_CALL -> {
                if (call != null) {
                    openInCall = true
                    inCallCallId = call.id
                    inCallNumber = call.number
                }
            }
            CallRestoreTarget.NONE -> Unit
        }
    }

    override fun onResume() {
        super.onResume()
        visibleBeforeNewIntent = true
        refreshPhoneSetup()
        maybeLaunchInAppReview()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        AppLockSession.onOwnActivityLaunchedForResult()
        super.startActivityForResult(intent, requestCode, options)
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        AppLockSession.onOwnActivityResult(SystemClock.elapsedRealtime())
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStop() {
        super.onStop()
        AppVisibility.isForeground = false
        visibleBeforeNewIntent = false
        val c = DefaultCallRepository.activeCall.value
        if (c != null && (c.state == CallState.ACTIVE || c.state == CallState.DIALING || c.state == CallState.ON_HOLD)) {
            CallNotificationManager.showOngoing(this, c.id, c.number)
        } else {
            CallNotificationManager.stopOngoing(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupPermissionLauncher()
        checkOnboardingStatus()
        installSplashScreen().setKeepOnScreenCondition { !isDataLoaded }
        super.onCreate(savedInstanceState)
        PhoneActions.setupCallPermissionLauncher(this)
        readCallExtras(intent)
        if (savedInstanceState == null) {
            handleMissedCallExtras(intent)
            readWidgetAndShortcutExtras(intent.extras)
        } else {
            leaveAfterCall = savedInstanceState.getBoolean(STATE_LEAVE_AFTER_CALL)
            readWidgetAndShortcutExtras(savedInstanceState.getBundle(STATE_PENDING_SHORTCUTS))
        }
        readDialerExtras(intent, initialLaunch = true)
        updateLockScreenBehavior(
            shouldShowCallWhileLocked(DefaultCallRepository.activeCall.value) || openIncoming || openInCall
        )
        val composeView = ComposeView(this)
        setContentView(composeView)
        composeView.setContent {
            val settingsViewModel = this@MainActivity.settingsViewModel
            val contactsViewModel = this@MainActivity.contactsViewModel
            val navigationViewModel = this@MainActivity.navigationViewModel
            LaunchedEffect(openPomodoro) {
                if (openPomodoro) {
                    navigationViewModel.navigateTo(NavigationItem.Extras.route)
                    navigationViewModel.openSecondary(SecondaryScreen.POMODORO)
                    openPomodoro = false
                }
            }
            LaunchedEffect(Unit) {
                isDataLoaded = false
                withContext(Dispatchers.IO) {
                    try {
                        val settingsDeferred = async {
                            settingsViewModel.preloadSettings(application)
                        }
                        val contactsDeferred = async {
                            if (hasCompletedOnboarding && !isLimitedMode) {
                                contactsViewModel.preloadContacts()
                            } else null
                        }
                        settingsDeferred.await()
                        val preloadResult =
                            if (hasCompletedOnboarding && !isLimitedMode) contactsDeferred.await() else null
                        if (preloadResult != null) {
                            DiagnosticLog.debug(DiagnosticEvent.APP_CONTACTS_PRELOAD_COMPLETED)
                        }
                        DiagnosticLog.debug(DiagnosticEvent.APP_DATA_LOAD_COMPLETED)
                    } catch (_: Exception) {
                        DiagnosticLog.error(DiagnosticEvent.APP_DATA_LOAD_FAILED)
                    } finally {
                        isDataLoaded = true
                    }
                }
                if (hasCompletedOnboarding && !isLimitedMode) {
                    scheduleContactSync()
                }
            }

            LaunchedEffect(isDataLoaded, hasCompletedOnboarding, isLimitedMode) {
                if (isDataLoaded && hasCompletedOnboarding && !isLimitedMode) {
                    withContext(Dispatchers.IO) {
                        try {
                            contactsViewModel.loadAdditionalData()
                            DiagnosticLog.debug(DiagnosticEvent.APP_ADDITIONAL_DATA_LOAD_COMPLETED)
                        } catch (_: Exception) {
                            DiagnosticLog.error(DiagnosticEvent.APP_ADDITIONAL_DATA_LOAD_FAILED)
                        }
                    }
                }
            }

            val startupScreen by settingsViewModel.startupScreen.collectAsStateWithLifecycle()
            LaunchedEffect(startupScreen, isDataLoaded, openRecents) {
                if (isDataLoaded) {
                    navigationViewModel.setInitialRoute(startupScreen)
                    if (openRecents) {
                        navigationViewModel.navigateTo(NavigationItem.Recents.route)
                        openRecents = false
                    }
                }
            }

            val animationSpeed by settingsViewModel.animationSpeed.collectAsStateWithLifecycle()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val darkModeStyle by settingsViewModel.darkModeStyle.collectAsStateWithLifecycle()
            val accentColor by settingsViewModel.accentColor.collectAsStateWithLifecycle()
            val accentBarStyle by settingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
            val openFavorites = openFavoritesShortcut
            val openContactDetails = openContactDetailsShortcut
            val contactId = widgetContactId
            val navigationUiState by navigationViewModel.uiState.collectAsStateWithLifecycle()
            val secondaryScreen = navigationUiState.secondaryScreen
            LaunchedEffect(secondaryScreen) {
                if (secondaryScreen == null) refreshPhoneSetup()
            }
            CompositionLocalProvider(LocalAnimationSpeed provides animationSpeed) {
                val fontType by settingsViewModel.fontType.collectAsStateWithLifecycle()
                val fontFamily = remember(fontType) { Fonts.getFontFamily(fontType) }
                val fontScale by settingsViewModel.fontScale.collectAsStateWithLifecycle()
                val glassEffects by settingsViewModel.glassEffects.collectAsStateWithLifecycle()
                ThemeRevealHost(
                    target = themeMode to darkModeStyle,
                    durationMs = themeRevealDurationMs(animationSpeed.durationMs),
                    modifier = Modifier.fillMaxSize(),
                ) { appearance ->
                    ContactsAppTheme(
                        accentSeed = accentColor,
                        fontFamily = fontFamily,
                        fontScale = fontScale,
                        themeMode = appearance.first,
                        darkModeStyle = appearance.second,
                        accentBarStyle = accentBarStyle,
                        glassEffects = glassEffects,
                    ) {
                        val backgroundColor = MaterialTheme.colorScheme.background
                        val isIncomingCall = secondaryScreen == SecondaryScreen.INCOMING_CALL
                        val retroStatusBarColor = MaterialTheme.accentPalette.seed.takeIf {
                            accentBarStyle == AccentBarStyle.RETRO &&
                                !isIncomingCall && secondaryScreen != SecondaryScreen.IN_CALL
                        }
                        val statusBarBackground = retroStatusBarColor ?: backgroundColor
                        val systemBarsReady = isDataLoaded && hasCompletedOnboarding
                        LaunchedEffect(systemBarsReady, backgroundColor) {
                            if (systemBarsReady) {
                                configureTransparentSystemBars(backgroundColor, statusBarBackground)
                            }
                        }
                        LaunchedEffect(systemBarsReady, statusBarBackground, isIncomingCall) {
                            if (systemBarsReady && !isIncomingCall) {
                                updateStatusBarContrast(statusBarBackground)
                            }
                        }
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            when {
                                // A shortcut must not mistake unloaded auth settings for an unconfigured PIN.
                                !isDataLoaded -> Unit
                                !hasCompletedOnboarding -> {
                                    OnboardingFlowScreen(onFinish = { limited, reviewPhoneStatus ->
                                        isLimitedMode = limited
                                        hasCompletedOnboarding = true
                                        saveOnboardingCompleted()
                                        if (reviewPhoneStatus) {
                                            navigationViewModel.openSecondary(
                                                SecondaryScreen.PHONE_STATUS
                                            )
                                        }
                                        if (!limited) {
                                            contactsViewModel.loadContacts()
                                            scheduleContactSync()
                                        }
                                    }, settingsViewModel = settingsViewModel)
                                }

                                else -> {
                                    MainScreen(
                                        appSettingsViewModel = settingsViewModel,
                                        navigationViewModel = navigationViewModel,
                                        openFavorites = openFavorites,
                                        openContactDetails = openContactDetails,
                                        contactId = contactId,
                                        contactsViewModel = contactsViewModel,
                                        callContactLookup = callContactLookup,
                                        holidaysViewModel = holidaysViewModel,
                                        contactTagViewModel = contactTagViewModel,
                                        qrHistoryViewModel = qrHistoryViewModel,
                                        pomodoroViewModel = pomodoroViewModel,
                                        noteLookup = noteLookup,
                                        retroStatusBarColor = retroStatusBarColor,
                                        isLimitedMode = isLimitedMode,
                                        openIncoming = openIncoming,
                                        incomingCallId = incomingCallId,
                                        openInCall = openInCall,
                                        inCallCallId = inCallCallId,
                                        inCallNumber = inCallNumber,
                                        onFavoriteShortcutConsumed = {
                                            openFavoritesShortcut = false
                                        },
                                        onContactShortcutConsumed = {
                                            openContactDetailsShortcut = false; widgetContactId =
                                            null
                                        },
                                        homeShortcutDestination = homeShortcutDestination,
                                        onHomeShortcutConsumed = { homeShortcutDestination = null },
                                        onOpenInCallConsumed = { openInCall = false },
                                        openNoteId = openNoteId,
                                        onNoteOpenConsumed = { openNoteId = null },
                                        openNotesList = openNotesList,
                                        onNotesListConsumed = { openNotesList = false },
                                    )
                                    val lockScopedCall by DefaultCallRepository.activeCall.collectAsStateWithLifecycle()
                                    val vaultShortcutEnabled by settingsViewModel.passwordManagerEnabled.collectAsStateWithLifecycle()
                                    LaunchedEffect(vaultShortcutEnabled) {
                                        withContext(Dispatchers.IO) {
                                            DynamicShortcuts.publish(this@MainActivity, vaultShortcutEnabled)
                                        }
                                    }
                                    AppLockGate(
                                        onCallScreen = secondaryScreen == SecondaryScreen.INCOMING_CALL ||
                                            secondaryScreen == SecondaryScreen.IN_CALL,
                                        callActive = shouldShowCallWhileLocked(lockScopedCall),
                                        onReturnToCall = {
                                            navigationViewModel.openSecondary(
                                                if (lockScopedCall?.state == CallState.RINGING) {
                                                    SecondaryScreen.INCOMING_CALL
                                                } else {
                                                    SecondaryScreen.IN_CALL
                                                },
                                            )
                                        },
                                    )
                                }
                            }
                            SimSelectionHost()
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            DefaultCallRepository.calls
                .map { calls -> calls.any { it.state != CallState.DISCONNECTED } }
                .distinctUntilChanged()
                .collect { hasOngoingCall ->
                    val started = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                    if (shouldLeaveAfterCallEnds(leaveAfterCall, hasOngoingCall, started)) {
                        moveTaskToBack(true)
                    }
                    if (!hasOngoingCall) leaveAfterCall = false
                }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    DefaultCallRepository.activeCall.collect { call ->
                        updateLockScreenBehavior(shouldShowCallWhileLocked(call) || openInCall)
                        if (call != null) {
                            hadActiveCallForReview = true
                        } else if (hadActiveCallForReview) {
                            hadActiveCallForReview = false
                            maybeLaunchInAppReview()
                        }
                    }
                }
                launch {
                    settingsViewModel.answerWithVolumeUpEnabled.collect { enabled ->
                        answerWithVolumeUpEnabled = enabled
                    }
                }
                launch {
                    settingsViewModel.declineWithVolumeDownEnabled.collect { enabled ->
                        declineWithVolumeDownEnabled = enabled
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_LEAVE_AFTER_CALL, leaveAfterCall)
        outState.putBundle(STATE_PENDING_SHORTCUTS, Bundle().apply {
            if (openFavoritesShortcut) putBoolean("open_favorites", true)
            if (openRecents) putBoolean("open_recents", true)
            if (openContactDetailsShortcut) {
                putBoolean("open_contact_details", true)
                putString("contact_id", widgetContactId)
            }
            if (openPomodoro) putBoolean("OPEN_POMODORO", true)
            if (openNotesList) putBoolean("open_notes_list", true)
            openNoteId?.let { id ->
                putBoolean("open_note", true)
                putLong("note_id", id)
            }
            homeShortcutDestination?.let { putString(HomeShortcutDestination.EXTRA_KEY, it.id) }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readCallExtras(intent)
        handleMissedCallExtras(intent)
        readDialerExtras(intent)
        readWidgetAndShortcutExtras(intent.extras)
        updateLockScreenBehavior(
            shouldShowCallWhileLocked(DefaultCallRepository.activeCall.value) || openIncoming || openInCall
        )
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (handleIncomingCallKey(keyCode)) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun readCallExtras(intent: Intent) {
        if (intent.hasExtra("open_incoming")) {
            openIncoming = intent.getBooleanExtra("open_incoming", false)
            incomingCallId = intent.getStringExtra("incoming_call_id")
        }
        if (intent.hasExtra("open_incall")) {
            openInCall = intent.getBooleanExtra("open_incall", false)
            inCallCallId = intent.getStringExtra("incall_call_id")
            inCallNumber = intent.getStringExtra("incall_number")
            if (isCallOpenedFromOutside(openInCall, visibleBeforeNewIntent)) leaveAfterCall = true
        }
    }

    private fun handleMissedCallExtras(intent: Intent) {
        if (!intent.getBooleanExtra(CallNotificationManager.EXTRA_CLEAR_MISSED_CALLS, false)) return
        intent.removeExtra(CallNotificationManager.EXTRA_CLEAR_MISSED_CALLS)
        CallNotificationManager.clearMissedCalls(this)
        val number = intent.getStringExtra(CallNotificationManager.EXTRA_MISSED_CALL_NUMBER)
        val action = intent.getStringExtra(CallNotificationManager.EXTRA_MISSED_CALL_ACTION)
        intent.removeExtra(CallNotificationManager.EXTRA_MISSED_CALL_ACTION)
        if (number.isNullOrBlank()) return
        when (action) {
            CallNotificationManager.MISSED_ACTION_CALL -> PhoneActions.callPhoneNumber(this, number)
            CallNotificationManager.MISSED_ACTION_MESSAGE ->
                runCatching { PhoneActions.openSmsApp(this, number) }
        }
    }

    private fun readDialerExtras(intent: Intent, initialLaunch: Boolean = false) {
        val request = resolveDialerLaunchRequest(intent.action, intent.data?.scheme, intent.data?.schemeSpecificPart)
        if (initialLaunch) navigationViewModel.handleInitialDialerLaunch(request)
        else request?.let { navigationViewModel.openDialer(it.number) }
        if (request != null) DiagnosticLog.debug(DiagnosticEvent.APP_DIALER_OPENED)
    }

    private fun readWidgetAndShortcutExtras(extras: Bundle?) {
        homeShortcutDestination = HomeShortcutDestination.fromId(extras?.getString(HomeShortcutDestination.EXTRA_KEY))
        if (extras == null) return
        if (extras.containsKey("open_recents")) {
            openRecents = extras.getBoolean("open_recents")
        }
        if (extras.getBoolean("open_favorites")) {
            openFavoritesShortcut = true
        }
        if (extras.getBoolean("open_contact_details")) {
            openContactDetailsShortcut = true
            widgetContactId = extras.getString("contact_id")
        }
        if (extras.getBoolean("OPEN_POMODORO")) {
            openPomodoro = true
        }
        if (extras.getBoolean("open_notes_list")) {
            openNotesList = true
        }
        if (extras.getBoolean("open_note")) {
            openNoteId = extras.getLong("note_id")
        }
    }

    private fun setupPermissionLauncher() {
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val allGranted = permissions.all { it.value }
                if (!allGranted && !hasCompletedOnboarding) {
                    isLimitedMode = true
                }
            }
    }

    private fun checkOnboardingStatus() {
        val prefs = getSharedPreferences("onboarding", MODE_PRIVATE)
        hasCompletedOnboarding = prefs.getBoolean("completed", false)
        isLimitedMode = prefs.getBoolean("limited_mode", false)
    }

    private fun refreshPhoneSetup() {
        if (!hasCompletedOnboarding) return
        val limited = !readPhoneStatus(this).coreRequirementsGranted
        if (limited != isLimitedMode) {
            isLimitedMode = limited
            saveOnboardingCompleted()
            if (!limited && isDataLoaded) {
                contactsViewModel.loadContacts()
                scheduleContactSync()
            }
        }
    }

    private fun saveOnboardingCompleted() {
        val prefs = getSharedPreferences("onboarding", MODE_PRIVATE)
        prefs.edit { putBoolean("completed", true).putBoolean("limited_mode", isLimitedMode) }
    }

    private fun shouldShowCallWhileLocked(call: UiCall?): Boolean {
        return call != null && call.state != CallState.DISCONNECTED
    }

    private fun maybeLaunchInAppReview() {
        if (reviewPromptInFlight) return
        reviewPromptInFlight = true
        lifecycleScope.launch {
            try {
                delay(800L.milliseconds)
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@launch
                if (DefaultCallRepository.activeCall.value != null) return@launch
                if (!ReviewEligibilityStore.isEligible(this@MainActivity)) return@launch
                if (appContainer.reviewPromptLauncher.launch(this@MainActivity)) {
                    ReviewEligibilityStore.markPromptRequested(this@MainActivity)
                }
            } finally {
                reviewPromptInFlight = false
            }
        }
    }

    private fun handleIncomingCallKey(keyCode: Int): Boolean {
        val action = IncomingCallKeyPolicy.actionFor(
            keyCode = keyCode,
            answerWithVolumeUp = answerWithVolumeUpEnabled,
            declineWithVolumeDown = declineWithVolumeDownEnabled,
        ) ?: return false
        val call = DefaultCallRepository.activeCall.value
        val pendingIncomingId = incomingCallId
        val callId = when {
            call != null && call.state == CallState.RINGING -> call.id
            openIncoming && pendingIncomingId != null -> pendingIncomingId
            else -> return false
        }
        when (action) {
            IncomingCallKeyAction.ANSWER -> CallController.answer(callId)
            IncomingCallKeyAction.DECLINE -> CallController.reject(callId)
        }
        return true
    }

    private fun updateLockScreenBehavior(enable: Boolean) {
        if (lockScreenModeEnabled == enable) return
        lockScreenModeEnabled = enable
        setShowWhenLocked(enable)
        setTurnScreenOn(enable)
        if (enable) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun scheduleContactSync() {
        if (!PermissionUtils.hasContactsPermission(this)) {
            return
        }
        ContactSyncWorker.enqueueImmediate(this)
        ContactSyncWorker.ensurePeriodicSync(this)
    }
}

internal enum class CallRestoreTarget {
    INCOMING,
    IN_CALL,
    NONE,
}

internal fun resolveCallRestoreTarget(call: UiCall?): CallRestoreTarget = when (call?.state) {
    CallState.RINGING -> CallRestoreTarget.INCOMING
    CallState.DIALING, CallState.ACTIVE, CallState.ON_HOLD -> CallRestoreTarget.IN_CALL
    CallState.DISCONNECTED, null -> CallRestoreTarget.NONE
}

private const val STATE_LEAVE_AFTER_CALL = "leave_after_call"
private const val STATE_PENDING_SHORTCUTS = "pending_widget_and_shortcut_extras"

internal fun isCallOpenedFromOutside(openInCall: Boolean, appWasVisible: Boolean): Boolean =
    openInCall && !appWasVisible

internal fun shouldLeaveAfterCallEnds(
    leaveAfterCall: Boolean,
    hasOngoingCall: Boolean,
    activityStarted: Boolean,
): Boolean = leaveAfterCall && !hasOngoingCall && activityStarted

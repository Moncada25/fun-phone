package com.bookverse.contacts.ui.screens.settings

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.MainActivity
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.ui.auth.PasswordAuthDialogs
import com.bookverse.contacts.ui.auth.rememberPasswordAuthState
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.LoaderDialog
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.TopBarSearchField
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.navigation.HomeShortcutDestination
import com.bookverse.contacts.ui.theme.accentPalette
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.NotesViewModel
import com.bookverse.contacts.ui.viewmodel.calls.CallSettingsViewModel
import com.bookverse.contacts.ui.screens.contacts.ContactVCardResultHost
import com.bookverse.contacts.ui.screens.contacts.VCARD_MIME_TYPE
import com.bookverse.contacts.ui.screens.contacts.VCARD_OPEN_MIME_TYPES
import com.bookverse.contacts.utils.JsonUtils
import com.bookverse.contacts.utils.PermissionUtils
import com.bookverse.contacts.widget.FavoritesWidgetProvider
import com.bookverse.contacts.widget.NotesWidgetReceiver
import com.bookverse.contacts.widget.RecentCallsWidgetReceiver
import com.bookverse.contacts.widget.ExpensesWidgetReceiver
import com.composables.icons.lucide.Gamepad
import com.composables.icons.lucide.KeySquare
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.PiggyBank
import com.composables.icons.lucide.QrCode
import com.composables.icons.lucide.ScanQrCode
import com.composables.icons.lucide.SearchX
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    onFontChange: (() -> Unit)? = null,
    contactsViewModel: ContactsViewModel,
    notesViewModel: NotesViewModel,
    listState: LazyListState,
    onNavigateToPhoneStatus: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToTagManager: () -> Unit = {},
    onNavigateToDuplicateContacts: () -> Unit = {},
    onNavigateToContactRecycleBin: () -> Unit = {},
    onNavigateToToolsReorder: () -> Unit = {}
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = textColor.copy(alpha = 0.6f)
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = rememberAppSnackbarState()
    val fontType by appSettingsViewModel.fontType.collectAsStateWithLifecycle()
    val animationSpeed by appSettingsViewModel.animationSpeed.collectAsStateWithLifecycle()
    val transitionType by appSettingsViewModel.transitionType.collectAsStateWithLifecycle()
    val vibrationDuration by appSettingsViewModel.vibrationDuration.collectAsStateWithLifecycle()
    val vibrationPattern by appSettingsViewModel.vibrationPattern.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    val accentSeed = MaterialTheme.accentPalette.seed
    val themeMode by appSettingsViewModel.themeMode.collectAsStateWithLifecycle()
    val darkModeStyle by appSettingsViewModel.darkModeStyle.collectAsStateWithLifecycle()
    val accentBarStyle by appSettingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
    val appLanguage by appSettingsViewModel.appLanguage.collectAsStateWithLifecycle()
    val startupScreen by appSettingsViewModel.startupScreen.collectAsStateWithLifecycle()
    val callAnswerStyle by appSettingsViewModel.callAnswerStyle.collectAsStateWithLifecycle()
    val fontScale by appSettingsViewModel.fontScale.collectAsStateWithLifecycle()
    val listDensity by appSettingsViewModel.listDensity.collectAsStateWithLifecycle()
    val glassEffects by appSettingsViewModel.glassEffects.collectAsStateWithLifecycle()
    val dtmfTonesEnabled by appSettingsViewModel.dtmfTonesEnabled.collectAsStateWithLifecycle()
    val immersiveIncomingCallsEnabled by appSettingsViewModel.immersiveIncomingCalls.collectAsStateWithLifecycle()
    val answerWithVolumeUpEnabled by appSettingsViewModel.answerWithVolumeUpEnabled.collectAsStateWithLifecycle()
    val speedDialContacts by appSettingsViewModel.speedDialContacts.collectAsStateWithLifecycle()
    val shortcutContainerColor = MaterialTheme.colorScheme.background
    val shortcutIconColor = MaterialTheme.colorScheme.onBackground
    var activePreferenceSheet by remember { mutableStateOf<SettingsPreferenceSheet?>(null) }
    var showSpeedDialDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val contacts by contactsViewModel.contacts.collectAsStateWithLifecycle()
    var showUploading by remember { mutableStateOf(false) }
    val accounts by contactsViewModel.accounts.collectAsStateWithLifecycle()
    val isImporting by contactsViewModel.isImporting.collectAsStateWithLifecycle()
    val importResult by contactsViewModel.importResult.collectAsStateWithLifecycle()
    val blockedNumbers by contactsViewModel.blockedNumbers.collectAsStateWithLifecycle()
    val callSettingsViewModel: CallSettingsViewModel = viewModel(
        factory = CallSettingsViewModel.provideFactory(
            context.applicationContext as android.app.Application,
        ),
    )
    val callSettings by callSettingsViewModel.uiState.collectAsStateWithLifecycle()
    var showCallSimSheet by remember { mutableStateOf(false) }
    var showSpamFilterSheet by remember { mutableStateOf(false) }
    val requestCallScreeningRole = rememberCallScreeningRoleRequest(
        onResult = callSettingsViewModel::refreshDeviceState,
    )
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        callSettingsViewModel.refreshDeviceState()
    }
    var showJsonPhotoChoiceForSave by remember { mutableStateOf(false) }
    var showJsonPhotoChoiceForShare by remember { mutableStateOf(false) }
    var pendingSaveUri by remember { mutableStateOf<Uri?>(null) }
    val defaultAccount by appSettingsViewModel.defaultContactAccount.collectAsStateWithLifecycle()
    var showDefaultAccountDialog by remember { mutableStateOf(false) }
    var showBlockedNumbersDialog by remember { mutableStateOf(false) }
    var showShortcutSheet by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val shortcutOptions = listOf(
        ShortcutOption(
            destination = HomeShortcutDestination.NOTES,
            title = stringResource(R.string.notes_title),
            description = stringResource(R.string.notes_description),
            icon = Icons.AutoMirrored.Filled.StickyNote2
        ),
        ShortcutOption(
            destination = HomeShortcutDestination.PASSWORDS,
            title = stringResource(R.string.passwords_item),
            description = stringResource(R.string.password_manager_description),
            icon = Lucide.KeySquare
        ),
        ShortcutOption(
            destination = HomeShortcutDestination.GAMES,
            title = stringResource(R.string.games_title),
            description = stringResource(R.string.games_description),
            icon = Lucide.Gamepad
        ),
        ShortcutOption(
            destination = HomeShortcutDestination.EXPENSES,
            title = stringResource(R.string.expense_manager_item_title),
            description = stringResource(R.string.expense_manager_item_description),
            icon = Lucide.PiggyBank
        ),
        ShortcutOption(
            destination = HomeShortcutDestination.MENSTRUAL_CALENDAR,
            title = stringResource(R.string.menstrual_calendar_item),
            description = stringResource(R.string.menstrual_calendar_description),
            icon = Icons.Default.Favorite
        ),
        ShortcutOption(
            destination = HomeShortcutDestination.HOLIDAYS,
            title = stringResource(R.string.holidays_item),
            description = stringResource(R.string.holidays_description),
            icon = Icons.Default.CalendarMonth
        ),
        ShortcutOption(
            destination = HomeShortcutDestination.QR_SCANNER,
            title = stringResource(R.string.read_qr),
            description = stringResource(R.string.qr_scanner_description),
            icon = Lucide.ScanQrCode
        ),
        ShortcutOption(
            destination = HomeShortcutDestination.QR_GENERATOR,
            title = stringResource(R.string.generate_qr),
            description = stringResource(R.string.qr_generator_description),
            icon = Lucide.QrCode
        ),
        ShortcutOption(
            destination = HomeShortcutDestination.DIALER,
            title = stringResource(R.string.dialer_shortcut_title),
            description = stringResource(R.string.dialer_shortcut_description),
            icon = Icons.Rounded.Phone
        ),
        ShortcutOption(
            destination = HomeShortcutDestination.PAINT,
            title = stringResource(R.string.paint_title),
            description = stringResource(R.string.paint_description),
            icon = Icons.Default.Palette
        ),
        ShortcutOption(
            destination = HomeShortcutDestination.VOICE_TOOLS,
            title = stringResource(R.string.voice_utilities_item),
            description = stringResource(R.string.voice_recognition_description),
            icon = Lucide.Mic
        ),
        ShortcutOption(
            destination = HomeShortcutDestination.POMODORO,
            title = stringResource(R.string.pomodoro_title),
            description = stringResource(R.string.pomodoro_description),
            icon = Icons.Default.Timer
        )
    )
    val saveJsonLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                pendingSaveUri = uri; showJsonPhotoChoiceForSave = true
            }
        }
    val shareJsonLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            showUploading = false
        }
    val restoreJsonLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                contactsViewModel.importContacts(uri, contactsViewModel, defaultAccount)
            }
        }
    val vCardResult by contactsViewModel.vCard.result.collectAsStateWithLifecycle()
    val exportVCardLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(VCARD_MIME_TYPE)) { uri ->
            if (uri != null) contactsViewModel.vCard.exportFile(uri)
        }
    val importVCardLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) contactsViewModel.vCard.importFile(uri, defaultAccount)
        }

    val saveSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                try {
                    val json = appSettingsViewModel.exportSettingsToJson()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                    snackbar.show(
                        message = resources.getString(R.string.settings_exported_successfully),
                        type = SnackbarType.Success
                    )
                } catch (_: Exception) {
                    snackbar.show(
                        message = resources.getString(R.string.settings_export_error),
                        type = SnackbarType.Error
                    )
                }
            }
        }

    val restoreSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes().toString(Charsets.UTF_8)
                    }
                    if (json != null) {
                        val success = appSettingsViewModel.importSettingsFromJson(context, json)
                        if (success) {
                            snackbar.show(
                                message = resources.getString(R.string.settings_imported_successfully),
                                type = SnackbarType.Success
                            )
                        } else {
                            snackbar.show(
                                message = resources.getString(R.string.settings_import_error),
                                type = SnackbarType.Error
                            )
                        }
                    }
                } catch (_: Exception) {
                    snackbar.show(
                        message = resources.getString(R.string.settings_import_error),
                        type = SnackbarType.Error
                    )
                }
            }
        }

    fun requestShareJson() {
        showJsonPhotoChoiceForShare = true
    }

    fun handleShortcutRequest(
        option: ShortcutOption,
        painter: Painter,
        backgroundColor: Color,
        iconColor: Color
    ) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            snackbar.show(
                message = resources.getString(R.string.home_shortcuts_not_supported),
                type = SnackbarType.Warning
            )
            return
        }
        val shortcutIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(HomeShortcutDestination.EXTRA_KEY, option.destination.id)
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val shortcutBitmap = createShortcutBitmap(
            painter = painter,
            density = density,
            layoutDirection = layoutDirection,
            backgroundColor = backgroundColor,
            iconColor = iconColor
        )
        val shortcut = ShortcutInfoCompat.Builder(
            context,
            "home_shortcut_${option.destination.id}"
        )
            .setShortLabel(option.title)
            .setLongLabel(option.title)
            .setIcon(IconCompat.createWithBitmap(shortcutBitmap))
            .setIntent(shortcutIntent)
            .build()
        val success = ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        snackbar.show(
            message = resources.getString(
                if (success) R.string.home_shortcuts_created else R.string.home_shortcuts_request_failed,
                option.title
            ),
            type = if (success) SnackbarType.Success else SnackbarType.Error
        )
        if (success) {
            showShortcutSheet = false
        }
    }

    fun requestWidget(receiver: Class<*>) {
        val widgetManager = AppWidgetManager.getInstance(context)
        if (!widgetManager.isRequestPinAppWidgetSupported) {
            snackbar.show(
                message = resources.getString(R.string.widget_pin_not_supported),
                type = SnackbarType.Warning
            )
            return
        }
        val success = widgetManager.requestPinAppWidget(ComponentName(context, receiver), null, null)
        if (!success) {
            snackbar.show(
                message = resources.getString(R.string.widget_pin_not_supported),
                type = SnackbarType.Error
            )
        }
    }

    val passwordAuthState = rememberPasswordAuthState(appSettingsViewModel)
    var showProtectionBackup by remember { mutableStateOf(false) }
    ProtectionBackupHost(
        visible = showProtectionBackup,
        onDismiss = { showProtectionBackup = false },
        appSettingsViewModel = appSettingsViewModel,
        passwordAuthState = passwordAuthState,
        onMessage = { message, type -> snackbar.show(message = message, type = type) },
    )
    val passwordAuthMethod by appSettingsViewModel.passwordAuthMethod.collectAsStateWithLifecycle()
    val passwordManagerEnabled by appSettingsViewModel.passwordManagerEnabled.collectAsStateWithLifecycle()
    val sensitiveSectionsLockEnabled by appSettingsViewModel.sensitiveSectionsLockEnabled.collectAsStateWithLifecycle()
    val toolsOrder by appSettingsViewModel.toolsOrder.collectAsStateWithLifecycle()

    val hasContactsPermission = PermissionUtils.hasContactsPermission(context)

    var searchActive by remember { mutableStateOf(false) }
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior { !searchActive }
    var search by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(searchActive) { if (searchActive) focusRequester.requestFocus() }

    fun requestSensitiveLockDisable() {
        if (!sensitiveSectionsLockEnabled) return
        if (passwordAuthMethod == null) {
            appSettingsViewModel.setSensitiveSectionsLock(context, false)
            return
        }
        passwordAuthState.handleProtectedAction(
            sensitiveSectionsLockEnabled,
            passwordAuthMethod
        ) {
            appSettingsViewModel.setSensitiveSectionsLock(context, false)
        }
    }

    val themeModeValue = stringResource(themeModeLabelRes(themeMode))
    val themeValue = if (themeMode == com.bookverse.contacts.ui.theme.ThemeMode.LIGHT) {
        themeModeValue
    } else {
        "$themeModeValue · ${stringResource(darkModeStyleLabelRes(darkModeStyle))}"
    }
    val selectedColorKey = appSettingsViewModel.availableColors.entries
        .firstOrNull { it.value == accentSeed }
        ?.key
        ?: "blue"
    val defaultAccountValue = if (defaultAccount.type == ContactAccount.LOCAL_ACCOUNT.type) {
        stringResource(R.string.device)
    } else {
        defaultAccount.displayName
    }
    val blockedNumbersValue = pluralStringResource(
        R.plurals.settings_blocked_count,
        blockedNumbers.size,
        blockedNumbers.size,
    )
    val callSimValue = callSimPreferenceLabel(
        accounts = callSettings.simAccounts,
        preference = callSettings.effectiveSimPreference,
    )
    val enabledSpamRules = callSettings.screeningSettings.enabledRuleCount
    val spamFilterValue = if (enabledSpamRules == 0) {
        stringResource(R.string.spam_filter_value_off)
    } else {
        pluralStringResource(R.plurals.spam_filter_rules_active, enabledSpamRules, enabledSpamRules)
    }
    val speedDialValue = pluralStringResource(
        R.plurals.settings_assigned_count,
        speedDialContacts.size,
        speedDialContacts.size,
    )

    val settingsSections = buildSettingsSections(
        state = SettingsSectionsState(
            hasContactsPermission = hasContactsPermission,
            themeValue = themeValue,
            accentStyleValue = stringResource(accentStyleLabelRes(accentBarStyle)),
            accentColorValue = settingsColorName(selectedColorKey),
            fontValue = settingsFontDisplayName(fontType),
            fontScaleValue = stringResource(R.string.font_scale_value, fontScalePercent(fontScale)),
            listDensityValue = listDensityLabel(listDensity),
            glassEffectsValue = stringResource(glassEffectsLabelRes(glassEffects)),
            animationSpeedValue = stringResource(animationSpeedLabelRes(animationSpeed)),
            transitionValue = stringResource(transitionTypeLabelRes(transitionType)),
            vibrationDurationValue = stringResource(
                R.string.vibration_duration_value,
                vibrationDuration,
            ),
            vibrationPatternValue = stringResource(vibrationPatternLabelRes(vibrationPattern)),
            defaultAccountValue = defaultAccountValue,
            blockedNumbersValue = blockedNumbersValue,
            callSimVisible = callSettings.hasMultipleSims,
            callSimValue = callSimValue,
            spamFilterValue = spamFilterValue,
            callAnswerStyleValue = stringResource(callAnswerStyleLabelRes(callAnswerStyle)),
            speedDialValue = speedDialValue,
            startupScreenValue = stringResource(startupScreenLabelRes(startupScreen)),
            languageValue = stringResource(appLanguage.labelRes),
            toolsMenuVisible = !toolsOrder.toolsMenuHidden,
            immersiveIncomingCallsEnabled = immersiveIncomingCallsEnabled,
            answerWithVolumeUpEnabled = answerWithVolumeUpEnabled,
            dtmfTonesEnabled = dtmfTonesEnabled == "enabled",
            passwordAuthEnabled = passwordAuthMethod != null,
            passwordManagerEnabled = passwordManagerEnabled,
            sensitiveSectionsLockEnabled = sensitiveSectionsLockEnabled,
        ),
        actions = SettingsSectionsActions(
            onTheme = { activePreferenceSheet = SettingsPreferenceSheet.THEME },
            onAccentStyle = { activePreferenceSheet = SettingsPreferenceSheet.ACCENT_STYLE },
            onAccentColor = { activePreferenceSheet = SettingsPreferenceSheet.COLOR },
            onFont = { activePreferenceSheet = SettingsPreferenceSheet.FONT },
            onFontScale = { activePreferenceSheet = SettingsPreferenceSheet.FONT_SCALE },
            onListDensity = { activePreferenceSheet = SettingsPreferenceSheet.LIST_DENSITY },
            onGlassEffects = { activePreferenceSheet = SettingsPreferenceSheet.GLASS_EFFECTS },
            onAnimationSpeed = {
                activePreferenceSheet = SettingsPreferenceSheet.ANIMATION_SPEED
            },
            onTransition = { activePreferenceSheet = SettingsPreferenceSheet.TRANSITION },
            onVibrationDuration = {
                activePreferenceSheet = SettingsPreferenceSheet.VIBRATION_DURATION
            },
            onVibrationPattern = {
                activePreferenceSheet = SettingsPreferenceSheet.VIBRATION_PATTERN
            },
            onToolsMenuVisibleChanged = { visible ->
                appSettingsViewModel.setToolsMenuHidden(context, !visible)
            },
            onCustomizeTools = onNavigateToToolsReorder,
            onProfile = onNavigateToProfile,
            onDefaultAccount = { showDefaultAccountDialog = true },
            onTagManager = onNavigateToTagManager,
            onDuplicateContacts = onNavigateToDuplicateContacts,
            onBlockedNumbers = { showBlockedNumbersDialog = true },
            onCallAnswerStyle = {
                activePreferenceSheet = SettingsPreferenceSheet.CALL_ANSWER_STYLE
            },
            onCallSim = { showCallSimSheet = true },
            onSpamFilter = { showSpamFilterSheet = true },
            onSpeedDial = { showSpeedDialDialog = true },
            onPhoneStatus = onNavigateToPhoneStatus,
            onImmersiveIncomingCallsChanged = { enabled ->
                appSettingsViewModel.setImmersiveIncomingCalls(context, enabled)
            },
            onAnswerWithVolumeChanged = { enabled ->
                appSettingsViewModel.setAnswerWithVolumeUpEnabled(context, enabled)
            },
            onDtmfTonesChanged = { enabled ->
                appSettingsViewModel.setDtmfTonesEnabled(
                    context,
                    if (enabled) "enabled" else "disabled",
                )
            },
            onHomeShortcuts = { showShortcutSheet = true },
            onFavoritesWidget = { requestWidget(FavoritesWidgetProvider::class.java) },
            onNotesWidget = { requestWidget(NotesWidgetReceiver::class.java) },
            onRecentCallsWidget = { requestWidget(RecentCallsWidgetReceiver::class.java) },
            onExpensesWidget = { requestWidget(ExpensesWidgetReceiver::class.java) },
            onPasswordAuthChanged = { enabled ->
                if (enabled) {
                    passwordAuthState.startSetupFlow()
                } else {
                    passwordAuthState.requestRemoval(passwordAuthMethod) {
                        notesViewModel.unlockAllLockedNotes()
                        snackbar.show(
                            message = resources.getString(R.string.biometrics_disabled_snackbar),
                            type = SnackbarType.Warning,
                        )
                    }
                }
            },
            onPasswordManagerChanged = { enabled ->
                appSettingsViewModel.setPasswordManagerEnabled(context, enabled)
            },
            onSensitiveSectionsLockChanged = { enabled ->
                if (enabled) {
                    if (passwordAuthMethod == null) {
                        passwordAuthState.startSetupFlow {
                            appSettingsViewModel.setSensitiveSectionsLock(context, true)
                        }
                    } else {
                        appSettingsViewModel.setSensitiveSectionsLock(context, true)
                    }
                } else {
                    requestSensitiveLockDisable()
                }
            },
            onSaveContacts = {
                val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
                saveJsonLauncher.launch("Contacts_$date.json")
            },
            onShareContacts = ::requestShareJson,
            onRestoreContacts = {
                restoreJsonLauncher.launch(arrayOf("application/json"))
            },
            onExportVCard = {
                if (contacts.isEmpty()) {
                    snackbar.show(resources.getString(R.string.contacts_empty), SnackbarType.Info)
                } else {
                    val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
                    exportVCardLauncher.launch("Contacts_$date.vcf")
                }
            },
            onImportVCard = { importVCardLauncher.launch(VCARD_OPEN_MIME_TYPES) },
            onContactRecycleBin = onNavigateToContactRecycleBin,
            onSaveSettings = {
                val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
                saveSettingsLauncher.launch("FunPhone_Settings_$date.json")
            },
            onRestoreSettings = {
                restoreSettingsLauncher.launch(arrayOf("application/json"))
            },
            onStartupScreen = {
                activePreferenceSheet = SettingsPreferenceSheet.STARTUP_SCREEN
            },
            onLanguage = { activePreferenceSheet = SettingsPreferenceSheet.LANGUAGE },
            onAbout = onNavigateToAbout,
            onProtectionBackup = { showProtectionBackup = true },
        ),
    )
    val filteredSettings = filterSettingsItems(settingsSections, search)

    BackHandler {
        if (showShortcutSheet) {
            showShortcutSheet = false
        } else {
            onBackClick()
        }
    }

    BackHandler(enabled = searchActive) {
        searchActive = false
        search = ""
        focusManager.clearFocus(force = true)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.settings),
                expandable = true,
                onBackClick = onBackClick,
                searchAction = com.bookverse.contacts.ui.components.AppBarSearchAction(
                    stringResource(R.string.search_setting), { searchActive = true },
                ),
                appSettingsViewModel = appSettingsViewModel,
                scrollBehavior = scrollBehavior,
                titleContent = if (searchActive) {
                    {
                        TopBarSearchField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = stringResource(R.string.search_setting),
                            focusRequester = focusRequester,
                            onClose = {
                                search = ""
                                focusManager.clearFocus(force = true)
                                searchActive = false
                            },
                            leadingContentDescription = stringResource(R.string.search_setting),
                            clearContentDescription = stringResource(R.string.clear),
                        )
                    }
                } else null
            )
        },
        snackbarHost = { AppSnackbarHost(snackbar.hostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(top = paddingValues.calculateTopPadding()),
                state = listState
            ) {
                if (searchActive) {
                    if (search.isNotBlank() && filteredSettings.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    imageVector = Lucide.SearchX,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = textColor.copy(alpha = 0.4f),
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.no_results),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor.copy(alpha = 0.6f),
                                )
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = stringResource(
                                    if (search.isBlank()) {
                                        R.string.all_settings
                                    } else {
                                        R.string.settings_results
                                    },
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                        items(
                            items = filteredSettings,
                            key = SettingsItemModel::key,
                        ) { setting ->
                            val searchItem = setting.copy(
                                onClick = {
                                    setting.onClick()
                                    searchActive = false
                                    search = ""
                                },
                                toggle = setting.toggle?.let { toggle ->
                                    toggle.copy(
                                        onCheckedChange = { checked ->
                                            toggle.onCheckedChange(checked)
                                            searchActive = false
                                            search = ""
                                        },
                                    )
                                },
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                            ) {
                                if (searchItem.toggle == null) {
                                    SettingsItemRow(
                                        item = searchItem,
                                    )
                                } else {
                                    SettingsToggleItem(
                                        item = searchItem,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = settingsSections,
                        key = SettingsSectionModel::key,
                    ) { section ->
                        SettingsSectionCard(
                            section = section,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
    if (isImporting) {
        LoaderDialog(
            message = stringResource(R.string.importing_contacts),
        )
    }
    if (importResult != null && !isImporting) {
        MessageDialog(
            title = stringResource(R.string.import_success),
            message1 = pluralStringResource(
                R.plurals.imported_count,
                importResult ?: 0,
                importResult ?: 0,
            ),
            onDismiss = { contactsViewModel.clearImportResult() },
            onButtonClick = { contactsViewModel.clearImportResult() },
        )
    }
    ContactVCardResultHost(
        result = vCardResult,
        isImporting = isImporting,
        onConsumed = contactsViewModel.vCard::clearResult,
        onMessage = { message, type -> snackbar.show(message, type) },
    )
    if (showUploading) {
        LoaderDialog(
            message = stringResource(R.string.uploading_photos),
        )
    }
    SettingsPreferenceSheets(
        activeSheet = activePreferenceSheet,
        state = SettingsPreferenceSheetState(
            fontType = fontType,
            animationSpeed = animationSpeed,
            transitionType = transitionType,
            vibrationDuration = vibrationDuration,
            vibrationPattern = vibrationPattern,
            accentSeed = accentSeed,
            themeMode = themeMode,
            darkModeStyle = darkModeStyle,
            accentBarStyle = accentBarStyle,
            appLanguage = appLanguage,
            startupScreen = startupScreen,
            callAnswerStyle = callAnswerStyle,
            fontScale = fontScale,
            listDensity = listDensity,
            glassEffects = glassEffects,
            animationOptions = appSettingsViewModel.animationOptions,
            languageOptions = appSettingsViewModel.appLanguageOptions,
            availableColors = appSettingsViewModel.availableColors,
            vibrationDurations = appSettingsViewModel.vibrationDurations,
            vibrationPatterns = appSettingsViewModel.vibrationPatterns,
            transitionOptions = appSettingsViewModel.transitionOptions,
            themeModes = appSettingsViewModel.themeModes,
            darkModeStyles = appSettingsViewModel.darkModeStyles,
        ),
        onDismiss = { activePreferenceSheet = null },
        onAnimationSpeedSelected = { speed -> appSettingsViewModel.setAnimationSpeed(context, speed) },
        onLanguageSelected = { language -> appSettingsViewModel.setAppLanguage(context, language) },
        onFontSelected = { chosen ->
            if (chosen != fontType) {
                appSettingsViewModel.setFontType(chosen)
                appSettingsViewModel.saveSettings(context)
                onFontChange?.invoke()
            }
        },
        onAccentStyleSelected = { style -> appSettingsViewModel.setAccentBarStyle(context, style) },
        onColorSelected = { key ->
            val selectedColor = appSettingsViewModel.availableColors[key]
                ?: appSettingsViewModel.availableColors.getValue("blue")
            appSettingsViewModel.setAccentColor(selectedColor)
            appSettingsViewModel.saveSettings(context)
        },
        onVibrationDurationSelected = { duration ->
            appSettingsViewModel.setVibrationDuration(context, duration)
        },
        onVibrationPatternSelected = { pattern ->
            appSettingsViewModel.setVibrationPattern(context, pattern)
        },
        onFontScaleSelected = { scale -> appSettingsViewModel.setFontScale(context, scale) },
        onListDensitySelected = { selectedDensity ->
            appSettingsViewModel.setListDensity(context, selectedDensity)
        },
        onGlassEffectsSelected = { effects -> appSettingsViewModel.setGlassEffects(context, effects) },
        onStartupScreenSelected = { screen -> appSettingsViewModel.setStartupScreen(context, screen) },
        onCallAnswerStyleSelected = { style ->
            appSettingsViewModel.setCallAnswerStyle(context, style)
        },
        onThemeModeSelected = { mode ->
            appSettingsViewModel.setThemeMode(context, mode)
        },
        onDarkModeStyleSelected = { style ->
            appSettingsViewModel.setDarkModeStyle(context, style)
        },
        onTransitionSelected = { transition ->
            appSettingsViewModel.setTransitionType(context, transition)
        },
    )
    if (showJsonPhotoChoiceForSave) {
        SettingsSelectionSheet(
            title = stringResource(R.string.json_include_photos_title),
            options = listOf(
                true,
                false,
            ),
            selectedOption = null,
            onSelected = { includePhotos ->
                val uri = pendingSaveUri
                if (uri != null) {
                    showUploading = true
                    coroutineScope.launch {
                        val profileId = appSettingsViewModel.getOrCreateProfileId(context)
                        JsonUtils.saveContactsAsJson(
                            context,
                            contacts,
                            uri,
                            profileId,
                            includePhotos,
                        )
                        showUploading = false
                        pendingSaveUri = null
                    }
                }
            },
            onDismiss = { showJsonPhotoChoiceForSave = false; pendingSaveUri = null },
            optionKey = { includePhotos -> includePhotos },
            optionLabel = { includePhotos ->
                stringResource(
                    if (includePhotos) R.string.json_with_photos else R.string.json_without_photos,
                )
            },
        )
    }
    if (showBlockedNumbersDialog) {
        BlockedNumbersSheet(
            blockedNumbers = blockedNumbers,
            accentColor = accentColor,
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            onDismiss = { showBlockedNumbersDialog = false },
            onUnblockContact = contactsViewModel::unblockContact,
            onUnblockNumber = contactsViewModel::unblockNumber,
            onBlockNumber = contactsViewModel::blockNumber,
        )
    }
    if (showCallSimSheet) {
        CallSimPreferenceSheet(
            accounts = callSettings.simAccounts,
            selected = callSettings.effectiveSimPreference,
            onSelected = callSettingsViewModel::setSimPreference,
            onDismiss = { showCallSimSheet = false },
        )
    }
    if (showSpamFilterSheet) {
        CallScreeningSheet(
            settings = callSettings.screeningSettings,
            roleActive = callSettings.screeningRoleActive,
            roleAvailable = callSettings.screeningRoleAvailable,
            onBlockListedChanged = callSettingsViewModel::setBlockListedNumbers,
            onBlockHiddenChanged = callSettingsViewModel::setBlockHiddenNumbers,
            onBlockNotInContactsChanged = callSettingsViewModel::setBlockNumbersNotInContacts,
            onRequestRole = requestCallScreeningRole,
            onDismiss = { showSpamFilterSheet = false },
        )
    }
    if (showShortcutSheet) {
        val sheetScrollState = rememberScrollState()
        SettingsSheetScaffold(
            title = stringResource(R.string.home_shortcuts_sheet_title),
            subtitle = stringResource(R.string.home_shortcuts_sheet_subtitle),
            onDismiss = { showShortcutSheet = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(sheetScrollState)
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                shortcutOptions.forEach { option ->
                    ShortcutOptionRow(
                        option = option,
                        onAddShortcut = { painter ->
                            handleShortcutRequest(
                                option = option,
                                painter = painter,
                                backgroundColor = shortcutContainerColor,
                                iconColor = shortcutIconColor
                            )
                        },
                    )
                }
            }
        }
    }

    if (showJsonPhotoChoiceForShare) {
        SettingsSelectionSheet(
            title = stringResource(R.string.json_include_photos_title),
            options = listOf(
                true,
                false,
            ),
            selectedOption = null,
            onSelected = { includePhotos ->
                if (includePhotos) showUploading = true
                coroutineScope.launch {
                    val profileId = appSettingsViewModel.getOrCreateProfileId(context)
                    JsonUtils.shareContactsAsJson(
                        context,
                        contacts,
                        shareJsonLauncher,
                        profileId,
                        includePhotos,
                    )
                }
            },
            onDismiss = { showJsonPhotoChoiceForShare = false },
            optionKey = { includePhotos -> includePhotos },
            optionLabel = { includePhotos ->
                stringResource(
                    if (includePhotos) R.string.json_with_photos else R.string.json_without_photos,
                )
            },
        )
    }
    PasswordAuthDialogs(
        state = passwordAuthState,
    )
    if (showDefaultAccountDialog) {
        val preselected = accounts.firstOrNull {
            it.name == defaultAccount.name && it.type == defaultAccount.type
        } ?: ContactAccount.LOCAL_ACCOUNT

        SettingsSelectionSheet(
            title = stringResource(R.string.default_account_title),
            options = accounts,
            selectedOption = preselected,
            onDismiss = { showDefaultAccountDialog = false },
            onSelected = { acc ->
                appSettingsViewModel.setDefaultContactAccount(context, acc)
            },
            optionKey = { account -> "${account.name}:${account.type}" },
            optionLabel = { account ->
                if (account.type == ContactAccount.LOCAL_ACCOUNT.type) {
                    stringResource(R.string.device)
                } else {
                    account.displayName
                }
            },
        )
    }

    if (showSpeedDialDialog) {
        SpeedDialConfigSheet(
            onDismiss = { showSpeedDialDialog = false },
            contacts = contacts,
            speedDialContacts = speedDialContacts,
            accentColor = accentColor,
            onAssignmentChanged = { number, contactId ->
                appSettingsViewModel.setSpeedDialContact(context, number, contactId)
            },
            loadContactPhoto = contactsViewModel::uriToBitmap,
        )
    }
}

@Composable
private fun ShortcutOptionRow(
    option: ShortcutOption,
    onAddShortcut: (Painter) -> Unit,
) {
    val painter = rememberVectorPainter(option.icon)
    val iconContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val iconContentColor = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = iconContainerColor,
            contentColor = iconContentColor,
            shape = RoundedCornerShape(14.dp),
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.title,
                    tint = iconContentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = option.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = option.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedButton(onClick = { onAddShortcut(painter) }) {
            Text(
                text = stringResource(R.string.home_shortcuts_add_button),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private data class ShortcutOption(
    val destination: HomeShortcutDestination,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private fun createShortcutBitmap(
    painter: Painter,
    density: Density,
    layoutDirection: LayoutDirection,
    backgroundColor: Color,
    iconColor: Color
): Bitmap {
    val sizePx = with(density) { 108.dp.toPx().roundToInt() }
    val imageBitmap = ImageBitmap(sizePx, sizePx)
    val canvas = Canvas(imageBitmap)
    val drawScope = CanvasDrawScope()
    val totalSize = Size(sizePx.toFloat(), sizePx.toFloat())
    val iconSize = sizePx * 0.9f
    val offset = (sizePx - iconSize) / 2f
    drawScope.draw(
        density = density,
        layoutDirection = layoutDirection,
        canvas = canvas,
        size = totalSize
    ) {
        drawRoundRect(
            color = backgroundColor,
            size = totalSize,
            cornerRadius = CornerRadius(sizePx * 0.22f, sizePx * 0.22f)
        )
        withTransform({
            translate(left = offset, top = offset)
        }) {
            with(painter) {
                draw(
                    size = Size(iconSize, iconSize),
                    alpha = 1f,
                    colorFilter = ColorFilter.tint(iconColor)
                )
            }
        }
    }
    return imageBitmap.asAndroidBitmap()
}

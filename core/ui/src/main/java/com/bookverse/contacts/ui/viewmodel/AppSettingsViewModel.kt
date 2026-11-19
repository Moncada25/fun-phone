package com.bookverse.contacts.ui.viewmodel

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.domain.contacts.ContactSortOrder
import com.bookverse.contacts.domain.expenses.ExpenseCurrency
import com.bookverse.contacts.domain.expenses.ExpenseCurrencyProvider
import com.bookverse.contacts.settings.GlassEffects
import com.bookverse.contacts.settings.AnimationSpeed
import com.bookverse.contacts.settings.AppDataStore
import com.bookverse.contacts.settings.AppSettingsStore
import com.bookverse.contacts.settings.DataStoreAppSettingsStore
import com.bookverse.contacts.settings.ListDensity
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.settings.TransitionType
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.theme.DarkModeStyle
import com.bookverse.contacts.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.bookverse.contacts.security.PinAuthenticator
import com.bookverse.contacts.security.PinResult

enum class PasswordAuthMethod {
    BIOMETRIC, PIN;

    companion object {
        fun fromName(n: String?): PasswordAuthMethod? {
            return when (n) {
                "BIOMETRIC" -> BIOMETRIC
                "PIN" -> PIN
                else -> null
            }
        }
    }
}

enum class FavoritesSortOrder {
    ALPHABETICAL,
    CUSTOM;

    companion object {
        fun fromName(name: String?): FavoritesSortOrder {
            return when (name) {
                "CUSTOM" -> CUSTOM
                else -> ALPHABETICAL
            }
        }
    }
}

enum class FavoritesCardShape {
    SQUARE,
    ROUND;

    companion object {
        fun fromName(name: String?): FavoritesCardShape {
            return when (name) {
                ROUND.name -> ROUND
                else -> SQUARE
            }
        }
    }
}

enum class AppLanguage(
    @param:StringRes val labelRes: Int,
) {
    SYSTEM(R.string.app_language_system),
    SPANISH(R.string.app_language_spanish),
    ENGLISH(R.string.app_language_english),
    PORTUGUESE(R.string.app_language_portuguese),
    FRENCH(R.string.app_language_french),
    GERMAN(R.string.app_language_german);

    companion object {
        fun fromName(name: String?): AppLanguage {
            return LocaleController.languageForStoredValue(name)
        }
    }

    fun toLocaleList(): LocaleListCompat {
        return LocaleController.localeListFor(this)
    }
}

class AppSettingsViewModel(
    private val settingsStore: AppSettingsStore = DataStoreAppSettingsStore,
) : ViewModel() {
    companion object {
        const val FAVORITES_SORT_KEY_NAME = "favorites_sort"
        const val FAVORITES_CUSTOM_ORDER_KEY_NAME = "favorites_custom_order"
        const val IMMERSIVE_INCOMING_CALLS_KEY_NAME = "immersive_incoming_calls"

        private val immersiveIncomingCallsKey = booleanPreferencesKey(IMMERSIVE_INCOMING_CALLS_KEY_NAME)

        suspend fun isImmersiveIncomingCallsEnabled(context: Context): Boolean {
            return runCatching {
                val prefs = AppDataStore.getDataStore(context).data.first()
                prefs[immersiveIncomingCallsKey] ?: false
            }.getOrDefault(false)
        }
    }

    private val animationSpeedKey = stringPreferencesKey("animation_speed")
    private val transitionTypeKey = stringPreferencesKey("transition_type")
    private val themeKey = stringPreferencesKey("theme_mode")
    private val darkModeStyleKey = stringPreferencesKey("dark_mode_style")
    private val profileIdKey = stringPreferencesKey("profile_id")
    private val accentColorKey = stringPreferencesKey("accent_color")
    private val accentColorNameKey = stringPreferencesKey("accent_color_name")
    private val fontTypeKey = stringPreferencesKey("font_type")
    private val defaultDialerConsentKey = booleanPreferencesKey("default_dialer_consent")
    private val passwordManagerEnabledKey = booleanPreferencesKey("password_manager_enabled")
    private val passwordBiometricKey = booleanPreferencesKey("password_biometric")
    private val passwordAuthMethodKey = stringPreferencesKey("password_auth_method")
    private val sensitiveSectionsLockKey = booleanPreferencesKey("sensitive_sections_lock")
    private val defaultAccountNameKey = stringPreferencesKey("default_account_name")
    private val defaultAccountTypeKey = stringPreferencesKey("default_account_type")
    private val vibrationDurationKey = intPreferencesKey("vibration_duration")
    private val vibrationPatternKey = stringPreferencesKey("vibration_pattern")
    private val favoritesColumnsKey = intPreferencesKey("favorites_columns")
    private val favoritesCardShapeKey = stringPreferencesKey("favorites_card_shape")
    private val favoritesSortKey = stringPreferencesKey(FAVORITES_SORT_KEY_NAME)
    private val favoritesCustomOrderKey = stringPreferencesKey(FAVORITES_CUSTOM_ORDER_KEY_NAME)
    private val accentBarStyleKey = stringPreferencesKey("accent_bar_style")
    private val expenseCurrencyKey = stringPreferencesKey("expense_currency")
    private val toolsOrderKey = stringPreferencesKey("tools_order_v2")
    private val noteFoldersOrderKey = stringPreferencesKey("note_folders_order")
    private val passwordFoldersOrderKey = stringPreferencesKey("password_folders_order")
    private val startupScreenKey = stringPreferencesKey("startup_screen")
    private val callAnswerStyleKey = stringPreferencesKey("call_answer_style")
    private val dtmfTonesEnabledKey = stringPreferencesKey("dtmf_tones_enabled")
    private val answerWithVolumeUpKey = booleanPreferencesKey("answer_with_volume_up")
    private val declineWithVolumeDownKey = booleanPreferencesKey("decline_with_volume_down")
    private val showProfileInFavoritesKey = booleanPreferencesKey("show_profile_in_favorites")
    private val speedDialKey = stringPreferencesKey("speed_dial_contacts")
    private val fontScaleKey = stringPreferencesKey("font_scale")
    private val listDensityKey = stringPreferencesKey("list_density")
    private val glassEffectsKey = stringPreferencesKey("glass_effects")
    private val birthdayReminderPromptShownKey =
        booleanPreferencesKey("birthday_reminder_prompt_shown")

    private val _accentColor = MutableStateFlow(Color(0xFF2196F3))
    val accentColor: StateFlow<Color> = _accentColor.asStateFlow()

    private val _accentColorName = MutableStateFlow("blue")

    private val _fontType = MutableStateFlow("default")
    val fontType: StateFlow<String> = _fontType.asStateFlow()

    private val _animationSpeed = MutableStateFlow(AnimationSpeed.NORMAL)
    val animationSpeed: StateFlow<AnimationSpeed> = _animationSpeed.asStateFlow()

    private val _vibrationDuration = MutableStateFlow(50)
    val vibrationDuration: StateFlow<Int> = _vibrationDuration.asStateFlow()

    private val _vibrationPattern = MutableStateFlow("simple")
    val vibrationPattern: StateFlow<String> = _vibrationPattern.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _darkModeStyle = MutableStateFlow(DarkModeStyle.AMOLED)
    val darkModeStyle: StateFlow<DarkModeStyle> = _darkModeStyle.asStateFlow()

    private val _profileId = MutableStateFlow<String?>(null)

    private val _transitionType = MutableStateFlow(TransitionType.SLIDE)
    val transitionType: StateFlow<TransitionType> = _transitionType.asStateFlow()

    private val _defaultDialerConsent = MutableStateFlow(false)
    val defaultDialerConsent: StateFlow<Boolean> = _defaultDialerConsent.asStateFlow()

    private val _passwordManagerEnabled = MutableStateFlow(false)
    val passwordManagerEnabled: StateFlow<Boolean> = _passwordManagerEnabled.asStateFlow()

    private val _passwordBiometric = MutableStateFlow(true)

    private val _passwordAuthMethod = MutableStateFlow<PasswordAuthMethod?>(null)
    val passwordAuthMethod: StateFlow<PasswordAuthMethod?> = _passwordAuthMethod.asStateFlow()

    private var pinAuthenticator: PinAuthenticator? = null
    private fun pinAuthenticator(context: Context): PinAuthenticator =
        pinAuthenticator ?: PinAuthenticator(settingsStore.dataStore(context.applicationContext)).also { pinAuthenticator = it }

    private val _protectedSessionUnlocked = MutableStateFlow(false)
    val protectedSessionUnlocked = _protectedSessionUnlocked.asStateFlow()
    fun unlockProtectedSession() { _protectedSessionUnlocked.value = true }
    fun lockProtectedSession() { _protectedSessionUnlocked.value = false }

    private val _sensitiveSectionsLockEnabled = MutableStateFlow(false)
    val sensitiveSectionsLockEnabled: StateFlow<Boolean> = _sensitiveSectionsLockEnabled.asStateFlow()

    private val _defaultContactAccount = MutableStateFlow(ContactAccount.LOCAL_ACCOUNT)
    val defaultContactAccount: StateFlow<ContactAccount> = _defaultContactAccount.asStateFlow()

    private val _favoritesColumns = MutableStateFlow(2)
    val favoritesColumns: StateFlow<Int> = _favoritesColumns.asStateFlow()

    private val _favoritesCardShape = MutableStateFlow(FavoritesCardShape.SQUARE)
    val favoritesCardShape: StateFlow<FavoritesCardShape> = _favoritesCardShape.asStateFlow()

    private val _favoritesSortOrder = MutableStateFlow(FavoritesSortOrder.ALPHABETICAL)
    val favoritesSortOrder: StateFlow<FavoritesSortOrder> = _favoritesSortOrder.asStateFlow()

    private val _favoritesCustomOrder = MutableStateFlow<List<String>>(emptyList())
    val favoritesCustomOrder: StateFlow<List<String>> = _favoritesCustomOrder.asStateFlow()

    private val _accentBarStyle = MutableStateFlow(AccentBarStyle.MODERN)
    val accentBarStyle: StateFlow<AccentBarStyle> = _accentBarStyle.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.SYSTEM)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _expenseCurrency =
        MutableStateFlow(ExpenseCurrencyProvider.fromCode(null))
    val expenseCurrency: StateFlow<ExpenseCurrency> = _expenseCurrency.asStateFlow()

    private val _toolsOrder = MutableStateFlow(com.bookverse.contacts.data.model.ToolsOrderConfig.default())
    val toolsOrder: StateFlow<com.bookverse.contacts.data.model.ToolsOrderConfig> = _toolsOrder.asStateFlow()

    private val _noteFoldersOrder = MutableStateFlow<List<Long>>(emptyList())
    val noteFoldersOrder: StateFlow<List<Long>> = _noteFoldersOrder.asStateFlow()

    private val _passwordFoldersOrder = MutableStateFlow<List<Long>>(emptyList())
    val passwordFoldersOrder: StateFlow<List<Long>> = _passwordFoldersOrder.asStateFlow()

    private val _startupScreen = MutableStateFlow("contacts")
    val startupScreen: StateFlow<String> = _startupScreen.asStateFlow()

    private val _callAnswerStyle = MutableStateFlow("swipe")
    val callAnswerStyle: StateFlow<String> = _callAnswerStyle.asStateFlow()

    private val _dtmfTonesEnabled = MutableStateFlow("disabled")
    val dtmfTonesEnabled: StateFlow<String> = _dtmfTonesEnabled.asStateFlow()

    private val _immersiveIncomingCalls = MutableStateFlow(false)
    val immersiveIncomingCalls: StateFlow<Boolean> = _immersiveIncomingCalls.asStateFlow()

    private val _answerWithVolumeUpEnabled = MutableStateFlow(false)
    val answerWithVolumeUpEnabled: StateFlow<Boolean> = _answerWithVolumeUpEnabled.asStateFlow()

    private val _declineWithVolumeDownEnabled = MutableStateFlow(false)
    val declineWithVolumeDownEnabled: StateFlow<Boolean> = _declineWithVolumeDownEnabled.asStateFlow()

    private val _showProfileInFavorites = MutableStateFlow(false)
    val showProfileInFavorites: StateFlow<Boolean> = _showProfileInFavorites.asStateFlow()

    private val _speedDialContacts = MutableStateFlow<Map<Int, String>>(emptyMap())
    val speedDialContacts: StateFlow<Map<Int, String>> = _speedDialContacts.asStateFlow()

    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _listDensity = MutableStateFlow(ListDensity.NORMAL)
    val listDensity: StateFlow<ListDensity> = _listDensity.asStateFlow()

    private val _glassEffects = MutableStateFlow(GlassEffects.AUTO)
    val glassEffects: StateFlow<GlassEffects> = _glassEffects.asStateFlow()

    private val _birthdayReminderPromptShown = MutableStateFlow(false)
    val birthdayReminderPromptShown: StateFlow<Boolean> =
        _birthdayReminderPromptShown.asStateFlow()

    private val localeController = LocaleController(settingsStore)

    val animationOptions = listOf(
        AnimationSpeed.FAST,
        AnimationSpeed.NORMAL,
        AnimationSpeed.SLOW,
        AnimationSpeed.OFF
    )

    val vibrationDurations = listOf(20, 30, 50, 80, 120)
    val vibrationPatterns = listOf("off", "simple", "double", "triple")
    val themeModes = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)
    val darkModeStyles = listOf(DarkModeStyle.AMOLED, DarkModeStyle.DARK_GRAY, DarkModeStyle.DIM)
    val appLanguageOptions = AppLanguage.entries.toList()
    val transitionOptions = listOf(
        TransitionType.SCALE,
        TransitionType.SLIDE,
        TransitionType.FADE,
        TransitionType.FLIP,
        TransitionType.SHRINK,
        TransitionType.EXPAND,
        TransitionType.ELASTIC,
        TransitionType.CIRCLE_REVEAL,
        TransitionType.ROTATE,
        TransitionType.ZOOM_BLUR,
        TransitionType.WAVE
    )

    val availableColors = mapOf(
        "blue" to Color(0xFF1E88E5),
        "blue_dark" to Color(0xFF1565C0),
        "blue_light" to Color(0xFF90CAF9),
        "green" to Color(0xFF43A047),
        "green_dark" to Color(0xFF2E7D32),
        "green_light" to Color(0xFFA5D6A7),
        "red" to Color(0xFFD32F2F),
        "red_dark" to Color(0xFFB71C1C),
        "red_light" to Color(0xFFEF9A9A),
        "orange" to Color(0xFFFB8C00),
        "orange_dark" to Color(0xFFEF6C00),
        "orange_light" to Color(0xFFFFCC80),
        "amber" to Color(0xFFFFA000),
        "purple" to Color(0xFF7E57C2),
        "purple_dark" to Color(0xFF6A1B9A),
        "purple_light" to Color(0xFFAF89F5),
        "pink" to Color(0xFFD81B60),
        "pink_dark" to Color(0xFFC2185B),
        "pink_light" to Color(0xFFF48FB1),
        "teal" to Color(0xFF009688),
        "teal_dark" to Color(0xFF00695C),
        "teal_light" to Color(0xFF80CBC4),
        "cyan" to Color(0xFF00ACC1),
        "cyan_light" to Color(0xFF80DEEA),
        "indigo" to Color(0xFF3949AB),
        "blue_grey" to Color(0xFF607D8B),
        "yellow" to Color(0xFFFBC02D),
        "sage" to Color(0xFF748873),
        "petrol" to Color(0xFF276C76),
        "terracotta" to Color(0xFFB76E56),
        "mauve" to Color(0xFF9473A6),
        "sand" to Color(0xFFAA8C62),
        "graphite" to Color(0xFF62666D),
    )

    val defaultColors = mapOf(
        "blue" to Color(0xFF2196F3),
        "blue_dark" to Color(0xFF1565C0),
        "blue_light" to Color(0xFF90CAF9),
        "green" to Color(0xFF4CAF50),
        "green_dark" to Color(0xFF087F23),
        "red" to Color(0xFFF44336),
        "orange" to Color(0xFFFF823C),
        "purple" to Color(0xFF9C27B0),
        "purple_light" to Color(0xFFAF89F5),
        "pink" to Color(0xFFE91E63),
        "pink_light" to Color(0xFFF48FB1),
        "yellow" to Color(0xFFFFEB3B)
    )

    fun getDefaultColors(background: Color, onBackground: Color): Map<String, Color> {
        return defaultColors + mapOf(
            "background" to background,
            "on_background" to onBackground
        )
    }

    fun setAccentColor(color: Color) {
        val colorName = availableColors.entries.find { it.value == color }?.key ?: "custom"
        _accentColorName.value = colorName
        _accentColor.value = color
    }

    fun setFontType(fontType: String) {
        _fontType.value = fontType
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[themeKey] = mode.name
            }
            _themeMode.value = mode
        }
    }

    fun setDarkModeStyle(context: Context, style: DarkModeStyle) {
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[darkModeStyleKey] = style.name
            }
            _darkModeStyle.value = style
        }
    }

    fun setVibrationDuration(context: Context, duration: Int) {
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[vibrationDurationKey] = duration
            }
            _vibrationDuration.value = duration
        }
    }

    fun setVibrationPattern(context: Context, pattern: String) {
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[vibrationPatternKey] = pattern
            }
            _vibrationPattern.value = pattern
        }
    }

    fun setFontScale(context: Context, scale: Float) {
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[fontScaleKey] = scale.toString()
            }
            _fontScale.value = scale
        }
    }

    fun setListDensity(context: Context, density: ListDensity) {
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[listDensityKey] = density.name
            }
            _listDensity.value = density
        }
    }

    fun setGlassEffects(context: Context, effects: GlassEffects) {
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[glassEffectsKey] = effects.name
            }
            _glassEffects.value = effects
        }
    }

    private val contactSortOrderKey = stringPreferencesKey("contact_sort_order")
    private val _contactSortOrder = MutableStateFlow(ContactSortOrder.FIRST_NAME)
    val contactSortOrder: StateFlow<ContactSortOrder> = _contactSortOrder.asStateFlow()

    fun setContactSortOrder(context: Context, order: ContactSortOrder) {
        if (_contactSortOrder.value == order) return
        _contactSortOrder.value = order
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[contactSortOrderKey] = order.name
            }
        }
    }

    fun markBirthdayReminderPromptShown(context: Context) {
        if (_birthdayReminderPromptShown.value) return
        _birthdayReminderPromptShown.value = true
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[birthdayReminderPromptShownKey] = true
            }
        }
    }

    fun setAnimationSpeed(context: Context, speed: AnimationSpeed) {
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[animationSpeedKey] = speed.name
            }
            _animationSpeed.value = speed
        }
    }

    fun setTransitionType(context: Context, type: TransitionType) {
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[transitionTypeKey] = type.name
            }
            _transitionType.value = type
        }
    }

    suspend fun getOrCreateProfileId(context: Context): String {
        val dataStore = settingsStore.dataStore(context)
        val prefs = dataStore.data.first()
        val existingId = prefs[profileIdKey]
        if (existingId != null) {
            _profileId.value = existingId
            return existingId
        }
        val newId = java.util.UUID.randomUUID().toString()
        dataStore.edit { it[profileIdKey] = newId }
        _profileId.value = newId
        return newId
    }

    fun saveSettings(context: Context) {
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[fontTypeKey] = _fontType.value
                prefs[accentColorNameKey] = _accentColorName.value
                if (_accentColorName.value == "custom") {
                    prefs[accentColorKey] = _accentColor.value.value.toString()
                }
                prefs[passwordManagerEnabledKey] = _passwordManagerEnabled.value
                prefs[defaultAccountNameKey] = _defaultContactAccount.value.name
                prefs[defaultAccountTypeKey] = _defaultContactAccount.value.type
                prefs[vibrationDurationKey] = _vibrationDuration.value
                prefs[vibrationPatternKey] = _vibrationPattern.value
                prefs[favoritesColumnsKey] = _favoritesColumns.value
                prefs[favoritesCardShapeKey] = _favoritesCardShape.value.name
                prefs[favoritesSortKey] = _favoritesSortOrder.value.name
                prefs[favoritesCustomOrderKey] =
                    _favoritesCustomOrder.value.joinToString(separator = "|")
                prefs[accentBarStyleKey] = _accentBarStyle.value.name
                prefs[sensitiveSectionsLockKey] = _sensitiveSectionsLockEnabled.value
            }
            localeController.persist(context, _appLanguage.value)
        }
    }

    fun setPasswordManagerEnabled(context: Context, enabled: Boolean) {
        _passwordManagerEnabled.value = enabled
        if (!enabled) {
            lockProtectedSession()
            setToolsOrder(context, _toolsOrder.value)
        }
        saveSettings(context)
    }

    suspend fun configureBiometricAuth(context: Context) {
        pinAuthenticator(context).configureBiometric()
        _passwordAuthMethod.value = PasswordAuthMethod.BIOMETRIC
        _passwordBiometric.value = true
    }

    fun setPasswordPin(context: Context, pin: String) = viewModelScope.launch {
        configurePasswordPin(context, pin)
    }

    suspend fun configurePasswordPin(context: Context, pin: String) {
        pinAuthenticator(context).configure(pin)
        _passwordAuthMethod.value = PasswordAuthMethod.PIN
        _passwordBiometric.value = false
    }

    suspend fun clearPasswordAuth(context: Context) {
        lockProtectedSession()
        pinAuthenticator(context).clear()
        _passwordAuthMethod.value = null
        _passwordBiometric.value = false
        _sensitiveSectionsLockEnabled.value = false
    }

    suspend fun validatePin(context: Context, pin: String): PinResult = pinAuthenticator(context).verify(pin)

    fun setSensitiveSectionsLock(context: Context, enabled: Boolean) {
        _sensitiveSectionsLockEnabled.value = enabled
        saveSettings(context)
    }

    fun setDefaultContactAccount(context: Context, account: ContactAccount) {
        _defaultContactAccount.value = account
        saveSettings(context)
    }

    fun setFavoritesColumns(context: Context, columns: Int) {
        val normalized = columns.coerceIn(2, 3)
        _favoritesColumns.value = normalized
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[favoritesColumnsKey] = normalized
            }
        }
    }

    fun setFavoritesCardShape(context: Context, shape: FavoritesCardShape) {
        _favoritesCardShape.value = shape
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[favoritesCardShapeKey] = shape.name
            }
        }
    }

    fun setFavoritesSortOrder(context: Context, order: FavoritesSortOrder) {
        _favoritesSortOrder.value = order
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[favoritesSortKey] = order.name
            }
        }
    }

    fun updateFavoritesCustomOrder(context: Context, ids: List<String>) {
        val sanitized = ids.distinct()
        if (sanitized == _favoritesCustomOrder.value) return
        _favoritesCustomOrder.value = sanitized
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[favoritesCustomOrderKey] = sanitized.joinToString(separator = "|")
            }
        }
    }

    fun setShowProfileInFavorites(context: Context, show: Boolean) {
        _showProfileInFavorites.value = show
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[showProfileInFavoritesKey] = show
            }
        }
    }

    fun setAccentBarStyle(context: Context, style: AccentBarStyle) {
        _accentBarStyle.value = style
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[accentBarStyleKey] = style.name
                com.bookverse.contacts.settings.ModernAppearanceMigration.markApplied(prefs)
            }
        }
    }

    fun setExpenseCurrency(context: Context, currencyCode: String) {
        val resolved = ExpenseCurrencyProvider.fromCode(
            currencyCode,
            LocaleController.effectiveLocale(context),
        )
        _expenseCurrency.value = resolved
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[expenseCurrencyKey] = resolved.code
            }
        }
    }

    fun setToolsOrder(context: Context, order: com.bookverse.contacts.data.model.ToolsOrderConfig) {
        val unavailableToolIds = if (_passwordManagerEnabled.value) {
            emptySet()
        } else {
            setOf("passwords")
        }
        val normalizedOrder = order.normalized(unavailableToolIds)
        _toolsOrder.value = normalizedOrder
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[toolsOrderKey] = normalizedOrder.toJson()
            }
        }
    }

    fun setToolHidden(context: Context, toolId: String, hidden: Boolean) {
        val updated = _toolsOrder.value.withToolHidden(toolId, hidden)
        setToolsOrder(context, updated)
    }

    fun setToolsMenuHidden(context: Context, hidden: Boolean) {
        val updated = _toolsOrder.value.withMenuHidden(hidden)
        setToolsOrder(context, updated)
    }

    fun setNoteFoldersOrder(context: Context, order: List<Long>) {
        _noteFoldersOrder.value = order
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[noteFoldersOrderKey] = order.joinToString(",")
            }
        }
    }

    fun setPasswordFoldersOrder(context: Context, order: List<Long>) {
        _passwordFoldersOrder.value = order
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[passwordFoldersOrderKey] = order.joinToString(",")
            }
        }
    }

    fun setStartupScreen(context: Context, screen: String) {
        _startupScreen.value = screen
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[startupScreenKey] = screen
            }
        }
    }

    fun setCallAnswerStyle(context: Context, style: String) {
        _callAnswerStyle.value = style
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[callAnswerStyleKey] = style
            }
        }
    }

    fun setImmersiveIncomingCalls(context: Context, enabled: Boolean) {
        _immersiveIncomingCalls.value = enabled
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[immersiveIncomingCallsKey] = enabled
            }
        }
    }

    fun setAnswerWithVolumeUpEnabled(context: Context, enabled: Boolean) {
        _answerWithVolumeUpEnabled.value = enabled
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[answerWithVolumeUpKey] = enabled
            }
        }
    }

    fun setDeclineWithVolumeDownEnabled(context: Context, enabled: Boolean) {
        _declineWithVolumeDownEnabled.value = enabled
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[declineWithVolumeDownKey] = enabled
            }
        }
    }

    fun setDtmfTonesEnabled(context: Context, enabled: String) {
        _dtmfTonesEnabled.value = enabled
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                prefs[dtmfTonesEnabledKey] = enabled
            }
        }
    }

    fun setSpeedDialContact(context: Context, number: Int, contactId: String?) {
        val currentMap = _speedDialContacts.value.toMutableMap()
        if (contactId != null) {
            currentMap[number] = contactId
        } else {
            currentMap.remove(number)
        }
        _speedDialContacts.value = currentMap
        viewModelScope.launch {
            settingsStore.dataStore(context).edit { prefs ->
                val serialized = currentMap.entries.joinToString("|") { "${it.key}:${it.value}" }
                prefs[speedDialKey] = serialized
            }
        }
    }

    fun setAppLanguage(context: Context, language: AppLanguage) {
        if (_appLanguage.value == language) return
        _appLanguage.value = language
        viewModelScope.launch {
            localeController.persist(context, language)
            // Persist before recreating the activity so a language change survives the restart.
            localeController.apply(language)
        }
    }

    suspend fun preloadSettings(context: Context) {
        try {
            val dataStore = settingsStore.dataStore(context)
            val prefs = dataStore.edit(com.bookverse.contacts.settings.ModernAppearanceMigration::migrate)

            _themeMode.value = ThemeMode.fromName(prefs[themeKey])
            _darkModeStyle.value = DarkModeStyle.fromName(prefs[darkModeStyleKey])

            val storedColorName = prefs[accentColorNameKey] ?: "blue"
            val storedColorValue = prefs[accentColorKey]
            if (storedColorName == "custom" && storedColorValue != null) {
                runCatching { Color(storedColorValue.toULong()) }
                    .onSuccess {
                        _accentColor.value = it
                        _accentColorName.value = "custom"
                    }
                    .onFailure {
                        _accentColor.value = availableColors["blue"] ?: Color(0xFF2196F3)
                        _accentColorName.value = "blue"
                    }
            } else {
                val resolved =
                    availableColors[storedColorName] ?: availableColors["blue"] ?: Color(0xFF2196F3)
                _accentColor.value = resolved
                _accentColorName.value = storedColorName
            }

            _fontType.value = prefs[fontTypeKey] ?: "default"
            _animationSpeed.value = AnimationSpeed.fromName(prefs[animationSpeedKey])
            _transitionType.value = TransitionType.fromName(prefs[transitionTypeKey])
            _vibrationDuration.value = prefs[vibrationDurationKey] ?: 50
            _vibrationPattern.value = prefs[vibrationPatternKey] ?: "simple"
            _birthdayReminderPromptShown.value =
                prefs[birthdayReminderPromptShownKey] ?: false
            _defaultDialerConsent.value = prefs[defaultDialerConsentKey] ?: false
            _passwordManagerEnabled.value = prefs[passwordManagerEnabledKey] ?: true
            _passwordBiometric.value = prefs[passwordBiometricKey] ?: true
            _passwordAuthMethod.value = PasswordAuthMethod.fromName(prefs[passwordAuthMethodKey])
            val lockEnabled = prefs[sensitiveSectionsLockKey] ?: false
            _sensitiveSectionsLockEnabled.value =
                if (_passwordAuthMethod.value == null) false else lockEnabled

            val accountName = prefs[defaultAccountNameKey]
            val accountType = prefs[defaultAccountTypeKey]
            _defaultContactAccount.value = if (accountName != null && accountType != null) {
                ContactAccount(name = accountName, type = accountType, displayName = accountName)
            } else {
                ContactAccount.LOCAL_ACCOUNT
            }

            _favoritesColumns.value = (prefs[favoritesColumnsKey] ?: 2).coerceIn(2, 3)
            _favoritesCardShape.value = FavoritesCardShape.fromName(prefs[favoritesCardShapeKey])
            _favoritesSortOrder.value = FavoritesSortOrder.fromName(prefs[favoritesSortKey])
            _favoritesCustomOrder.value =
                prefs[favoritesCustomOrderKey]?.takeIf { it.isNotBlank() }
                    ?.split('|')
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList()
            _accentBarStyle.value = AccentBarStyle.fromName(prefs[accentBarStyleKey])
            val storedLanguage = localeController.readAndMigrate(context)
            _appLanguage.value = storedLanguage

            val storedCurrency = prefs[expenseCurrencyKey]
            val resolvedCurrency = storedCurrency?.let {
                ExpenseCurrencyProvider.fromCode(it, LocaleController.effectiveLocale(context))
            }
                ?: ExpenseCurrencyProvider.defaultFor(context)
            if (storedCurrency == null) {
                dataStore.edit { editPrefs ->
                    editPrefs[expenseCurrencyKey] = resolvedCurrency.code
                }
            }
            _expenseCurrency.value = resolvedCurrency

            val storedOrder = prefs[toolsOrderKey]
            _toolsOrder.value = storedOrder?.let {
                com.bookverse.contacts.data.model.ToolsOrderConfig.fromJson(it)
            }?.normalized(
                unavailableToolIds = if (_passwordManagerEnabled.value) {
                    emptySet()
                } else {
                    setOf("passwords")
                }
            ) ?: com.bookverse.contacts.data.model.ToolsOrderConfig.default()
            val storedFoldersOrder = prefs[noteFoldersOrderKey]
            _noteFoldersOrder.value = storedFoldersOrder
                ?.split(',')
                ?.mapNotNull { it.toLongOrNull() }
                ?: emptyList()
            val storedPasswordFoldersOrder = prefs[passwordFoldersOrderKey]
            _passwordFoldersOrder.value = storedPasswordFoldersOrder
                ?.split(',')
                ?.mapNotNull { it.toLongOrNull() }
                ?: emptyList()

            _startupScreen.value = prefs[startupScreenKey] ?: "contacts"
            _fontScale.value = prefs[fontScaleKey]?.toFloatOrNull() ?: 1.0f
            _listDensity.value = ListDensity.fromName(prefs[listDensityKey])
            _glassEffects.value = GlassEffects.fromName(prefs[glassEffectsKey])
            _contactSortOrder.value = ContactSortOrder.fromName(prefs[contactSortOrderKey])
            _callAnswerStyle.value = prefs[callAnswerStyleKey] ?: "swipe"
            _dtmfTonesEnabled.value = prefs[dtmfTonesEnabledKey] ?: "disabled"
            _immersiveIncomingCalls.value = prefs[immersiveIncomingCallsKey] ?: false
            _answerWithVolumeUpEnabled.value = prefs[answerWithVolumeUpKey] ?: false
            _declineWithVolumeDownEnabled.value = prefs[declineWithVolumeDownKey] ?: false
            _showProfileInFavorites.value = prefs[showProfileInFavoritesKey] ?: false
            _profileId.value = prefs[profileIdKey]

            val speedDialData = prefs[speedDialKey] ?: ""
            _speedDialContacts.value = if (speedDialData.isNotEmpty()) {
                speedDialData.split("|")
                    .mapNotNull { entry ->
                        val parts = entry.split(":")
                        if (parts.size == 2) {
                            parts[0].toIntOrNull()?.let { key -> key to parts[1] }
                        } else null
                    }
                    .toMap()
            } else {
                emptyMap()
            }
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.SETTINGS_PRELOAD_FAILED)
        }
    }

    fun exportSettingsToJson(): String {
        val backup = com.bookverse.contacts.data.model.AppSettingsBackup(
            accentColorName = _accentColorName.value,
            fontType = _fontType.value,
            animationSpeed = _animationSpeed.value.name,
            vibrationDuration = _vibrationDuration.value,
            vibrationPattern = _vibrationPattern.value,
            themeMode = _themeMode.value.name,
            transitionType = _transitionType.value.name,
            defaultDialerConsent = _defaultDialerConsent.value,
            passwordManagerEnabled = _passwordManagerEnabled.value,
            sensitiveSectionsLockEnabled = _sensitiveSectionsLockEnabled.value,
            defaultContactAccount = _defaultContactAccount.value.name,
            favoritesColumns = _favoritesColumns.value,
            favoritesCardShape = _favoritesCardShape.value.name,
            favoritesSortOrder = _favoritesSortOrder.value.name,
            favoritesCustomOrder = _favoritesCustomOrder.value,
            accentBarStyle = _accentBarStyle.value.name,
            appLanguage = LocaleController.stableLanguageTag(_appLanguage.value),
            expenseCurrency = _expenseCurrency.value.code,
            toolsOrder = _toolsOrder.value,
            startupScreen = _startupScreen.value,
            callAnswerStyle = _callAnswerStyle.value,
            dtmfTonesEnabled = _dtmfTonesEnabled.value,
            immersiveIncomingCalls = _immersiveIncomingCalls.value,
            answerWithVolumeUp = _answerWithVolumeUpEnabled.value,
            declineWithVolumeDown = _declineWithVolumeDownEnabled.value,
            fontScale = _fontScale.value,
            listDensity = _listDensity.value.name,
            darkModeStyle = _darkModeStyle.value.name,
            customAccentColor = _accentColor.value.value.toString().takeIf { _accentColorName.value == "custom" },
        )
        return backup.toJson()
    }

    fun importSettingsFromJson(context: Context, json: String): Boolean {
        return try {
            val backup = com.bookverse.contacts.data.model.AppSettingsBackup.fromJson(json) ?: return false
            val importedLanguage = LocaleController.languageForStoredValue(backup.appLanguage)
            val importedFavoritesCardShape = FavoritesCardShape.fromName(backup.favoritesCardShape)
            _appLanguage.value = importedLanguage

            viewModelScope.launch {
                val colorValue = backup.customAccentColor?.toULongOrNull()?.let { Color(it) }
                    ?: availableColors[backup.accentColorName] ?: availableColors.getValue("blue")
                setAccentColor(colorValue)

                val dataStore = settingsStore.dataStore(context)
                dataStore.edit { prefs ->
                    prefs[fontTypeKey] = backup.fontType
                    prefs[animationSpeedKey] = backup.animationSpeed
                    prefs[vibrationDurationKey] = backup.vibrationDuration
                    prefs[vibrationPatternKey] = backup.vibrationPattern
                    prefs[themeKey] = backup.themeMode
                    prefs[transitionTypeKey] = backup.transitionType
                    prefs[defaultDialerConsentKey] = backup.defaultDialerConsent
                    prefs[passwordManagerEnabledKey] = backup.passwordManagerEnabled
                    prefs[sensitiveSectionsLockKey] = backup.sensitiveSectionsLockEnabled
                    prefs[defaultAccountNameKey] = backup.defaultContactAccount
                    prefs[defaultAccountTypeKey] = backup.defaultContactAccount
                    prefs[favoritesColumnsKey] = backup.favoritesColumns
                    prefs[favoritesCardShapeKey] = importedFavoritesCardShape.name
                    prefs[favoritesSortKey] = backup.favoritesSortOrder
                    prefs[favoritesCustomOrderKey] = backup.favoritesCustomOrder.joinToString("|")
                    prefs[accentBarStyleKey] = AccentBarStyle.fromName(backup.accentBarStyle).name
                    com.bookverse.contacts.settings.ModernAppearanceMigration.markApplied(prefs)
                    prefs[expenseCurrencyKey] = backup.expenseCurrency
                    prefs[toolsOrderKey] = backup.toolsOrder.toJson()
                    prefs[startupScreenKey] = backup.startupScreen
                    prefs[callAnswerStyleKey] = backup.callAnswerStyle
                    prefs[dtmfTonesEnabledKey] = backup.dtmfTonesEnabled
                    prefs[immersiveIncomingCallsKey] = backup.immersiveIncomingCalls
                    prefs[answerWithVolumeUpKey] = backup.answerWithVolumeUp
                    prefs[declineWithVolumeDownKey] = backup.declineWithVolumeDown
                    prefs[accentColorNameKey] = _accentColorName.value
                    prefs[accentColorKey] = colorValue.value.toString()
                    prefs[darkModeStyleKey] = DarkModeStyle.fromName(backup.darkModeStyle).name
                    prefs[fontScaleKey] = backup.fontScale.toString()
                    prefs[listDensityKey] = backup.listDensity
                }
                localeController.persist(context, importedLanguage)
                localeController.apply(importedLanguage)

                _darkModeStyle.value = DarkModeStyle.fromName(backup.darkModeStyle)
                _fontType.value = backup.fontType
                try {
                    _animationSpeed.value = AnimationSpeed.valueOf(backup.animationSpeed)
                } catch (_: Exception) {
                    _animationSpeed.value = AnimationSpeed.NORMAL
                }
                _vibrationDuration.value = backup.vibrationDuration
                _vibrationPattern.value = backup.vibrationPattern
                try {
                    _themeMode.value = ThemeMode.valueOf(backup.themeMode)
                } catch (_: Exception) {
                    _themeMode.value = ThemeMode.SYSTEM
                }
                try {
                    _transitionType.value = TransitionType.valueOf(backup.transitionType)
                } catch (_: Exception) {
                    _transitionType.value = TransitionType.SLIDE
                }
                _defaultDialerConsent.value = backup.defaultDialerConsent
                _passwordManagerEnabled.value = backup.passwordManagerEnabled
                _sensitiveSectionsLockEnabled.value = backup.sensitiveSectionsLockEnabled
                _defaultContactAccount.value = ContactAccount(
                    name = backup.defaultContactAccount,
                    type = backup.defaultContactAccount,
                    displayName = backup.defaultContactAccount
                )
                _favoritesColumns.value = backup.favoritesColumns
                _favoritesCardShape.value = importedFavoritesCardShape
                try {
                    _favoritesSortOrder.value = FavoritesSortOrder.valueOf(backup.favoritesSortOrder)
                } catch (_: Exception) {
                    _favoritesSortOrder.value = FavoritesSortOrder.ALPHABETICAL
                }
                _favoritesCustomOrder.value = backup.favoritesCustomOrder
                try {
                    _accentBarStyle.value = AccentBarStyle.valueOf(backup.accentBarStyle)
                } catch (_: Exception) {
                    _accentBarStyle.value = AccentBarStyle.MODERN
                }
                _expenseCurrency.value = ExpenseCurrencyProvider.fromCode(
                    backup.expenseCurrency,
                    LocaleController.effectiveLocale(context),
                )
                _toolsOrder.value = backup.toolsOrder
                _startupScreen.value = backup.startupScreen
                _callAnswerStyle.value = backup.callAnswerStyle
                _dtmfTonesEnabled.value = backup.dtmfTonesEnabled
                _immersiveIncomingCalls.value = backup.immersiveIncomingCalls
                _answerWithVolumeUpEnabled.value = backup.answerWithVolumeUp
                _declineWithVolumeDownEnabled.value = backup.declineWithVolumeDown
                _fontScale.value = backup.fontScale
                _listDensity.value = ListDensity.fromName(backup.listDensity)
            }
            true
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.SETTINGS_IMPORT_FAILED)
            false
        }
    }
}

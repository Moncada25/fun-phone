package com.bookverse.contacts.benchmark

import com.bookverse.contacts.data.backup.AppBackupManager
import com.bookverse.contacts.BuildConfig
import com.bookverse.contacts.utils.PhoneActions
import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bookverse.contacts.App
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.expenses.ExpenseBudgetEntity
import com.bookverse.contacts.data.expenses.ExpenseCategoryEntity
import com.bookverse.contacts.data.expenses.ExpenseTransactionEntity
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.data.model.ToolsOrderConfig
import com.bookverse.contacts.data.notes.ChecklistItem
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.NoteImage
import com.bookverse.contacts.data.notes.NoteTag
import com.bookverse.contacts.data.notes.NoteTagCrossRef
import com.bookverse.contacts.data.notes.ReminderData
import com.bookverse.contacts.data.notes.ReminderRepeat
import com.bookverse.contacts.data.notes.ReminderType
import com.bookverse.contacts.data.password.PasswordEntity
import com.bookverse.contacts.data.password.PasswordRepository
import com.bookverse.contacts.data.repository.ContactCacheRepository
import com.bookverse.contacts.domain.call.CallContactLookup
import com.bookverse.contacts.domain.call.CallContactPresentation
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.domain.call.UiCall
import com.bookverse.contacts.integration.ExpensesViewModelFactory
import com.bookverse.contacts.integration.NotesViewModelFactory
import com.bookverse.contacts.security.CryptoManager
import com.bookverse.contacts.settings.AppDataStore
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.navigation.MainNavigationScaffold
import com.bookverse.contacts.ui.navigation.MainTabNavigationBar
import com.bookverse.contacts.ui.navigation.NavigationItem
import com.bookverse.contacts.ui.screens.calls.IncomingCallScreen
import com.bookverse.contacts.ui.screens.calls.RejectWithMessageSheet
import com.bookverse.contacts.ui.screens.contacts.ContactListScreen
import com.bookverse.contacts.ui.screens.dialer.DialerScreen
import com.bookverse.contacts.ui.screens.contacts.FavoritesScreen
import com.bookverse.contacts.ui.screens.expenses.ExpenseManagerScreen
import com.bookverse.contacts.ui.screens.games.GamesScreen
import com.bookverse.contacts.ui.screens.notes.NoteListScreen
import com.bookverse.contacts.ui.screens.notes.NoteScreen
import com.bookverse.contacts.ui.screens.passwords.PasswordBackupActions
import com.bookverse.contacts.ui.screens.passwords.PasswordListScreen
import com.bookverse.contacts.ui.screens.qr.QRGeneratorScreen
import com.bookverse.contacts.ui.screens.settings.SettingsScreen
import com.bookverse.contacts.ui.screens.tools.ToolsScreen
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.settings.Fonts
import com.bookverse.contacts.ui.theme.accentPalette
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactTagViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.theme.DarkModeStyle
import com.bookverse.contacts.ui.viewmodel.ExpensesViewModel
import com.bookverse.contacts.ui.viewmodel.NotesViewModel
import com.bookverse.contacts.ui.viewmodel.PasswordsViewModel
import com.bookverse.contacts.ui.viewmodel.QRHistoryViewModel
import com.bookverse.contacts.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Shell-only capture host. Reuses shipped Compose screens; never enters Telecom or a real vault. */
class StorePreviewActivity : FragmentActivity() {
    private val container get() = (application as App).container
    private val settings: AppSettingsViewModel by viewModels { container.appSettingsViewModelFactory }
    private val contacts: ContactsViewModel by viewModels { container.contactsViewModelFactory }
    private val tags: ContactTagViewModel by viewModels { container.contactTagViewModelFactory }
    private val notes: NotesViewModel by viewModels { NotesViewModelFactory(this) }
    private val expenses: ExpensesViewModel by viewModels { ExpensesViewModelFactory(this) }
    private val qrHistory: QRHistoryViewModel by viewModels { container.qrHistoryViewModelFactory }
    private val passwords: PasswordsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PasswordsViewModel(PasswordRepository(AppDatabase.getInstance(applicationContext).passwordDao(), applicationContext)) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        check(packageName == "com.bookverse.contacts.community.benchmark")
        // The host refuses any ability to read the physical phone's personal providers.
        check(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        check(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
        val scene = intent.getStringExtra("scene") ?: "favorites"
        check(scene in SCENES)
        val language = intent.getStringExtra("language")?.takeIf { it in LANGUAGES } ?: "es"
        val dark = intent.getBooleanExtra("dark", false)
        val style = AccentBarStyle.fromName(intent.getStringExtra("style"))
        val darkStyle = DarkModeStyle.fromName(intent.getStringExtra("darkStyle"))
        val accent = intent.getStringExtra("accent")?.takeIf { it in settings.availableColors } ?: "blue"
        val number = intent.getStringExtra("number")?.takeIf { value -> value.length <= 16 && value.all { it.isDigit() || it == '+' } } ?: "555"
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        lifecycleScope.launch {
            val fixtureNote = withContext(Dispatchers.IO) {
                seedFixtures(language, style, dark, darkStyle, accent)
                AppDatabase.getInstance(applicationContext).noteDao().getAllNotesSync().first { it.id == 90_002L }
            }
            LocaleController().apply(LocaleController.languageForStoredValue(language))
            settings.preloadSettings(applicationContext)
            // The VM observes our fixture cache on initialization; preloading would refresh the provider.
            if (scene == "vault") passwords.unlock()
            if (scene.startsWith("call")) seedFixtureCall()
            val configuration = Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
            val localized = createConfigurationContext(configuration)
            setContent {
                CompositionLocalProvider(
                    LocalContext provides localized,
                    LocalConfiguration provides configuration,
                    LocalActivityResultRegistryOwner provides this@StorePreviewActivity,
                    LocalOnBackPressedDispatcherOwner provides this@StorePreviewActivity,
                ) {
                    val accentColor by settings.accentColor.collectAsStateWithLifecycle()
                    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
                    val darkModeStyle by settings.darkModeStyle.collectAsStateWithLifecycle()
                    val accentBarStyle by settings.accentBarStyle.collectAsStateWithLifecycle()
                    val fontType by settings.fontType.collectAsStateWithLifecycle()
                    ContactsAppTheme(
                        accentSeed = accentColor,
                        fontFamily = Fonts.getFontFamily(fontType),
                        themeMode = themeMode,
                        darkModeStyle = darkModeStyle,
                        accentBarStyle = accentBarStyle,
                    ) {
                        StoreScene(scene, language, number, fixtureNote, accentBarStyle)
                    }
                }
            }
        }
    }

    @Composable
    private fun StoreScene(scene: String, language: String, number: String, fixtureNote: Note, style: AccentBarStyle) {
        val colors = MaterialTheme.colorScheme
        val primaryTab = scene in setOf("favorites", "contacts", "tools")
        val callScene = scene.startsWith("call")
        // Retro tints the status bar with the accent, exactly as the shipped activity does.
        val statusBackground = if (style == AccentBarStyle.RETRO && !callScene) MaterialTheme.accentPalette.seed else colors.background
        val statusContent = if (statusBackground.luminance() > 0.5f) Color.Black else Color.White
        // Screens already reserve the display-cutout band, so the fixture chrome is drawn inside it.
        val cutout = with(LocalDensity.current) { WindowInsets.displayCutout.getTop(this).toDp() }
        val chrome: @Composable (Modifier) -> Unit = { modifier ->
            // Normalized fixture chrome: no real clock, notifications, carrier or battery data.
            Row(modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("9:41", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = statusContent)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Default.SignalCellular4Bar, null, Modifier.size(16.dp), tint = statusContent)
                    Icon(Icons.Default.Wifi, null, Modifier.size(16.dp), tint = statusContent)
                    Icon(Icons.Default.BatteryFull, null, Modifier.size(16.dp), tint = statusContent)
                }
            }
        }
        Box(Modifier.fillMaxSize().semantics { contentDescription = "store-preview:$scene:$language" }) {
        Column(Modifier.fillMaxSize().background(colors.background)) {
            if (cutout < 24.dp) chrome(Modifier.background(statusBackground).height(28.dp))
            Box(Modifier.weight(1f)) {
                if (primaryTab) {
                    MainNavigationScaffold(
                        modifier = Modifier,
                        bottomBar = {
                            MainTabNavigationBar(
                                tabs = listOf(NavigationItem.Favorites, NavigationItem.Contacts, NavigationItem.Recents, NavigationItem.Extras),
                                indicatorProgress = when (scene) { "contacts" -> 1f; "tools" -> 3f; else -> 0f },
                                backgroundColor = colors.background,
                                onTabSelected = {},
                            )
                        },
                        floatingActionButton = {},
                        containerColor = colors.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    ) { padding -> Box(Modifier.fillMaxSize().padding(padding)) { SceneContent(scene, number, fixtureNote) } }
                } else {
                    SceneContent(scene, number, fixtureNote)
                }
            }
            Box(Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(108.dp).height(4.dp).background(colors.onBackground.copy(alpha = 0.7f), RoundedCornerShape(8.dp)))
            }
        }
        if (cutout >= 24.dp) chrome(Modifier.height(cutout))
        }
    }

    @Composable
    private fun SceneContent(scene: String, number: String, fixtureNote: Note) {
        when (scene) {
            "favorites" -> FavoritesScreen(contacts, {}, appSettingsViewModel = settings)
            "contacts" -> ContactListScreen(contacts, tags, {}, settings, onCreateContact = {})
            "dialer" -> DialerScreen({}, settings, contacts, number, {}, phoneActions = PhoneActions, onCallPhoneNumber = { _, _ -> })
            "call", "call-reply" -> {
                val themeMode by settings.themeMode.collectAsStateWithLifecycle()
                IncomingCallScreen(FIXTURE_CALL_ID, {}, {}, {}, FixtureCallLookup, themeMode, settings)
                if (scene == "call-reply") RejectWithMessageSheet(onSend = {}, onDismiss = {})
            }
            "notes" -> NoteListScreen({}, { _, _, _ -> }, { _, _ -> }, { _, _ -> }, {}, {}, {}, notesViewModel = notes, appSettingsViewModel = settings)
            "note" -> NoteScreen(fixtureNote, {}, {}, settings)
            "vault" -> PasswordListScreen({}, {}, {}, {}, {}, passwords, settings, PasswordBackupActions({}, {}))
            "expenses" -> ExpenseManagerScreen({}, expenses, settings)
            "tools" -> ToolsScreen({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, settings, rememberLazyListState())
            "games" -> GamesScreen({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, settings, container.gameStatsRepository)
            "qr" -> QRGeneratorScreen({}, settings, qrHistory)
            else -> SettingsScreen({}, settings, AppBackupManager(this, container.gameStatsRepository, BuildConfig.VERSION_NAME), contactsViewModel = contacts, notesViewModel = notes, listState = rememberLazyListState())
        }
    }

    /** In-memory fictional ringing call: no Telecom call, service or provider is involved. */
    private fun seedFixtureCall() {
        val avatar = resources.openRawResource(R.drawable.avatar_person_pink).use { it.readBytes() }
        container.callRepository.upsert(
            FIXTURE_CALL_ID,
            UiCall(id = FIXTURE_CALL_ID, number = "+1 555 010 0004", displayName = "Emma Wilson", state = CallState.RINGING, avatarBytes = avatar, canRespondViaText = true),
        )
    }

    private object FixtureCallLookup : CallContactLookup {
        override suspend fun findByPhoneNumber(phoneNumber: String): CallContactPresentation? = null
        override suspend fun loadPhoto(uri: Uri): Bitmap? = null
    }

    private suspend fun seedFixtures(language: String, style: AccentBarStyle, dark: Boolean, darkStyle: DarkModeStyle, accent: String) {
        val db = AppDatabase.getInstance(applicationContext)
        // Refuse to mix a screenshot fixture with anything created outside this capture host.
        check(db.noteDao().getAllNotesSync().all { it.id in FIXTURE_IDS })
        check(db.passwordDao().getAllSync().all { it.id in FIXTURE_IDS })
        check(db.expenseDao().observeTransactions().first().all { it.id in FIXTURE_IDS })
        val contactCache = ContactCacheRepository(applicationContext)
        check(contactCache.getCachedContacts().all { it.id.startsWith("store-fixture-") })
        check(contactCache.getCachedProfile() == null)
        val fixtures = assets.open("store-fixtures.json").bufferedReader().use { JSONObject(it.readText()) }
        val copy = fixtures.getJSONObject(language)
        fun strings(values: JSONArray): List<String> = List(values.length()) { values.getString(it) }
        fun localizedList(key: String): List<String> = strings(copy.getJSONArray(key))
        val names = listOf("Marco Vega", "Alex Rivera", "Sofía Torres", "Leo Martín", "Emma Wilson", "Daniel Costa", "Maya Chen", "Oliver Reed", "Lucía Silva", "Mateo Ruiz", "Zoe Moreau", "Liam Becker")
        val avatars = listOf(R.drawable.avatar_person_orange, R.drawable.avatar_person_teal, R.drawable.avatar_person_violet, R.drawable.avatar_person_blue, R.drawable.avatar_person_pink, R.drawable.avatar_person_green)
        contactCache.replaceCachedContacts(names.mapIndexed { index, name ->
            Contact(
                id = "store-fixture-$index",
                name = name,
                phones = listOf(ContactPhone("+15550100${index.toString().padStart(3, '0')}", null)),
                isFavorite = index < 6,
                photoUri = Uri.parse("android.resource://$packageName/${avatars[index % avatars.size]}"),
                email = "demo${index + 1}@example.test",
            )
        }, null)
        AppDataStore.getDataStore(applicationContext).edit {
            it[stringPreferencesKey("app_language")] = language
            it[stringPreferencesKey("expense_currency")] = "USD"
            it[stringPreferencesKey("tools_order_v2")] = ToolsOrderConfig.default().copy(hiddenItems = emptyList(), toolsMenuHidden = false).toJson()
            it[booleanPreferencesKey("show_profile_in_favorites")] = false
            it[booleanPreferencesKey("modern_appearance_migrated")] = true
            it[stringPreferencesKey("accent_bar_style")] = style.name
            it[stringPreferencesKey("theme_mode")] = if (dark) ThemeMode.DARK.name else ThemeMode.LIGHT.name
            it[stringPreferencesKey("dark_mode_style")] = darkStyle.name
            it[stringPreferencesKey("accent_color_name")] = accent
        }
        val now = System.currentTimeMillis()
        // Tags ignore conflicts and images abort on them, so the fixture range is rebuilt on every launch.
        db.openHelper.writableDatabase.apply {
            execSQL("DELETE FROM note_tag_cross_ref WHERE noteId BETWEEN 90000 AND 90999")
            execSQL("DELETE FROM note_tags WHERE id BETWEEN 90000 AND 90999")
            execSQL("DELETE FROM note_images WHERE id BETWEEN 90000 AND 90999")
            execSQL("DELETE FROM reminder_data WHERE id BETWEEN 90000 AND 90999")
        }
        val titles = localizedList("noteTitles")
        val bodies = localizedList("noteBodies")
        titles.forEachIndexed { index, title ->
            db.noteDao().insertNote(Note(id = 90_001L + index, title = title, content = bodies[index], isChecklist = index == 2, isReminder = index == 3, isPinned = index < 2, timestamp = now - index * 60_000L, createdAt = now, updatedAt = now))
        }
        localizedList("checklist").forEachIndexed { index, text ->
            db.noteDao().insertChecklistItem(ChecklistItem(id = 90_101L + index, noteId = 90_003L, text = text, isChecked = index == 0))
        }
        val reminderAt = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        db.noteDao().insertReminderData(ReminderData(id = 90_801L, noteId = 90_004L, date = reminderAt, interval = 0, message = titles[3], type = ReminderType.SINGLE_EVENT, repeatMode = ReminderRepeat.WEEKLY))
        val tagColors = listOf("#E7663C", "#2E9D8F", "#7E57C2", "#C39A54")
        localizedList("noteTags").forEachIndexed { index, name ->
            db.noteDao().insertNoteTag(NoteTag(id = 90_601L + index, name = name, colorHex = tagColors[index]))
        }
        listOf(0 to 0, 1 to 1, 2 to 1, 2 to 2, 3 to 2, 4 to 3, 5 to 0, 6 to 2, 7 to 0).forEach { (note, tag) ->
            db.noteDao().insertNoteTagCrossRef(NoteTagCrossRef(noteId = 90_001L + note, tagId = 90_601L + tag))
        }
        listOf("storefixture1.jpg", "storefixture2.jpg").forEachIndexed { index, fileName ->
            writeFixturePhoto(File(com.bookverse.contacts.utils.NotesImageStore.directory(applicationContext), fileName), index)
            db.noteDao().insertNoteImage(NoteImage(id = 90_701L + index, noteId = 90_002L, fileName = fileName, createdAt = now + index))
        }
        val vaultNames = localizedList("vaultNames")
        val vaultSites = strings(fixtures.getJSONArray("vaultSites"))
        vaultNames.forEachIndexed { index, name ->
            fun sealed(value: String) = checkNotNull(CryptoManager.encrypt(applicationContext, value.toByteArray()))
            db.passwordDao().insert(
                PasswordEntity(
                    90_201L + index, name, sealed("demo${index + 1}@example.test"), sealed("Synthetic-fixture-$index!Qz7"), sealed(""),
                    urlEnc = sealed(vaultSites[index]),
                    totpEnc = sealed("JBSWY3DPEHPK3PX" + "PQRSTUVW"[index]),
                ),
            )
        }
        val categories = localizedList("expenseCategories")
        val colors = listOf("#D67247", "#6B8C70", "#6388AA", "#C39A54", "#8E6BB0", "#C2577A")
        categories.forEachIndexed { index, name ->
            db.expenseDao().insertCategory(ExpenseCategoryEntity(90_301L + index, name, colors[index], type = if (index == 3) ExpenseType.INCOME else ExpenseType.EXPENSE))
        }
        // A full fictional year so the yearly view has every month up to the capture date.
        val base = listOf(850.0, 142.5, 58.0, 3200.0, 96.0, 64.0)
        val today = LocalDate.now()
        var transactionId = 90_401L
        for (month in 1..today.monthValue) {
            base.forEachIndexed { index, amount ->
                val swing = listOf(0, 6, -4, 9, 3, -7, 12, -2, 8, -5, 0, 5)[(month + index * 5) % 12] / 100.0
                val scaled = if (index == 0 || month == today.monthValue) amount else Math.round(amount * (1 + swing) * 2) / 2.0
                val day = minOf(index + 1, if (month == today.monthValue) today.dayOfMonth else 28)
                db.expenseDao().insertTransaction(ExpenseTransactionEntity(transactionId++, if (index == 3) ExpenseType.INCOME else ExpenseType.EXPENSE, scaled, 90_301L + index, categories[index], colors[index], LocalDate.of(today.year, month, day)))
            }
        }
        db.expenseDao().insertBudget(ExpenseBudgetEntity(90_501L, 90_302L, 450.0))
    }

    /** Procedural landscape: a believable note attachment without shipping or reading a real photo. */
    private fun writeFixturePhoto(target: File, variant: Int) {
        val width = 1200
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val sky = if (variant == 0) intArrayOf(0xFFF7B267.toInt(), 0xFFF4845F.toInt(), 0xFF7B6D8D.toInt()) else intArrayOf(0xFF9AD1D4.toInt(), 0xFFCCE6F4.toInt(), 0xFFF2EFEA.toInt())
        paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), sky, null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        paint.color = if (variant == 0) 0xFFFFE8C2.toInt() else 0xFFFFFFFF.toInt()
        canvas.drawCircle(if (variant == 0) 860f else 300f, 250f, 78f, paint)
        val ridges = if (variant == 0) listOf(0xFF5B4B6B.toInt(), 0xFF3F3550.toInt(), 0xFF2A2438.toInt()) else listOf(0xFF7FA6A0.toInt(), 0xFF4F7F78.toInt(), 0xFF2F5650.toInt())
        ridges.forEachIndexed { layer, color ->
            val baseline = 430f + layer * 110f
            val path = Path().apply {
                moveTo(0f, height.toFloat())
                lineTo(0f, baseline + 40f)
                var x = 0f
                var peak = 0
                while (x < width) {
                    val step = 150f + ((peak * 67 + layer * 41 + variant * 29) % 110)
                    val rise = 70f + ((peak * 53 + layer * 31 + variant * 17) % 120)
                    lineTo(x + step / 2f, baseline - rise)
                    lineTo(x + step, baseline + 30f)
                    x += step
                    peak++
                }
                lineTo(width.toFloat(), height.toFloat())
                close()
            }
            paint.color = color
            canvas.drawPath(path, paint)
        }
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
    }

    private companion object {
        val FIXTURE_IDS = 90_000L..90_999L
        val LANGUAGES = setOf("es", "en", "de", "fr", "pt")
        val SCENES = setOf("favorites", "contacts", "dialer", "call", "call-reply", "notes", "note", "vault", "expenses", "tools", "games", "qr", "settings")
        const val FIXTURE_CALL_ID = "store-fixture-call"
    }
}

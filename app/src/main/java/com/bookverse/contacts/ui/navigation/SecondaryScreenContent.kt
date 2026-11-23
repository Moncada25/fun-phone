package com.bookverse.contacts.ui.navigation

import androidx.compose.runtime.remember
import com.bookverse.contacts.data.backup.AppBackupManager
import com.bookverse.contacts.BuildConfig
import com.bookverse.contacts.utils.PhoneActions
import android.annotation.SuppressLint
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.App
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.notes.ChecklistItem
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.RecordingData
import com.bookverse.contacts.data.notes.ReminderData
import com.bookverse.contacts.domain.call.CallContactLookup
import com.bookverse.contacts.settings.Country
import com.bookverse.contacts.ui.screens.settings.AboutScreen
import com.bookverse.contacts.ui.screens.tools.HolidaysScreen
import com.bookverse.contacts.ui.screens.tools.MenstrualCalendarScreen
import com.bookverse.contacts.ui.screens.settings.PhoneStatusScreen
import com.bookverse.contacts.ui.screens.qr.QRGeneratorScreen
import com.bookverse.contacts.ui.screens.qr.QRScannerScreen
import com.bookverse.contacts.utils.SecondaryScreen
import com.bookverse.contacts.ui.screens.settings.SettingsScreen
import com.bookverse.contacts.ui.screens.tools.PomodoroScreen
import com.bookverse.contacts.ui.screens.tools.VoiceScreen
import com.bookverse.contacts.ui.screens.expenses.ExpenseManagerScreen
import com.bookverse.contacts.ui.screens.calls.InCallScreen
import com.bookverse.contacts.ui.screens.calls.IncomingCallScreen
import com.bookverse.contacts.ui.screens.contacts.ContactDetailsScreen
import com.bookverse.contacts.ui.screens.contacts.CreateContactScreen
import com.bookverse.contacts.ui.screens.contacts.CreateProfileScreen
import com.bookverse.contacts.ui.screens.dialer.DialerScreen
import com.bookverse.contacts.ui.screens.contacts.ContactRecycleBinScreen
import com.bookverse.contacts.ui.screens.contacts.DuplicateContactsScreen
import com.bookverse.contacts.ui.screens.contacts.FavoritesReorderScreen
import com.bookverse.contacts.ui.screens.contacts.ProfileScreen
import com.bookverse.contacts.ui.screens.contacts.UpdateContactScreen
import com.bookverse.contacts.ui.screens.contacts.UpdateProfileScreen
import com.bookverse.contacts.ui.screens.games.BreakoutScreen
import com.bookverse.contacts.ui.screens.games.FunBirdScreen
import com.bookverse.contacts.ui.screens.games.Game2048Screen
import com.bookverse.contacts.ui.screens.games.GamesScreen
import com.bookverse.contacts.ui.screens.games.HangmanScreen
import com.bookverse.contacts.ui.screens.games.MemoryGameScreen
import com.bookverse.contacts.ui.screens.games.MinesweeperScreen
import com.bookverse.contacts.ui.screens.tools.PaintScreen
import com.bookverse.contacts.ui.screens.games.PuzzleScreen
import com.bookverse.contacts.ui.screens.games.SnakeScreen
import com.bookverse.contacts.ui.screens.games.SudokuScreen
import com.bookverse.contacts.ui.screens.games.TicTacToeScreen
import com.bookverse.contacts.ui.screens.notes.ChecklistScreen
import com.bookverse.contacts.ui.screens.notes.FoldersNotesScreen
import com.bookverse.contacts.ui.screens.notes.NoteListScreen
import com.bookverse.contacts.ui.screens.notes.NoteScreen
import com.bookverse.contacts.ui.screens.notes.RecordingScreen
import com.bookverse.contacts.ui.screens.notes.RecycleBinScreen
import com.bookverse.contacts.ui.screens.notes.ReminderScreen
import com.bookverse.contacts.ui.screens.passwords.PasswordEditScreen
import com.bookverse.contacts.ui.screens.passwords.PasswordFoldersScreen
import com.bookverse.contacts.ui.screens.passwords.PasswordListScreen
import com.bookverse.contacts.ui.screens.passwords.PasswordRecycleBinScreen
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactRecycleBinViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactTagViewModel
import com.bookverse.contacts.ui.viewmodel.DuplicateContactsViewModel
import com.bookverse.contacts.ui.viewmodel.NavigationViewModel
import com.bookverse.contacts.ui.viewmodel.NotesViewModel
import com.bookverse.contacts.ui.viewmodel.ExpensesViewModel
import com.bookverse.contacts.ui.viewmodel.HolidaysViewModel
import com.bookverse.contacts.ui.viewmodel.PasswordsViewModel
import com.bookverse.contacts.ui.viewmodel.PomodoroViewModel
import com.bookverse.contacts.ui.viewmodel.QRHistoryViewModel
import com.bookverse.contacts.ui.theme.ThemeMode

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun SecondaryScreenContent(
    screen: SecondaryScreen?,
    settingsListState: LazyListState,
    navigationViewModel: NavigationViewModel,
    appSettingsViewModel: AppSettingsViewModel,
    themeMode: ThemeMode,
    contactsViewModel: ContactsViewModel,
    callContactLookup: CallContactLookup,
    holidaysViewModel: HolidaysViewModel,
    qrHistoryViewModel: QRHistoryViewModel,
    notesViewModel: NotesViewModel,
    passwordsViewModel: PasswordsViewModel,
    passwordBackupActions: com.bookverse.contacts.ui.screens.passwords.PasswordBackupActions,
    expensesViewModel: ExpensesViewModel,
    pomodoroViewModel: PomodoroViewModel,
    contactTagViewModel: ContactTagViewModel,
    selectedContact: Contact?,
    onSelectedContactChange: (Contact?) -> Unit,
    contactBeingEdited: Contact?,
    onContactBeingEditedChange: (Contact?) -> Unit,
    addContactPhone: String,
    onAddContactPhoneChange: (String) -> Unit,
    addContactCountry: Country?,
    onAddContactCountryChange: (Country?) -> Unit,
    dialerPhoneNumber: String,
    onDialerPhoneNumberChange: (String) -> Unit,
    editNoteData: Pair<Note?, List<ChecklistItem>>?,
    onEditNoteDataChange: (Pair<Note?, List<ChecklistItem>>?) -> Unit,
    editReminderData: Pair<Note?, ReminderData?>?,
    onEditReminderDataChange: (Pair<Note?, ReminderData?>?) -> Unit,
    editRecordingData: Pair<Note?, RecordingData?>?,
    onEditRecordingDataChange: (Pair<Note?, RecordingData?>?) -> Unit,
    pendingIncomingId: String?,
    pendingInCallId: String?,
    pendingInCallNumber: String?,
    selectedPasswordId: Long?,
    onSelectedPasswordIdChange: (Long?) -> Unit,
    onFontChange: (() -> Unit)?
) {
    val appContainer = (LocalContext.current.applicationContext as App).container
    when (screen) {
        SecondaryScreen.SETTINGS -> {
            val backupContext = LocalContext.current
            val appBackupManager = remember(backupContext) {
                AppBackupManager(backupContext, appContainer.gameStatsRepository, BuildConfig.VERSION_NAME)
            }
            SettingsScreen(
                onBackClick = { navigationViewModel.navigateBack() },
                appSettingsViewModel = appSettingsViewModel,
                appBackupManager = appBackupManager,
                onFontChange = onFontChange,
                contactsViewModel = contactsViewModel,
                notesViewModel = notesViewModel,
                listState = settingsListState,
                onNavigateToPhoneStatus = { navigationViewModel.openSecondary(SecondaryScreen.PHONE_STATUS) },
                onNavigateToProfile = { navigationViewModel.openSecondary(SecondaryScreen.PROFILE) },
                onNavigateToAbout = { navigationViewModel.openSecondary(SecondaryScreen.ABOUT) },
                onNavigateToTagManager = { navigationViewModel.openSecondary(SecondaryScreen.TAG_MANAGER) },
                onNavigateToDuplicateContacts = { navigationViewModel.openSecondary(SecondaryScreen.DUPLICATE_CONTACTS) },
                onNavigateToContactRecycleBin = { navigationViewModel.openSecondary(SecondaryScreen.CONTACT_RECYCLE_BIN) },
                onNavigateToToolsReorder = { navigationViewModel.openSecondary(SecondaryScreen.TOOLS_ORDER) }
            )
        }

        SecondaryScreen.DIALER -> DialerScreen(
            onBack = { navigationViewModel.navigateBack() },
            appSettingsViewModel = appSettingsViewModel,
            directory = contactsViewModel,
            phoneActions = PhoneActions,
            phoneNumber = dialerPhoneNumber,
            onPhoneNumberChange = { onDialerPhoneNumberChange(it) },
            onAddContactClick = { phone, country ->
                onAddContactPhoneChange(phone)
                onAddContactCountryChange(country)
                navigationViewModel.openSecondary(SecondaryScreen.ADD_CONTACT)
            }
        )

        SecondaryScreen.DETAIL -> selectedContact?.let { contact ->
            ContactDetailsScreen(
                phoneActions = PhoneActions,
                contact = contact,
                onBackClick = { navigationViewModel.navigateBack(); onSelectedContactChange(null) },
                appSettingsViewModel = appSettingsViewModel,
                contactsViewModel = contactsViewModel,
                contactTagViewModel = contactTagViewModel,
                onEditContact = { contactToEdit ->
                    navigationViewModel.openSecondary(SecondaryScreen.UPDATE_CONTACT)
                    onContactBeingEditedChange(contactToEdit)
                }
            )
        }

        SecondaryScreen.FAVORITES_ORDER -> FavoritesReorderScreen(
            appSettingsViewModel = appSettingsViewModel,
            contactsViewModel = contactsViewModel,
            onBackClick = { navigationViewModel.navigateBack() }
        )

        SecondaryScreen.TOOLS_ORDER -> com.bookverse.contacts.ui.screens.tools.ToolsReorderScreen(
            appSettingsViewModel = appSettingsViewModel,
            onBackClick = { navigationViewModel.navigateBack() },
            onPresetSelected = { preset -> appContainer.funnelTelemetry.toolsPresetSelected(preset) },
        )

        SecondaryScreen.GAMES -> GamesScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            onFunBirdClick = { navigationViewModel.openSecondary(SecondaryScreen.FUN_BIRD) },
            onTicTacToeClick = { navigationViewModel.openSecondary(SecondaryScreen.TIC_TAC_TOE) },
            onPuzzleClick = { navigationViewModel.openSecondary(SecondaryScreen.PUZZLE) },
            onHangmanClick = { navigationViewModel.openSecondary(SecondaryScreen.HANGMAN) },
            onSnakeClick = { navigationViewModel.openSecondary(SecondaryScreen.SNAKE) },
            on2048Click = { navigationViewModel.openSecondary(SecondaryScreen.GAME_2048) },
            onMemoryGameClick = { navigationViewModel.openSecondary(SecondaryScreen.MEMORY) },
            onBreakoutClick = { navigationViewModel.openSecondary(SecondaryScreen.BREAKOUT) },
            onMinesweeperClick = { navigationViewModel.openSecondary(SecondaryScreen.MINESWEEPER) },
            onSudokuClick = { navigationViewModel.openSecondary(SecondaryScreen.SUDOKU) },
            gameStatsRepository = appContainer.gameStatsRepository,
        )

        SecondaryScreen.TIC_TAC_TOE -> TicTacToeScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            gameStatsRepository = appContainer.gameStatsRepository,
        )

        SecondaryScreen.FUN_BIRD -> FunBirdScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            gameStatsRepository = appContainer.gameStatsRepository,
        )

        SecondaryScreen.PUZZLE -> PuzzleScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            gameStatsRepository = appContainer.gameStatsRepository,
        )

        SecondaryScreen.NOTES -> NoteListScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            onEditNoteClick = { note, checklist, noteType ->
                onEditNoteDataChange(note to checklist)
                navigationViewModel.openSecondary(if (noteType == "checklist") SecondaryScreen.CHECKLIST else SecondaryScreen.NOTE)
            },
            onEditReminderClick = { note, reminderData ->
                onEditReminderDataChange(note to reminderData)
                navigationViewModel.openSecondary(SecondaryScreen.REMINDER)
            },
            onEditRecordingClick = { note, recordingData ->
                onEditRecordingDataChange(note to recordingData)
                navigationViewModel.openSecondary(SecondaryScreen.RECORDING)
            },
            onCreateNoteClick = { noteType ->
                when (noteType) {
                    "note" -> {
                        onEditNoteDataChange(null to emptyList()); navigationViewModel.openSecondary(
                            SecondaryScreen.NOTE
                        )
                    }

                    "checklist" -> {
                        onEditNoteDataChange(null to emptyList()); navigationViewModel.openSecondary(
                            SecondaryScreen.CHECKLIST
                        )
                    }

                    "reminder" -> {
                        onEditReminderDataChange(null to null); navigationViewModel.openSecondary(
                            SecondaryScreen.REMINDER
                        )
                    }

                    "recording" -> {
                        onEditRecordingDataChange(null to null); navigationViewModel.openSecondary(
                            SecondaryScreen.RECORDING
                        )
                    }
                }
            },
            onNavigateToRecycleBin = {
                navigationViewModel.openSecondary(SecondaryScreen.RECYCLE_BIN)
            },
            onManageFolders = {
                navigationViewModel.openSecondary(SecondaryScreen.NOTE_FOLDERS)
            },
            onTransformToAudio = { noteContent ->
                navigationViewModel.openSecondaryWithVoiceText(
                    SecondaryScreen.VOICE_RECOGNITION,
                    noteContent
                )
            },
            appSettingsViewModel = appSettingsViewModel
        )

        SecondaryScreen.NOTE_FOLDERS -> FoldersNotesScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            appSettingsViewModel = appSettingsViewModel
        )

        SecondaryScreen.RECYCLE_BIN -> RecycleBinScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            notesViewModel = notesViewModel,
            appSettingsViewModel = appSettingsViewModel
        )

        SecondaryScreen.NOTE -> key(editNoteData?.first?.id) {
            NoteScreen(
                note = editNoteData?.first,
                onSave = { note ->
                    if (note.id == 0L) notesViewModel.addNoteWithChecklist(
                        note,
                        emptyList()
                    ) else notesViewModel.updateNoteWithChecklist(note, emptyList())
                    navigationViewModel.replaceSecondary(SecondaryScreen.NOTES)
                },
                onCancel = { navigationViewModel.navigateBack() },
                appSettingsViewModel = appSettingsViewModel
            )
        }

        SecondaryScreen.CHECKLIST -> key(editNoteData?.first?.id) {
            ChecklistScreen(
                note = editNoteData?.first,
                checklist = editNoteData?.second ?: emptyList(),
                onSave = { note, checklist ->
                    if (note.id == 0L) notesViewModel.addNoteWithChecklist(
                        note,
                        checklist
                    ) else notesViewModel.updateNoteWithChecklist(note, checklist)
                    navigationViewModel.replaceSecondary(SecondaryScreen.NOTES)
                },
                onCancel = { navigationViewModel.navigateBack() },
                appSettingsViewModel = appSettingsViewModel
            )
        }

        SecondaryScreen.REMINDER -> ReminderScreen(
            note = editReminderData?.first,
            reminderData = editReminderData?.second,
            onSave = { note, reminderData ->
                notesViewModel.addNoteWithReminder(note, reminderData)
                navigationViewModel.replaceSecondary(SecondaryScreen.NOTES)
            },
            onCancel = { navigationViewModel.navigateBack() },
            appSettingsViewModel = appSettingsViewModel
        )

        SecondaryScreen.RECORDING -> RecordingScreen(
            note = editRecordingData?.first,
            recordingData = editRecordingData?.second,
            onSave = { note, recordingData ->
                if (note.id == 0L) notesViewModel.addNoteWithRecording(
                    note,
                    recordingData
                ) else notesViewModel.updateNoteWithRecording(note, recordingData)
                navigationViewModel.replaceSecondary(SecondaryScreen.NOTES)
            },
            onCancel = { navigationViewModel.navigateBack() },
            appSettingsViewModel = appSettingsViewModel
        )

        SecondaryScreen.ABOUT -> AboutScreen(
            onBack = { navigationViewModel.navigateBack() },
        )

        SecondaryScreen.HANGMAN -> HangmanScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            appSettingsViewModel = appSettingsViewModel,
            gameStatsRepository = appContainer.gameStatsRepository,
        )

        SecondaryScreen.PAINT -> PaintScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            appSettingsViewModel = appSettingsViewModel
        )

        SecondaryScreen.SNAKE -> SnakeScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            appSettingsViewModel = appSettingsViewModel,
            viewModelFactory = appContainer.snakeViewModelFactory,
        )

        SecondaryScreen.GAME_2048 -> Game2048Screen(
            onBackClick = { navigationViewModel.navigateBack() },
            gameStatsRepository = appContainer.gameStatsRepository,
        )

        SecondaryScreen.MEMORY -> MemoryGameScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            appSettingsViewModel = appSettingsViewModel,
            gameStatsRepository = appContainer.gameStatsRepository,
        )

        SecondaryScreen.BREAKOUT -> BreakoutScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            appSettingsViewModel = appSettingsViewModel,
            viewModelFactory = appContainer.breakoutViewModelFactory,
        )

        SecondaryScreen.MINESWEEPER -> MinesweeperScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            gameStatsRepository = appContainer.gameStatsRepository,
        )

        SecondaryScreen.SUDOKU -> SudokuScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            gameStatsRepository = appContainer.gameStatsRepository,
        )

        SecondaryScreen.QR_SCANNER -> QRScannerScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            appSettingsViewModel = appSettingsViewModel,
            onCreateContact = { contact, successMessage, account ->
                contactsViewModel.createContact(
                    contact = contact,
                    successMessage = successMessage,
                    account = account,
                )
            },
            qrHistoryViewModel = qrHistoryViewModel,
            onContactSaved = {
                onSelectedContactChange(it); navigationViewModel.openSecondary(
                SecondaryScreen.DETAIL
            )
            }
        )

        SecondaryScreen.QR_GENERATOR -> QRGeneratorScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            qrHistoryViewModel = qrHistoryViewModel
        )

        SecondaryScreen.PROFILE -> ProfileScreen(
            phoneActions = PhoneActions,
            onBackClick = { navigationViewModel.navigateBack() },
            onEditProfile = { contact ->
                onContactBeingEditedChange(contact)
                navigationViewModel.openSecondary(SecondaryScreen.UPDATE_PROFILE)
            },
            onNavigateToCreateProfile = {
                navigationViewModel.replaceSecondary(SecondaryScreen.CREATE_PROFILE)
            },
            appSettingsViewModel = appSettingsViewModel,
            contactsViewModel = contactsViewModel
        )

        SecondaryScreen.ADD_CONTACT -> CreateContactScreen(
            onContactSaved = { contactCreated ->
                val resolvedContact = when {
                    contactCreated.id.isNotBlank() -> contactsViewModel.findContactById(
                        contactCreated.id
                    )

                    else -> contactsViewModel.findContactByPhones(contactCreated.phones)
                } ?: contactCreated
                onSelectedContactChange(resolvedContact)
                navigationViewModel.replaceSecondary(SecondaryScreen.DETAIL)
                onContactBeingEditedChange(null)
                onAddContactPhoneChange("")
            },
            onBackClick = { navigationViewModel.navigateBack() },
            phoneNumber = addContactPhone,
            country = addContactCountry,
            appSettingsViewModel = appSettingsViewModel,
            contactsViewModel = contactsViewModel,
            contactTagViewModel = contactTagViewModel
        )

        SecondaryScreen.UPDATE_CONTACT -> contactBeingEdited?.let { contact ->
            UpdateContactScreen(
                onContactUpdated = { contactUpdated ->
                    val resolved = when {
                        contactUpdated.id.isNotBlank() -> contactsViewModel.findContactById(
                            contactUpdated.id
                        )

                        else -> contactsViewModel.findContactByPhones(contactUpdated.phones)
                    } ?: contactUpdated
                    onSelectedContactChange(resolved)
                    navigationViewModel.replaceSecondary(SecondaryScreen.DETAIL)
                    onContactBeingEditedChange(null)
                },
                onBackClick = {
                    onSelectedContactChange(contactBeingEdited)
                    navigationViewModel.navigateBack()
                    onContactBeingEditedChange(null)
                },
                contactToEdit = contact,
                appSettingsViewModel = appSettingsViewModel,
                contactsViewModel = contactsViewModel,
                contactTagViewModel = contactTagViewModel
            )
        }

        SecondaryScreen.UPDATE_PROFILE -> contactBeingEdited?.let { contact ->
            UpdateProfileScreen(
                onProfileUpdated = {
                    onContactBeingEditedChange(null)
                    navigationViewModel.replaceSecondary(SecondaryScreen.PROFILE)
                },
                onBackClick = {
                    onContactBeingEditedChange(null)
                    navigationViewModel.navigateBack()
                },
                profileToEdit = contact,
                appSettingsViewModel = appSettingsViewModel,
                contactsViewModel = contactsViewModel
            )
        }

        SecondaryScreen.CREATE_PROFILE -> CreateProfileScreen(
            onProfileCreated = { navigationViewModel.replaceSecondary(SecondaryScreen.PROFILE) },
            onBackClick = { navigationViewModel.navigateBack() },
            appSettingsViewModel = appSettingsViewModel,
            contactsViewModel = contactsViewModel
        )

        SecondaryScreen.HOLIDAYS -> HolidaysScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            contactsFlow = contactsViewModel.contacts,
            profileFlow = contactsViewModel.profile,
            viewModel = holidaysViewModel,
        )

        SecondaryScreen.MENSTRUAL_CALENDAR -> MenstrualCalendarScreen(
            onBackClick = { navigationViewModel.navigateBack() },
        )

        SecondaryScreen.VOICE_RECOGNITION -> VoiceScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            initialTextToSpeak = navigationViewModel.uiState.value.voiceTextToSpeak,
            showSnackbar = navigationViewModel.uiState.value.voiceTextToSpeak != null
        )

        SecondaryScreen.PHONE_STATUS -> PhoneStatusScreen(
            onBackClick = { navigationViewModel.navigateBack() },
        )

        SecondaryScreen.POMODORO -> PomodoroScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            pomodoroViewModel = pomodoroViewModel
        )

        SecondaryScreen.TAG_MANAGER -> com.bookverse.contacts.ui.screens.contacts.TagManagerScreen(
            contactTagViewModel = contactTagViewModel,
            appSettingsViewModel = appSettingsViewModel,
            onBack = { navigationViewModel.navigateBack() },
        )

        SecondaryScreen.INCOMING_CALL -> IncomingCallScreen(
            callId = pendingIncomingId,
            onAnswer = { navigationViewModel.openSecondary(SecondaryScreen.IN_CALL) },
            onDismiss = { navigationViewModel.closeSecondary() },
            onAutoMissed = { navigationViewModel.closeSecondary() },
            callContactLookup = callContactLookup,
            themeMode = themeMode,
            appSettingsViewModel = appSettingsViewModel
        )

        SecondaryScreen.IN_CALL -> InCallScreen(
            callId = pendingInCallId,
            number = pendingInCallNumber,
            onAddCall = { navigationViewModel.openSecondary(SecondaryScreen.DIALER) },
            onEnd = { navigationViewModel.closeSecondary() },
            callContactLookup = callContactLookup,
            appSettingsViewModel = appSettingsViewModel
        )

        SecondaryScreen.PASSWORD_LIST -> PasswordListScreen(
            backupActions = passwordBackupActions,
            onBackClick = { navigationViewModel.navigateBack() },
            onEdit = { cred ->
                onSelectedPasswordIdChange(cred.id)
                navigationViewModel.openSecondary(SecondaryScreen.PASSWORDS_NEW)
            },
            onCreateNew = { navigationViewModel.openSecondary(SecondaryScreen.PASSWORDS_NEW) },
            onManageFolders = { navigationViewModel.openSecondary(SecondaryScreen.PASSWORD_FOLDERS) },
            onNavigateToRecycleBin = {
                navigationViewModel.openSecondary(SecondaryScreen.PASSWORD_RECYCLE_BIN)
            },
            passwordsViewModel = passwordsViewModel,
            appSettingsViewModel = appSettingsViewModel
        )

        SecondaryScreen.PASSWORD_FOLDERS -> PasswordFoldersScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            passwordsViewModel = passwordsViewModel,
            appSettingsViewModel = appSettingsViewModel,
        )

        SecondaryScreen.PASSWORD_RECYCLE_BIN -> PasswordRecycleBinScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            passwordsViewModel = passwordsViewModel,
        )

        SecondaryScreen.PASSWORDS_NEW -> PasswordEditScreen(
            id = selectedPasswordId,
            passwordsViewModel = passwordsViewModel,
            appSettingsViewModel = appSettingsViewModel,
            onBackClick = {
                navigationViewModel.navigateBack()
                onSelectedPasswordIdChange(null)
            },
            onSaved = {
                navigationViewModel.replaceSecondary(SecondaryScreen.PASSWORD_LIST)
                onSelectedPasswordIdChange(null)
            }
        )

        SecondaryScreen.EXPENSES -> ExpenseManagerScreen(
            onBackClick = { navigationViewModel.navigateBack() },
            expensesViewModel = expensesViewModel,
            appSettingsViewModel = appSettingsViewModel
        )

        SecondaryScreen.DUPLICATE_CONTACTS -> {
            val context = LocalContext.current
            val duplicateContactsViewModel: DuplicateContactsViewModel = viewModel(
                factory = (context.applicationContext as App)
                    .container
                    .duplicateContactsViewModelFactory,
            )
            val contacts = contactsViewModel.contacts.collectAsStateWithLifecycle().value

            DuplicateContactsScreen(
                contacts = contacts,
                duplicateContactsViewModel = duplicateContactsViewModel,
                onBack = { navigationViewModel.navigateBack() },
                onContactsMerged = { contactsViewModel.loadContacts() },
            )
        }

        SecondaryScreen.CONTACT_RECYCLE_BIN -> {
            val contactRecycleBinViewModel: ContactRecycleBinViewModel = viewModel(
                factory = appContainer.contactRecycleBinViewModelFactory,
            )

            ContactRecycleBinScreen(
                onBackClick = { navigationViewModel.navigateBack() },
                recycleBinViewModel = contactRecycleBinViewModel,
                onContactRestored = { contactsViewModel.loadContacts() },
            )
        }

        null -> {}
    }
}

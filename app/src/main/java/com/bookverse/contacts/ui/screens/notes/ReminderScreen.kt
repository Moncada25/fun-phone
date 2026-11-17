package com.bookverse.contacts.ui.screens.notes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.R
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.ReminderData
import com.bookverse.contacts.data.notes.ReminderRepeat
import com.bookverse.contacts.data.notes.effectiveRepeat
import com.bookverse.contacts.data.notes.nextOccurrence
import com.bookverse.contacts.data.notes.repeats
import com.bookverse.contacts.integration.NotesViewModelFactory
import com.bookverse.contacts.data.notes.ReminderType
import com.bookverse.contacts.service.NotificationService
import com.bookverse.contacts.ui.components.ActionPillScope
import com.bookverse.contacts.ui.components.DatePickerDialog
import com.bookverse.contacts.ui.components.GroupedCardList
import com.bookverse.contacts.ui.components.InfoGroupItem
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.NoteTitle
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SelectBottomSheet
import com.bookverse.contacts.ui.components.TextField
import com.bookverse.contacts.ui.components.TimePickerDialog
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.viewmodel.NotesViewModel
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    note: Note?,
    reminderData: ReminderData? = null,
    onSave: (Note, ReminderData) -> Unit,
    onCancel: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel = viewModel()
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val accentColor = MaterialTheme.colorScheme.primary

    var stableNote by remember { mutableStateOf(note) }
    if (note != null && note != stableNote) {
        stableNote = note
    }

    var stableReminderData by remember { mutableStateOf(reminderData) }
    if (reminderData != null && reminderData != stableReminderData) {
        stableReminderData = reminderData
    }

    val isNewReminder = stableNote?.id == 0L || stableNote == null

    val isReminderExpired = if (!isNewReminder && stableReminderData != null) {
        stableReminderData!!.nextOccurrence(System.currentTimeMillis()) == null
    } else {
        false
    }

    val topBarTitle = when {
        isNewReminder -> stringResource(R.string.create_reminder)
        isReminderExpired -> stringResource(R.string.reminder_expired)
        else -> stringResource(R.string.reminder_in_progress)
    }

    var title by remember { mutableStateOf(note?.title ?: "") }
    var message by remember { mutableStateOf(reminderData?.message ?: "") }

    var selectedDate by remember {
        mutableLongStateOf(
            reminderData?.date ?: System.currentTimeMillis()
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat(
        "EEEE, dd MMMM yyyy - hh:mm a",
        LocaleController.effectiveLocale(LocalContext.current),
    )

    var reminderType by remember {
        mutableStateOf(reminderData?.type ?: ReminderType.PERIODIC)
    }

    var repeatMode by remember {
        mutableStateOf(reminderData?.repeatMode ?: ReminderRepeat.NONE)
    }

    val intervalOptions = listOf(60, 600, 1800, 3600, -1)
    val intervalLabels = listOf(
        "1 min",
        "10 min",
        "30 min",
        "60 min",
        stringResource(R.string.custom_interval_seconds)
    )
    var selectedIntervalIndex by remember {
        mutableIntStateOf(
            if (reminderData != null) {
                val index = intervalOptions.indexOf(reminderData.interval)
                if (index >= 0) index else 4
            } else 0
        )
    }
    var customInterval by remember {
        mutableStateOf(
            if (reminderData != null && !intervalOptions.contains(reminderData.interval)) {
                reminderData.interval.toString()
            } else "60"
        )
    }

    val advanceNotificationOptions = listOf(0, 5, 10, 15, 30, 60, -1)
    val advanceNotificationLabels = listOf(
        stringResource(R.string.no_advance_notification),
        stringResource(R.string.advance_5_min),
        stringResource(R.string.advance_10_min),
        stringResource(R.string.advance_15_min),
        stringResource(R.string.advance_30_min),
        stringResource(R.string.advance_1_hour),
        stringResource(R.string.advance_custom)
    )
    var selectedAdvanceIndex by remember {
        mutableIntStateOf(
            if (reminderData != null) {
                val index =
                    advanceNotificationOptions.indexOf(reminderData.advanceNotificationMinutes)
                if (index >= 0) index else 6
            } else 2
        )
    }
    var customAdvanceMinutes by remember {
        mutableStateOf(
            if (reminderData != null && !advanceNotificationOptions.contains(reminderData.advanceNotificationMinutes)) {
                reminderData.advanceNotificationMinutes.toString()
            } else "10"
        )
    }

    val protectedDraft = if (note?.isLocked == true) com.bookverse.contacts.ui.auth.encryptedDraft("reminder-draft-${note.id}", appSettingsViewModel) else null
    com.bookverse.contacts.ui.auth.BindProtectedDraft(
        draft = protectedDraft,
        snapshot = { mapOf("title" to title, "message" to message, "date" to selectedDate.toString(), "type" to reminderType.name, "intervalIndex" to selectedIntervalIndex.toString(), "interval" to customInterval, "advanceIndex" to selectedAdvanceIndex.toString(), "advance" to customAdvanceMinutes, "repeat" to repeatMode.name) },
        restore = { fields ->
            title = fields["title"].orEmpty()
            message = fields["message"].orEmpty()
            selectedDate = fields["date"]?.toLongOrNull() ?: selectedDate
            reminderType = ReminderType.entries.firstOrNull { it.name == fields["type"] } ?: reminderType
            selectedIntervalIndex = (fields["intervalIndex"]?.toIntOrNull() ?: 0).coerceIn(intervalOptions.indices)
            customInterval = fields["interval"].orEmpty()
            selectedAdvanceIndex = (fields["advanceIndex"]?.toIntOrNull() ?: 0).coerceIn(advanceNotificationOptions.indices)
            customAdvanceMinutes = fields["advance"].orEmpty()
            repeatMode = ReminderRepeat.entries.firstOrNull { it.name == fields["repeat"] } ?: repeatMode
        },
    )

    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val resources = LocalResources.current
    val notesViewModel: NotesViewModel = viewModel(
        factory = NotesViewModelFactory(context)
    )
    val noteFolders by notesViewModel.noteFolders.collectAsStateWithLifecycle()
    var selectedFolderId by rememberSaveable(stableNote?.id) { mutableStateOf(stableNote?.folderId) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var hasSaved by remember { mutableStateOf(false) }
    var showValidationDialog by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var showExactAlarmPermissionDialog by remember { mutableStateOf(false) }
    var showSilentChannelDialog by remember { mutableStateOf(false) }
    var permissionSettingsResult by remember { mutableIntStateOf(0) }

    val permissionSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        permissionSettingsResult++
    }

    fun saveReminder() {
        hasSaved = true
        protectedDraft?.clear()
        val interval = if (reminderType == ReminderType.PERIODIC) {
            if (selectedIntervalIndex == 4) {
                val customValue = customInterval.toIntOrNull() ?: 60
                maxOf(customValue, 60)
            } else {
                intervalOptions[selectedIntervalIndex]
            }
        } else {
            0
        }

        val advanceNotification = if (reminderType == ReminderType.SINGLE_EVENT) {
            if (selectedAdvanceIndex == 6) {
                val customValue = customAdvanceMinutes.toIntOrNull() ?: 10
                maxOf(customValue, 0)
            } else {
                advanceNotificationOptions[selectedAdvanceIndex]
            }
        } else {
            0
        }

        val now = System.currentTimeMillis()
        onSave(
            Note(
                id = stableNote?.id ?: 0,
                title = title,
                content = message,
                isChecklist = false,
                isReminder = true,
                isLocked = stableNote?.isLocked ?: false,
                folderId = selectedFolderId,
                createdAt = stableNote?.createdAt?.takeIf { it > 0L } ?: now,
                updatedAt = now
            ),
            ReminderData(
                id = stableReminderData?.id ?: 0,
                noteId = stableNote?.id ?: 0,
                date = selectedDate,
                interval = interval,
                message = message,
                creationDate = stableReminderData?.creationDate ?: now,
                type = reminderType,
                advanceNotificationMinutes = advanceNotification,
                repeatMode = if (reminderType == ReminderType.SINGLE_EVENT) repeatMode else ReminderRepeat.NONE,
            )
        )
    }

    fun selectedAdvanceMinutes(): Int {
        return if (selectedAdvanceIndex == 6) {
            maxOf(customAdvanceMinutes.toIntOrNull() ?: 10, 0)
        } else {
            advanceNotificationOptions[selectedAdvanceIndex]
        }
    }

    fun relevantReminderChannelIds(): List<String> {
        return when (reminderType) {
            ReminderType.PERIODIC -> listOf(NotificationService.PERIODIC_CHANNEL_ID)
            ReminderType.SINGLE_EVENT -> buildList {
                add(NotificationService.ALARM_CHANNEL_ID)
                if (selectedAdvanceMinutes() > 0) {
                    add(NotificationService.ADVANCE_CHANNEL_ID)
                }
            }
        }
    }

    fun currentPermissionAction(
        notificationPermissionGranted: Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    ): ReminderPermissionAction {
        NotificationService.createNotificationChannels(context)
        return resolveReminderPermissionAction(
            requiresNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            notificationPermissionGranted = notificationPermissionGranted,
            notificationChannelsEnabled = relevantReminderChannelIds().all { channelId ->
                NotificationService.isReminderChannelEnabled(context, channelId)
            },
            requiresExactAlarmPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            exactAlarmPermissionGranted = NotificationService.canScheduleExactAlarms(context)
        )
    }

    fun applyPermissionAction(action: ReminderPermissionAction) {
        when (action) {
            ReminderPermissionAction.REQUEST_NOTIFICATION_PERMISSION -> {
                showNotificationPermissionDialog = true
            }

            ReminderPermissionAction.OPEN_NOTIFICATION_SETTINGS -> {
                showNotificationSettingsDialog = true
            }

            ReminderPermissionAction.REQUEST_EXACT_ALARM_PERMISSION -> {
                showExactAlarmPermissionDialog = true
            }

            ReminderPermissionAction.SAVE -> {
                if (
                    relevantReminderChannelIds().all { channelId ->
                        NotificationService.isReminderChannelAudible(context, channelId)
                    }
                ) {
                    saveReminder()
                } else {
                    showSilentChannelDialog = true
                }
            }
        }
    }

    LaunchedEffect(permissionSettingsResult) {
        if (permissionSettingsResult > 0) {
            applyPermissionAction(currentPermissionAction())
        }
    }

    val requestNotificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showNotificationPermissionDialog = false
            applyPermissionAction(
                currentPermissionAction(notificationPermissionGranted = true)
            )
        }
    }

    fun handleSave() {
        if (hasSaved) return

        if (isNewReminder) {
            if (title.isEmpty()) {
                validationMessage =
                    resources.getString(R.string.reminder_validation_empty_fields_message)
                showValidationDialog = true
                return
            }

            if (selectedDate <= System.currentTimeMillis()) {
                validationMessage =
                    resources.getString(R.string.reminder_validation_past_date_message)
                showValidationDialog = true
                return
            }

            applyPermissionAction(currentPermissionAction())
        } else {
            onCancel()
        }
    }

    LaunchedEffect(Unit) {
        if (isNewReminder) {
            focusRequester.requestFocus()
        }
    }

    BackHandler {
        onCancel()
    }

    val topBarActions: (@Composable ActionPillScope.() -> Unit)? =
        if (noteFolders.isNotEmpty() || isNewReminder) {
            {
                if (noteFolders.isNotEmpty() && isNewReminder) {
                    IconAction(
                        imageVector = Icons.Default.Folder,
                        contentDescription = stringResource(R.string.note_folder_action_description),
                        onClick = { showFolderDialog = true }
                    )
                }
                if (isNewReminder) {
                    IconAction(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.save),
                        onClick = { handleSave() }
                    )
                }
            }
        } else null

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = topBarTitle,
                onBackClick = { onCancel() },
                appSettingsViewModel = appSettingsViewModel,
                actions = topBarActions
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
                .padding(horizontal = 18.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                if (isNewReminder) {
                    NoteTitle(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.focusRequester(focusRequester),
                        accentColor = accentColor
                    )
                } else {
                    NoteTitle(
                        value = title,
                        accentColor = accentColor,
                        editable = false
                    )
                }
            }

            if (!isNewReminder) {
                val typeDescription =
                    if (stableReminderData?.type == ReminderType.SINGLE_EVENT) stringResource(R.string.one_event_type) else stringResource(
                        R.string.multiple_event_type
                    )
                val dateDescription = dateFormatter.format(Date(selectedDate))
                val intervalText =
                    if (stableReminderData != null && stableReminderData!!.type == ReminderType.PERIODIC) {
                        when (stableReminderData!!.interval) {
                            60 -> "1 min"
                            600 -> "10 min"
                            1800 -> "30 min"
                            3600 -> "60 min"
                            else -> "${stableReminderData!!.interval} sec"
                        }
                    } else null
                val advanceText =
                    if (stableReminderData != null && stableReminderData!!.type == ReminderType.SINGLE_EVENT) {
                        when (stableReminderData!!.advanceNotificationMinutes) {
                            0 -> stringResource(R.string.no_advance_notification)
                            5 -> "5 min antes"
                            10 -> "10 min antes"
                            15 -> "15 min antes"
                            30 -> "30 min antes"
                            60 -> "1 hora antes"
                            else -> "${stableReminderData!!.advanceNotificationMinutes} min antes"
                        }
                    } else null
                val creationText = stableReminderData?.let {
                    dateFormatter.format(Date(it.creationDate))
                }
                val detailRepeat = stableReminderData?.effectiveRepeat ?: ReminderRepeat.NONE
                val repeatText = if (stableReminderData?.type == ReminderType.SINGLE_EVENT) {
                    reminderRepeatLabel(detailRepeat)
                } else null
                val nextOccurrenceText = stableReminderData
                    ?.takeIf { it.repeats }
                    ?.nextOccurrence(System.currentTimeMillis())
                    ?.let { dateFormatter.format(Date(it)) }
                val items = mutableListOf<@Composable () -> Unit>()
                items += {
                    InfoGroupItem(
                        icon = Icons.Default.Repeat,
                        title = stringResource(R.string.reminder_type),
                        description = typeDescription,
                    )
                }
                items += {
                    InfoGroupItem(
                        icon = Icons.Default.DateRange,
                        title = stringResource(
                            if (detailRepeat == ReminderRepeat.NONE) R.string.expiration_date
                            else R.string.reminder_first_occurrence
                        ),
                        description = dateDescription,
                    )
                }
                if (repeatText != null) {
                    items += {
                        InfoGroupItem(
                            icon = Icons.Default.EventRepeat,
                            title = stringResource(R.string.reminder_repeat),
                            description = repeatText,
                        )
                    }
                }
                if (nextOccurrenceText != null) {
                    items += {
                        InfoGroupItem(
                            icon = Icons.Default.Update,
                            title = stringResource(R.string.reminder_next_occurrence),
                            description = nextOccurrenceText,
                        )
                    }
                }
                if (intervalText != null) {
                    items += {
                        InfoGroupItem(
                            icon = Icons.Default.AccessTime,
                            title = stringResource(R.string.interval),
                            description = intervalText,
                        )
                    }
                }
                if (advanceText != null) {
                    items += {
                        InfoGroupItem(
                            icon = Icons.Default.Notifications,
                            title = stringResource(R.string.with_notification),
                            description = advanceText,
                        )
                    }
                }
                if (message.isNotEmpty() && stableReminderData != null) {
                    items += {
                        InfoGroupItem(
                            icon = Icons.AutoMirrored.Filled.Message,
                            title = stringResource(R.string.message),
                            description = message,
                        )
                    }
                }

                if (creationText != null) {
                    items += {
                        InfoGroupItem(
                            icon = Icons.Default.History,
                            title = stringResource(R.string.creation_date),
                            description = creationText,
                        )
                    }
                }
                GroupedCardList(items = items)
                Spacer(Modifier.height(16.dp))
            } else {
                Text(
                    text = stringResource(R.string.reminder_type),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        onClick = { reminderType = ReminderType.PERIODIC },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (reminderType == ReminderType.PERIODIC)
                                accentColor.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.multiple_event_type),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.multiple_event_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Card(
                        onClick = { reminderType = ReminderType.SINGLE_EVENT },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (reminderType == ReminderType.SINGLE_EVENT)
                                accentColor.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.one_event_type),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.one_event_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.expiration_date),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = dateFormatter.format(Date(selectedDate)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = textColor
                        ),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.select_date)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.select_date))
                    }

                    Button(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = textColor
                        ),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = stringResource(R.string.select_time)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.select_time))
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (reminderType == ReminderType.PERIODIC) {
                    Text(
                        text = stringResource(R.string.interval),
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.height(8.dp))

                    Column(Modifier.selectableGroup()) {
                        intervalOptions.forEachIndexed { index, _ ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = selectedIntervalIndex == index,
                                        onClick = { selectedIntervalIndex = index },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedIntervalIndex == index,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = accentColor,
                                        unselectedColor = textColor
                                    )
                                )
                                Text(
                                    text = intervalLabels[index],
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp),
                                    color = textColor
                                )
                            }
                        }

                        if (selectedIntervalIndex == 4) {
                            TextField(
                                value = customInterval,
                                onValueChange = { customInterval = it },
                                label = stringResource(R.string.custom_interval_seconds),
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .imePadding(),
                            )
                            Text(
                                text = stringResource(R.string.minimum_value),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.reminder_repeat),
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.height(8.dp))

                    Column(Modifier.selectableGroup()) {
                        ReminderRepeat.entries.forEach { mode ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = repeatMode == mode,
                                        onClick = { repeatMode = mode },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = repeatMode == mode,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = accentColor,
                                        unselectedColor = textColor
                                    )
                                )
                                Text(
                                    text = reminderRepeatLabel(mode),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp),
                                    color = textColor
                                )
                            }
                        }
                    }

                    if (repeatMode != ReminderRepeat.NONE) {
                        Text(
                            text = stringResource(R.string.reminder_repeat_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.with_notification),
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.height(8.dp))

                    Column(Modifier.selectableGroup()) {
                        advanceNotificationOptions.forEachIndexed { index, _ ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = selectedAdvanceIndex == index,
                                        onClick = { selectedAdvanceIndex = index },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedAdvanceIndex == index,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = accentColor,
                                        unselectedColor = textColor
                                    )
                                )
                                Text(
                                    text = advanceNotificationLabels[index],
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp),
                                    color = textColor
                                )
                            }
                        }

                        if (selectedAdvanceIndex == 6) {
                            TextField(
                                value = customAdvanceMinutes,
                                onValueChange = { customAdvanceMinutes = it },
                                label = stringResource(R.string.advance_custom_minutes),
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .imePadding(),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                TextField(
                    value = message,
                    onValueChange = { message = it },
                    label = stringResource(R.string.notification_message),
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    singleLine = false,
                )

                Spacer(Modifier.height(16.dp))
            }
        }

        if (showDatePicker && isNewReminder) {
            DatePickerDialog(
                selectedDate = selectedDate,
                onDateSelected = { newDateMillis ->
                    selectedDate = newDateMillis
                },
                onDismiss = { showDatePicker = false },
                title = stringResource(R.string.select_expiration_date)
            )
        }

        if (showTimePicker && isNewReminder) {
            TimePickerDialog(
                selectedDate = selectedDate,
                onTimeSelected = { newTimeMillis ->
                    selectedDate = newTimeMillis
                },
                onDismiss = { showTimePicker = false },
                title = stringResource(R.string.select_expiration_time)
            )
        }

        if (showFolderDialog && noteFolders.isNotEmpty()) {
            val folderOptions = listOf(
                FolderSelectionOption(null, stringResource(R.string.note_move_to_folder_none))
            ) + noteFolders.map { FolderSelectionOption(it.id, it.name) }
            val selectedOption =
                folderOptions.firstOrNull { it.id == selectedFolderId } ?: folderOptions.first()
            SelectBottomSheet(
                onDismiss = { showFolderDialog = false },
                title = stringResource(R.string.note_folder_picker_title),
                options = folderOptions,
                selectedOption = selectedOption,
                onSelect = { option ->
                    selectedFolderId = option.id
                    showFolderDialog = false
                },
                optionKey = { it.id ?: -1L },
                optionLabel = { it.label },
            )
        }

        if (showValidationDialog) {
            MessageDialog(
                title = stringResource(R.string.reminder_validation_error_title),
                message1 = validationMessage,
                buttonText = stringResource(R.string.understood),
                onButtonClick = { showValidationDialog = false },
                onDismiss = { showValidationDialog = false },
            )
        }

        if (showNotificationPermissionDialog) {
            MessageDialog(
                title = stringResource(R.string.reminder_notification_permission_title),
                message1 = stringResource(R.string.reminder_notification_permission_message),
                buttonText = stringResource(R.string.grant_permission),
                onButtonClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onDismiss = { showNotificationPermissionDialog = false },
            )
        }

        if (showNotificationSettingsDialog) {
            MessageDialog(
                title = stringResource(R.string.reminder_notification_settings_title),
                message1 = stringResource(R.string.reminder_notification_settings_message),
                buttonText = stringResource(R.string.open_settings),
                onButtonClick = {
                    showNotificationSettingsDialog = false
                    permissionSettingsLauncher.launch(
                        NotificationService.createNotificationSettingsIntent(context)
                    )
                },
                onDismiss = { showNotificationSettingsDialog = false },
            )
        }

        if (showExactAlarmPermissionDialog) {
            MessageDialog(
                title = stringResource(R.string.reminder_exact_alarm_permission_title),
                message1 = stringResource(R.string.reminder_exact_alarm_permission_message),
                buttonText = stringResource(R.string.action_continue),
                onButtonClick = {
                    showExactAlarmPermissionDialog = false
                    NotificationService.createAlarmPermissionIntent(context)?.let { intent ->
                        permissionSettingsLauncher.launch(intent)
                    }
                },
                secondaryButtonText = stringResource(R.string.reminder_save_without_alarm),
                onSecondaryButtonClick = {
                    showExactAlarmPermissionDialog = false
                    saveReminder()
                },
                onDismiss = { showExactAlarmPermissionDialog = false },
            )
        }

        if (showSilentChannelDialog) {
            MessageDialog(
                title = stringResource(R.string.reminder_silent_channel_title),
                message1 = stringResource(R.string.reminder_silent_channel_message),
                buttonText = stringResource(R.string.save),
                onButtonClick = {
                    showSilentChannelDialog = false
                    saveReminder()
                },
                secondaryButtonText = stringResource(R.string.open_settings),
                onSecondaryButtonClick = {
                    showSilentChannelDialog = false
                    permissionSettingsLauncher.launch(
                        NotificationService.createNotificationSettingsIntent(context),
                    )
                },
                onDismiss = { showSilentChannelDialog = false },
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

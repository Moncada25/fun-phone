package com.bookverse.contacts.ui.screens.notes

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.data.notes.RecordingData
import com.bookverse.contacts.integration.NotesViewModelFactory
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.NoteTitle
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SelectBottomSheet
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.NotesViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    note: Note?,
    recordingData: RecordingData?,
    onSave: (Note, RecordingData?) -> Unit,
    onCancel: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val accentColor = MaterialTheme.colorScheme.primary
    val notesViewModel: NotesViewModel = viewModel(
        factory = NotesViewModelFactory(context)
    )
    val noteFolders by notesViewModel.noteFolders.collectAsStateWithLifecycle()
    var selectedFolderId by rememberSaveable(note?.id) { mutableStateOf(note?.folderId) }
    var showFolderDialog by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf(note?.title ?: "") }
    val originalTitle = remember { note?.title ?: "" }
    val originalRecordingData = remember { recordingData }
    val focusRequester = remember { FocusRequester() }

    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableLongStateOf(recordingData?.duration ?: 0L) }
    var currentRecordingData by remember { mutableStateOf(recordingData) }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }

    var showValidationDialog by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val recordingTimer = remember { mutableLongStateOf(0L) }
    var currentDuration by remember { mutableLongStateOf(0L) }

    val protectedDraft = if (note?.isLocked == true) com.bookverse.contacts.ui.auth.encryptedDraft("recording-draft-${note.id}", appSettingsViewModel) else null
    val recordingSnapshot: () -> Map<String, String> = {
        val recording = currentRecordingData
        mapOf("title" to title, "path" to recording?.filePath.orEmpty(), "id" to recording?.id.toString(), "noteId" to recording?.noteId.toString(), "duration" to recordingDuration.toString(), "createdAt" to recording?.createdAt.toString(), "transcription" to recording?.transcription.orEmpty(), "transcribed" to recording?.isTranscribed.toString())
    }
    com.bookverse.contacts.ui.auth.BindProtectedDraft(
        draft = protectedDraft,
        snapshot = recordingSnapshot,
        restore = { fields ->
            title = fields["title"].orEmpty()
            recordingDuration = fields["duration"]?.toLongOrNull() ?: 0L
            currentRecordingData = fields["path"]?.takeIf { it.isNotBlank() }?.let { path ->
                RecordingData(
                    id = fields["id"]?.toLongOrNull() ?: 0,
                    noteId = fields["noteId"]?.toLongOrNull() ?: note?.id ?: 0,
                    filePath = path,
                    duration = recordingDuration,
                    createdAt = fields["createdAt"]?.toLongOrNull() ?: System.currentTimeMillis(),
                    transcription = fields["transcription"].orEmpty(),
                    isTranscribed = fields["transcribed"].toBoolean(),
                )
            }
            audioFile = currentRecordingData?.let { File(it.filePath) }
        },
    )

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                currentDuration = System.currentTimeMillis() - recordingTimer.longValue
                kotlinx.coroutines.delay(100.milliseconds)
            }
        }
    }

    fun hasChanges(): Boolean {
        if (note == null) return title.trim().isNotEmpty() || currentRecordingData != null
        if (title.trim() != originalTitle) return true
        if (currentRecordingData?.filePath != originalRecordingData?.filePath) return true
        return false
    }

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun createAudioFile(): File {
        val recordingsDir = File(context.filesDir, "recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        return File(recordingsDir, "recording_${System.currentTimeMillis()}.m4a")
    }

    fun stopPlayingLogic() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        isPlaying = false
        isPaused = false
    }

    fun startRecordingLogic(): Boolean {
        return try {
            stopPlayingLogic()

            audioFile = createAudioFile()
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            recordingTimer.longValue = System.currentTimeMillis()

            true
        } catch (_: IOException) {
            DiagnosticLog.error(DiagnosticEvent.RECORDING_START_FAILED)
            false
        }
    }

    fun stopRecordingLogic(): Boolean {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            recordingDuration = System.currentTimeMillis() - recordingTimer.longValue

            audioFile?.let { file ->
                currentRecordingData = RecordingData(
                    id = currentRecordingData?.id ?: 0,
                    noteId = note?.id ?: 0,
                    filePath = file.absolutePath,
                    duration = recordingDuration,
                    transcription = "",
                    isTranscribed = false
                )
            }
            true
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.RECORDING_STOP_FAILED)
            false
        }
    }

    fun startPlayingLogic() {
        val filePath = currentRecordingData?.filePath ?: return

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    isPlaying = false
                    isPaused = false
                }
                start()
            }
            isPlaying = true
            isPaused = false
        } catch (_: IOException) {
            DiagnosticLog.error(DiagnosticEvent.RECORDING_PLAYBACK_FAILED)
            validationMessage = resources.getString(R.string.recording_error_play)
            showValidationDialog = true
        }
    }

    fun deleteRecordingLogic() {
        stopPlayingLogic()
        currentRecordingData?.filePath?.let { filePath ->
            File(filePath).delete()
        }
        currentRecordingData = null
        audioFile = null
        recordingDuration = 0L
    }

    fun handleSaveLogic() {
        if (title.trim().isEmpty()) {
            validationMessage = resources.getString(R.string.recording_empty_title)
            showValidationDialog = true
            return
        }

        if (currentRecordingData == null) {
            validationMessage = resources.getString(R.string.recording_empty_audio)
            showValidationDialog = true
            return
        }

        val now = System.currentTimeMillis()
        val updatedNote = note?.copy(
            title = title.trim(),
            content = "",
            timestamp = now,
            createdAt = note.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now,
            isRecording = true,
            folderId = selectedFolderId
        ) ?: Note(
            title = title.trim(),
            content = "",
            isRecording = true,
            folderId = selectedFolderId,
            createdAt = now,
            updatedAt = now
        )

        protectedDraft?.clear()
        onSave(updatedNote, currentRecordingData)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (!startRecordingLogic()) {
                validationMessage = resources.getString(R.string.recording_error_start)
                showValidationDialog = true
            }
        } else {
            showPermissionDialog = true
        }
    }

    LaunchedEffect(recordingData) {
        if (protectedDraft == null && recordingData?.filePath?.isNotEmpty() == true) {
            audioFile = File(recordingData.filePath)
            currentRecordingData = recordingData
            recordingDuration = recordingData.duration
        }

        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) stopRecordingLogic()
            protectedDraft?.sealFinalSnapshot(recordingSnapshot())
            mediaRecorder?.release()
            mediaPlayer?.release()
        }
    }

    BackHandler {
        if (hasChanges()) {
            handleSaveLogic()
        } else {
            onCancel()
        }
    }

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = if (recordingData != null) stringResource(R.string.edit_recording)
                else stringResource(R.string.create_recording),
                onBackClick = onCancel,
                actions = {
                    if (noteFolders.isNotEmpty() && note == null) {
                        IconAction(
                            imageVector = Icons.Default.Folder,
                            contentDescription = stringResource(R.string.note_folder_action_description),
                            onClick = { showFolderDialog = true }
                        )
                    }
                    IconAction(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.save),
                        onClick = { handleSaveLogic() }
                    )
                },
                appSettingsViewModel = appSettingsViewModel
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                NoteTitle(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.focusRequester(focusRequester),
                    accentColor = accentColor
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (currentRecordingData != null || isRecording) {
                            val displayDuration =
                                if (isRecording) currentDuration else currentRecordingData!!.duration
                            val durationSeconds = displayDuration / 1000
                            val minutes = durationSeconds / 60
                            val seconds = durationSeconds % 60

                            Text(
                                text = stringResource(
                                    R.string.recording_duration,
                                    "${minutes}:${seconds.toString().padStart(2, '0')}"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = textColor,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isRecording) {
                                IconButton(
                                    onClick = {
                                        if (hasAudioPermission()) {
                                            if (!startRecordingLogic()) {
                                                validationMessage =
                                                    resources.getString(R.string.recording_error_start)
                                                showValidationDialog = true
                                            }
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                ) {
                                    Icon(
                                        imageVector = Lucide.Mic,
                                        contentDescription = stringResource(R.string.cd_record),
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        if (!stopRecordingLogic()) {
                                            validationMessage =
                                                resources.getString(R.string.recording_error_stop)
                                            showValidationDialog = true
                                        }
                                    },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = stringResource(R.string.cd_stop_recording),
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            if (currentRecordingData != null && !isRecording) {
                                IconButton(
                                    onClick = {
                                        when {
                                            isPlaying -> mediaPlayer?.pause()
                                                .also { isPaused = true; isPlaying = false }

                                            isPaused -> mediaPlayer?.start()
                                                .also { isPaused = false; isPlaying = true }

                                            else -> startPlayingLogic()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = stringResource(
                                            if (isPlaying) R.string.cd_pause_recording else R.string.cd_play_recording,
                                        ),
                                        tint = Color.White
                                    )
                                }

                                IconButton(
                                    onClick = { deleteRecordingLogic() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.cd_delete_recording),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        if (isRecording) {
                            Text(
                                text = stringResource(R.string.recording_in_progress),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (currentRecordingData == null && !isRecording) {
                            Text(
                                text = stringResource(R.string.recording_tap_to_start),
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
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
            title = stringResource(R.string.error_title),
            message1 = validationMessage,
            buttonText = stringResource(R.string.understood),
            onButtonClick = { showValidationDialog = false },
            onDismiss = { showValidationDialog = false },
        )
    }

    if (showPermissionDialog) {
        MessageDialog(
            title = stringResource(R.string.permission_required),
            message1 = stringResource(R.string.microphone_permission_message),
            buttonText = stringResource(R.string.understood),
            onButtonClick = { showPermissionDialog = false },
            onDismiss = { showPermissionDialog = false },
        )
    }
}

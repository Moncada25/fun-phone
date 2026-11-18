@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bookverse.contacts.ui.screens.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.bookverse.contacts.feature.tools.R
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.TextField
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.screens.settings.SettingsChoiceRow
import com.bookverse.contacts.ui.screens.settings.SettingsSheetScaffold
import com.bookverse.contacts.utils.ClipboardUtils
import com.bookverse.contacts.utils.LanguageOption
import com.bookverse.contacts.utils.SpeechUtils
import com.bookverse.contacts.utils.VoiceOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VoiceScreen(
    onBackClick: () -> Unit,
    initialTextToSpeak: String? = null,
    showSnackbar: Boolean = false,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    val accentColor = MaterialTheme.colorScheme.primary
    val snackbarState = rememberAppSnackbarState()
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior(expandable = false)

    var isListening by remember { mutableStateOf(false) }
    var transcriptionText by remember { mutableStateOf("") }
    var partialTranscription by remember { mutableStateOf("") }
    var showTranscription by remember { mutableStateOf(true) }
    var textToSpeak by remember { mutableStateOf("") }
    var isSpeaking by remember { mutableStateOf(false) }
    var availableVoices by remember { mutableStateOf<List<VoiceOption>>(emptyList()) }
    var selectedVoice by remember { mutableStateOf<VoiceOption?>(null) }
    var showVoiceSelector by remember { mutableStateOf(false) }
    var availableLanguages by remember { mutableStateOf<List<LanguageOption>>(emptyList()) }
    var selectedLanguage by remember { mutableStateOf<LanguageOption?>(null) }
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showValidationDialog by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showImportSnackbar by remember { mutableStateOf(showSnackbar) }

    val speechUtils = remember { SpeechUtils(context) }

    LaunchedEffect(initialTextToSpeak) {
        initialTextToSpeak?.let {
            textToSpeak = it
            if (showImportSnackbar) {
                snackbarState.show(
                    message = resources.getString(R.string.note_imported_to_voice),
                    type = SnackbarType.Success,
                )
                showImportSnackbar = false
            }
        }
    }

    fun hasAudioPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    fun startSpeechRecognition() {
        if (!speechUtils.isSpeechRecognitionAvailable()) {
            validationMessage = resources.getString(R.string.speech_recognition_unavailable)
            showValidationDialog = true
            return
        }
        partialTranscription = ""
        speechUtils.startListening(
            onResult = { result ->
                transcriptionText = result
                partialTranscription = ""
                isListening = false
            },
            onError = { error ->
                validationMessage = error
                showValidationDialog = true
                isListening = false
            },
            onStart = { isListening = true },
            onEnd = { isListening = false },
            onPartial = { partial ->
                if (transcriptionText.isEmpty()) partialTranscription = partial
            },
            continuous = false,
            languageCode = selectedLanguage?.code,
        )
    }

    fun stopSpeechRecognition() {
        speechUtils.stopListening()
        isListening = false
        partialTranscription = ""
    }

    fun speakText() {
        if (textToSpeak.trim().isNotEmpty()) {
            coroutineScope.launch {
                isSpeaking = true
                speechUtils.speakText(
                    text = textToSpeak.trim(),
                    voice = selectedVoice?.voice,
                    onStart = { },
                    onDone = { isSpeaking = false },
                    onError = { error ->
                        isSpeaking = false
                        validationMessage = error
                        showValidationDialog = true
                    },
                )
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) startSpeechRecognition() else showPermissionDialog = true
    }

    LaunchedEffect(Unit) {
        speechUtils.initializeTextToSpeech()
        availableVoices = speechUtils.getAvailableVoices().map(VoiceOption::fromVoice)
        availableLanguages = speechUtils.getAvailableLanguages()
        selectedLanguage = availableLanguages.firstOrNull { it.code == speechUtils.getCurrentLanguage() }
    }

    LaunchedEffect(showVoiceSelector) {
        if (showVoiceSelector) DiagnosticLog.debug(DiagnosticEvent.VOICE_OPTIONS_DISCOVERED)
    }

    DisposableEffect(Unit) {
        onDispose { speechUtils.destroy() }
    }

    BackHandler { onBackClick() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.voice_utilities_title),
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarState.hostState) },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
        ) {
            val expanded = maxWidth >= 840.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (expanded) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                TextToSpeechCard(
                                    modifier = Modifier.weight(1f),
                                    textToSpeak = textToSpeak,
                                    onTextChange = { textToSpeak = it },
                                    selectedVoice = selectedVoice,
                                    isSpeaking = isSpeaking,
                                    accentColor = accentColor,
                                    onSelectVoice = { showVoiceSelector = true },
                                    onShare = {
                                        shareAudio(
                                            context = context,
                                            text = textToSpeak.trim(),
                                            voice = selectedVoice,
                                            speechUtils = speechUtils,
                                            onError = { error ->
                                                validationMessage = error
                                                showValidationDialog = true
                                            },
                                        )
                                    },
                                    onSpeak = {
                                        if (isSpeaking) {
                                            speechUtils.stopSpeaking()
                                            isSpeaking = false
                                        } else {
                                            speakText()
                                        }
                                    },
                                )
                                SpeechToTextCard(
                                    modifier = Modifier.weight(1f),
                                    isListening = isListening,
                                    partialTranscription = partialTranscription,
                                    selectedLanguage = selectedLanguage,
                                    accentColor = accentColor,
                                    onSelectLanguage = { showLanguageSelector = true },
                                    onListen = {
                                        if (isListening) {
                                            stopSpeechRecognition()
                                        } else if (hasAudioPermission()) {
                                            startSpeechRecognition()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                )
                            }
                        } else {
                            TextToSpeechCard(
                                textToSpeak = textToSpeak,
                                onTextChange = { textToSpeak = it },
                                selectedVoice = selectedVoice,
                                isSpeaking = isSpeaking,
                                accentColor = accentColor,
                                onSelectVoice = { showVoiceSelector = true },
                                onShare = {
                                    shareAudio(
                                        context = context,
                                        text = textToSpeak.trim(),
                                        voice = selectedVoice,
                                        speechUtils = speechUtils,
                                        onError = { error ->
                                            validationMessage = error
                                            showValidationDialog = true
                                        },
                                    )
                                },
                                onSpeak = {
                                    if (isSpeaking) {
                                        speechUtils.stopSpeaking()
                                        isSpeaking = false
                                    } else {
                                        speakText()
                                    }
                                },
                            )
                            SpeechToTextCard(
                                isListening = isListening,
                                partialTranscription = partialTranscription,
                                selectedLanguage = selectedLanguage,
                                accentColor = accentColor,
                                onSelectLanguage = { showLanguageSelector = true },
                                onListen = {
                                    if (isListening) {
                                        stopSpeechRecognition()
                                    } else if (hasAudioPermission()) {
                                        startSpeechRecognition()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                            )
                        }

                        if (transcriptionText.isNotEmpty()) {
                            TranscriptionCard(
                                transcriptionText = transcriptionText,
                                accentColor = accentColor,
                                showTranscription = showTranscription,
                                onCopy = {
                                    ClipboardUtils.copyToClipboard(
                                        context = context,
                                        text = transcriptionText,
                                        label = resources.getString(R.string.label_transcribed_text),
                                    )
                                },
                                onToggle = { showTranscription = !showTranscription },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showVoiceSelector) {
        VoiceOptionSheet(
            options = availableVoices,
            selectedVoice = selectedVoice,
            onSelect = {
                selectedVoice = it
                showVoiceSelector = false
            },
            onDismiss = { showVoiceSelector = false },
        )
    }

    if (showLanguageSelector) {
        LanguageOptionSheet(
            options = availableLanguages,
            selectedLanguage = selectedLanguage,
            onSelect = {
                selectedLanguage = it
                showLanguageSelector = false
            },
            onDismiss = { showLanguageSelector = false },
        )
    }

    if (showValidationDialog) {
        MessageDialog(
            title = stringResource(CoreUiR.string.error_title),
            message1 = validationMessage,
            buttonText = stringResource(CoreUiR.string.understood),
            onButtonClick = { showValidationDialog = false },
            onDismiss = { showValidationDialog = false },
        )
    }

    if (showPermissionDialog) {
        MessageDialog(
            title = stringResource(CoreUiR.string.permission_required),
            message1 = stringResource(CoreUiR.string.microphone_permission_message),
            buttonText = stringResource(CoreUiR.string.understood),
            onButtonClick = { showPermissionDialog = false },
            onDismiss = { showPermissionDialog = false },
        )
    }
}

@Composable
internal fun TextToSpeechCard(
    textToSpeak: String,
    onTextChange: (String) -> Unit,
    selectedVoice: VoiceOption?,
    isSpeaking: Boolean,
    accentColor: Color,
    onSelectVoice: () -> Unit,
    onShare: () -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasText = textToSpeak.trim().isNotEmpty()
    VoiceToolCard(
        modifier = modifier,
        title = stringResource(R.string.voice_title),
        icon = Icons.Default.RecordVoiceOver,
        accentColor = accentColor,
    ) {
        TextField(
            value = textToSpeak,
            onValueChange = onTextChange,
            label = stringResource(R.string.voice_placeholder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp),
            singleLine = false,
        )

        OutlinedButton(
            onClick = onSelectVoice,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
        ) {
            Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = null,
            )
            Text(
                text = selectedVoice?.displayName ?: stringResource(R.string.select_voice),
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.material3.Button(
                onClick = onSpeak,
                enabled = hasText,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = contentColorFor(accentColor),
                ),
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(
                        if (isSpeaking) R.string.cd_stop_synthesis else R.string.cd_play_text,
                    ),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedButton(
                onClick = onShare,
                enabled = hasText,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                Text(
                    text = stringResource(R.string.share_audio),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
internal fun SpeechToTextCard(
    isListening: Boolean,
    partialTranscription: String,
    selectedLanguage: LanguageOption?,
    accentColor: Color,
    onSelectLanguage: () -> Unit,
    onListen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusText = if (isListening) {
        stringResource(R.string.listening_status)
    } else {
        stringResource(R.string.tap_to_listen)
    }
    VoiceToolCard(
        modifier = modifier,
        title = stringResource(R.string.speech_recognition_title),
        icon = Icons.Default.Mic,
        accentColor = accentColor,
    ) {
        OutlinedButton(
            onClick = onSelectLanguage,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
        ) {
            Icon(imageVector = Icons.Default.Language, contentDescription = null)
            Text(
                text = selectedLanguage?.displayName ?: stringResource(R.string.select_language),
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        FilledIconButton(
            onClick = onListen,
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.CenterHorizontally)
                .semantics {
                    role = Role.Button
                    stateDescription = statusText
                },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isListening) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    accentColor
                },
                contentColor = if (isListening) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    contentColorFor(accentColor)
                },
            ),
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isListening) {
                    stringResource(R.string.cd_stop_listening)
                } else {
                    stringResource(R.string.cd_start_listening)
                },
                modifier = Modifier.size(32.dp),
            )
        }

        Text(
            text = statusText,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isListening) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isListening) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )

        if (isListening && partialTranscription.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp, max = 144.dp),
                shape = MaterialTheme.shapes.medium,
                color = accentColor.copy(alpha = 0.08f),
            ) {
                Text(
                    text = partialTranscription,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
internal fun TranscriptionCard(
    transcriptionText: String,
    accentColor: Color? = null,
    showTranscription: Boolean,
    onCopy: () -> Unit,
    onToggle: () -> Unit,
) {
    val actionColor = accentColor ?: MaterialTheme.colorScheme.onSurface
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.transcription_result),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.cd_copy_text),
                            tint = actionColor,
                        )
                    }
                    IconButton(
                        onClick = onToggle,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = if (showTranscription) {
                                Icons.Default.ExpandLess
                            } else {
                                Icons.Default.ExpandMore
                            },
                            contentDescription = stringResource(
                                if (showTranscription) {
                                    R.string.cd_hide_transcription
                                } else {
                                    R.string.cd_show_transcription
                                },
                            ),
                            tint = actionColor,
                        )
                    }
                }
            }

            if (showTranscription) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp, max = 220.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Text(
                        text = transcriptionText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceToolCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.14f),
                    contentColor = accentColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null)
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            content()
        }
    }
}

@Composable
private fun VoiceOptionSheet(
    options: List<VoiceOption>,
    selectedVoice: VoiceOption?,
    onSelect: (VoiceOption) -> Unit,
    onDismiss: () -> Unit,
) {
    SettingsSheetScaffold(
        title = stringResource(R.string.select_voice),
        onDismiss = onDismiss,
    ) {
        if (options.isEmpty()) {
            Text(
                text = stringResource(R.string.speech_recognition_unavailable),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(options, key = { it.voice.name }) { option ->
                    SettingsChoiceRow(
                        label = option.displayName,
                        selected = selectedVoice?.voice == option.voice,
                        onClick = { onSelect(option) },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionSheet(
    options: List<LanguageOption>,
    selectedLanguage: LanguageOption?,
    onSelect: (LanguageOption) -> Unit,
    onDismiss: () -> Unit,
) {
    SettingsSheetScaffold(
        title = stringResource(R.string.recognition_language),
        onDismiss = onDismiss,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(options, key = { it.code }) { option ->
                SettingsChoiceRow(
                    label = option.displayName,
                    selected = selectedLanguage?.code == option.code,
                    onClick = { onSelect(option) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

private fun shareAudio(
    context: Context,
    text: String,
    voice: VoiceOption?,
    speechUtils: SpeechUtils,
    onError: (String) -> Unit,
) {
    val cachePath = File(context.cacheDir, "audio")
    cachePath.mkdirs()
    val file = File(cachePath, "voice_audio_${System.currentTimeMillis()}.wav")
    try {
        CoroutineScope(Dispatchers.IO).launch {
            speechUtils.synthesizeToFile(
                text = text,
                voice = voice?.voice,
                outputFile = file,
                onStart = { },
                onDone = {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val contentUri = FileProvider.getUriForFile(
                                context,
                                context.packageName + ".provider",
                                file,
                            )
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "audio/*"
                                putExtra(Intent.EXTRA_STREAM, contentUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    context.getString(R.string.share_audio),
                                ),
                            )
                        } catch (_: Exception) {
                            onError(context.getString(R.string.error_sharing_audio))
                        }
                    }
                },
                onError = { error ->
                    CoroutineScope(Dispatchers.Main).launch {
                        onError(context.getString(R.string.audio_generate_error, error))
                    }
                },
            )
        }
    } catch (_: Exception) {
        onError(context.getString(R.string.audio_file_create_error))
    }
}

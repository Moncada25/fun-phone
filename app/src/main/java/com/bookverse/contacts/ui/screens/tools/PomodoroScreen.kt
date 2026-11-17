@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bookverse.contacts.ui.screens.tools

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.R
import com.bookverse.contacts.service.PomodoroService
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.screens.settings.SettingsSheetScaffold
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.PomodoroSettings
import com.bookverse.contacts.ui.viewmodel.PomodoroTimerState
import com.bookverse.contacts.ui.viewmodel.PomodoroViewModel
import kotlin.math.roundToInt

@Composable
fun PomodoroScreen(
    onBackClick: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    pomodoroViewModel: PomodoroViewModel,
) {
    val context = LocalContext.current
    val accentColor = MaterialTheme.colorScheme.primary
    val timerState by pomodoroViewModel.timerState.collectAsStateWithLifecycle()
    val timeLeftSeconds by pomodoroViewModel.timeLeftSeconds.collectAsStateWithLifecycle()
    val totalSeconds by pomodoroViewModel.totalSeconds.collectAsStateWithLifecycle()
    val completedSessions by pomodoroViewModel.completedSessions.collectAsStateWithLifecycle()
    val settings by pomodoroViewModel.settings.collectAsStateWithLifecycle()
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior(expandable = false)
    var showSettingsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(timerState) {
        when (timerState) {
            PomodoroTimerState.WORK -> {
                PomodoroService.startWorkSession(context, timeLeftSeconds, totalSeconds)
            }

            PomodoroTimerState.BREAK -> {
                PomodoroService.startBreakSession(context, timeLeftSeconds, totalSeconds)
            }

            PomodoroTimerState.PAUSED -> PomodoroService.pauseTimer(context)
            PomodoroTimerState.IDLE -> PomodoroService.stopTimer(context)
        }
    }

    BackHandler { onBackClick() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.pomodoro_title),
                onBackClick = onBackClick,
                appSettingsViewModel = appSettingsViewModel,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconAction(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        onClick = { showSettingsSheet = true },
                    )
                },
            )
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
        ) {
            val expanded = maxWidth >= 840.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (expanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        TimerCard(
                            timeLeftSeconds = timeLeftSeconds,
                            totalSeconds = totalSeconds,
                            timerState = timerState,
                            accentColor = accentColor,
                            modifier = Modifier.weight(1.1f),
                        )
                        Column(
                            modifier = Modifier.weight(0.9f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            SessionInfoCard(
                                completedSessions = completedSessions,
                                sessionsUntilLongBreak = settings.sessionsUntilLongBreak,
                                accentColor = accentColor,
                            )
                            ControlCard(
                                timerState = timerState,
                                accentColor = accentColor,
                                onStartWork = pomodoroViewModel::startWorkSession,
                                onStartBreak = pomodoroViewModel::startBreakSession,
                                onPause = pomodoroViewModel::pauseTimer,
                                onResume = pomodoroViewModel::resumeTimer,
                                onReset = pomodoroViewModel::resetTimer,
                                onSkip = pomodoroViewModel::skipSession,
                                expanded = true,
                            )
                        }
                    }
                } else {
                    TimerCard(
                        timeLeftSeconds = timeLeftSeconds,
                        totalSeconds = totalSeconds,
                        timerState = timerState,
                        accentColor = accentColor,
                    )
                    SessionInfoCard(
                        completedSessions = completedSessions,
                        sessionsUntilLongBreak = settings.sessionsUntilLongBreak,
                        accentColor = accentColor,
                    )
                    ControlCard(
                        timerState = timerState,
                        accentColor = accentColor,
                        onStartWork = pomodoroViewModel::startWorkSession,
                        onStartBreak = pomodoroViewModel::startBreakSession,
                        onPause = pomodoroViewModel::pauseTimer,
                        onResume = pomodoroViewModel::resumeTimer,
                        onReset = pomodoroViewModel::resetTimer,
                        onSkip = pomodoroViewModel::skipSession,
                        expanded = false,
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        PomodoroSettingsSheet(
            settings = settings,
            accentColor = accentColor,
            onDismiss = { showSettingsSheet = false },
            onSave = { newSettings ->
                pomodoroViewModel.updateSettings(newSettings)
                showSettingsSheet = false
            },
        )
    }
}

@Composable
private fun TimerCard(
    timeLeftSeconds: Int,
    totalSeconds: Int,
    timerState: PomodoroTimerState,
    accentColor: Color,
    modifier: Modifier = Modifier,
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null)
                    }
                }
                Column {
                    Text(
                        text = stringResource(R.string.pomodoro_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = timerStateLabel(timerState),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            TimerDisplay(
                timeLeftSeconds = timeLeftSeconds,
                totalSeconds = totalSeconds,
                timerState = timerState,
                accentColor = accentColor,
            )
        }
    }
}

@Composable
private fun SessionInfoCard(
    completedSessions: Int,
    sessionsUntilLongBreak: Int,
    accentColor: Color,
) {
    val safeCycleLength = sessionsUntilLongBreak.coerceAtLeast(1)
    val sessionsToLongBreak = safeCycleLength - (completedSessions % safeCycleLength)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SessionMetric(
                label = stringResource(R.string.pomodoro_completed_sessions),
                value = completedSessions.toString(),
                valueColor = accentColor,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outline),
            )
            SessionMetric(
                label = stringResource(R.string.pomodoro_until_long_break_label),
                value = sessionsToLongBreak.toString(),
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SessionMetric(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun TimerDisplay(
    timeLeftSeconds: Int,
    totalSeconds: Int,
    timerState: PomodoroTimerState,
    accentColor: Color,
) {
    val progress by animateFloatAsState(
        targetValue = if (totalSeconds > 0) {
            timeLeftSeconds.toFloat() / totalSeconds
        } else {
            0f
        },
        label = "pomodoro_progress",
    )
    val stateLabel = timerStateLabel(timerState)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .sizeIn(maxWidth = 320.dp, maxHeight = 320.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            color = accentColor.copy(alpha = 0.18f),
            strokeWidth = 12.dp,
        )
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            color = accentColor,
            strokeWidth = 12.dp,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = formatTime(timeLeftSeconds),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ControlCard(
    timerState: PomodoroTimerState,
    accentColor: Color,
    onStartWork: () -> Unit,
    onStartBreak: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    expanded: Boolean,
) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ControlButtons(
                timerState = timerState,
                accentColor = accentColor,
                onStartWork = onStartWork,
                onStartBreak = onStartBreak,
                onPause = onPause,
                onResume = onResume,
                onReset = onReset,
                onSkip = onSkip,
                expanded = expanded,
            )
        }
    }
}

@Composable
internal fun ControlButtons(
    timerState: PomodoroTimerState,
    accentColor: Color,
    onStartWork: () -> Unit,
    onStartBreak: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    expanded: Boolean,
) {
    val primaryContentColor = contentColorFor(accentColor)
    when (timerState) {
        PomodoroTimerState.IDLE -> {
            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StartWorkButton(
                        onClick = onStartWork,
                        accentColor = accentColor,
                        contentColor = primaryContentColor,
                        modifier = Modifier.weight(1f),
                    )
                    StartBreakButton(
                        onClick = onStartBreak,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StartWorkButton(
                        onClick = onStartWork,
                        accentColor = accentColor,
                        contentColor = primaryContentColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    StartBreakButton(
                        onClick = onStartBreak,
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        PomodoroTimerState.PAUSED -> {
            Button(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = primaryContentColor,
                ),
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Text(
                    text = stringResource(R.string.resume_label),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            SecondaryControlButtons(
                onReset = onReset,
                onSkip = onSkip,
                accentColor = accentColor,
            )
        }

        PomodoroTimerState.WORK,
        PomodoroTimerState.BREAK,
        -> {
            Button(
                onClick = onPause,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = primaryContentColor,
                ),
            ) {
                Icon(imageVector = Icons.Default.Pause, contentDescription = null)
                Text(
                    text = stringResource(R.string.pause_label),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            SecondaryControlButtons(
                onReset = onReset,
                onSkip = onSkip,
                accentColor = accentColor,
            )
        }
    }
}

@Composable
private fun StartWorkButton(
    onClick: () -> Unit,
    accentColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = contentColor,
        ),
    ) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        Text(
            text = stringResource(R.string.pomodoro_start_work),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun StartBreakButton(
    onClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
    ) {
        Text(text = stringResource(R.string.pomodoro_start_break))
    }
}

@Composable
private fun SecondaryControlButtons(
    onReset: () -> Unit,
    onSkip: () -> Unit,
    accentColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Text(
                text = stringResource(R.string.reset_label),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
        ) {
            Icon(imageVector = Icons.Default.SkipNext, contentDescription = null)
            Text(
                text = stringResource(R.string.pomodoro_skip),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
internal fun PomodoroSettingsSheet(
    settings: PomodoroSettings,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (PomodoroSettings) -> Unit,
) {
    var workMinutes by remember { mutableIntStateOf(settings.workMinutes) }
    var breakMinutes by remember { mutableIntStateOf(settings.breakMinutes) }
    var longBreakMinutes by remember { mutableIntStateOf(settings.longBreakMinutes) }
    var sessionsUntilLongBreak by remember { mutableIntStateOf(settings.sessionsUntilLongBreak) }

    SettingsSheetScaffold(
        title = stringResource(R.string.pomodoro_settings_title),
        subtitle = stringResource(R.string.pomodoro_description),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PomodoroSettingSlider(
                label = stringResource(R.string.pomodoro_work_minutes),
                value = workMinutes,
                range = 1..90,
                accentColor = accentColor,
                onValueChange = { workMinutes = it },
            )
            PomodoroSettingSlider(
                label = stringResource(R.string.pomodoro_break_minutes),
                value = breakMinutes,
                range = 1..30,
                accentColor = accentColor,
                onValueChange = { breakMinutes = it },
            )
            PomodoroSettingSlider(
                label = stringResource(R.string.pomodoro_long_break_minutes),
                value = longBreakMinutes,
                range = 1..60,
                accentColor = accentColor,
                onValueChange = { longBreakMinutes = it },
            )
            PomodoroSettingSlider(
                label = stringResource(R.string.pomodoro_sessions_until_long_break),
                value = sessionsUntilLongBreak,
                range = 2..10,
                accentColor = accentColor,
                onValueChange = { sessionsUntilLongBreak = it },
            )
            Button(
                onClick = {
                    onSave(
                        PomodoroSettings(
                            workMinutes = workMinutes.coerceIn(1, 90),
                            breakMinutes = breakMinutes.coerceIn(1, 30),
                            longBreakMinutes = longBreakMinutes.coerceIn(1, 60),
                            sessionsUntilLongBreak = sessionsUntilLongBreak.coerceIn(2, 10),
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = contentColorFor(accentColor),
                ),
            ) {
                Text(text = stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun PomodoroSettingSlider(
    label: String,
    value: Int,
    range: IntRange,
    accentColor: Color,
    onValueChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(range.first, range.last)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                activeTickColor = contentColorFor(accentColor),
                inactiveTrackColor = accentColor.copy(alpha = 0.32f),
                inactiveTickColor = accentColor.copy(alpha = 0.72f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
        )
    }
}

@Composable
private fun timerStateLabel(timerState: PomodoroTimerState): String = when (timerState) {
    PomodoroTimerState.WORK -> stringResource(R.string.pomodoro_work_session)
    PomodoroTimerState.BREAK -> stringResource(R.string.pomodoro_break_session)
    PomodoroTimerState.PAUSED -> stringResource(R.string.pause_label)
    PomodoroTimerState.IDLE -> stringResource(R.string.pomodoro_ready)
}

@SuppressLint("DefaultLocale")
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}

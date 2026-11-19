@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bookverse.contacts.ui.screens.tools

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.feature.tools.R
import com.bookverse.contacts.service.PomodoroService
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.screens.settings.SettingsSheetScaffold
import com.bookverse.contacts.ui.viewmodel.PomodoroSettings
import com.bookverse.contacts.ui.viewmodel.PomodoroTimerState
import com.bookverse.contacts.ui.viewmodel.PomodoroViewModel
import kotlin.math.roundToInt
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold

private const val TABULAR_FIGURES = "tnum"

@Composable
fun PomodoroScreen(
    onBackClick: () -> Unit,
    pomodoroViewModel: PomodoroViewModel,
) {
    val context = LocalContext.current
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

    val controls: @Composable (Boolean) -> Unit = { expanded ->
        ControlButtons(
            timerState = timerState,
            onStartWork = pomodoroViewModel::startWorkSession,
            onStartBreak = pomodoroViewModel::startBreakSession,
            onPause = pomodoroViewModel::pauseTimer,
            onResume = pomodoroViewModel::resumeTimer,
            onReset = pomodoroViewModel::resetTimer,
            onSkip = pomodoroViewModel::skipSession,
            expanded = expanded,
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                title = stringResource(CoreUiR.string.pomodoro_title),
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconAction(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(CoreUiR.string.settings),
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
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (expanded) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 1040.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            TimerDisplay(
                                timeLeftSeconds = timeLeftSeconds,
                                totalSeconds = totalSeconds,
                                timerState = timerState,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(32.dp),
                        ) {
                            SessionSummary(
                                completedSessions = completedSessions,
                                sessionsUntilLongBreak = settings.sessionsUntilLongBreak,
                            )
                            controls(true)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 480.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                    ) {
                        TimerDisplay(
                            timeLeftSeconds = timeLeftSeconds,
                            totalSeconds = totalSeconds,
                            timerState = timerState,
                        )
                        SessionSummary(
                            completedSessions = completedSessions,
                            sessionsUntilLongBreak = settings.sessionsUntilLongBreak,
                        )
                        controls(false)
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        PomodoroSettingsSheet(
            settings = settings,
            onDismiss = { showSettingsSheet = false },
            onSave = { newSettings ->
                pomodoroViewModel.updateSettings(newSettings)
                showSettingsSheet = false
            },
        )
    }
}

@Composable
private fun SessionSummary(
    completedSessions: Int,
    sessionsUntilLongBreak: Int,
) {
    val safeCycleLength = sessionsUntilLongBreak.coerceAtLeast(1)
    val completedInCycle = completedSessions % safeCycleLength
    val sessionsToLongBreak = safeCycleLength - completedInCycle
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        CycleIndicator(
            completedInCycle = completedInCycle,
            cycleLength = safeCycleLength,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SessionMetric(
                label = stringResource(R.string.pomodoro_completed_sessions),
                value = completedSessions.toString(),
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            SessionMetric(
                label = stringResource(R.string.pomodoro_until_long_break_label),
                value = sessionsToLongBreak.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CycleIndicator(
    completedInCycle: Int,
    cycleLength: Int,
) {
    val filledColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(cycleLength) { index ->
            val filled = index < completedInCycle
            Box(
                modifier = Modifier
                    .size(width = if (filled) 20.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (filled) filledColor else emptyColor),
            )
        }
    }
}

@Composable
private fun SessionMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = TABULAR_FIGURES),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun TimerDisplay(
    timeLeftSeconds: Int,
    totalSeconds: Int,
    timerState: PomodoroTimerState,
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
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
        )
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = formatTime(timeLeftSeconds),
                style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = TABULAR_FIGURES),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun ControlButtons(
    timerState: PomodoroTimerState,
    onStartWork: () -> Unit,
    onStartBreak: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    expanded: Boolean,
) {
    when (timerState) {
        PomodoroTimerState.IDLE -> {
            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StartWorkButton(onClick = onStartWork, modifier = Modifier.weight(1f))
                    StartBreakButton(onClick = onStartBreak, modifier = Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StartWorkButton(onClick = onStartWork, modifier = Modifier.fillMaxWidth())
                    StartBreakButton(onClick = onStartBreak, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        PomodoroTimerState.PAUSED,
        PomodoroTimerState.WORK,
        PomodoroTimerState.BREAK,
        -> {
            val paused = timerState == PomodoroTimerState.PAUSED
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
            ) {
                RoundControl(
                    icon = Icons.Default.Refresh,
                    label = stringResource(CoreUiR.string.reset_label),
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                )
                RoundControl(
                    icon = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    label = stringResource(
                        if (paused) CoreUiR.string.resume_label else CoreUiR.string.pause_label,
                    ),
                    onClick = if (paused) onResume else onPause,
                    emphasized = true,
                    modifier = Modifier.weight(1f),
                )
                RoundControl(
                    icon = Icons.Default.SkipNext,
                    label = stringResource(R.string.pomodoro_skip),
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RoundControl(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(if (emphasized) 72.dp else 56.dp)
                    .background(
                        color = if (emphasized) colors.primary else colors.surfaceContainerHigh,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (emphasized) colors.onPrimary else colors.onSurface,
                    modifier = Modifier.size(if (emphasized) 32.dp else 24.dp),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StartWorkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
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
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
    ) {
        Text(text = stringResource(R.string.pomodoro_start_break))
    }
}

@Composable
internal fun PomodoroSettingsSheet(
    settings: PomodoroSettings,
    onDismiss: () -> Unit,
    onSave: (PomodoroSettings) -> Unit,
) {
    var workMinutes by remember { mutableIntStateOf(settings.workMinutes) }
    var breakMinutes by remember { mutableIntStateOf(settings.breakMinutes) }
    var longBreakMinutes by remember { mutableIntStateOf(settings.longBreakMinutes) }
    var sessionsUntilLongBreak by remember { mutableIntStateOf(settings.sessionsUntilLongBreak) }

    SettingsSheetScaffold(
        title = stringResource(R.string.pomodoro_settings_title),
        subtitle = stringResource(CoreUiR.string.pomodoro_description),
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PomodoroSettingSlider(
                label = stringResource(R.string.pomodoro_work_minutes),
                value = workMinutes,
                range = 1..90,
                onValueChange = { workMinutes = it },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PomodoroSettingSlider(
                label = stringResource(R.string.pomodoro_break_minutes),
                value = breakMinutes,
                range = 1..30,
                onValueChange = { breakMinutes = it },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PomodoroSettingSlider(
                label = stringResource(R.string.pomodoro_long_break_minutes),
                value = longBreakMinutes,
                range = 1..60,
                onValueChange = { longBreakMinutes = it },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PomodoroSettingSlider(
                label = stringResource(R.string.pomodoro_sessions_until_long_break),
                value = sessionsUntilLongBreak,
                range = 2..10,
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
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                Text(text = stringResource(CoreUiR.string.save))
            }
        }
    }
}

@Composable
private fun PomodoroSettingSlider(
    label: String,
    value: Int,
    range: IntRange,
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
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TABULAR_FIGURES),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(range.first, range.last)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
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
    PomodoroTimerState.PAUSED -> stringResource(CoreUiR.string.pause_label)
    PomodoroTimerState.IDLE -> stringResource(R.string.pomodoro_ready)
}

@SuppressLint("DefaultLocale")
private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}

package com.bookverse.contacts.ui.screens.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import com.bookverse.contacts.feature.tools.R
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.DatePickerDialog
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.theme.menstrualPhaseColors
import com.bookverse.contacts.ui.viewmodel.DEFAULT_CYCLE_LENGTH
import com.bookverse.contacts.ui.viewmodel.DEFAULT_LUTEAL_LENGTH
import com.bookverse.contacts.ui.viewmodel.MenstrualCalendarUiState
import com.bookverse.contacts.ui.viewmodel.MenstrualCalendarViewModel
import com.bookverse.contacts.ui.viewmodel.MenstrualCycleSnapshot
import com.bookverse.contacts.ui.viewmodel.MenstrualPhaseDetail
import com.bookverse.contacts.ui.viewmodel.MenstrualPhaseType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenstrualCalendarScreen(
    onBackClick: () -> Unit,
    viewModel: MenstrualCalendarViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior()
    val focusManager = LocalFocusManager.current
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    val today = LocalDate.now()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showResetConfirmation by rememberSaveable { mutableStateOf(false) }
    var selectedDateValue by rememberSaveable { mutableStateOf<String?>(null) }
    var displayedMonthValue by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val displayedMonth = remember(displayedMonthValue) {
        runCatching { YearMonth.parse(displayedMonthValue) }.getOrDefault(YearMonth.now())
    }
    val selectedDate = remember(selectedDateValue) {
        selectedDateValue?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
    }
    val isConfigured = uiState.snapshot != null && uiState.selectedStartDate != null

    LaunchedEffect(isConfigured) {
        if (isConfigured) {
            displayedMonthValue = YearMonth.from(today).toString()
        }
    }

    BackHandler {
        when {
            showResetConfirmation -> showResetConfirmation = false
            showDatePicker -> showDatePicker = false
            showSettings -> {
                viewModel.discardConfigurationChanges()
                showSettings = false
            }
            else -> onBackClick()
        }
    }

    if (showDatePicker) {
        val selectedDateForPicker = uiState.selectedStartDate
            ?.takeIf { MenstrualCalendarPolicy.isSelectableStartDate(it, today) }
            ?: today
        DatePickerDialog(
            selectedDate = selectedDateForPicker.toEpochMilli(),
            maxSelectableDate = today.toUtcEpochMilli(),
            onDateSelected = { dateMillis ->
                dateMillis.toLocalDate()?.let(viewModel::onStartDateSelected)
            },
            onDismiss = { showDatePicker = false },
            title = stringResource(R.string.menstrual_start_date_label),
        )
    }

    if (showSettings) {
        CycleConfigurationSheet(
            state = uiState,
            isConfigured = isConfigured,
            onDateClick = {
                focusManager.clearFocus(force = true)
                showDatePicker = true
            },
            onCycleChange = viewModel::onCycleLengthChange,
            onLutealChange = viewModel::onLutealLengthChange,
            onSave = {
                focusManager.clearFocus(force = true)
                if (viewModel.saveConfiguration()) {
                    showSettings = false
                    displayedMonthValue = YearMonth.from(today).toString()
                }
            },
            onReset = {
                showSettings = false
                showResetConfirmation = true
            },
            onDismiss = {
                viewModel.discardConfigurationChanges()
                showSettings = false
            },
        )
    }

    if (showResetConfirmation) {
        ConfirmDialog(
            onDismiss = { showResetConfirmation = false },
            onConfirm = {
                selectedDateValue = null
                displayedMonthValue = YearMonth.from(today).toString()
                viewModel.resetConfiguration()
            },
            title = stringResource(R.string.menstrual_reset_title),
            message = stringResource(R.string.menstrual_reset_message),
            confirmButtonLabel = stringResource(R.string.menstrual_reset_confirm),
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                title = stringResource(R.string.menstrual_calendar_title),
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (isConfigured) {
                        IconAction(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.menstrual_edit_cycle),
                            onClick = { showSettings = true },
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            !uiState.isLoaded -> MenstrualCalendarLoading(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )

            !isConfigured -> MenstrualSetupContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onConfigure = { showSettings = true },
            )

            else -> {
                val baseStartDate = requireNotNull(uiState.selectedStartDate)
                val cycleLength = uiState.cycleLengthInput.toIntOrNull()
                    ?.takeIf { it > 0 }
                    ?: DEFAULT_CYCLE_LENGTH
                val lutealLength = uiState.lutealLengthInput.toIntOrNull()
                    ?.takeIf { it > 0 }
                    ?: DEFAULT_LUTEAL_LENGTH
                val selectedSnapshot = selectedDate?.let { date ->
                    MenstrualCalendarPolicy.snapshotForDate(
                        date = date,
                        baseStartDate = baseStartDate,
                        cycleLength = cycleLength,
                        lutealLength = lutealLength,
                    )
                }
                val selectedPhase = selectedDate?.let { date ->
                    MenstrualCalendarPolicy.phaseForDate(
                        date = date,
                        baseStartDate = baseStartDate,
                        cycleLength = cycleLength,
                        lutealLength = lutealLength,
                    )
                }

                MenstrualCalendarDashboard(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    baseStartDate = baseStartDate,
                    cycleLength = cycleLength,
                    lutealLength = lutealLength,
                    displayedMonth = displayedMonth,
                    selectedDate = selectedDate,
                    today = today,
                    locale = locale,
                    onPreviousMonth = {
                        displayedMonthValue = displayedMonth.minusMonths(1).toString()
                    },
                    onNextMonth = {
                        displayedMonthValue = displayedMonth.plusMonths(1).toString()
                    },
                    onToday = {
                        displayedMonthValue = YearMonth.from(today).toString()
                        selectedDateValue = null
                    },
                    onDateSelected = { date -> selectedDateValue = date.toString() },
                )

                if (selectedDate != null && selectedSnapshot != null && selectedPhase != null) {
                    DayEstimateSheet(
                        date = selectedDate,
                        snapshot = selectedSnapshot,
                        phase = selectedPhase,
                        locale = locale,
                        onDismiss = { selectedDateValue = null },
                    )
                }
            }
        }
    }
}

@Composable
private fun MenstrualCalendarLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.menstrual_loading),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun MenstrualSetupContent(
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.menstrual_setup_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.menstrual_setup_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = stringResource(R.string.menstrual_local_only),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(
                    onClick = onConfigure,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.menstrual_setup_action))
                }
            }
        }
    }
}

@Composable
private fun MenstrualCalendarDashboard(
    baseStartDate: LocalDate,
    cycleLength: Int,
    lutealLength: Int,
    displayedMonth: YearMonth,
    selectedDate: LocalDate?,
    today: LocalDate,
    locale: Locale,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentSnapshot = remember(baseStartDate, cycleLength, lutealLength, today) {
        MenstrualCalendarPolicy.snapshotForDate(
            date = today,
            baseStartDate = baseStartDate,
            cycleLength = cycleLength,
            lutealLength = lutealLength,
        )
    }
    val currentPhase = remember(currentSnapshot, today) {
        requireNotNull(currentSnapshot.phases.firstOrNull { today in it.startDate..it.endDate })
    }

    BoxWithConstraints(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
    ) {
        val expanded = maxWidth >= 840.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "today") {
                TodayEstimateCard(
                    snapshot = currentSnapshot,
                    phase = currentPhase,
                    today = today,
                    locale = locale,
                )
            }
            if (expanded) {
                item(key = "calendar_and_summary") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        CycleCalendarCard(
                            baseStartDate = baseStartDate,
                            cycleLength = cycleLength,
                            lutealLength = lutealLength,
                            displayedMonth = displayedMonth,
                            selectedDate = selectedDate,
                            today = today,
                            locale = locale,
                            onPreviousMonth = onPreviousMonth,
                            onNextMonth = onNextMonth,
                            onToday = onToday,
                            onDateSelected = onDateSelected,
                            modifier = Modifier.weight(1.55f),
                        )
                        CycleGuideCard(
                            snapshot = currentSnapshot,
                            locale = locale,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                item(key = "calendar") {
                    CycleCalendarCard(
                        baseStartDate = baseStartDate,
                        cycleLength = cycleLength,
                        lutealLength = lutealLength,
                        displayedMonth = displayedMonth,
                        selectedDate = selectedDate,
                        today = today,
                        locale = locale,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onToday = onToday,
                        onDateSelected = onDateSelected,
                    )
                }
                item(key = "guide") {
                    CycleGuideCard(snapshot = currentSnapshot, locale = locale)
                }
            }
            item(key = "disclaimer") {
                Text(
                    text = stringResource(R.string.menstrual_info_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TodayEstimateCard(
    snapshot: MenstrualCycleSnapshot,
    phase: MenstrualPhaseDetail,
    today: LocalDate,
    locale: Locale,
) {
    val colors = menstrualPhaseColors(phase.type)
    val phaseName = stringResource(phase.type.titleRes())
    val dayOfPhase = ChronoUnit.DAYS.between(phase.startDate, today).toInt() + 1
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.menstrual_today_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                color = colors.container,
                contentColor = colors.content,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = phaseName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(
                            R.string.menstrual_today_phase_summary,
                            dayOfPhase,
                            phase.durationDays,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.menstrual_ovulation_label),
                    value = snapshot.ovulationDate.formatLocalized(locale),
                )
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.menstrual_next_cycle_label),
                    value = snapshot.nextCycleStart.formatLocalized(locale),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.menstrual_today_tip_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(phase.type.tipRes()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun CycleCalendarCard(
    baseStartDate: LocalDate,
    cycleLength: Int,
    lutealLength: Int,
    displayedMonth: YearMonth,
    selectedDate: LocalDate?,
    today: LocalDate,
    locale: Locale,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstDayOfWeek = remember(locale) { MenstrualCalendarPolicy.firstDayOfWeek(locale) }
    val visibleDates = remember(displayedMonth, firstDayOfWeek) {
        MenstrualCalendarPolicy.visibleDates(displayedMonth, firstDayOfWeek)
    }
    val phaseMap = remember(visibleDates, baseStartDate, cycleLength, lutealLength) {
        MenstrualCalendarPolicy.phasesForRange(
            range = visibleDates,
            baseStartDate = baseStartDate,
            cycleLength = cycleLength,
            lutealLength = lutealLength,
        )
    }
    val weekDays = remember(firstDayOfWeek) {
        (0 until DayOfWeek.entries.size).map { offset ->
            firstDayOfWeek.plus(offset.toLong())
        }
    }
    val monthLabel = remember(displayedMonth, locale) {
        displayedMonth.month.getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { character ->
                if (character.isLowerCase()) character.titlecase(locale) else character.toString()
            }
            .plus(" ${displayedMonth.year}")
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.menstrual_calendar_calendar_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(R.string.menstrual_calendar_previous_month),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(
                        onClick = onToday,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 8.dp,
                            vertical = 0.dp,
                        ),
                    ) {
                        Text(stringResource(R.string.menstrual_calendar_today))
                    }
                }
                IconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.menstrual_calendar_next_month),
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { dayOfWeek ->
                    Text(
                        text = dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            visibleDates.chunked(DayOfWeek.entries.size).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        CalendarDayCell(
                            date = date,
                            phase = phaseMap.getValue(date),
                            isCurrentMonth = YearMonth.from(date) == displayedMonth,
                            isToday = date == today,
                            isSelected = date == selectedDate,
                            locale = locale,
                            onClick = { onDateSelected(date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.CalendarDayCell(
    date: LocalDate,
    phase: MenstrualPhaseType,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    locale: Locale,
    onClick: () -> Unit,
) {
    val phaseColors = menstrualPhaseColors(phase)
    val phaseName = stringResource(phase.titleRes())
    val state = when {
        isToday && isSelected -> stringResource(R.string.menstrual_today_selected_marker)
        isSelected -> stringResource(R.string.menstrual_selected_marker)
        isToday -> stringResource(R.string.menstrual_today_marker)
        else -> stringResource(R.string.menstrual_estimated_label)
    }
    val description = stringResource(
        R.string.menstrual_calendar_day_description,
        date.formatLocalized(locale),
        phaseName,
        state,
    )
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.outline
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val cellModifier = Modifier
        .weight(1f)
        .heightIn(min = 48.dp)
        .padding(2.dp)

    Surface(
        modifier = if (isCurrentMonth) {
            cellModifier
                .semantics {
                    contentDescription = description
                    role = Role.Button
                }
                .clip(MaterialTheme.shapes.medium)
                .border(2.dp, borderColor, MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
        } else {
            cellModifier
                .semantics { contentDescription = description }
                .clip(MaterialTheme.shapes.medium)
                .border(1.dp, borderColor, MaterialTheme.shapes.medium)
        },
        color = if (isCurrentMonth) phaseColors.container else phaseColors.container.copy(alpha = 0.38f),
        contentColor = if (isCurrentMonth) phaseColors.content else phaseColors.content.copy(alpha = 0.62f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CycleGuideCard(
    snapshot: MenstrualCycleSnapshot,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.menstrual_cycle_guide_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    R.string.menstrual_timeline_subtitle,
                    snapshot.phases.sumOf(MenstrualPhaseDetail::durationDays),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MenstrualTimeline(phases = snapshot.phases)
            snapshot.phases.forEach { phase ->
                PhaseGuideRow(phase = phase, locale = locale)
            }
        }
    }
}

@Composable
private fun MenstrualTimeline(phases: List<MenstrualPhaseDetail>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(CircleShape),
    ) {
        phases.forEach { phase ->
            Box(
                modifier = Modifier
                    .weight(phase.durationDays.coerceAtLeast(1).toFloat())
                    .height(12.dp)
                    .background(menstrualPhaseColors(phase.type).indicator),
            )
        }
    }
}

@Composable
private fun PhaseGuideRow(
    phase: MenstrualPhaseDetail,
    locale: Locale,
) {
    val colors = menstrualPhaseColors(phase.type)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(colors.indicator),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(phase.type.titleRes()),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = formatRange(phase.startDate, phase.endDate, locale),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = pluralStringResource(
                R.plurals.menstrual_phase_duration,
                phase.durationDays,
                phase.durationDays,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayEstimateSheet(
    date: LocalDate,
    snapshot: MenstrualCycleSnapshot,
    phase: MenstrualPhaseType,
    locale: Locale,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val phaseDetail = requireNotNull(snapshot.phases.firstOrNull { date in it.startDate..it.endDate })
    val phaseColors = menstrualPhaseColors(phase)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.menstrual_day_detail_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = date.formatLocalized(locale),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                color = phaseColors.container,
                contentColor = phaseColors.content,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(phase.titleRes()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(
                            R.string.menstrual_day_detail_phase_range,
                            formatRange(phaseDetail.startDate, phaseDetail.endDate, locale),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            SummaryMetric(
                label = stringResource(R.string.menstrual_next_cycle_label),
                value = snapshot.nextCycleStart.formatLocalized(locale),
            )
            Text(
                text = stringResource(R.string.menstrual_day_detail_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(CoreUiR.string.close))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleConfigurationSheet(
    state: MenstrualCalendarUiState,
    isConfigured: Boolean,
    onDateClick: () -> Unit,
    onCycleChange: (String) -> Unit,
    onLutealChange: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.menstrual_settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.menstrual_calendar_form_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DateSelectorField(
                label = stringResource(R.string.menstrual_start_date_label),
                selectedDate = state.selectedStartDate,
                errorMessage = state.startDateError?.messageRes,
                onClick = onDateClick,
            )
            NumberInputField(
                label = stringResource(R.string.menstrual_cycle_length_label),
                value = state.cycleLengthInput,
                onValueChange = onCycleChange,
                placeholder = DEFAULT_CYCLE_LENGTH.toString(),
                errorMessage = state.cycleLengthError?.messageRes,
            )
            NumberInputField(
                label = stringResource(R.string.menstrual_luteal_length_label),
                value = state.lutealLengthInput,
                onValueChange = onLutealChange,
                placeholder = DEFAULT_LUTEAL_LENGTH.toString(),
                errorMessage = state.lutealLengthError?.messageRes,
            )
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.primary),
                ),
            ) {
                Text(stringResource(R.string.menstrual_save_button))
            }
            if (isConfigured) {
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.menstrual_reset_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DateSelectorField(
    label: String,
    selectedDate: LocalDate?,
    errorMessage: Int?,
    onClick: () -> Unit,
) {
    val isError = errorMessage != null
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedDate?.formatLocalized()
                            ?: stringResource(R.string.menstrual_start_date_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.menstrual_start_date_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = label,
                )
            }
        }
        errorMessage?.let { messageRes ->
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun NumberInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    errorMessage: Int?,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        isError = errorMessage != null,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        supportingText = errorMessage?.let { messageRes ->
            {
                Text(
                    text = stringResource(messageRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}

private fun MenstrualPhaseType.titleRes(): Int = when (this) {
    MenstrualPhaseType.MENSTRUATION -> R.string.menstrual_phase_menstruation
    MenstrualPhaseType.FOLLICULAR -> R.string.menstrual_phase_follicular
    MenstrualPhaseType.OVULATION -> R.string.menstrual_phase_ovulation
    MenstrualPhaseType.LUTEAL -> R.string.menstrual_phase_luteal
}

private fun MenstrualPhaseType.tipRes(): Int = when (this) {
    MenstrualPhaseType.MENSTRUATION -> R.string.menstrual_tip_menstruation
    MenstrualPhaseType.FOLLICULAR -> R.string.menstrual_tip_follicular
    MenstrualPhaseType.OVULATION -> R.string.menstrual_tip_ovulation
    MenstrualPhaseType.LUTEAL -> R.string.menstrual_tip_luteal
}

private fun LocalDate.formatLocalized(locale: Locale = Locale.getDefault()): String {
    return format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
}

private fun formatRange(start: LocalDate, end: LocalDate, locale: Locale): String {
    return if (start == end) {
        start.formatLocalized(locale)
    } else {
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        "${start.format(formatter)} – ${end.format(formatter)}"
    }
}

private fun LocalDate.toEpochMilli(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun LocalDate.toUtcEpochMilli(): Long {
    return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun Long.toLocalDate(): LocalDate? {
    return runCatching {
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrNull()
}

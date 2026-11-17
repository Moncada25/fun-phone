package com.bookverse.contacts.ui.screens.tools

import android.Manifest
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.CalendarEvent
import com.bookverse.contacts.data.model.PublicHoliday
import com.bookverse.contacts.domain.holidays.BirthdayAgendaEntry
import com.bookverse.contacts.domain.holidays.BirthdayCalendarPolicy
import com.bookverse.contacts.settings.Country
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.components.CountrySelectDialog
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.HolidaysUiState
import com.bookverse.contacts.ui.viewmodel.HolidaysViewModel
import com.bookverse.contacts.utils.PermissionUtils
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle

private const val MAX_CALENDAR_WEEKS = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidaysScreen(
    onBackClick: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    contactsViewModel: ContactsViewModel,
    viewModel: HolidaysViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val contacts by contactsViewModel.contacts.collectAsStateWithLifecycle()
    val profile by contactsViewModel.profile.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    val monthPagerScope = rememberCoroutineScope()
    val pagerBaseMonth = remember { state.selectedMonth }
    val monthPagerState = rememberPagerState(initialPage = MONTH_PAGER_CENTER) {
        Int.MAX_VALUE
    }
    val focusedMonth by remember(monthPagerState, pagerBaseMonth) {
        derivedStateOf {
            monthForPage(pagerBaseMonth, monthPagerState.currentPage)
        }
    }
    val context = LocalContext.current
    val selectCountryDescription = stringResource(R.string.holidays_select_country)
    val activity = context as? Activity
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior()
    var showCountrySelector by rememberSaveable { mutableStateOf(false) }
    var showCalendarSelector by rememberSaveable { mutableStateOf(false) }
    var showCalendarRationale by rememberSaveable { mutableStateOf(false) }
    var showCalendarSettings by rememberSaveable { mutableStateOf(false) }
    var requestedCalendarPermission by rememberSaveable { mutableStateOf(false) }
    var hasCalendarPermission by remember {
        mutableStateOf(PermissionUtils.hasCalendarPermission(context))
    }
    var hasContactsPermission by remember {
        mutableStateOf(PermissionUtils.hasPermission(context, Manifest.permission.READ_CONTACTS))
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCalendarPermission = granted
        requestedCalendarPermission = true
        if (granted) {
            viewModel.refreshCalendarSources()
            showCalendarSelector = true
        } else {
            val permanentlyDenied = activity == null ||
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.READ_CALENDAR,
                )
            if (permanentlyDenied) {
                showCalendarSettings = true
            }
        }
    }

    fun openCalendarManager() {
        if (hasCalendarPermission) {
            viewModel.refreshCalendarSources()
            showCalendarSelector = true
        } else {
            showCalendarRationale = true
        }
    }

    LaunchedEffect(state.isNetworkDisclosureAccepted) {
        if (state.isNetworkDisclosureAccepted) viewModel.performInitialLoad()
    }

    LaunchedEffect(hasCalendarPermission) {
        if (hasCalendarPermission) viewModel.refreshCalendarSources()
    }

    LaunchedEffect(
        monthPagerState.currentPage,
        monthPagerState.isScrollInProgress,
    ) {
        if (monthPagerState.isScrollInProgress) return@LaunchedEffect
        val settledMonth = monthForPage(pagerBaseMonth, monthPagerState.currentPage)
        if (settledMonth != state.selectedMonth) viewModel.selectMonth(settledMonth)
    }

    LaunchedEffect(state.selectedMonth) {
        val pagerMonth = monthForPage(pagerBaseMonth, monthPagerState.currentPage)
        if (pagerMonth == state.selectedMonth || monthPagerState.isScrollInProgress) {
            return@LaunchedEffect
        }
        monthPagerState.animateScrollToPage(
            pageForMonth(pagerBaseMonth, state.selectedMonth),
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val nowGranted = PermissionUtils.hasCalendarPermission(context)
        if (nowGranted && !hasCalendarPermission) {
            viewModel.refreshCalendarSources()
        }
        hasCalendarPermission = nowGranted
        hasContactsPermission = PermissionUtils.hasPermission(context, Manifest.permission.READ_CONTACTS)
    }

    BackHandler(onBack = onBackClick)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                title = stringResource(R.string.holidays_title),
                onBackClick = onBackClick,
                appSettingsViewModel = appSettingsViewModel,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconAction(onClick = { showCountrySelector = true }) {
                        Text(
                            text = state.selectedCountry?.flag ?: "🌐",
                            modifier = Modifier.semantics {
                                contentDescription = selectCountryDescription
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            val expanded = maxWidth >= 840.dp
            val birthdays = remember(contacts, profile, state.selectedMonth, hasContactsPermission) {
                if (hasContactsPermission) {
                    BirthdayCalendarPolicy.entriesForMonth(
                        contacts = birthdayAgendaContacts(contacts, profile),
                        month = state.selectedMonth,
                    )
                } else {
                    emptyList()
                }
            }
            val monthItems = remember(
                state.holidays,
                state.calendarEvents,
                state.selectedMonth,
                birthdays,
            ) {
                agendaItems(state, birthdays)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "month_controls") {
                    MonthControls(
                        month = focusedMonth,
                        country = state.selectedCountry,
                        holidayCount = state.holidays.size,
                        onPrevious = {
                            monthPagerScope.launch {
                                monthPagerState.animateScrollToPage(
                                    (monthPagerState.currentPage - 1).coerceAtLeast(0),
                                )
                            }
                        },
                        onNext = {
                            monthPagerScope.launch {
                                monthPagerState.animateScrollToPage(
                                    (monthPagerState.currentPage + 1)
                                        .coerceAtMost(Int.MAX_VALUE - 1),
                                )
                            }
                        },
                        onToday = {
                            monthPagerScope.launch {
                                monthPagerState.animateScrollToPage(
                                    pageForMonth(pagerBaseMonth, YearMonth.now()),
                                )
                            }
                        },
                        accentColor = accentColor,
                    )
                }

                if (state.isLoadingHolidays) {
                    item(key = "holiday_loading") {
                        LoadingCard(message = stringResource(R.string.holidays_loading))
                    }
                }

                state.holidaysError?.let { error ->
                    item(key = "holiday_error") {
                        InlineErrorCard(
                            message = holidayErrorMessage(error),
                            onRetry = viewModel::retryHolidays,
                            accentColor = accentColor,
                        )
                    }
                }

                item(key = "calendar_and_agenda") {
                    if (expanded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            MonthCalendarPager(
                                pagerState = monthPagerState,
                                baseMonth = pagerBaseMonth,
                                holidays = state.holidays,
                                events = state.calendarEvents,
                                birthdays = birthdays,
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f),
                            )
                            AgendaPanel(
                                items = monthItems,
                                state = state,
                                hasCalendarPermission = hasCalendarPermission,
                                hasContactsPermission = hasContactsPermission,
                                accentColor = accentColor,
                                onManageCalendars = ::openCalendarManager,
                                onRetryCalendars = {
                                    viewModel.refreshCalendarSources()
                                    viewModel.refreshCalendarEvents()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            MonthCalendarPager(
                                pagerState = monthPagerState,
                                baseMonth = pagerBaseMonth,
                                holidays = state.holidays,
                                events = state.calendarEvents,
                                birthdays = birthdays,
                                accentColor = accentColor,
                            )
                            AgendaPanel(
                                items = monthItems,
                                state = state,
                                hasCalendarPermission = hasCalendarPermission,
                                hasContactsPermission = hasContactsPermission,
                                accentColor = accentColor,
                                onManageCalendars = ::openCalendarManager,
                                onRetryCalendars = {
                                    viewModel.refreshCalendarSources()
                                    viewModel.refreshCalendarEvents()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCountrySelector) {
        CountrySelectDialog(
            onDismiss = { showCountrySelector = false },
            onCountrySelected = viewModel::selectCountry,
            selectedCountry = state.selectedCountry,
        )
    }

    if (!state.isNetworkDisclosureAccepted) {
        MessageDialog(
            title = stringResource(R.string.holidays_network_disclosure_title),
            message1 = stringResource(R.string.holidays_network_disclosure_message),
            message2 = stringResource(R.string.holidays_network_disclosure_detail),
            buttonText = stringResource(R.string.action_continue),
            onButtonClick = viewModel::acceptNetworkDisclosure,
            secondaryButtonText = stringResource(R.string.action_not_now),
            onSecondaryButtonClick = onBackClick,
            onDismiss = onBackClick,
        )
    }

    if (showCalendarRationale) {
        CalendarPermissionDialog(
            onDismiss = {
                showCalendarRationale = false
            },
            onContinue = {
                showCalendarRationale = false
                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            },
            accentColor = accentColor,
        )
    }

    if (showCalendarSettings && requestedCalendarPermission) {
        MessageDialog(
            title = stringResource(R.string.holidays_calendar_permission_denied_title),
            message1 = stringResource(R.string.holidays_calendar_permission_denied_message),
            buttonText = stringResource(R.string.open_settings),
            onButtonClick = {
                showCalendarSettings = false
                PermissionUtils.openAppSettings(context)
            },
            secondaryButtonText = stringResource(R.string.cancel),
            onSecondaryButtonClick = {
                showCalendarSettings = false
            },
            onDismiss = {
                showCalendarSettings = false
            },
        )
    }

    if (showCalendarSelector) {
        CalendarSelectorSheet(
            calendars = state.calendarSources,
            selectedIds = state.selectedCalendarIds,
            isLoading = state.isLoadingCalendarSources,
            accentColor = accentColor,
            onApply = viewModel::applyCalendarSelection,
            onDismiss = { showCalendarSelector = false },
        )
    }

}

@Composable
private fun MonthControls(
    month: YearMonth,
    country: Country?,
    holidayCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    accentColor: Color,
) {
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    val monthName = month.month.getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { it.titlecase(locale) }
    val isCurrentMonth = month == YearMonth.now()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(R.string.cd_previous_month),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthName + " " + month.year,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    country?.let {
                        Text(
                            text = pluralStringResource(
                                R.plurals.calendar_title,
                                holidayCount,
                                it.name,
                                holidayCount,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.cd_next_month),
                    )
                }
            }
            if (!isCurrentMonth) {
                TextButton(
                    onClick = onToday,
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
                ) {
                    Text(stringResource(R.string.today))
                }
            }
        }
    }
}

@Composable
private fun MonthCalendarPager(
    pagerState: PagerState,
    baseMonth: YearMonth,
    holidays: List<PublicHoliday>,
    events: List<CalendarEvent>,
    birthdays: List<BirthdayAgendaEntry>,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        pageSpacing = 8.dp,
        key = { it },
    ) { page ->
        MonthCalendarCard(
            month = monthForPage(baseMonth, page),
            holidays = holidays,
            events = events,
            birthdays = birthdays,
            accentColor = accentColor,
        )
    }
}

@Composable
private fun MonthCalendarCard(
    month: YearMonth,
    holidays: List<PublicHoliday>,
    events: List<CalendarEvent>,
    birthdays: List<BirthdayAgendaEntry>,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    val holidayDates = remember(holidays) {
        holidays.mapNotNull { holiday ->
            runCatching { LocalDate.parse(holiday.date) }.getOrNull()
        }.toSet()
    }
    val eventDates = remember(events) {
        events.map {
            val eventZone = if (it.allDay) java.time.ZoneOffset.UTC else ZoneId.systemDefault()
            Instant.ofEpochMilli(it.startDate).atZone(eventZone).toLocalDate()
        }.toSet()
    }
    val birthdayDates = remember(birthdays) { birthdays.mapTo(mutableSetOf(), BirthdayAgendaEntry::date) }
    val firstDay = month.atDay(1)
    val leadingCells = firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value
    val today = LocalDate.now()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 20.dp, end = 12.dp, bottom = 12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                DayOfWeek.entries.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, locale),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.outline,
            )
            repeat(MAX_CALENDAR_WEEKS) { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { weekday ->
                        val dayNumber = week * 7 + weekday - leadingCells + 1
                        if (dayNumber in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayNumber)
                            CalendarDayCell(
                                day = dayNumber,
                                isToday = date == today,
                                isHoliday = date in holidayDates,
                                hasEvent = date in eventDates,
                                hasBirthday = date in birthdayDates,
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaPanel(
    items: List<HolidayAgendaItem>,
    state: HolidaysUiState,
    hasCalendarPermission: Boolean,
    hasContactsPermission: Boolean,
    accentColor: Color,
    onManageCalendars: () -> Unit,
    onRetryCalendars: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedHoliday by remember { mutableStateOf<PublicHoliday?>(null) }
    var selectedEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    var showPastItems by rememberSaveable(state.selectedMonth.toString()) { mutableStateOf(false) }
    val today = LocalDate.now()
    val sections = remember(items, today) { HolidayAgendaPolicy.sections(items, today) }
    val upcomingGrouped = remember(sections.upcoming) {
        sections.upcoming.groupBy(HolidayAgendaItem::date).toSortedMap()
    }
    val pastGrouped = remember(sections.past) {
        sections.past.groupBy(HolidayAgendaItem::date).toSortedMap()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
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
                    text = stringResource(R.string.holidays_agenda_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(
                    onClick = onManageCalendars,
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.selectedCalendarIds.isEmpty()) {
                            stringResource(R.string.holidays_add_calendars)
                        } else {
                            stringResource(
                                R.string.manage_calendars,
                                state.selectedCalendarIds.size,
                            )
                        },
                    )
                }
            }

            if (!hasCalendarPermission) {
                Text(
                    text = stringResource(R.string.holidays_calendar_permission_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (!hasContactsPermission) {
                Text(
                    text = stringResource(R.string.holidays_contacts_permission_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (state.isLoadingCalendarEvents) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.holidays_loading_events))
                }
            }

            if (state.calendarError) {
                InlineErrorCard(
                    message = stringResource(R.string.holidays_calendar_load_error),
                    onRetry = onRetryCalendars,
                    accentColor = accentColor,
                )
            }

            if (sections.upcoming.isEmpty() && sections.past.isEmpty()) {
                if (!state.isLoadingHolidays && !state.isLoadingCalendarEvents) {
                    Text(
                        text = stringResource(R.string.holidays_empty_agenda),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                if (sections.past.isNotEmpty()) {
                    PastAgendaToggle(
                        pastItemCount = sections.past.size,
                        expanded = showPastItems,
                        accentColor = accentColor,
                        onToggle = { showPastItems = !showPastItems },
                    )
                }

                if (upcomingGrouped.isEmpty()) {
                    Text(
                        text = stringResource(R.string.holidays_no_upcoming_agenda),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                upcomingGrouped.forEach { (date, dayItems) ->
                    AgendaDay(
                        date = date,
                        items = dayItems,
                        accentColor = accentColor,
                        onHolidayClick = { selectedHoliday = it },
                        onEventClick = { selectedEvent = it },
                    )
                }

                if (showPastItems) {
                    Text(
                        text = stringResource(R.string.holidays_past_agenda_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    pastGrouped.forEach { (date, dayItems) ->
                        AgendaDay(
                            date = date,
                            items = dayItems,
                            accentColor = accentColor,
                            onHolidayClick = { selectedHoliday = it },
                            onEventClick = { selectedEvent = it },
                        )
                    }
                }
            }
        }
    }

    selectedHoliday?.let { holiday ->
        HolidayDetailSheet(
            holiday = holiday,
            accentColor = accentColor,
            onDismiss = { selectedHoliday = null },
        )
    }
    selectedEvent?.let { event ->
        EventDetailBottomSheet(
            event = event,
            accentColor = accentColor,
            onDismiss = { selectedEvent = null },
        )
    }
}

@Composable
private fun AgendaDay(
    date: LocalDate,
    items: List<HolidayAgendaItem>,
    accentColor: Color,
    onHolidayClick: (PublicHoliday) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
) {
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = HolidayDateFormatter.dateHeader(date.toString(), locale),
            style = MaterialTheme.typography.titleSmall,
            color = accentColor,
            fontWeight = FontWeight.Bold,
        )
        items.forEachIndexed { index, item ->
            when (item) {
                is HolidayAgendaItem.Holiday -> AgendaHolidayRow(
                    holiday = item.value,
                    accentColor = accentColor,
                    onClick = { onHolidayClick(item.value) },
                )

                is HolidayAgendaItem.Event -> AgendaEventRow(
                    event = item.value,
                    accentColor = accentColor,
                    onClick = { onEventClick(item.value) },
                )

                is HolidayAgendaItem.Birthday -> AgendaBirthdayRow(
                    birthday = item.value,
                    accentColor = accentColor,
                )
            }
            if (index != items.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun AgendaHolidayRow(
    holiday: PublicHoliday,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Public,
            contentDescription = null,
            tint = accentColor,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = holiday.localName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (holiday.name != holiday.localName) {
                Text(
                    text = holiday.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = stringResource(R.string.holidays_public_badge),
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
        )
    }
}

@Composable
private fun AgendaEventRow(
    event: CalendarEvent,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Event,
            contentDescription = null,
            tint = accentColor,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title.ifBlank {
                    stringResource(R.string.holidays_untitled_event)
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (event.allDay) {
                    stringResource(R.string.holidays_all_day)
                } else {
                    HolidayDateFormatter.eventTime(event.startDate, event.endDate, locale)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HolidayDetailSheet(
    holiday: PublicHoliday,
    accentColor: Color,
    onDismiss: () -> Unit,
) {
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = holiday.localName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (holiday.name != holiday.localName) {
                Text(
                    text = holiday.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = HolidayDateFormatter.holiday(holiday.date, locale),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (holiday.global) {
                Text(
                    text = stringResource(R.string.national_holiday),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accentColor.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                )
            }
        }
    }
}

@Composable
private fun LoadingCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Text(message)
        }
    }
}

@Composable
private fun InlineErrorCard(
    message: String,
    onRetry: () -> Unit,
    accentColor: Color,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.qr_scanner_retry), color = accentColor)
            }
        }
    }
}

private fun agendaItems(
    state: HolidaysUiState,
    birthdays: List<BirthdayAgendaEntry>,
): List<HolidayAgendaItem> {
    val month = state.selectedMonth
    val holidays = state.holidays.mapNotNull { holiday ->
        runCatching { LocalDate.parse(holiday.date) }
            .getOrNull()
            ?.takeIf { YearMonth.from(it) == month }
            ?.let { HolidayAgendaItem.Holiday(holiday, it) }
    }
    val events = state.calendarEvents.map { event ->
        val eventZone = if (event.allDay) java.time.ZoneOffset.UTC else ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(event.startDate)
            .atZone(eventZone)
            .toLocalDate()
        HolidayAgendaItem.Event(event, date)
    }
    val birthdayItems = birthdays.map { HolidayAgendaItem.Birthday(it, it.date) }
    return holidays + birthdayItems + events
}

package com.bookverse.contacts.ui.screens.tools

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.data.model.CalendarEvent
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.PublicHoliday
import com.bookverse.contacts.domain.holidays.BirthdayAgendaEntry
import com.bookverse.contacts.feature.tools.R
import com.bookverse.contacts.ui.state.HolidaysUiError
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

internal const val MONTH_PAGER_CENTER = Int.MAX_VALUE / 2

internal fun birthdayAgendaContacts(
    contacts: List<Contact>,
    profile: Contact?,
): List<Contact> = profile?.let { profileContact ->
    listOf(profileContact) + contacts.filterNot { it.id == profileContact.id }
} ?: contacts

@Composable
internal fun PastAgendaToggle(
    pastItemCount: Int,
    expanded: Boolean,
    accentColor: Color,
    onToggle: () -> Unit,
) {
    TextButton(
        onClick = onToggle,
        colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
    ) {
        Icon(
            imageVector = if (expanded) {
                Icons.Default.KeyboardArrowUp
            } else {
                Icons.Default.KeyboardArrowDown
            },
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (expanded) {
                stringResource(R.string.holidays_hide_past_agenda)
            } else {
                stringResource(R.string.holidays_show_past_agenda, pastItemCount)
            },
        )
    }
}

internal sealed interface HolidayAgendaItem {
    val date: LocalDate

    data class Holiday(
        val value: PublicHoliday,
        override val date: LocalDate,
    ) : HolidayAgendaItem

    data class Event(
        val value: CalendarEvent,
        override val date: LocalDate,
    ) : HolidayAgendaItem

    data class Birthday(
        val value: BirthdayAgendaEntry,
        override val date: LocalDate,
    ) : HolidayAgendaItem
}

internal data class HolidayAgendaSections(
    val upcoming: List<HolidayAgendaItem>,
    val past: List<HolidayAgendaItem>,
)

internal object HolidayAgendaPolicy {
    fun sections(
        items: List<HolidayAgendaItem>,
        today: LocalDate,
    ): HolidayAgendaSections {
        val ordered = items.sortedWith(
            compareBy<HolidayAgendaItem>(HolidayAgendaItem::date)
                .thenBy { it is HolidayAgendaItem.Event },
        )
        val (past, upcoming) = ordered.partition { it.date.isBefore(today) }
        return HolidayAgendaSections(upcoming = upcoming, past = past)
    }
}

@Composable
internal fun holidayErrorMessage(error: HolidaysUiError): String = when (error) {
    HolidaysUiError.NoInternet -> stringResource(R.string.holidays_internet_error)
    is HolidaysUiError.Client -> stringResource(R.string.holidays_request_error, error.code)
    is HolidaysUiError.Server -> stringResource(R.string.holidays_service_error, error.code)
    is HolidaysUiError.Unknown -> stringResource(R.string.holidays_unknown_error)
}

internal fun monthForPage(baseMonth: YearMonth, page: Int): YearMonth =
    baseMonth.plusMonths(page.toLong() - MONTH_PAGER_CENTER.toLong())

internal fun pageForMonth(baseMonth: YearMonth, month: YearMonth): Int {
    val monthOffset = ChronoUnit.MONTHS.between(baseMonth, month)
    return (MONTH_PAGER_CENTER.toLong() + monthOffset)
        .coerceIn(0L, Int.MAX_VALUE.toLong() - 1L)
        .toInt()
}

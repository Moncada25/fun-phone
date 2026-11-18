package com.bookverse.contacts.ui.screens.tools

import android.provider.CalendarContract
import android.text.Html
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.data.model.CalendarAttendee
import com.bookverse.contacts.data.model.CalendarEvent
import com.bookverse.contacts.feature.tools.R
import com.bookverse.contacts.settings.LocaleController

@Composable
fun HtmlText(
    html: String,
    style: TextStyle,
    color: Color,
) {
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
                textSize = style.fontSize.value
                setTextColor(color.hashCode())
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailBottomSheet(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    accentColor: Color,
) {
    val locale = LocaleController.effectiveLocale(LocalContext.current)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Event,
                contentDescription = stringResource(R.string.holidays_event_icon),
                tint = accentColor,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.CenterHorizontally),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(
                    R.string.holiday_event_start,
                    if (event.allDay) {
                        HolidayDateFormatter.eventDate(event.startDate, true, locale)
                    } else {
                        HolidayDateFormatter.eventDateTime(event.startDate, locale)
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.holiday_event_end,
                    if (event.allDay) {
                        HolidayDateFormatter.eventDate(event.endDate - 1L, true, locale)
                    } else {
                        HolidayDateFormatter.eventDateTime(event.endDate, locale)
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (event.description?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.holiday_event_description),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                HtmlText(
                    html = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (event.location?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.holiday_event_location),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (event.attendees.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.event_attendees_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    event.attendees.forEach { attendee ->
                        AttendeeRow(attendee = attendee, accentColor = accentColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.holiday_event_calendar, event.calendarName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontStyle = FontStyle.Italic,
            )
            Text(
                text = stringResource(R.string.holiday_event_account, event.accountName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontStyle = FontStyle.Italic,
            )
            if (event.recurrenceRule != null) {
                Text(
                    text = stringResource(R.string.calendar_event_recurring_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun AttendeeRow(
    attendee: CalendarAttendee,
    accentColor: Color,
) {
    val displayName = attendee.displayName.ifBlank {
        stringResource(R.string.event_attendee_unknown)
    }
    val statusText = attendeeStatusText(attendee.status)
    val badgeText = if (attendee.isOrganizer()) {
        stringResource(R.string.event_attendee_role_organizer)
    } else {
        null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val initials = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        badgeText?.let { badge ->
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun attendeeStatusText(status: Int): String {
    val resId = when (status) {
        CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED -> R.string.event_attendee_status_accepted
        CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED -> R.string.event_attendee_status_declined
        CalendarContract.Attendees.ATTENDEE_STATUS_INVITED -> R.string.event_attendee_status_invited
        CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE -> R.string.event_attendee_status_tentative
        CalendarContract.Attendees.ATTENDEE_STATUS_NONE -> R.string.event_attendee_status_none
        else -> R.string.event_attendee_status_unknown
    }
    return stringResource(resId)
}

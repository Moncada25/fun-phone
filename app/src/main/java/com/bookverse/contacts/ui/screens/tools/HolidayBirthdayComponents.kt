package com.bookverse.contacts.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.R
import com.bookverse.contacts.domain.holidays.BirthdayAgendaEntry

@Composable
internal fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    isHoliday: Boolean,
    hasEvent: Boolean,
    hasBirthday: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val background = when {
        isToday -> accentColor
        isHoliday -> accentColor.copy(alpha = 0.2f)
        else -> Color.Transparent
    }
    val foreground = if (isToday) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val dayDescription = stringResource(
        when {
            hasBirthday && hasEvent -> R.string.holidays_calendar_day_birthday_event
            hasBirthday -> R.string.holidays_calendar_day_birthday
            hasEvent -> R.string.holidays_calendar_day_event
            else -> R.string.holidays_calendar_day
        },
        day,
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .defaultMinSize(minHeight = 48.dp)
            .padding(3.dp)
            .clip(CircleShape)
            .background(background)
            .semantics { contentDescription = dayDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday || isHoliday) FontWeight.Bold else FontWeight.Normal,
            color = foreground,
        )
        if (hasBirthday) {
            Icon(
                imageVector = Icons.Default.Cake,
                contentDescription = null,
                tint = if (isToday) foreground else accentColor,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
                    .size(14.dp),
            )
        }
        if (hasEvent) {
            Box(
                modifier = Modifier
                    .align(if (hasBirthday) Alignment.BottomEnd else Alignment.BottomCenter)
                    .padding(
                        end = if (hasBirthday) 7.dp else 0.dp,
                        bottom = 4.dp,
                    )
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isToday) foreground else accentColor),
            )
        }
    }
}

@Composable
internal fun AgendaBirthdayRow(
    birthday: BirthdayAgendaEntry,
    accentColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Cake,
            contentDescription = null,
            tint = accentColor,
        )
        Text(
            text = birthday.contactName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(R.string.holidays_birthday_badge),
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
        )
    }
}

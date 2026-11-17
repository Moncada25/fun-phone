package com.bookverse.contacts.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallMissedOutgoing
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.RecentOperation
import com.bookverse.contacts.data.model.RecentType
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.utils.ContactUtils
import com.bookverse.contacts.utils.ContactUtils.toRelativeDate
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItem(
    operation: RecentOperation,
    textColor: Color,
    secondaryTextColor: Color,
    onEditBeforeDial: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("HH:mm", LocaleController.effectiveLocale(context))
    val (icon, label) = when (operation.type) {
        RecentType.WHATSAPP_OPEN -> Icons.Default.Whatsapp to stringResource(R.string.recent_type_whatsapp)
        RecentType.CALL_SENT -> Icons.AutoMirrored.Filled.CallMade to stringResource(R.string.recent_type_call_sent)
        RecentType.CALL_RECEIVED -> Icons.AutoMirrored.Filled.CallReceived to stringResource(
            R.string.recent_type_call_received
        )

        RecentType.CALL_MISSED -> Icons.AutoMirrored.Filled.CallMissed to stringResource(R.string.recent_type_call_missed)
        RecentType.CALL_REJECTED -> Icons.AutoMirrored.Filled.CallMissedOutgoing to stringResource(
            R.string.recent_type_call_rejected
        )

        RecentType.CALL_BLOCKED -> Icons.Filled.HideSource to stringResource(R.string.recent_type_call_blocked)
        RecentType.CALL_UNKNOWN -> Icons.Default.DeviceUnknown to stringResource(R.string.recent_type_call_unknown)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = secondaryTextColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = operation.target,
                color = secondaryTextColor,
                fontSize = 14.sp,
                modifier = if (onEditBeforeDial != null) {
                    Modifier.clickable { onEditBeforeDial(operation.target) }
                } else {
                    Modifier
                }
            )
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            val fullDate = ContactUtils.capitalizeFirstLetter(
                operation.date.toRelativeDate(context)
            ) + ", " + timeFormat.format(Date(operation.date))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fullDate,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }

            val duration = if (operation.time != null) operation.time.toInt() else 0

            Text(
                text = ContactUtils.formatSeconds(context, duration),
                color = secondaryTextColor,
                fontSize = 14.sp
            )
        }
    }
}

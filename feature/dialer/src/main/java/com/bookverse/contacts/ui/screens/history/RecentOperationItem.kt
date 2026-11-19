package com.bookverse.contacts.ui.screens.history

import com.bookverse.contacts.utils.ContactDisplay
import com.bookverse.contacts.utils.PhoneActionLauncher
import android.telephony.PhoneNumberUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallMissedOutgoing
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.data.model.RecentOperation
import com.bookverse.contacts.data.model.RecentType
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.components.getPhoneTypeLabel
import java.text.SimpleDateFormat
import java.util.Date
import com.bookverse.contacts.core.ui.R as CoreUiR

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentOperationItem(
    operation: RecentOperation,
    textColor: Color,
    isSelected: Boolean,
    accentColor: Color,
    onOperationClick: () -> Unit,
    onLongPress: () -> Unit,
    callHistory: CallHistoryDirectory,
    secondaryTextColor: Color,
    phoneActions: PhoneActionLauncher,
) {
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("HH:mm", LocaleController.effectiveLocale(context))
    val hasSavedContact = operation.contact.id.isNotBlank()
    val contactName = if (hasSavedContact) {
        operation.contact.name.ifBlank { stringResource(CoreUiR.string.unknown) }
    } else {
        val fallbackNumber = operation.contact.phones.firstOrNull()?.number.orEmpty()
        operation.target.ifBlank { fallbackNumber }
            .ifBlank { stringResource(CoreUiR.string.unknown) }
    }
    val (typeIcon, typeLabel) = recentTypePresentation(operation.type)
    val isMissed = operation.type == RecentType.CALL_MISSED
    val phoneType = remember(operation.contact.phones, operation.target) {
        operation.contact.phones.firstOrNull { phone ->
            recentNumbersMatch(phone.number, operation.target)
        }?.type ?: operation.contact.phones.firstOrNull()?.type
    }
    val phoneTypeLabel = getPhoneTypeLabel(phoneType)
    val timeLabel = timeFormat.format(Date(operation.date))
    val phoneNumber = remember(operation.target, operation.contact.phones) {
        operation.target.ifBlank {
            operation.contact.phones.firstOrNull()?.number.orEmpty()
        }
    }
    val errorColor = MaterialTheme.colorScheme.error
    val selectionColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(selectionColor)
            .combinedClickable(
                onClick = onOperationClick,
                onLongClick = onLongPress,
            )
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecentOperationAvatar(
            operation = operation,
            hasSavedContact = hasSavedContact,
            isSelected = isSelected,
            accentColor = accentColor,
            callHistory = callHistory,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contactName,
                color = if (isMissed) errorColor else textColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = typeLabel,
                    tint = if (isMissed) errorColor else secondaryTextColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = phoneTypeLabel,
                    color = secondaryTextColor,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = timeLabel,
            color = secondaryTextColor,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
        IconButton(
            onClick = {
                if (phoneNumber.isNotBlank()) {
                    phoneActions.callPhoneNumber(context, phoneNumber)
                }
            },
            enabled = phoneNumber.isNotBlank(),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Call,
                contentDescription = stringResource(CoreUiR.string.call),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun RecentOperationAvatar(
    operation: RecentOperation,
    hasSavedContact: Boolean,
    isSelected: Boolean,
    accentColor: Color,
    callHistory: CallHistoryDirectory,
) {
    Box(
        modifier = Modifier
            .size(HistoryAvatarSize)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.fillMaxSize(),
            )
            return@Box
        }
        val photoUri = operation.contact.photoUri
        val bitmap = remember(photoUri) {
            photoUri?.let(callHistory::uriToBitmap)
        }
        val initials = remember(operation.contact.name) { historyInitials(operation.contact.name) }
        when {
            bitmap != null -> Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            hasSavedContact && initials != null -> {
                val avatarColor = ContactDisplay.getRandomColor(operation.contact.name)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(avatarColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initials,
                        color = if (avatarColor.luminance() > 0.5f) Color.Black else Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal val HistoryAvatarSize = 44.dp

internal fun historyInitials(name: String): String? =
    name.split(' ')
        .mapNotNull { word -> word.firstOrNull(Char::isLetter)?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifEmpty { null }

@Composable
private fun recentTypePresentation(type: RecentType) = when (type) {
    RecentType.WHATSAPP_OPEN ->
        Icons.Default.Whatsapp to stringResource(CoreUiR.string.recent_type_whatsapp)
    RecentType.CALL_SENT ->
        Icons.AutoMirrored.Filled.CallMade to stringResource(CoreUiR.string.recent_type_call_sent)
    RecentType.CALL_RECEIVED ->
        Icons.AutoMirrored.Filled.CallReceived to stringResource(CoreUiR.string.recent_type_call_received)
    RecentType.CALL_MISSED ->
        Icons.AutoMirrored.Filled.CallMissed to stringResource(CoreUiR.string.recent_type_call_missed)
    RecentType.CALL_REJECTED ->
        Icons.AutoMirrored.Filled.CallMissedOutgoing to
            stringResource(CoreUiR.string.recent_type_call_rejected)
    RecentType.CALL_BLOCKED ->
        Icons.Filled.HideSource to stringResource(CoreUiR.string.recent_type_call_blocked)
    RecentType.CALL_UNKNOWN ->
        Icons.Default.DeviceUnknown to stringResource(CoreUiR.string.recent_type_call_unknown)
}

internal fun normalizeRecentPhoneNumber(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val normalized = PhoneNumberUtils.normalizeNumber(raw)
    if (!normalized.isNullOrBlank()) return normalized
    val digits = raw.replace("[^0-9+]".toRegex(), "")
    return digits.takeIf { it.isNotBlank() }
}

internal fun recentDigitsOnly(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return raw.filter(Char::isDigit).takeIf { it.isNotBlank() }
}

internal fun recentNumbersMatch(first: String?, second: String?): Boolean {
    val normalizedFirst = normalizeRecentPhoneNumber(first) ?: return false
    val normalizedSecond = normalizeRecentPhoneNumber(second) ?: return false
    if (normalizedFirst == normalizedSecond) return true
    val firstDigits = recentDigitsOnly(normalizedFirst) ?: return false
    val secondDigits = recentDigitsOnly(normalizedSecond) ?: return false
    return firstDigits.endsWith(secondDigits) || secondDigits.endsWith(firstDigits)
}

package com.bookverse.contacts.ui.screens.contacts

import android.telephony.PhoneNumberUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.feature.contacts.R
import com.bookverse.contacts.ui.components.ContactAvatarText
import com.bookverse.contacts.ui.theme.BadgeBackgroundColor
import com.bookverse.contacts.ui.theme.BadgeColor

internal val DetailHorizontalPadding = 24.dp
internal val DetailIconSize = 24.dp
internal val DetailIconGap = 20.dp
internal val DetailContentInset: Dp = DetailHorizontalPadding + DetailIconSize + DetailIconGap

@Composable
internal fun BlockedBadge() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BadgeBackgroundColor,
        border = BorderStroke(1.dp, BadgeColor.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = BadgeColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.contact_blocked_badge),
                color = BadgeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun ContactDetailsAvatar(
    photo: ImageBitmap?,
    initials: String,
    fallbackColor: Color,
    initialsColor: Color,
    onPhotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(112.dp)
            .clip(CircleShape)
            .background(fallbackColor),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onPhotoClick),
                contentScale = ContentScale.Crop,
            )
        } else {
            ContactAvatarText(
                text = initials,
                color = initialsColor,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun ContactQuickAction(
    icon: Painter,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    selected: Boolean? = null,
) {
    val colors = MaterialTheme.colorScheme
    val active = emphasized || selected == true
    val containerColor = if (active) colors.primaryContainer else colors.surfaceContainerHigh
    val contentColor = if (active) colors.onPrimaryContainer else colors.onSurface
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                if (selected != null) this.selected = selected
            }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(containerColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(DetailIconSize),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun DetailSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = DetailHorizontalPadding,
                end = DetailHorizontalPadding,
                top = 12.dp,
                bottom = 4.dp,
            ),
    )
}

@Composable
internal fun DetailSectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = DetailHorizontalPadding, vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DetailRow(
    headline: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    supporting: String? = null,
    headlineColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    headlineMaxLines: Int = Int.MAX_VALUE,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interactive = onClick != null || onLongClick != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (interactive) {
                    Modifier.combinedClickable(
                        onClick = onClick ?: {},
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                },
            )
            .heightIn(min = 56.dp)
            .padding(
                start = DetailHorizontalPadding,
                end = if (trailing != null) 12.dp else DetailHorizontalPadding,
                top = 10.dp,
                bottom = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(DetailIconSize),
            )
        } else {
            Spacer(modifier = Modifier.width(DetailIconSize))
        }
        Spacer(modifier = Modifier.width(DetailIconGap))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                color = headlineColor,
                maxLines = headlineMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

internal fun normalizeContactDetailsPhone(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val normalized = PhoneNumberUtils.normalizeNumber(raw)
    if (!normalized.isNullOrBlank()) return normalized
    val digitsOnly = raw.replace("[^0-9+]".toRegex(), "")
    return digitsOnly.takeIf { it.isNotBlank() }
}

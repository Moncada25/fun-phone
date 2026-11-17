package com.bookverse.contacts.ui.screens.contacts

import android.telephony.PhoneNumberUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.theme.BadgeBackgroundColor
import com.bookverse.contacts.ui.theme.BadgeColor

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

internal fun normalizeContactDetailsPhone(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val normalized = PhoneNumberUtils.normalizeNumber(raw)
    if (!normalized.isNullOrBlank()) return normalized
    val digitsOnly = raw.replace("[^0-9+]".toRegex(), "")
    return digitsOnly.takeIf { it.isNotBlank() }
}

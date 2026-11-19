package com.bookverse.contacts.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.data.model.SocialMediaType
import com.bookverse.contacts.feature.contacts.R
import com.bookverse.contacts.ui.screens.contacts.DetailRow
import com.bookverse.contacts.core.ui.R as CoreUiR

@Composable
fun SocialMediaField(
    socialNetworks: List<String>,
    onSocialNetworksChange: (List<String>) -> Unit,
    onNavigateToSearch: () -> Unit,
) {
    val neutralIconTint = MaterialTheme.colorScheme.onSurfaceVariant
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var indexSelected by remember { mutableIntStateOf(0) }
    val websiteFallback = stringResource(CoreUiR.string.website)
    val websiteIcon = rememberVectorPainter(Icons.Outlined.Public)

    DetailRow(
        icon = rememberVectorPainter(Icons.Outlined.AddLink),
        headline = stringResource(R.string.websites),
        supporting = stringResource(R.string.add_website),
        onClick = onNavigateToSearch,
    )

    socialNetworks.forEachIndexed { index, socialMediaUrl ->
        val iconRes = SocialMediaType.getSocialMediaIcon(socialMediaUrl)
        DetailRow(
            icon = if (iconRes == SocialMediaType.WEBSITE_ICON) {
                websiteIcon
            } else {
                painterResource(id = iconRes)
            },
            headline = SocialMediaType.getSocialMediaName(socialMediaUrl, websiteFallback),
            supporting = socialMediaUrl,
            headlineMaxLines = 1,
            trailing = {
                IconButton(
                    onClick = {
                        indexSelected = index
                        showDeleteConfirm = true
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(CoreUiR.string.delete),
                        tint = neutralIconTint,
                    )
                }
            },
        )
    }

    if (showDeleteConfirm && indexSelected in socialNetworks.indices) {
        ConfirmDialog(
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                val updatedList = socialNetworks.toMutableList()
                updatedList.removeAt(indexSelected)
                onSocialNetworksChange(updatedList)
            },
            title = stringResource(R.string.delete_social_media_title),
            message = stringResource(
                R.string.delete_social_media,
                SocialMediaType.getSocialMediaName(
                    socialNetworks[indexSelected],
                    stringResource(CoreUiR.string.unknown),
                )
            ),
            toastMessage = null,
        )
    }
}

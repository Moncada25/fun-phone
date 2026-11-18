package com.bookverse.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.data.model.SocialMediaType
import com.bookverse.contacts.feature.contacts.R
import com.bookverse.contacts.core.ui.R as CoreUiR

@Composable
fun SocialMediaField(
    socialNetworks: List<String>,
    onSocialNetworksChange: (List<String>) -> Unit,
    onNavigateToSearch: () -> Unit,) {
    val accentColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var indexSelected by remember { mutableIntStateOf(0) }

    ContactFormActionRow(
        icon = Icons.Default.AddLink,
        title = stringResource(R.string.websites),
        value = stringResource(R.string.add_website),
        accentColor = accentColor,
        onClick = onNavigateToSearch
    )

    if (socialNetworks.isNotEmpty()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.websites),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                socialNetworks.forEachIndexed { index, socialMediaUrl ->
                    val iconRes = SocialMediaType.getSocialMediaIcon(socialMediaUrl)
                    val networkColor = SocialMediaType.getSocialMediaColor(socialMediaUrl, accentColor)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) {
                                showDeleteConfirm = true
                                indexSelected = index
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = networkColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(networkColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (iconRes == SocialMediaType.WEBSITE_ICON) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.size(12.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = SocialMediaType.getSocialMediaName(
                                        socialMediaUrl,
                                        stringResource(CoreUiR.string.website)
                                    ),
                                    color = networkColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(CoreUiR.string.delete),
                                tint = networkColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (showDeleteConfirm) {
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
    }
}

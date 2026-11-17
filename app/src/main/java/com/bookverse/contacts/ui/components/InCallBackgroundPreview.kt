package com.bookverse.contacts.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.theme.CallEndColor
import com.bookverse.contacts.ui.theme.GradientScrimColor
import com.bookverse.contacts.utils.ContactUtils

@Composable
internal fun InCallBackgroundPreviewDialog(
    displayName: String,
    phoneLabel: String?,
    photoUri: Uri?,
    backgroundUri: Uri?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            InCallBackgroundPreview(
                displayName = displayName,
                phoneLabel = phoneLabel,
                photoUri = photoUri,
                backgroundUri = backgroundUri,
            )
        }
    }
}

@Composable
internal fun InCallBackgroundPreview(
    displayName: String,
    phoneLabel: String?,
    photoUri: Uri?,
    backgroundUri: Uri?,
    modifier: Modifier = Modifier,
) {
    var backgroundLoadFailed by remember(backgroundUri) { mutableStateOf(false) }
    var photoLoadFailed by remember(photoUri) { mutableStateOf(false) }
    val hasBackground = backgroundUri != null && !backgroundLoadFailed
    val primaryTextColor = if (hasBackground) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val secondaryTextColor = primaryTextColor.copy(alpha = 0.76f)
    val tertiaryTextColor = primaryTextColor.copy(alpha = 0.58f)
    val dialogDescription = stringResource(R.string.in_call_preview_title)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics { contentDescription = dialogDescription },
    ) {
        if (backgroundUri != null && !backgroundLoadFailed) {
            AsyncImage(
                model = backgroundUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { backgroundLoadFailed = true },
                onSuccess = { backgroundLoadFailed = false },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                GradientScrimColor.copy(alpha = 0.72f),
                                GradientScrimColor.copy(alpha = 0.58f),
                                GradientScrimColor.copy(alpha = 0.82f),
                            ),
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (!hasBackground) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .shadow(20.dp, CircleShape, clip = false)
                            .border(
                                width = if (hasBackground) 2.dp else 0.dp,
                                color = primaryTextColor.copy(alpha = 0.82f),
                                shape = CircleShape,
                            )
                            .clip(CircleShape)
                            .background(ContactUtils.getRandomColor(displayName)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (photoUri != null && !photoLoadFailed) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = stringResource(
                                    R.string.contact_photo_description,
                                    displayName,
                                ),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                placeholder = ColorPainter(
                                    ContactUtils.getRandomColor(displayName),
                                ),
                                error = ColorPainter(
                                    ContactUtils.getRandomColor(displayName),
                                ),
                                onError = { photoLoadFailed = true },
                                onSuccess = { photoLoadFailed = false },
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = stringResource(
                                    R.string.contact_no_photo_description,
                                    displayName,
                                ),
                                modifier = Modifier.fillMaxSize(0.64f),
                                tint = tertiaryTextColor,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                phoneLabel?.let { label ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.in_call),
                    style = MaterialTheme.typography.bodyLarge,
                    color = secondaryTextColor,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        if (hasBackground) {
                            GradientScrimColor.copy(alpha = 0.42f)
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                        },
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    PreviewInCallButton(
                        icon = Icons.Default.Mic,
                        label = stringResource(R.string.microphone_on),
                        textColor = primaryTextColor,
                        modifier = Modifier.weight(1f),
                    )
                    PreviewInCallButton(
                        icon = Icons.Default.PhoneInTalk,
                        label = stringResource(R.string.earpiece),
                        textColor = primaryTextColor,
                        modifier = Modifier.weight(1f),
                    )
                    PreviewInCallButton(
                        icon = Icons.Default.Dialpad,
                        label = stringResource(R.string.dialer),
                        textColor = primaryTextColor,
                        modifier = Modifier.weight(1f),
                    )
                    PreviewInCallButton(
                        icon = Icons.Default.MoreHoriz,
                        label = stringResource(R.string.more),
                        textColor = primaryTextColor,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                PreviewInCallButton(
                    icon = Icons.Default.CallEnd,
                    label = stringResource(R.string.hang_up),
                    textColor = primaryTextColor,
                    containerColor = CallEndColor,
                    isLarge = true,
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PreviewInCallButton(
    icon: ImageVector,
    label: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    isLarge: Boolean = false,
) {
    // Mirrors InCallScreen.ActionButton: unselected pills are translucent white over
    // the photo background, labels sit below in labelMedium centered (wrapping).
    val unselectedContainer = Color.White.copy(alpha = 0.22f)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor ?: unselectedContainer,
            ),
            modifier = Modifier
                .fillMaxWidth(if (isLarge) 0.5f else 1f)
                .height(if (isLarge) 70.dp else 64.dp)
                .clip(RoundedCornerShape(100)),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (containerColor != null) Color.White else textColor,
                modifier = Modifier.size(if (isLarge) 36.dp else 32.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = textColor,
        )
    }
}

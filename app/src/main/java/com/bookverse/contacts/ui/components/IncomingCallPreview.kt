package com.bookverse.contacts.ui.components

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.theme.CallAcceptColor
import com.bookverse.contacts.ui.theme.CallRejectColor
import com.bookverse.contacts.ui.theme.GradientScrimColor
import com.bookverse.contacts.ui.viewmodel.ThemeMode
import com.bookverse.contacts.utils.ContactUtils

@Composable
internal fun IncomingCallPreviewDialog(
    displayName: String,
    phoneLabel: String?,
    photoUri: Uri?,
    backgroundUri: Uri?,
    callAnswerStyle: String,
    themeMode: ThemeMode,
    accentColor: Color,
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
            IncomingCallPreview(
                displayName = displayName,
                phoneLabel = phoneLabel,
                photoUri = photoUri,
                backgroundUri = backgroundUri,
                callAnswerStyle = callAnswerStyle,
                themeMode = themeMode,
                accentColor = accentColor,
            )
        }
    }
}

@Composable
internal fun IncomingCallPreview(
    displayName: String,
    phoneLabel: String?,
    photoUri: Uri?,
    backgroundUri: Uri?,
    callAnswerStyle: String,
    themeMode: ThemeMode,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    var backgroundLoadFailed by remember(backgroundUri) { mutableStateOf(false) }
    var photoLoadFailed by remember(photoUri) { mutableStateOf(false) }
    val hasIncomingCallBackground = backgroundUri != null && !backgroundLoadFailed
    val primaryTextColor = if (hasIncomingCallBackground) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val secondaryTextColor = primaryTextColor.copy(alpha = 0.76f)
    val tertiaryTextColor = primaryTextColor.copy(alpha = 0.58f)
    val callLabel = stringResource(R.string.recent_type_call_received)
    val isDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val callBadgeAlpha = if (isDarkTheme) 0.18f else 0.12f
    val view = LocalView.current

    DisposableEffect(view, hasIncomingCallBackground, isDarkTheme) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousLightStatusBars = controller?.isAppearanceLightStatusBars
        val previousLightNavigationBars = controller?.isAppearanceLightNavigationBars
        if (controller != null) {
            val useLightSystemBarIcons = !hasIncomingCallBackground && !isDarkTheme
            controller.isAppearanceLightStatusBars = useLightSystemBarIcons
            controller.isAppearanceLightNavigationBars = useLightSystemBarIcons
        }
        onDispose {
            if (controller != null) {
                previousLightStatusBars?.let { controller.isAppearanceLightStatusBars = it }
                previousLightNavigationBars?.let {
                    controller.isAppearanceLightNavigationBars = it
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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

        val compactHeight = maxHeight < 600.dp || LocalDensity.current.fontScale >= 1.5f
        val horizontalPadding = if (maxWidth < 360.dp) 16.dp else 24.dp
        val verticalPadding = if (compactHeight) 16.dp else 40.dp
        val actionBottomPadding = if (compactHeight) 8.dp else 24.dp
        val identitySpacing = if (compactHeight) 4.dp else 12.dp
        val avatarSize = when {
            compactHeight -> 72.dp
            hasIncomingCallBackground -> 112.dp
            else -> 160.dp
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val badgeContentColor = if (hasIncomingCallBackground) Color.White else accentColor
            val badgeContainerColor = if (hasIncomingCallBackground) {
                GradientScrimColor.copy(alpha = 0.42f)
            } else {
                accentColor.copy(alpha = callBadgeAlpha)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeContainerColor)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PhoneCallback,
                    contentDescription = callLabel,
                    tint = badgeContentColor,
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = callLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = badgeContentColor,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        identitySpacing,
                        Alignment.CenterVertically,
                    ),
                ) {
                    if (!hasIncomingCallBackground) {
                        Box(
                            modifier = Modifier
                                .size(avatarSize)
                                .shadow(20.dp, CircleShape, clip = false)
                                .border(
                                    width = if (hasIncomingCallBackground) 2.dp else 0.dp,
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
                    }

                    Text(
                        text = displayName,
                        style = if (compactHeight) {
                            MaterialTheme.typography.headlineMedium
                        } else {
                            MaterialTheme.typography.headlineLarge
                        },
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    phoneLabel?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = secondaryTextColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (callAnswerStyle == "buttons") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = actionBottomPadding,
                            start = 24.dp,
                            end = 24.dp,
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PreviewCallAction(
                        icon = Icons.Default.CallEnd,
                        label = stringResource(R.string.reject),
                        color = CallRejectColor,
                        textColor = primaryTextColor,
                    )
                    PreviewCallAction(
                        icon = Icons.Default.Call,
                        label = stringResource(R.string.answer),
                        color = CallAcceptColor,
                        textColor = primaryTextColor,
                    )
                }
            } else {
                val onSurface = primaryTextColor
                val neutralKnob = MaterialTheme.colorScheme.surface
                val leftColor = CallRejectColor.copy(alpha = 0.22f)
                val rightColor = CallAcceptColor.copy(alpha = 0.22f)
                val trackBrush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to leftColor,
                        0.32f to leftColor.copy(alpha = leftColor.alpha * 0.7f),
                        0.5f to onSurface.copy(alpha = 0.16f),
                        0.68f to rightColor.copy(alpha = rightColor.alpha * 0.7f),
                        1f to rightColor,
                    ),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = actionBottomPadding),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .background(trackBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(R.string.reject),
                                color = onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(R.string.answer),
                                color = onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(12.dp, CircleShape, clip = false)
                                .clip(CircleShape)
                                .background(neutralKnob),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PhoneCallback,
                                contentDescription = stringResource(
                                    R.string.slide_to_answer_or_reject,
                                    displayName,
                                ),
                                tint = onSurface,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewCallAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    textColor: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

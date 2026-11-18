package com.bookverse.contacts.ui.screens.contacts.dialer

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.Whatsapp
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.R
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed
import com.bookverse.contacts.ui.components.LiquidGlassShape
import com.bookverse.contacts.ui.components.LocalAppBackdrop
import com.bookverse.contacts.ui.components.LiquidGlassSurface
import com.bookverse.contacts.ui.components.NumberKeypad
import com.bookverse.contacts.ui.theme.AppChrome
import com.bookverse.contacts.ui.theme.CallAcceptColor
import com.bookverse.contacts.ui.theme.AccentBarStyle

private val SheetContentMaxWidth = 480.dp
private val SheetHorizontalPadding = 16.dp
private val SheetTopPadding = 12.dp
private val SheetBottomPadding = 12.dp
private val NumberRowHeight = 64.dp
private val NumberRowGap = 6.dp
private val ActionRowGap = 14.dp
private val MinKeyHeight = 48.dp
private val MaxKeyHeight = 68.dp
private val KeyContentHeight = 42.dp
private const val MaxNumberFontScale = 1.3f

@Composable
internal fun DialerModernResults(
    modifier: Modifier = Modifier,
    topInset: Dp,
    bottomInset: Dp,
    dialedNumber: String,
    suggestionEntries: List<DialerSuggestionEntry>,
    filteredContacts: List<DialerContactSearchIndex>,
    unknownTitle: String,
    accentColor: Color,
    textColor: Color,
    loadContactPhoto: suspend (Uri) -> Bitmap?,
    onContactCall: (String) -> Unit,
    onAddContact: () -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        DialerResultsArea(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = topInset + 12.dp,
                bottom = bottomInset + 12.dp,
            ),
            modern = true,
            dialedNumber = dialedNumber,
            suggestionEntries = suggestionEntries,
            filteredContacts = filteredContacts,
            unknownTitle = unknownTitle,
            accentColor = accentColor,
            textColor = textColor,
            loadContactPhoto = loadContactPhoto,
            onContactCall = onContactCall,
            onAddContact = onAddContact,
        )
    }
}

@Composable
internal fun DialerModernKeypadSheet(
    modifier: Modifier = Modifier,
    accentColor: Color,
    fontType: String,
    dtmfTonesEnabled: String,
    vibrationDuration: Int,
    vibrationPattern: String,
    phoneFieldValue: TextFieldValue,
    dialedNumber: String,
    onValueChange: (TextFieldValue) -> Unit,
    registerEditText: (DialerEditText) -> Unit,
    onAddContact: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onDelete: () -> Unit,
    onLongDelete: () -> Unit,
    onDigit: (String) -> Unit,
    onOneLongPress: () -> Unit,
    onNumberLongPress: (Int) -> Unit,
    numberContext: DialerNumberContext = DialerNumberContext(),
    onCountryClick: (() -> Unit)? = null,
    onMatchedContactClick: (() -> Unit)? = null,
    bottomInset: Dp = 0.dp,
    onHeightChanged: (Dp) -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
    ) {
        val density = LocalDensity.current
        val colorScheme = MaterialTheme.colorScheme
        val keyGap = if (maxHeight < 680.dp) 6.dp else 8.dp
        val contentWidth = min(maxWidth, SheetContentMaxWidth)
        val fixedHeight = SheetTopPadding + NumberRowHeight + NumberRowGap + ActionRowGap +
            SheetBottomPadding + keyGap * 3
        val minKeyHeight = max(MinKeyHeight, KeyContentHeight * density.fontScale)
        val keyHeight = (((maxHeight - bottomInset) * 0.66f - fixedHeight) / 5).coerceIn(
            minKeyHeight,
            max(MaxKeyHeight, minKeyHeight),
        )
        val keyWidth = ((contentWidth - SheetHorizontalPadding * 2 - keyGap * 2) / 3).coerceAtLeast(0.dp)
        val maxSheetHeight = maxHeight
        val keyContainerColor = colorScheme.onSurface.copy(
            alpha = if (colorScheme.surface.luminance() < 0.5f) 0.10f else 0.06f,
        )

        CompositionLocalProvider(LocalAppBackdrop provides null) {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size -> onHeightChanged(with(density) { size.height.toDp() }) }
                    .pointerInput(Unit) {},
                shape = LiquidGlassShape.SHEET,
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = SheetContentMaxWidth)
                        .fillMaxWidth()
                        .heightIn(max = maxSheetHeight)
                        .clip(RoundedCornerShape(topStart = AppChrome.sheetCorner, topEnd = AppChrome.sheetCorner))
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = SheetHorizontalPadding,
                            end = SheetHorizontalPadding,
                            top = SheetTopPadding,
                            bottom = SheetBottomPadding + bottomInset,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DialerNumberContextRow(
                        context = numberContext,
                        modifier = Modifier.fillMaxWidth(),
                        onCountryClick = onCountryClick,
                        onContactClick = onMatchedContactClick,
                    )
                    DialerModernNumberRow(
                        accentColor = accentColor,
                        phoneFieldValue = phoneFieldValue,
                        dialedNumber = dialedNumber,
                        onValueChange = onValueChange,
                        registerEditText = registerEditText,
                        onAddContact = onAddContact,
                        onDelete = onDelete,
                        onLongDelete = onLongDelete,
                    )
                    Spacer(modifier = Modifier.height(NumberRowGap))
                    NumberKeypad(
                        modifier = Modifier.fillMaxWidth(),
                        onKeyPressed = onDigit,
                        onOneLongPress = onOneLongPress,
                        onNumberLongPress = onNumberLongPress,
                        vibrationDuration = vibrationDuration,
                        vibrationPattern = vibrationPattern,
                        accentColor = accentColor,
                        fontType = fontType,
                        accentBarStyle = AccentBarStyle.MODERN,
                        dtmfTonesEnabled = dtmfTonesEnabled,
                        buttonSize = keyHeight,
                        keySpacing = keyGap,
                        keyContainerColor = keyContainerColor,
                        digitFontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(ActionRowGap))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(keyGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DialerModernPressable(
                            modifier = Modifier
                                .width(keyWidth)
                                .height(keyHeight),
                            label = stringResource(R.string.whatsapp_action),
                            containerColor = keyContainerColor,
                            contentColor = colorScheme.onSurface,
                            onClick = onWhatsApp,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Whatsapp,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        DialerModernPressable(
                            modifier = Modifier
                                .weight(1f)
                                .height(keyHeight),
                            label = null,
                            containerColor = CallAcceptColor,
                            contentColor = contentColorFor(CallAcceptColor),
                            onClick = onCall,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Call,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                                Text(
                                    text = stringResource(R.string.call),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialerModernNumberRow(
    accentColor: Color,
    phoneFieldValue: TextFieldValue,
    dialedNumber: String,
    onValueChange: (TextFieldValue) -> Unit,
    registerEditText: (DialerEditText) -> Unit,
    onAddContact: () -> Unit,
    onDelete: () -> Unit,
    onLongDelete: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val hasNumber = dialedNumber.isNotEmpty()
    val numberTextSize = when {
        dialedNumber.length <= 11 -> 34.sp
        dialedNumber.length <= 14 -> 30.sp
        dialedNumber.length <= 17 -> 26.sp
        else -> 22.sp
    }
    val sideAlpha by animateFloatAsState(
        targetValue = if (hasNumber) 1f else 0f,
        animationSpec = tween(LocalAnimationSpeed.current.durationMs.coerceAtMost(180)),
        label = "Dialer number actions",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(NumberRowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DialerModernIconButton(
            modifier = Modifier.graphicsLayer { alpha = sideAlpha },
            icon = Icons.Outlined.PersonAddAlt1,
            label = stringResource(R.string.add_contact),
            enabled = hasNumber,
            onClick = onAddContact,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(NumberRowHeight),
            contentAlignment = Alignment.Center,
        ) {
            DialerNumberEditor(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NumberRowHeight)
                    .focusable(true),
                value = phoneFieldValue,
                onValueChange = onValueChange,
                registerEditText = registerEditText,
                textColor = colorScheme.onSurface.toArgb(),
                textSizePx = numberTextSize.value * density.density *
                    density.fontScale.coerceAtMost(MaxNumberFontScale),
                cursorColor = accentColor.toArgb(),
                selectionBackgroundColor = accentColor.copy(alpha = 0.4f).toArgb(),
            )
            if (!hasNumber) {
                Text(
                    text = stringResource(R.string.enter_a_number),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface.copy(alpha = 0.42f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        DialerModernIconButton(
            modifier = Modifier.graphicsLayer { alpha = sideAlpha },
            icon = Icons.AutoMirrored.Outlined.Backspace,
            label = stringResource(R.string.delete),
            enabled = hasNumber,
            onClick = onDelete,
            onLongClick = onLongDelete,
        )
    }
}

@Composable
private fun DialerModernIconButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(AppChrome.touchTarget)
            .clip(CircleShape)
            .then(
                if (enabled) {
                    Modifier.combinedClickable(
                        role = Role.Button,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.Button
                if (!enabled) {
                    disabled()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun DialerModernPressable(
    label: String?,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(LocalAnimationSpeed.current.durationMs.coerceAtMost(160)),
        label = "Dialer action press",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .then(
                if (label != null) {
                    Modifier.semantics { contentDescription = label }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

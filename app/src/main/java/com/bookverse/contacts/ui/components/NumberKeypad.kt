package com.bookverse.contacts.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.bookverse.contacts.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.settings.Fonts
import com.bookverse.contacts.ui.theme.chromePolicy
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.utils.VibrationUtils

@Composable
fun NumberKeypad(
    onKeyPressed: (String) -> Unit,
    onKeyDown: ((String) -> Unit)? = null,
    onKeyUp: ((String) -> Unit)? = null,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onOneLongPress: () -> Unit = {},
    onNumberLongPress: ((Int) -> Unit)? = null,
    vibrationDuration: Int = 30,
    vibrationPattern: String = "default",
    fontType: String = "default",
    accentBarStyle: AccentBarStyle = AccentBarStyle.MINIMAL,
    dtmfTonesEnabled: String = "disabled",
    buttonSize: Dp? = null,
    keySpacing: Dp = 8.dp,
    keyContainerColor: Color? = null,
    digitFontWeight: FontWeight = FontWeight.Bold,
) {
    val keypadButtons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(keySpacing)
    ) {
        keypadButtons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(keySpacing)
            ) {
                row.forEach { digit ->
                    val numberValue = digit.toIntOrNull()
                    val longPressHandler = when {
                        digit == "1" -> onOneLongPress
                        numberValue != null && onNumberLongPress != null -> {
                            { onNumberLongPress(numberValue) }
                        }
                        else -> null
                    }

                    NumberKeypadButton(
                        text = digit,
                        subText = when (digit) {
                            "1" -> "~"
                            "2" -> "ABC"
                            "3" -> "DEF"
                            "4" -> "GHI"
                            "5" -> "JKL"
                            "6" -> "MNO"
                            "7" -> "PQRS"
                            "8" -> "TUV"
                            "9" -> "WXYZ"
                            "0" -> ""
                            else -> ""
                        },
                        onClick = { onKeyPressed(digit) },
                        onKeyDown = onKeyDown?.let { callback -> { callback(digit) } },
                        onKeyUp = onKeyUp?.let { callback -> { callback(digit) } },
                        onLongPress = longPressHandler,
                        vibrationDuration = vibrationDuration,
                        vibrationPattern = vibrationPattern,
                        accentColor = accentColor,
                        fontType = fontType,
                        accentBarStyle = accentBarStyle,
                        dtmfTonesEnabled = dtmfTonesEnabled,
                        buttonSize = buttonSize,
                        keyContainerColor = keyContainerColor,
                        digitFontWeight = digitFontWeight,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.NumberKeypadButton(
    text: String,
    subText: String,
    onClick: () -> Unit,
    onKeyDown: (() -> Unit)?,
    onKeyUp: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
    vibrationDuration: Int,
    vibrationPattern: String,
    accentColor: Color,
    fontType: String,
    accentBarStyle: AccentBarStyle,
    dtmfTonesEnabled: String,
    buttonSize: Dp?,
    keyContainerColor: Color?,
    digitFontWeight: FontWeight,
) {
    var isPressed by remember { mutableStateOf(false) }
    val textColor = MaterialTheme.colorScheme.onBackground
    val context = LocalContext.current
    val resolvedButtonSize = buttonSize ?: if (Fonts.needsLargeButton(fontType)) 70.dp else 60.dp

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnKeyDown by rememberUpdatedState(onKeyDown)
    val currentOnKeyUp by rememberUpdatedState(onKeyUp)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentVibrationDuration by rememberUpdatedState(vibrationDuration)
    val currentVibrationPattern by rememberUpdatedState(vibrationPattern)
    val currentDtmfEnabled by rememberUpdatedState(dtmfTonesEnabled)

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "keypad_button_scale"
    )

    val isMinimal = accentBarStyle.chromePolicy.neutralControls
    val buttonShape = CircleShape
    val backgroundColor = if (isMinimal) {
        if (isPressed) accentColor.copy(alpha = 0.5f) else keyContainerColor ?: Color.Transparent
    } else {
        if (isPressed) accentColor else accentColor.copy(alpha = 0.2f)
    }

    val sizeModifier = if (isMinimal) {
        Modifier.size(resolvedButtonSize)
    } else {
        Modifier.height(resolvedButtonSize)
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .then(sizeModifier)
            .clip(buttonShape)
            .background(backgroundColor, shape = buttonShape)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        VibrationUtils.vibrate(context, currentVibrationDuration, currentVibrationPattern)
                        if (currentDtmfEnabled == "enabled") {
                            com.bookverse.contacts.utils.DTMFTonePlayer.playTone(text)
                        }
                        currentOnKeyDown?.invoke()
                        try {
                            tryAwaitRelease()
                        } finally {
                            currentOnKeyUp?.invoke()
                            isPressed = false
                        }
                    },
                    onTap = { currentOnClick() },
                    onLongPress = if (onLongPress != null) {
                        {
                            VibrationUtils.vibrate(context, currentVibrationDuration, currentVibrationPattern)
                            currentOnLongPress?.invoke()
                        }
                    } else null
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = text,
                fontSize = if (Fonts.needsLargeButton(fontType)) 24.sp else 28.sp,
                fontWeight = digitFontWeight,
                color = textColor,
                lineHeight = 1.sp
            )
            if (text == "1") {
                Icon(
                    imageVector = Icons.Default.Voicemail,
                    contentDescription = stringResource(R.string.voicemail),
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.height(if (Fonts.needsLargeButton(fontType)) 14.dp else 18.dp)
                )
            } else if (subText.isNotEmpty()) {
                Text(
                    text = subText,
                    fontSize = if (Fonts.needsLargeButton(fontType)) 10.sp else 12.sp,
                    color = textColor.copy(alpha = 0.7f),
                    lineHeight = 1.sp
                )
            }
        }
    }
}

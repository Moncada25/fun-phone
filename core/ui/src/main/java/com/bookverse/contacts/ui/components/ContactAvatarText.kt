package com.bookverse.contacts.ui.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Composable
fun ContactAvatarText(
    text: String,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontSize: TextUnit = style.fontSize,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center,
        maxLines = 1,
        lineHeight = 1.em,
        autoSize = TextAutoSize.StepBased(minFontSize = 1.sp, maxFontSize = fontSize),
    )
}

package com.bookverse.contacts.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.ui.theme.AppChrome
import com.bookverse.contacts.core.ui.R as CoreUiR

private const val ARCADE_LIGHT_SPACING = 2.4f

@Composable
fun ArcadeLightStrip(
    modifier: Modifier = Modifier,
    litColor: Color = MaterialTheme.colorScheme.primary,
    dimColor: Color = MaterialTheme.colorScheme.outlineVariant,
    lightSize: Dp = 6.dp,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(lightSize)
            .clearAndSetSemantics {},
    ) {
        val light = size.height
        val step = light * ARCADE_LIGHT_SPACING
        val count = ((size.width + step - light) / step).toInt().coerceAtLeast(1)
        val used = light + step * (count - 1)
        val start = (size.width - used) / 2f
        repeat(count) { index ->
            drawRoundRect(
                color = if (index % 2 == 0) litColor else dimColor,
                topLeft = Offset(start + step * index, 0f),
                size = Size(light, light),
                cornerRadius = CornerRadius(light / 4f),
            )
        }
    }
}

fun Modifier.crtScanlines(
    lineColor: Color = Color.Black.copy(alpha = 0.18f),
    spacing: Dp = 3.dp,
): Modifier = drawWithContent {
    drawContent()
    val step = spacing.toPx().coerceAtLeast(2f)
    val stroke = (step / 3f).coerceAtLeast(1f)
    var y = 0f
    while (y < size.height) {
        drawRect(color = lineColor, topLeft = Offset(0f, y), size = Size(size.width, stroke))
        y += step
    }
}

val arcadeLabelStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp,
    )

val arcadeScoreStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
    )

@Composable
fun ArcadeDialogHeader(
    title: String,
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onClose != null) {
                Box(modifier = Modifier.width(AppChrome.touchTarget))
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (eyebrow != null) {
                    Text(
                        text = eyebrow,
                        style = arcadeLabelStyle,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(AppChrome.touchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(CoreUiR.string.close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        ArcadeLightStrip(modifier = Modifier.padding(horizontal = 8.dp))
    }
}

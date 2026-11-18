package com.bookverse.contacts.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.settings.GlassLevel
import com.bookverse.contacts.ui.theme.AppChrome

enum class LiquidGlassShape { CAPSULE, TOP_BAR, SHEET }

/** The optical layer is separate from the sharp, accessible foreground controls. */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: LiquidGlassShape = LiquidGlassShape.CAPSULE,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val backdrop = LocalAppBackdrop.current != null
    val refraction = backdrop && rememberGlassLevel() == GlassLevel.FULL
    val topBar = shape == LiquidGlassShape.TOP_BAR
    val sheet = shape == LiquidGlassShape.SHEET
    val outline = when (shape) {
        LiquidGlassShape.TOP_BAR -> RectangleShape
        LiquidGlassShape.SHEET -> RoundedCornerShape(topStart = AppChrome.sheetCorner, topEnd = AppChrome.sheetCorner)
        LiquidGlassShape.CAPSULE -> CircleShape
    }
    val sheetCornerPx = with(LocalDensity.current) { AppChrome.sheetCorner.toPx() }
    val glassColor = if (topBar) scheme.background else if (dark) scheme.surfaceContainer else scheme.surfaceContainerHigh
    Box(
        modifier.shadow(
            elevation = if (topBar) 0.dp else 8.dp, shape = outline, clip = false,
            ambientColor = scheme.scrim.copy(alpha = 0.12f),
            spotColor = scheme.scrim.copy(alpha = 0.20f),
        ),
    ) {
        Box(
            Modifier.matchParentSize().clip(outline)
                .glassRefraction(enabled = refraction, shape = shape, sheetCornerPx = sheetCornerPx)
                .appBackdropSurface(
                    color = glassColor,
                    tintAlpha = if (topBar) 0.80f else if (dark) 0.76f else 0.58f,
                    blurRadius = if (topBar) 16.dp else 10.dp,
                ),
        )
        Box(
            Modifier.matchParentSize()
                .then(if (sheet) Modifier.clip(outline) else Modifier)
                .glassFinish(dark, shape, scheme.scrim, sheetCornerPx),
        )
        content()
    }
}

@Composable
private fun Modifier.glassRefraction(enabled: Boolean, shape: LiquidGlassShape, sheetCornerPx: Float): Modifier =
    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) refractedGlassLayer(shape, sheetCornerPx) else this

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Modifier.refractedGlassLayer(shape: LiquidGlassShape, sheetCornerPx: Float): Modifier {
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current.density
    val shader = remember { RuntimeShader(GLASS_REFRACTION) }
    val effect = remember(bounds, density, shape, sheetCornerPx) {
        if (bounds.width == 0 || bounds.height == 0) null else {
            // A sheet is flush with the bottom edge: its lower corners sit below the layer.
            val height = bounds.height + if (shape == LiquidGlassShape.SHEET) sheetCornerPx * 2f else 0f
            val capsuleRadius = minOf(bounds.width.toFloat(), height) / 2f
            shader.setFloatUniform("resolution", bounds.width.toFloat(), height)
            shader.setFloatUniform(
                "cornerRadius",
                when (shape) {
                    LiquidGlassShape.TOP_BAR -> 0f
                    LiquidGlassShape.SHEET -> sheetCornerPx.coerceAtMost(capsuleRadius)
                    LiquidGlassShape.CAPSULE -> capsuleRadius
                },
            )
            shader.setFloatUniform("edgeWidth", 18f * density)
            shader.setFloatUniform("refraction", 6f * density)
            RenderEffect.createRuntimeShaderEffect(shader, "backdrop").asComposeRenderEffect()
        }
    }
    return onSizeChanged { bounds = it }.graphicsLayer { renderEffect = effect }
}

private fun Modifier.glassFinish(
    dark: Boolean,
    shape: LiquidGlassShape,
    shade: Color,
    sheetCornerPx: Float,
): Modifier = drawWithCache {
    val topBar = shape == LiquidGlassShape.TOP_BAR
    val sheet = shape == LiquidGlassShape.SHEET
    val bodySize = if (sheet) Size(size.width, size.height + sheetCornerPx * 2f) else size
    val radius = when (shape) {
        LiquidGlassShape.TOP_BAR -> CornerRadius.Zero
        LiquidGlassShape.SHEET -> CornerRadius(sheetCornerPx.coerceAtMost(bodySize.minDimension / 2f))
        LiquidGlassShape.CAPSULE -> CornerRadius(size.minDimension / 2f)
    }
    val stroke = (if (topBar) 0.8.dp else 1.1.dp).toPx()
    // Normalize the light direction so long capsules do not illuminate both end caps completely.
    val highlightEnd = if (topBar || sheet) Offset(size.width, 0f) else {
        val direction = Offset(1f / size.width.coerceAtLeast(1f), 1f / size.height.coerceAtLeast(1f))
        direction * (2f / direction.getDistanceSquared())
    }
    // Two opposing glints fade completely between highlights instead of outlining the surface.
    val highlight = Brush.linearGradient(
        0f to Color.White.copy(alpha = if (dark) 0.66f else 0.95f),
        0.12f to Color.White.copy(alpha = if (dark) 0.32f else 0.60f),
        0.30f to Color.Transparent,
        0.70f to Color.Transparent,
        0.88f to Color.White.copy(alpha = if (dark) 0.24f else 0.50f),
        1f to Color.White.copy(alpha = if (dark) 0.50f else 0.85f),
        start = Offset.Zero, end = highlightEnd,
    )
    val sheen = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (topBar) 0.02f else if (dark) 0.06f else 0.14f), Color.Transparent),
    )
    // Refraction alone cannot give a uniform backdrop depth. Broad absorption and reflection
    // keep white-on-white glass legible without adding an inset ring or blurring its controls.
    val depth = if (topBar) 0.012f else if (dark) 0.025f else 0.075f
    val absorption = Brush.verticalGradient(
        0f to shade.copy(alpha = depth * 0.3f),
        0.5f to shade.copy(alpha = depth),
        1f to shade.copy(alpha = depth * 0.45f),
    )
    // A sheet catches light only along its exposed top edge and corners.
    val sheetEdge = if (sheet) {
        val inset = stroke / 2f
        val arc = (radius.x - inset).coerceAtLeast(0f) * 2f
        Path().apply {
            moveTo(inset, inset + arc / 2f)
            arcTo(Rect(inset, inset, inset + arc, inset + arc), 180f, 90f, false)
            lineTo(size.width - inset - arc / 2f, inset)
            arcTo(Rect(size.width - inset - arc, inset, size.width - inset, inset + arc), 270f, 90f, false)
        }
    } else {
        null
    }
    onDrawBehind {
        drawRoundRect(absorption, size = bodySize, cornerRadius = radius)
        drawRoundRect(sheen, size = bodySize, cornerRadius = radius)
        if (sheetEdge != null) {
            drawPath(sheetEdge, highlight, alpha = 0.12f, style = Stroke(3.dp.toPx()))
            drawPath(sheetEdge, highlight, style = Stroke(stroke))
        } else if (topBar) {
            // A full-bleed header catches light only along its exposed lower edge.
            drawLine(highlight, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), strokeWidth = stroke, alpha = 0.24f)
        } else {
            drawRoundRect(
                brush = highlight,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size((size.width - stroke).coerceAtLeast(0f), (size.height - stroke).coerceAtLeast(0f)),
                cornerRadius = radius,
                alpha = 0.12f,
                style = Stroke(3.dp.toPx()),
            )
            drawRoundRect(
                brush = highlight,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size((size.width - stroke).coerceAtLeast(0f), (size.height - stroke).coerceAtLeast(0f)),
                cornerRadius = radius,
                style = Stroke(stroke),
            )
        }
    }
}

private const val GLASS_REFRACTION = """
uniform shader backdrop;
uniform float2 resolution;
uniform float cornerRadius;
uniform float edgeWidth;
uniform float refraction;

half4 main(float2 position) {
    float2 halfSize = resolution * 0.5;
    float radius = cornerRadius;
    float2 local = position - halfSize;
    float2 core = max(halfSize - radius, float2(0.0));
    float2 q = abs(local) - core;
    float distance = length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
    float edge = 1.0 - smoothstep(0.0, edgeWidth, -distance);
    float2 outside = local - clamp(local, -core, core);
    float2 normal = length(outside) > 0.001 ? normalize(outside)
        : (q.x > q.y ? float2(sign(local.x), 0.0) : float2(0.0, sign(local.y)));
    float2 sampleAt = position - normal * refraction * edge * edge;
    sampleAt = clamp(sampleAt, float2(0.5), resolution - float2(0.5));
    return backdrop.eval(sampleAt);
}
"""

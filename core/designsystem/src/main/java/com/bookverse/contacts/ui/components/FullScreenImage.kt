package com.bookverse.contacts.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

@Composable
fun FullScreenImage(
    bitmap: ImageBitmap,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val radiusAnim = remember { Animatable(0f) }
    var dismissing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val maxRadius = if (containerSize.width == 0 || containerSize.height == 0) 0f else hypot(
        containerSize.width.toDouble(),
        containerSize.height.toDouble()
    ).toFloat() / 2f

    LaunchedEffect(Unit) {
        radiusAnim.snapTo(0f)
        radiusAnim.animateTo(1f, animationSpec = tween(450, easing = FastOutSlowInEasing))
    }

    fun triggerDismiss() {
        if (dismissing) return
        dismissing = true
        scope.launch {
            radiusAnim.animateTo(0f, animationSpec = tween(350, easing = FastOutSlowInEasing))
            onDismiss()
        }
    }

    BackHandler { triggerDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .background(Color.Black.copy(alpha = 0.95f * radiusAnim.value)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (dismissing) return@detectTransformGestures
                        val prevScale = scale
                        scale = (scale * zoom).coerceIn(1f, 3f)
                        val scaleFactor = scale / prevScale
                        offsetX = offsetX * scaleFactor
                        offsetY = offsetY * scaleFactor
                        if (scale > 1f) {
                            val maxOffsetX = max(0f, (scale - 1f) * size.width / 2)
                            val maxOffsetY = max(0f, (scale - 1f) * size.height / 2)
                            offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapPosition ->
                            if (dismissing) return@detectTapGestures
                            if (scale > 1.1f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 1.5f
                                val tapX = tapPosition.x - size.width / 2
                                val tapY = tapPosition.y - size.height / 2
                                offsetX = -tapX * 0.5f
                                offsetY = -tapY * 0.5f
                                val maxOffsetX = max(0f, (scale - 1f) * size.width / 2)
                                val maxOffsetY = max(0f, (scale - 1f) * size.height / 2)
                                offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                            }
                        },
                        onTap = { triggerDismiss() }
                    )
                }
                .drawWithCircularReveal(radiusAnim.value, maxRadius),
            contentScale = ContentScale.Fit
        )
    }
}

private fun Modifier.drawWithCircularReveal(progress: Float, maxRadius: Float): Modifier =
    drawWithContent {
        if (maxRadius > 0f) {
            val currentRadius = progress * maxRadius
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - currentRadius, center.y - currentRadius)
            val rect = Rect(topLeft, Size(currentRadius * 2, currentRadius * 2))
            val path = Path().apply { addOval(rect) }
            clipPath(
                path,
                clipOp = androidx.compose.ui.graphics.ClipOp.Intersect
            ) { this@drawWithContent.drawContent() }
        }
    }

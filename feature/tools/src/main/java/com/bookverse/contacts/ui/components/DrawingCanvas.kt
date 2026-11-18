package com.bookverse.contacts.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.bookverse.contacts.ui.viewmodel.DrawingElement
import com.bookverse.contacts.ui.viewmodel.DrawingInputMode
import com.bookverse.contacts.ui.viewmodel.DrawingAction
import com.bookverse.contacts.ui.viewmodel.DrawingState
import com.bookverse.contacts.ui.viewmodel.PathData
import com.bookverse.contacts.ui.viewmodel.PathType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.launch
import android.graphics.Paint as AndroidPaint

@Composable
fun DrawingCanvas(
    state: DrawingState,
    onAction: (DrawingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentPath = state.currentPath
    val pendingText = state.pendingText
    val pendingShape = state.pendingShape
    val importedImage = state.importedImage
    val isImagePlacementMode = state.isImagePlacementMode
    val canvasColor =
        state.canvasColor.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.background
    val graphicsLayer = rememberGraphicsLayer()
    val blurPoints = remember { mutableStateListOf<Offset>() }
    val scope = rememberCoroutineScope()

    var canvasModifier = modifier
        .clipToBounds()

    when {
        isImagePlacementMode && importedImage != null -> {
            canvasModifier = canvasModifier.pointerInput(importedImage.bitmap) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    onAction(
                        DrawingAction.OnUpdateImageTransform(
                            pan = pan,
                            zoom = zoom,
                            centroid = centroid,
                            rotation = rotation
                        )
                    )
                }
            }
        }

        pendingShape != null -> {
            canvasModifier = canvasModifier.pointerInput(pendingShape.id) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    onAction(
                        DrawingAction.OnUpdatePendingShapeTransform(
                            pan = pan,
                            zoom = zoom,
                            centroid = centroid,
                            rotation = rotation
                        )
                    )
                }
            }
        }

        pendingText != null -> {
            canvasModifier = canvasModifier.pointerInput(pendingText.id) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    onAction(
                        DrawingAction.OnUpdatePendingTextTransform(
                            pan = pan,
                            zoom = zoom,
                            centroid = centroid,
                            rotation = rotation
                        )
                    )
                }
            }
        }

        state.inputMode == DrawingInputMode.BLUR && !state.isBlurProcessing -> {
            canvasModifier = canvasModifier
                .pointerInput(state.inputMode, state.blurBrushSize) {
                    detectTapGestures { offset ->
                        blurPoints.clear()
                        blurPoints.add(offset)
                        scope.launch {
                            val snapshot = graphicsLayer.toImageBitmap()
                            onAction(DrawingAction.OnCommitBlur(snapshot, listOf(offset)))
                            blurPoints.clear()
                        }
                    }
                }
                .pointerInput(state.inputMode, state.blurBrushSize) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            blurPoints.clear()
                            blurPoints.add(offset)
                        },
                        onDragEnd = {
                            val completedPoints = blurPoints.toList()
                            scope.launch {
                                val snapshot = graphicsLayer.toImageBitmap()
                                onAction(DrawingAction.OnCommitBlur(snapshot, completedPoints))
                                blurPoints.clear()
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            blurPoints.add(change.position)
                        },
                        onDragCancel = {
                            blurPoints.clear()
                        },
                    )
                }
        }

        state.inputMode in setOf(DrawingInputMode.PENCIL, DrawingInputMode.ERASER) -> {
            canvasModifier = canvasModifier
                .pointerInput(state.inputMode) {
                    detectTapGestures { offset ->
                        onAction(DrawingAction.OnNewPathStart)
                        onAction(DrawingAction.OnDraw(offset))
                        onAction(DrawingAction.OnPathEnd)
                    }
                }
                .pointerInput(state.inputMode) {
                    detectDragGestures(
                        onDragStart = { onAction(DrawingAction.OnNewPathStart) },
                        onDragEnd = { onAction(DrawingAction.OnPathEnd) },
                        onDrag = { change, _ -> onAction(DrawingAction.OnDraw(change.position)) },
                        onDragCancel = { onAction(DrawingAction.OnPathEnd) },
                    )
                }
        }
    }

    Box(modifier = canvasModifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    graphicsLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(graphicsLayer)
                },
        ) {
            drawRect(canvasColor)
            val textPaint = AndroidPaint().apply {
                isAntiAlias = true
                style = AndroidPaint.Style.FILL
            }
            state.elements.forEach { element ->
                drawElement(element, textPaint)
            }
            importedImage?.let { imageState ->
                drawTransformedImage(
                    bitmap = imageState.bitmap,
                    offset = imageState.offset,
                    scale = imageState.scale,
                    rotation = imageState.rotation,
                )
            }
            pendingShape?.let { shape ->
                drawTransformedShape(
                    type = shape.type,
                    color = shape.color.copy(alpha = 0.7f),
                    brushSize = shape.brushSize,
                    isFilled = shape.isFilled,
                    offset = shape.offset,
                    scale = shape.scale,
                    rotation = shape.rotation,
                )
            }
            currentPath?.let(::drawPathData)
            pendingText?.let { entry ->
                textPaint.color = entry.color.copy(alpha = 0.6f).toArgb()
                textPaint.textSize = entry.size
                val previewPosition = if (entry.position != Offset.Unspecified) {
                    entry.position
                } else {
                    Offset(size.width / 2f, size.height / 2f)
                }
                withTransform({
                    translate(previewPosition.x, previewPosition.y)
                    rotate(degrees = entry.rotation)
                }) {
                    drawMultilineText(
                        text = entry.text,
                        paint = textPaint,
                        backgroundColor = entry.backgroundColor?.copy(alpha = 0.6f),
                    )
                }
            }
        }
        if (blurPoints.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawBlurPreview(blurPoints, state.blurBrushSize)
            }
        }
    }
}

private fun DrawScope.drawElement(element: DrawingElement, textPaint: AndroidPaint) {
    when (element) {
        is DrawingElement.Path -> drawPathData(element.value)
        is DrawingElement.Text -> {
            val entry = element.value
            textPaint.color = entry.color.toArgb()
            textPaint.textSize = entry.size
            withTransform({
                translate(entry.position.x, entry.position.y)
                rotate(degrees = entry.rotation)
            }) {
                drawMultilineText(entry.text, textPaint, entry.backgroundColor)
            }
        }
        is DrawingElement.Image -> with(element.value) {
            drawTransformedImage(bitmap, offset, scale, rotation)
        }
        is DrawingElement.Shape -> with(element.value) {
            drawTransformedShape(type, color, brushSize, isFilled, offset, scale, rotation)
        }
        is DrawingElement.Blur -> drawImage(
            image = element.value.bitmap,
            topLeft = element.value.offset,
        )
    }
}

private fun DrawScope.drawPathData(pathData: PathData) {
    when (pathData.type) {
        PathType.FREEHAND -> drawFreehandPath(pathData)
        PathType.CIRCLE -> drawCircle(pathData)
        PathType.SQUARE -> drawSquare(pathData)
        PathType.RECTANGLE -> drawRectangle(pathData)
        PathType.TRIANGLE -> drawTriangle(pathData)
        PathType.HEART -> drawHeart(pathData)
        PathType.STAR -> drawStar(pathData)
    }
}

private fun DrawScope.drawBlurPreview(points: List<Offset>, brushSize: Float) {
    val preview = Color.White.copy(alpha = 0.32f)
    if (points.size == 1) {
        drawCircle(preview, radius = brushSize / 2f, center = points.first())
        return
    }
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path = path,
        color = preview,
        style = Stroke(
            width = brushSize,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

private fun DrawScope.drawTransformedImage(
    bitmap: ImageBitmap,
    offset: Offset,
    scale: Float,
    rotation: Float
) {
    val safeScale = scale.coerceAtLeast(0.001f)
    val imageCenter = Offset(
        bitmap.width / 2f,
        bitmap.height / 2f
    )
    withTransform(
        transformBlock = {
            translate(offset.x, offset.y)
            rotate(degrees = rotation, pivot = imageCenter)
            scale(
                scaleX = safeScale,
                scaleY = safeScale,
                pivot = imageCenter
            )
        }
    ) {
        drawImage(bitmap)
    }
}

private fun DrawScope.drawTransformedShape(
    type: PathType,
    color: Color,
    brushSize: Float,
    isFilled: Boolean,
    offset: Offset,
    scale: Float,
    rotation: Float
) {
    val baseSize = 200f
    val safeScale = scale.coerceAtLeast(0.001f)
    val shapeCenter = Offset(baseSize / 2f, baseSize / 2f)

    withTransform(
        transformBlock = {
            translate(offset.x, offset.y)
            rotate(degrees = rotation, pivot = shapeCenter)
            scale(
                scaleX = safeScale,
                scaleY = safeScale,
                pivot = shapeCenter
            )
        }
    ) {
        val pathData = PathData(
            id = "preview",
            color = color,
            brushSize = brushSize,
            path = listOf(Offset.Zero, Offset(baseSize, baseSize)),
            type = type,
            brushStyle = BrushStyle.NORMAL,
            isFilled = isFilled
        )

        when (type) {
            PathType.CIRCLE -> drawCircle(pathData)
            PathType.SQUARE -> drawSquare(pathData)
            PathType.RECTANGLE -> drawRectangle(pathData)
            PathType.TRIANGLE -> drawTriangle(pathData)
            PathType.HEART -> drawHeart(pathData)
            PathType.STAR -> drawStar(pathData)
            PathType.FREEHAND -> {}
        }
    }
}

private fun DrawScope.drawMultilineText(
    text: String,
    paint: AndroidPaint,
    backgroundColor: Color? = null
) {
    val lines = text.lines().ifEmpty { listOf("") }
    val fontSpacing = paint.fontSpacing
    val fontMetrics = paint.fontMetrics

    backgroundColor?.let { bgColor ->
        val maxLineWidth = lines.maxOf { paint.measureText(it) }
        val textHeight = (fontMetrics.bottom - fontMetrics.top) + (lines.size - 1) * fontSpacing
        val padding = 12f
        drawRoundRect(
            color = bgColor,
            topLeft = Offset(-padding, fontMetrics.top - padding),
            size = androidx.compose.ui.geometry.Size(
                maxLineWidth + 2 * padding,
                textHeight + 2 * padding
            ),
            cornerRadius = CornerRadius(8f, 8f)
        )
    }

    var currentY = 0f
    lines.forEach { line ->
        drawContext.canvas.nativeCanvas.drawText(line, 0f, currentY, paint)
        currentY += fontSpacing
    }
}

private fun DrawScope.drawFreehandPath(pathData: PathData) {
    val path = pathData.path
    val color = pathData.color
    val thickness = pathData.brushSize
    val brushStyle = pathData.brushStyle

    if (path.size == 1) {
        val point = path.first()
        drawCircle(
            color = color,
            radius = thickness / 2,
            center = point,
            style = Stroke(
                width = thickness,
                cap = brushStyle.toStrokeCap(),
                join = StrokeJoin.Round
            )
        )
        return
    }

    when {
        brushStyle == BrushStyle.SPRAY -> {
            val seed = pathData.id.hashCode().toLong()
            val random = java.util.Random(seed)
            path.forEachIndexed { index, point ->
                if (index < path.lastIndex) {
                    val nextPoint = path[index + 1]
                    val distance = kotlin.math.sqrt(
                        (nextPoint.x - point.x) * (nextPoint.x - point.x) +
                                (nextPoint.y - point.y) * (nextPoint.y - point.y)
                    )
                    val numParticles = (distance / 5).toInt().coerceAtLeast(1)
                    repeat(numParticles) {
                        val t = random.nextFloat()
                        val x = point.x + (nextPoint.x - point.x) * t
                        val y = point.y + (nextPoint.y - point.y) * t
                        val offsetX = (random.nextFloat() - 0.5f) * thickness * 1.5f
                        val offsetY = (random.nextFloat() - 0.5f) * thickness * 1.5f
                        drawCircle(
                            color = color.copy(alpha = 0.3f + random.nextFloat() * 0.3f),
                            radius = 0.5f + random.nextFloat() * (thickness * 0.3f),
                            center = Offset(x + offsetX, y + offsetY)
                        )
                    }
                }
            }
        }

        brushStyle == BrushStyle.GLOW -> {
            val smoothedPath = Path().apply {
                if (path.isNotEmpty()) {
                    moveTo(path.first().x, path.first().y)
                    val smoothness = 5
                    for (i in 1..path.lastIndex) {
                        val from = path[i - 1]
                        val to = path[i]
                        val dx = abs(from.x - to.x)
                        val dy = abs(from.y - to.y)
                        if (dx >= smoothness || dy >= smoothness) {
                            quadraticTo(
                                x1 = (from.x + to.x) / 2f,
                                y1 = (from.y + to.y) / 2f,
                                x2 = to.x,
                                y2 = to.y
                            )
                        }
                    }
                }
            }
            drawPath(
                path = smoothedPath,
                color = color.copy(alpha = 0.3f),
                style = Stroke(
                    width = thickness * 2.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            drawPath(
                path = smoothedPath,
                color = color,
                style = Stroke(
                    width = thickness,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        else -> {
            val smoothedPath = Path().apply {
                if (path.isNotEmpty()) {
                    moveTo(path.first().x, path.first().y)
                    val smoothness = 5
                    for (i in 1..path.lastIndex) {
                        val from = path[i - 1]
                        val to = path[i]
                        val dx = abs(from.x - to.x)
                        val dy = abs(from.y - to.y)
                        if (dx >= smoothness || dy >= smoothness) {
                            quadraticTo(
                                x1 = (from.x + to.x) / 2f,
                                y1 = (from.y + to.y) / 2f,
                                x2 = to.x,
                                y2 = to.y
                            )
                        }
                    }
                }
            }
            val strokeWidth = when (brushStyle) {
                BrushStyle.CALLIGRAPHY -> thickness * 1.5f
                BrushStyle.MARKER -> thickness * 1.3f
                BrushStyle.SKETCH -> thickness * 0.7f
                else -> thickness
            }
            val strokeColor = when (brushStyle) {
                BrushStyle.MARKER -> color.copy(alpha = 0.7f)
                else -> color
            }
            drawPath(
                path = smoothedPath,
                color = strokeColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = brushStyle.toStrokeCap(),
                    join = StrokeJoin.Round,
                    pathEffect = brushStyle.toPathEffect(thickness)
                )
            )
        }
    }
}

private fun DrawScope.drawCircle(pathData: PathData) {
    val path = pathData.path
    val color = pathData.color
    val thickness = pathData.brushSize
    val isFilled = pathData.isFilled

    if (path.size < 2) return

    val start = path.first()
    val end = path.last()
    val radius = (end - start).getDistance() / 2
    val center = Offset(
        (start.x + end.x) / 2,
        (start.y + end.y) / 2
    )

    if (isFilled) {
        drawCircle(
            color = color,
            radius = radius,
            center = center
        )
    }
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawSquare(pathData: PathData) {
    val path = pathData.path
    val color = pathData.color
    val thickness = pathData.brushSize
    val isFilled = pathData.isFilled

    if (path.size < 2) return

    val start = path.first()
    val end = path.last()
    val size = min(abs(end.x - start.x), abs(end.y - start.y))

    val squarePath = Path().apply {
        moveTo(start.x, start.y)
        lineTo(start.x + size * if (end.x > start.x) 1 else -1, start.y)
        lineTo(
            start.x + size * if (end.x > start.x) 1 else -1,
            start.y + size * if (end.y > start.y) 1 else -1
        )
        lineTo(start.x, start.y + size * if (end.y > start.y) 1 else -1)
        close()
    }

    if (isFilled) {
        drawPath(
            path = squarePath,
            color = color
        )
    }
    drawPath(
        path = squarePath,
        color = color,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawRectangle(pathData: PathData) {
    val path = pathData.path
    val color = pathData.color
    val thickness = pathData.brushSize
    val isFilled = pathData.isFilled

    if (path.size < 2) return

    val start = path.first()
    val end = path.last()

    val rectPath = Path().apply {
        moveTo(start.x, start.y)
        lineTo(end.x, start.y)
        lineTo(end.x, end.y)
        lineTo(start.x, end.y)
        close()
    }

    if (isFilled) {
        drawPath(
            path = rectPath,
            color = color
        )
    }
    drawPath(
        path = rectPath,
        color = color,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawTriangle(pathData: PathData) {
    val path = pathData.path
    val color = pathData.color
    val thickness = pathData.brushSize
    val isFilled = pathData.isFilled

    if (path.size < 2) return

    val start = path.first()
    val end = path.last()

    val trianglePath = Path().apply {
        moveTo(start.x, end.y)
        lineTo((start.x + end.x) / 2, start.y)
        lineTo(end.x, end.y)
        close()
    }

    if (isFilled) {
        drawPath(
            path = trianglePath,
            color = color
        )
    }
    drawPath(
        path = trianglePath,
        color = color,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawHeart(pathData: PathData) {
    val path = pathData.path
    val color = pathData.color
    val thickness = pathData.brushSize
    val isFilled = pathData.isFilled

    if (path.size < 2) return

    val first = path.first()
    var minX = first.x
    var maxX = first.x
    var minY = first.y
    var maxY = first.y

    path.forEach { point ->
        if (point.x < minX) minX = point.x
        if (point.x > maxX) maxX = point.x
        if (point.y < minY) minY = point.y
        if (point.y > maxY) maxY = point.y
    }

    val width = (maxX - minX).coerceAtLeast(1f)
    val height = (maxY - minY).coerceAtLeast(1f)

    val centerX = (minX + maxX) / 2f
    val bottomY = maxY
    val topY = minY
    val upperCurveY = topY + height * 0.3f

    val heartPath = Path().apply {
        moveTo(centerX, bottomY)
        cubicTo(
            centerX + width * 0.25f,
            bottomY - height * 0.1f,
            maxX,
            bottomY - height * 0.35f,
            maxX,
            upperCurveY
        )
        cubicTo(
            maxX,
            topY,
            centerX + width * 0.2f,
            topY,
            centerX,
            topY + height * 0.28f
        )
        cubicTo(
            centerX - width * 0.2f,
            topY,
            minX,
            topY,
            minX,
            upperCurveY
        )
        cubicTo(
            minX,
            bottomY - height * 0.35f,
            centerX - width * 0.25f,
            bottomY - height * 0.1f,
            centerX,
            bottomY
        )
        close()
    }

    if (isFilled) {
        drawPath(
            path = heartPath,
            color = color
        )
    }
    drawPath(
        path = heartPath,
        color = color,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawStar(pathData: PathData) {
    val path = pathData.path
    val color = pathData.color
    val thickness = pathData.brushSize
    val isFilled = pathData.isFilled

    if (path.size < 2) return

    val first = path.first()
    var minX = first.x
    var maxX = first.x
    var minY = first.y
    var maxY = first.y

    path.forEach { point ->
        if (point.x < minX) minX = point.x
        if (point.x > maxX) maxX = point.x
        if (point.y < minY) minY = point.y
        if (point.y > maxY) maxY = point.y
    }

    val width = (maxX - minX).coerceAtLeast(1f)
    val height = (maxY - minY).coerceAtLeast(1f)
    val centerX = (minX + maxX) / 2f
    val centerY = (minY + maxY) / 2f
    val outerRadiusX = width / 2f
    val outerRadiusY = height / 2f
    val innerRadiusX = outerRadiusX * 0.5f
    val innerRadiusY = outerRadiusY * 0.5f

    val starPath = Path().apply {
        repeat(10) { index ->
            val angle = PI / 2 + index * PI / 5
            val useOuter = index % 2 == 0
            val radiusX = if (useOuter) outerRadiusX else innerRadiusX
            val radiusY = if (useOuter) outerRadiusY else innerRadiusY
            val x = centerX + (radiusX * cos(angle)).toFloat()
            val y = centerY - (radiusY * sin(angle)).toFloat()
            if (index == 0) {
                moveTo(x, y)
            } else {
                lineTo(x, y)
            }
        }
        close()
    }

    if (isFilled) {
        drawPath(
            path = starPath,
            color = color
        )
    }
    drawPath(
        path = starPath,
        color = color,
        style = Stroke(
            width = thickness,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

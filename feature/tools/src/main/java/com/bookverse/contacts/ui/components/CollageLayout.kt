package com.bookverse.contacts.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.annotation.StringRes
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import com.bookverse.contacts.feature.tools.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class CollageLayout(
    val requiredPhotos: Int,
    @StringRes val labelRes: Int
) {
    TWO_HORIZONTAL(2, R.string.collage_two_horizontal),
    TWO_VERTICAL(2, R.string.collage_two_vertical),
    THREE_LEFT_BIG(3, R.string.collage_three_left_big),
    THREE_COLUMNS(3, R.string.collage_three_columns),
    FOUR_GRID(4, R.string.collage_four_grid),
    FOUR_LEFT_BIG(4, R.string.collage_four_left_big);
}

data class CollageCellTransform(
    val pan: Offset = Offset.Zero,
    val zoom: Float = 1f,
    val rotation: Float = 0f,
)

fun collageCellRects(
    layout: CollageLayout,
    width: Float,
    height: Float,
    gap: Float = 8f,
): List<RectF> {
    if (width <= 0f || height <= 0f) return emptyList()

    val safeGap = gap.coerceIn(0f, min(width, height) / 4f)

    return when (layout) {
        CollageLayout.TWO_HORIZONTAL -> listOf(
            RectF(0f, 0f, width / 2f - safeGap / 2f, height),
            RectF(width / 2f + safeGap / 2f, 0f, width, height),
        )

        CollageLayout.TWO_VERTICAL -> listOf(
            RectF(0f, 0f, width, height / 2f - safeGap / 2f),
            RectF(0f, height / 2f + safeGap / 2f, width, height),
        )

        CollageLayout.THREE_LEFT_BIG -> listOf(
            RectF(0f, 0f, width * 0.6f - safeGap / 2f, height),
            RectF(width * 0.6f + safeGap / 2f, 0f, width, height / 2f - safeGap / 2f),
            RectF(width * 0.6f + safeGap / 2f, height / 2f + safeGap / 2f, width, height),
        )

        CollageLayout.THREE_COLUMNS -> {
            val columnWidth = (width - 2f * safeGap) / 3f
            listOf(
                RectF(0f, 0f, columnWidth, height),
                RectF(columnWidth + safeGap, 0f, 2f * columnWidth + safeGap, height),
                RectF(2f * columnWidth + 2f * safeGap, 0f, width, height),
            )
        }

        CollageLayout.FOUR_GRID -> listOf(
            RectF(0f, 0f, width / 2f - safeGap / 2f, height / 2f - safeGap / 2f),
            RectF(width / 2f + safeGap / 2f, 0f, width, height / 2f - safeGap / 2f),
            RectF(0f, height / 2f + safeGap / 2f, width / 2f - safeGap / 2f, height),
            RectF(width / 2f + safeGap / 2f, height / 2f + safeGap / 2f, width, height),
        )

        CollageLayout.FOUR_LEFT_BIG -> {
            val rightWidth = width * 0.4f
            val leftWidth = width - rightWidth - safeGap
            val cellHeight = (height - 2f * safeGap) / 3f
            listOf(
                RectF(0f, 0f, leftWidth, height),
                RectF(leftWidth + safeGap, 0f, width, cellHeight),
                RectF(leftWidth + safeGap, cellHeight + safeGap, width, 2f * cellHeight + safeGap),
                RectF(leftWidth + safeGap, 2f * cellHeight + 2f * safeGap, width, height),
            )
        }
    }
}

fun renderCollageBitmap(
    photos: List<Bitmap>,
    layout: CollageLayout,
    width: Int,
    height: Int,
    gap: Int = 8,
    transforms: List<CollageCellTransform> = emptyList(),
): Bitmap? {
    if (photos.size < layout.requiredPhotos || width <= 0 || height <= 0) return null

    val result = createBitmap(width, height)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val cells = collageCellRects(layout, width.toFloat(), height.toFloat(), gap.toFloat())

    cells.forEachIndexed { index, rect ->
        if (index < photos.size) {
            drawTransformedBitmap(
                canvas = canvas,
                source = photos[index],
                destination = rect,
                transform = transforms.getOrNull(index) ?: CollageCellTransform(),
                paint = paint,
            )
        }
    }

    return result
}

fun collageCoverScale(
    sourceWidth: Int,
    sourceHeight: Int,
    destinationWidth: Float,
    destinationHeight: Float,
    rotation: Float,
): Float {
    if (sourceWidth <= 0 || sourceHeight <= 0 || destinationWidth <= 0f || destinationHeight <= 0f) {
        return 1f
    }

    val radians = Math.toRadians(rotation.toDouble())
    val absoluteCosine = abs(cos(radians)).toFloat()
    val absoluteSine = abs(sin(radians)).toFloat()
    val rotatedWidth = sourceWidth * absoluteCosine + sourceHeight * absoluteSine
    val rotatedHeight = sourceWidth * absoluteSine + sourceHeight * absoluteCosine

    return max(
        destinationWidth / rotatedWidth.coerceAtLeast(0.001f),
        destinationHeight / rotatedHeight.coerceAtLeast(0.001f),
    )
}

fun collagePanBounds(
    sourceWidth: Int,
    sourceHeight: Int,
    destinationWidth: Float,
    destinationHeight: Float,
    rotation: Float,
    zoom: Float,
): Offset {
    if (destinationWidth <= 0f || destinationHeight <= 0f) return Offset.Zero

    val safeZoom = zoom.takeIf(Float::isFinite)?.coerceIn(1f, 5f) ?: 1f
    val coverScale = collageCoverScale(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        destinationWidth = destinationWidth,
        destinationHeight = destinationHeight,
        rotation = rotation,
    ) * safeZoom
    val radians = Math.toRadians(rotation.toDouble())
    val absoluteCosine = abs(cos(radians)).toFloat()
    val absoluteSine = abs(sin(radians)).toFloat()
    val displayedWidth =
        (sourceWidth * absoluteCosine + sourceHeight * absoluteSine) * coverScale
    val displayedHeight =
        (sourceWidth * absoluteSine + sourceHeight * absoluteCosine) * coverScale

    return Offset(
        x = max(0f, (displayedWidth - destinationWidth) / (2f * destinationWidth)),
        y = max(0f, (displayedHeight - destinationHeight) / (2f * destinationHeight)),
    )
}

fun clampCollagePan(
    pan: Offset,
    sourceWidth: Int,
    sourceHeight: Int,
    destinationWidth: Float,
    destinationHeight: Float,
    rotation: Float,
    zoom: Float,
): Offset {
    val bounds = collagePanBounds(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        destinationWidth = destinationWidth,
        destinationHeight = destinationHeight,
        rotation = rotation,
        zoom = zoom,
    )
    val safePan = Offset(
        x = pan.x.takeIf(Float::isFinite) ?: 0f,
        y = pan.y.takeIf(Float::isFinite) ?: 0f,
    )
    return Offset(
        x = safePan.x.coerceIn(-bounds.x, bounds.x),
        y = safePan.y.coerceIn(-bounds.y, bounds.y),
    )
}

private fun drawTransformedBitmap(
    canvas: Canvas,
    source: Bitmap,
    destination: RectF,
    transform: CollageCellTransform,
    paint: Paint,
) {
    if (destination.width() <= 0f || destination.height() <= 0f) return

    val safeRotation = transform.rotation.takeIf(Float::isFinite) ?: 0f
    val safeZoom = transform.zoom.takeIf(Float::isFinite)?.coerceIn(1f, 5f) ?: 1f
    val safePan = clampCollagePan(
        pan = transform.pan,
        sourceWidth = source.width,
        sourceHeight = source.height,
        destinationWidth = destination.width(),
        destinationHeight = destination.height(),
        rotation = safeRotation,
        zoom = safeZoom,
    )
    val scale = collageCoverScale(
        sourceWidth = source.width,
        sourceHeight = source.height,
        destinationWidth = destination.width(),
        destinationHeight = destination.height(),
        rotation = safeRotation,
    ) * safeZoom

    canvas.withClip(destination) {
        translate(
            destination.centerX() + safePan.x * destination.width(),
            destination.centerY() + safePan.y * destination.height(),
        )
        rotate(safeRotation)
        scale(scale, scale)
        drawBitmap(source, -source.width / 2f, -source.height / 2f, paint)
    }
}

fun DrawScope.drawCollageCell(
    image: ImageBitmap,
    destination: RectF,
    transform: CollageCellTransform,
) {
    drawTransformedBitmap(
        canvas = drawContext.canvas.nativeCanvas,
        source = image.asAndroidBitmap(),
        destination = destination,
        transform = transform,
        paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
    )
}

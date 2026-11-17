package com.bookverse.contacts.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import kotlin.math.ceil
import kotlin.math.floor

enum class BlurIntensity(val radiusPx: Int) {
    LOW(5),
    MEDIUM(10),
    HIGH(18),
}

data class PaintBlurRequest(
    val source: ImageBitmap,
    val points: List<Offset>,
    val brushSize: Float,
    val intensity: BlurIntensity,
)

data class BlurPatch(
    val bitmap: ImageBitmap,
    val offset: Offset,
)

fun interface PaintBlurProcessor {
    fun process(request: PaintBlurRequest): BlurPatch?
}

object DefaultPaintBlurProcessor : PaintBlurProcessor {
    override fun process(request: PaintBlurRequest): BlurPatch? {
        if (request.points.isEmpty() || request.source.width <= 0 || request.source.height <= 0) {
            return null
        }
        val radius = request.intensity.radiusPx
        val padding = request.brushSize / 2f + radius + 2f
        val left = floor(request.points.minOf { it.x } - padding)
            .toInt()
            .coerceIn(0, request.source.width - 1)
        val top = floor(request.points.minOf { it.y } - padding)
            .toInt()
            .coerceIn(0, request.source.height - 1)
        val right = ceil(request.points.maxOf { it.x } + padding)
            .toInt()
            .coerceIn(left + 1, request.source.width)
        val bottom = ceil(request.points.maxOf { it.y } + padding)
            .toInt()
            .coerceIn(top + 1, request.source.height)
        val width = right - left
        val height = bottom - top
        if (width <= 0 || height <= 0) return null

        val softwareSource = request.source.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
        val sourcePixels = IntArray(width * height)
        softwareSource.getPixels(sourcePixels, 0, width, left, top, width, height)
        if (softwareSource !== request.source.asAndroidBitmap()) softwareSource.recycle()

        val blurredPixels = boxBlur(sourcePixels, width, height, radius)
        val maskPixels = createStrokeMask(
            width = width,
            height = height,
            points = request.points,
            offset = Offset(left.toFloat(), top.toFloat()),
            brushSize = request.brushSize,
        )
        blurredPixels.indices.forEach { index ->
            val maskAlpha = Color.alpha(maskPixels[index])
            val sourceAlpha = Color.alpha(blurredPixels[index])
            blurredPixels[index] = Color.argb(
                sourceAlpha * maskAlpha / 255,
                Color.red(blurredPixels[index]),
                Color.green(blurredPixels[index]),
                Color.blue(blurredPixels[index]),
            )
        }
        val output = createBitmap(width, height)
        output.setPixels(blurredPixels, 0, width, 0, 0, width, height)
        return BlurPatch(
            bitmap = output.asImageBitmap(),
            offset = Offset(left.toFloat(), top.toFloat()),
        )
    }
}

private fun createStrokeMask(
    width: Int,
    height: Int,
    points: List<Offset>,
    offset: Offset,
    brushSize: Float,
): IntArray {
    val mask = createBitmap(width, height)
    val canvas = Canvas(mask)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = brushSize
    }
    if (points.size == 1) {
        canvas.drawCircle(
            points.first().x - offset.x,
            points.first().y - offset.y,
            brushSize / 2f,
            paint.apply { style = Paint.Style.FILL },
        )
    } else {
        val path = Path().apply {
            moveTo(points.first().x - offset.x, points.first().y - offset.y)
            points.drop(1).forEach { point ->
                lineTo(point.x - offset.x, point.y - offset.y)
            }
        }
        canvas.drawPath(path, paint)
    }
    return IntArray(width * height).also { pixels ->
        mask.getPixels(pixels, 0, width, 0, 0, width, height)
        mask.recycle()
    }
}

private fun boxBlur(source: IntArray, width: Int, height: Int, radius: Int): IntArray {
    if (radius <= 0) return source.copyOf()
    var input = source.copyOf()
    repeat(2) {
        input = blurVertically(
            blurHorizontally(input, width, height, radius),
            width,
            height,
            radius,
        )
    }
    return input
}

private fun blurHorizontally(
    source: IntArray,
    width: Int,
    height: Int,
    radius: Int,
): IntArray {
    val output = IntArray(source.size)
    for (y in 0 until height) {
        var alpha = 0
        var red = 0
        var green = 0
        var blue = 0
        var count = radius.coerceAtMost(width - 1) + 1
        for (sampleX in 0 until count) {
            val color = source[y * width + sampleX]
            alpha += Color.alpha(color)
            red += Color.red(color)
            green += Color.green(color)
            blue += Color.blue(color)
        }
        for (x in 0 until width) {
            output[y * width + x] = Color.argb(
                alpha / count,
                red / count,
                green / count,
                blue / count,
            )
            val outgoingX = x - radius
            if (outgoingX >= 0) {
                val color = source[y * width + outgoingX]
                alpha -= Color.alpha(color)
                red -= Color.red(color)
                green -= Color.green(color)
                blue -= Color.blue(color)
                count--
            }
            val incomingX = x + radius + 1
            if (incomingX < width) {
                val color = source[y * width + incomingX]
                alpha += Color.alpha(color)
                red += Color.red(color)
                green += Color.green(color)
                blue += Color.blue(color)
                count++
            }
        }
    }
    return output
}

private fun blurVertically(
    source: IntArray,
    width: Int,
    height: Int,
    radius: Int,
): IntArray {
    val output = IntArray(source.size)
    for (x in 0 until width) {
        var alpha = 0
        var red = 0
        var green = 0
        var blue = 0
        var count = radius.coerceAtMost(height - 1) + 1
        for (sampleY in 0 until count) {
            val color = source[sampleY * width + x]
            alpha += Color.alpha(color)
            red += Color.red(color)
            green += Color.green(color)
            blue += Color.blue(color)
        }
        for (y in 0 until height) {
            output[y * width + x] = Color.argb(
                alpha / count,
                red / count,
                green / count,
                blue / count,
            )
            val outgoingY = y - radius
            if (outgoingY >= 0) {
                val color = source[outgoingY * width + x]
                alpha -= Color.alpha(color)
                red -= Color.red(color)
                green -= Color.green(color)
                blue -= Color.blue(color)
                count--
            }
            val incomingY = y + radius + 1
            if (incomingY < height) {
                val color = source[incomingY * width + x]
                alpha += Color.alpha(color)
                red += Color.red(color)
                green += Color.green(color)
                blue += Color.blue(color)
                count++
            }
        }
    }
    return output
}

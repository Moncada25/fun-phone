package com.bookverse.contacts.integration

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.camera.core.ImageProxy
import androidx.core.graphics.scale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

fun createQrCodeDecoder(): QrCodeDecoder = ZxingQrCodeDecoder()

private const val MAX_BITMAP_DIMENSION = 2048

private class ZxingQrCodeDecoder : QrCodeDecoder {
    private val frameReader = ZxingQrReader(tryHarder = false)
    private val bitmapReader = ZxingQrReader(tryHarder = true)
    private val bitmapExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun decodeFrame(imageProxy: ImageProxy, onComplete: (DecodedQrCode?) -> Unit) {
        val decoded = runCatching { readLuminancePlane(imageProxy) }.getOrNull()
        mainHandler.post { onComplete(decoded) }
    }

    override fun decodeBitmap(
        bitmap: Bitmap,
        onSuccess: (DecodedQrCode?) -> Unit,
        onFailure: () -> Unit,
    ) {
        bitmapExecutor.execute {
            val result = runCatching { readBitmap(bitmap) }
            mainHandler.post {
                result.fold(onSuccess = onSuccess, onFailure = { onFailure() })
            }
        }
    }

    override fun close() {
        bitmapExecutor.shutdown()
    }

    private fun readLuminancePlane(imageProxy: ImageProxy): DecodedQrCode? {
        val plane = imageProxy.planes.firstOrNull() ?: return null
        val rowStride = plane.rowStride
        val height = imageProxy.height
        val buffer = plane.buffer.duplicate().apply { rewind() }
        val data = ByteArray(rowStride * height)
        buffer.get(data, 0, minOf(buffer.remaining(), data.size))
        return frameReader.decodeLuminance(
            data = data,
            dataWidth = rowStride,
            dataHeight = height,
            width = imageProxy.width,
            height = height,
        )
    }

    private fun readBitmap(source: Bitmap): DecodedQrCode? {
        val largest = max(source.width, source.height)
        val bitmap = if (largest > MAX_BITMAP_DIMENSION) {
            val ratio = MAX_BITMAP_DIMENSION.toFloat() / largest
            source.scale(
                (source.width * ratio).toInt().coerceAtLeast(1),
                (source.height * ratio).toInt().coerceAtLeast(1),
            )
        } else {
            source
        }
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmapReader.decodeArgb(pixels, bitmap.width, bitmap.height)
    }
}

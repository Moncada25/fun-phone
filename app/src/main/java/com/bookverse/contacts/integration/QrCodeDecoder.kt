package com.bookverse.contacts.integration

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

data class DecodedQrCode(
    val rawValue: String,
    val isUrl: Boolean,
)

interface QrCodeDecoder : AutoCloseable {
    fun decodeFrame(imageProxy: ImageProxy, onComplete: (DecodedQrCode?) -> Unit)

    fun decodeBitmap(
        bitmap: Bitmap,
        onSuccess: (DecodedQrCode?) -> Unit,
        onFailure: () -> Unit,
    )
}

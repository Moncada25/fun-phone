package com.bookverse.contacts.integration

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

fun createQrCodeDecoder(): QrCodeDecoder = MlKitQrCodeDecoder(
    BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build(),
    ),
)

private class MlKitQrCodeDecoder(
    private val scanner: BarcodeScanner,
) : QrCodeDecoder {

    @OptIn(ExperimentalGetImage::class)
    override fun decodeFrame(imageProxy: ImageProxy, onComplete: (DecodedQrCode?) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            onComplete(null)
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        var decoded: DecodedQrCode? = null
        scanner.process(image)
            .addOnSuccessListener { barcodes -> decoded = barcodes.firstDecoded() }
            .addOnCompleteListener { onComplete(decoded) }
    }

    override fun decodeBitmap(
        bitmap: Bitmap,
        onSuccess: (DecodedQrCode?) -> Unit,
        onFailure: () -> Unit,
    ) {
        scanner.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { barcodes -> onSuccess(barcodes.firstDecoded()) }
            .addOnFailureListener { onFailure() }
    }

    override fun close() {
        scanner.close()
    }

    private fun List<Barcode>.firstDecoded(): DecodedQrCode? {
        val barcode = firstOrNull() ?: return null
        val rawValue = barcode.rawValue ?: return null
        return DecodedQrCode(rawValue = rawValue, isUrl = barcode.valueType == Barcode.TYPE_URL)
    }
}

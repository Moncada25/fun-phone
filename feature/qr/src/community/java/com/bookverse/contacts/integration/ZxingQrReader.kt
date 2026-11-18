package com.bookverse.contacts.integration

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.client.result.ParsedResultType
import com.google.zxing.client.result.ResultParser
import com.google.zxing.common.HybridBinarizer

class ZxingQrReader(tryHarder: Boolean) {
    private val reader = MultiFormatReader().apply {
        setHints(
            buildMap {
                put(DecodeHintType.POSSIBLE_FORMATS, listOf(BarcodeFormat.QR_CODE))
                if (tryHarder) put(DecodeHintType.TRY_HARDER, true)
            },
        )
    }

    fun decodeLuminance(
        data: ByteArray,
        dataWidth: Int,
        dataHeight: Int,
        width: Int,
        height: Int,
        tryInverted: Boolean = false,
    ): DecodedQrCode? = decode(
        PlanarYUVLuminanceSource(data, dataWidth, dataHeight, 0, 0, width, height, false),
        tryInverted,
    )

    fun decodeArgb(
        pixels: IntArray,
        width: Int,
        height: Int,
        tryInverted: Boolean = true,
    ): DecodedQrCode? = decode(RGBLuminanceSource(width, height, pixels), tryInverted)

    private fun decode(source: LuminanceSource, tryInverted: Boolean): DecodedQrCode? =
        decodeSource(source) ?: if (tryInverted) decodeSource(source.invert()) else null

    private fun decodeSource(source: LuminanceSource): DecodedQrCode? {
        val result = try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
        } catch (_: ReaderException) {
            return null
        } finally {
            reader.reset()
        }
        val rawValue = result.text ?: return null
        return DecodedQrCode(
            rawValue = rawValue,
            isUrl = ResultParser.parseResult(result).type == ParsedResultType.URI,
        )
    }
}

package com.bookverse.contacts.integration

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZxingQrReaderTest {
    private val size = 240

    @Test
    fun decodesPlainTextFromLuminancePlaneWithRowPadding() {
        val rowStride = size + 16
        val luminance = ByteArray(rowStride * size) { WHITE }
        val matrix = encode("Hola Fun Phone")
        for (y in 0 until size) {
            for (x in 0 until size) {
                luminance[y * rowStride + x] = if (matrix[x, y]) BLACK else WHITE
            }
        }

        val decoded = ZxingQrReader(tryHarder = false).decodeLuminance(
            data = luminance,
            dataWidth = rowStride,
            dataHeight = size,
            width = size,
            height = size,
        )

        assertEquals("Hola Fun Phone", decoded?.rawValue)
        assertFalse(decoded!!.isUrl)
    }

    @Test
    fun flagsUrlsFromArgbPixels() {
        val decoded = ZxingQrReader(tryHarder = true).decodeArgb(
            pixels = argbPixels("https://example.com/fun-phone", inverted = false),
            width = size,
            height = size,
        )

        assertEquals("https://example.com/fun-phone", decoded?.rawValue)
        assertTrue(decoded!!.isUrl)
    }

    @Test
    fun decodesInvertedCodesOnlyWhenRequested() {
        val pixels = argbPixels("inverted", inverted = true)
        val reader = ZxingQrReader(tryHarder = true)

        assertNull(reader.decodeArgb(pixels, size, size, tryInverted = false))
        assertEquals("inverted", reader.decodeArgb(pixels, size, size, tryInverted = true)?.rawValue)
    }

    @Test
    fun returnsNullWhenNoCodeIsPresent() {
        val blank = IntArray(size * size) { WHITE_ARGB }

        assertNull(ZxingQrReader(tryHarder = true).decodeArgb(blank, size, size))
    }

    private fun encode(content: String) = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(EncodeHintType.MARGIN to 4),
    )

    private fun argbPixels(content: String, inverted: Boolean): IntArray {
        val matrix = encode(content)
        return IntArray(size * size) { index ->
            val dark = matrix[index % size, index / size] != inverted
            if (dark) BLACK_ARGB else WHITE_ARGB
        }
    }

    private companion object {
        const val BLACK: Byte = 0
        const val WHITE: Byte = -1
        const val BLACK_ARGB = 0xFF000000.toInt()
        const val WHITE_ARGB = 0xFFFFFFFF.toInt()
    }
}

package com.bookverse.contacts.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaintBlurProcessorTest {

    @Test
    fun blurPatchChangesPixelsOnlyInsideStrokeMask() {
        val source = checkerboard(64, 64)

        val patch = DefaultPaintBlurProcessor.process(
            PaintBlurRequest(
                source = source.asImageBitmap(),
                points = listOf(Offset(16f, 32f), Offset(48f, 32f)),
                brushSize = 16f,
                intensity = BlurIntensity.LOW,
            ),
        )

        assertNotNull(patch)
        val result = requireNotNull(patch)
        val bitmap = result.bitmap.asAndroidBitmap()
        val centerX = (32f - result.offset.x).toInt()
        val centerY = (32f - result.offset.y).toInt()
        val center = bitmap.getPixel(centerX, centerY)
        assertTrue(Color.alpha(center) > 0)
        assertNotEquals(Color.BLACK, center)
        assertNotEquals(Color.WHITE, center)
        assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
    }

    @Test
    fun blurStrokeAtCanvasEdgeIsClamped() {
        val source = checkerboard(32, 32)

        val patch = DefaultPaintBlurProcessor.process(
            PaintBlurRequest(
                source = source.asImageBitmap(),
                points = listOf(Offset.Zero, Offset(8f, 8f)),
                brushSize = 20f,
                intensity = BlurIntensity.HIGH,
            ),
        )

        assertNotNull(patch)
        assertEquals(Offset.Zero, requireNotNull(patch).offset)
    }

    private fun checkerboard(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    setPixel(x, y, if ((x + y) % 2 == 0) Color.BLACK else Color.WHITE)
                }
            }
        }
}

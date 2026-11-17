package com.bookverse.contacts.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageSizingPolicyTest {
    @Test
    fun fitPreservesAspectRatioAndPixelBudget() {
        val result = ImageSizingPolicy.fit(
            sourceWidth = 12_000,
            sourceHeight = 8_000,
            target = ImageTarget(maxLongEdge = 4_096, maxPixels = 8_000_000L),
        )

        assertTrue(result.width.toLong() * result.height <= 8_000_000L)
        assertTrue(maxOf(result.width, result.height) <= 4_096)
        assertEquals(1.5, result.width.toDouble() / result.height, 0.01)
    }

    @Test
    fun paintUsesAtMostTwiceTheCanvasLongEdge() {
        val target = ImageSizingPolicy.paint(canvasWidth = 900, canvasHeight = 600)

        assertEquals(1_800, target.maxLongEdge)
        assertEquals(8_000_000L, target.maxPixels)
    }

    @Test
    fun collageSplitsTheTotalPixelBudgetBetweenPhotos() {
        val target = ImageSizingPolicy.collage(
            requiredPhotos = 4,
            outputWidth = 2_000,
            outputHeight = 1_500,
        )

        assertEquals(4_000, target.maxLongEdge)
        assertEquals(3_000_000L, target.maxPixels)
    }

    @Test
    fun contactCropKeepsLocalQualityAboveSmallProviderLimit() {
        assertEquals(2_048, ContactPhotoSizingPolicy.cropMaxDimension(720))
        assertEquals(4_096, ContactPhotoSizingPolicy.cropMaxDimension(4_096))
    }

    @Test
    fun localContactTargetUsesSelectedHighQualityProfile() {
        val target = ContactPhotoSizingPolicy.localTarget()

        assertEquals(2_048, target.maxLongEdge)
        assertEquals(2_048L * 2_048L, target.maxPixels)
        assertEquals(97, ContactPhotoSizingPolicy.LOCAL_JPEG_QUALITY)
    }

    @Test
    fun providerContactTargetsStartAtDeviceMaximumAndDegradeInOrder() {
        val targets = ContactPhotoSizingPolicy.providerTargets(720)

        assertEquals(listOf(720, 540, 360), targets.map { it.maxLongEdge })
        assertTrue(targets.zipWithNext().all { (first, second) ->
            first.maxLongEdge > second.maxLongEdge
        })
    }

    @Test
    fun fitDoesNotUpscaleSmallContactPhotos() {
        val result = ImageSizingPolicy.fit(
            sourceWidth = 640,
            sourceHeight = 480,
            target = ContactPhotoSizingPolicy.localTarget(),
        )

        assertEquals(640, result.width)
        assertEquals(480, result.height)
    }
}

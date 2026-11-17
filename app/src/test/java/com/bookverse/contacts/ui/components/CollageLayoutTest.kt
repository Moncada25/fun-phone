package com.bookverse.contacts.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollageLayoutTest {
    @Test
    fun everyLayoutProducesTheRequiredNumberOfCells() {
        CollageLayout.entries.forEach { layout ->
            assertEquals(
                layout.requiredPhotos,
                collageCellRects(layout, width = 600f, height = 400f).size,
            )
        }
    }

    @Test
    fun fourGridProducesFourCells() {
        val cells = collageCellRects(
            layout = CollageLayout.FOUR_GRID,
            width = 100f,
            height = 100f,
            gap = 8f,
        )

        assertEquals(4, cells.size)
    }

    @Test
    fun coverScaleAccountsForRotation() {
        assertEquals(
            2f,
            collageCoverScale(
                sourceWidth = 100,
                sourceHeight = 50,
                destinationWidth = 100f,
                destinationHeight = 100f,
                rotation = 0f,
            ),
            0.001f,
        )
        assertEquals(
            2f,
            collageCoverScale(
                sourceWidth = 100,
                sourceHeight = 50,
                destinationWidth = 100f,
                destinationHeight = 100f,
                rotation = 90f,
            ),
            0.001f,
        )
    }

    @Test
    fun panIsClampedToTheVisibleImageBounds() {
        val clamped = clampCollagePan(
            pan = Offset(2f, -2f),
            sourceWidth = 100,
            sourceHeight = 100,
            destinationWidth = 100f,
            destinationHeight = 100f,
            rotation = 0f,
            zoom = 2f,
        )

        assertEquals(0.5f, clamped.x, 0.001f)
        assertEquals(-0.5f, clamped.y, 0.001f)
        assertTrue(clamped.x.isFinite() && clamped.y.isFinite())
    }
}

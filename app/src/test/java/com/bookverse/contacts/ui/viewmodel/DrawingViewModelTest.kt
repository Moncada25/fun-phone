package com.bookverse.contacts.ui.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import com.bookverse.contacts.ui.components.BlurPatch
import com.bookverse.contacts.ui.components.PaintBlurProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingViewModelTest {

    @Test
    fun undoAndRedoRestorePathInChronologicalOrder() {
        val viewModel = DrawingViewModel()

        drawPath(viewModel, Offset(4f, 6f), Offset(18f, 24f))

        assertEquals(1, viewModel.state.value.elements.size)
        assertTrue(viewModel.state.value.canUndo)
        viewModel.onAction(DrawingAction.OnUndoClick)
        assertTrue(viewModel.state.value.elements.isEmpty())
        assertTrue(viewModel.state.value.canRedo)

        viewModel.onAction(DrawingAction.OnRedoClick)

        assertEquals(1, viewModel.state.value.elements.size)
        assertFalse(viewModel.state.value.canRedo)
    }

    @Test
    fun newEditAfterUndoClearsRedoStack() {
        val viewModel = DrawingViewModel()
        drawPath(viewModel, Offset.Zero, Offset(10f, 10f))
        viewModel.onAction(DrawingAction.OnUndoClick)
        assertTrue(viewModel.state.value.canRedo)

        drawPath(viewModel, Offset(20f, 20f), Offset(30f, 30f))

        assertFalse(viewModel.state.value.canRedo)
        assertEquals(1, viewModel.state.value.elements.size)
    }

    @Test
    fun clearCanvasCanBeUndoneAndRedone() {
        val viewModel = DrawingViewModel()
        drawPath(viewModel, Offset.Zero, Offset(10f, 10f))

        viewModel.onAction(DrawingAction.OnClearCanvasClick)
        assertTrue(viewModel.state.value.elements.isEmpty())

        viewModel.onAction(DrawingAction.OnUndoClick)
        assertEquals(1, viewModel.state.value.elements.size)

        viewModel.onAction(DrawingAction.OnRedoClick)
        assertTrue(viewModel.state.value.elements.isEmpty())
    }

    @Test
    fun canvasColorParticipatesInUndoAndRedo() {
        val viewModel = DrawingViewModel()
        viewModel.onAction(DrawingAction.OnSetBackgroundColor(Color.White))

        viewModel.onAction(DrawingAction.OnFillCanvas(Color.Blue))
        assertEquals(Color.Blue, viewModel.state.value.canvasColor)

        viewModel.onAction(DrawingAction.OnUndoClick)
        assertEquals(Color.White, viewModel.state.value.canvasColor)

        viewModel.onAction(DrawingAction.OnRedoClick)
        assertEquals(Color.Blue, viewModel.state.value.canvasColor)
    }

    @Test
    fun textImageAndShapeElementsEachParticipateInUndoAndRedo() {
        val viewModel = DrawingViewModel()

        viewModel.onAction(DrawingAction.OnPrepareText("Label", Color.Black, 32f))
        viewModel.onAction(DrawingAction.OnConfirmTextPlacement(Offset(20f, 30f)))
        assertUndoRedoElement<DrawingElement.Text>(viewModel)

        viewModel.onAction(DrawingAction.OnImageSelected(FakeImageBitmap(), 100, 100))
        viewModel.onAction(DrawingAction.OnConfirmImagePlacement)
        assertUndoRedoElement<DrawingElement.Image>(viewModel)

        viewModel.onAction(
            DrawingAction.OnPrepareShape(
                type = PathType.CIRCLE,
                color = Color.Red,
                brushSize = 12f,
                isFilled = true,
                canvasWidth = 100,
                canvasHeight = 100,
            ),
        )
        viewModel.onAction(DrawingAction.OnConfirmShapePlacement)
        assertUndoRedoElement<DrawingElement.Shape>(viewModel)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun blurElementParticipatesInUndoAndRedo() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val bitmap = FakeImageBitmap()
            val processor = PaintBlurProcessor {
                BlurPatch(bitmap = bitmap, offset = Offset(2f, 3f))
            }
            val viewModel = DrawingViewModel(processor, dispatcher)

            viewModel.onAction(
                DrawingAction.OnCommitBlur(
                    source = bitmap,
                    points = listOf(Offset(1f, 1f), Offset(6f, 6f)),
                ),
            )
            advanceUntilIdle()

            assertUndoRedoElement<DrawingElement.Blur>(viewModel)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun selectingInputModeKeepsDrawingToolsExclusive() {
        val viewModel = DrawingViewModel()

        viewModel.onAction(DrawingAction.OnSelectInputMode(DrawingInputMode.ERASER))
        assertTrue(viewModel.state.value.isEraserMode)

        viewModel.onAction(DrawingAction.OnSelectInputMode(DrawingInputMode.BLUR))
        assertEquals(DrawingInputMode.BLUR, viewModel.state.value.inputMode)
        assertFalse(viewModel.state.value.isEraserMode)

        viewModel.onAction(DrawingAction.OnSelectInputMode(DrawingInputMode.PENCIL))
        assertEquals(DrawingInputMode.PENCIL, viewModel.state.value.inputMode)
    }

    private fun drawPath(viewModel: DrawingViewModel, start: Offset, end: Offset) {
        viewModel.onAction(DrawingAction.OnSelectInputMode(DrawingInputMode.PENCIL))
        viewModel.onAction(DrawingAction.OnNewPathStart)
        viewModel.onAction(DrawingAction.OnDraw(start))
        viewModel.onAction(DrawingAction.OnDraw(end))
        viewModel.onAction(DrawingAction.OnPathEnd)
    }

    private inline fun <reified T : DrawingElement> assertUndoRedoElement(viewModel: DrawingViewModel) {
        assertTrue(viewModel.state.value.elements.last() is T)
        val elementCount = viewModel.state.value.elements.size

        viewModel.onAction(DrawingAction.OnUndoClick)
        assertEquals(elementCount - 1, viewModel.state.value.elements.size)

        viewModel.onAction(DrawingAction.OnRedoClick)
        assertEquals(elementCount, viewModel.state.value.elements.size)
        assertTrue(viewModel.state.value.elements.last() is T)
    }

    private class FakeImageBitmap : ImageBitmap {
        override val width: Int = 8
        override val height: Int = 8
        override val colorSpace: ColorSpace = ColorSpaces.Srgb
        override val hasAlpha: Boolean = true
        override val config: ImageBitmapConfig = ImageBitmapConfig.Argb8888

        override fun readPixels(
            buffer: IntArray,
            startX: Int,
            startY: Int,
            width: Int,
            height: Int,
            bufferOffset: Int,
            stride: Int,
        ) = Unit

        override fun prepareToDraw() = Unit
    }
}

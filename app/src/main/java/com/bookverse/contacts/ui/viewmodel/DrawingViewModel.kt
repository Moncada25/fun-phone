package com.bookverse.contacts.ui.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.ui.components.BlurIntensity
import com.bookverse.contacts.ui.components.BrushStyle
import com.bookverse.contacts.ui.components.DefaultPaintBlurProcessor
import com.bookverse.contacts.ui.components.PaintBlurProcessor
import com.bookverse.contacts.ui.components.PaintBlurRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PathType {
    FREEHAND, CIRCLE, SQUARE, RECTANGLE, TRIANGLE, HEART, STAR
}

enum class DrawingInputMode {
    PENCIL,
    ERASER,
    BLUR,
    NONE,
}

data class ImportedImageState(
    val bitmap: ImageBitmap,
    val offset: Offset,
    val scale: Float,
    val rotation: Float,
)

data class ImageEntry(
    val id: String,
    val bitmap: ImageBitmap,
    val offset: Offset,
    val scale: Float,
    val rotation: Float,
)

data class PendingText(
    val id: String,
    val text: String,
    val color: Color,
    val size: Float,
    val position: Offset = Offset.Unspecified,
    val rotation: Float = 0f,
    val backgroundColor: Color? = null,
)

data class PendingShape(
    val id: String,
    val type: PathType,
    val color: Color,
    val brushSize: Float,
    val isFilled: Boolean,
    val offset: Offset,
    val scale: Float,
    val rotation: Float,
)

data class ShapeEntry(
    val id: String,
    val type: PathType,
    val color: Color,
    val brushSize: Float,
    val isFilled: Boolean,
    val offset: Offset,
    val scale: Float,
    val rotation: Float,
)

data class TextEntry(
    val id: String,
    val text: String,
    val color: Color,
    val position: Offset,
    val size: Float,
    val rotation: Float,
    val backgroundColor: Color? = null,
)

data class BlurEntry(
    val id: String,
    val bitmap: ImageBitmap,
    val offset: Offset,
)

data class PathData(
    val id: String,
    val color: Color,
    val brushSize: Float,
    val path: List<Offset>,
    val type: PathType = PathType.FREEHAND,
    val brushStyle: BrushStyle = BrushStyle.NORMAL,
    val isFilled: Boolean = false,
)

sealed interface DrawingElement {
    val id: String

    data class Path(val value: PathData) : DrawingElement {
        override val id: String = value.id
    }

    data class Text(val value: TextEntry) : DrawingElement {
        override val id: String = value.id
    }

    data class Image(val value: ImageEntry) : DrawingElement {
        override val id: String = value.id
    }

    data class Shape(val value: ShapeEntry) : DrawingElement {
        override val id: String = value.id
    }

    data class Blur(val value: BlurEntry) : DrawingElement {
        override val id: String = value.id
    }
}

sealed interface DrawingEdit {
    data class AddElement(
        val element: DrawingElement,
        val index: Int,
    ) : DrawingEdit

    data class CanvasColor(
        val previousColor: Color,
        val newColor: Color,
    ) : DrawingEdit

    data class ClearCanvas(
        val previousElements: List<DrawingElement>,
    ) : DrawingEdit
}

data class DrawingState(
    val selectedColor: Color = Color.Gray,
    val brushSize: Float = 10f,
    val brushStyle: BrushStyle = BrushStyle.NORMAL,
    val inputMode: DrawingInputMode = DrawingInputMode.PENCIL,
    val blurBrushSize: Float = 32f,
    val blurIntensity: BlurIntensity = BlurIntensity.MEDIUM,
    val isFillMode: Boolean = false,
    val canvasColor: Color = Color.Unspecified,
    val currentPath: PathData? = null,
    val currentPathType: PathType = PathType.FREEHAND,
    val importedImage: ImportedImageState? = null,
    val isImagePlacementMode: Boolean = false,
    val pendingText: PendingText? = null,
    val pendingShape: PendingShape? = null,
    val elements: List<DrawingElement> = emptyList(),
    val undoStack: List<DrawingEdit> = emptyList(),
    val redoStack: List<DrawingEdit> = emptyList(),
    val isBlurProcessing: Boolean = false,
) {
    val isEraserMode: Boolean get() = inputMode == DrawingInputMode.ERASER
    val canUndo: Boolean
        get() = pendingText != null || pendingShape != null ||
            (isImagePlacementMode && importedImage != null) || undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}

sealed interface DrawingAction {
    data object OnNewPathStart : DrawingAction
    data class OnDraw(val offset: Offset) : DrawingAction
    data object OnPathEnd : DrawingAction
    data class OnSelectColor(val color: Color) : DrawingAction
    data class OnSelectBrushSize(val size: Float) : DrawingAction
    data class OnSelectBrushStyle(val style: BrushStyle) : DrawingAction
    data class OnSelectInputMode(val mode: DrawingInputMode) : DrawingAction
    data class OnSelectBlurBrushSize(val size: Float) : DrawingAction
    data class OnSelectBlurIntensity(val intensity: BlurIntensity) : DrawingAction
    data class OnCommitBlur(
        val source: ImageBitmap,
        val points: List<Offset>,
    ) : DrawingAction
    data class OnToggleFillMode(val isFillMode: Boolean) : DrawingAction
    data class OnSetBackgroundColor(val color: Color) : DrawingAction
    data class OnFillCanvas(val color: Color) : DrawingAction
    data object OnClearCanvasClick : DrawingAction
    data object OnUndoClick : DrawingAction
    data object OnRedoClick : DrawingAction
    data class OnSelectPathType(val type: PathType) : DrawingAction
    data class OnImageSelected(
        val bitmap: ImageBitmap,
        val canvasWidth: Int,
        val canvasHeight: Int,
    ) : DrawingAction
    data class OnUpdateImageTransform(
        val pan: Offset,
        val zoom: Float,
        val centroid: Offset,
        val rotation: Float,
    ) : DrawingAction
    data object OnConfirmImagePlacement : DrawingAction
    data object OnCancelImagePlacement : DrawingAction
    data class OnPrepareText(
        val text: String,
        val color: Color,
        val size: Float,
        val backgroundColor: Color? = null,
    ) : DrawingAction
    data class OnUpdatePendingTextTransform(
        val pan: Offset,
        val zoom: Float,
        val centroid: Offset,
        val rotation: Float,
    ) : DrawingAction
    data class OnConfirmTextPlacement(val fallbackPosition: Offset) : DrawingAction
    data object OnCancelTextPlacement : DrawingAction
    data class OnPrepareShape(
        val type: PathType,
        val color: Color,
        val brushSize: Float,
        val isFilled: Boolean,
        val canvasWidth: Int,
        val canvasHeight: Int,
    ) : DrawingAction
    data class OnUpdatePendingShapeTransform(
        val pan: Offset,
        val zoom: Float,
        val centroid: Offset,
        val rotation: Float,
    ) : DrawingAction
    data object OnConfirmShapePlacement : DrawingAction
    data object OnCancelShapePlacement : DrawingAction
}

sealed interface DrawingEvent {
    data object BlurFailed : DrawingEvent
}

class DrawingViewModel(
    private val blurProcessor: PaintBlurProcessor = DefaultPaintBlurProcessor,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(DrawingState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<DrawingEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var defaultCanvasColor: Color = Color.White

    override fun onCleared() {
        _state.value = DrawingState(canvasColor = defaultCanvasColor)
        super.onCleared()
    }

    fun onAction(action: DrawingAction) {
        when (action) {
            DrawingAction.OnClearCanvasClick -> onClearCanvasClick()
            is DrawingAction.OnCommitBlur -> onCommitBlur(action.source, action.points)
            is DrawingAction.OnDraw -> onDraw(action.offset)
            DrawingAction.OnNewPathStart -> onNewPathStart()
            DrawingAction.OnPathEnd -> onPathEnd()
            is DrawingAction.OnSelectBlurBrushSize -> _state.update {
                it.copy(blurBrushSize = action.size.coerceIn(12f, 96f))
            }
            is DrawingAction.OnSelectBlurIntensity -> _state.update {
                it.copy(blurIntensity = action.intensity)
            }
            is DrawingAction.OnSelectBrushSize -> onSelectBrushSize(action.size)
            is DrawingAction.OnSelectBrushStyle -> onSelectBrushStyle(action.style)
            is DrawingAction.OnSelectColor -> onSelectColor(action.color)
            is DrawingAction.OnSelectInputMode -> onSelectInputMode(action.mode)
            is DrawingAction.OnToggleFillMode -> _state.update {
                it.copy(isFillMode = action.isFillMode)
            }
            is DrawingAction.OnSetBackgroundColor -> onSetBackgroundColor(action.color)
            is DrawingAction.OnFillCanvas -> onFillCanvas(action.color)
            is DrawingAction.OnSelectPathType -> onSelectPathType(action.type)
            DrawingAction.OnUndoClick -> onUndoClick()
            DrawingAction.OnRedoClick -> onRedoClick()
            is DrawingAction.OnImageSelected -> onImageSelected(
                action.bitmap,
                action.canvasWidth,
                action.canvasHeight,
            )
            is DrawingAction.OnUpdateImageTransform -> onUpdateImageTransform(
                action.pan,
                action.zoom,
                action.centroid,
                action.rotation,
            )
            DrawingAction.OnConfirmImagePlacement -> onConfirmImagePlacement()
            DrawingAction.OnCancelImagePlacement -> onCancelImagePlacement()
            is DrawingAction.OnPrepareText -> onPrepareText(
                action.text,
                action.color,
                action.size,
                action.backgroundColor,
            )
            is DrawingAction.OnUpdatePendingTextTransform -> onUpdatePendingTextTransform(
                action.pan,
                action.zoom,
                action.centroid,
                action.rotation,
            )
            is DrawingAction.OnConfirmTextPlacement -> onConfirmTextPlacement(action.fallbackPosition)
            DrawingAction.OnCancelTextPlacement -> onCancelTextPlacement()
            is DrawingAction.OnPrepareShape -> onPrepareShape(
                action.type,
                action.color,
                action.brushSize,
                action.isFilled,
                action.canvasWidth,
                action.canvasHeight,
            )
            is DrawingAction.OnUpdatePendingShapeTransform -> onUpdatePendingShapeTransform(
                action.pan,
                action.zoom,
                action.centroid,
                action.rotation,
            )
            DrawingAction.OnConfirmShapePlacement -> onConfirmShapePlacement()
            DrawingAction.OnCancelShapePlacement -> onCancelShapePlacement()
        }
    }

    private fun onUndoClick() {
        _state.update { current ->
            when {
                current.pendingText != null -> current.copy(pendingText = null)
                current.pendingShape != null -> current.copy(pendingShape = null)
                current.isImagePlacementMode && current.importedImage != null -> current.copy(
                    importedImage = null,
                    isImagePlacementMode = false,
                )
                current.undoStack.isNotEmpty() -> {
                    val edit = current.undoStack.last()
                    revertEdit(current, edit).copy(
                        undoStack = current.undoStack.dropLast(1),
                        redoStack = current.redoStack + edit,
                    )
                }
                else -> current
            }
        }
    }

    private fun onRedoClick() {
        _state.update { current ->
            val edit = current.redoStack.lastOrNull() ?: return@update current
            applyEdit(current, edit).copy(
                undoStack = current.undoStack + edit,
                redoStack = current.redoStack.dropLast(1),
            )
        }
    }

    private fun onSelectPathType(type: PathType) {
        _state.update {
            it.copy(currentPathType = type, pendingText = null, pendingShape = null)
        }
    }

    private fun onSelectInputMode(mode: DrawingInputMode) {
        _state.update {
            it.copy(
                inputMode = mode,
                currentPathType = PathType.FREEHAND,
                isFillMode = false,
                currentPath = null,
            )
        }
    }

    private fun onSetBackgroundColor(color: Color) {
        defaultCanvasColor = color
        _state.update { current ->
            if (current.canvasColor == Color.Unspecified) current.copy(canvasColor = color) else current
        }
    }

    private fun onFillCanvas(color: Color) {
        val previousColor = currentCanvasColor()
        if (color == previousColor) return
        defaultCanvasColor = color
        commitEdit(DrawingEdit.CanvasColor(previousColor, color))
    }

    private fun onSelectBrushSize(size: Float) {
        _state.update { it.copy(brushSize = size.coerceIn(5f, 50f)) }
    }

    private fun onSelectBrushStyle(style: BrushStyle) {
        _state.update { it.copy(brushStyle = style) }
    }

    private fun onSelectColor(color: Color) {
        _state.update { it.copy(selectedColor = color) }
    }

    private fun currentCanvasColor(): Color {
        val color = state.value.canvasColor
        return if (color == Color.Unspecified) defaultCanvasColor else color
    }

    private fun onPathEnd() {
        val currentPathData = state.value.currentPath ?: return
        if (currentPathData.path.isEmpty()) {
            _state.update { it.copy(currentPath = null) }
            return
        }
        _state.update { current ->
            addElementEdit(current.copy(currentPath = null), DrawingElement.Path(currentPathData))
        }
    }

    private fun onNewPathStart() {
        val current = state.value
        if (current.inputMode !in setOf(DrawingInputMode.PENCIL, DrawingInputMode.ERASER)) return
        val canvasColor = currentCanvasColor()
        _state.update {
            it.copy(
                currentPath = PathData(
                    id = nextId("path"),
                    color = if (it.inputMode == DrawingInputMode.ERASER) canvasColor else it.selectedColor,
                    brushSize = it.brushSize,
                    path = emptyList(),
                    type = PathType.FREEHAND,
                    brushStyle = it.brushStyle,
                ),
            )
        }
    }

    private fun onDraw(offset: Offset) {
        val currentPathData = state.value.currentPath ?: return
        _state.update {
            it.copy(currentPath = currentPathData.copy(path = currentPathData.path + offset))
        }
    }

    private fun onClearCanvasClick() {
        _state.update { current ->
            val withoutPending = current.copy(
                currentPath = null,
                importedImage = null,
                isImagePlacementMode = false,
                pendingText = null,
                pendingShape = null,
            )
            if (current.elements.isEmpty()) {
                withoutPending
            } else {
                val edit = DrawingEdit.ClearCanvas(current.elements)
                applyEdit(withoutPending, edit).copy(
                    undoStack = current.undoStack + edit,
                    redoStack = emptyList(),
                )
            }
        }
    }

    private fun onCommitBlur(source: ImageBitmap, points: List<Offset>) {
        val current = state.value
        if (current.isBlurProcessing || points.isEmpty()) return
        val request = PaintBlurRequest(
            source = source,
            points = points,
            brushSize = current.blurBrushSize,
            intensity = current.blurIntensity,
        )
        _state.update { it.copy(isBlurProcessing = true) }
        viewModelScope.launch {
            try {
                val patch = withContext(workerDispatcher) { blurProcessor.process(request) }
                if (patch == null) {
                    onBlurFailed()
                } else {
                    val element = DrawingElement.Blur(
                        BlurEntry(nextId("blur"), patch.bitmap, patch.offset),
                    )
                    _state.update { state ->
                        addElementEdit(state.copy(isBlurProcessing = false), element)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                onBlurFailed()
            }
        }
    }

    private fun onBlurFailed() {
        _state.update { it.copy(isBlurProcessing = false) }
        _events.tryEmit(DrawingEvent.BlurFailed)
    }

    private fun onImageSelected(bitmap: ImageBitmap, canvasWidth: Int, canvasHeight: Int) {
        if (canvasWidth == 0 || canvasHeight == 0) return
        val widthScale = canvasWidth.toFloat() / bitmap.width.toFloat()
        val heightScale = canvasHeight.toFloat() / bitmap.height.toFloat()
        val initialScale = minOf(widthScale, heightScale).coerceAtMost(1.5f).coerceAtLeast(0.2f)
        val initialOffset = Offset(
            (canvasWidth - bitmap.width * initialScale) / 2f,
            (canvasHeight - bitmap.height * initialScale) / 2f,
        )
        _state.update {
            it.copy(
                importedImage = ImportedImageState(bitmap, initialOffset, initialScale, 0f),
                isImagePlacementMode = true,
                inputMode = DrawingInputMode.NONE,
            )
        }
    }

    private fun onUpdateImageTransform(pan: Offset, zoom: Float, centroid: Offset, rotation: Float) {
        _state.update { current ->
            val image = current.importedImage ?: return@update current
            val sanitizedZoom = if (zoom.isFinite() && zoom > 0f) zoom else 1f
            val sanitizedPan = Offset(
                x = if (pan.x.isFinite()) pan.x else 0f,
                y = if (pan.y.isFinite()) pan.y else 0f,
            )
            val newScale = (image.scale * sanitizedZoom).coerceIn(0.25f, 5f)
            val appliedZoom = if (image.scale == 0f) 1f else newScale / image.scale
            current.copy(
                importedImage = image.copy(
                    offset = (image.offset + sanitizedPan) + centroid - centroid * appliedZoom,
                    scale = newScale,
                    rotation = image.rotation + rotation,
                ),
            )
        }
    }

    private fun onConfirmImagePlacement() {
        _state.update { current ->
            val pending = current.importedImage ?: return@update current
            val id = nextId("image")
            addElementEdit(
                current.copy(importedImage = null, isImagePlacementMode = false),
                DrawingElement.Image(
                    ImageEntry(id, pending.bitmap, pending.offset, pending.scale, pending.rotation),
                ),
            )
        }
    }

    private fun onCancelImagePlacement() {
        _state.update { it.copy(importedImage = null, isImagePlacementMode = false) }
    }

    private fun onPrepareText(text: String, color: Color, size: Float, backgroundColor: Color?) {
        if (text.isBlank()) return
        _state.update {
            it.copy(
                pendingText = PendingText(
                    id = nextId("text"),
                    text = text,
                    color = color,
                    size = size.coerceIn(24f, 256f),
                    backgroundColor = backgroundColor,
                ),
                inputMode = DrawingInputMode.NONE,
            )
        }
    }

    private fun onUpdatePendingTextTransform(
        pan: Offset,
        zoom: Float,
        centroid: Offset,
        rotation: Float,
    ) {
        _state.update { current ->
            val pending = current.pendingText ?: return@update current
            val sanitizedZoom = if (zoom.isFinite() && zoom > 0f) zoom else 1f
            val sanitizedPan = Offset(
                x = if (pan.x.isFinite()) pan.x else 0f,
                y = if (pan.y.isFinite()) pan.y else 0f,
            )
            val newSize = (pending.size * sanitizedZoom).coerceIn(24f, 256f)
            val appliedZoom = if (pending.size == 0f) 1f else newSize / pending.size
            val currentPosition = pending.position.takeIf { it != Offset.Unspecified } ?: centroid
            current.copy(
                pendingText = pending.copy(
                    position = (currentPosition + sanitizedPan) + centroid - centroid * appliedZoom,
                    size = newSize,
                    rotation = pending.rotation + rotation,
                ),
            )
        }
    }

    private fun onConfirmTextPlacement(fallbackPosition: Offset) {
        _state.update { current ->
            val pending = current.pendingText ?: return@update current
            val position = pending.position.takeIf { it != Offset.Unspecified } ?: fallbackPosition
            addElementEdit(
                current.copy(pendingText = null),
                DrawingElement.Text(
                    TextEntry(
                        id = pending.id,
                        text = pending.text,
                        color = pending.color,
                        position = position,
                        size = pending.size,
                        rotation = pending.rotation,
                        backgroundColor = pending.backgroundColor,
                    ),
                ),
            )
        }
    }

    private fun onCancelTextPlacement() {
        _state.update { it.copy(pendingText = null) }
    }

    private fun onPrepareShape(
        type: PathType,
        color: Color,
        brushSize: Float,
        isFilled: Boolean,
        canvasWidth: Int,
        canvasHeight: Int,
    ) {
        if (canvasWidth == 0 || canvasHeight == 0 || type == PathType.FREEHAND) return
        val baseSize = 200f
        _state.update {
            it.copy(
                pendingShape = PendingShape(
                    id = nextId("shape"),
                    type = type,
                    color = color,
                    brushSize = brushSize,
                    isFilled = isFilled,
                    offset = Offset((canvasWidth - baseSize) / 2f, (canvasHeight - baseSize) / 2f),
                    scale = 1f,
                    rotation = 0f,
                ),
                inputMode = DrawingInputMode.NONE,
            )
        }
    }

    private fun onUpdatePendingShapeTransform(
        pan: Offset,
        zoom: Float,
        centroid: Offset,
        rotation: Float,
    ) {
        _state.update { current ->
            val pending = current.pendingShape ?: return@update current
            val sanitizedZoom = if (zoom.isFinite() && zoom > 0f) zoom else 1f
            val sanitizedPan = Offset(
                x = if (pan.x.isFinite()) pan.x else 0f,
                y = if (pan.y.isFinite()) pan.y else 0f,
            )
            val newScale = (pending.scale * sanitizedZoom).coerceIn(0.25f, 5f)
            val appliedZoom = if (pending.scale == 0f) 1f else newScale / pending.scale
            current.copy(
                pendingShape = pending.copy(
                    offset = (pending.offset + sanitizedPan) + centroid - centroid * appliedZoom,
                    scale = newScale,
                    rotation = pending.rotation + rotation,
                ),
            )
        }
    }

    private fun onConfirmShapePlacement() {
        _state.update { current ->
            val pending = current.pendingShape ?: return@update current
            addElementEdit(
                current.copy(pendingShape = null),
                DrawingElement.Shape(
                    ShapeEntry(
                        id = pending.id,
                        type = pending.type,
                        color = pending.color,
                        brushSize = pending.brushSize,
                        isFilled = pending.isFilled,
                        offset = pending.offset,
                        scale = pending.scale,
                        rotation = pending.rotation,
                    ),
                ),
            )
        }
    }

    private fun onCancelShapePlacement() {
        _state.update { it.copy(pendingShape = null) }
    }

    private fun commitEdit(edit: DrawingEdit) {
        _state.update { current ->
            applyEdit(current, edit).copy(
                undoStack = current.undoStack + edit,
                redoStack = emptyList(),
            )
        }
    }

    private fun addElementEdit(current: DrawingState, element: DrawingElement): DrawingState {
        val edit = DrawingEdit.AddElement(element, current.elements.size)
        return applyEdit(current, edit).copy(
            undoStack = current.undoStack + edit,
            redoStack = emptyList(),
        )
    }

    private fun applyEdit(current: DrawingState, edit: DrawingEdit): DrawingState = when (edit) {
        is DrawingEdit.AddElement -> {
            val index = edit.index.coerceIn(0, current.elements.size)
            current.copy(
                elements = current.elements.toMutableList().apply { add(index, edit.element) },
            )
        }
        is DrawingEdit.CanvasColor -> current.copy(canvasColor = edit.newColor)
        is DrawingEdit.ClearCanvas -> current.copy(elements = emptyList())
    }

    private fun revertEdit(current: DrawingState, edit: DrawingEdit): DrawingState = when (edit) {
        is DrawingEdit.AddElement -> current.copy(
            elements = current.elements.filterNot { it.id == edit.element.id },
        )
        is DrawingEdit.CanvasColor -> current.copy(canvasColor = edit.previousColor)
        is DrawingEdit.ClearCanvas -> current.copy(elements = edit.previousElements)
    }

    private fun nextId(prefix: String): String = "${prefix}_${System.nanoTime()}"
}

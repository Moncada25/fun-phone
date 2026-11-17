package com.bookverse.contacts.ui.screens.tools

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.components.CollageCellTransform
import com.bookverse.contacts.ui.components.CollageLayout
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.clampCollagePan
import com.bookverse.contacts.ui.components.collageCellRects
import com.bookverse.contacts.ui.components.drawCollageCell
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageEditorScreen(
    layout: CollageLayout,
    photos: List<Bitmap>,
    canvasWidth: Int,
    canvasHeight: Int,
    isRendering: Boolean,
    onCancel: () -> Unit,
    onConfirm: (List<CollageCellTransform>) -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val onAccent = contentColorFor(accentColor)
    val previewBackground = MaterialTheme.colorScheme.surface
    val editorHint = stringResource(R.string.paint_collage_editor_hint)
    val imageBitmaps = remember(photos) { photos.map { it.asImageBitmap() } }
    var selectedCell by remember(layout, photos) { mutableIntStateOf(0) }
    var transforms by remember(layout, photos) {
        mutableStateOf(List(layout.requiredPhotos) { CollageCellTransform() })
    }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    val latestTransforms by rememberUpdatedState(transforms)
    val safeCanvasWidth = canvasWidth.coerceAtLeast(1)
    val safeCanvasHeight = canvasHeight.coerceAtLeast(1)
    val previewAspectRatio = safeCanvasWidth.toFloat() / safeCanvasHeight.toFloat()

    fun updateTransform(index: Int, transform: CollageCellTransform) {
        transforms = transforms.toMutableList().also { values ->
            values[index] = transform
        }
    }

    fun selectCellAt(x: Float, y: Float) {
        val rects = collageCellRects(
            layout = layout,
            width = previewSize.width.toFloat(),
            height = previewSize.height.toFloat(),
        )
        rects.indexOfFirst { it.contains(x, y) }
            .takeIf { it >= 0 }
            ?.let { selectedCell = it }
    }

    BackHandler {
        if (!isRendering) onCancel()
    }

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.paint_collage_editor_title),
                onBackClick = { if (!isRendering) onCancel() },
                appSettingsViewModel = appSettingsViewModel,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                val density = LocalDensity.current
                val availableWidth = with(density) { maxWidth.toPx() }
                val availableHeight = with(density) { maxHeight.toPx() }
                val previewWidth = min(availableWidth, availableHeight * previewAspectRatio)
                val previewHeight = previewWidth / previewAspectRatio
                val previewWidthDp = with(density) { previewWidth.coerceAtLeast(1f).toDp() }
                val previewHeightDp = with(density) { previewHeight.coerceAtLeast(1f).toDp() }

                Box(
                    modifier = Modifier
                        .size(previewWidthDp, previewHeightDp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(previewBackground)
                        .semantics {
                            contentDescription = editorHint
                        }
                        .onSizeChanged { previewSize = it }
                        .pointerInput(layout, previewSize) {
                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                val rects = collageCellRects(
                                    layout = layout,
                                    width = size.width.toFloat(),
                                    height = size.height.toFloat(),
                                )
                                val index = rects.indexOfFirst { it.contains(centroid.x, centroid.y) }
                                if (index < 0) return@detectTransformGestures

                                val cell = rects[index]
                                val photo = photos[index]
                                val current = latestTransforms[index]
                                val nextZoom = (current.zoom * zoom).coerceIn(1f, 5f)
                                val nextRotation = current.rotation + rotation
                                val nextPan = clampCollagePan(
                                    pan = current.pan + androidx.compose.ui.geometry.Offset(
                                        x = pan.x / cell.width().coerceAtLeast(1f),
                                        y = pan.y / cell.height().coerceAtLeast(1f),
                                    ),
                                    sourceWidth = photo.width,
                                    sourceHeight = photo.height,
                                    destinationWidth = cell.width(),
                                    destinationHeight = cell.height(),
                                    rotation = nextRotation,
                                    zoom = nextZoom,
                                )
                                selectedCell = index
                                updateTransform(
                                    index,
                                    current.copy(
                                        pan = nextPan,
                                        zoom = nextZoom,
                                        rotation = nextRotation,
                                    ),
                                )
                            }
                        }
                        .pointerInput(layout, previewSize) {
                            detectTapGestures { offset ->
                                selectCellAt(offset.x, offset.y)
                            }
                        },
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(previewBackground)
                        val rects = collageCellRects(
                            layout = layout,
                            width = size.width,
                            height = size.height,
                        )
                        imageBitmaps.forEachIndexed { index, image ->
                            val rect = rects.getOrNull(index) ?: return@forEachIndexed
                            drawCollageCell(
                                image = image,
                                destination = rect,
                                transform = transforms[index],
                            )
                            if (index == selectedCell) {
                                drawRect(
                                    color = accentColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
                                    size = androidx.compose.ui.geometry.Size(rect.width(), rect.height()),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f),
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isRendering,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        accentColor.copy(alpha = 0.8f),
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
                Button(
                    onClick = { onConfirm(transforms) },
                    enabled = !isRendering,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = onAccent,
                    ),
                ) {
                    if (isRendering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = onAccent,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(text = stringResource(R.string.paint_collage_insert))
                    }
                }
            }
        }
    }
}

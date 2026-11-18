package com.bookverse.contacts.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.feature.tools.R
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.PathType

internal const val PAINT_CONTROLS_HEADER_TAG = "paint_controls_header"

enum class PaintToolCategory(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    PENCIL(R.string.paint_tool_pencil, Icons.Default.Edit),
    ERASER(R.string.paint_tool_eraser, Icons.Default.DeleteOutline),
    BLUR(R.string.paint_tool_blur, Icons.Default.BlurOn),
    TEXT(R.string.paint_tool_text, Icons.Default.Title),
    SHAPES(R.string.paint_tool_shapes, Icons.Default.Category),
    CANVAS(R.string.paint_tool_canvas, Icons.Default.Palette),
}

data class PaintControlsState(
    val category: PaintToolCategory,
    val selectedColor: Color,
    val brushSize: Float,
    val brushStyle: BrushStyle,
    val blurBrushSize: Float,
    val blurIntensity: BlurIntensity,
    val selectedShape: PathType,
    val shapeFilled: Boolean,
    val accentColor: Color,
    val enabled: Boolean,
)

sealed interface PaintControlsEvent {
    data class SelectCategory(val category: PaintToolCategory) : PaintControlsEvent
    data class SelectBrushStyle(val style: BrushStyle) : PaintControlsEvent
    data class SelectColor(val color: Color) : PaintControlsEvent
    data class SelectBrushSize(val size: Float) : PaintControlsEvent
    data class SelectBlurBrushSize(val size: Float) : PaintControlsEvent
    data class SelectBlurIntensity(val intensity: BlurIntensity) : PaintControlsEvent
    data class SelectShape(val shape: PathType) : PaintControlsEvent
    data class ToggleShapeFill(val filled: Boolean) : PaintControlsEvent
    data object AddShape : PaintControlsEvent
    data class FillCanvas(val color: Color) : PaintControlsEvent
    data object OpenCollage : PaintControlsEvent
    data object ClearCanvas : PaintControlsEvent
}

@Composable
fun TopControlsBar(
    state: PaintControlsState,
    onEvent: (PaintControlsEvent) -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    initiallyExpanded: Boolean = true,
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showCanvasFillPicker by remember { mutableStateOf(false) }
    var controlsExpanded by rememberSaveable(initiallyExpanded) {
        mutableStateOf(initiallyExpanded)
    }
    var fillColor by remember(backgroundColor) { mutableStateOf(backgroundColor) }
    val animationSpeed = LocalAnimationSpeed.current
    val duration = (animationSpeed.durationMs / 2).coerceAtMost(280)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val swipeThreshold = with(LocalDensity.current) { 32.dp.toPx() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(PAINT_CONTROLS_HEADER_TAG)
                    .pointerInput(controlsExpanded, swipeThreshold) {
                        var totalDrag = 0f
                        detectVerticalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                totalDrag += dragAmount
                            },
                            onDragEnd = {
                                when {
                                    totalDrag >= swipeThreshold -> controlsExpanded = false
                                    totalDrag <= -swipeThreshold -> controlsExpanded = true
                                }
                            },
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(PaintToolCategory.entries, key = { it.name }) { category ->
                        FilterChip(
                            selected = state.category == category,
                            onClick = { onEvent(PaintControlsEvent.SelectCategory(category)) },
                            enabled = state.enabled,
                            label = { Text(stringResource(category.labelRes)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = onSurfaceColor.copy(alpha = 0.04f),
                                labelColor = onSurfaceColor,
                                iconColor = onSurfaceColor,
                                selectedContainerColor = onSurfaceColor.copy(alpha = 0.12f),
                                selectedLabelColor = state.accentColor,
                                selectedLeadingIconColor = state.accentColor,
                            ),
                            border = BorderStroke(
                                width = if (state.category == category) 2.dp else 1.dp,
                                color = if (state.category == category) {
                                    state.accentColor
                                } else {
                                    onSurfaceColor.copy(alpha = 0.24f)
                                },
                            ),
                            modifier = Modifier
                                .requiredHeightIn(min = 48.dp)
                                .semantics { selected = state.category == category },
                        )
                    }
                }
                IconButton(
                    onClick = { controlsExpanded = !controlsExpanded },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (controlsExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = stringResource(
                            if (controlsExpanded) R.string.paint_collapse_controls else R.string.paint_expand_controls,
                        ),
                        tint = state.accentColor,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }

            AnimatedVisibility(
                visible = controlsExpanded && state.category != PaintToolCategory.TEXT,
                enter = if (duration == 0) fadeIn(snap()) else expandVertically(tween(duration)) + fadeIn(tween(duration)),
                exit = if (duration == 0) fadeOut(snap()) else shrinkVertically(tween(duration)) + fadeOut(tween(duration)),
            ) {
                AnimatedContent(
                    targetState = state.category,
                    transitionSpec = {
                        val enter = if (duration == 0) fadeIn(snap()) else fadeIn(tween(duration))
                        val exit = if (duration == 0) fadeOut(snap()) else fadeOut(tween(duration))
                        enter togetherWith exit
                    },
                    label = "paintToolPanel",
                ) { category ->
                    when (category) {
                        PaintToolCategory.PENCIL -> PencilPanel(
                            state = state,
                            animationDuration = duration,
                            onEvent = onEvent,
                            onSelectColor = { showColorPicker = true },
                        )
                        PaintToolCategory.ERASER -> SizePanel(
                            label = stringResource(R.string.brush_size),
                            value = state.brushSize,
                            range = 5f..50f,
                            accentColor = state.accentColor,
                            enabled = state.enabled,
                            onValueChange = { onEvent(PaintControlsEvent.SelectBrushSize(it)) },
                        )
                        PaintToolCategory.BLUR -> BlurPanel(state, onEvent)
                        PaintToolCategory.TEXT -> Spacer(Modifier.height(0.dp))
                        PaintToolCategory.SHAPES -> ShapePanel(
                            state = state,
                            onEvent = onEvent,
                            onSelectColor = { showColorPicker = true },
                        )
                        PaintToolCategory.CANVAS -> CanvasPanel(
                            accentColor = state.accentColor,
                            enabled = state.enabled,
                            onFill = { showCanvasFillPicker = true },
                            onCollage = { onEvent(PaintControlsEvent.OpenCollage) },
                            onClear = { onEvent(PaintControlsEvent.ClearCanvas) },
                        )
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = state.selectedColor,
            onColorSelected = {
                onEvent(PaintControlsEvent.SelectColor(it))
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
            appSettingsViewModel = appSettingsViewModel,
            title = stringResource(R.string.brush_color),
        )
    }
    if (showCanvasFillPicker) {
        ColorPickerDialog(
            initialColor = fillColor,
            onColorSelected = {
                fillColor = it
                onEvent(PaintControlsEvent.FillCanvas(it))
                showCanvasFillPicker = false
            },
            onDismiss = { showCanvasFillPicker = false },
            appSettingsViewModel = appSettingsViewModel,
            title = stringResource(R.string.background_color),
        )
    }
}

@Composable
private fun PencilPanel(
    state: PaintControlsState,
    animationDuration: Int,
    onEvent: (PaintControlsEvent) -> Unit,
    onSelectColor: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.brushStyle, animationDuration) {
        val index = BrushStyle.entries.indexOf(state.brushStyle)
        if (animationDuration == 0) listState.scrollToItem(index) else listState.animateScrollToItem(index)
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(BrushStyle.entries, key = { it.name }) { style ->
                PencilStyleItem(
                    style = style,
                    color = state.selectedColor,
                    accentColor = state.accentColor,
                    selected = state.brushStyle == style,
                    enabled = state.enabled,
                    animationDuration = animationDuration,
                    onClick = { onEvent(PaintControlsEvent.SelectBrushStyle(style)) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onSelectColor,
                enabled = state.enabled,
                border = BorderStroke(1.dp, state.accentColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = state.accentColor),
            ) {
                Icon(Icons.Default.Palette, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.brush_color))
            }
            Icon(Icons.Default.Straighten, contentDescription = null)
            Slider(
                value = state.brushSize,
                onValueChange = { onEvent(PaintControlsEvent.SelectBrushSize(it)) },
                valueRange = 5f..50f,
                steps = 8,
                enabled = state.enabled,
                colors = SliderDefaults.colors(
                    thumbColor = state.accentColor,
                    activeTrackColor = state.accentColor,
                    activeTickColor = state.accentColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                    inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
                modifier = Modifier.width(220.dp),
            )
            Text(stringResource(R.string.brush_size_value, state.brushSize.toInt()))
        }
    }
}

@Composable
private fun PencilStyleItem(
    style: BrushStyle,
    color: Color,
    accentColor: Color,
    selected: Boolean,
    enabled: Boolean,
    animationDuration: Int,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = if (animationDuration == 0) snap() else tween(animationDuration),
        label = "pencilScale",
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        selected = selected,
        modifier = Modifier
            .size(width = 104.dp, height = 56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (selected) -3.dp.toPx() else 0f
            },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
        },
        contentColor = if (selected) accentColor else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BrushStrokePreview(style, color, Modifier.fillMaxWidth().height(14.dp))
            Text(
                text = style.getLabel(),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BrushStrokePreview(style: BrushStyle, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawLine(
            color = color,
            start = Offset(size.width * 0.08f, size.height / 2f),
            end = Offset(size.width * 0.92f, size.height / 2f),
            strokeWidth = 3.dp.toPx(),
            pathEffect = style.previewPathEffect(3.dp.toPx()),
        )
    }
}

private fun BrushStyle.previewPathEffect(width: Float): PathEffect? = when (this) {
    BrushStyle.DASHED, BrushStyle.SKETCH -> PathEffect.dashPathEffect(floatArrayOf(width * 3f, width * 2f))
    BrushStyle.DOTTED, BrushStyle.SPRAY -> PathEffect.dashPathEffect(floatArrayOf(width, width * 1.5f))
    else -> null
}

@Composable
private fun SizePanel(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.Brush, contentDescription = null)
        Text(label, style = MaterialTheme.typography.labelLarge)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                activeTickColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ),
            modifier = Modifier.weight(1f),
        )
        Text(value.toInt().toString())
    }
}

@Composable
private fun BlurPanel(state: PaintControlsState, onEvent: (PaintControlsEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SizePanel(
            label = stringResource(R.string.paint_blur_size),
            value = state.blurBrushSize,
            range = 12f..96f,
            accentColor = state.accentColor,
            enabled = state.enabled,
            onValueChange = { onEvent(PaintControlsEvent.SelectBlurBrushSize(it)) },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.paint_blur_intensity), style = MaterialTheme.typography.labelLarge)
            BlurIntensity.entries.forEach { intensity ->
                FilterChip(
                    selected = state.blurIntensity == intensity,
                    onClick = { onEvent(PaintControlsEvent.SelectBlurIntensity(intensity)) },
                    enabled = state.enabled,
                    label = { Text(stringResource(intensity.labelRes())) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        selectedLabelColor = state.accentColor,
                    ),
                    border = BorderStroke(
                        width = if (state.blurIntensity == intensity) 2.dp else 1.dp,
                        color = if (state.blurIntensity == intensity) {
                            state.accentColor
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
                        },
                    ),
                )
            }
        }
    }
}

@StringRes
private fun BlurIntensity.labelRes(): Int = when (this) {
    BlurIntensity.LOW -> R.string.paint_blur_low
    BlurIntensity.MEDIUM -> R.string.paint_blur_medium
    BlurIntensity.HIGH -> R.string.paint_blur_high
}

@Composable
private fun ShapePanel(
    state: PaintControlsState,
    onEvent: (PaintControlsEvent) -> Unit,
    onSelectColor: () -> Unit,
) {
    val shapes = listOf(
        PathType.CIRCLE to R.string.shape_circle,
        PathType.SQUARE to R.string.shape_square,
        PathType.RECTANGLE to R.string.shape_rectangle,
        PathType.TRIANGLE to R.string.shape_triangle,
        PathType.HEART to R.string.shape_heart,
        PathType.STAR to R.string.shape_star,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(shapes, key = { it.first.name }) { (shape, label) ->
                FilterChip(
                    selected = state.selectedShape == shape,
                    onClick = { onEvent(PaintControlsEvent.SelectShape(shape)) },
                    enabled = state.enabled,
                    label = { Text(stringResource(label)) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        selectedLabelColor = state.accentColor,
                    ),
                    border = BorderStroke(
                        width = if (state.selectedShape == shape) 2.dp else 1.dp,
                        color = if (state.selectedShape == shape) {
                            state.accentColor
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
                        },
                    ),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.fill_shape))
            Switch(
                checked = state.shapeFilled,
                onCheckedChange = { onEvent(PaintControlsEvent.ToggleShapeFill(it)) },
                enabled = state.enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = contentColorFor(state.accentColor),
                    checkedTrackColor = state.accentColor,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                    uncheckedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                ),
            )
            OutlinedButton(
                onClick = onSelectColor,
                enabled = state.enabled,
                border = BorderStroke(1.dp, state.accentColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = state.accentColor),
            ) {
                Icon(Icons.Default.Palette, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.brush_color))
            }
            Button(
                onClick = { onEvent(PaintControlsEvent.AddShape) },
                enabled = state.enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = state.accentColor,
                    contentColor = contentColorFor(state.accentColor),
                ),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.paint_add_shape))
            }
        }
    }
}

@Composable
private fun CanvasPanel(
    accentColor: Color,
    enabled: Boolean,
    onFill: () -> Unit,
    onCollage: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onFill,
            enabled = enabled,
            border = BorderStroke(1.dp, accentColor),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
        ) {
            Icon(Icons.Default.FormatColorFill, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.paint_fill_canvas))
        }
        OutlinedButton(
            onClick = onCollage,
            enabled = enabled,
            border = BorderStroke(1.dp, accentColor),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
        ) {
            Icon(Icons.Default.GridView, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.paint_collage))
        }
        Button(
            onClick = onClear,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.erase))
        }
    }
}

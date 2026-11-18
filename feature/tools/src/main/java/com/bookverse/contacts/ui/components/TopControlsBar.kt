package com.bookverse.contacts.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.feature.tools.R
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.PathType
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.Droplets
import com.composables.icons.lucide.Eraser
import com.composables.icons.lucide.Frame
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PaintBucket
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.RectangleHorizontal
import com.composables.icons.lucide.Shapes
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Triangle
import com.composables.icons.lucide.Type
import kotlinx.coroutines.launch

internal const val PAINT_CONTROLS_HEADER_TAG = "paint_controls_header"

private val BrushSizeRange = 5f..50f
private val BlurSizeRange = 12f..96f

enum class PaintToolCategory(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    PENCIL(R.string.paint_tool_pencil, Lucide.Pencil),
    ERASER(R.string.paint_tool_eraser, Lucide.Eraser),
    BLUR(R.string.paint_tool_blur, Lucide.Droplets),
    TEXT(R.string.paint_tool_text, Lucide.Type),
    SHAPES(R.string.paint_tool_shapes, Lucide.Shapes),
    CANVAS(R.string.paint_tool_canvas, Lucide.Frame),
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
    val swipeThreshold = with(LocalDensity.current) { 32.dp.toPx() }
    val panelVisible = controlsExpanded && state.category != PaintToolCategory.TEXT

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PanelHandle(
                expanded = panelVisible,
                onToggle = { controlsExpanded = !controlsExpanded },
            )
            AnimatedVisibility(
                visible = panelVisible,
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
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        when (category) {
                            PaintToolCategory.PENCIL -> PencilPanel(
                                state = state,
                                animationDuration = duration,
                                onEvent = onEvent,
                                onSelectColor = { showColorPicker = true },
                            )
                            PaintToolCategory.ERASER -> LabeledSlider(
                                label = stringResource(R.string.paint_size),
                                description = stringResource(R.string.brush_size),
                                value = state.brushSize,
                                range = BrushSizeRange,
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
                                enabled = state.enabled,
                                onFill = { showCanvasFillPicker = true },
                                onCollage = { onEvent(PaintControlsEvent.OpenCollage) },
                                onClear = { onEvent(PaintControlsEvent.ClearCanvas) },
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(PAINT_CONTROLS_HEADER_TAG)
                    .pointerInput(swipeThreshold) {
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
                    }
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PaintToolCategory.entries.forEach { category ->
                    ToolButton(
                        category = category,
                        selected = state.category == category,
                        enabled = state.enabled,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (state.category == category) {
                                controlsExpanded = !controlsExpanded
                            } else {
                                controlsExpanded = true
                            }
                            onEvent(PaintControlsEvent.SelectCategory(category))
                        },
                    )
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
private fun PanelHandle(expanded: Boolean, onToggle: () -> Unit) {
    val description = stringResource(
        if (expanded) R.string.paint_collapse_controls else R.string.paint_expand_controls,
    )
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ToolButton(
    category: PaintToolCategory,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(category.labelRes)
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val contentColor = when {
        !enabled -> colors.onSurface.copy(alpha = 0.38f)
        selected -> colors.onPrimaryContainer
        else -> colors.onSurfaceVariant
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(label) } },
            state = tooltipState,
            focusable = false,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 40.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.primaryContainer else Color.Transparent)
                    .combinedClickable(
                        enabled = enabled,
                        role = Role.Tab,
                        onClick = onClick,
                        onLongClick = { scope.launch { tooltipState.show() } },
                    )
                    .semantics {
                        contentDescription = label
                        this.selected = selected
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun OptionPill(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        selected = selected,
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(
                if (description != null) {
                    Modifier.semantics { contentDescription = description }
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.primaryContainer else Color.Transparent,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, enabled: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.brush_color)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        )
    }
}

@Composable
private fun SizeSlider(
    description: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = description },
        )
        Text(
            text = value.toInt().toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(28.dp),
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    description: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OptionLabel(label)
        SizeSlider(
            description = description,
            value = value,
            range = range,
            enabled = enabled,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OptionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(76.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(BrushStyle.entries, key = { it.name }) { style ->
                OptionPill(
                    selected = state.brushStyle == style,
                    enabled = state.enabled,
                    onClick = { onEvent(PaintControlsEvent.SelectBrushStyle(style)) },
                    modifier = Modifier.width(76.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        BrushStrokePreview(style, state.selectedColor, Modifier.fillMaxWidth().height(10.dp))
                        Text(
                            text = style.getLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColorSwatch(color = state.selectedColor, enabled = state.enabled, onClick = onSelectColor)
            SizeSlider(
                description = stringResource(R.string.brush_size),
                value = state.brushSize,
                range = BrushSizeRange,
                enabled = state.enabled,
                onValueChange = { onEvent(PaintControlsEvent.SelectBrushSize(it)) },
                modifier = Modifier.weight(1f),
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
private fun BlurPanel(state: PaintControlsState, onEvent: (PaintControlsEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LabeledSlider(
            label = stringResource(R.string.paint_size),
            description = stringResource(R.string.paint_blur_size),
            value = state.blurBrushSize,
            range = BlurSizeRange,
            enabled = state.enabled,
            onValueChange = { onEvent(PaintControlsEvent.SelectBlurBrushSize(it)) },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OptionLabel(stringResource(R.string.paint_blur_intensity))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BlurIntensity.entries.forEach { intensity ->
                    OptionPill(
                        selected = state.blurIntensity == intensity,
                        enabled = state.enabled,
                        onClick = { onEvent(PaintControlsEvent.SelectBlurIntensity(intensity)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(intensity.labelRes()),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
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

private val PaintShapes = listOf(
    Triple(PathType.CIRCLE, R.string.shape_circle, Lucide.Circle),
    Triple(PathType.SQUARE, R.string.shape_square, Lucide.Square),
    Triple(PathType.RECTANGLE, R.string.shape_rectangle, Lucide.RectangleHorizontal),
    Triple(PathType.TRIANGLE, R.string.shape_triangle, Lucide.Triangle),
    Triple(PathType.HEART, R.string.shape_heart, Lucide.Heart),
    Triple(PathType.STAR, R.string.shape_star, Lucide.Star),
)

@Composable
private fun ShapePanel(
    state: PaintControlsState,
    onEvent: (PaintControlsEvent) -> Unit,
    onSelectColor: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PaintShapes.forEach { (shape, label, icon) ->
                OptionPill(
                    selected = state.selectedShape == shape,
                    enabled = state.enabled,
                    onClick = { onEvent(PaintControlsEvent.SelectShape(shape)) },
                    description = stringResource(label),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ColorSwatch(color = state.selectedColor, enabled = state.enabled, onClick = onSelectColor)
            OptionPill(
                selected = state.shapeFilled,
                enabled = state.enabled,
                onClick = { onEvent(PaintControlsEvent.ToggleShapeFill(!state.shapeFilled)) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Lucide.PaintBucket, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.fill_shape), style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.weight(1f))
            FilledTonalButton(
                onClick = { onEvent(PaintControlsEvent.AddShape) },
                enabled = state.enabled,
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.paint_add_shape))
            }
        }
    }
}

@Composable
private fun CanvasPanel(
    enabled: Boolean,
    onFill: () -> Unit,
    onCollage: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CanvasAction(
            icon = Lucide.PaintBucket,
            label = stringResource(R.string.paint_fill_canvas),
            enabled = enabled,
            onClick = onFill,
            modifier = Modifier.weight(1f),
        )
        CanvasAction(
            icon = Lucide.LayoutGrid,
            label = stringResource(R.string.paint_collage),
            enabled = enabled,
            onClick = onCollage,
            modifier = Modifier.weight(1f),
        )
        CanvasAction(
            icon = Lucide.Trash2,
            label = stringResource(R.string.erase),
            enabled = enabled,
            onClick = onClear,
            destructive = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CanvasAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val tint = when {
        !enabled -> colors.onSurface.copy(alpha = 0.38f)
        destructive -> colors.error
        else -> colors.onSurface
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(colors.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (destructive && enabled) colors.error else colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

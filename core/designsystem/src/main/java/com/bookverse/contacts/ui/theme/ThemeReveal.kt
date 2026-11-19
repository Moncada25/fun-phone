package com.bookverse.contacts.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.hypot
import kotlin.math.max

@Stable
class ThemeRevealState internal constructor() {
    internal var originOnScreen: Offset? = null
    internal val progress = Animatable(1f)
    internal var centerOnScreen by mutableStateOf(Offset.Unspecified)
    internal var maxRadius by mutableFloatStateOf(0f)
    internal val surfaces = mutableListOf<ThemeRevealSurface>()

    fun markOrigin(positionOnScreen: Offset) {
        originOnScreen = positionOnScreen
    }
}

@Stable
class ThemeRevealSurface internal constructor(internal val layer: GraphicsLayer) {
    internal var coordinates: LayoutCoordinates? = null
    internal var snapshot by mutableStateOf<ImageBitmap?>(null)

    internal suspend fun capture(): ImageBitmap? {
        if (layer.size == IntSize.Zero || coordinates?.isAttached != true) return null
        return runCatching { layer.toImageBitmap() }.getOrNull()
    }
}

val LocalThemeRevealState = staticCompositionLocalOf<ThemeRevealState?> { null }

@Composable
fun rememberThemeRevealSurface(state: ThemeRevealState? = LocalThemeRevealState.current): ThemeRevealSurface {
    val layer = rememberGraphicsLayer()
    val surface = remember(layer) { ThemeRevealSurface(layer) }
    DisposableEffect(state, surface) {
        state?.surfaces?.add(surface)
        onDispose { state?.surfaces?.remove(surface) }
    }
    return surface
}

fun Modifier.themeRevealSurface(state: ThemeRevealState?, surface: ThemeRevealSurface): Modifier {
    if (state == null) return this
    return this
        .onGloballyPositioned { surface.coordinates = it }
        .drawWithContent {
            surface.layer.record { this@drawWithContent.drawContent() }
            drawLayer(surface.layer)
            val image = surface.snapshot ?: return@drawWithContent
            val coordinates = surface.coordinates?.takeIf { it.isAttached } ?: return@drawWithContent
            val center = coordinates.screenToLocal(state.centerOnScreen)
            val radius = state.maxRadius * state.progress.value
            val hole = Path().apply { addOval(Rect(center = center, radius = radius)) }
            clipPath(hole, clipOp = ClipOp.Difference) {
                drawImage(image)
            }
        }
}

fun Modifier.themeRevealOrigin(state: ThemeRevealState?): Modifier =
    if (state == null) this else this.then(ThemeRevealOriginElement(state))

@Composable
fun <T> ThemeRevealHost(
    target: T,
    durationMs: Int,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val state = remember { ThemeRevealState() }
    val hostSurface = rememberThemeRevealSurface(state)
    var displayed by remember { mutableStateOf(target) }

    LaunchedEffect(target) {
        if (target == displayed) return@LaunchedEffect
        val hostCoordinates = hostSurface.coordinates
        if (durationMs <= 0 || hostCoordinates?.isAttached != true) {
            displayed = target
            return@LaunchedEffect
        }
        val captures = coroutineScope {
            state.surfaces.toList()
                .map { surface -> async { surface.capture()?.let { surface to it } } }
                .awaitAll()
                .filterNotNull()
        }
        if (!hostCoordinates.isAttached || captures.none { it.first === hostSurface }) {
            displayed = target
            return@LaunchedEffect
        }
        val hostSize = Size(hostCoordinates.size.width.toFloat(), hostCoordinates.size.height.toFloat())
        val localCenter = state.originOnScreen
            ?.let { runCatching { hostCoordinates.screenToLocal(it) }.getOrNull() }
            ?.takeIf { it.x in 0f..hostSize.width && it.y in 0f..hostSize.height }
            ?: Offset(hostSize.width / 2f, hostSize.height / 2f)
        state.originOnScreen = null
        state.centerOnScreen = hostCoordinates.localToScreen(localCenter)
        state.maxRadius = revealRadius(localCenter, hostSize)
        state.progress.snapTo(0f)
        captures.forEach { (surface, image) -> surface.snapshot = image }
        displayed = target
        try {
            state.progress.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing))
        } finally {
            captures.forEach { (surface, _) -> surface.snapshot = null }
        }
    }

    CompositionLocalProvider(LocalThemeRevealState provides state) {
        Box(modifier = modifier.themeRevealSurface(state, hostSurface)) {
            content(displayed)
        }
    }
}

fun themeRevealDurationMs(animationDurationMs: Int): Int = animationDurationMs * 6 / 5

internal fun revealRadius(center: Offset, size: Size): Float {
    val dx = max(center.x, size.width - center.x)
    val dy = max(center.y, size.height - center.y)
    return hypot(dx, dy)
}

private data class ThemeRevealOriginElement(
    val state: ThemeRevealState,
) : ModifierNodeElement<ThemeRevealOriginNode>() {
    override fun create() = ThemeRevealOriginNode(state)

    override fun update(node: ThemeRevealOriginNode) {
        node.state = state
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "themeRevealOrigin"
    }
}

private class ThemeRevealOriginNode(
    var state: ThemeRevealState,
) : Modifier.Node(), PointerInputModifierNode {
    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Initial) return
        val down = pointerEvent.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: return
        state.markOrigin(requireLayoutCoordinates().localToScreen(down.position))
    }

    override fun onCancelPointerInput() = Unit
}

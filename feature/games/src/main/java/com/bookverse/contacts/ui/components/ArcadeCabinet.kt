package com.bookverse.contacts.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.feature.games.R
import com.bookverse.contacts.ui.theme.AppChrome
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class ArcadeDirection { UP, DOWN, LEFT, RIGHT }

@Stable
class ArcadeControls internal constructor() {
    var primaryLabel by mutableStateOf<String?>(null)
        internal set
    var primaryEnabled by mutableStateOf(false)
        internal set
    var directionEnabled by mutableStateOf(false)
        internal set
    internal var onPrimaryPress: (() -> Unit)? = null
    internal var onPrimaryRelease: (() -> Unit)? = null
    internal var onDirection: ((ArcadeDirection?) -> Unit)? = null
}

@Stable
class ArcadeChrome internal constructor() {
    var title by mutableStateOf("")
        internal set
    var actions by mutableStateOf<(@Composable ActionPillScope.() -> Unit)?>(null, referentialEqualityPolicy())
        internal set
    internal var onBack: () -> Unit = {}

    fun navigateBack() = onBack()
}

@Stable
class ArcadeHostState {
    var chrome by mutableStateOf<ArcadeChrome?>(null)
        internal set
    var controls by mutableStateOf<ArcadeControls?>(null)
        internal set
}

val LocalArcadeHost = staticCompositionLocalOf<ArcadeHostState?> { null }

@Composable
fun ArcadeControlsEffect(
    primaryLabel: String?,
    onPrimaryPress: (() -> Unit)?,
    onDirection: ((ArcadeDirection?) -> Unit)?,
    onPrimaryRelease: (() -> Unit)? = null,
) {
    val host = LocalArcadeHost.current ?: return
    val controls = remember(host) { ArcadeControls() }
    SideEffect {
        controls.primaryLabel = primaryLabel
        controls.primaryEnabled = onPrimaryPress != null
        controls.directionEnabled = onDirection != null
        controls.onPrimaryPress = onPrimaryPress
        controls.onPrimaryRelease = onPrimaryRelease
        controls.onDirection = onDirection
    }
    DisposableEffect(host, controls) {
        host.controls = controls
        onDispose {
            if (host.controls === controls) host.controls = null
        }
    }
}

@Composable
fun GameScaffold(
    title: String,
    onBackClick: () -> Unit,
    actions: (@Composable ActionPillScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val host = LocalArcadeHost.current
    if (host != null) {
        val chrome = remember(host) { ArcadeChrome() }
        SideEffect {
            chrome.title = title
            chrome.actions = actions
            chrome.onBack = onBackClick
        }
        DisposableEffect(host, chrome) {
            host.chrome = chrome
            onDispose {
                if (host.chrome === chrome) host.chrome = null
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content(PaddingValues(0.dp))
    }
}

@Composable
fun ArcadeCabinet(
    marquee: String,
    controls: ArcadeControls?,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    screen: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight && maxHeight < 560.dp
        val showMarquee = !landscape || maxHeight >= 400.dp
        val shape = RoundedCornerShape(AppChrome.sheetCorner)
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        ) {
            if (landscape) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.width(140.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        JoystickControl(controls)
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (showMarquee) CabinetMarquee(text = marquee, compact = true)
                        CabinetScreen(modifier = Modifier.weight(1f), content = screen)
                    }
                    Column(
                        modifier = Modifier.width(140.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ActionButtons(
                            controls = controls,
                            secondaryLabel = secondaryLabel,
                            onSecondary = onSecondary,
                            stacked = true,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CabinetMarquee(text = marquee, compact = false)
                    CabinetScreen(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 640.dp),
                        content = screen,
                    )
                    ControlPanel(
                        controls = controls,
                        secondaryLabel = secondaryLabel,
                        onSecondary = onSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CabinetMarquee(text: String, compact: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = if (compact) 6.dp else 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
    ) {
        Text(
            text = text.uppercase(locale),
            style = (if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium).copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { contentDescription = text },
        )
        ArcadeLightStrip(
            dimColor = MaterialTheme.colorScheme.surface,
            lightSize = if (compact) 3.dp else 4.dp,
        )
    }
}

@Composable
private fun CabinetScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scanlineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.035f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .drawWithContent {
                    drawContent()
                    val step = 3.dp.toPx()
                    var y = 0f
                    while (y < size.height) {
                        drawRect(
                            color = scanlineColor,
                            topLeft = Offset(0f, y),
                            size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()),
                        )
                        y += step
                    }
                    drawRect(
                        brush = Brush.linearGradient(
                            0f to Color.White.copy(alpha = 0.06f),
                            0.35f to Color.Transparent,
                            start = Offset.Zero,
                            end = Offset(size.width, size.height),
                        ),
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
private fun ControlPanel(
    controls: ArcadeControls?,
    secondaryLabel: String,
    onSecondary: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                JoystickControl(controls)
            }
            ActionButtons(
                controls = controls,
                secondaryLabel = secondaryLabel,
                onSecondary = onSecondary,
                stacked = false,
            )
        }
    }
}

@Composable
private fun ColumnScope.JoystickControl(controls: ArcadeControls?) {
    val enabled = controls?.directionEnabled == true
    ArcadeJoystick(
        enabled = enabled,
        onDirection = { direction -> controls?.onDirection?.invoke(direction) },
    )
    Text(
        text = stringResource(if (enabled) R.string.games_arcade_move else R.string.games_arcade_touch)
            .uppercase(LocalConfiguration.current.locales[0]),
        style = arcadeLabelStyle,
        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(top = 4.dp)
            .clearAndSetSemantics {},
    )
}

@Composable
private fun ActionButtons(
    controls: ArcadeControls?,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    stacked: Boolean,
) {
    val primaryLabel = controls?.primaryLabel
    val primary: @Composable () -> Unit = {
        ArcadeButton(
            letter = "A",
            label = primaryLabel ?: stringResource(R.string.games_arcade_unavailable),
            enabled = controls?.primaryEnabled == true,
            emphasized = true,
            onPress = { controls?.onPrimaryPress?.invoke() },
            onRelease = { controls?.onPrimaryRelease?.invoke() },
        )
    }
    val secondary: @Composable () -> Unit = {
        ArcadeButton(
            letter = "B",
            label = secondaryLabel,
            enabled = true,
            emphasized = false,
            onPress = onSecondary,
            onRelease = {},
        )
    }
    if (stacked) {
        primary()
        secondary()
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(modifier = Modifier.padding(top = 20.dp)) { secondary() }
            Box(modifier = Modifier.padding(bottom = 20.dp)) { primary() }
        }
    }
}

@Composable
fun ArcadeButton(
    letter: String,
    label: String,
    enabled: Boolean,
    emphasized: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    size: Dp = if (emphasized) 62.dp else 52.dp,
) {
    val haptic = LocalHapticFeedback.current
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnRelease by rememberUpdatedState(onRelease)
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "arcade_button_scale",
    )
    val container = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
        emphasized -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val content = when {
        !enabled -> MaterialTheme.colorScheme.outline
        emphasized -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier
            .widthIn(min = AppChrome.touchTarget)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = label
                if (!enabled) disabled()
                onClick(label = label) {
                    if (enabled) {
                        onPress()
                        onRelease()
                    }
                    enabled
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .clip(CircleShape)
                .background(container)
                .border(
                    width = 3.dp,
                    color = if (emphasized && enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                )
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        pressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        currentOnPress()
                        waitForUpOrCancellation()
                        pressed = false
                        currentOnRelease()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = content,
            )
        }
        Text(
            text = label.uppercase(LocalConfiguration.current.locales[0]),
            style = arcadeLabelStyle,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 120.dp),
        )
    }
}

@Composable
fun ArcadeJoystick(
    enabled: Boolean,
    onDirection: (ArcadeDirection?) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    val haptic = LocalHapticFeedback.current
    val currentOnDirection by rememberUpdatedState(onDirection)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val knobOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val maxTravel = with(density) { (size * 0.26f).toPx() }
    val deadZone = with(density) { 14.dp.toPx() }
    val knobSize = size * 0.46f
    val baseColor = MaterialTheme.colorScheme.surface
    val ringColor = MaterialTheme.colorScheme.outlineVariant
    val arrowColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val knobColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val knobRing = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant
    val actionLabels = listOf(
        ArcadeDirection.UP to stringResource(R.string.cd_move_up),
        ArcadeDirection.DOWN to stringResource(R.string.cd_move_down),
        ArcadeDirection.LEFT to stringResource(R.string.cd_move_left),
        ArcadeDirection.RIGHT to stringResource(R.string.cd_move_right),
    )
    val joystickDescription = stringResource(R.string.games_arcade_joystick)

    Box(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = joystickDescription
                if (!enabled) {
                    disabled()
                } else {
                    customActions = actionLabels.map { (direction, label) ->
                        CustomAccessibilityAction(label) {
                            onDirection(direction)
                            onDirection(null)
                            true
                        }
                    }
                }
            }
            .pointerInput(enabled, maxTravel) {
                if (!enabled) return@pointerInput
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var current: ArcadeDirection? = null
                    fun update(position: Offset) {
                        val raw = position - center
                        val distance = sqrt(raw.x * raw.x + raw.y * raw.y)
                        val clamped = if (distance > maxTravel) raw * (maxTravel / distance) else raw
                        scope.launch { knobOffset.snapTo(clamped) }
                        val next = when {
                            distance < deadZone -> null
                            abs(raw.x) > abs(raw.y) -> if (raw.x > 0f) ArcadeDirection.RIGHT else ArcadeDirection.LEFT
                            else -> if (raw.y > 0f) ArcadeDirection.DOWN else ArcadeDirection.UP
                        }
                        if (next != current) {
                            current = next
                            if (next != null) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            currentOnDirection(next)
                        }
                    }
                    down.consume()
                    update(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        update(change.position)
                    }
                    if (current != null) currentOnDirection(null)
                    scope.launch {
                        knobOffset.animateTo(
                            Offset.Zero,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            drawCircle(color = ringColor, radius = radius)
            drawCircle(color = baseColor, radius = radius - 3.dp.toPx())
            val arrow = radius * 0.16f
            val inset = radius * 0.78f
            val c = this.center
            listOf(
                Offset(0f, -1f),
                Offset(0f, 1f),
                Offset(-1f, 0f),
                Offset(1f, 0f),
            ).forEach { dir ->
                val tip = c + dir * (inset + arrow * 0.4f)
                val back = c + dir * (inset - arrow * 0.6f)
                val side = Offset(-dir.y, dir.x) * arrow * 0.7f
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(back.x + side.x, back.y + side.y)
                    lineTo(back.x - side.x, back.y - side.y)
                    close()
                }
                drawPath(path, color = arrowColor)
            }
            drawCircle(color = ringColor, radius = radius * 0.2f)
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.value.x.roundToInt(), knobOffset.value.y.roundToInt()) }
                .size(knobSize)
                .clip(CircleShape)
                .background(knobColor)
                .border(3.dp, knobRing, CircleShape)
                .drawWithContent {
                    drawContent()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.28f),
                        radius = this.size.minDimension * 0.16f,
                        center = Offset(this.size.width * 0.36f, this.size.height * 0.34f),
                    )
                }
        )
    }
}

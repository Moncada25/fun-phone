package com.bookverse.contacts.ui.screens.games

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import com.bookverse.contacts.feature.games.R
import com.bookverse.contacts.ui.components.ArcadeControlsEffect
import com.bookverse.contacts.ui.components.GameScaffold
import com.bookverse.contacts.core.ui.R as CoreUiR
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.domain.games.SNAKE_BOARD_HEIGHT
import com.bookverse.contacts.domain.games.SNAKE_BOARD_WIDTH
import com.bookverse.contacts.domain.games.SnakeBoundaryMode
import com.bookverse.contacts.domain.games.SnakeDirection
import com.bookverse.contacts.domain.games.SnakeGameState
import com.bookverse.contacts.domain.games.SnakePhase
import com.bookverse.contacts.domain.games.SnakePoint
import com.bookverse.contacts.settings.AnimationSpeed
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.ScoreCard
import com.bookverse.contacts.ui.components.ScoreCardEntry
import com.bookverse.contacts.ui.components.retroGameDialogBlur
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.SnakeEvent
import com.bookverse.contacts.ui.viewmodel.SnakeViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import kotlin.math.abs
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnakeScreen(
    onBackClick: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    viewModelFactory: ViewModelProvider.Factory,
) {
    val viewModel: SnakeViewModel = viewModel(factory = viewModelFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    val animationSpeed by appSettingsViewModel.animationSpeed.collectAsStateWithLifecycle()
    val setupVisible = state.phase == SnakePhase.READY
    val blurRadius = retroGameDialogBlur(setupVisible)
    val context = LocalContext.current

    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
    }
    val eatSoundId = remember { mutableIntStateOf(-1) }
    val startSoundId = remember { mutableIntStateOf(-1) }
    val gameOverSoundId = remember { mutableIntStateOf(-1) }
    val winSoundId = remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        try {
            context.assets.openFd("snake_eat.wav").use {
                eatSoundId.intValue = soundPool.load(it, 1)
            }
            context.assets.openFd("snake_start.wav").use {
                startSoundId.intValue = soundPool.load(it, 1)
            }
            context.assets.openFd("snake_gameover.wav").use {
                gameOverSoundId.intValue = soundPool.load(it, 1)
            }
            context.assets.openFd("snake_win.wav").use {
                winSoundId.intValue = soundPool.load(it, 1)
            }
        } catch (_: Exception) {
            // Sounds are optional enhancement; game stays fully playable muted.
        }
    }

    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }

    var prevScore by remember { mutableIntStateOf(0) }
    var prevPhase by remember { mutableStateOf(state.phase) }
    LaunchedEffect(state.score, state.phase) {
        if (state.phase == SnakePhase.RUNNING && prevPhase != SnakePhase.RUNNING) {
            playSnakeSound(startSoundId.intValue, soundPool)
            prevScore = state.score
        } else if (state.score > prevScore) {
            playSnakeSound(eatSoundId.intValue, soundPool, rate = 1f + (state.score % 6) * 0.02f)
            prevScore = state.score
        }
        if (state.phase == SnakePhase.GAME_OVER && prevPhase != SnakePhase.GAME_OVER) {
            playSnakeSound(gameOverSoundId.intValue, soundPool)
        }
        if (state.phase == SnakePhase.WON && prevPhase != SnakePhase.WON) {
            playSnakeSound(winSoundId.intValue, soundPool)
        }
        prevPhase = state.phase
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.onEvent(SnakeEvent.ScreenExit) }
    }
    BackHandler { onBackClick() }

    ArcadeControlsEffect(
        primaryLabel = stringResource(
            if (state.phase == SnakePhase.PAUSED) R.string.games_arcade_resume else R.string.games_arcade_pause
        ),
        onPrimaryPress = if (state.phase == SnakePhase.RUNNING || state.phase == SnakePhase.PAUSED) {
            { viewModel.onEvent(SnakeEvent.TogglePause) }
        } else {
            null
        },
        onDirection = { direction ->
            direction?.let { viewModel.onEvent(SnakeEvent.ChangeDirection(SnakeDirection.valueOf(it.name))) }
        },
    )

    GameScaffold(
        title = stringResource(R.string.snake_title),
        onBackClick = onBackClick,
        actions = {
            if (state.phase == SnakePhase.RUNNING || state.phase == SnakePhase.PAUSED) {
                IconAction(
                    imageVector = if (state.phase == SnakePhase.PAUSED) {
                        Lucide.Play
                    } else {
                        Lucide.Pause
                    },
                    contentDescription = if (state.phase == SnakePhase.PAUSED) {
                        stringResource(R.string.cd_resume_game)
                    } else {
                        stringResource(R.string.cd_pause_game)
                    },
                    onClick = { viewModel.onEvent(SnakeEvent.TogglePause) },
                )
            }
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .blur(blurRadius),
        ) {
            if (maxWidth >= 600.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SnakeBoard(
                        state = state,
                        accentColor = accentColor,
                        animationSpeed = animationSpeed,
                        onDirection = { direction ->
                            viewModel.onEvent(SnakeEvent.ChangeDirection(direction))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .aspectRatio(1f),
                    )
                    Column(
                        modifier = Modifier.widthIn(min = 240.dp, max = 320.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        SnakeScoreCard(state)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SnakeScoreCard(state)
                    SnakeBoard(
                        state = state,
                        accentColor = accentColor,
                        animationSpeed = animationSpeed,
                        onDirection = { direction ->
                            viewModel.onEvent(SnakeEvent.ChangeDirection(direction))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (setupVisible) {
        SnakeSetupDialog(
            initialMode = state.boundaryMode,
            accentColor = accentColor,
            onStart = { mode -> viewModel.onEvent(SnakeEvent.Start(mode)) },
            onDismiss = onBackClick,
        )
    }

    if (state.phase == SnakePhase.GAME_OVER || state.phase == SnakePhase.WON) {
        MessageDialog(
            title = if (state.phase == SnakePhase.WON) {
                stringResource(R.string.snake_victory_title)
            } else {
                stringResource(R.string.lose_title)
            },
            message1 = stringResource(R.string.game_over_message, state.score),
            buttonText = stringResource(R.string.play_again),
            onButtonClick = { viewModel.onEvent(SnakeEvent.Restart) },
            secondaryButtonText = stringResource(R.string.snake_change_mode),
            onSecondaryButtonClick = { viewModel.onEvent(SnakeEvent.ChangeMode) },
            onDismiss = onBackClick,
        )
    }
}

@Composable
private fun SnakeScoreCard(state: SnakeGameState) {
    val modeLabel = when (state.boundaryMode) {
        SnakeBoundaryMode.WALLS -> stringResource(R.string.snake_mode_walls_title)
        SnakeBoundaryMode.WRAP -> stringResource(R.string.snake_mode_wrap_title)
    }
    val phaseLabel = snakePhaseLabel(state.phase)
    ScoreCard(
        entries = listOf(
            ScoreCardEntry(stringResource(R.string.score_label), state.score.toString()),
            ScoreCardEntry(stringResource(R.string.best_score_label), state.highScore.toString()),
            ScoreCardEntry(stringResource(R.string.snake_mode_label), modeLabel),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                stateDescription = phaseLabel
            },
    )
}

@Composable
private fun SnakeBoard(
    state: SnakeGameState,
    accentColor: Color,
    animationSpeed: AnimationSpeed,
    onDirection: (SnakeDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 24.dp.toPx() }
    val modeLabel = when (state.boundaryMode) {
        SnakeBoundaryMode.WALLS -> stringResource(R.string.snake_mode_walls_title)
        SnakeBoundaryMode.WRAP -> stringResource(R.string.snake_mode_wrap_title)
    }
    val phaseLabel = snakePhaseLabel(state.phase)
    val boardDescription = stringResource(
        R.string.snake_board_description,
        phaseLabel,
        state.score,
        modeLabel,
        foodVerticalLabel(state),
        foodHorizontalLabel(state),
    )
    val infiniteTransition = rememberInfiniteTransition(label = "snake_food_pulse")
    val animatedPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween((animationSpeed.durationMs * 3).coerceAtLeast(600)),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "snake_food_scale",
    )
    val foodPulse = if (animationSpeed == AnimationSpeed.OFF) 1f else animatedPulse
    val boardShape = RoundedCornerShape(24.dp)
    val borderWidth = if (state.boundaryMode == SnakeBoundaryMode.WALLS) 3.dp else 1.dp
    val neutralDecorationColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(boardShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
            .border(borderWidth, accentColor.copy(alpha = 0.78f), boardShape)
            .semantics { contentDescription = boardDescription },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.phase, swipeThreshold) {
                    var drag = Offset.Zero
                    detectDragGestures(
                        onDragStart = { drag = Offset.Zero },
                        onDrag = { change, amount ->
                            change.consume()
                            drag += amount
                        },
                        onDragEnd = {
                            val direction = when {
                                abs(drag.x) > abs(drag.y) && abs(drag.x) >= swipeThreshold -> {
                                    if (drag.x > 0f) SnakeDirection.RIGHT else SnakeDirection.LEFT
                                }
                                abs(drag.y) >= swipeThreshold -> {
                                    if (drag.y > 0f) SnakeDirection.DOWN else SnakeDirection.UP
                                }
                                else -> null
                            }
                            direction?.let(onDirection)
                        },
                    )
                },
        ) {
            drawSnakeGrid(
                neutralColor = neutralDecorationColor,
                accentColor = accentColor,
                boundaryMode = state.boundaryMode,
            )
            drawOrganicSnake(state, accentColor)
            state.food?.let { food -> drawSnakeFood(food, foodPulse) }
        }
    }
}

private fun DrawScope.drawSnakeGrid(
    neutralColor: Color,
    accentColor: Color,
    boundaryMode: SnakeBoundaryMode,
) {
    val cellWidth = size.width / SNAKE_BOARD_WIDTH
    val cellHeight = size.height / SNAKE_BOARD_HEIGHT
    val gridColor = neutralColor.copy(alpha = 0.045f)
    for (index in 1 until SNAKE_BOARD_WIDTH) {
        val x = index * cellWidth
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
    }
    for (index in 1 until SNAKE_BOARD_HEIGHT) {
        val y = index * cellHeight
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
    }
    if (boundaryMode == SnakeBoundaryMode.WRAP) {
        val portalColor = accentColor.copy(alpha = 0.45f)
        val inset = min(cellWidth, cellHeight) * 0.28f
        drawLine(portalColor, Offset(0f, inset), Offset(0f, size.height - inset), inset)
        drawLine(portalColor, Offset(size.width, inset), Offset(size.width, size.height - inset), inset)
        drawLine(portalColor, Offset(inset, 0f), Offset(size.width - inset, 0f), inset)
        drawLine(portalColor, Offset(inset, size.height), Offset(size.width - inset, size.height), inset)
    }
}

private fun DrawScope.drawOrganicSnake(state: SnakeGameState, accentColor: Color) {
    if (state.segments.isEmpty()) return
    val cellWidth = size.width / SNAKE_BOARD_WIDTH
    val cellHeight = size.height / SNAKE_BOARD_HEIGHT
    val cell = min(cellWidth, cellHeight)
    val centers = state.segments.map { point -> point.center(cellWidth, cellHeight) }

    state.segments.zipWithNext().forEachIndexed { index, (from, to) ->
        if (abs(from.x - to.x) + abs(from.y - to.y) != 1) return@forEachIndexed
        val progress = 1f - index.toFloat() / state.segments.size.coerceAtLeast(2)
        drawLine(
            color = accentColor.copy(alpha = 0.72f + progress * 0.2f),
            start = centers[index],
            end = centers[index + 1],
            strokeWidth = cell * (0.48f + progress * 0.18f),
            cap = StrokeCap.Round,
        )
    }

    for (index in state.segments.lastIndex downTo 1) {
        val progress = 1f - index.toFloat() / state.segments.lastIndex.coerceAtLeast(1)
        val radius = cell * (0.20f + progress * 0.12f)
        val center = centers[index]
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.96f),
                    accentColor.copy(alpha = 0.62f),
                ),
                center = center - Offset(radius * 0.22f, radius * 0.25f),
                radius = radius * 1.35f,
            ),
            radius = radius,
            center = center,
        )
        if (index % 2 == 0 && radius > 4f) {
            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = radius * 0.22f,
                center = center - Offset(radius * 0.22f, radius * 0.28f),
            )
        }
    }

    drawSnakeHead(centers.first(), state.direction, cell, accentColor)
}

private fun DrawScope.drawSnakeHead(
    center: Offset,
    direction: SnakeDirection,
    cell: Float,
    accentColor: Color,
) {
    val rotation = when (direction) {
        SnakeDirection.RIGHT -> 0f
        SnakeDirection.DOWN -> 90f
        SnakeDirection.LEFT -> 180f
        SnakeDirection.UP -> 270f
    }
    rotate(rotation, center) {
        val headSize = Size(cell * 1.02f, cell * 0.78f)
        val headTopLeft = Offset(center.x - cell * 0.46f, center.y - headSize.height / 2f)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(accentColor, accentColor.copy(alpha = 0.68f)),
                start = headTopLeft,
                end = headTopLeft + Offset(headSize.width, headSize.height),
            ),
            topLeft = headTopLeft,
            size = headSize,
            cornerRadius = CornerRadius(cell * 0.34f),
        )
        drawOval(
            color = Color.White.copy(alpha = 0.18f),
            topLeft = headTopLeft + Offset(cell * 0.12f, cell * 0.08f),
            size = Size(cell * 0.56f, cell * 0.20f),
        )

        val eyeX = center.x + cell * 0.24f
        val eyeOffsetY = cell * 0.22f
        val eyeRadius = cell * 0.115f
        val pupilRadius = eyeRadius * 0.48f
        listOf(center.y - eyeOffsetY, center.y + eyeOffsetY).forEach { eyeY ->
            drawCircle(Color.White, eyeRadius, Offset(eyeX, eyeY))
            drawCircle(Color(0xFF111318), pupilRadius, Offset(eyeX + eyeRadius * 0.22f, eyeY))
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = pupilRadius * 0.28f,
                center = Offset(eyeX + eyeRadius * 0.08f, eyeY - eyeRadius * 0.12f),
            )
        }
        drawArc(
            color = Color(0xFF111318).copy(alpha = 0.7f),
            startAngle = 35f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(center.x + cell * 0.23f, center.y - cell * 0.10f),
            size = Size(cell * 0.20f, cell * 0.20f),
            style = Stroke(width = cell * 0.035f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawSnakeFood(food: SnakePoint, pulse: Float) {
    val cellWidth = size.width / SNAKE_BOARD_WIDTH
    val cellHeight = size.height / SNAKE_BOARD_HEIGHT
    val cell = min(cellWidth, cellHeight)
    val center = food.center(cellWidth, cellHeight)
    val radius = cell * 0.31f * pulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF7A6E), Color(0xFFD7263D), Color(0xFF8E1427)),
            center = center - Offset(radius * 0.3f, radius * 0.3f),
            radius = radius * 1.25f,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.45f),
        radius = radius * 0.18f,
        center = center - Offset(radius * 0.32f, radius * 0.30f),
    )
    drawLine(
        color = Color(0xFF6D4C41),
        start = center - Offset(0f, radius * 0.75f),
        end = center - Offset(radius * 0.12f, radius * 1.15f),
        strokeWidth = cell * 0.055f,
        cap = StrokeCap.Round,
    )
    val leaf = Path().apply {
        moveTo(center.x - radius * 0.1f, center.y - radius)
        quadraticTo(
            center.x + radius * 0.7f,
            center.y - radius * 1.35f,
            center.x + radius * 0.48f,
            center.y - radius * 0.65f,
        )
        close()
    }
    drawPath(leaf, Color(0xFF43A047), style = Fill)
}

private fun playSnakeSound(soundId: Int, soundPool: SoundPool, rate: Float = 1f) {
    if (soundId == -1) return
    try {
        soundPool.play(soundId, 0.5f, 0.5f, 1, 0, rate.coerceIn(0.5f, 2f))
    } catch (_: Exception) {
        // Ignore playback failures; never break gameplay for sound.
    }
}


@Composable
private fun SnakeSetupDialog(
    initialMode: SnakeBoundaryMode,
    accentColor: Color,
    onStart: (SnakeBoundaryMode) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedModeName by rememberSaveable(initialMode) { mutableStateOf(initialMode.name) }
    val selectedMode = SnakeBoundaryMode.valueOf(selectedModeName)
    val onAccent = contentColorFor(accentColor)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 620.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = stringResource(R.string.snake_setup_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.snake_instructions_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                SnakeModeOption(
                    selected = selectedMode == SnakeBoundaryMode.WALLS,
                    title = stringResource(R.string.snake_mode_walls_title),
                    description = stringResource(R.string.snake_mode_walls_description),
                    accentColor = accentColor,
                    onClick = { selectedModeName = SnakeBoundaryMode.WALLS.name },
                )
                SnakeModeOption(
                    selected = selectedMode == SnakeBoundaryMode.WRAP,
                    title = stringResource(R.string.snake_mode_wrap_title),
                    description = stringResource(R.string.snake_mode_wrap_description),
                    accentColor = accentColor,
                    onClick = { selectedModeName = SnakeBoundaryMode.WRAP.name },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Text(stringResource(CoreUiR.string.cancel))
                    }
                    Button(
                        onClick = { onStart(selectedMode) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = onAccent,
                        ),
                    ) {
                        Icon(Lucide.Play, contentDescription = null)
                        Text(stringResource(R.string.play_game))
                    }
                }
            }
        }
    }
}

@Composable
private fun SnakeModeOption(
    selected: Boolean,
    title: String,
    description: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .selectable(selected = selected, onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    accentColor
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                },
                shape = shape,
            ),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = accentColor,
                    unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
            }
        }
    }
}

@Composable
private fun snakePhaseLabel(phase: SnakePhase): String = stringResource(
    when (phase) {
        SnakePhase.READY -> R.string.snake_phase_ready
        SnakePhase.RUNNING -> R.string.snake_phase_running
        SnakePhase.PAUSED -> R.string.snake_phase_paused
        SnakePhase.GAME_OVER -> R.string.snake_phase_game_over
        SnakePhase.WON -> R.string.snake_phase_won
    }
)

@Composable
private fun foodVerticalLabel(state: SnakeGameState): String {
    val food = state.food ?: return stringResource(R.string.snake_food_none)
    val head = state.segments.first()
    return stringResource(
        when {
            food.y < head.y -> R.string.snake_food_above
            food.y > head.y -> R.string.snake_food_below
            else -> R.string.snake_food_same_row
        }
    )
}

@Composable
private fun foodHorizontalLabel(state: SnakeGameState): String {
    val food = state.food ?: return stringResource(R.string.snake_food_none)
    val head = state.segments.first()
    return stringResource(
        when {
            food.x < head.x -> R.string.snake_food_left
            food.x > head.x -> R.string.snake_food_right
            else -> R.string.snake_food_same_column
        }
    )
}

private fun SnakePoint.center(cellWidth: Float, cellHeight: Float): Offset = Offset(
    x = x * cellWidth + cellWidth / 2f,
    y = y * cellHeight + cellHeight / 2f,
)

private fun SnakeGameState.canRequest(requested: SnakeDirection): Boolean =
    queuedDirection == null && requested != direction && !requested.isOpposite(direction)

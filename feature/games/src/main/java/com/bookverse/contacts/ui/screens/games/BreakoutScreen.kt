package com.bookverse.contacts.ui.screens.games

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.feature.games.R
import com.bookverse.contacts.ui.components.ArcadeControlsEffect
import com.bookverse.contacts.ui.components.ArcadeDirection
import com.bookverse.contacts.ui.components.GameScaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.domain.games.BREAKOUT_BALL_RADIUS
import com.bookverse.contacts.domain.games.BREAKOUT_BRICK_GAP
import com.bookverse.contacts.domain.games.BREAKOUT_BRICK_HEIGHT
import com.bookverse.contacts.domain.games.BREAKOUT_BRICK_TOP
import com.bookverse.contacts.domain.games.BREAKOUT_COLUMNS
import com.bookverse.contacts.domain.games.BREAKOUT_PADDLE_HEIGHT
import com.bookverse.contacts.domain.games.BREAKOUT_PADDLE_WIDTH
import com.bookverse.contacts.domain.games.BREAKOUT_PADDLE_Y
import com.bookverse.contacts.domain.games.BreakoutBrick
import com.bookverse.contacts.domain.games.BreakoutGameState
import com.bookverse.contacts.domain.games.BreakoutPhase
import com.bookverse.contacts.domain.games.BreakoutPaddleInput
import com.bookverse.contacts.settings.AnimationSpeed
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.RetroGameInstructionsDialog
import com.bookverse.contacts.ui.components.ScoreCard
import com.bookverse.contacts.ui.components.ScoreCardEntry
import com.bookverse.contacts.ui.components.retroGameDialogBlur
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.BreakoutEvent
import com.bookverse.contacts.ui.viewmodel.BreakoutViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakoutScreen(
    onBackClick: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    viewModelFactory: ViewModelProvider.Factory,
) {
    val viewModel: BreakoutViewModel = viewModel(factory = viewModelFactory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    val animationSpeed by appSettingsViewModel.animationSpeed.collectAsStateWithLifecycle()
    var showInstructions by remember { mutableStateOf(true) }
    val blurRadius = retroGameDialogBlur(showInstructions)
    val context = LocalContext.current

    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
    }
    val paddleSoundId = remember { mutableIntStateOf(-1) }
    val brickSoundId = remember { mutableIntStateOf(-1) }
    val wallSoundId = remember { mutableIntStateOf(-1) }
    val gameOverSoundId = remember { mutableIntStateOf(-1) }
    val launchSoundId = remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        try {
            context.assets.openFd("breakout_paddle.wav").use {
                paddleSoundId.intValue = soundPool.load(it, 1)
            }
            context.assets.openFd("breakout_brick.wav").use {
                brickSoundId.intValue = soundPool.load(it, 1)
            }
            context.assets.openFd("breakout_wall.wav").use {
                wallSoundId.intValue = soundPool.load(it, 1)
            }
            context.assets.openFd("breakout_gameover.wav").use {
                gameOverSoundId.intValue = soundPool.load(it, 1)
            }
            context.assets.openFd("breakout_launch.wav").use {
                launchSoundId.intValue = soundPool.load(it, 1)
            }
        } catch (_: Exception) {
            // Sounds are optional enhancement; game stays fully playable muted.
        }
    }

    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }

    var prevScore by remember { mutableIntStateOf(0) }
    var prevPaddleHits by remember { mutableIntStateOf(0) }
    var prevPhase by remember { mutableStateOf(state.phase) }
    var prevVelocityX by remember { mutableStateOf(state.ball.velocity.x) }
    var prevVelocityY by remember { mutableStateOf(state.ball.velocity.y) }

    LaunchedEffect(state.score, state.paddleHits, state.phase, state.ball.velocity) {
        if (state.phase == BreakoutPhase.RUNNING && prevPhase != BreakoutPhase.RUNNING) {
            play8Bit(launchSoundId.intValue, soundPool, rate = 1f)
            prevScore = state.score
            prevPaddleHits = state.paddleHits
        } else if (state.score > prevScore) {
            val pitch = 1f + (state.score % 8) * 0.03f
            play8Bit(brickSoundId.intValue, soundPool, rate = pitch)
            prevScore = state.score
            prevPaddleHits = state.paddleHits
        } else if (state.paddleHits > prevPaddleHits) {
            play8Bit(paddleSoundId.intValue, soundPool, rate = 1f)
            prevPaddleHits = state.paddleHits
        } else if (
            state.phase == BreakoutPhase.RUNNING &&
            prevVelocityX * state.ball.velocity.x < 0f &&
            (state.ball.position.x < 0.12f || state.ball.position.x > 0.88f)
        ) {
            play8Bit(wallSoundId.intValue, soundPool, rate = 1f)
        } else if (
            state.phase == BreakoutPhase.RUNNING &&
            prevVelocityY * state.ball.velocity.y < 0f &&
            state.ball.position.y < 0.12f
        ) {
            play8Bit(wallSoundId.intValue, soundPool, rate = 1.1f)
        }
        if (state.phase == BreakoutPhase.GAME_OVER && prevPhase != BreakoutPhase.GAME_OVER) {
            play8Bit(gameOverSoundId.intValue, soundPool, rate = 1f)
        }
        prevPhase = state.phase
        prevVelocityX = state.ball.velocity.x
        prevVelocityY = state.ball.velocity.y
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.onEvent(BreakoutEvent.ScreenExit) }
    }
    BackHandler { onBackClick() }

    ArcadeControlsEffect(
        primaryLabel = stringResource(
            if (state.phase == BreakoutPhase.PAUSED) R.string.games_arcade_resume else R.string.games_arcade_pause
        ),
        onPrimaryPress = if (state.phase == BreakoutPhase.RUNNING || state.phase == BreakoutPhase.PAUSED) {
            { viewModel.onEvent(BreakoutEvent.TogglePause) }
        } else {
            null
        },
        onDirection = { direction ->
            when (direction) {
                ArcadeDirection.LEFT -> viewModel.onEvent(BreakoutEvent.SetPaddleInput(BreakoutPaddleInput.LEFT))
                ArcadeDirection.RIGHT -> viewModel.onEvent(BreakoutEvent.SetPaddleInput(BreakoutPaddleInput.RIGHT))
                else -> viewModel.onEvent(BreakoutEvent.ReleasePaddle)
            }
        },
    )

    GameScaffold(
        title = stringResource(R.string.breakout_title),
        onBackClick = onBackClick,
        actions = {
            if (state.phase == BreakoutPhase.RUNNING || state.phase == BreakoutPhase.PAUSED) {
                IconAction(
                    imageVector = if (state.phase == BreakoutPhase.PAUSED) {
                        Lucide.Play
                    } else {
                        Lucide.Pause
                    },
                    contentDescription = stringResource(
                        if (state.phase == BreakoutPhase.PAUSED) {
                            R.string.cd_resume_game
                        } else {
                            R.string.cd_pause_game
                        }
                    ),
                    onClick = { viewModel.onEvent(BreakoutEvent.TogglePause) },
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
            val wideLayout = maxWidth >= 600.dp
            if (wideLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BreakoutArena(
                        state = state,
                        accentColor = accentColor,
                        animationSpeed = animationSpeed,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .aspectRatio(0.72f, matchHeightConstraintsFirst = true),
                    )
                    BreakoutSidePanel(
                        state = state,
                        accentColor = accentColor,
                        onEvent = viewModel::onEvent,
                        modifier = Modifier.widthIn(min = 240.dp, max = 320.dp),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BreakoutStats(state)
                    Text(
                        text = breakoutPhaseLabel(state.phase),
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                    )
                    BreakoutArena(
                        state = state,
                        accentColor = accentColor,
                        animationSpeed = animationSpeed,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.72f, matchHeightConstraintsFirst = true),
                    )
                }
            }
        }
    }

    if (state.phase == BreakoutPhase.GAME_OVER) {
        MessageDialog(
            title = stringResource(R.string.lose_title),
            message1 = stringResource(R.string.game_over_message, state.score),
            buttonText = stringResource(R.string.play_again),
            onButtonClick = { viewModel.onEvent(BreakoutEvent.Restart) },
            onDismiss = onBackClick,
        )
    }

    if (showInstructions) {
        RetroGameInstructionsDialog(
            title = stringResource(R.string.breakout_title),
            instructions = stringResource(R.string.breakout_instructions_text),
            onPlayClick = {
                showInstructions = false
                viewModel.onEvent(BreakoutEvent.Start)
            },
            onDismiss = {
                showInstructions = false
                onBackClick()
            },
        )
    }
}

private fun play8Bit(soundId: Int, soundPool: SoundPool, rate: Float = 1f) {
    if (soundId == -1) return
    try {
        soundPool.play(soundId, 0.6f, 0.6f, 1, 0, rate.coerceIn(0.5f, 2f))
    } catch (_: Exception) {
        // Ignore playback failures; never break gameplay for sound.
    }
}

@Composable
private fun BreakoutSidePanel(
    state: BreakoutGameState,
    accentColor: Color,
    onEvent: (BreakoutEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        BreakoutStats(state)
        Text(
            text = breakoutPhaseLabel(state.phase),
            style = MaterialTheme.typography.labelLarge,
            color = accentColor,
        )
    }
}

@Composable
private fun BreakoutStats(state: BreakoutGameState) {
    ScoreCard(
        entries = listOf(
            ScoreCardEntry(
                title = stringResource(R.string.score_label),
                value = state.score.toString(),
            ),
            ScoreCardEntry(
                title = stringResource(R.string.best_score_label),
                value = state.highScore.toString(),
            ),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}


@Composable
private fun BreakoutArena(
    state: BreakoutGameState,
    accentColor: Color,
    animationSpeed: AnimationSpeed,
    modifier: Modifier = Modifier,
) {
    val phaseLabel = breakoutPhaseLabel(state.phase)
    val description = stringResource(
        R.string.breakout_arena_description,
        phaseLabel,
        state.score,
        state.highScore,
    )
    val neutralDecorationColor = MaterialTheme.colorScheme.onSurface
    val animationsEnabled = animationSpeed != AnimationSpeed.OFF

    var particles by remember { mutableStateOf(listOf<BreakoutBreakParticle>()) }
    var prevBricks by remember { mutableStateOf(state.bricks) }

    LaunchedEffect(state.bricks) {
        if (!animationsEnabled) {
            particles = emptyList()
            prevBricks = state.bricks
            return@LaunchedEffect
        }
        if (prevBricks.size > state.bricks.size) {
            val current = state.bricks.toSet()
            val removed = prevBricks.filter { it !in current }
            val spawned = removed.flatMap { brick ->
                val center = brickCenter(brick)
                val base = brickTone(accentColor, brick.row)
                List(8) { index ->
                    val seed = brick.row * 31 + brick.column * 17 + index * 7
                    BreakoutBreakParticle(
                        x = center.x,
                        y = center.y,
                        vx = ((seed % 13) - 6) * 0.0028f,
                        vy = -0.010f + (index % 4) * 0.004f,
                        color = base,
                        ttl = 14 + (seed % 7),
                    )
                }
            }
            particles = (particles + spawned).takeLast(120)
        }
        prevBricks = state.bricks
    }

    LaunchedEffect(state.ball.position) {
        if (!animationsEnabled) {
            particles = emptyList()
            return@LaunchedEffect
        }
        if (particles.isNotEmpty()) {
            particles = particles.mapNotNull { particle ->
                val ttl = particle.ttl - 1
                if (ttl <= 0) {
                    null
                } else {
                    particle.copy(
                        x = particle.x + particle.vx,
                        y = particle.y + particle.vy,
                        vy = particle.vy + 0.0012f,
                        ttl = ttl,
                    )
                }
            }
        }
    }

    Box(
        modifier = modifier
            .semantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArenaGrid(neutralDecorationColor)
            state.bricks.forEach { brick -> drawBrick(brick, accentColor) }
            particles.forEach { particle -> drawBreakParticle(particle) }
            drawPaddle(state.paddleX, accentColor)
            drawBall(state.ball.position.x, state.ball.position.y, accentColor)
        }
    }
}

private data class BreakoutBreakParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val ttl: Int,
)

private fun DrawScope.drawArenaGrid(neutralColor: Color) {
    val lineColor = neutralColor.copy(alpha = 0.055f)
    repeat(10) { index ->
        val y = size.height * (index + 1) / 11f
        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 1f)
    }
    repeat(6) { index ->
        val x = size.width * (index + 1) / 7f
        drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 1f)
    }
}

private fun brickTone(accentColor: Color, row: Int): Color {
    val depth = ((row % 6) + 6) % 6
    return accentColor.copy(alpha = 0.95f - depth * 0.09f)
}

private fun brickCenter(brick: BreakoutBrick): Offset {
    val brickWidth = (1f - BREAKOUT_BRICK_GAP * (BREAKOUT_COLUMNS - 1)) / BREAKOUT_COLUMNS
    return Offset(
        x = brick.column * (brickWidth + BREAKOUT_BRICK_GAP) + brickWidth / 2f,
        y = BREAKOUT_BRICK_TOP + brick.row * (BREAKOUT_BRICK_HEIGHT + BREAKOUT_BRICK_GAP) +
            BREAKOUT_BRICK_HEIGHT / 2f,
    )
}

private fun DrawScope.drawBrick(brick: BreakoutBrick, accentColor: Color) {
    val brickWidth = (1f - BREAKOUT_BRICK_GAP * (BREAKOUT_COLUMNS - 1)) / BREAKOUT_COLUMNS
    val left = (brick.column * (brickWidth + BREAKOUT_BRICK_GAP)) * size.width
    val top = (BREAKOUT_BRICK_TOP + brick.row * (BREAKOUT_BRICK_HEIGHT + BREAKOUT_BRICK_GAP)) * size.height
    val width = brickWidth * size.width
    val height = BREAKOUT_BRICK_HEIGHT * size.height
    drawRoundRect(
        color = brickTone(accentColor, brick.row),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(height * 0.25f, height * 0.25f),
    )
}

private fun DrawScope.drawPaddle(paddleX: Float, accentColor: Color) {
    val left = paddleX * size.width
    val top = BREAKOUT_PADDLE_Y * size.height
    val width = BREAKOUT_PADDLE_WIDTH * size.width
    val height = BREAKOUT_PADDLE_HEIGHT * size.height
    drawRoundRect(
        color = accentColor,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2f, height / 2f),
    )
}

private fun DrawScope.drawBall(x: Float, y: Float, accentColor: Color) {
    val radius = BREAKOUT_BALL_RADIUS * min(size.width, size.height)
    drawCircle(
        color = accentColor,
        radius = radius,
        center = Offset(x * size.width, y * size.height),
    )
}

private fun DrawScope.drawBreakParticle(particle: BreakoutBreakParticle) {
    val radius = min(size.width, size.height) * 0.008f
    val alpha = (particle.ttl / 20f).coerceIn(0f, 1f)
    drawCircle(
        color = particle.color.copy(alpha = alpha),
        radius = radius,
        center = Offset(particle.x * size.width, particle.y * size.height),
    )
}

@Composable
private fun breakoutPhaseLabel(phase: BreakoutPhase): String = stringResource(
    when (phase) {
        BreakoutPhase.READY -> R.string.breakout_phase_ready
        BreakoutPhase.RUNNING -> R.string.breakout_phase_running
        BreakoutPhase.PAUSED -> R.string.breakout_phase_paused
        BreakoutPhase.GAME_OVER -> R.string.breakout_phase_game_over
    }
)

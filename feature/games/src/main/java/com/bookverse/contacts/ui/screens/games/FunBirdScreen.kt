package com.bookverse.contacts.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import android.media.AudioAttributes
import android.media.SoundPool
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.domain.games.GameStatsSnapshot
import com.bookverse.contacts.ui.components.RetroGameInstructionsDialog
import com.bookverse.contacts.ui.components.retroGameDialogBlur
import com.bookverse.contacts.ui.components.ScoreCard
import com.bookverse.contacts.ui.components.ScoreCardEntry
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private enum class FunBirdPhase { Ready, Playing, Paused, GameOver }

private data class FunBirdPipe(
    val id: Int,
    val x: Float,
    val gapCenter: Float,
    val gapSize: Float
)

private const val SCORE_PER_LEVEL = 100
private const val MAX_LEVEL = 5

private data class FunBirdCloudLayer(
    val heightFactor: Float,
    val sizeFactor: Float,
    val spacingFraction: Float,
    val speedMultiplier: Float,
    val alpha: Float = 1f,
    val phaseOffset: Float = 0f
)

private data class FunBirdStar(
    val xFactor: Float,
    val yFactor: Float,
    val radiusFactor: Float,
    val alpha: Float,
    val color: Color = Color.White
)

private data class FunBirdSkyOverlay(
    val colors: List<Color>,
    val heightFraction: Float,
    val alpha: Float = 1f
)

private data class FunBirdLevelStyle(
    val skyColors: List<Color>,
    val hillLightColor: Color,
    val hillDarkColor: Color,
    val cloudColor: Color,
    val stripeColor: Color,
    val pipeColor: Color,
    val pipeHighlight: Color,
    val pipeShadow: Color,
    val sunColor: Color? = Color(0xFFFFF59D),
    val sunGlowColor: Color? = Color(0x66FFF59D),
    val moonColor: Color? = null,
    val cloudLayers: List<FunBirdCloudLayer> = emptyList(),
    val stars: List<FunBirdStar> = emptyList(),
    val overlay: FunBirdSkyOverlay? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunBirdScreen(
    onBackClick: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    gameStatsRepository: GameStatsRepository,
) {
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior(expandable = false)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val gameSession = rememberGameSessionRecorder(GameId.FUN_BIRD, gameStatsRepository)
    val stats by gameStatsRepository.stats.collectAsStateWithLifecycle(
        initialValue = GameStatsSnapshot(),
    )
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
    val flapSoundId = remember { mutableIntStateOf(-1) }
    val pointSoundId = remember { mutableIntStateOf(-1) }
    val hitSoundId = remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        try {
            context.assets.openFd("funbird_flap.wav").use {
                flapSoundId.intValue = soundPool.load(it, 1)
            }
            context.assets.openFd("funbird_point.wav").use {
                pointSoundId.intValue = soundPool.load(it, 1)
            }
            context.assets.openFd("funbird_hit.wav").use {
                hitSoundId.intValue = soundPool.load(it, 1)
            }
        } catch (_: Exception) {
            // Sounds are optional enhancement; game stays fully playable muted.
        }
    }

    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }

    BackHandler { onBackClick() }

    var showInstructions by rememberSaveable { mutableStateOf(true) }
    val blurRadius = retroGameDialogBlur(showInstructions)
    var resetAfterInstructions by rememberSaveable { mutableStateOf(true) }
    var phase by rememberSaveable { mutableStateOf(FunBirdPhase.Ready) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var bestScore by rememberSaveable { mutableIntStateOf(0) }
    var birdY by remember { mutableFloatStateOf(0f) }
    var birdVelocity by remember { mutableFloatStateOf(0f) }
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    var nextPipeId by remember { mutableIntStateOf(0) }
    val pipes = remember { mutableStateListOf<FunBirdPipe>() }
    val scoredPipes = remember { mutableStateListOf<Int>() }
    var lastFrameTime by remember { mutableLongStateOf(0L) }
    var backgroundOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(stats) {
        bestScore = max(bestScore, stats.games[GameId.FUN_BIRD]?.bestScore ?: 0)
    }

    val accentColor = MaterialTheme.colorScheme.primary
    val currentLevel = ((score / SCORE_PER_LEVEL) + 1).coerceAtMost(MAX_LEVEL)
    val levelStyles = remember {
        listOf(
            FunBirdLevelStyle(
                skyColors = listOf(Color(0xFFB3E5FC), Color(0xFFE0F7FA)),
                hillLightColor = Color(0xFF81C784),
                hillDarkColor = Color(0xFF66BB6A),
                cloudColor = Color(0xFFFFFFFF),
                stripeColor = Color(0x40FFFFFF),
                pipeColor = Color(0xFF4CAF50),
                pipeHighlight = Color(0xFFA5D6A7),
                pipeShadow = Color(0xFF2E7D32),
                cloudLayers = listOf(
                    FunBirdCloudLayer(0.22f, 0.14f, 0.42f, 0.35f),
                    FunBirdCloudLayer(
                        0.32f,
                        0.12f,
                        0.46f,
                        0.28f,
                        alpha = 0.85f,
                        phaseOffset = 0.45f
                    ),
                    FunBirdCloudLayer(0.26f, 0.1f, 0.38f, 0.42f, alpha = 0.75f, phaseOffset = 0.2f),
                ),
            ),
            FunBirdLevelStyle(
                skyColors = listOf(Color(0xFFFFE0B2), Color(0xFFFFF8E1)),
                hillLightColor = Color(0xFFFFCC80),
                hillDarkColor = Color(0xFFFFB74D),
                cloudColor = Color(0xFFFFF3E0),
                stripeColor = Color(0x4DFFE082),
                pipeColor = Color(0xFFFFA726),
                pipeHighlight = Color(0xFFFFCC80),
                pipeShadow = Color(0xFFF57C00),
                cloudLayers = listOf(
                    FunBirdCloudLayer(0.18f, 0.11f, 0.5f, 0.3f),
                    FunBirdCloudLayer(0.3f, 0.09f, 0.45f, 0.36f, alpha = 0.8f, phaseOffset = 0.35f),
                    FunBirdCloudLayer(
                        0.24f,
                        0.12f,
                        0.58f,
                        0.28f,
                        alpha = 0.9f,
                        phaseOffset = 0.18f
                    ),
                ),
                overlay = FunBirdSkyOverlay(
                    colors = listOf(Color(0x66FF8F00), Color.Transparent),
                    heightFraction = 0.55f,
                    alpha = 0.8f
                ),
            ),
            FunBirdLevelStyle(
                skyColors = listOf(Color(0xFFBA68C8), Color(0xFFFFCDD2)),
                hillLightColor = Color(0xFF90CAF9),
                hillDarkColor = Color(0xFF64B5F6),
                cloudColor = Color(0xFFFCE4EC),
                stripeColor = Color(0x40FFFFFF),
                pipeColor = Color(0xFF7E57C2),
                pipeHighlight = Color(0xFFB39DDB),
                pipeShadow = Color(0xFF5E35B1),
                cloudLayers = listOf(
                    FunBirdCloudLayer(0.2f, 0.13f, 0.36f, 0.33f, alpha = 0.85f),
                    FunBirdCloudLayer(0.28f, 0.1f, 0.42f, 0.37f, alpha = 0.75f, phaseOffset = 0.5f),
                    FunBirdCloudLayer(
                        0.34f,
                        0.09f,
                        0.48f,
                        0.4f,
                        alpha = 0.65f,
                        phaseOffset = 0.25f
                    ),
                ),
                overlay = FunBirdSkyOverlay(
                    colors = listOf(Color(0x66EC407A), Color.Transparent),
                    heightFraction = 0.6f,
                    alpha = 0.9f
                ),
            ),
            FunBirdLevelStyle(
                skyColors = listOf(Color(0xFF0D1B2A), Color(0xFF1B263B)),
                hillLightColor = Color(0xFF394B63),
                hillDarkColor = Color(0xFF223043),
                cloudColor = Color(0xFFCFD8DC),
                stripeColor = Color(0x4015C3D6),
                pipeColor = Color(0xFF26C6DA),
                pipeHighlight = Color(0xFF80DEEA),
                pipeShadow = Color(0xFF0097A7),
                sunColor = null,
                sunGlowColor = null,
                moonColor = Color(0xFFE0F7FA),
                cloudLayers = listOf(
                    FunBirdCloudLayer(0.22f, 0.12f, 0.46f, 0.2f, alpha = 0.7f),
                    FunBirdCloudLayer(0.3f, 0.1f, 0.52f, 0.24f, alpha = 0.6f, phaseOffset = 0.5f),
                ),
                stars = listOf(
                    FunBirdStar(0.15f, 0.18f, 0.007f, 0.9f),
                    FunBirdStar(0.28f, 0.12f, 0.005f, 0.65f),
                    FunBirdStar(0.42f, 0.2f, 0.009f, 0.8f),
                    FunBirdStar(0.58f, 0.1f, 0.0065f, 0.7f),
                    FunBirdStar(0.72f, 0.17f, 0.0075f, 0.9f),
                    FunBirdStar(0.85f, 0.08f, 0.0055f, 0.75f),
                    FunBirdStar(0.33f, 0.25f, 0.006f, 0.62f),
                    FunBirdStar(0.64f, 0.28f, 0.008f, 0.85f),
                ),
            ),
            FunBirdLevelStyle(
                skyColors = listOf(Color(0xFF162447), Color(0xFF1F4068)),
                hillLightColor = Color(0xFF374785),
                hillDarkColor = Color(0xFF24305E),
                cloudColor = Color(0xFFB3E5FC),
                stripeColor = Color(0x40FFFFFF),
                pipeColor = Color(0xFF7E57C2),
                pipeHighlight = Color(0xFFB388FF),
                pipeShadow = Color(0xFF5E35B1),
                sunColor = null,
                sunGlowColor = null,
                moonColor = Color(0xFFB2EBF2),
                cloudLayers = listOf(
                    FunBirdCloudLayer(0.18f, 0.11f, 0.38f, 0.24f, alpha = 0.85f),
                    FunBirdCloudLayer(
                        0.27f,
                        0.09f,
                        0.42f,
                        0.3f,
                        alpha = 0.75f,
                        phaseOffset = 0.45f
                    ),
                    FunBirdCloudLayer(0.34f, 0.1f, 0.48f, 0.28f, alpha = 0.7f, phaseOffset = 0.2f),
                ),
                stars = listOf(
                    FunBirdStar(0.12f, 0.16f, 0.006f, 0.9f, color = Color(0xFFB2EBF2)),
                    FunBirdStar(0.22f, 0.08f, 0.0045f, 0.7f),
                    FunBirdStar(0.38f, 0.18f, 0.0075f, 0.85f, color = Color(0xFFE1BEE7)),
                    FunBirdStar(0.55f, 0.1f, 0.0055f, 0.75f),
                    FunBirdStar(0.7f, 0.22f, 0.0065f, 0.82f, color = Color(0xFFB2EBF2)),
                    FunBirdStar(0.83f, 0.14f, 0.005f, 0.78f),
                ),
                overlay = FunBirdSkyOverlay(
                    colors = listOf(Color(0x8038E0D0), Color(0x803A60A9), Color.Transparent),
                    heightFraction = 0.6f,
                    alpha = 1f
                ),
            )
        )
    }
    require(levelStyles.size >= MAX_LEVEL) { "FunBird requires at least $MAX_LEVEL level styles." }
    val currentStyle = levelStyles[currentLevel - 1]
    val birdBodyColor = Color(0xFFFFEB3B)
    val birdWingColor = Color(0xFFFBC02D)
    val birdBellyColor = Color(0xFFFFFF8D)
    val birdBeakColor = Color(0xFFFF8F00)
    val birdEyeColor = Color(0xFF263238)
    val birdEyeHighlightColor = Color.White
    val birdCrestColor = Color(0xFFFFC107)
    val birdCheekColor = Color(0xFFFFCDD2)

    val gravity = with(density) { 780.dp.toPx() }
    val flapImpulse = -with(density) { 250.dp.toPx() }
    val pipeSpeed = with(density) { 200.dp.toPx() }
    val pipeSpacing = with(density) { 210.dp.toPx() }
    val pipeWidth = with(density) { 64.dp.toPx() }
    val gapHeight = with(density) { 190.dp.toPx() }
    val groundHeight = with(density) { 80.dp.toPx() }
    val verticalPadding = with(density) { 16.dp.toPx() }
    val birdBodyWidth = with(density) { 46.dp.toPx() }
    val birdBodyHeight = with(density) { 32.dp.toPx() }
    val birdHeadRadius = with(density) { 15.dp.toPx() }
    val birdWingWidth = birdBodyWidth * 0.6f
    val birdWingHeight = birdBodyHeight * 0.75f
    val birdX = with(density) { 72.dp.toPx() }

    fun spawnPipe(startX: Float) {
        if (layoutSize.height == 0) return
        val stageHeight = layoutSize.height.toFloat()
        val availableGapSpace = stageHeight - groundHeight - verticalPadding * 2f
        if (availableGapSpace < birdBodyHeight * 1.1f) return
        val gapSize = min(gapHeight, availableGapSpace)
        val minCenter = verticalPadding + gapSize / 2f
        val maxCenter = stageHeight - groundHeight - verticalPadding - gapSize / 2f
        if (maxCenter < minCenter) return
        val range = maxCenter - minCenter
        val center = if (range <= 0f) minCenter else Random.nextFloat() * range + minCenter
        pipes.add(
            FunBirdPipe(
                id = nextPipeId++,
                x = startX,
                gapCenter = center,
                gapSize = gapSize
            )
        )
    }

    fun prepareGame() {
        if (layoutSize.width == 0) return
        pipes.clear()
        scoredPipes.clear()
        nextPipeId = 0
        score = 0
        birdY = layoutSize.height / 2f
        birdVelocity = 0f
        lastFrameTime = 0L
        val baseX = layoutSize.width.toFloat() * 0.9f
        repeat(4) { index ->
            spawnPipe(baseX + index * pipeSpacing)
        }
    }

    fun resetGame() {
        prepareGame()
        phase = FunBirdPhase.Ready
    }

    fun startGame() {
        if (layoutSize.width == 0) return
        if (pipes.isEmpty()) {
            prepareGame()
        }
        phase = FunBirdPhase.Playing
        gameSession.start()
        birdVelocity = flapImpulse
        lastFrameTime = 0L
        playFunBirdSound(flapSoundId.intValue, soundPool)
    }

    fun resumeGame() {
        phase = FunBirdPhase.Playing
        lastFrameTime = 0L
    }

    fun pauseGame() {
        if (phase == FunBirdPhase.Playing) {
            phase = FunBirdPhase.Paused
        }
    }

    fun finishGame() {
        if (phase == FunBirdPhase.GameOver) return
        bestScore = max(bestScore, score)
        gameSession.complete(GameCompletion(score = score))
        phase = FunBirdPhase.GameOver
        playFunBirdSound(hitSoundId.intValue, soundPool)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun handleFlap() {
        when (phase) {
            FunBirdPhase.Ready -> {
                startGame()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }

            FunBirdPhase.Playing -> {
                birdVelocity = flapImpulse
                lastFrameTime = 0L
                playFunBirdSound(flapSoundId.intValue, soundPool)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }

            FunBirdPhase.Paused -> resumeGame()
            FunBirdPhase.GameOver -> {
                resetGame()
                startGame()
            }
        }
    }

    LaunchedEffect(layoutSize) {
        if (layoutSize.width > 0 && layoutSize.height > 0) {
            prepareGame()
        }
    }

    LaunchedEffect(phase, layoutSize) {
        if (phase != FunBirdPhase.Playing || layoutSize.width == 0) return@LaunchedEffect
        while (phase == FunBirdPhase.Playing) {
            val frameTime = withFrameNanos { it }
            if (lastFrameTime == 0L) {
                lastFrameTime = frameTime
                continue
            }
            val delta = (frameTime - lastFrameTime) / 1_000_000_000f
            lastFrameTime = frameTime

            if (layoutSize.width > 0) {
                val newOffset = (backgroundOffset + pipeSpeed * 0.35f * delta) % layoutSize.width
                backgroundOffset = if (newOffset >= 0f) newOffset else newOffset + layoutSize.width
            }

            birdVelocity += gravity * delta
            birdY += birdVelocity * delta

            val maxBirdY = layoutSize.height.toFloat() - birdBodyHeight / 2f
            val minBirdY = birdBodyHeight / 2f
            birdY = birdY.coerceIn(minBirdY, maxBirdY)
            if (birdY >= maxBirdY - 1f) {
                finishGame()
            }

            if (pipes.isNotEmpty()) {
                val updated = ArrayList<FunBirdPipe>(pipes.size)
                pipes.forEach { pipe ->
                    updated.add(pipe.copy(x = pipe.x - pipeSpeed * delta))
                }
                pipes.clear()
                pipes.addAll(updated)

                while (pipes.isNotEmpty() && pipes.first().x + pipeWidth < 0f) {
                    val removed = pipes.removeAt(0)
                    scoredPipes.remove(removed.id)
                }

                while (pipes.size < 4) {
                    val baseX = if (pipes.isEmpty()) {
                        layoutSize.width.toFloat() * 0.9f + pipeSpacing
                    } else {
                        pipes.last().x + pipeSpacing
                    }
                    spawnPipe(baseX)
                }

                val birdLeft = birdX - birdBodyWidth / 2f
                val birdRight = birdX + birdBodyWidth / 2f
                val birdTop = birdY - birdBodyHeight / 2f
                val birdBottom = birdY + birdBodyHeight / 2f

                for (pipe in pipes) {
                    val pipeLeft = pipe.x
                    val pipeRight = pipe.x + pipeWidth
                    if (birdRight > pipeLeft && birdLeft < pipeRight) {
                        val gapTop = pipe.gapCenter - pipe.gapSize / 2f
                        val gapBottom = pipe.gapCenter + pipe.gapSize / 2f
                        if (birdTop < gapTop || birdBottom > gapBottom) {
                            finishGame()
                            break
                        }
                    }
                    if (!scoredPipes.contains(pipe.id) && pipeRight < birdX) {
                        scoredPipes.add(pipe.id)
                        score += 1
                        bestScore = max(bestScore, score)
                        playFunBirdSound(pointSoundId.intValue, soundPool)
                    }
                }
            }
        }
    }

    val primaryLabel = when (phase) {
        FunBirdPhase.Ready -> stringResource(R.string.play_label)
        FunBirdPhase.Playing -> stringResource(R.string.pause_label)
        FunBirdPhase.Paused -> stringResource(R.string.resume_label)
        FunBirdPhase.GameOver -> stringResource(R.string.play_label)
    }
    val primaryAction: () -> Unit = when (phase) {
        FunBirdPhase.Ready -> {
            { startGame() }
        }

        FunBirdPhase.Playing -> {
            { pauseGame() }
        }

        FunBirdPhase.Paused -> {
            { resumeGame() }
        }

        FunBirdPhase.GameOver -> {
            { resetGame(); startGame() }
        }
    }
    val resetEnabled = phase != FunBirdPhase.Ready || score > 0


    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.fun_bird_title),
                onBackClick = onBackClick,
                appSettingsViewModel = appSettingsViewModel,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .blur(blurRadius),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ScoreCard(
                entries = listOf(
                    ScoreCardEntry(
                        title = stringResource(R.string.score_label),
                        value = score.toString()
                    ),
                    ScoreCardEntry(
                        title = stringResource(R.string.best_score_label),
                        value = bestScore.toString()
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(36.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.35f
                    )
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(36.dp))
                        .pointerInput(phase) {
                            detectTapGestures { handleFlap() }
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { layoutSize = it }
                    ) {
                        drawScenery(
                            style = currentStyle,
                            offset = backgroundOffset,
                            groundHeight = groundHeight
                        )

                        pipes.forEach { pipe ->
                            drawPipe(
                                pipe = pipe,
                                pipeWidth = pipeWidth,
                                gapSize = pipe.gapSize,
                                groundHeight = groundHeight,
                                pipeColor = currentStyle.pipeColor,
                                pipeHighlight = currentStyle.pipeHighlight,
                                pipeShadow = currentStyle.pipeShadow
                            )
                        }

                        drawBird(
                            center = Offset(birdX, birdY),
                            bodyWidth = birdBodyWidth,
                            bodyHeight = birdBodyHeight,
                            headRadius = birdHeadRadius,
                            wingWidth = birdWingWidth,
                            wingHeight = birdWingHeight,
                            bodyColor = birdBodyColor,
                            wingColor = birdWingColor,
                            bellyColor = birdBellyColor,
                            beakColor = birdBeakColor,
                            eyeColor = birdEyeColor,
                            eyeHighlightColor = birdEyeHighlightColor,
                            crestColor = birdCrestColor,
                            cheekColor = birdCheekColor
                        )
                    }

                    val overlayTitle = when (phase) {
                        FunBirdPhase.Ready -> stringResource(R.string.fun_bird_ready_message)
                        FunBirdPhase.Paused -> stringResource(R.string.fun_bird_paused_title)
                        FunBirdPhase.GameOver -> stringResource(R.string.fun_bird_game_over_title)
                        else -> null
                    }
                    val overlaySubtitle = when (phase) {
                        FunBirdPhase.Ready -> stringResource(R.string.fun_bird_tap_hint)
                        FunBirdPhase.Paused -> stringResource(R.string.fun_bird_resume_hint)
                        FunBirdPhase.GameOver -> stringResource(R.string.fun_bird_game_over)
                        else -> null
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = overlayTitle != null || overlaySubtitle != null,
                        enter = fadeIn(animationSpec = tween(durationMillis = 240)) +
                                scaleIn(
                                    initialScale = 0.9f,
                                    animationSpec = tween(durationMillis = 320)
                                ),
                        exit = fadeOut(animationSpec = tween(durationMillis = 160))
                    ) {
                        val overlayColor = if (phase == FunBirdPhase.GameOver) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(overlayColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                overlayTitle?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                overlaySubtitle?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (phase == FunBirdPhase.GameOver) {
                                    Text(
                                        text = stringResource(
                                            R.string.fun_bird_final_score,
                                            score
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = primaryAction,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = accentColor.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    Text(text = primaryLabel, style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = { resetGame() },
                    enabled = resetEnabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    Text(
                        text = stringResource(R.string.reset_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showInstructions) {
        RetroGameInstructionsDialog(
            title = stringResource(R.string.instructions_title),
            instructions = stringResource(R.string.fun_bird_instructions),
            onPlayClick = {
                showInstructions = false
                if (resetAfterInstructions) {
                    resetGame()
                } else if (phase == FunBirdPhase.Paused) {
                    resumeGame()
                }
                resetAfterInstructions = false
            },
            onDismiss = {
                showInstructions = false
                if (resetAfterInstructions) {
                    onBackClick()
                } else if (phase == FunBirdPhase.Paused) {
                    resumeGame()
                }
                resetAfterInstructions = false
            },
        )
    }
}

private fun playFunBirdSound(soundId: Int, soundPool: SoundPool, rate: Float = 1f) {
    if (soundId == -1) return
    try {
        soundPool.play(soundId, 0.5f, 0.5f, 1, 0, rate.coerceIn(0.5f, 2f))
    } catch (_: Exception) {
        // Ignore playback failures; never break gameplay for sound.
    }
}

private fun DrawScope.drawScenery(
    style: FunBirdLevelStyle,
    offset: Float,
    groundHeight: Float
) {
    drawRect(
        brush = Brush.verticalGradient(colors = style.skyColors),
        size = size
    )

    style.overlay?.let { overlay ->
        val overlayHeight = size.height * overlay.heightFraction
        if (overlayHeight > 0f) {
            val overlayColors = overlay.colors.map { color ->
                color.copy(alpha = color.alpha * overlay.alpha)
            }
            drawRect(
                brush = Brush.verticalGradient(colors = overlayColors),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, overlayHeight)
            )
        }
    }

    style.sunColor?.let { sunColor ->
        val sunRadius = size.minDimension * 0.12f
        val sunCenter = Offset(size.width * 0.15f, size.height * 0.18f)
        drawCircle(
            color = sunColor,
            radius = sunRadius,
            center = sunCenter
        )
        style.sunGlowColor?.let { glowColor ->
            drawCircle(
                color = glowColor,
                radius = sunRadius * 1.6f,
                center = sunCenter
            )
        }
    }

    style.moonColor?.let { moonColor ->
        val moonRadius = size.minDimension * 0.09f
        val moonCenter = Offset(size.width * 0.78f, size.height * 0.2f)
        drawMoon(center = moonCenter, radius = moonRadius, color = moonColor)
    }

    if (style.stars.isNotEmpty()) {
        style.stars.forEach { star ->
            val starCenter = Offset(size.width * star.xFactor, size.height * star.yFactor)
            val starRadius = size.minDimension * star.radiusFactor
            drawCircle(
                color = star.color.copy(alpha = star.alpha),
                radius = starRadius,
                center = starCenter
            )
        }
    }

    if (style.cloudLayers.isNotEmpty()) {
        style.cloudLayers.forEach { layer ->
            val spacing = size.width * layer.spacingFraction
            if (spacing <= 0f) return@forEach
            val adjustedOffset = (-offset * layer.speedMultiplier) + spacing * layer.phaseOffset
            var baseX = ((adjustedOffset % spacing) + spacing) % spacing
            baseX -= spacing
            while (baseX < size.width + spacing) {
                drawCloud(
                    center = Offset(baseX, size.height * layer.heightFactor),
                    baseRadius = size.minDimension * layer.sizeFactor,
                    color = style.cloudColor.copy(alpha = layer.alpha)
                )
                baseX += spacing
            }
        }
    }

    val hillBaseline = size.height - groundHeight
    val hillHeight = groundHeight * 1.4f
    val hillPath = Path().apply {
        moveTo(-size.width * 0.1f, hillBaseline)
        cubicTo(
            size.width * 0.15f,
            hillBaseline - hillHeight,
            size.width * 0.4f,
            hillBaseline + hillHeight * 0.2f,
            size.width * 0.75f,
            hillBaseline - hillHeight * 0.7f
        )
        lineTo(size.width * 1.2f, hillBaseline)
        lineTo(size.width * 1.2f, size.height)
        lineTo(-size.width * 0.1f, size.height)
        close()
    }
    drawPath(hillPath, style.hillLightColor)

    val hillPath2 = Path().apply {
        moveTo(-size.width * 0.05f, hillBaseline + hillHeight * 0.2f)
        cubicTo(
            size.width * 0.25f,
            hillBaseline - hillHeight * 0.3f,
            size.width * 0.55f,
            hillBaseline,
            size.width * 0.95f,
            hillBaseline - hillHeight * 0.4f
        )
        lineTo(size.width * 1.1f, hillBaseline + hillHeight * 0.3f)
        lineTo(size.width * 1.1f, size.height)
        lineTo(-size.width * 0.05f, size.height)
        close()
    }
    drawPath(hillPath2, style.hillDarkColor)

    drawRect(
        color = style.hillDarkColor.copy(alpha = 0.12f),
        topLeft = Offset(0f, hillBaseline - 8f),
        size = Size(size.width, 8f)
    )

    drawRect(
        color = style.hillDarkColor.copy(alpha = 0.08f),
        topLeft = Offset(0f, hillBaseline - groundHeight * 0.3f),
        size = Size(size.width, groundHeight * 0.3f)
    )

    drawRect(
        color = style.hillDarkColor.copy(alpha = 0.22f),
        topLeft = Offset(0f, size.height - groundHeight),
        size = Size(size.width, groundHeight)
    )

    val stripeWidth = size.width / 18f
    var stripeX = (offset * 0.6f) % stripeWidth
    if (stripeX < 0f) stripeX += stripeWidth
    while (stripeX < size.width + stripeWidth) {
        drawRoundRect(
            color = style.stripeColor,
            topLeft = Offset(stripeX - stripeWidth, size.height - groundHeight + 6f),
            size = Size(stripeWidth * 0.45f, groundHeight - 12f),
            cornerRadius = CornerRadius(12f, 12f)
        )
        stripeX += stripeWidth
    }
}

private fun DrawScope.drawCloud(center: Offset, baseRadius: Float, color: Color) {
    drawCircle(color = color, radius = baseRadius, center = center)
    drawCircle(
        color = color,
        radius = baseRadius * 0.75f,
        center = center + Offset(-baseRadius * 0.8f, baseRadius * 0.2f)
    )
    drawCircle(
        color = color,
        radius = baseRadius * 0.65f,
        center = center + Offset(baseRadius * 0.85f, baseRadius * 0.15f)
    )
    drawCircle(
        color = color,
        radius = baseRadius * 0.5f,
        center = center + Offset(0f, baseRadius * 0.45f)
    )
}

private fun DrawScope.drawMoon(center: Offset, radius: Float, color: Color) {
    drawCircle(
        color = color.copy(alpha = 0.9f),
        radius = radius,
        center = center
    )
    drawCircle(
        color = color.copy(alpha = 0.35f),
        radius = radius * 1.35f,
        center = center
    )
    val craterColor = color.copy(alpha = 0.6f)
    drawCircle(
        color = craterColor,
        radius = radius * 0.24f,
        center = center + Offset(radius * 0.3f, radius * 0.05f)
    )
    drawCircle(
        color = craterColor,
        radius = radius * 0.18f,
        center = center + Offset(-radius * 0.28f, -radius * 0.22f)
    )
    drawCircle(
        color = craterColor,
        radius = radius * 0.15f,
        center = center + Offset(-radius * 0.1f, radius * 0.28f)
    )
}

private fun DrawScope.drawPipe(
    pipe: FunBirdPipe,
    pipeWidth: Float,
    gapSize: Float,
    groundHeight: Float,
    pipeColor: Color,
    pipeHighlight: Color,
    pipeShadow: Color
) {
    val topHeight = (pipe.gapCenter - gapSize / 2f).coerceAtLeast(0f)
    if (topHeight > 0f) {
        val capHeight = pipeWidth * 0.35f
        drawRoundRect(
            color = pipeColor,
            topLeft = Offset(pipe.x, 0f),
            size = Size(pipeWidth, topHeight),
            cornerRadius = CornerRadius(26f, 26f)
        )
        drawRoundRect(
            color = pipeHighlight,
            topLeft = Offset(pipe.x + pipeWidth * 0.12f, topHeight * 0.1f),
            size = Size(pipeWidth * 0.76f, min(topHeight * 0.18f, 56f)),
            cornerRadius = CornerRadius(20f, 20f)
        )
        drawRoundRect(
            color = pipeShadow,
            topLeft = Offset(pipe.x, topHeight - 16f),
            size = Size(pipeWidth, 20f),
            cornerRadius = CornerRadius(18f, 18f)
        )
        drawRoundRect(
            color = pipeColor,
            topLeft = Offset(pipe.x - pipeWidth * 0.08f, topHeight - capHeight),
            size = Size(pipeWidth * 1.16f, capHeight),
            cornerRadius = CornerRadius(26f, 26f)
        )
        drawRoundRect(
            color = pipeShadow.copy(alpha = 0.45f),
            topLeft = Offset(pipe.x - pipeWidth * 0.08f, topHeight - capHeight),
            size = Size(pipeWidth * 1.16f, capHeight * 0.4f),
            cornerRadius = CornerRadius(22f, 22f)
        )
    }

    val bottomTop = pipe.gapCenter + gapSize / 2f
    val bottomHeight = (size.height - groundHeight - bottomTop).coerceAtLeast(0f)
    if (bottomHeight > 0f) {
        val capHeight = pipeWidth * 0.35f
        drawRoundRect(
            color = pipeColor,
            topLeft = Offset(pipe.x, bottomTop),
            size = Size(pipeWidth, bottomHeight),
            cornerRadius = CornerRadius(26f, 26f)
        )
        drawRoundRect(
            color = pipeHighlight,
            topLeft = Offset(pipe.x + pipeWidth * 0.12f, bottomTop + bottomHeight * 0.45f),
            size = Size(pipeWidth * 0.76f, min(bottomHeight * 0.18f, 56f)),
            cornerRadius = CornerRadius(20f, 20f)
        )
        drawRoundRect(
            color = pipeShadow,
            topLeft = Offset(pipe.x, bottomTop),
            size = Size(pipeWidth, 20f),
            cornerRadius = CornerRadius(18f, 18f)
        )
        drawRoundRect(
            color = pipeColor,
            topLeft = Offset(pipe.x - pipeWidth * 0.08f, bottomTop),
            size = Size(pipeWidth * 1.16f, capHeight),
            cornerRadius = CornerRadius(26f, 26f)
        )
        drawRoundRect(
            color = pipeHighlight.copy(alpha = 0.4f),
            topLeft = Offset(pipe.x - pipeWidth * 0.04f, bottomTop + capHeight * 0.35f),
            size = Size(pipeWidth * 1.08f, capHeight * 0.35f),
            cornerRadius = CornerRadius(20f, 20f)
        )
    }
}

private fun DrawScope.drawBird(
    center: Offset,
    bodyWidth: Float,
    bodyHeight: Float,
    headRadius: Float,
    wingWidth: Float,
    wingHeight: Float,
    bodyColor: Color,
    wingColor: Color,
    bellyColor: Color,
    beakColor: Color,
    eyeColor: Color,
    eyeHighlightColor: Color,
    crestColor: Color,
    cheekColor: Color
) {
    val bodyRect = Rect(
        left = center.x - bodyWidth / 2f,
        top = center.y - bodyHeight / 2f,
        right = center.x + bodyWidth / 2f,
        bottom = center.y + bodyHeight / 2f
    )
    drawOval(
        color = bodyColor,
        topLeft = Offset(bodyRect.left, bodyRect.top),
        size = Size(bodyRect.width, bodyRect.height)
    )

    val bellyRect = Rect(
        left = bodyRect.left + bodyWidth * 0.28f,
        top = center.y - bodyHeight * 0.05f,
        right = bodyRect.right - bodyWidth * 0.08f,
        bottom = bodyRect.bottom - bodyHeight * 0.08f
    )
    drawOval(
        color = bellyColor,
        topLeft = Offset(bellyRect.left, bellyRect.top),
        size = Size(bellyRect.width, bellyRect.height)
    )

    val tailPath = Path().apply {
        moveTo(bodyRect.left + bodyWidth * 0.05f, center.y - bodyHeight * 0.1f)
        lineTo(bodyRect.left - bodyWidth * 0.22f, center.y - bodyHeight * 0.38f)
        lineTo(bodyRect.left - bodyWidth * 0.28f, center.y)
        lineTo(bodyRect.left - bodyWidth * 0.2f, center.y + bodyHeight * 0.36f)
        lineTo(bodyRect.left + bodyWidth * 0.05f, center.y + bodyHeight * 0.1f)
        close()
    }
    drawPath(path = tailPath, color = wingColor.copy(alpha = 0.95f))

    val wingRect = Rect(
        left = center.x - wingWidth * 0.75f,
        top = center.y - wingHeight * 0.35f,
        right = center.x + wingWidth * 0.35f,
        bottom = center.y + wingHeight * 0.7f
    )
    drawOval(
        color = wingColor,
        topLeft = Offset(wingRect.left, wingRect.top),
        size = Size(wingRect.width, wingRect.height)
    )

    val headCenter = Offset(
        x = center.x + bodyWidth * 0.32f,
        y = center.y - bodyHeight * 0.12f
    )
    val crestPath = Path().apply {
        moveTo(headCenter.x - headRadius * 0.35f, headCenter.y - headRadius * 1.05f)
        quadraticTo(
            headCenter.x,
            headCenter.y - headRadius * 1.7f,
            headCenter.x + headRadius * 0.4f,
            headCenter.y - headRadius * 1.05f
        )
        lineTo(headCenter.x + headRadius * 0.12f, headCenter.y - headRadius * 0.6f)
        lineTo(headCenter.x - headRadius * 0.18f, headCenter.y - headRadius * 0.7f)
        close()
    }
    drawPath(path = crestPath, color = crestColor)

    drawCircle(
        color = bodyColor,
        radius = headRadius * 1.05f,
        center = headCenter
    )

    val beakPath = Path().apply {
        moveTo(headCenter.x + headRadius * 0.65f, headCenter.y - headRadius * 0.1f)
        lineTo(headCenter.x + headRadius * 1.45f, headCenter.y - headRadius * 0.3f)
        lineTo(headCenter.x + headRadius * 1.5f, headCenter.y + headRadius * 0.15f)
        lineTo(headCenter.x + headRadius * 0.7f, headCenter.y + headRadius * 0.35f)
        close()
    }
    drawPath(path = beakPath, color = beakColor)

    val eyeCenter = Offset(
        x = headCenter.x + headRadius * 0.28f,
        y = headCenter.y - headRadius * 0.05f
    )
    drawCircle(
        color = Color.White,
        radius = headRadius * 0.42f,
        center = eyeCenter
    )
    drawCircle(
        color = eyeColor,
        radius = headRadius * 0.2f,
        center = eyeCenter + Offset(headRadius * 0.05f, 0f)
    )
    drawCircle(
        color = eyeHighlightColor,
        radius = headRadius * 0.1f,
        center = eyeCenter + Offset(headRadius * 0.15f, -headRadius * 0.12f)
    )

    drawCircle(
        color = cheekColor,
        radius = headRadius * 0.22f,
        center = headCenter + Offset(-headRadius * 0.25f, headRadius * 0.3f)
    )
}

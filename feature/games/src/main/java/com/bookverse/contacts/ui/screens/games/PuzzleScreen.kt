package com.bookverse.contacts.ui.screens.games

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.bookverse.contacts.feature.games.R
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameOutcome
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.domain.games.GameStatsSnapshot
import com.bookverse.contacts.ui.components.GameSetupDialog
import com.bookverse.contacts.ui.components.GameSetupOption
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.RetroGameOptionsDialog
import com.bookverse.contacts.ui.components.ScoreCard
import com.bookverse.contacts.ui.components.ScoreCardEntry
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.retroGameDialogBlur
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RotateCcw
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("DefaultLocale", "UnusedBoxWithConstraintsScope")
@Composable
fun PuzzleScreen(
    onBackClick: () -> Unit,
    gameStatsRepository: GameStatsRepository,
) {
    val context = LocalContext.current
    val gameSession = rememberGameSessionRecorder(GameId.PUZZLE, gameStatsRepository)
    val stats by gameStatsRepository.stats.collectAsStateWithLifecycle(
        initialValue = GameStatsSnapshot(),
    )
    var bestTimeSeconds by remember { mutableIntStateOf(0) }
    val gridSize = 4
    var board by remember { mutableStateOf(generatePuzzle(gridSize)) }
    var ids by remember { mutableStateOf(generateIds(gridSize)) }
    var won by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(0) }
    var views by remember { mutableIntStateOf(0) }
    val accentColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    var showSetupDialog by remember { mutableStateOf(true) }
    var setupSelectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showImageErrorDialog by remember { mutableStateOf(false) }
    var puzzleMode by remember { mutableStateOf(PuzzleMode.Numbers) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var pendingCropDestination by remember { mutableStateOf<Uri?>(null) }
    var showImagePreviewDialog by remember { mutableStateOf(false) }
    var previewIsHint by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }

    val anyDialogVisible =
        showSetupDialog || showImageSourceDialog || showImagePreviewDialog
    val blurRadius = retroGameDialogBlur(anyDialogVisible)

    val defaultImageRes = remember {
        listOf(
            CoreUiR.drawable.puzzle_sample1,
            CoreUiR.drawable.puzzle_sample2,
            CoreUiR.drawable.puzzle_sample3,
        )
    }

    fun startPuzzle(mode: PuzzleMode, bitmapOverride: ImageBitmap? = null) {
        puzzleMode = mode
        showImageErrorDialog = false
        if (mode == PuzzleMode.Image) {
            val resolvedBitmap = bitmapOverride
                ?: imageBitmap
                ?: loadDrawableAsBitmap(context, defaultImageRes.first())

            if (resolvedBitmap == null) {
                showImageSourceDialog = true
                return
            }

            imageBitmap = resolvedBitmap
            previewIsHint = false
            showImagePreviewDialog = true
            hasStarted = false
        } else {
            showImagePreviewDialog = false
            hasStarted = true
            gameSession.start()
        }

        board = generatePuzzle(gridSize)
        ids = generateIds(gridSize)
        won = false
        attempts = 0
        elapsedSeconds = 0
        views = 0
    }

    val cropLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val outputUri = result.data?.let { UCrop.getOutput(it) } ?: pendingCropDestination
            pendingCropDestination = null

            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    val bitmap = outputUri?.let { loadImageBitmap(context, it) }
                    if (bitmap != null) {
                        showImageSourceDialog = false
                        startPuzzle(PuzzleMode.Image, bitmap)
                    } else {
                        showImageErrorDialog = true
                        showImageSourceDialog = false
                    }
                }

                Activity.RESULT_CANCELED -> {
                    showImageErrorDialog = false
                    showImageSourceDialog = true
                }

                UCrop.RESULT_ERROR -> {
                    showImageErrorDialog = true
                    showImageSourceDialog = false
                }

                else -> {
                    showImageErrorDialog = true
                    showImageSourceDialog = false
                }
            }
        }

    val pickImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val destination = runCatching {
                    File(
                        context.cacheDir,
                        "puzzle_cropped_${System.currentTimeMillis()}.jpg"
                    ).apply { createNewFile() }
                }.getOrNull()?.let(Uri::fromFile)

                if (destination != null) {
                    pendingCropDestination = destination
                    val cropIntent = UCrop.of(uri, destination)
                        .withAspectRatio(1f, 1f)
                        .withMaxResultSize(2048, 2048)
                        .withOptions(
                            createPuzzleCropOptions(
                                accentColor = accentColor,
                                textColor = textColor,
                                context = context
                            )
                        )
                        .getIntent(context)
                    cropLauncher.launch(cropIntent)
                } else {
                    showImageErrorDialog = true
                    showImageSourceDialog = false
                }
            }
        }

    LaunchedEffect(won, hasStarted) {
        if (won || !hasStarted) return@LaunchedEffect

        while (isActive) {
            delay(1000.milliseconds)
            if (won || !hasStarted) break
            elapsedSeconds++
        }
    }

    LaunchedEffect(won) {
        if (won && elapsedSeconds > 0) {
            val currentBest = bestTimeSeconds
            if (currentBest == 0 || elapsedSeconds < currentBest) {
                bestTimeSeconds = elapsedSeconds
            }
            gameSession.complete(
                GameCompletion(
                    outcome = GameOutcome.WIN,
                    durationSeconds = elapsedSeconds,
                    moves = attempts,
                )
            )
        }
    }

    LaunchedEffect(stats) {
        val storedBest = stats.games[GameId.PUZZLE]?.bestTimeSeconds ?: 0
        if (storedBest > 0 && (bestTimeSeconds == 0 || storedBest < bestTimeSeconds)) {
            bestTimeSeconds = storedBest
        }
    }

    fun moveBySwipe(direction: SwipeDirection) {
        val empty = findEmpty(board)
        val targetPos = when (direction) {
            SwipeDirection.UP -> empty.first + 1 to empty.second
            SwipeDirection.DOWN -> empty.first - 1 to empty.second
            SwipeDirection.LEFT -> empty.first to empty.second + 1
            SwipeDirection.RIGHT -> empty.first to empty.second - 1
        }

        if (targetPos.first in 0 until gridSize && targetPos.second in 0 until gridSize) {
            val newBoard = board.mapIndexed { i, row ->
                row.mapIndexed { j, v ->
                    when {
                        i == targetPos.first && j == targetPos.second -> board[empty.first][empty.second]
                        i == empty.first && j == empty.second -> board[targetPos.first][targetPos.second]
                        else -> v
                    }
                }
            }
            val newIds = ids.mapIndexed { i, row ->
                row.mapIndexed { j, v ->
                    when {
                        i == targetPos.first && j == targetPos.second -> ids[empty.first][empty.second]
                        i == empty.first && j == empty.second -> ids[targetPos.first][targetPos.second]
                        else -> v
                    }
                }
            }
            board = newBoard
            ids = newIds
            attempts++
            if (isSolved(board)) won = true
        }
    }

    BackHandler {
        onBackClick()
    }

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.puzzle_title),
                onBackClick = onBackClick,
                actions = {
                    if (puzzleMode == PuzzleMode.Numbers) {
                        IconAction(
                            imageVector = Lucide.RotateCcw,
                            contentDescription = stringResource(R.string.cd_restart_game),
                            onClick = {
                                startPuzzle(puzzleMode, null)
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
                .blur(blurRadius),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            val formattedTime =
                String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
            val scoreEntries = mutableListOf(
                ScoreCardEntry(
                    title = stringResource(R.string.time_label),
                    value = formattedTime
                )
            )
            if (puzzleMode == PuzzleMode.Image) {
                scoreEntries.add(
                    ScoreCardEntry(
                        title = stringResource(R.string.puzzle_views_label),
                        value = views.toString()
                    )
                )
            }
            scoreEntries.add(
                ScoreCardEntry(
                    title = stringResource(R.string.moves_label),
                    value = attempts.toString()
                )
            )
            ScoreCard(
                entries = scoreEntries,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = textColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    val cellSpacing = if (puzzleMode == PuzzleMode.Image) 2.dp else 8.dp
                    val gridPadding = if (puzzleMode == PuzzleMode.Image) 4.dp else 8.dp
                    val tileSize: Dp =
                        (maxWidth - gridPadding * 2 - cellSpacing * (gridSize - 1)) / gridSize
                    val animSpec =
                        tween<Dp>(durationMillis = 140, easing = LinearOutSlowInEasing)
                    val density = LocalDensity.current
                    val threshold = with(density) { 24.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(gridPadding)
                            .pointerInput(board) {
                                var dx = 0f
                                var dy = 0f
                                detectDragGestures(
                                    onDragEnd = {
                                        if (abs(dx) > abs(dy) && abs(dx) > threshold) {
                                            if (dx > 0) moveBySwipe(SwipeDirection.RIGHT) else moveBySwipe(
                                                SwipeDirection.LEFT
                                            )
                                        } else if (abs(dy) > threshold) {
                                            if (dy > 0) moveBySwipe(SwipeDirection.DOWN) else moveBySwipe(
                                                SwipeDirection.UP
                                            )
                                        }
                                        dx = 0f
                                        dy = 0f
                                    },
                                    onDrag = { _, dragAmount ->
                                        dx += dragAmount.x
                                        dy += dragAmount.y
                                    }
                                )
                            }
                    ) {
                        val tileShape = RoundedCornerShape(12.dp)

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(cellSpacing),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            repeat(gridSize) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(gridSize) {
                                        Box(
                                            modifier = Modifier
                                                .size(tileSize)
                                                .clip(tileShape)
                                                .background(Color(0xFFCDC1B4).copy(alpha = 0.3f))
                                        )
                                    }
                                }
                            }
                        }

                        val tiles = remember(board, ids) {
                            buildList {
                                for (i in 0 until gridSize) for (j in 0 until gridSize) {
                                    val value = board[i][j]
                                    val id = ids[i][j]
                                    if (value.isNotEmpty() && id != 0L) add(
                                        PuzzleTileItem(
                                            id,
                                            value,
                                            i,
                                            j
                                        )
                                    )
                                }
                            }
                        }

                        tiles.forEach { t ->
                            key(t.id) {
                                val xTarget = (tileSize + cellSpacing) * t.col
                                val yTarget = (tileSize + cellSpacing) * t.row
                                val animX by animateDpAsState(
                                    xTarget,
                                    animSpec,
                                    label = "x"
                                )
                                val animY by animateDpAsState(
                                    yTarget,
                                    animSpec,
                                    label = "y"
                                )

                                Card(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                x = animX.roundToPx(),
                                                y = animY.roundToPx(),
                                            )
                                        }
                                        .size(tileSize),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = backgroundColor
                                    ),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 8.dp
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = accentColor.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                width = 2.dp,
                                                color = textColor.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (puzzleMode == PuzzleMode.Numbers) {
                                            Text(
                                                text = t.value,
                                                fontSize = (tileSize.value * 0.4f).sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor,
                                            )
                                        } else {
                                            val bmp = imageBitmap
                                            if (bmp != null && t.value.isNotEmpty()) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    val idx = t.value.toInt() - 1
                                                    val srcW = bmp.width
                                                    val srcH = bmp.height
                                                    val tileSrcW = srcW / gridSize
                                                    val tileSrcH = srcH / gridSize
                                                    val srcCol = idx % gridSize
                                                    val srcRow = idx / gridSize
                                                    val srcX = srcCol * tileSrcW
                                                    val srcY = srcRow * tileSrcH
                                                    drawImage(
                                                        image = bmp,
                                                        srcOffset = IntOffset(
                                                            srcX,
                                                            srcY
                                                        ),
                                                        srcSize = androidx.compose.ui.unit.IntSize(
                                                            tileSrcW,
                                                            tileSrcH
                                                        ),
                                                        dstOffset = IntOffset(
                                                            0,
                                                            0
                                                        ),
                                                        dstSize = androidx.compose.ui.unit.IntSize(
                                                            size.width.toInt(),
                                                            size.height.toInt()
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            val hasPreviewAction = puzzleMode == PuzzleMode.Image
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = if (hasPreviewAction) Arrangement.spacedBy(16.dp) else Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasPreviewAction) {
                    FilledTonalButton(
                        onClick = {
                            views++
                            previewIsHint = true
                            showImagePreviewDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = accentColor.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.puzzle_hint_button),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                if (puzzleMode == PuzzleMode.Image) {
                    OutlinedButton(
                        onClick = {
                            val bitmap = if (puzzleMode == PuzzleMode.Image) imageBitmap else null
                            startPuzzle(puzzleMode, bitmap)
                        },
                        modifier = if (hasPreviewAction) {
                            Modifier.weight(1f)
                        } else {
                            Modifier.fillMaxWidth()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Text(
                            text = stringResource(CoreUiR.string.reset_label),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
        if (won) {
            MessageDialog(
                title = stringResource(R.string.puzzle_win_message),
                message1 = stringResource(R.string.moves_count, attempts) + "\n" +
                        stringResource(
                            R.string.puzzle_time, String.format(
                                "%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60
                            )
                        ),
                buttonText = stringResource(R.string.play_again),
                onButtonClick = {
                    val bitmap = if (puzzleMode == PuzzleMode.Image) imageBitmap else null
                    startPuzzle(puzzleMode, bitmap)
                },
            )
        }
    }

    if (showSetupDialog) {
        val isImageSelected = setupSelectedIndex == 1
        GameSetupDialog(
            title = stringResource(R.string.snake_setup_title),
            instructions = stringResource(
                if (isImageSelected) {
                    R.string.puzzle_instructions_image
                } else {
                    R.string.puzzle_instructions_numbers
                },
            ),
            options = listOf(
                GameSetupOption(
                    title = stringResource(R.string.puzzle_mode_numbers),
                    description = stringResource(R.string.puzzle_mode_numbers_description),
                ),
                GameSetupOption(
                    title = stringResource(R.string.puzzle_mode_image),
                    description = stringResource(R.string.puzzle_mode_image_description),
                ),
            ),
            selectedIndex = setupSelectedIndex,
            onOptionSelected = { setupSelectedIndex = it },
            accentColor = accentColor,
            onStart = {
                if (setupSelectedIndex == 0) {
                    showSetupDialog = false
                    startPuzzle(PuzzleMode.Numbers)
                } else {
                    showSetupDialog = false
                    showImageSourceDialog = true
                }
            },
            onDismiss = {
                showSetupDialog = false
                onBackClick()
            },
        )
        return
    }

    if (showImageSourceDialog) {
        val imageOptions = buildList {
            add(stringResource(R.string.puzzle_choose_from_device))
            defaultImageRes.forEachIndexed { index, _ ->
                add(stringResource(R.string.puzzle_use_default_image, index + 1))
            }
        }

        RetroGameOptionsDialog(
            title = stringResource(R.string.puzzle_image_source_title),
            options = imageOptions,
            onOptionSelected = { index ->
                showImageErrorDialog = false
                if (index == 0) {
                    showImageSourceDialog = false
                    pickImageLauncher.launch("image/*")
                } else {
                    val resId = defaultImageRes[index - 1]
                    loadDrawableAsBitmap(context, resId)?.let { bitmap ->
                        showImageSourceDialog = false
                        startPuzzle(PuzzleMode.Image, bitmap)
                    }
                }
            },
            onDismiss = {
                showImageSourceDialog = false
                showSetupDialog = true
            },
        )
        return
    }

    if (showImageErrorDialog) {
        MessageDialog(
            title = stringResource(R.string.puzzle_image_error_title),
            message1 = stringResource(R.string.puzzle_image_error_message),
            buttonText = stringResource(R.string.puzzle_image_error_retry),
            onButtonClick = {
                showImageErrorDialog = false
                showImageSourceDialog = true
            },
            onDismiss = { showImageErrorDialog = false },
        )
        return
    }

    if (showImagePreviewDialog && puzzleMode == PuzzleMode.Image) {
        val bitmap = imageBitmap
        if (bitmap != null) {
            val titleText = if (previewIsHint) {
                stringResource(R.string.puzzle_preview_hint_title)
            } else {
                stringResource(R.string.puzzle_preview_title)
            }
            val subtitleText = if (previewIsHint) {
                stringResource(R.string.puzzle_preview_hint_subtitle)
            } else {
                stringResource(R.string.puzzle_preview_subtitle)
            }
            val buttonText = if (previewIsHint) {
                stringResource(CoreUiR.string.close)
            } else {
                stringResource(CoreUiR.string.understood)
            }

            ImagePreviewDialog(
                title = titleText,
                subtitle = subtitleText,
                buttonLabel = buttonText,
                bitmap = bitmap,
                accentColor = accentColor,
                onDismiss = {
                    if (!previewIsHint) {
                        hasStarted = true
                        gameSession.start()
                    }
                    showImagePreviewDialog = false
                }
            )
        }
    }
}

private enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

private data class PuzzleTileItem(val id: Long, val value: String, val row: Int, val col: Int)

private enum class PuzzleMode { Numbers, Image }

@Composable
private fun ImagePreviewDialog(
    title: String,
    subtitle: String,
    buttonLabel: String,
    bitmap: ImageBitmap,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val buttonContentColor = if (accentColor.luminance() > 0.5f) {
        Color.Black
    } else {
        Color.White
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = subtitle,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(20.dp))

                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.puzzle_preview),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = buttonContentColor
                    ),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = buttonLabel)
                }
            }
        }
    }
}

private fun createPuzzleCropOptions(
    accentColor: Color,
    textColor: Color,
    context: Context
): UCrop.Options {
    return UCrop.Options().apply {
        setToolbarTitle(context.getString(CoreUiR.string.crop_image))
        setToolbarColor(accentColor.toArgb())
        setStatusBarLight(true)
        setToolbarWidgetColor(textColor.toArgb())
        setActiveControlsWidgetColor(accentColor.toArgb())
        setAllowedGestures(
            UCropActivity.SCALE,
            UCropActivity.ROTATE,
            UCropActivity.ALL
        )
    }
}

private fun loadImageBitmap(context: Context, uri: Uri): ImageBitmap? {
    return try {
        val initial: Bitmap? =
            runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }.getOrNull()

        val bitmap = initial
            ?: context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
            ?: if (uri.scheme == "file") {
                BitmapFactory.decodeFile(uri.path)
            } else {
                null
            }

        bitmap?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

private fun loadDrawableAsBitmap(context: Context, resId: Int, sizePx: Int = 1024): ImageBitmap? {
    return try {
        val drawable = ContextCompat.getDrawable(context, resId) ?: return null
        val bmp = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        bmp.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

fun generatePuzzle(size: Int): List<List<String>> {
    val numbers = (1 until size * size).map { it.toString() }.toMutableList()
    numbers.shuffle()
    numbers.add("")
    return numbers.chunked(size)
}

fun generateIds(size: Int): List<List<Long>> {
    return List(size) { i ->
        List(size) { j ->
            if (i == size - 1 && j == size - 1) 0L else (i * size + j + 1).toLong()
        }
    }
}

fun findEmpty(board: List<List<String>>): Pair<Int, Int> {
    for (i in board.indices) for (j in board[i].indices) if (board[i][j] == "") return i to j
    error("No empty space found")
}

fun isSolved(board: List<List<String>>): Boolean {
    val flat = board.flatten()
    return flat.dropLast(1) == (1 until flat.size).map { it.toString() } && flat.last() == ""
}

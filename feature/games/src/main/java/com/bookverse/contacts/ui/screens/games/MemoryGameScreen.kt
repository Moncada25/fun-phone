package com.bookverse.contacts.ui.screens.games

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.feature.games.R
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameOutcome
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.domain.games.GameStatsSnapshot
import com.bookverse.contacts.settings.AnimationSpeed
import com.bookverse.contacts.ui.components.GameSetupDialog
import com.bookverse.contacts.ui.components.GameSetupOption
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.RetroGameOptionsDialog
import com.bookverse.contacts.ui.components.ScoreCard
import com.bookverse.contacts.ui.components.ScoreCardEntry
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.retroGameDialogBlur
import com.bookverse.contacts.ui.model.CardState
import com.bookverse.contacts.ui.model.MemoryCard
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.BoardSize
import com.bookverse.contacts.ui.viewmodel.CardTheme
import com.bookverse.contacts.ui.viewmodel.MemoryGameViewModel
import com.bookverse.contacts.utils.VibrationUtils
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RotateCcw

private const val MEMORY_MIN_CARD_SIZE_DP = 48
private const val MEMORY_GRID_SPACING_DP = 6
private const val MEMORY_BOARD_PADDING_DP = 12

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MemoryGameScreen(
    onBackClick: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    gameStatsRepository: GameStatsRepository,
) {
    val viewModel: MemoryGameViewModel = viewModel()
    val gameState by viewModel.uiState.collectAsStateWithLifecycle()
    val accentColor = MaterialTheme.colorScheme.primary
    val animationSpeed by appSettingsViewModel.animationSpeed.collectAsStateWithLifecycle()
    val vibrationDuration by appSettingsViewModel.vibrationDuration.collectAsStateWithLifecycle()
    val vibrationPattern by appSettingsViewModel.vibrationPattern.collectAsStateWithLifecycle()
    val stats by gameStatsRepository.stats.collectAsStateWithLifecycle(
        initialValue = GameStatsSnapshot(),
    )
    val context = LocalContext.current
    val gameSession = rememberGameSessionRecorder(GameId.MEMORY, gameStatsRepository)
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior(expandable = false)

    var bestMoves by remember { mutableIntStateOf(0) }
    var showWinDialog by remember { mutableStateOf(false) }
    var showBoardSizeDialog by remember { mutableStateOf(true) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf(CardTheme.Emoji) }
    var selectedBoardSize by remember { mutableStateOf(BoardSize.SMALL) }

    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
    }
    val matchSoundId = remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        val afd = context.assets.openFd("match_sound.ogg")
        matchSoundId.intValue = soundPool.load(afd, 1)
        afd.close()
    }

    DisposableEffect(Unit) {
        onDispose {
            soundPool.release()
        }
    }

    val cards = gameState.cards
    val moves = gameState.moves
    val isGameWon = gameState.isGameWon
    val isBoardLocked = gameState.isBoardLocked
    val lastMatchedName = gameState.lastMatchedName
    val matchedPairs = cards.count { it.state == CardState.MATCHED } / 2
    val totalPairs = cards.size / 2
    val anyDialogVisible = showBoardSizeDialog || showThemeDialog
    val blurRadius = retroGameDialogBlur(anyDialogVisible)
    val selectedThemeLabel = when (selectedTheme) {
        CardTheme.Emoji -> stringResource(R.string.memory_theme_emoji)
        CardTheme.Animals -> stringResource(R.string.memory_theme_animals)
        CardTheme.Food -> stringResource(R.string.memory_theme_food)
        CardTheme.Sports -> stringResource(R.string.memory_theme_sports)
        CardTheme.Nature -> stringResource(R.string.memory_theme_nature)
    }
    val selectedBoardSizeLabel = when (selectedBoardSize) {
        BoardSize.SMALL -> stringResource(R.string.memory_board_size_4x4)
        BoardSize.LARGE -> stringResource(R.string.memory_board_size_6x4)
    }

    fun exitGame() {
        gameSession.abandon()
        onBackClick()
    }

    fun startGame(theme: CardTheme, boardSize: BoardSize) {
        selectedTheme = theme
        selectedBoardSize = boardSize
        viewModel.setCardTheme(theme)
        viewModel.setBoardSize(boardSize)
        viewModel.resetGame()
    }

    fun restartGame() {
        showWinDialog = false
        viewModel.resetGame()
        gameSession.start()
    }

    fun onCardClick(card: MemoryCard) {
        if (isBoardLocked || card.state != CardState.HIDDEN || isGameWon) return
        VibrationUtils.vibrate(context, vibrationDuration, vibrationPattern)
        viewModel.onCardClicked(card)
    }

    LaunchedEffect(lastMatchedName) {
        if (lastMatchedName != null && matchSoundId.intValue != -1) {
            soundPool.play(matchSoundId.intValue, 0.5f, 0.5f, 1, 0, 1f)
        }
    }

    LaunchedEffect(isGameWon) {
        if (isGameWon) {
            showWinDialog = true
            if (moves > 0) {
                val currentBest = bestMoves
                if (currentBest == 0 || moves < currentBest) {
                    bestMoves = moves
                }
                gameSession.complete(
                    GameCompletion(
                        outcome = GameOutcome.WIN,
                        moves = moves,
                    ),
                )
            }
        }
    }

    LaunchedEffect(stats) {
        val storedBest = stats.games[GameId.MEMORY]?.bestMoves ?: 0
        if (storedBest > 0 && (bestMoves == 0 || storedBest < bestMoves)) {
            bestMoves = storedBest
        }
    }

    BackHandler { exitGame() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.memory_game_title),
                onBackClick = ::exitGame,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (!anyDialogVisible && !showWinDialog) {
                        IconAction(
                            imageVector = Lucide.RotateCcw,
                            contentDescription = stringResource(R.string.cd_restart_game),
                            onClick = ::restartGame,
                        )
                    }
                },
            )
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
                    MemoryBoardStage(
                        cards = cards,
                        boardSize = selectedBoardSize,
                        matchedPairs = matchedPairs,
                        totalPairs = totalPairs,
                        isBoardLocked = isBoardLocked,
                        accentColor = accentColor,
                        animationSpeed = animationSpeed,
                        themeLabel = selectedThemeLabel,
                        boardSizeLabel = selectedBoardSizeLabel,
                        wideLayout = true,
                        onCardClick = ::onCardClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    MemorySidePanel(
                        moves = moves,
                        bestMoves = bestMoves,
                        matchedPairs = matchedPairs,
                        totalPairs = totalPairs,
                        accentColor = accentColor,
                        modifier = Modifier
                            .widthIn(min = 240.dp, max = 320.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    )
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
                    MemoryStatsCard(
                        moves = moves,
                        bestMoves = bestMoves,
                        matchedPairs = matchedPairs,
                        totalPairs = totalPairs,
                    )
                    MemoryBoardStage(
                        cards = cards,
                        boardSize = selectedBoardSize,
                        matchedPairs = matchedPairs,
                        totalPairs = totalPairs,
                        isBoardLocked = isBoardLocked,
                        accentColor = accentColor,
                        animationSpeed = animationSpeed,
                        themeLabel = selectedThemeLabel,
                        boardSizeLabel = selectedBoardSizeLabel,
                        wideLayout = false,
                        onCardClick = ::onCardClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
            }
        }
    }

    if (showBoardSizeDialog) {
        GameSetupDialog(
            title = stringResource(R.string.memory_board_size_title),
            instructions = stringResource(R.string.memory_instructions_text),
            options = listOf(
                GameSetupOption(
                    title = stringResource(R.string.memory_board_size_4x4),
                    description = stringResource(R.string.memory_board_size_4x4_description),
                ),
                GameSetupOption(
                    title = stringResource(R.string.memory_board_size_6x4),
                    description = stringResource(R.string.memory_board_size_6x4_description),
                ),
            ),
            selectedIndex = if (selectedBoardSize == BoardSize.LARGE) 1 else 0,
            onOptionSelected = { index ->
                selectedBoardSize = if (index == 1) BoardSize.LARGE else BoardSize.SMALL
            },
            accentColor = accentColor,
            onStart = {
                showBoardSizeDialog = false
                showThemeDialog = true
            },
            onDismiss = ::exitGame,
        )
        return
    }

    if (showThemeDialog) {
        val themeOptions = listOf(
            stringResource(R.string.memory_theme_emoji),
            stringResource(R.string.memory_theme_animals),
            stringResource(R.string.memory_theme_food),
            stringResource(R.string.memory_theme_sports),
            stringResource(R.string.memory_theme_nature),
        )

        RetroGameOptionsDialog(
            title = stringResource(R.string.memory_theme_selection_title),
            options = themeOptions,
            onOptionSelected = { index ->
                val theme = when (index) {
                    1 -> CardTheme.Animals
                    2 -> CardTheme.Food
                    3 -> CardTheme.Sports
                    4 -> CardTheme.Nature
                    else -> CardTheme.Emoji
                }
                showThemeDialog = false
                startGame(theme, selectedBoardSize)
                gameSession.start()
            },
            onDismiss = {
                showThemeDialog = false
                showBoardSizeDialog = true
            },
        )
        return
    }

    if (showWinDialog) {
        MessageDialog(
            title = stringResource(R.string.memory_win_message),
            message1 = stringResource(R.string.moves_count, moves),
            buttonText = stringResource(R.string.play_again),
            onButtonClick = ::restartGame,
            onDismiss = { showWinDialog = false },
        )
    }
}

@Composable
private fun MemorySidePanel(
    moves: Int,
    bestMoves: Int,
    matchedPairs: Int,
    totalPairs: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MemoryStatsCard(
            moves = moves,
            bestMoves = bestMoves,
            matchedPairs = matchedPairs,
            totalPairs = totalPairs,
        )
        MemoryProgressCard(
            matchedPairs = matchedPairs,
            totalPairs = totalPairs,
            accentColor = accentColor,
        )
        MemoryRulesCard()
    }
}

@Composable
private fun MemoryStatsCard(
    moves: Int,
    bestMoves: Int,
    matchedPairs: Int,
    totalPairs: Int,
) {
    ScoreCard(
        entries = listOf(
            ScoreCardEntry(
                title = stringResource(R.string.moves_label),
                value = moves.toString(),
            ),
            ScoreCardEntry(
                title = stringResource(R.string.best_moves_label),
                value = bestMoves.toString(),
            ),
            ScoreCardEntry(
                title = stringResource(R.string.memory_pairs_label),
                value = stringResource(R.string.memory_pairs_value, matchedPairs, totalPairs),
            ),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun MemoryBoardStage(
    cards: List<MemoryCard>,
    boardSize: BoardSize,
    matchedPairs: Int,
    totalPairs: Int,
    isBoardLocked: Boolean,
    accentColor: Color,
    animationSpeed: AnimationSpeed,
    themeLabel: String,
    boardSizeLabel: String,
    wideLayout: Boolean,
    onCardClick: (MemoryCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MemoryStageHeader(
            matchedPairs = matchedPairs,
            totalPairs = totalPairs,
            themeLabel = themeLabel,
            boardSizeLabel = boardSizeLabel,
            accentColor = accentColor,
        )
        MemoryBoard(
            cards = cards,
            boardSize = boardSize,
            matchedPairs = matchedPairs,
            totalPairs = totalPairs,
            isBoardLocked = isBoardLocked,
            accentColor = accentColor,
            animationSpeed = animationSpeed,
            onCardClick = onCardClick,
            modifier = if (wideLayout) {
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            } else {
                Modifier.fillMaxWidth()
            },
        )
    }
}

@Composable
private fun MemoryStageHeader(
    matchedPairs: Int,
    totalPairs: Int,
    themeLabel: String,
    boardSizeLabel: String,
    accentColor: Color,
) {
    val progress = if (totalPairs > 0) {
        (matchedPairs.toFloat() / totalPairs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val progressDescription = stringResource(
        R.string.memory_progress_description,
        matchedPairs,
        totalPairs,
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .semantics {
                    contentDescription = progressDescription
                    liveRegion = LiveRegionMode.Polite
                },
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.memory_stage_kicker),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
                    )
                    Text(
                        text = stringResource(R.string.memory_stage_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                ) {
                    Text(
                        text = stringResource(
                            R.string.memory_pairs_value,
                            matchedPairs,
                            totalPairs,
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MemoryStagePill(
                    label = stringResource(R.string.memory_style_label),
                    value = themeLabel,
                    modifier = Modifier.weight(1f),
                )
                MemoryStagePill(
                    label = stringResource(R.string.memory_board_size_short_label),
                    value = boardSizeLabel,
                    modifier = Modifier.weight(1f),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.memory_progress_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                )
            }
        }
    }
}

@Composable
private fun MemoryStagePill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.11f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MemoryProgressCard(
    matchedPairs: Int,
    totalPairs: Int,
    accentColor: Color,
) {
    val progress = if (totalPairs > 0) {
        (matchedPairs.toFloat() / totalPairs).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = accentColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.memory_progress_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.memory_pairs_value,
                        matchedPairs,
                        totalPairs,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            )
        }
    }
}

@Composable
private fun MemoryRulesCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.memory_rules_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.memory_instructions_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MemoryBoard(
    cards: List<MemoryCard>,
    boardSize: BoardSize,
    matchedPairs: Int,
    totalPairs: Int,
    isBoardLocked: Boolean,
    accentColor: Color,
    animationSpeed: AnimationSpeed,
    onCardClick: (MemoryCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val minBoardWidth = (
        MEMORY_MIN_CARD_SIZE_DP * boardSize.columns +
            MEMORY_GRID_SPACING_DP * (boardSize.columns - 1) +
            MEMORY_BOARD_PADDING_DP * 2
        ).dp
    val boardDescription = stringResource(
        R.string.memory_board_description,
        matchedPairs,
        totalPairs,
    )
    val boardShape = RoundedCornerShape(28.dp)
    val neutralDecorationColor = MaterialTheme.colorScheme.onSurface

    BoxWithConstraints(modifier = modifier) {
        val availableBoardSize = if (maxHeight != Dp.Infinity) {
            minOf(maxWidth, maxHeight)
        } else {
            maxWidth
        }
        val boardWidth = availableBoardSize.coerceAtLeast(minBoardWidth)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .width(boardWidth)
                    .aspectRatio(1f)
                    .semantics {
                        contentDescription = boardDescription
                        liveRegion = LiveRegionMode.Polite
                    },
                shape = boardShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.background,
                                ),
                            ),
                        ),
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridColor = neutralDecorationColor.copy(alpha = 0.055f)
                        val gridStep = size.minDimension / 8f
                        for (index in 1..7) {
                            val position = gridStep * index
                            drawLine(
                                color = gridColor,
                                start = Offset(position, 0f),
                                end = Offset(position, size.height),
                                strokeWidth = 1f,
                            )
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, position),
                                end = Offset(size.width, position),
                                strokeWidth = 1f,
                            )
                        }
                        drawCircle(
                            color = neutralDecorationColor.copy(alpha = 0.045f),
                            radius = size.minDimension * 0.34f,
                            center = Offset(size.width / 2f, size.height / 2f),
                        )
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(boardSize.columns),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        userScrollEnabled = false,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(cards, key = { it.id }) { card ->
                            MemoryCardView(
                                card = card,
                                onClick = { onCardClick(card) },
                                accentColor = accentColor,
                                animationSpeed = animationSpeed,
                                boardSize = boardSize,
                                enabled = !isBoardLocked,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCardView(
    card: MemoryCard,
    onClick: () -> Unit,
    accentColor: Color,
    animationSpeed: AnimationSpeed,
    boardSize: BoardSize,
    enabled: Boolean,
) {
    val isFlipped = card.state != CardState.HIDDEN
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = memoryFloatAnimationSpec(animationSpeed),
        label = "memory_card_flip_${card.id}",
    )
    val scale by animateFloatAsState(
        targetValue = when (card.state) {
            CardState.HIDDEN -> 0.97f
            CardState.VISIBLE -> 1f
            CardState.MATCHED -> 0.94f
        },
        animationSpec = memoryFloatAnimationSpec(animationSpeed),
        label = "memory_card_scale_${card.id}",
    )
    val cardStateLabel = when (card.state) {
        CardState.HIDDEN -> stringResource(R.string.memory_card_state_hidden)
        CardState.VISIBLE -> stringResource(R.string.memory_card_state_revealed)
        CardState.MATCHED -> stringResource(R.string.memory_card_state_matched)
    }
    val cardDescription = when (card.state) {
        CardState.HIDDEN -> stringResource(R.string.memory_card_hidden_description)
        CardState.VISIBLE -> stringResource(R.string.memory_card_revealed_description, card.name)
        CardState.MATCHED -> stringResource(R.string.memory_card_matched_description, card.name)
    }
    val cardShape = RoundedCornerShape(16.dp)
    val borderColor = when (card.state) {
        CardState.HIDDEN -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
        CardState.VISIBLE -> accentColor.copy(alpha = 0.82f)
        CardState.MATCHED -> accentColor
    }
    val cardColor = when (card.state) {
        CardState.HIDDEN -> MaterialTheme.colorScheme.surface
        CardState.VISIBLE -> MaterialTheme.colorScheme.surface
        CardState.MATCHED -> accentColor.copy(alpha = 0.16f)
    }
    val textStyle = when (boardSize) {
        BoardSize.SMALL -> MaterialTheme.typography.displayLarge
        BoardSize.LARGE -> MaterialTheme.typography.headlineLarge
    }
    val backIconSize = when (boardSize) {
        BoardSize.SMALL -> 44.dp
        BoardSize.LARGE -> 28.dp
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale)
            .clickable(
                enabled = enabled && card.state == CardState.HIDDEN,
                role = Role.Button,
                onClickLabel = stringResource(R.string.memory_card_reveal_action),
                onClick = onClick,
            )
            .semantics {
                contentDescription = cardDescription
                stateDescription = cardStateLabel
            },
        shape = cardShape,
        color = cardColor,
        tonalElevation = if (card.state == CardState.MATCHED) 2.dp else 4.dp,
        border = BorderStroke(
            width = if (card.state == CardState.MATCHED) 2.dp else 1.dp,
            color = borderColor,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (rotation <= 90f) {
                MemoryCardBack(
                    rotation = rotation,
                    accentColor = accentColor,
                    iconSize = backIconSize,
                )
            } else {
                MemoryCardFront(
                    name = card.name,
                    rotation = rotation - 180f,
                    textStyle = textStyle,
                    accentColor = accentColor,
                    matched = card.state == CardState.MATCHED,
                )
            }
        }
    }
}

@Composable
private fun MemoryCardBack(
    rotation: Float,
    accentColor: Color,
    iconSize: Dp,
) {
    val colors = MaterialTheme.colorScheme
    val neutralDecorationColor = colors.onSurface
    Box(
        modifier = Modifier
            .fillMaxSize()
            .rotate(rotation)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colors.primaryContainer,
                        colors.surface,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = neutralDecorationColor.copy(alpha = 0.11f)
            val step = size.minDimension / 4f
            for (index in -4..4) {
                val offset = index * step
                drawLine(
                    color = lineColor,
                    start = Offset(offset, 0f),
                    end = Offset(offset + size.height, size.height),
                    strokeWidth = 1f,
                )
                drawLine(
                    color = lineColor,
                    start = Offset(size.width - offset, 0f),
                    end = Offset(size.width - offset - size.height, size.height),
                    strokeWidth = 1f,
                )
            }
        }
        Surface(
            modifier = Modifier.size(iconSize),
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Help,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(iconSize * 0.56f),
                )
            }
        }
    }
}

@Composable
private fun MemoryCardFront(
    name: String,
    rotation: Float,
    textStyle: androidx.compose.ui.text.TextStyle,
    accentColor: Color,
    matched: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .rotate(rotation)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name,
            style = textStyle,
            textAlign = TextAlign.Center,
            color = accentColor,
        )
        if (matched) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp),
                shape = CircleShape,
                color = accentColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = contentColorFor(accentColor),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}

private fun memoryFloatAnimationSpec(animationSpeed: AnimationSpeed): AnimationSpec<Float> =
    if (animationSpeed == AnimationSpeed.OFF) {
        snap()
    } else {
        tween(
            durationMillis = animationSpeed.durationMs,
            easing = FastOutSlowInEasing,
        )
    }

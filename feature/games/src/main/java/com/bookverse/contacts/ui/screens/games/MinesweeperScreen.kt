package com.bookverse.contacts.ui.screens.games

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.bookverse.contacts.feature.games.R
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameOutcome
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.ui.components.RetroGameInstructionsDialog
import com.bookverse.contacts.ui.components.ScoreCard
import com.bookverse.contacts.ui.components.ScoreCardEntry
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.retroGameDialogBlur
import kotlin.random.Random

data class Cell(
    val row: Int,
    val col: Int,
    val isMine: Boolean = false,
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val adjacentMines: Int = 0
)

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MinesweeperScreen(
    onBackClick: () -> Unit,
    gameStatsRepository: GameStatsRepository,
) {
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior(expandable = false)
    val accentColor = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    val gameSession = rememberGameSessionRecorder(GameId.MINESWEEPER, gameStatsRepository)

    val gridSize = 10
    val totalMines = 15

    var board by remember { mutableStateOf(createBoard(gridSize, totalMines)) }
    var gameOver by remember { mutableStateOf(false) }
    var gameWon by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(true) }
    val blurRadius = retroGameDialogBlur(showInstructions)

    val flagsPlaced = board.flatten().count { it.isFlagged }

    LaunchedEffect(gameWon) {
        if (gameWon) {
            gameSession.complete(GameCompletion(outcome = GameOutcome.WIN))
        }
    }

    LaunchedEffect(gameOver) {
        if (gameOver && !gameWon) {
            gameSession.complete(GameCompletion(outcome = GameOutcome.LOSS))
        }
    }

    fun resetGame() {
        board = createBoard(gridSize, totalMines)
        gameOver = false
        gameWon = false
        gameSession.start()
    }

    BackHandler {
        onBackClick()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.minesweeper_title),
                onBackClick = onBackClick,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        val columnScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(columnScrollState)
                .blur(blurRadius),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScoreCard(
                entries = listOf(
                    ScoreCardEntry(
                        title = stringResource(R.string.minesweeper_mines_label),
                        value = stringResource(
                            R.string.minesweeper_mines_value,
                            flagsPlaced,
                            totalMines
                        )
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            )

            @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val gridSpacing = 2.dp
                val boardPadding = 8.dp
                val boardWidth = maxWidth
                val cellSize =
                    (boardWidth - (boardPadding * 2) - gridSpacing * (gridSize - 1)) / gridSize
                val boardHeight =
                    cellSize * gridSize + gridSpacing * (gridSize - 1) + boardPadding * 2

                Surface(
                    modifier = Modifier
                        .width(boardWidth)
                        .height(boardHeight),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridSize),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(boardPadding),
                        userScrollEnabled = false,
                        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                        verticalArrangement = Arrangement.spacedBy(gridSpacing)
                    ) {
                        items(gridSize * gridSize) { index ->
                            val row = index / gridSize
                            val col = index % gridSize
                            val cell = board[row][col]

                            CellItem(
                                cell = cell,
                                accentColor = accentColor,
                                onCellClick = {
                                    if (!gameOver && !gameWon && !cell.isFlagged && !cell.isRevealed) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (cell.isMine) {
                                            gameOver = true
                                            board = revealAllMines(board)
                                        } else {
                                            board = revealCellImmutable(board, row, col, gridSize)
                                            if (checkWin(board, totalMines)) {
                                                gameWon = true
                                            }
                                        }
                                    }
                                },
                                onCellLongClick = {
                                    if (!gameOver && !gameWon && !cell.isRevealed) {
                                        val currentFlags = board.flatten().count { it.isFlagged }
                                        val canAddFlag =
                                            !cell.isFlagged && currentFlags < totalMines
                                        val canRemoveFlag = cell.isFlagged

                                        if (canAddFlag || canRemoveFlag) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            board = board.map { rowList ->
                                                rowList.map { c ->
                                                    if (c.row == row && c.col == col) {
                                                        c.copy(isFlagged = !c.isFlagged)
                                                    } else c
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Crossfade(
                targetState = gameOver || gameWon,
                modifier = Modifier.fillMaxWidth()
            ) { showResult ->
                if (showResult) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = if (gameWon) accentColor.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (gameWon) "🎉" else "💥",
                                fontSize = 48.sp
                            )
                            Text(
                                text = if (gameWon) stringResource(R.string.minesweeper_win_message)
                                else stringResource(R.string.minesweeper_game_over),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (gameWon) accentColor else MaterialTheme.colorScheme.error
                            )
                            FilledTonalButton(
                                onClick = { resetGame() },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = accentColor.copy(alpha = 0.5f),
                                    contentColor = accentColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(CoreUiR.string.reset_label),
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(CoreUiR.string.reset_label),
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            resetGame()
                        },
                        modifier = Modifier.fillMaxWidth(),
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
    }

    if (showInstructions) {
        RetroGameInstructionsDialog(
            title = stringResource(R.string.instructions_title),
            instructions = stringResource(R.string.minesweeper_instructions_text),
            onPlayClick = {
                showInstructions = false
                gameSession.start()
            },
            onDismiss = { showInstructions = false; onBackClick() },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CellItem(
    cell: Cell,
    accentColor: Color,
    onCellClick: () -> Unit,
    onCellLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (cell.isRevealed) 0.92f else 1f,
        label = "cellScale",
        animationSpec = spring()
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val outlineColor = MaterialTheme.colorScheme.outline

    val cellColor = when {
        cell.isRevealed && cell.isMine -> Color(0xFFFF6B6B)
        cell.isRevealed -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        else -> surfaceColor
    }

    val borderColor = when {
        cell.isRevealed -> outlineColor.copy(alpha = 0.3f)
        else -> onBackgroundColor.copy(alpha = 0.15f)
    }

    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCellClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCellLongClick()
                }
            ),
        color = cellColor,
        shape = RoundedCornerShape(6.dp),
        shadowElevation = if (cell.isRevealed) 0.dp else 2.dp,
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedVisibility(
                visible = cell.isFlagged && !cell.isRevealed,
                enter = scaleIn(spring()),
                exit = scaleOut()
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = stringResource(R.string.minesweeper_flagged),
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = cell.isRevealed && cell.isMine,
                enter = scaleIn(spring()),
                exit = scaleOut()
            ) {
                Text(
                    text = "💣",
                    fontSize = 20.sp
                )
            }

            AnimatedVisibility(
                visible = cell.isRevealed && cell.adjacentMines > 0 && !cell.isMine,
                enter = scaleIn(spring()),
                exit = scaleOut()
            ) {
                Text(
                    text = cell.adjacentMines.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = getNumberColor(cell.adjacentMines)
                )
            }
        }
    }
}

fun createBoard(size: Int, mineCount: Int): List<List<Cell>> {
    val cells = mutableListOf<MutableList<Cell>>()

    for (row in 0 until size) {
        val rowList = mutableListOf<Cell>()
        for (col in 0 until size) {
            rowList.add(Cell(row, col))
        }
        cells.add(rowList)
    }

    var minesPlaced = 0
    while (minesPlaced < mineCount) {
        val row = Random.nextInt(size)
        val col = Random.nextInt(size)
        if (!cells[row][col].isMine) {
            cells[row][col] = cells[row][col].copy(isMine = true)
            minesPlaced++
        }
    }

    for (row in 0 until size) {
        for (col in 0 until size) {
            if (!cells[row][col].isMine) {
                val count = countAdjacentMines(cells, row, col, size)
                cells[row][col] = cells[row][col].copy(adjacentMines = count)
            }
        }
    }

    return cells.map { it.toList() }
}

fun countAdjacentMines(board: List<List<Cell>>, row: Int, col: Int, size: Int): Int {
    var count = 0
    for (i in -1..1) {
        for (j in -1..1) {
            if (i == 0 && j == 0) continue
            val newRow = row + i
            val newCol = col + j
            if (newRow in 0 until size && newCol in 0 until size) {
                if (board[newRow][newCol].isMine) count++
            }
        }
    }
    return count
}

fun revealCellImmutable(board: List<List<Cell>>, row: Int, col: Int, size: Int): List<List<Cell>> {
    val newBoard = board.map { it.toMutableList() }.toMutableList()

    fun revealRecursive(r: Int, c: Int) {
        if (r !in 0 until size || c !in 0 until size) return

        val cell = newBoard[r][c]
        if (cell.isRevealed || cell.isFlagged || cell.isMine) return

        newBoard[r][c] = cell.copy(isRevealed = true)

        if (cell.adjacentMines == 0) {
            for (i in -1..1) {
                for (j in -1..1) {
                    if (i == 0 && j == 0) continue
                    revealRecursive(r + i, c + j)
                }
            }
        }
    }

    revealRecursive(row, col)
    return newBoard.map { it.toList() }
}

fun revealAllMines(board: List<List<Cell>>): List<List<Cell>> {
    return board.map { row ->
        row.map { cell ->
            if (cell.isMine) cell.copy(isRevealed = true) else cell
        }
    }
}

fun checkWin(board: List<List<Cell>>, totalMines: Int): Boolean {
    val allCells = board.flatten()
    val revealedSafeCells = allCells.count { !it.isMine && it.isRevealed }
    val totalSafeCells = allCells.size - totalMines
    return revealedSafeCells == totalSafeCells
}

fun getNumberColor(number: Int): Color {
    return when (number) {
        1 -> Color(0xFF1976D2)
        2 -> Color(0xFF388E3C)
        3 -> Color(0xFFD32F2F)
        4 -> Color(0xFF7B1FA2)
        5 -> Color(0xFFFF6F00)
        6 -> Color(0xFF0097A7)
        7 -> Color(0xFF424242)
        else -> Color.Black
    }
}

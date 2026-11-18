package com.bookverse.contacts.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.feature.games.R
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameOutcome
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.ui.components.GameSetupDialog
import com.bookverse.contacts.ui.components.GameSetupOption
import com.bookverse.contacts.ui.components.ScoreCard
import com.bookverse.contacts.ui.components.ScoreCardEntry
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.retroGameDialogBlur
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RotateCcw
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeScreen(
    onBackClick: () -> Unit,
    gameStatsRepository: GameStatsRepository,
) {
    val playerX = "X"
    val playerY = "O"
    val gameSession = rememberGameSessionRecorder(GameId.TIC_TAC_TOE, gameStatsRepository)
    var board by remember { mutableStateOf(List(3) { MutableList(3) { "" } }) }
    var currentPlayer by remember { mutableStateOf(playerX) }
    var piecesOnBoard by remember { mutableStateOf(mapOf(playerX to 0, playerY to 0)) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var winner by remember { mutableStateOf<String?>(null) }
    var winningLine by remember { mutableStateOf<List<Pair<Int, Int>>?>(null) }
    var showSetupDialog by remember { mutableStateOf(true) }
    var setupSelectedIndex by rememberSaveable { mutableIntStateOf(0) }

    val anyDialogVisible = showSetupDialog
    val blurRadius = retroGameDialogBlur(anyDialogVisible)

    var isCpuMode by remember { mutableStateOf(false) }
    var isCpuTurn by remember { mutableStateOf(false) }
    var cpuStartsNext by remember { mutableStateOf(false) }
    var isCpuThinking by remember { mutableStateOf(false) }
    var playerStartsNext by remember { mutableStateOf(true) }

    var scoreX by remember { mutableIntStateOf(0) }
    var scoreO by remember { mutableIntStateOf(0) }
    var showRoundWonMessage by remember { mutableStateOf(false) }
    var roundWinner by remember { mutableStateOf<String?>(null) }
    val accentColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background

    val baseTurnMessage = if (isCpuMode) {
        if (isCpuThinking || isCpuTurn) {
            stringResource(R.string.tictactoe_cpu_thinking)
        } else {
            stringResource(R.string.tictactoe_player_turn)
        }
    } else {
        stringResource(R.string.tic_tac_toe_turn, currentPlayer)
    }
    val roundWinnerName = when {
        roundWinner == null -> null
        isCpuMode -> if (roundWinner == playerX) {
            stringResource(R.string.tictactoe_player)
        } else {
            stringResource(R.string.tictactoe_ai)
        }
        else -> if (roundWinner == playerX) {
            stringResource(R.string.tictactoe_player_x)
        } else {
            stringResource(R.string.tictactoe_player_o)
        }
    }
    val displayTurnMessage = if (showRoundWonMessage && roundWinnerName != null) {
        stringResource(R.string.tictactoe_round_won, roundWinnerName)
    } else {
        baseTurnMessage
    }

    BackHandler {
        onBackClick()
    }

    fun resetGame() {
        board = List(3) { MutableList(3) { "" } }
        piecesOnBoard = mapOf(playerX to 0, playerY to 0)
        selectedCell = null
        winner = null
        winningLine = null
        isCpuThinking = false
        showRoundWonMessage = false
        roundWinner = null

        if (isCpuMode) {
            currentPlayer = playerX
            isCpuTurn = cpuStartsNext
            cpuStartsNext = !cpuStartsNext
        } else {
            currentPlayer = if (playerStartsNext) playerX else playerY
            playerStartsNext = !playerStartsNext
        }
    }

    fun resetFullGame() {
        resetGame()
        scoreX = 0
        scoreO = 0
        winner = null
        gameSession.start()
    }

    fun handleWin(winnerPlayer: String) {
        winner = winnerPlayer
        roundWinner = winnerPlayer

        if (winnerPlayer == playerX) {
            scoreX++
        } else if (winnerPlayer == playerY) {
            scoreO++
        }
    }

    LaunchedEffect(roundWinner) {
        val completedBy = roundWinner
        if (completedBy != null) {
            val outcome = when {
                !isCpuMode -> GameOutcome.NEUTRAL
                completedBy == playerX -> GameOutcome.WIN
                else -> GameOutcome.LOSS
            }
            gameSession.complete(GameCompletion(outcome = outcome))
        }
    }

    fun checkWinner(): Pair<String?, List<Pair<Int, Int>>?> {
        val lines = listOf(
            listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2)),
            listOf(Pair(1, 0), Pair(1, 1), Pair(1, 2)),
            listOf(Pair(2, 0), Pair(2, 1), Pair(2, 2)),
            listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0)),
            listOf(Pair(0, 1), Pair(1, 1), Pair(2, 1)),
            listOf(Pair(0, 2), Pair(1, 2), Pair(2, 2)),
            listOf(Pair(0, 0), Pair(1, 1), Pair(2, 2)),
            listOf(Pair(0, 2), Pair(1, 1), Pair(2, 0))
        )

        for (line in lines) {
            val values = line.map { (row, col) -> board[row][col] }
            if (values.all { it == playerX }) return playerX to line
            if (values.all { it == playerY }) return playerY to line
        }
        return null to null
    }

    fun handleCellClick(row: Int, col: Int) {
        if (winner != null) return
        val cellValue = board[row][col]
        if (piecesOnBoard[currentPlayer]!! < 3 && cellValue == "") {
            board = board.mapIndexed { r, rowList ->
                rowList.mapIndexed { c, v ->
                    if (r == row && c == col) currentPlayer else v
                }.toMutableList()
            }
            piecesOnBoard = piecesOnBoard + (currentPlayer to (piecesOnBoard[currentPlayer]!! + 1))
            val win = checkWinner()
            if (win.first != null) {
                winningLine = win.second
                handleWin(win.first!!)
            } else {
                if (isCpuMode && currentPlayer == playerX) {
                    currentPlayer = playerY
                    isCpuTurn = true
                } else {
                    currentPlayer = if (currentPlayer == playerX) playerY else playerX
                }
            }
            selectedCell = null
        } else if (piecesOnBoard[currentPlayer] == 3) {
            if (selectedCell == null && cellValue == currentPlayer) {
                selectedCell = row to col
            } else if (selectedCell != null && cellValue == "") {
                val (fromRow, fromCol) = selectedCell!!
                board = board.mapIndexed { r, rowList ->
                    rowList.mapIndexed { c, v ->
                        when {
                            r == fromRow && c == fromCol -> ""
                            r == row && c == col -> currentPlayer
                            else -> v
                        }
                    }.toMutableList()
                }
                selectedCell = null
                val win = checkWinner()
                if (win.first != null) {
                    winningLine = win.second
                    handleWin(win.first!!)
                } else {
                    if (isCpuMode && currentPlayer == playerX) {
                        currentPlayer = playerY
                        isCpuTurn = true
                    } else {
                        currentPlayer = if (currentPlayer == playerX) playerY else playerX
                    }
                }
            } else if (selectedCell != null && (row != selectedCell!!.first || col != selectedCell!!.second)) {
                if (cellValue == currentPlayer) {
                    selectedCell = row to col
                }
            } else if (selectedCell != null && cellValue == currentPlayer && row == selectedCell!!.first && col == selectedCell!!.second) {
                selectedCell = null
            }
        }
    }

    fun findEmptyCells(): List<Pair<Int, Int>> {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == "") {
                    emptyCells.add(row to col)
                }
            }
        }
        return emptyCells
    }

    fun findPlayerPieces(player: String): List<Pair<Int, Int>> {
        val pieces = mutableListOf<Pair<Int, Int>>()
        for (row in 0..2) {
            for (col in 0..2) {
                if (board[row][col] == player) {
                    pieces.add(row to col)
                }
            }
        }
        return pieces
    }

    fun wouldWin(player: String, row: Int, col: Int): Boolean {
        val tempBoard = board.map { it.toMutableList() }
        tempBoard[row][col] = player

        val lines = listOf(
            listOf(tempBoard[0][0], tempBoard[0][1], tempBoard[0][2]),
            listOf(tempBoard[1][0], tempBoard[1][1], tempBoard[1][2]),
            listOf(tempBoard[2][0], tempBoard[2][1], tempBoard[2][2]),
            listOf(tempBoard[0][0], tempBoard[1][0], tempBoard[2][0]),
            listOf(tempBoard[0][1], tempBoard[1][1], tempBoard[2][1]),
            listOf(tempBoard[0][2], tempBoard[1][2], tempBoard[2][2]),
            listOf(tempBoard[0][0], tempBoard[1][1], tempBoard[2][2]),
            listOf(tempBoard[0][2], tempBoard[1][1], tempBoard[2][0])
        )

        for (line in lines) {
            if (line.all { it == player }) return true
        }
        return false
    }

    fun findWinningMove(player: String): Pair<Pair<Int, Int>?, Pair<Int, Int>?> {
        val playerPieces = findPlayerPieces(player)
        val emptyCells = findEmptyCells()

        if (piecesOnBoard[player]!! < 3) {
            for (cell in emptyCells) {
                if (wouldWin(player, cell.first, cell.second)) {
                    return null to cell
                }
            }
        } else {
            for (fromCell in playerPieces) {
                for (toCell in emptyCells) {
                    val tempBoard = board.map { it.toMutableList() }
                    tempBoard[fromCell.first][fromCell.second] = ""
                    tempBoard[toCell.first][toCell.second] = player

                    val lines = listOf(
                        listOf(tempBoard[0][0], tempBoard[0][1], tempBoard[0][2]),
                        listOf(tempBoard[1][0], tempBoard[1][1], tempBoard[1][2]),
                        listOf(tempBoard[2][0], tempBoard[2][1], tempBoard[2][2]),
                        listOf(tempBoard[0][0], tempBoard[1][0], tempBoard[2][0]),
                        listOf(tempBoard[0][1], tempBoard[1][1], tempBoard[2][1]),
                        listOf(tempBoard[0][2], tempBoard[1][2], tempBoard[2][2]),
                        listOf(tempBoard[0][0], tempBoard[1][1], tempBoard[2][2]),
                        listOf(tempBoard[0][2], tempBoard[1][1], tempBoard[2][0])
                    )

                    for (line in lines) {
                        if (line.all { it == player }) {
                            return fromCell to toCell
                        }
                    }
                }
            }
        }

        return null to null
    }

    LaunchedEffect(winner) {
        if (winner != null) {
            showRoundWonMessage = true
            delay(1000.milliseconds)
            resetGame()
            gameSession.start()
        }
    }

    LaunchedEffect(isCpuTurn, winner) {
        if (isCpuMode && isCpuTurn && winner == null) {
            isCpuThinking = true
            delay(500.milliseconds)

            if (piecesOnBoard[playerY]!! < 3) {
                val emptyCells = findEmptyCells()

                if (emptyCells.isNotEmpty()) {
                    var moveRow = -1
                    var moveCol = -1

                    for (cell in emptyCells) {
                        if (wouldWin(playerY, cell.first, cell.second)) {
                            moveRow = cell.first
                            moveCol = cell.second
                            break
                        }
                    }

                    if (moveRow == -1) {
                        for (cell in emptyCells) {
                            if (wouldWin(playerX, cell.first, cell.second)) {
                                moveRow = cell.first
                                moveCol = cell.second
                                break
                            }
                        }
                    }

                    if (moveRow == -1 && board[1][1] == "") {
                        moveRow = 1
                        moveCol = 1
                    }

                    if (moveRow == -1) {
                        val corners = listOf(0 to 0, 0 to 2, 2 to 0, 2 to 2)
                        val availableCorners = corners.filter { (r, c) -> board[r][c] == "" }
                        if (availableCorners.isNotEmpty()) {
                            val corner = availableCorners.random()
                            moveRow = corner.first
                            moveCol = corner.second
                        }
                    }

                    if (moveRow == -1) {
                        val randomCell = emptyCells.random()
                        moveRow = randomCell.first
                        moveCol = randomCell.second
                    }

                    board = board.mapIndexed { r, rowList ->
                        rowList.mapIndexed { c, v ->
                            if (r == moveRow && c == moveCol) playerY else v
                        }.toMutableList()
                    }
                    piecesOnBoard = piecesOnBoard + (playerY to (piecesOnBoard[playerY]!! + 1))
                }
            } else {
                val oPieces = findPlayerPieces(playerY)
                val emptyCells = findEmptyCells()

                if (oPieces.isNotEmpty() && emptyCells.isNotEmpty()) {
                    var fromCell: Pair<Int, Int>? = null
                    var toCell: Pair<Int, Int>? = null

                    val winningMove = findWinningMove(playerY)
                    if (winningMove.first != null && winningMove.second != null) {
                        fromCell = winningMove.first
                        toCell = winningMove.second
                    }

                    if (fromCell == null) {
                        val blockingMove = findWinningMove(playerX)
                        if (blockingMove.second != null) {
                            for (piece in oPieces) {
                                var isSafePiece = true
                                for (emptyCell in emptyCells) {
                                    val tempBoard = board.map { it.toMutableList() }
                                    tempBoard[piece.first][piece.second] = ""
                                    if (wouldWin(playerX, emptyCell.first, emptyCell.second)) {
                                        isSafePiece = false
                                        break
                                    }
                                }

                                if (isSafePiece) {
                                    fromCell = piece
                                    toCell = blockingMove.second
                                    break
                                }
                            }

                            if (fromCell == null && oPieces.isNotEmpty()) {
                                fromCell = oPieces.random()
                                toCell = blockingMove.second
                            }
                        }
                    }

                    if (fromCell == null) {
                        for (from in oPieces) {
                            for (to in emptyCells) {
                                val tempBoard = board.map { it.toMutableList() }
                                tempBoard[from.first][from.second] = ""
                                tempBoard[to.first][to.second] = playerY

                                var potentialWin = false
                                for (r in 0..2) {
                                    for (c in 0..2) {
                                        if (tempBoard[r][c] == "") {
                                            tempBoard[r][c] = playerY
                                            val lines = listOf(
                                                listOf(
                                                    tempBoard[0][0],
                                                    tempBoard[0][1],
                                                    tempBoard[0][2]
                                                ),
                                                listOf(
                                                    tempBoard[1][0],
                                                    tempBoard[1][1],
                                                    tempBoard[1][2]
                                                ),
                                                listOf(
                                                    tempBoard[2][0],
                                                    tempBoard[2][1],
                                                    tempBoard[2][2]
                                                ),
                                                listOf(
                                                    tempBoard[0][0],
                                                    tempBoard[1][0],
                                                    tempBoard[2][0]
                                                ),
                                                listOf(
                                                    tempBoard[0][1],
                                                    tempBoard[1][1],
                                                    tempBoard[2][1]
                                                ),
                                                listOf(
                                                    tempBoard[0][2],
                                                    tempBoard[1][2],
                                                    tempBoard[2][2]
                                                ),
                                                listOf(
                                                    tempBoard[0][0],
                                                    tempBoard[1][1],
                                                    tempBoard[2][2]
                                                ),
                                                listOf(
                                                    tempBoard[0][2],
                                                    tempBoard[1][1],
                                                    tempBoard[2][0]
                                                )
                                            )

                                            for (line in lines) {
                                                if (line.all { it == playerY }) {
                                                    potentialWin = true
                                                    break
                                                }
                                            }

                                            tempBoard[r][c] = ""
                                            if (potentialWin) break
                                        }
                                    }
                                    if (potentialWin) break
                                }

                                if (potentialWin) {
                                    fromCell = from
                                    toCell = to
                                    break
                                }
                            }
                            if (fromCell != null) break
                        }
                    }

                    if (fromCell == null) {
                        fromCell = oPieces.random()
                        toCell = emptyCells.random()
                    }

                    board = board.mapIndexed { r, rowList ->
                        rowList.mapIndexed { c, v ->
                            when {
                                r == fromCell.first && c == fromCell.second -> ""
                                r == toCell!!.first && c == toCell.second -> playerY
                                else -> v
                            }
                        }.toMutableList()
                    }
                }
            }

            val win = checkWinner()
            if (win.first != null) {
                winningLine = win.second
                handleWin(win.first!!)
            } else {
                currentPlayer = playerX
                isCpuTurn = false
            }

            isCpuThinking = false
        }
    }

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.tic_tac_toe_title),
                onBackClick = onBackClick,
                actions = {
                    IconAction(
                        imageVector = Lucide.RotateCcw,
                        contentDescription = stringResource(R.string.cd_restart_game),
                        onClick = { resetFullGame() }
                    )
                }
            )
        }
    ) { padding ->
        val player1Name = if (isCpuMode) {
            stringResource(R.string.tictactoe_player)
        } else {
            stringResource(R.string.tictactoe_player_x)
        }
        val player2Name = if (isCpuMode) {
            stringResource(R.string.tictactoe_ai)
        } else {
            stringResource(R.string.tictactoe_player_o)
        }
        val modeLabel = if (isCpuMode) {
            stringResource(R.string.tictactoe_mode_vs_cpu)
        } else {
            stringResource(R.string.tictactoe_mode_two_players)
        }
        val scoreEntries = listOf(
            ScoreCardEntry(
                title = stringResource(R.string.tictactoe_score_title, player1Name),
                value = scoreX.toString(),
            ),
            ScoreCardEntry(
                title = stringResource(R.string.tictactoe_score_title, player2Name),
                value = scoreO.toString(),
            ),
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
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
                    TicTacToeBoard(
                        board = board,
                        selectedCell = selectedCell,
                        winningLine = winningLine,
                        onCellClick = ::handleCellClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .aspectRatio(1f),
                    )
                    Column(
                        modifier = Modifier.widthIn(min = 240.dp, max = 320.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ScoreCard(
                            entries = scoreEntries,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TicTacToeTurnPill(
                            message = displayTurnMessage,
                            modeLabel = modeLabel,
                        )
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
                    ScoreCard(
                        entries = scoreEntries,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                            },
                    )
                    TicTacToeTurnPill(
                        message = displayTurnMessage,
                        modeLabel = modeLabel,
                    )
                    TicTacToeBoard(
                        board = board,
                        selectedCell = selectedCell,
                        winningLine = winningLine,
                        onCellClick = ::handleCellClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                    )
                    Spacer(modifier = Modifier.weight(1f, fill = false))
                }
            }
        }
    }

    if (showSetupDialog) {
        GameSetupDialog(
            gameTitle = stringResource(R.string.tic_tac_toe_title),
            title = stringResource(R.string.snake_setup_title),
            instructions = stringResource(R.string.tictactoe_instructions_text),
            options = listOf(
                GameSetupOption(
                    title = stringResource(R.string.tictactoe_mode_two_players),
                    description = stringResource(R.string.tictactoe_mode_two_players_description),
                ),
                GameSetupOption(
                    title = stringResource(R.string.tictactoe_mode_vs_cpu),
                    description = stringResource(R.string.tictactoe_mode_vs_cpu_description),
                ),
            ),
            selectedIndex = setupSelectedIndex,
            onOptionSelected = { setupSelectedIndex = it },
            accentColor = accentColor,
            onStart = {
                isCpuMode = setupSelectedIndex == 1
                if (isCpuMode) {
                    cpuStartsNext = false
                    isCpuTurn = false
                }
                showSetupDialog = false
                resetGame()
                gameSession.start()
            },
            onDismiss = {
                showSetupDialog = false
                onBackClick()
            },
        )
    }
}

@Composable
private fun TicTacToeTurnPill(
    message: String,
    modeLabel: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = message
        },
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        )
                        .padding(4.dp),
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            text = modeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TicTacToeBoard(
    board: List<List<String>>,
    selectedCell: Pair<Int, Int>?,
    winningLine: List<Pair<Int, Int>>?,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val boardShape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier,
        shape = boardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (row in 0..2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (col in 0..2) {
                        TicTacToeCell(
                            value = board[row][col],
                            isSelected = selectedCell != null && selectedCell == (row to col),
                            isWinning = winningLine?.contains(row to col) == true,
                            onClick = { onCellClick(row, col) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TicTacToeCell(
    value: String,
    isSelected: Boolean,
    isWinning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasContent = value.isNotEmpty()
    val scale by animateFloatAsState(
        targetValue = if (hasContent) 1f else 0.94f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 800f,
        ),
        label = "cellScale",
    )

    val elevation by animateFloatAsState(
        targetValue = when {
            isWinning -> 0f
            hasContent -> 6f
            else -> 2f
        },
        animationSpec = tween(300),
        label = "cellElevation",
    )

    val containerColor = when {
        isWinning -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        isWinning -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (isWinning || isSelected) 2.dp else 1.dp
    val pieceColor = when {
        isWinning -> MaterialTheme.colorScheme.onPrimary
        value == "X" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .padding(2.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = pieceColor,
            ),
            border = BorderStroke(borderWidth, borderColor),
            elevation = CardDefaults.cardElevation(
                defaultElevation = elevation.dp,
            ),
            onClick = onClick,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (hasContent) {
                    this@Card.AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(
                            animationSpec = spring(
                                dampingRatio = 0.7f,
                                stiffness = 1000f,
                            ),
                        ) + fadeIn(animationSpec = tween(200)),
                        exit = scaleOut(
                            animationSpec = spring(
                                dampingRatio = 0.7f,
                                stiffness = 1000f,
                            ),
                        ) + fadeOut(animationSpec = tween(200)),
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = pieceColor,
                        )
                    }
                }
            }
        }
    }
}

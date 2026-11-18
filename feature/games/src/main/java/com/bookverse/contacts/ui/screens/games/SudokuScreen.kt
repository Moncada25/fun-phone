package com.bookverse.contacts.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameOutcome
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.GameSetupDialog
import com.bookverse.contacts.ui.components.GameSetupOption
import com.bookverse.contacts.ui.components.ScoreCard
import com.bookverse.contacts.ui.components.ScoreCardEntry
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.retroGameDialogBlur
import com.bookverse.contacts.ui.viewmodel.SudokuViewModel
import com.bookverse.contacts.ui.viewmodel.SudokuViewModel.SudokuCell
import com.bookverse.contacts.ui.viewmodel.SudokuViewModel.SudokuDifficulty
import com.bookverse.contacts.ui.viewmodel.SudokuViewModel.SudokuUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuScreen(
    onBackClick: () -> Unit,
    gameStatsRepository: GameStatsRepository,
) {
    val viewModel: SudokuViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gameSession = rememberGameSessionRecorder(GameId.SUDOKU, gameStatsRepository)
    val accentColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val contentScrollState = rememberScrollState()

    var showDifficultyDialog by remember { mutableStateOf(true) }
    var setupSelectedIndex by remember { mutableIntStateOf(1) }

    val blurRadius = retroGameDialogBlur(showDifficultyDialog)

    val difficultyOptions = listOf(
        GameSetupOption(
            title = stringResource(R.string.sudoku_difficulty_easy),
            description = stringResource(R.string.sudoku_difficulty_easy_description),
        ),
        GameSetupOption(
            title = stringResource(R.string.sudoku_difficulty_medium),
            description = stringResource(R.string.sudoku_difficulty_medium_description),
        ),
        GameSetupOption(
            title = stringResource(R.string.sudoku_difficulty_hard),
            description = stringResource(R.string.sudoku_difficulty_hard_description),
        ),
    )
    val difficultyValues = listOf(
        SudokuDifficulty.EASY,
        SudokuDifficulty.MEDIUM,
        SudokuDifficulty.HARD
    )

    LaunchedEffect(uiState.showVictoryDialog) {
        if (uiState.showVictoryDialog) {
            gameSession.complete(
                GameCompletion(
                    outcome = GameOutcome.WIN,
                    durationSeconds = uiState.elapsedSeconds,
                )
            )
        }
    }

    BackHandler {
        onBackClick()
    }

    val formattedTime =
        remember(uiState.elapsedSeconds) { formatElapsedTime(uiState.elapsedSeconds) }
    val difficultyLabel = when (uiState.difficulty) {
        SudokuDifficulty.EASY -> stringResource(R.string.sudoku_difficulty_easy)
        SudokuDifficulty.MEDIUM -> stringResource(R.string.sudoku_difficulty_medium)
        SudokuDifficulty.HARD -> stringResource(R.string.sudoku_difficulty_hard)
    }

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.sudoku_title),
                onBackClick = onBackClick,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(contentScrollState)
                .blur(blurRadius),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScoreCard(
                entries = listOf(
                    ScoreCardEntry(
                        title = stringResource(R.string.time_label),
                        value = formattedTime
                    ),
                    ScoreCardEntry(
                        title = stringResource(R.string.sudoku_difficulty_label),
                        value = difficultyLabel
                    ),
                    ScoreCardEntry(
                        title = stringResource(R.string.sudoku_errors_label),
                        value = uiState.mistakes.toString()
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            SudokuBoard(
                uiState = uiState,
                accentColor = accentColor,
                textColor = textColor,
                onCellSelected = { row, col -> viewModel.selectCell(row, col) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            NumberPad(
                accentColor = accentColor,
                onNumberClick = { viewModel.inputNumber(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        viewModel.clearCell()
                        viewModel.clearSelection()
                    },
                    modifier = Modifier
                        .weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = accentColor.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    Text(
                        text = stringResource(R.string.sudoku_clear_cell),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                OutlinedButton(
                    onClick = {
                        viewModel.startNewGame(uiState.difficulty)
                        gameSession.start()
                    },
                    modifier = Modifier
                        .weight(1f),
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

    if (showDifficultyDialog) {
        GameSetupDialog(
            title = stringResource(R.string.sudoku_setup_title),
            instructions = stringResource(R.string.sudoku_instructions_text),
            options = difficultyOptions,
            selectedIndex = setupSelectedIndex,
            onOptionSelected = { setupSelectedIndex = it },
            accentColor = accentColor,
            onStart = {
                val difficulty = difficultyValues[setupSelectedIndex]
                showDifficultyDialog = false
                viewModel.startNewGame(difficulty)
                gameSession.start()
            },
            onDismiss = {
                showDifficultyDialog = false
                onBackClick()
            },
        )
    }

    if (uiState.showVictoryDialog) {
        MessageDialog(
            title = stringResource(R.string.sudoku_victory_title),
            message1 = pluralStringResource(
                R.plurals.sudoku_victory_message,
                uiState.mistakes,
                formattedTime,
                uiState.mistakes,
            ),
            buttonText = stringResource(R.string.play_again),
            onButtonClick = {
                viewModel.dismissVictoryDialog()
                viewModel.startNewGame(uiState.difficulty)
                gameSession.start()
            },
            onDismiss = { viewModel.dismissVictoryDialog() },
        )
    }
}

@Composable
private fun SudokuBoard(
    uiState: SudokuUiState,
    accentColor: Color,
    textColor: Color,
    onCellSelected: (Int, Int) -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            uiState.cells.chunked(9).forEach { rowCells ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    rowCells.forEach { cell ->
                        val isSelected = uiState.selectedCell == cell.row to cell.col
                        val isPeer = uiState.selectedCell?.let { selected ->
                            selected.first == cell.row ||
                                    selected.second == cell.col ||
                                    (selected.first / 3 == cell.row / 3 && selected.second / 3 == cell.col / 3)
                        } == true
                        val isInvalid = uiState.invalidCells.contains(cell.row to cell.col)
                        SudokuCellView(
                            cell = cell,
                            isSelected = isSelected,
                            isPeer = isPeer,
                            isInvalid = isInvalid,
                            accentColor = accentColor,
                            textColor = textColor,
                            outlineColor = outlineColor,
                            onClick = { onCellSelected(cell.row, cell.col) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.SudokuCellView(
    cell: SudokuCell,
    isSelected: Boolean,
    isPeer: Boolean,
    isInvalid: Boolean,
    accentColor: Color,
    textColor: Color,
    outlineColor: Color,
    onClick: () -> Unit
) {
    val baseColor = if (cell.isGiven) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.background
    }

    val backgroundColor = when {
        isInvalid -> MaterialTheme.colorScheme.errorContainer
        isSelected -> accentColor.copy(alpha = 0.35f)
        isPeer -> accentColor.copy(alpha = 0.15f)
        else -> baseColor
    }

    val textSize = if (cell.isGiven) 20.sp else 18.sp
    val fontWeight = if (cell.isGiven) FontWeight.Bold else FontWeight.Medium

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .aspectRatio(1f)
            .drawBehind {
                val thin = 1.dp.toPx()
                val thick = 3.dp.toPx()
                // Draw thicker lines for sub-grid separators and thin lines for regular cells.
                val topStroke = if (cell.row % 3 == 0) thick else thin
                drawLine(
                    color = outlineColor,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = topStroke
                )
                val leftStroke = if (cell.col % 3 == 0) thick else thin
                drawLine(
                    color = outlineColor,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(0f, size.height),
                    strokeWidth = leftStroke
                )
                if (cell.row == 8) {
                    drawLine(
                        color = outlineColor,
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = thick
                    )
                }
                if (cell.col == 8) {
                    drawLine(
                        color = outlineColor,
                        start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = thick
                    )
                }
            }
            .background(
                color = backgroundColor
            )
            .clickable(enabled = true) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cell.value?.toString() ?: "",
            color = if (isInvalid) MaterialTheme.colorScheme.error else textColor,
            fontSize = textSize,
            fontWeight = fontWeight
        )
    }
}

@Composable
private fun NumberPad(
    accentColor: Color,
    onNumberClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        ).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { number ->
                    NumberKeyButton(
                        label = number.toString(),
                        accentColor = accentColor,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp),
                        onClick = { onNumberClick(number) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun NumberKeyButton(
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border = BorderStroke(2.dp, accentColor),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = accentColor
        )
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatElapsedTime(seconds: Int): String {
    val minutesPart = seconds / 60
    val secondsPart = seconds % 60
    return String.format(Locale.ROOT, "%02d:%02d", minutesPart, secondsPart)
}

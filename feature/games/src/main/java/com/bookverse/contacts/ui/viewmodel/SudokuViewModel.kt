package com.bookverse.contacts.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class SudokuViewModel : ViewModel() {

    private val puzzlesByDifficulty = mapOf(
        SudokuDifficulty.EASY to listOf(
            SudokuPuzzle(
                puzzle = "504008900670100300008342507050761400426853091013920856000037284007010635300086170",
                solution = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
            ),
            SudokuPuzzle(
                puzzle = "600789020080016000219403608901072530507060012824030067072640090390021046056307201",
                solution = "645789123783216459219453678961872534537964812824135967172648395398521746456397281"
            ),
            SudokuPuzzle(
                puzzle = "009040805005001796386090100618023950050010483407850602800170569064038270700562008",
                solution = "179246835245381796386795124618423957952617483437859612823174569564938271791562348"
            )
        ),
        SudokuDifficulty.MEDIUM to listOf(
            SudokuPuzzle(
                puzzle = "004078002002105040100002560859700403400050790003020056000537204080010035305000000",
                solution = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
            ),
            SudokuPuzzle(
                puzzle = "045000023080206059010000070900870004530900002824100960000648000000500046006390281",
                solution = "645789123783216459219453678961872534537964812824135967172648395398521746456397281"
            ),
            SudokuPuzzle(
                puzzle = "070000835245301090086790100000020007050617400400009000000104500564908200790060300",
                solution = "179246835245381796386795124618423957952617483437859612823174569564938271791562348"
            )
        ),
        SudokuDifficulty.HARD to listOf(
            SudokuPuzzle(
                puzzle = "530078012000000000000000500850001023006053000000024050001500080080000030045206000",
                solution = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
            ),
            SudokuPuzzle(
                puzzle = "640000100003010009219000678960000000037000010000100000000008300000001700056007081",
                solution = "645789123783216459219453678961872534537964812824135967172648395398521746456397281"
            ),
            SudokuPuzzle(
                puzzle = "000040030205081000000090000000000900002010080007009000003174069564000001790002300",
                solution = "179246835245381796386795124618423957952617483437859612823174569564938271791562348"
            )
        )
    )

    private val _uiState = MutableStateFlow(SudokuUiState())
    val uiState: StateFlow<SudokuUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        startNewGame(SudokuDifficulty.MEDIUM)
    }

    fun startNewGame(difficulty: SudokuDifficulty = _uiState.value.difficulty) {
        val puzzles = puzzlesByDifficulty[difficulty].orEmpty()
        if (puzzles.isEmpty()) return

        val puzzle = puzzles.random(Random(System.currentTimeMillis()))
        val cells = puzzle.puzzle.mapIndexed { index, char ->
            val solutionValue = puzzle.solution[index].digitToInt()
            val value = char.takeIf { it != '0' }?.digitToInt()
            val row = index / 9
            val col = index % 9
            SudokuCell(
                row = row,
                col = col,
                value = value,
                solution = solutionValue,
                isGiven = value != null
            )
        }
        _uiState.value = SudokuUiState(
            cells = cells,
            selectedCell = null,
            invalidCells = computeConflicts(cells),
            mistakes = 0,
            isComplete = false,
            difficulty = difficulty,
            elapsedSeconds = 0,
            showVictoryDialog = false
        )
        restartTimer()
    }

    fun selectCell(row: Int, col: Int) {
        val current = _uiState.value
        if (row !in 0..8 || col !in 0..8) return
        if (current.selectedCell == row to col) return
        _uiState.update { it.copy(selectedCell = row to col) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedCell = null) }
    }

    fun inputNumber(number: Int) {
        if (number !in 1..9) return
        val current = _uiState.value
        val position = current.selectedCell ?: return
        val index = position.first * 9 + position.second
        val cell = current.cells.getOrNull(index) ?: return
        if (cell.isGiven || current.isComplete) return

        val newCells = current.cells.toMutableList()
        newCells[index] = cell.copy(value = number)

        val invalidCells = computeConflicts(newCells)
        val isCorrectValue = number == cell.solution
        val newMistakes = if (isCorrectValue) current.mistakes else current.mistakes + 1

        val isComplete = invalidCells.isEmpty() && newCells.all { it.value != null }
        if (isComplete) stopTimer()

        _uiState.update {
            it.copy(
                cells = newCells,
                invalidCells = invalidCells,
                mistakes = newMistakes,
                isComplete = isComplete,
                showVictoryDialog = isComplete
            )
        }
    }

    fun clearCell() {
        val current = _uiState.value
        val position = current.selectedCell ?: return
        val index = position.first * 9 + position.second
        val cell = current.cells.getOrNull(index) ?: return
        if (cell.isGiven || current.isComplete) return

        val newCells = current.cells.toMutableList()
        newCells[index] = cell.copy(value = null)
        val invalidCells = computeConflicts(newCells)

        _uiState.update {
            it.copy(
                cells = newCells,
                invalidCells = invalidCells
            )
        }
    }

    fun dismissVictoryDialog() {
        _uiState.update { it.copy(showVictoryDialog = false) }
    }

    private fun computeConflicts(cells: List<SudokuCell>): Set<Pair<Int, Int>> {
        val invalid = mutableSetOf<Pair<Int, Int>>()

        cells.forEach { cell ->
            val value = cell.value ?: return@forEach
            if (value != cell.solution) {
                invalid += cell.row to cell.col
            }
        }

        for (row in 0 until 9) {
            val rowCells = cells.filter { it.row == row && it.value != null }
            val duplicates = rowCells.groupBy { it.value }.filter { it.value.size > 1 }
            duplicates.values.flatten().forEach { invalid += it.row to it.col }
        }

        for (col in 0 until 9) {
            val colCells = cells.filter { it.col == col && it.value != null }
            val duplicates = colCells.groupBy { it.value }.filter { it.value.size > 1 }
            duplicates.values.flatten().forEach { invalid += it.row to it.col }
        }

        for (boxRow in 0 until 3) {
            for (boxCol in 0 until 3) {
                val subCells = cells.filter {
                    it.row / 3 == boxRow && it.col / 3 == boxCol && it.value != null
                }
                val duplicates = subCells.groupBy { it.value }.filter { it.value.size > 1 }
                duplicates.values.flatten().forEach { invalid += it.row to it.col }
            }
        }

        return invalid
    }

    private fun restartTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _uiState.update { state ->
                    if (state.isComplete) state else state.copy(elapsedSeconds = state.elapsedSeconds + 1)
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    data class SudokuCell(
        val row: Int,
        val col: Int,
        val value: Int?,
        val solution: Int,
        val isGiven: Boolean
    )

    data class SudokuUiState(
        val cells: List<SudokuCell> = emptyList(),
        val selectedCell: Pair<Int, Int>? = null,
        val invalidCells: Set<Pair<Int, Int>> = emptySet(),
        val mistakes: Int = 0,
        val isComplete: Boolean = false,
        val difficulty: SudokuDifficulty = SudokuDifficulty.MEDIUM,
        val elapsedSeconds: Int = 0,
        val showVictoryDialog: Boolean = false
    )

    data class SudokuPuzzle(
        val puzzle: String,
        val solution: String
    )

    enum class SudokuDifficulty {
        EASY,
        MEDIUM,
        HARD
    }
}

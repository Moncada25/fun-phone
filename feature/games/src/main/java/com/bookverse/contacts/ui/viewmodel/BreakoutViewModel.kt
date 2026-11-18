package com.bookverse.contacts.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.domain.games.BreakoutGameEngine
import com.bookverse.contacts.domain.games.BreakoutGameState
import com.bookverse.contacts.domain.games.BreakoutPhase
import com.bookverse.contacts.domain.games.BreakoutPaddleInput
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameStatsRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface BreakoutEvent {
    data object Start : BreakoutEvent
    data object TogglePause : BreakoutEvent
    data object Restart : BreakoutEvent
    data class SetPaddleInput(val input: BreakoutPaddleInput) : BreakoutEvent
    data object ReleasePaddle : BreakoutEvent
    data object ScreenExit : BreakoutEvent
}

class BreakoutViewModel(
    private val statsRepository: GameStatsRepository,
    private val engine: BreakoutGameEngine = BreakoutGameEngine(),
    private val tickDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sessionIdFactory: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(engine.ready())
    val uiState: StateFlow<BreakoutGameState> = _uiState.asStateFlow()

    private var loopJob: Job? = null
    private var activeSessionId: String? = null

    init {
        viewModelScope.launch {
            statsRepository.stats.collect { snapshot ->
                val storedBest = snapshot.games[GameId.BREAKOUT]?.bestScore ?: 0
                _uiState.update { state ->
                    if (storedBest > state.highScore) state.copy(highScore = storedBest) else state
                }
            }
        }
    }

    fun onEvent(event: BreakoutEvent) {
        when (event) {
            BreakoutEvent.Start -> startGame()
            BreakoutEvent.TogglePause -> togglePause()
            BreakoutEvent.Restart -> startGame()
            is BreakoutEvent.SetPaddleInput -> {
                _uiState.update { state -> engine.setPaddleInput(state, event.input) }
            }

            BreakoutEvent.ReleasePaddle -> {
                _uiState.update { state ->
                    engine.setPaddleInput(state, BreakoutPaddleInput.NONE)
                }
            }

            BreakoutEvent.ScreenExit -> exitScreen()
        }
    }

    private fun startGame() {
        stopLoop()
        val sessionId = sessionIdFactory()
        activeSessionId = sessionId
        _uiState.value = engine.start(_uiState.value.highScore)
        viewModelScope.launch {
            statsRepository.startSession(GameId.BREAKOUT, sessionId, clock())
        }
        startLoop(sessionId)
    }

    private fun togglePause() {
        val updated = engine.togglePause(_uiState.value)
        _uiState.value = updated
        if (updated.phase == BreakoutPhase.RUNNING) {
            activeSessionId?.let(::startLoop)
        } else {
            stopLoop()
        }
    }

    private fun startLoop(sessionId: String) {
        stopLoop()
        loopJob = viewModelScope.launch(tickDispatcher) {
            while (isActive && _uiState.value.phase == BreakoutPhase.RUNNING) {
                delay(FRAME_DELAY_MS)
                val updated = engine.tick(_uiState.value, FRAME_DELTA_SECONDS)
                _uiState.value = updated
                if (updated.phase == BreakoutPhase.GAME_OVER) {
                    completeSession(sessionId, updated.score)
                }
            }
        }
    }

    private fun completeSession(sessionId: String, score: Int) {
        if (activeSessionId != sessionId) return
        activeSessionId = null
        viewModelScope.launch {
            statsRepository.completeSession(
                gameId = GameId.BREAKOUT,
                sessionId = sessionId,
                completion = GameCompletion(score = score),
            )
        }
    }

    private fun exitScreen() {
        stopLoop()
        activeSessionId = null
        _uiState.value = engine.ready(_uiState.value.highScore)
    }

    private fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
    }

    override fun onCleared() {
        stopLoop()
        super.onCleared()
    }

    private companion object {
        const val FRAME_DELAY_MS = 16L
        const val FRAME_DELTA_SECONDS = FRAME_DELAY_MS / 1000f
    }
}

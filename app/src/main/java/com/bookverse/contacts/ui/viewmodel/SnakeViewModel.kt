package com.bookverse.contacts.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.domain.games.SnakeBoundaryMode
import com.bookverse.contacts.domain.games.SnakeDirection
import com.bookverse.contacts.domain.games.SnakeGameEngine
import com.bookverse.contacts.domain.games.SnakeGameState
import com.bookverse.contacts.domain.games.SnakePhase
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

sealed interface SnakeEvent {
    data class Start(val boundaryMode: SnakeBoundaryMode) : SnakeEvent
    data class ChangeDirection(val direction: SnakeDirection) : SnakeEvent
    data object TogglePause : SnakeEvent
    data object Restart : SnakeEvent
    data object ChangeMode : SnakeEvent
    data object ScreenExit : SnakeEvent
}

class SnakeViewModel(
    private val statsRepository: GameStatsRepository,
    private val engine: SnakeGameEngine = SnakeGameEngine(),
    private val tickDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sessionIdFactory: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(engine.ready())
    val uiState: StateFlow<SnakeGameState> = _uiState.asStateFlow()

    private var loopJob: Job? = null
    private var activeSessionId: String? = null

    init {
        viewModelScope.launch {
            statsRepository.stats.collect { snapshot ->
                val storedBest = snapshot.games[GameId.SNAKE]?.bestScore ?: 0
                _uiState.update { state ->
                    if (storedBest > state.highScore) state.copy(highScore = storedBest) else state
                }
            }
        }
    }

    fun onEvent(event: SnakeEvent) {
        when (event) {
            is SnakeEvent.Start -> startGame(event.boundaryMode)
            is SnakeEvent.ChangeDirection -> {
                _uiState.update { state -> engine.queueDirection(state, event.direction) }
            }
            SnakeEvent.TogglePause -> togglePause()
            SnakeEvent.Restart -> startGame(_uiState.value.boundaryMode)
            SnakeEvent.ChangeMode -> returnToSetup()
            SnakeEvent.ScreenExit -> exitScreen()
        }
    }

    private fun startGame(boundaryMode: SnakeBoundaryMode) {
        stopLoop()
        val sessionId = sessionIdFactory()
        activeSessionId = sessionId
        _uiState.value = engine.start(boundaryMode, _uiState.value.highScore)
        viewModelScope.launch {
            statsRepository.startSession(GameId.SNAKE, sessionId, clock())
        }
        startLoop(sessionId)
    }

    private fun togglePause() {
        val updated = engine.togglePause(_uiState.value)
        _uiState.value = updated
        if (updated.phase == SnakePhase.RUNNING) {
            activeSessionId?.let(::startLoop)
        } else {
            stopLoop()
        }
    }

    private fun startLoop(sessionId: String) {
        stopLoop()
        loopJob = viewModelScope.launch(tickDispatcher) {
            while (isActive && _uiState.value.phase == SnakePhase.RUNNING) {
                delay(_uiState.value.speedMs.toLong())
                val previousPhase = _uiState.value.phase
                val updated = engine.tick(_uiState.value)
                _uiState.value = updated
                if (
                    previousPhase == SnakePhase.RUNNING &&
                    updated.phase in setOf(SnakePhase.GAME_OVER, SnakePhase.WON)
                ) {
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
                gameId = GameId.SNAKE,
                sessionId = sessionId,
                completion = GameCompletion(score = score),
            )
        }
    }

    private fun returnToSetup() {
        stopLoop()
        activeSessionId = null
        _uiState.value = engine.ready(
            boundaryMode = _uiState.value.boundaryMode,
            highScore = _uiState.value.highScore,
        )
    }

    private fun exitScreen() {
        returnToSetup()
    }

    private fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
    }

    override fun onCleared() {
        stopLoop()
        super.onCleared()
    }
}

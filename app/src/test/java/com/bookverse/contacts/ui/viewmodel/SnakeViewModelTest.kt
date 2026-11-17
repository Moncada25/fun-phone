package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.domain.games.GameStatsSnapshot
import com.bookverse.contacts.domain.games.SnakeBoundaryMode
import com.bookverse.contacts.domain.games.SnakeFoodPicker
import com.bookverse.contacts.domain.games.SnakeGameEngine
import com.bookverse.contacts.domain.games.SnakePhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SnakeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun completedBoardRecordsSingleSessionAndScore() = runTest(dispatcher) {
        val repository = FakeGameStatsRepository()
        val engine = SnakeGameEngine(
            width = 4,
            height = 1,
            foodPicker = SnakeFoodPicker { it.single() },
        )
        val viewModel = SnakeViewModel(
            statsRepository = repository,
            engine = engine,
            tickDispatcher = dispatcher,
            clock = { 42L },
            sessionIdFactory = { "snake-session" },
        )

        viewModel.onEvent(SnakeEvent.Start(SnakeBoundaryMode.WALLS))
        advanceTimeBy(151)
        advanceUntilIdle()

        assertEquals(SnakePhase.WON, viewModel.uiState.value.phase)
        assertEquals(listOf("snake-session"), repository.startedSessionIds)
        assertEquals(1, repository.completions.size)
        assertEquals(1, repository.completions.single().score)
    }

    @Test
    fun pauseStopsTicksAndScreenExitAbandonsWithoutCompletion() = runTest(dispatcher) {
        val repository = FakeGameStatsRepository()
        val viewModel = SnakeViewModel(
            statsRepository = repository,
            tickDispatcher = dispatcher,
            sessionIdFactory = { "snake-session" },
        )

        viewModel.onEvent(SnakeEvent.Start(SnakeBoundaryMode.WALLS))
        val initialHead = viewModel.uiState.value.segments.first()
        viewModel.onEvent(SnakeEvent.TogglePause)
        advanceTimeBy(1_000)
        assertEquals(initialHead, viewModel.uiState.value.segments.first())

        viewModel.onEvent(SnakeEvent.ScreenExit)
        advanceUntilIdle()
        assertEquals(SnakePhase.READY, viewModel.uiState.value.phase)
        assertTrue(repository.completions.isEmpty())
    }
}

private class FakeGameStatsRepository : GameStatsRepository {
    override val stats: Flow<GameStatsSnapshot> = MutableStateFlow(GameStatsSnapshot())
    val startedSessionIds = mutableListOf<String>()
    val completions = mutableListOf<GameCompletion>()

    override suspend fun startSession(gameId: GameId, sessionId: String, startedAt: Long) {
        startedSessionIds += sessionId
    }

    override suspend fun completeSession(
        gameId: GameId,
        sessionId: String,
        completion: GameCompletion,
    ) {
        completions += completion
    }
}

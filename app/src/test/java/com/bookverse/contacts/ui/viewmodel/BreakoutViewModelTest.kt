package com.bookverse.contacts.ui.viewmodel

import com.bookverse.contacts.domain.games.BreakoutPhase
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.domain.games.GameStatsSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BreakoutViewModelTest {
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
    fun startRegistersOneSessionAndScreenExitAbandonsIt() = runTest(dispatcher) {
        val repository = BreakoutFakeGameStatsRepository()
        val viewModel = BreakoutViewModel(
            statsRepository = repository,
            tickDispatcher = dispatcher,
            clock = { 42L },
            sessionIdFactory = { "breakout-session" },
        )

        viewModel.onEvent(BreakoutEvent.Start)
        runCurrent()

        assertEquals(BreakoutPhase.RUNNING, viewModel.uiState.value.phase)
        assertEquals(listOf("breakout-session"), repository.startedSessionIds)
        assertTrue(repository.completions.isEmpty())

        viewModel.onEvent(BreakoutEvent.ScreenExit)
        advanceUntilIdle()

        assertEquals(BreakoutPhase.READY, viewModel.uiState.value.phase)
        assertTrue(repository.completions.isEmpty())
    }
}

private class BreakoutFakeGameStatsRepository : GameStatsRepository {
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

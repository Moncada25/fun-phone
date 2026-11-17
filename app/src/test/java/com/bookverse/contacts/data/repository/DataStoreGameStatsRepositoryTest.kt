package com.bookverse.contacts.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameOutcome
import com.bookverse.contacts.domain.games.GameRecordKind
import com.bookverse.contacts.domain.games.gameRecordKinds
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreGameStatsRepositoryTest {
    @Test
    fun legacyImportPreservesRecordsAndRunsOnlyOnce() = runTest {
        var reads = 0
        val repository = repository(
            legacySource = LegacyGameStatsSource {
                reads += 1
                LegacyGameStats(
                    sessionsStarted = mapOf(GameId.SNAKE to 4),
                    wins = mapOf(GameId.MINESWEEPER to 3),
                    bestScores = mapOf(GameId.SNAKE to 12),
                    bestTimes = mapOf(GameId.PUZZLE to 80),
                    bestMoves = mapOf(GameId.MEMORY to 19),
                    currentWinStreak = 2,
                    bestWinStreak = 5,
                )
            },
        )

        val first = repository.stats.first()
        val second = repository.stats.first()

        assertEquals(1, reads)
        assertEquals(4, first.games.getValue(GameId.SNAKE).sessionsStarted)
        assertEquals(12, first.games.getValue(GameId.SNAKE).bestScore)
        assertEquals(3, first.games.getValue(GameId.MINESWEEPER).wins)
        assertEquals(3, first.games.getValue(GameId.MINESWEEPER).sessionsCompleted)
        assertEquals(80, first.games.getValue(GameId.PUZZLE).bestTimeSeconds)
        assertEquals(19, first.games.getValue(GameId.MEMORY).bestMoves)
        assertEquals(5, second.bestWinStreak)
    }

    @Test
    fun duplicateSessionEventsAreIdempotent() = runTest {
        val repository = repository()

        repository.startSession(GameId.SNAKE, "one", 10L)
        repository.startSession(GameId.SNAKE, "one", 10L)
        repository.completeSession(GameId.SNAKE, "one", GameCompletion(score = 8))
        repository.completeSession(GameId.SNAKE, "one", GameCompletion(score = 99))

        val performance = repository.stats.first().games.getValue(GameId.SNAKE)
        assertEquals(1, performance.sessionsStarted)
        assertEquals(1, performance.sessionsCompleted)
        assertEquals(8, performance.bestScore)
    }

    @Test
    fun recordsKeepHighestScoreLowestTimeAndLowestMoves() = runTest {
        val repository = repository()

        complete(repository, GameId.FUN_BIRD, "score-1", GameCompletion(score = 7))
        complete(repository, GameId.FUN_BIRD, "score-2", GameCompletion(score = 3))
        complete(repository, GameId.PUZZLE, "time-1", GameCompletion(durationSeconds = 90))
        complete(repository, GameId.PUZZLE, "time-2", GameCompletion(durationSeconds = 72))
        complete(repository, GameId.MEMORY, "moves-1", GameCompletion(moves = 20))
        complete(repository, GameId.MEMORY, "moves-2", GameCompletion(moves = 24))

        val snapshot = repository.stats.first()
        assertEquals(7, snapshot.games.getValue(GameId.FUN_BIRD).bestScore)
        assertEquals(72, snapshot.games.getValue(GameId.PUZZLE).bestTimeSeconds)
        assertEquals(20, snapshot.games.getValue(GameId.MEMORY).bestMoves)
    }

    @Test
    fun neutralCompletionDoesNotChangeStreakAndLossResetsIt() = runTest {
        val repository = repository()

        complete(
            repository,
            GameId.MINESWEEPER,
            "win",
            GameCompletion(outcome = GameOutcome.WIN),
        )
        complete(repository, GameId.SNAKE, "neutral", GameCompletion(score = 4))
        assertEquals(1, repository.stats.first().currentWinStreak)

        complete(
            repository,
            GameId.HANGMAN,
            "loss",
            GameCompletion(outcome = GameOutcome.LOSS),
        )
        val snapshot = repository.stats.first()
        assertEquals(0, snapshot.currentWinStreak)
        assertEquals(1, snapshot.bestWinStreak)
    }

    @Test
    fun favoriteUsesMostSessionsThenMostRecentStart() = runTest {
        val repository = repository()

        repository.startSession(GameId.SNAKE, "snake", 10L)
        repository.startSession(GameId.FUN_BIRD, "bird", 20L)
        assertEquals(GameId.FUN_BIRD, repository.stats.first().favoriteGame)

        repository.startSession(GameId.SNAKE, "snake-2", 30L)
        assertEquals(GameId.SNAKE, repository.stats.first().favoriteGame)
    }

    @Test
    fun everyGameHasExactlyOneRecordKind() {
        assertEquals(GameId.entries.toSet(), gameRecordKinds.keys)
        assertEquals(GameRecordKind.HIGH_SCORE, gameRecordKinds[GameId.SNAKE])
        assertEquals(GameRecordKind.LOW_TIME, gameRecordKinds[GameId.PUZZLE])
        assertEquals(GameRecordKind.LOW_MOVES, gameRecordKinds[GameId.MEMORY])
        assertEquals(GameRecordKind.WIN_COUNT, gameRecordKinds[GameId.SUDOKU])
        assertNull(gameRecordKinds.keys.singleOrNull { it.storageKey.isBlank() })
        assertTrue(GameId.entries.map(GameId::storageKey).toSet().size == GameId.entries.size)
    }

    private fun TestScope.repository(
        legacySource: LegacyGameStatsSource = LegacyGameStatsSource { LegacyGameStats() },
    ): DataStoreGameStatsRepository {
        val directory = File.createTempFile("game-stats", "test").apply {
            delete()
            mkdirs()
            deleteOnExit()
        }
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(directory, "stats.preferences_pb") },
        )
        return DataStoreGameStatsRepository(dataStore, legacySource)
    }

    private suspend fun complete(
        repository: DataStoreGameStatsRepository,
        gameId: GameId,
        sessionId: String,
        completion: GameCompletion,
    ) {
        repository.startSession(gameId, sessionId, 1L)
        repository.completeSession(gameId, sessionId, completion)
    }
}

package com.bookverse.contacts.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameOutcome
import com.bookverse.contacts.domain.games.GamePerformance
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.domain.games.GameStatsSnapshot
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DataStoreGameStatsRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val legacySource: LegacyGameStatsSource,
) : GameStatsRepository {
    private val migrationMutex = Mutex()
    private var migrationChecked = false

    override val stats: Flow<GameStatsSnapshot> = flow {
        ensureMigrated()
        emitAll(
            dataStore.data
                .catch { error ->
                    if (error is IOException) emit(emptyPreferences()) else throw error
                }
                .map(::toSnapshot)
        )
    }

    override suspend fun startSession(gameId: GameId, sessionId: String, startedAt: Long) {
        require(sessionId.isNotBlank()) { "sessionId must not be blank" }
        ensureMigrated()
        dataStore.edit { preferences ->
            val activeKey = activeSessionKey(gameId)
            if (preferences[activeKey] == sessionId) return@edit

            val startedKey = sessionsStartedKey(gameId)
            preferences[startedKey] = (preferences[startedKey] ?: 0) + 1
            preferences[lastStartedKey(gameId)] = startedAt.coerceAtLeast(0L)
            preferences[activeKey] = sessionId
        }
    }

    override suspend fun completeSession(
        gameId: GameId,
        sessionId: String,
        completion: GameCompletion,
    ) {
        require(sessionId.isNotBlank()) { "sessionId must not be blank" }
        ensureMigrated()
        dataStore.edit { preferences ->
            val activeKey = activeSessionKey(gameId)
            if (preferences[activeKey] != sessionId) return@edit

            val completedKey = sessionsCompletedKey(gameId)
            preferences[completedKey] = (preferences[completedKey] ?: 0) + 1
            preferences.remove(activeKey)

            completion.score?.takeIf { it >= 0 }?.let { score ->
                val key = bestScoreKey(gameId)
                preferences[key] = maxOf(preferences[key] ?: 0, score)
            }
            completion.durationSeconds?.takeIf { it > 0 }?.let { seconds ->
                val key = bestTimeKey(gameId)
                preferences[key] = lowerPositive(preferences[key] ?: 0, seconds)
            }
            completion.moves?.takeIf { it > 0 }?.let { moves ->
                val key = bestMovesKey(gameId)
                preferences[key] = lowerPositive(preferences[key] ?: 0, moves)
            }

            when (completion.outcome) {
                GameOutcome.WIN -> {
                    val winsKey = winsKey(gameId)
                    preferences[winsKey] = (preferences[winsKey] ?: 0) + 1
                    val newStreak = (preferences[CURRENT_WIN_STREAK] ?: 0) + 1
                    preferences[CURRENT_WIN_STREAK] = newStreak
                    preferences[BEST_WIN_STREAK] = maxOf(
                        preferences[BEST_WIN_STREAK] ?: 0,
                        newStreak,
                    )
                }

                GameOutcome.LOSS -> {
                    val lossesKey = lossesKey(gameId)
                    preferences[lossesKey] = (preferences[lossesKey] ?: 0) + 1
                    preferences[CURRENT_WIN_STREAK] = 0
                }

                GameOutcome.NEUTRAL -> Unit
            }
        }
    }

    private suspend fun ensureMigrated() {
        if (migrationChecked) return
        migrationMutex.withLock {
            if (migrationChecked) return@withLock
            dataStore.edit { preferences ->
                if ((preferences[MIGRATION_VERSION] ?: 0) >= CURRENT_MIGRATION_VERSION) return@edit

                val legacy = legacySource.read()
                GameId.entries.forEach { gameId ->
                    val currentStarted = preferences[sessionsStartedKey(gameId)] ?: 0
                    preferences[sessionsStartedKey(gameId)] = maxOf(
                        currentStarted,
                        legacy.sessionsStarted[gameId] ?: 0,
                    )

                    val legacyWins = legacy.wins[gameId] ?: 0
                    preferences[winsKey(gameId)] = maxOf(
                        preferences[winsKey(gameId)] ?: 0,
                        legacyWins,
                    )
                    preferences[sessionsCompletedKey(gameId)] = maxOf(
                        preferences[sessionsCompletedKey(gameId)] ?: 0,
                        legacyWins,
                    )

                    legacy.bestScores[gameId]?.let { value ->
                        preferences[bestScoreKey(gameId)] = maxOf(
                            preferences[bestScoreKey(gameId)] ?: 0,
                            value,
                        )
                    }
                    legacy.bestTimes[gameId]?.takeIf { it > 0 }?.let { value ->
                        val key = bestTimeKey(gameId)
                        preferences[key] = lowerPositive(preferences[key] ?: 0, value)
                    }
                    legacy.bestMoves[gameId]?.takeIf { it > 0 }?.let { value ->
                        val key = bestMovesKey(gameId)
                        preferences[key] = lowerPositive(preferences[key] ?: 0, value)
                    }
                }
                preferences[CURRENT_WIN_STREAK] = maxOf(
                    preferences[CURRENT_WIN_STREAK] ?: 0,
                    legacy.currentWinStreak,
                )
                preferences[BEST_WIN_STREAK] = maxOf(
                    preferences[BEST_WIN_STREAK] ?: 0,
                    legacy.bestWinStreak,
                    preferences[CURRENT_WIN_STREAK] ?: 0,
                )
                preferences[MIGRATION_VERSION] = CURRENT_MIGRATION_VERSION
            }
            migrationChecked = true
        }
    }

    suspend fun exportRaw(): Pair<Map<String, Int>, Map<String, Long>> {
        ensureMigrated()
        val ints = mutableMapOf<String, Int>()
        val longs = mutableMapOf<String, Long>()
        dataStore.data.first().asMap().forEach { (key, value) ->
            when (value) {
                is Int -> ints[key.name] = value
                is Long -> longs[key.name] = value
                else -> Unit
            }
        }
        return ints to longs
    }

    suspend fun importRaw(ints: Map<String, Int>, longs: Map<String, Long>) {
        ensureMigrated()
        dataStore.edit { preferences ->
            preferences.clear()
            ints.forEach { (name, value) -> preferences[intPreferencesKey(name)] = value }
            longs.forEach { (name, value) -> preferences[longPreferencesKey(name)] = value }
            preferences[MIGRATION_VERSION] = CURRENT_MIGRATION_VERSION
        }
    }

    private fun toSnapshot(preferences: Preferences): GameStatsSnapshot = GameStatsSnapshot(
        games = GameId.entries.associateWith { gameId ->
            GamePerformance(
                sessionsStarted = preferences[sessionsStartedKey(gameId)] ?: 0,
                sessionsCompleted = preferences[sessionsCompletedKey(gameId)] ?: 0,
                wins = preferences[winsKey(gameId)] ?: 0,
                losses = preferences[lossesKey(gameId)] ?: 0,
                lastStartedAt = preferences[lastStartedKey(gameId)] ?: 0L,
                bestScore = preferences[bestScoreKey(gameId)] ?: 0,
                bestTimeSeconds = preferences[bestTimeKey(gameId)] ?: 0,
                bestMoves = preferences[bestMovesKey(gameId)] ?: 0,
            )
        },
        currentWinStreak = preferences[CURRENT_WIN_STREAK] ?: 0,
        bestWinStreak = preferences[BEST_WIN_STREAK] ?: 0,
    )

    companion object {
        private const val DATA_STORE_NAME = "game_stats_v2"
        private const val CURRENT_MIGRATION_VERSION = 1
        private val MIGRATION_VERSION = intPreferencesKey("migration_version")
        private val CURRENT_WIN_STREAK = intPreferencesKey("current_win_streak")
        private val BEST_WIN_STREAK = intPreferencesKey("best_win_streak")

        fun create(context: Context): DataStoreGameStatsRepository {
            val appContext = context.applicationContext
            val dataStore = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                produceFile = { appContext.preferencesDataStoreFile(DATA_STORE_NAME) },
            )
            return DataStoreGameStatsRepository(
                dataStore = dataStore,
                legacySource = AndroidLegacyGameStatsSource(appContext),
            )
        }
    }
}

internal data class LegacyGameStats(
    val sessionsStarted: Map<GameId, Int> = emptyMap(),
    val wins: Map<GameId, Int> = emptyMap(),
    val bestScores: Map<GameId, Int> = emptyMap(),
    val bestTimes: Map<GameId, Int> = emptyMap(),
    val bestMoves: Map<GameId, Int> = emptyMap(),
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
)

internal fun interface LegacyGameStatsSource {
    fun read(): LegacyGameStats
}

private class AndroidLegacyGameStatsSource(
    private val context: Context,
) : LegacyGameStatsSource {
    override fun read(): LegacyGameStats {
        val stats = context.getSharedPreferences("games_stats", Context.MODE_PRIVATE)
        val usage = context.getSharedPreferences("game_usage_prefs", Context.MODE_PRIVATE)
        val snake = context.getSharedPreferences("snake_prefs", Context.MODE_PRIVATE)
        val breakout = context.getSharedPreferences("breakout_prefs", Context.MODE_PRIVATE)
        val game2048 = context.getSharedPreferences("game_2048_prefs", Context.MODE_PRIVATE)

        return LegacyGameStats(
            sessionsStarted = mapOf(
                GameId.FUN_BIRD to usage.getInt("usage_fun_bird", 0),
                GameId.SNAKE to usage.getInt("usage_snake", 0),
                GameId.MINESWEEPER to usage.getInt("usage_minesweeper", 0),
                GameId.GAME_2048 to usage.getInt("usage_2048", 0),
                GameId.PUZZLE to usage.getInt("usage_puzzle", 0),
                GameId.TIC_TAC_TOE to usage.getInt("usage_tictactoe", 0),
                GameId.HANGMAN to usage.getInt("usage_hangman", 0),
                GameId.BREAKOUT to usage.getInt("usage_breakout", 0),
                GameId.SUDOKU to usage.getInt("usage_sudoku", 0),
                GameId.MEMORY to usage.getInt("usage_memory", 0),
            ),
            wins = mapOf(
                GameId.MINESWEEPER to stats.getInt("minesweeper_wins", 0),
                GameId.TIC_TAC_TOE to stats.getInt("tic_tac_toe_wins", 0),
                GameId.HANGMAN to stats.getInt("hangman_wins", 0),
                GameId.SUDOKU to stats.getInt("sudoku_solved_count", 0),
            ),
            bestScores = mapOf(
                GameId.FUN_BIRD to stats.getInt("fun_bird_best_score", 0),
                GameId.SNAKE to snake.getInt("high_score", 0),
                GameId.GAME_2048 to game2048.getInt("high_score_2048", 0),
                GameId.BREAKOUT to breakout.getInt("highScore", 0),
            ),
            bestTimes = mapOf(
                GameId.PUZZLE to stats.getInt("puzzle_best_time_seconds", 0),
            ),
            bestMoves = mapOf(
                GameId.MEMORY to stats.getInt("memory_best_moves", 0),
            ),
            currentWinStreak = stats.getInt("current_win_streak", 0),
            bestWinStreak = stats.getInt("best_win_streak", 0),
        )
    }
}

private fun lowerPositive(current: Int, candidate: Int): Int = when {
    current <= 0 -> candidate
    candidate <= 0 -> current
    else -> minOf(current, candidate)
}

private fun sessionsStartedKey(gameId: GameId) =
    intPreferencesKey("game.${gameId.storageKey}.sessions_started")

private fun sessionsCompletedKey(gameId: GameId) =
    intPreferencesKey("game.${gameId.storageKey}.sessions_completed")

private fun winsKey(gameId: GameId) = intPreferencesKey("game.${gameId.storageKey}.wins")

private fun lossesKey(gameId: GameId) = intPreferencesKey("game.${gameId.storageKey}.losses")

private fun lastStartedKey(gameId: GameId) =
    longPreferencesKey("game.${gameId.storageKey}.last_started_at")

private fun bestScoreKey(gameId: GameId) = intPreferencesKey("game.${gameId.storageKey}.best_score")

private fun bestTimeKey(gameId: GameId) =
    intPreferencesKey("game.${gameId.storageKey}.best_time_seconds")

private fun bestMovesKey(gameId: GameId) = intPreferencesKey("game.${gameId.storageKey}.best_moves")

private fun activeSessionKey(gameId: GameId) =
    stringPreferencesKey("game.${gameId.storageKey}.active_session_id")

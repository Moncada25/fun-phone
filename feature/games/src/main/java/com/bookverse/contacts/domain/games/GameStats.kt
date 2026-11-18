package com.bookverse.contacts.domain.games

import kotlinx.coroutines.flow.Flow

enum class GameId(val storageKey: String) {
    FUN_BIRD("fun_bird"),
    SNAKE("snake"),
    MINESWEEPER("minesweeper"),
    GAME_2048("2048"),
    PUZZLE("puzzle"),
    TIC_TAC_TOE("tic_tac_toe"),
    HANGMAN("hangman"),
    BREAKOUT("breakout"),
    SUDOKU("sudoku"),
    MEMORY("memory"),
}

enum class GameOutcome {
    WIN,
    LOSS,
    NEUTRAL,
}

enum class GameRecordKind {
    HIGH_SCORE,
    LOW_TIME,
    LOW_MOVES,
    WIN_COUNT,
}

data class GameCompletion(
    val outcome: GameOutcome = GameOutcome.NEUTRAL,
    val score: Int? = null,
    val durationSeconds: Int? = null,
    val moves: Int? = null,
)

data class GamePerformance(
    val sessionsStarted: Int = 0,
    val sessionsCompleted: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val lastStartedAt: Long = 0L,
    val bestScore: Int = 0,
    val bestTimeSeconds: Int = 0,
    val bestMoves: Int = 0,
) {
    fun recordValue(kind: GameRecordKind): Int = when (kind) {
        GameRecordKind.HIGH_SCORE -> bestScore
        GameRecordKind.LOW_TIME -> bestTimeSeconds
        GameRecordKind.LOW_MOVES -> bestMoves
        GameRecordKind.WIN_COUNT -> wins
    }
}

data class GameStatsSnapshot(
    val games: Map<GameId, GamePerformance> = GameId.entries.associateWith { GamePerformance() },
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
) {
    val totalSessionsStarted: Int
        get() = games.values.sumOf(GamePerformance::sessionsStarted)

    val totalSessionsCompleted: Int
        get() = games.values.sumOf(GamePerformance::sessionsCompleted)

    val gamesTried: Int
        get() = games.values.count { performance ->
            performance.sessionsStarted > 0 ||
                performance.sessionsCompleted > 0 ||
                performance.bestScore > 0 ||
                performance.bestTimeSeconds > 0 ||
                performance.bestMoves > 0 ||
                performance.wins > 0
        }

    val favoriteGame: GameId?
        get() = games.entries
            .filter { it.value.sessionsStarted > 0 }
            .maxWithOrNull(
                compareBy<Map.Entry<GameId, GamePerformance>> { it.value.sessionsStarted }
                    .thenBy { it.value.lastStartedAt }
                    .thenByDescending { it.key.ordinal }
            )
            ?.key
}

interface GameStatsRepository {
    val stats: Flow<GameStatsSnapshot>

    suspend fun startSession(gameId: GameId, sessionId: String, startedAt: Long)

    suspend fun completeSession(
        gameId: GameId,
        sessionId: String,
        completion: GameCompletion,
    )
}

val gameRecordKinds: Map<GameId, GameRecordKind> = mapOf(
    GameId.FUN_BIRD to GameRecordKind.HIGH_SCORE,
    GameId.SNAKE to GameRecordKind.HIGH_SCORE,
    GameId.MINESWEEPER to GameRecordKind.WIN_COUNT,
    GameId.GAME_2048 to GameRecordKind.HIGH_SCORE,
    GameId.PUZZLE to GameRecordKind.LOW_TIME,
    GameId.TIC_TAC_TOE to GameRecordKind.WIN_COUNT,
    GameId.HANGMAN to GameRecordKind.WIN_COUNT,
    GameId.BREAKOUT to GameRecordKind.HIGH_SCORE,
    GameId.SUDOKU to GameRecordKind.WIN_COUNT,
    GameId.MEMORY to GameRecordKind.LOW_MOVES,
)

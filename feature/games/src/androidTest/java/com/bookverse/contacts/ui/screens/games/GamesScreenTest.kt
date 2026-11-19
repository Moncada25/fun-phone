package com.bookverse.contacts.ui.screens.games

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GamePerformance
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.domain.games.GameStatsSnapshot
import com.bookverse.contacts.feature.games.R
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GamesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyScoreboardInvitesToPressStart() {
        setGamesScreen(GameStatsSnapshot())

        composeRule.onNodeWithText(text(R.string.games_arcade_scoreboard)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.games_arcade_press_start)).assertIsDisplayed()
    }

    @Test
    fun scoreboardOpensDashboardWithGameRecords() {
        val snake = GamePerformance(sessionsStarted = 3, sessionsCompleted = 2, lastStartedAt = 10L, bestScore = 1_250)
        val snapshot = GameStatsSnapshot(
            games = GameId.entries.associateWith { GamePerformance() } + (GameId.SNAKE to snake),
            bestWinStreak = 2,
        )
        setGamesScreen(snapshot)

        composeRule.onNodeWithText(text(R.string.games_dashboard_favorite_game)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.games_arcade_view_records)).performClick()

        composeRule.onNodeWithText(text(R.string.games_dashboard_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.games_dashboard_records_title)).assertIsDisplayed()
    }

    @Test
    fun everyCabinetLaunchesItsOwnGame() {
        val launched = mutableListOf<GameId>()
        setGamesScreen(GameStatsSnapshot(), onLaunch = { launched += it })
        val titles = mapOf(
            GameId.FUN_BIRD to R.string.fun_bird_title,
            GameId.SNAKE to R.string.snake_title,
            GameId.MINESWEEPER to R.string.minesweeper_title,
            GameId.GAME_2048 to R.string.game_2048_title,
            GameId.PUZZLE to R.string.puzzle_title,
            GameId.TIC_TAC_TOE to R.string.tic_tac_toe_title,
            GameId.HANGMAN to R.string.hangman_title,
            GameId.BREAKOUT to R.string.breakout_title,
            GameId.SUDOKU to R.string.sudoku_title,
            GameId.MEMORY to R.string.memory_game_title,
        )

        titles.forEach { (_, titleRes) ->
            val title = text(titleRes)
            composeRule.onNode(hasScrollToNodeAction() and hasVerticalScroll())
                .performScrollToNode(hasContentDescription(title))
            val cabinets = composeRule.onAllNodesWithContentDescription(title)
            cabinets[cabinets.fetchSemanticsNodes().lastIndex].performClick()
        }

        assertEquals(titles.keys.toList(), launched)
    }

    private fun hasVerticalScroll() = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    private fun setGamesScreen(
        snapshot: GameStatsSnapshot,
        onLaunch: (GameId) -> Unit = {},
    ) {
        val repository = FakeGameStatsRepository(snapshot)
        composeRule.setContent {
            MaterialTheme {
                GamesScreen(
                    onBackClick = {},
                    onFunBirdClick = { onLaunch(GameId.FUN_BIRD) },
                    onTicTacToeClick = { onLaunch(GameId.TIC_TAC_TOE) },
                    onPuzzleClick = { onLaunch(GameId.PUZZLE) },
                    onHangmanClick = { onLaunch(GameId.HANGMAN) },
                    onSnakeClick = { onLaunch(GameId.SNAKE) },
                    onMemoryGameClick = { onLaunch(GameId.MEMORY) },
                    onBreakoutClick = { onLaunch(GameId.BREAKOUT) },
                    on2048Click = { onLaunch(GameId.GAME_2048) },
                    onMinesweeperClick = { onLaunch(GameId.MINESWEEPER) },
                    onSudokuClick = { onLaunch(GameId.SUDOKU) },
                    gameStatsRepository = repository,
                )
            }
        }
    }

    private fun text(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)
}

private class FakeGameStatsRepository(snapshot: GameStatsSnapshot) : GameStatsRepository {
    override val stats = MutableStateFlow(snapshot)

    override suspend fun startSession(gameId: GameId, sessionId: String, startedAt: Long) = Unit

    override suspend fun completeSession(
        gameId: GameId,
        sessionId: String,
        completion: GameCompletion,
    ) = Unit
}

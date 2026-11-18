package com.bookverse.contacts.ui.screens.games

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GamePerformance
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.domain.games.GameStatsSnapshot
import com.bookverse.contacts.feature.games.R
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.BreakoutViewModel
import com.bookverse.contacts.ui.viewmodel.SnakeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class GamesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun joystickCyclesTheGamesShownOnTheCabinetScreen() {
        setGamesScreen(GameStatsSnapshot())

        composeRule.onNodeWithText(text(R.string.games_arcade_select)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.fun_bird_title)).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(text(R.string.games_arcade_joystick))
            .performCustomAccessibilityActionWithLabel(text(R.string.cd_move_right))

        composeRule.onNodeWithText(text(R.string.games_arcade_position, "2", "10")).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.snake_title)).assertIsDisplayed()
    }

    @Test
    fun selectorStartsOnTheMostRecentGame() {
        val sudoku = GamePerformance(sessionsStarted = 1, lastStartedAt = 42L)
        setGamesScreen(
            GameStatsSnapshot(games = GameId.entries.associateWith { GamePerformance() } + (GameId.SUDOKU to sudoku)),
        )

        composeRule.onNodeWithText(text(R.string.games_arcade_position, "9", "10")).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.sudoku_title)).assertIsDisplayed()
    }

    @Test
    fun buttonARunsTheSelectedGameInsideTheCabinet() {
        setGamesScreen(GameStatsSnapshot())

        composeRule.onNodeWithContentDescription(text(R.string.play_label)).performClick()

        composeRule.onNodeWithText(text(R.string.games_how_to_play)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.games_arcade_exit)).assertExists()
    }

    @Test
    fun buttonBOpensTheRecordsFromTheSelector() {
        setGamesScreen(GameStatsSnapshot())

        composeRule.onNodeWithContentDescription(text(R.string.games_arcade_records)).performClick()

        composeRule.onNodeWithText(text(R.string.games_dashboard_title)).assertIsDisplayed()
    }

    private fun setGamesScreen(snapshot: GameStatsSnapshot) {
        val repository = FakeGameStatsRepository(snapshot)
        val appSettingsViewModel = AppSettingsViewModel()
        val snakeFactory = viewModelFactory { initializer { SnakeViewModel(repository) } }
        val breakoutFactory = viewModelFactory { initializer { BreakoutViewModel(repository) } }
        composeRule.setContent {
            MaterialTheme {
                GamesScreen(
                    onBackClick = {},
                    appSettingsViewModel = appSettingsViewModel,
                    gameStatsRepository = repository,
                    snakeViewModelFactory = snakeFactory,
                    breakoutViewModelFactory = breakoutFactory,
                )
            }
        }
    }

    private fun text(resId: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId, *args)
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

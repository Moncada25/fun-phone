package com.bookverse.contacts.ui.screens.games

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameRecordKind
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.domain.games.GameStatsSnapshot
import com.bookverse.contacts.domain.games.gameRecordKinds
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds

data class GameData(
    val id: GameId,
    val title: String,
    val iconRes: Int,
    val onClick: () -> Unit
)

@SuppressLint("ConfigurationScreenWidthHeight", "FrequentlyChangingValue")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GamesScreen(
    onBackClick: () -> Unit,
    onFunBirdClick: () -> Unit,
    onTicTacToeClick: () -> Unit,
    onPuzzleClick: () -> Unit,
    onHangmanClick: () -> Unit,
    onSnakeClick: () -> Unit,
    onMemoryGameClick: () -> Unit,
    onBreakoutClick: () -> Unit,
    on2048Click: () -> Unit,
    onMinesweeperClick: () -> Unit,
    onSudokuClick: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    gameStatsRepository: GameStatsRepository,
) {
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior()
    var visibleSheet by remember { mutableStateOf<GamesSheet?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScrollState = rememberScrollState()
    val accentColor = MaterialTheme.colorScheme.primary
    val resources = LocalResources.current
    val stats by gameStatsRepository.stats.collectAsStateWithLifecycle(
        initialValue = GameStatsSnapshot(),
    )

    val games = remember(resources) {
        listOf(
            GameData(
                id = GameId.FUN_BIRD,
                title = resources.getString(R.string.fun_bird_title),
                iconRes = R.drawable.game_fun_bird,
                onClick = onFunBirdClick
            ),
            GameData(
                id = GameId.SNAKE,
                title = resources.getString(R.string.snake_title),
                iconRes = R.drawable.game_snake,
                onClick = onSnakeClick
            ),
            GameData(
                id = GameId.MINESWEEPER,
                title = resources.getString(R.string.minesweeper_title),
                iconRes = R.drawable.game_minesweeper,
                onClick = onMinesweeperClick
            ),
            GameData(
                id = GameId.GAME_2048,
                title = resources.getString(R.string.game_2048_title),
                iconRes = R.drawable.game_2048,
                onClick = on2048Click
            ),
            GameData(
                id = GameId.PUZZLE,
                title = resources.getString(R.string.puzzle_title),
                iconRes = R.drawable.game_puzzle,
                onClick = onPuzzleClick
            ),
            GameData(
                id = GameId.TIC_TAC_TOE,
                title = resources.getString(R.string.tic_tac_toe_title),
                iconRes = R.drawable.game_tictactoe,
                onClick = onTicTacToeClick
            ),
            GameData(
                id = GameId.HANGMAN,
                title = resources.getString(R.string.hangman_title),
                iconRes = R.drawable.game_hangman,
                onClick = onHangmanClick
            ),
            GameData(
                id = GameId.BREAKOUT,
                title = resources.getString(R.string.breakout_title),
                iconRes = R.drawable.game_breakout,
                onClick = onBreakoutClick
            ),
            GameData(
                id = GameId.SUDOKU,
                title = resources.getString(R.string.sudoku_title),
                iconRes = R.drawable.game_sudoku,
                onClick = onSudokuClick
            ),
            GameData(
                id = GameId.MEMORY,
                title = resources.getString(R.string.memory_game_title),
                iconRes = R.drawable.game_memory,
                onClick = onMemoryGameClick
            )
        )
    }

    val recentGames = remember(games, stats) {
        games.sortedWith(
            compareByDescending<GameData> { game ->
                stats.games[game.id]?.lastStartedAt ?: 0L
            }.thenBy { game -> games.indexOf(game) }
        ).take(5)
    }
    val pagerState = rememberPagerState(initialPage = 0) { recentGames.size }

    BackHandler {
        onBackClick()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                title = stringResource(R.string.games_title),
                actions = {
                    IconAction(
                        imageVector = Icons.Default.QueryStats,
                        contentDescription = stringResource(R.string.games_dashboard_action),
                        onClick = { visibleSheet = GamesSheet.Dashboard }
                    )
                },
                count = games.size,
                onBackClick = onBackClick,
                onCountClick = { visibleSheet = GamesSheet.FunFacts },
                appSettingsViewModel = appSettingsViewModel,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.games_featured_section),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            val cardWidth = 280.dp
            val edgePadding = remember(screenWidth) {
                ((screenWidth - cardWidth) / 2).coerceAtLeast(24.dp)
            }

            HorizontalPager(
                modifier = Modifier.fillMaxWidth(),
                state = pagerState,
                pageSize = PageSize.Fixed(cardWidth),
                contentPadding = PaddingValues(horizontal = edgePadding),
                pageSpacing = 16.dp,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
            ) { page ->
                val game = recentGames[page]
                var isVisible by remember { mutableStateOf(false) }

                LaunchedEffect(page) {
                    delay((page * 80L).milliseconds)
                    isVisible = true
                }

                val pageOffset =
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val proximity = (1f - pageOffset.absoluteValue).coerceIn(0f, 1f)
                val accordionScale by animateFloatAsState(
                    targetValue = 0.9f + 0.12f * proximity,
                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                    label = "featured_scale"
                )
                val accordionAlpha by animateFloatAsState(
                    targetValue = 0.6f + 0.4f * proximity,
                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
                    label = "featured_alpha"
                )

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(
                        animationSpec = tween(600, easing = FastOutSlowInEasing)
                    ) + scaleIn(
                        initialScale = 0.7f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    exit = fadeOut() + scaleOut()
                ) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            scaleX = accordionScale
                            scaleY = accordionScale
                            alpha = accordionAlpha
                        }
                    ) {
                        RetroGameCard(
                            game = game,
                            accentColor = accentColor,
                            onGameClick = game.onClick,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.games_all_games_section),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                games.forEach { game ->
                    CompactGameCard(
                        game = game,
                        onGameClick = game.onClick,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    val currentSheet = visibleSheet
    if (currentSheet != null) {
        LaunchedEffect(currentSheet) {
            sheetScrollState.scrollTo(0)
        }
        val configuration = LocalConfiguration.current
        val maxSheetHeight = configuration.screenHeightDp.dp / 2
        ModalBottomSheet(
            onDismissRequest = { visibleSheet = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .heightIn(max = maxSheetHeight)
                    .verticalScroll(sheetScrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentSheet) {
                    GamesSheet.FunFacts -> {
                        Text(
                            text = stringResource(R.string.games_fun_facts_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        gameFunFacts.forEach { fact ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${stringResource(fact.titleRes)} — ${fact.year}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(fact.factRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    GamesSheet.Dashboard -> {
                        val gameTitles = remember {
                            mapOf(
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
                        }
                        val favoriteGameTitleRes = stats.favoriteGame?.let(gameTitles::get)
                        val highScores = remember(stats) {
                            GameId.entries.mapNotNull { gameId ->
                                val titleRes = gameTitles[gameId] ?: return@mapNotNull null
                                val recordKind = gameRecordKinds.getValue(gameId)
                                val value = stats.games[gameId]?.recordValue(recordKind) ?: 0
                                GameMetricEntry(
                                    titleRes = titleRes,
                                    value = value,
                                    formatter = if (recordKind == GameRecordKind.LOW_TIME) {
                                        ::formatDuration
                                    } else {
                                        { metric -> metric.toString() }
                                    },
                                )
                            }
                        }
                        val recordedScores = remember(highScores) { highScores.filter { it.value > 0 } }
                        val hasAnyData = stats.totalSessionsStarted > 0 || recordedScores.isNotEmpty()

                        Text(
                            text = stringResource(R.string.games_dashboard_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )

                        if (!hasAnyData) {
                            Text(
                                text = stringResource(R.string.games_dashboard_no_data),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.games_dashboard_summary),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val dashboardEntries = listOf(
                                DashboardEntry(
                                    stringResource(R.string.games_dashboard_total_played),
                                    stats.totalSessionsStarted.toString(),
                                ),
                                DashboardEntry(
                                    stringResource(R.string.games_dashboard_completed),
                                    stats.totalSessionsCompleted.toString(),
                                ),
                                DashboardEntry(
                                    stringResource(R.string.games_dashboard_games_tried),
                                    "${stats.gamesTried}/${GameId.entries.size}",
                                ),
                                DashboardEntry(
                                    stringResource(R.string.games_dashboard_favorite_game),
                                    favoriteGameTitleRes?.let { stringResource(it) }
                                        ?: stringResource(R.string.games_dashboard_no_favorite),
                                    compactValue = true,
                                ),
                                DashboardEntry(
                                    stringResource(R.string.games_dashboard_best_streak),
                                    stats.bestWinStreak.toString(),
                                ),
                            )
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val columns = if (maxWidth >= 600.dp) 3 else 2
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    dashboardEntries.chunked(columns).forEach { rowEntries ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            rowEntries.forEach { entry ->
                                                DashboardStatCard(
                                                    modifier = Modifier.weight(1f),
                                                    label = entry.label,
                                                    value = entry.value,
                                                    accentColor = accentColor,
                                                    valueStyle = if (entry.compactValue) {
                                                        MaterialTheme.typography.titleMedium
                                                    } else {
                                                        MaterialTheme.typography.headlineMedium
                                                    },
                                                )
                                            }
                                            repeat(columns - rowEntries.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            if (recordedScores.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                                Text(
                                    text = stringResource(R.string.games_dashboard_records_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    recordedScores.forEach { entry ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(entry.titleRes),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = accentColor.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = entry.displayValue,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = accentColor,
                                                    modifier = Modifier.padding(
                                                        horizontal = 10.dp,
                                                        vertical = 4.dp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

private data class GameFunFact(
    val titleRes: Int,
    val year: String,
    val factRes: Int
)

private data class GameMetricEntry(
    val titleRes: Int,
    val value: Int,
    val formatter: (Int) -> String = { it.toString() }
) {
    val displayValue: String
        get() = formatter(value)
}

private data class DashboardEntry(
    val label: String,
    val value: String,
    val compactValue: Boolean = false,
)

private val gameFunFacts = listOf(
    GameFunFact(
        titleRes = R.string.fun_bird_title,
        year = "2013",
        factRes = R.string.game_fact_fun_bird
    ),
    GameFunFact(
        titleRes = R.string.snake_title,
        year = "1997",
        factRes = R.string.game_fact_snake
    ),
    GameFunFact(
        titleRes = R.string.breakout_title,
        year = "1976",
        factRes = R.string.game_fact_breakout
    ),
    GameFunFact(
        titleRes = R.string.memory_game_title,
        year = "1959",
        factRes = R.string.game_fact_memory
    ),
    GameFunFact(
        titleRes = R.string.minesweeper_title,
        year = "1990",
        factRes = R.string.game_fact_minesweeper
    ),
    GameFunFact(
        titleRes = R.string.tic_tac_toe_title,
        year = "1952",
        factRes = R.string.game_fact_tictactoe
    ),
    GameFunFact(
        titleRes = R.string.puzzle_title,
        year = "1874",
        factRes = R.string.game_fact_puzzle
    ),
    GameFunFact(
        titleRes = R.string.hangman_title,
        year = "1894",
        factRes = R.string.game_fact_hangman
    ),
    GameFunFact(
        titleRes = R.string.game_2048_title,
        year = "2014",
        factRes = R.string.game_fact_2048
    ),
    GameFunFact(
        titleRes = R.string.sudoku_title,
        year = "1979",
        factRes = R.string.game_fact_sudoku
    )
)

private enum class GamesSheet {
    FunFacts,
    Dashboard
}

private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "00:00"
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSeconds)
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color,
    valueStyle: TextStyle = MaterialTheme.typography.headlineMedium
) {
    Surface(
        modifier = modifier.heightIn(min = 90.dp, max = 120.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = valueStyle,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RetroGameCard(
    game: GameData,
    accentColor: Color,
    onGameClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    ElevatedCard(
        modifier = Modifier
            .width(280.dp)
            .height(360.dp)
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    isPressed = true
                    onGameClick()
                }
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = game.iconRes),
                    contentDescription = game.title,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .graphicsLayer {
                            rotationZ = if (isPressed) 5f else 0f
                        }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = game.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.games_play_now),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactGameCard(game: GameData, onGameClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "compact_card_scale"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        onClick = {
            isPressed = true
            onGameClick()
        },
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = game.iconRes),
                contentDescription = game.title,
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = game.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

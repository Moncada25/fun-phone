package com.bookverse.contacts.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameRecordKind
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.domain.games.GameStatsSnapshot
import com.bookverse.contacts.domain.games.gameRecordKinds
import com.bookverse.contacts.feature.games.R
import com.bookverse.contacts.ui.components.ArcadeDialogHeader
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.arcadeLabelStyle
import com.bookverse.contacts.ui.components.arcadeScoreStyle
import com.bookverse.contacts.ui.components.crtScanlines
import com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior
import com.bookverse.contacts.ui.components.withoutTop
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.theme.AppChrome
import com.bookverse.contacts.ui.theme.LocalAppChromeStyle
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold

@Immutable
data class GameData(
    val id: GameId,
    val title: String,
    val iconRes: Int,
    val onClick: () -> Unit
)

@Immutable
private data class GameCatalogEntry(
    val id: GameId,
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int,
)

@Immutable
private data class GameRecord(
    val kind: GameRecordKind,
    val value: Int,
)

private val gameCatalog = listOf(
    GameCatalogEntry(GameId.FUN_BIRD, R.string.fun_bird_title, CoreUiR.drawable.game_fun_bird),
    GameCatalogEntry(GameId.SNAKE, R.string.snake_title, CoreUiR.drawable.game_snake),
    GameCatalogEntry(GameId.MINESWEEPER, R.string.minesweeper_title, CoreUiR.drawable.game_minesweeper),
    GameCatalogEntry(GameId.GAME_2048, R.string.game_2048_title, CoreUiR.drawable.game_2048),
    GameCatalogEntry(GameId.PUZZLE, R.string.puzzle_title, CoreUiR.drawable.game_puzzle),
    GameCatalogEntry(GameId.TIC_TAC_TOE, R.string.tic_tac_toe_title, CoreUiR.drawable.game_tictactoe),
    GameCatalogEntry(GameId.HANGMAN, R.string.hangman_title, CoreUiR.drawable.game_hangman),
    GameCatalogEntry(GameId.BREAKOUT, R.string.breakout_title, CoreUiR.drawable.game_breakout),
    GameCatalogEntry(GameId.SUDOKU, R.string.sudoku_title, CoreUiR.drawable.game_sudoku),
    GameCatalogEntry(GameId.MEMORY, R.string.memory_game_title, CoreUiR.drawable.game_memory),
)

private val catalogById = gameCatalog.associateBy(GameCatalogEntry::id)

@OptIn(ExperimentalMaterial3Api::class)
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
    gameStatsRepository: GameStatsRepository,
) {
    val scrollBehavior = rememberAppBarScrollBehavior()
    var visibleSheet by remember { mutableStateOf<GamesSheet?>(null) }
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    val numberFormat = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val stats by gameStatsRepository.stats.collectAsStateWithLifecycle(
        initialValue = GameStatsSnapshot(),
    )

    val games = remember(resources) {
        val launchers = mapOf(
            GameId.FUN_BIRD to onFunBirdClick,
            GameId.SNAKE to onSnakeClick,
            GameId.MINESWEEPER to onMinesweeperClick,
            GameId.GAME_2048 to on2048Click,
            GameId.PUZZLE to onPuzzleClick,
            GameId.TIC_TAC_TOE to onTicTacToeClick,
            GameId.HANGMAN to onHangmanClick,
            GameId.BREAKOUT to onBreakoutClick,
            GameId.SUDOKU to onSudokuClick,
            GameId.MEMORY to onMemoryGameClick,
        )
        gameCatalog.map { entry ->
            GameData(
                id = entry.id,
                title = resources.getString(entry.titleRes),
                iconRes = entry.iconRes,
                onClick = launchers.getValue(entry.id),
            )
        }
    }

    val recentGames = remember(games, stats) {
        games.sortedWith(
            compareByDescending<GameData> { game ->
                stats.games[game.id]?.lastStartedAt ?: 0L
            }.thenBy { game -> games.indexOf(game) }
        ).take(5)
    }
    val records = remember(stats) {
        GameId.entries.associateWith { gameId -> stats.recordFor(gameId) }
    }
    val pagerState = rememberPagerState(initialPage = 0) { recentGames.size }
    val modern = LocalAppChromeStyle.current == AccentBarStyle.MODERN

    BackHandler {
        onBackClick()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                title = stringResource(CoreUiR.string.games_title),
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
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        val contentTopPadding = if (modern) paddingValues.calculateTopPadding() else 0.dp
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (modern) paddingValues.withoutTop() else paddingValues)
        ) {
            val horizontalPadding = if (maxWidth >= 600.dp) 24.dp else 16.dp
            val twoPaneHero = maxWidth >= 840.dp
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = contentTopPadding + 16.dp,
                    bottom = 24.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (twoPaneHero) {
                    item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                ArcadeSectionHeader(text = stringResource(R.string.games_dashboard_summary))
                                ArcadeScoreboard(
                                    stats = stats,
                                    numberFormat = numberFormat,
                                    onClick = { visibleSheet = GamesSheet.Dashboard },
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1.2f),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                ArcadeSectionHeader(text = stringResource(R.string.games_featured_section))
                                RecentGamesPager(
                                    games = recentGames,
                                    records = records,
                                    numberFormat = numberFormat,
                                    pagerState = pagerState,
                                )
                            }
                        }
                    }
                } else {
                    item(key = "scoreboard", span = { GridItemSpan(maxLineSpan) }) {
                        ArcadeScoreboard(
                            stats = stats,
                            numberFormat = numberFormat,
                            onClick = { visibleSheet = GamesSheet.Dashboard },
                        )
                    }
                    item(key = "recent_header", span = { GridItemSpan(maxLineSpan) }) {
                        ArcadeSectionHeader(text = stringResource(R.string.games_featured_section))
                    }
                    item(key = "recent_pager", span = { GridItemSpan(maxLineSpan) }) {
                        RecentGamesPager(
                            games = recentGames,
                            records = records,
                            numberFormat = numberFormat,
                            pagerState = pagerState,
                        )
                    }
                }
                item(key = "all_header", span = { GridItemSpan(maxLineSpan) }) {
                    ArcadeSectionHeader(text = stringResource(R.string.games_all_games_section))
                }
                items(games, key = { game -> game.id.storageKey }) { game ->
                    ArcadeCabinet(
                        game = game,
                        record = records.getValue(game.id),
                        numberFormat = numberFormat,
                    )
                }
            }
        }
    }

    val currentSheet = visibleSheet
    if (currentSheet != null) {
        GamesInfoSheet(
            sheet = currentSheet,
            stats = stats,
            records = records,
            numberFormat = numberFormat,
            onDismiss = { visibleSheet = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GamesInfoSheet(
    sheet: GamesSheet,
    stats: GameStatsSnapshot,
    records: Map<GameId, GameRecord>,
    numberFormat: NumberFormat,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScrollState = rememberScrollState()
    val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    LaunchedEffect(sheet) {
        sheetScrollState.scrollTo(0)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = windowHeight * 0.75f)
                .verticalScroll(sheetScrollState)
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (sheet) {
                GamesSheet.FunFacts -> FunFactsContent()
                GamesSheet.Dashboard -> DashboardContent(
                    stats = stats,
                    records = records,
                    numberFormat = numberFormat,
                )
            }
        }
    }
}

@Composable
private fun FunFactsContent() {
    ArcadeDialogHeader(
        title = stringResource(R.string.games_fun_facts_title),
        onClose = null,
    )
    gameFunFacts.forEach { fact ->
        val entry = catalogById.getValue(fact.gameId)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CrtScreen(
                iconRes = entry.iconRes,
                modifier = Modifier.size(56.dp),
                cornerRadius = 12.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(entry.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    ArcadeBadge(text = fact.year)
                }
                Text(
                    text = stringResource(fact.factRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    stats: GameStatsSnapshot,
    records: Map<GameId, GameRecord>,
    numberFormat: NumberFormat,
) {
    val recordedGames = remember(records) {
        gameCatalog.filter { entry -> records.getValue(entry.id).value > 0 }
    }
    val hasAnyData = stats.totalSessionsStarted > 0 || recordedGames.isNotEmpty()

    ArcadeDialogHeader(
        title = stringResource(R.string.games_dashboard_title),
        eyebrow = stringResource(R.string.games_arcade_scoreboard),
        onClose = null,
    )

    if (!hasAnyData) {
        Text(
            text = stringResource(R.string.games_dashboard_no_data),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }

    ArcadeSectionHeader(text = stringResource(R.string.games_dashboard_summary))

    val favoriteTitle = favoriteGameTitle(stats)
    val dashboardEntries = listOf(
        DashboardEntry(
            stringResource(R.string.games_dashboard_total_played),
            numberFormat.format(stats.totalSessionsStarted),
        ),
        DashboardEntry(
            stringResource(R.string.games_dashboard_completed),
            numberFormat.format(stats.totalSessionsCompleted),
        ),
        DashboardEntry(
            stringResource(R.string.games_dashboard_games_tried),
            "${numberFormat.format(stats.gamesTried)}/${numberFormat.format(GameId.entries.size)}",
        ),
        DashboardEntry(
            stringResource(R.string.games_dashboard_favorite_game),
            favoriteTitle,
            compactValue = true,
        ),
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 600.dp) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            dashboardEntries.chunked(columns).forEach { rowEntries ->
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowEntries.forEach { entry ->
                        DashboardStatCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            label = entry.label,
                            value = entry.value,
                            valueStyle = if (entry.compactValue) {
                                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            } else {
                                arcadeScoreStyle
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

    if (recordedGames.isNotEmpty()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ArcadeSectionHeader(text = stringResource(R.string.games_dashboard_records_title))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            recordedGames.forEach { entry ->
                val record = records.getValue(entry.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CrtScreen(
                        iconRes = entry.iconRes,
                        modifier = Modifier.size(44.dp),
                        cornerRadius = 10.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(entry.titleRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(record.kind.labelRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ArcadeBadge(text = record.display(numberFormat))
                }
            }
        }
    }
}

@Composable
private fun ArcadeSectionHeader(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun ArcadeScoreboard(
    stats: GameStatsSnapshot,
    numberFormat: NumberFormat,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(AppChrome.groupCorner)
    val hasData = stats.totalSessionsStarted > 0
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                onClickLabel = stringResource(R.string.games_dashboard_action),
                role = Role.Button,
                onClick = onClick,
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.games_arcade_scoreboard),
                        style = arcadeLabelStyle,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(
                            if (hasData) R.string.games_arcade_view_records else R.string.games_arcade_press_start
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScoreboardStat(
                    value = numberFormat.format(stats.totalSessionsStarted),
                    label = stringResource(R.string.games_dashboard_total_played),
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ScoreboardStat(
                    value = favoriteGameTitle(stats),
                    label = stringResource(R.string.games_dashboard_favorite_game),
                    modifier = Modifier.weight(1f),
                    compactValue = true,
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ScoreboardStat(
                    value = "${numberFormat.format(stats.gamesTried)}/${numberFormat.format(GameId.entries.size)}",
                    label = stringResource(R.string.games_dashboard_games_tried),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun favoriteGameTitle(stats: GameStatsSnapshot): String =
    stats.favoriteGame
        ?.let { catalogById.getValue(it).titleRes }
        ?.let { stringResource(it) }
        ?: stringResource(R.string.games_dashboard_no_favorite)

@Composable
private fun ScoreboardStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    compactValue: Boolean = false,
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (compactValue) {
            val scoreLineHeight = with(LocalDensity.current) { arcadeScoreStyle.lineHeight.toDp() }
            Box(
                modifier = Modifier.heightIn(min = scoreLineHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Text(
                text = value,
                style = arcadeScoreStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecentGamesPager(
    games: List<GameData>,
    records: Map<GameId, GameRecord>,
    numberFormat: NumberFormat,
    pagerState: PagerState,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val pageWidth = (maxWidth * 0.88f).coerceIn(minOf(240.dp, maxWidth), 440.dp)
        val screenSize = (pageWidth * 0.36f).coerceIn(84.dp, 136.dp)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalPager(
                modifier = Modifier.fillMaxWidth(),
                state = pagerState,
                pageSize = PageSize.Fixed(pageWidth),
                pageSpacing = 12.dp,
            ) { page ->
                val game = games[page]
                FeaturedCabinet(
                    game = game,
                    record = records.getValue(game.id),
                    numberFormat = numberFormat,
                    screenSize = screenSize,
                    modifier = Modifier.graphicsLayer {
                        val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val proximity = (1f - offset.absoluteValue).coerceIn(0f, 1f)
                        val pageScale = 0.94f + 0.06f * proximity
                        scaleX = pageScale
                        scaleY = pageScale
                        alpha = 0.72f + 0.28f * proximity
                    },
                )
            }
            if (games.size > 1) {
                PagerLevelIndicator(
                    count = games.size,
                    selected = pagerState.currentPage,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun PagerLevelIndicator(count: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(width = if (index == selected) 18.dp else 8.dp, height = 8.dp)
                    .background(
                        color = if (index == selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape,
                    )
            )
        }
    }
}

@Composable
private fun FeaturedCabinet(
    game: GameData,
    record: GameRecord,
    numberFormat: NumberFormat,
    screenSize: Dp,
    modifier: Modifier = Modifier,
) {
    CabinetFrame(
        game = game,
        modifier = modifier,
    ) {
        CabinetMarquee(title = game.title, minLines = 1)
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CrtScreen(
                iconRes = game.iconRes,
                modifier = Modifier.size(screenSize),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RecordReadout(
                    record = record,
                    numberFormat = numberFormat,
                    valueStyle = arcadeScoreStyle,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.games_play_now),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArcadeCabinet(
    game: GameData,
    record: GameRecord,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier,
) {
    CabinetFrame(
        game = game,
        modifier = modifier.fillMaxWidth(),
    ) {
        CabinetMarquee(title = game.title, minLines = 2)
        CrtScreen(
            iconRes = game.iconRes,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth(),
        )
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                RecordReadout(
                    record = record,
                    numberFormat = numberFormat,
                    valueStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                )
            }
        }
    }
}

@Composable
private fun CabinetFrame(
    game: GameData,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(AppChrome.groupCorner)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cabinet_scale"
    )
    Surface(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClickLabel = stringResource(R.string.play_label),
                role = Role.Button,
                onClick = game.onClick,
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column { content() }
    }
}

@Composable
private fun CabinetMarquee(title: String, minLines: Int) {
    val locale = LocalConfiguration.current.locales[0]
    val style = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.6.sp,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        val reservedHeight = with(LocalDensity.current) { style.lineHeight.toDp() } * minLines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .heightIn(min = reservedHeight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.uppercase(locale),
                style = style,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { contentDescription = title },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun CrtScreen(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
) {
    val outer = RoundedCornerShape(cornerRadius)
    val inner = RoundedCornerShape((cornerRadius - 4.dp).coerceAtLeast(4.dp))
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(outer)
            .background(Color.Black)
            .padding(4.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(inner)
                .crtScanlines()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(inner)
                .background(
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.14f),
                        0.45f to Color.Transparent,
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    )
                )
        )
    }
}

@Composable
private fun RecordReadout(
    record: GameRecord,
    numberFormat: NumberFormat,
    valueStyle: TextStyle,
) {
    Text(
        text = stringResource(record.kind.labelRes),
        style = arcadeLabelStyle,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    if (record.value > 0) {
        Text(
            text = record.display(numberFormat),
            style = valueStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    } else {
        Text(
            text = stringResource(R.string.games_record_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


@Composable
private fun ArcadeBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueStyle: TextStyle,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 88.dp)
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = arcadeLabelStyle,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = value,
                style = valueStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Immutable
private data class GameFunFact(
    val gameId: GameId,
    val year: String,
    @param:StringRes val factRes: Int,
)

@Immutable
private data class DashboardEntry(
    val label: String,
    val value: String,
    val compactValue: Boolean = false,
)

private val gameFunFacts = listOf(
    GameFunFact(GameId.FUN_BIRD, "2013", R.string.game_fact_fun_bird),
    GameFunFact(GameId.SNAKE, "1997", R.string.game_fact_snake),
    GameFunFact(GameId.BREAKOUT, "1976", R.string.game_fact_breakout),
    GameFunFact(GameId.MEMORY, "1959", R.string.game_fact_memory),
    GameFunFact(GameId.MINESWEEPER, "1990", R.string.game_fact_minesweeper),
    GameFunFact(GameId.TIC_TAC_TOE, "1952", R.string.game_fact_tictactoe),
    GameFunFact(GameId.PUZZLE, "1874", R.string.game_fact_puzzle),
    GameFunFact(GameId.HANGMAN, "1894", R.string.game_fact_hangman),
    GameFunFact(GameId.GAME_2048, "2014", R.string.game_fact_2048),
    GameFunFact(GameId.SUDOKU, "1979", R.string.game_fact_sudoku),
)

private enum class GamesSheet {
    FunFacts,
    Dashboard
}

private fun GameStatsSnapshot.recordFor(gameId: GameId): GameRecord {
    val kind = gameRecordKinds.getValue(gameId)
    return GameRecord(kind = kind, value = games[gameId]?.recordValue(kind) ?: 0)
}

@get:StringRes
private val GameRecordKind.labelRes: Int
    get() = when (this) {
        GameRecordKind.HIGH_SCORE -> R.string.games_record_high_score
        GameRecordKind.LOW_TIME -> R.string.games_record_best_time
        GameRecordKind.LOW_MOVES -> R.string.games_record_fewest_moves
        GameRecordKind.WIN_COUNT -> R.string.games_record_wins
    }

private fun GameRecord.display(numberFormat: NumberFormat): String =
    if (kind == GameRecordKind.LOW_TIME) formatDuration(value) else numberFormat.format(value)

private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "00:00"
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSeconds)
}

package com.bookverse.contacts.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameRecordKind
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.domain.games.GameStatsSnapshot
import com.bookverse.contacts.domain.games.gameRecordKinds
import com.bookverse.contacts.feature.games.R
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed
import com.bookverse.contacts.ui.components.ArcadeCabinet
import com.bookverse.contacts.ui.components.ArcadeControlsEffect
import com.bookverse.contacts.ui.components.ArcadeDialogHeader
import com.bookverse.contacts.ui.components.ArcadeDirection
import com.bookverse.contacts.ui.components.ArcadeHostState
import com.bookverse.contacts.ui.components.LocalArcadeHost
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.arcadeLabelStyle
import com.bookverse.contacts.ui.components.arcadeScoreStyle
import com.bookverse.contacts.ui.components.crtScanlines
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold

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
    appSettingsViewModel: AppSettingsViewModel,
    gameStatsRepository: GameStatsRepository,
    snakeViewModelFactory: ViewModelProvider.Factory,
    breakoutViewModelFactory: ViewModelProvider.Factory,
) {
    var visibleSheet by remember { mutableStateOf<GamesSheet?>(null) }
    var activeGame by rememberSaveable { mutableStateOf<GameId?>(null) }
    var positionedOnRecent by rememberSaveable { mutableStateOf(false) }
    val host = remember { ArcadeHostState() }
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    val numberFormat = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val animationDuration = LocalAnimationSpeed.current.durationMs
    val stats by gameStatsRepository.stats.collectAsStateWithLifecycle(
        initialValue = GameStatsSnapshot(),
    )
    val titles = remember(resources) {
        gameCatalog.associate { entry -> entry.id to resources.getString(entry.titleRes) }
    }
    val records = remember(stats) {
        GameId.entries.associateWith { gameId -> stats.recordFor(gameId) }
    }
    val pagerState = rememberPagerState(initialPage = 0) { gameCatalog.size }
    val mostRecent = remember(stats) {
        stats.games.entries
            .filter { it.value.lastStartedAt > 0L }
            .maxByOrNull { it.value.lastStartedAt }
            ?.key
    }

    LaunchedEffect(mostRecent) {
        if (!positionedOnRecent && mostRecent != null) {
            positionedOnRecent = true
            pagerState.scrollToPage(gameCatalog.indexOfFirst { it.id == mostRecent })
        }
    }

    BackHandler {
        if (activeGame != null) activeGame = null else onBackClick()
    }

    Scaffold(
        topBar = {
            val chrome = host.chrome
            SecondaryTopBar(
                title = chrome?.title ?: stringResource(CoreUiR.string.games_title),
                onBackClick = { chrome?.navigateBack() ?: onBackClick() },
                actions = {
                    if (chrome != null) {
                        chrome.actions?.invoke(this)
                    } else {
                        IconAction(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = stringResource(R.string.games_dashboard_action),
                            onClick = { visibleSheet = GamesSheet.Dashboard }
                        )
                    }
                },
                count = if (chrome == null) gameCatalog.size else null,
                onCountClick = if (chrome == null) {
                    { visibleSheet = GamesSheet.FunFacts }
                } else {
                    null
                },
            )
        }
    ) { paddingValues ->
        CompositionLocalProvider(LocalArcadeHost provides host) {
            val currentGame = activeGame
            ArcadeCabinet(
                marquee = currentGame?.let(titles::getValue) ?: stringResource(R.string.games_arcade_marquee),
                controls = host.controls,
                secondaryLabel = stringResource(
                    if (currentGame != null) R.string.games_arcade_exit else R.string.games_arcade_records
                ),
                onSecondary = {
                    if (activeGame != null) {
                        backDispatcher?.onBackPressed() ?: run { activeGame = null }
                    } else {
                        visibleSheet = GamesSheet.Dashboard
                    }
                },
                modifier = Modifier.padding(paddingValues),
            ) {
                AnimatedContent(
                    targetState = currentGame,
                    transitionSpec = {
                        (fadeIn(tween(animationDuration)) + scaleIn(tween(animationDuration), initialScale = 0.9f))
                            .togetherWith(fadeOut(tween(animationDuration / 2)) + scaleOut(tween(animationDuration / 2), targetScale = 1.04f))
                    },
                    label = "arcade_screen",
                ) { game ->
                    if (game == null) {
                        ArcadeSelector(
                            titles = titles,
                            records = records,
                            numberFormat = numberFormat,
                            pagerState = pagerState,
                            onPlay = { gameId -> activeGame = gameId },
                        )
                    } else {
                        ArcadeGame(
                            gameId = game,
                            onExit = { activeGame = null },
                            appSettingsViewModel = appSettingsViewModel,
                            gameStatsRepository = gameStatsRepository,
                            snakeViewModelFactory = snakeViewModelFactory,
                            breakoutViewModelFactory = breakoutViewModelFactory,
                        )
                    }
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

@Composable
private fun ArcadeGame(
    gameId: GameId,
    onExit: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    gameStatsRepository: GameStatsRepository,
    snakeViewModelFactory: ViewModelProvider.Factory,
    breakoutViewModelFactory: ViewModelProvider.Factory,
) {
    when (gameId) {
        GameId.FUN_BIRD -> FunBirdScreen(onExit, gameStatsRepository)
        GameId.SNAKE -> SnakeScreen(onExit, appSettingsViewModel, snakeViewModelFactory)
        GameId.MINESWEEPER -> MinesweeperScreen(onExit, gameStatsRepository)
        GameId.GAME_2048 -> Game2048Screen(onExit, gameStatsRepository)
        GameId.PUZZLE -> PuzzleScreen(onExit, gameStatsRepository)
        GameId.TIC_TAC_TOE -> TicTacToeScreen(onExit, gameStatsRepository)
        GameId.HANGMAN -> HangmanScreen(onExit, appSettingsViewModel, gameStatsRepository)
        GameId.BREAKOUT -> BreakoutScreen(onExit, appSettingsViewModel, breakoutViewModelFactory)
        GameId.SUDOKU -> SudokuScreen(onExit, gameStatsRepository)
        GameId.MEMORY -> MemoryGameScreen(onExit, appSettingsViewModel, gameStatsRepository)
    }
}

@Composable
private fun ArcadeSelector(
    titles: Map<GameId, String>,
    records: Map<GameId, GameRecord>,
    numberFormat: NumberFormat,
    pagerState: PagerState,
    onPlay: (GameId) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val count = gameCatalog.size
    fun step(delta: Int) {
        val target = (pagerState.currentPage + delta + count) % count
        scope.launch { pagerState.animateScrollToPage(target) }
    }
    ArcadeControlsEffect(
        primaryLabel = stringResource(R.string.play_label),
        onPrimaryPress = { onPlay(gameCatalog[pagerState.currentPage].id) },
        onDirection = { direction ->
            when (direction) {
                ArcadeDirection.LEFT, ArcadeDirection.UP -> step(-1)
                ArcadeDirection.RIGHT, ArcadeDirection.DOWN -> step(1)
                null -> Unit
            }
        },
    )
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxHeight < 320.dp
        val artSize = minOf(maxWidth * 0.62f, maxHeight * if (compact) 0.5f else 0.46f)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = if (compact) 6.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.games_arcade_select),
                    style = arcadeLabelStyle,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                Text(
                    text = stringResource(
                        R.string.games_arcade_position,
                        numberFormat.format(pagerState.currentPage + 1),
                        numberFormat.format(count),
                    ),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                val entry = gameCatalog[page]
                SelectorPage(
                    title = titles.getValue(entry.id),
                    iconRes = entry.iconRes,
                    record = records.getValue(entry.id),
                    numberFormat = numberFormat,
                    artSize = artSize,
                    compact = compact,
                    onPlay = { onPlay(entry.id) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { step(-1) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.games_arcade_previous),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                PagerLevelIndicator(
                    count = count,
                    selected = pagerState.currentPage,
                )
                IconButton(onClick = { step(1) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.games_arcade_next),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectorPage(
    title: String,
    @DrawableRes iconRes: Int,
    record: GameRecord,
    numberFormat: NumberFormat,
    artSize: Dp,
    compact: Boolean,
    onPlay: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val playLabel = stringResource(R.string.play_label)
    if (compact) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CrtScreen(
                iconRes = iconRes,
                modifier = Modifier
                    .size(artSize)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClickLabel = playLabel, role = Role.Button, onClick = onPlay),
            )
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SelectorTitle(title = title, locale = locale, textAlign = TextAlign.Start)
                RecordReadout(record = record, numberFormat = numberFormat, valueStyle = arcadeScoreStyle)
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            CrtScreen(
                iconRes = iconRes,
                modifier = Modifier
                    .size(artSize)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClickLabel = playLabel, role = Role.Button, onClick = onPlay),
            )
            SelectorTitle(title = title, locale = locale, textAlign = TextAlign.Center)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RecordReadout(record = record, numberFormat = numberFormat, valueStyle = arcadeScoreStyle)
            }
        }
    }
}

@Composable
private fun SelectorTitle(title: String, locale: Locale, textAlign: TextAlign) {
    Text(
        text = title.uppercase(locale),
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
        ),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = textAlign,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.semantics { contentDescription = title },
    )
}

@Composable
private fun PagerLevelIndicator(count: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(width = if (index == selected) 16.dp else 7.dp, height = 7.dp)
                    .background(
                        color = if (index == selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = RoundedCornerShape(2.dp),
                    )
            )
        }
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
        eyebrow = stringResource(R.string.games_arcade_records),
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

    val favoriteTitle = stats.favoriteGame
        ?.let { catalogById.getValue(it).titleRes }
        ?.let { stringResource(it) }
        ?: stringResource(R.string.games_dashboard_no_favorite)
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
            stringResource(R.string.games_dashboard_best_streak),
            numberFormat.format(stats.bestWinStreak),
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
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
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
            color = MaterialTheme.colorScheme.onBackground,
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

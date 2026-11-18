package com.bookverse.contacts.ui.screens.games

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.feature.games.R
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.domain.games.GameStatsSnapshot
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.RetroGameInstructionsDialog
import com.bookverse.contacts.ui.components.ScoreCard
import com.bookverse.contacts.ui.components.ScoreCardEntry
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.retroGameDialogBlur
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RotateCcw
import kotlin.math.abs
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun Game2048Screen(
    onBackClick: () -> Unit,
    gameStatsRepository: GameStatsRepository,
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val gameSession = rememberGameSessionRecorder(GameId.GAME_2048, gameStatsRepository)
    val stats by gameStatsRepository.stats.collectAsStateWithLifecycle(
        initialValue = GameStatsSnapshot(),
    )

    val size = 5
    var showInstructions by remember { mutableStateOf(true) }
    val blurRadius = retroGameDialogBlur(showInstructions)
    var gameOver by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(0) }
    var values by remember { mutableStateOf(Array(size) { IntArray(size) }) }
    var ids by remember { mutableStateOf(Array(size) { LongArray(size) }) }
    var nextId by remember { mutableLongStateOf(1L) }

    fun empties(): List<Pair<Int, Int>> {
        val list = ArrayList<Pair<Int, Int>>()
        for (i in 0 until size) for (j in 0 until size) if (values[i][j] == 0) list += i to j
        return list
    }

    fun addRandomTile() {
        val e = empties()
        if (e.isEmpty()) return
        val (i, j) = e.random()
        values = values.map { it.clone() }.toTypedArray()
            .also { v -> v[i][j] = if (Random.nextFloat() < 0.9f) 2 else 4 }
        ids = ids.map { it.clone() }.toTypedArray().also { a -> a[i][j] = nextId++ }
    }

    fun startNew(recordSession: Boolean = true) {
        score = 0
        values = Array(size) { IntArray(size) }
        ids = Array(size) { LongArray(size) }
        nextId = 1L
        addRandomTile()
        addRandomTile()
        gameOver = false
        if (recordSession) gameSession.start()
    }

    data class Line(val v: IntArray, val id: LongArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Line

            if (!v.contentEquals(other.v)) return false
            if (!id.contentEquals(other.id)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = v.contentHashCode()
            result = 31 * result + id.contentHashCode()
            return result
        }
    }

    fun mergeLeft(line: Line): Triple<IntArray, LongArray, Int> {
        val v = line.v
        val a = line.id
        val nonZero = ArrayList<Pair<Int, Long>>()
        for (k in v.indices) if (v[k] != 0) nonZero += v[k] to a[k]
        val resV = IntArray(size)
        val resA = LongArray(size)
        var i = 0
        var idx = 0
        var gain = 0
        while (idx < nonZero.size) {
            val (cv, cid) = nonZero[idx]
            if (idx + 1 < nonZero.size && nonZero[idx + 1].first == cv) {
                val nv = cv * 2
                resV[i] = nv
                resA[i] = cid
                gain += nv
                idx += 2
            } else {
                resV[i] = cv
                resA[i] = cid
                idx += 1
            }
            i++
        }
        return Triple(resV, resA, gain)
    }

    fun movesAvailable(): Boolean {
        if (empties().isNotEmpty()) return true
        for (i in 0 until size) for (j in 0 until size) {
            val v = values[i][j]
            if (i + 1 < size && values[i + 1][j] == v) return true
            if (j + 1 < size && values[i][j + 1] == v) return true
        }
        return false
    }

    fun move(dir: Dir): Boolean {
        var moved = false
        var gained = 0
        val newV = Array(size) { IntArray(size) }
        val newA = Array(size) { LongArray(size) }
        when (dir) {
            Dir.LEFT -> {
                for (i in 0 until size) {
                    val l = Line(IntArray(size) { values[i][it] }, LongArray(size) { ids[i][it] })
                    val (nv, na, g) = mergeLeft(l)
                    newV[i] = nv
                    newA[i] = na
                    if (!moved && !nv.contentEquals(values[i])) moved = true
                    gained += g
                }
            }

            Dir.RIGHT -> {
                for (i in 0 until size) {
                    val rv = IntArray(size) { values[i][size - 1 - it] }
                    val ra = LongArray(size) { ids[i][size - 1 - it] }
                    val (nv, na, g) = mergeLeft(Line(rv, ra))
                    val rowV = IntArray(size) { nv[size - 1 - it] }
                    val rowA = LongArray(size) { na[size - 1 - it] }
                    newV[i] = rowV
                    newA[i] = rowA
                    if (!moved && !rowV.contentEquals(values[i])) moved = true
                    gained += g
                }
            }

            Dir.UP -> {
                for (j in 0 until size) {
                    val cv = IntArray(size) { values[it][j] }
                    val ca = LongArray(size) { ids[it][j] }
                    val (nv, na, g) = mergeLeft(Line(cv, ca))
                    for (i in 0 until size) {
                        newV[i][j] = nv[i]
                        newA[i][j] = na[i]
                    }
                    if (!moved) {
                        var diff = false
                        for (i in 0 until size) if (values[i][j] != nv[i]) {
                            diff = true; break
                        }
                        if (diff) moved = true
                    }
                    gained += g
                }
            }

            Dir.DOWN -> {
                for (j in 0 until size) {
                    val cv = IntArray(size) { values[size - 1 - it][j] }
                    val ca = LongArray(size) { ids[size - 1 - it][j] }
                    val (nv, na, g) = mergeLeft(Line(cv, ca))
                    for (i in 0 until size) {
                        newV[size - 1 - i][j] = nv[i]
                        newA[size - 1 - i][j] = na[i]
                    }
                    if (!moved) {
                        var diff = false
                        for (i in 0 until size) if (values[i][j] != newV[i][j]) {
                            diff = true; break
                        }
                        if (diff) moved = true
                    }
                    gained += g
                }
            }
        }
        if (moved) {
            values = newV
            ids = newA
            score += gained
            if (score > highScore) {
                highScore = score
            }
            addRandomTile()
            if (!movesAvailable()) gameOver = true
        }
        return moved
    }

    LaunchedEffect(Unit) { startNew(recordSession = false) }
    LaunchedEffect(stats) {
        highScore = maxOf(highScore, stats.games[GameId.GAME_2048]?.bestScore ?: 0)
    }
    LaunchedEffect(gameOver) {
        if (gameOver) gameSession.complete(GameCompletion(score = score))
    }
    BackHandler { onBackClick() }

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.game_2048_title),
                onBackClick = onBackClick,
                actions = {
                    IconAction(
                        imageVector = Lucide.RotateCcw,
                        contentDescription = stringResource(R.string.cd_restart_game),
                        onClick = { startNew() }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
                .blur(blurRadius),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ScoreCard(
                entries = listOf(
                    ScoreCardEntry(
                        title = stringResource(R.string.score_label),
                        value = score.toString()
                    ),
                    ScoreCardEntry(
                        title = stringResource(R.string.best_score_label),
                        value = highScore.toString()
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = textColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    val cellSpacing = 8.dp
                    val gridPadding = 8.dp
                    val tileSize: Dp =
                        (maxWidth - gridPadding * 2 - cellSpacing * (size - 1)) / size
                    val animSpec =
                        tween<Dp>(durationMillis = 140, easing = LinearOutSlowInEasing)
                    val density = LocalDensity.current
                    val threshold = with(density) { 24.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(gridPadding)
                            .pointerInput(values) {
                                var dx = 0f
                                var dy = 0f
                                detectDragGestures(
                                    onDragEnd = {
                                        if (abs(dx) > abs(dy) && abs(dx) > threshold) {
                                            if (dx > 0) move(Dir.RIGHT) else move(Dir.LEFT)
                                        } else if (abs(dy) > threshold) {
                                            if (dy > 0) move(Dir.DOWN) else move(Dir.UP)
                                        }
                                        dx = 0f
                                        dy = 0f
                                    },
                                    onDrag = { _, dragAmount ->
                                        dx += dragAmount.x
                                        dy += dragAmount.y
                                    }
                                )
                            }
                    ) {
                        val tileShape = RoundedCornerShape(12.dp)

                        Column(
                            modifier = Modifier.matchParentSize(),
                            verticalArrangement = Arrangement.spacedBy(cellSpacing),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            repeat(size) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(size) {
                                        Box(
                                            modifier = Modifier
                                                .size(tileSize)
                                                .clip(tileShape)
                                                .background(Color(0xFFCDC1B4).copy(alpha = 0.8f))
                                        )
                                    }
                                }
                            }
                        }

                        val tiles = remember(values, ids) {
                            buildList {
                                for (i in 0 until size) for (j in 0 until size) {
                                    val v = values[i][j]
                                    val id = ids[i][j]
                                    if (v != 0 && id != 0L) add(TileItem(id, v, i, j))
                                }
                            }
                        }

                        tiles.forEach { t ->
                            key(t.id) {
                                val xTarget = gridPadding + (tileSize + cellSpacing) * t.col
                                val yTarget = gridPadding + (tileSize + cellSpacing) * t.row
                                val animX by androidx.compose.animation.core.animateDpAsState(
                                    xTarget,
                                    animSpec,
                                    label = "x"
                                )
                                val animY by androidx.compose.animation.core.animateDpAsState(
                                    yTarget,
                                    animSpec,
                                    label = "y"
                                )
                                    Box(
                                        modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                x = (animX - gridPadding).roundToPx(),
                                                y = (animY - gridPadding).roundToPx(),
                                            )
                                        }
                                        .size(tileSize)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(tileBgColor(t.value)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val base = when {
                                        t.value < 128 -> tileSize.value * 0.42f
                                        t.value < 1024 -> tileSize.value * 0.36f
                                        else -> tileSize.value * 0.30f
                                    }
                                    Text(
                                        text = t.value.toString(),
                                        color = tileTextColor(t.value),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = base.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showInstructions) {
        RetroGameInstructionsDialog(
            title = stringResource(R.string.instructions_title),
            instructions = stringResource(R.string.game_2048_instructions_text),
            onPlayClick = {
                showInstructions = false
                gameSession.start()
            },
            onDismiss = { showInstructions = false; onBackClick() },
        )
    }

    if (gameOver) {
        MessageDialog(
            title = stringResource(R.string.lose_title),
            message1 = stringResource(R.string.game_over_message, score),
            buttonText = stringResource(R.string.restart_game),
            onDismiss = { onBackClick() },
            onButtonClick = { showInstructions = false; startNew() },
        )
    }
}

private enum class Dir { UP, DOWN, LEFT, RIGHT }

private data class TileItem(val id: Long, val value: Int, val row: Int, val col: Int)

private fun tileBgColor(value: Int): Color = when (value) {
    2 -> Color(0xFFEEE4DA)
    4 -> Color(0xFFEDE0C8)
    8 -> Color(0xFFF2B179)
    16 -> Color(0xFFF59563)
    32 -> Color(0xFFF67C5F)
    64 -> Color(0xFFF65E3B)
    128 -> Color(0xFFEDCF72)
    256 -> Color(0xFFEDCC61)
    512 -> Color(0xFFEDC850)
    1024 -> Color(0xFFEDC53F)
    2048 -> Color(0xFFEDC22E)
    else -> Color(0xFF3C3A32)
}

private fun tileTextColor(value: Int): Color = when (value) {
    2, 4 -> Color(0xFF776E65)
    else -> Color.White
}

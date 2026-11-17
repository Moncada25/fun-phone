package com.bookverse.contacts.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class ScrollSnapshot(
    val listFirstIndex: Int,
    val listOffset: Int,
    val gridFirstIndex: Int,
    val gridOffset: Int,
    val containerHeight: Float,
)

@Composable
fun FastScroller(
    listState: LazyListState? = null,
    gridState: LazyGridState? = null,
    sections: List<Pair<String, Int>>,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    accentColor: Color,
    isDate: Boolean = false
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    var isDragging by remember { mutableStateOf(false) }
    var containerHeight by remember { mutableFloatStateOf(0f) }
    var showBubble by remember { mutableStateOf(false) }
    var currentSection by remember { mutableStateOf<String?>(null) }
    var thumbTarget by remember { mutableFloatStateOf(0f) }
    val thumbY by animateFloatAsState(
        targetValue = thumbTarget,
        animationSpec = if (isDragging) tween(durationMillis = 0) else tween(durationMillis = 120),
        label = "fastScrollerThumb"
    )
    var initialPositionSet by remember { mutableStateOf(false) }
    var showScrollbar by remember { mutableStateOf(false) }
    var lastScrollTime by remember { mutableLongStateOf(0L) }
    var cachedVisibleCount by remember { mutableIntStateOf(1) }
    var cachedAverageItemSize by remember { mutableFloatStateOf(1f) }
    val thumbHeight = 60.dp
    val thumbHeightPx = with(LocalDensity.current) { thumbHeight.toPx() }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(isDragging) {
        if (!isDragging && showBubble) {
            delay(500)
            showBubble = false
        }
    }
    LaunchedEffect(lastScrollTime) {
        if (showScrollbar && lastScrollTime > 0) {
            delay(500)
            if (!isDragging) {
                showScrollbar = false
            }
        }
    }
    fun updateLayoutCache() {
        listState?.layoutInfo?.visibleItemsInfo?.let { visible ->
            if (visible.isNotEmpty()) {
                cachedVisibleCount = visible.size
                cachedAverageItemSize =
                    visible.map { it.size }.average().toFloat().coerceAtLeast(1f)
            }
        }
        gridState?.layoutInfo?.visibleItemsInfo?.let { visible ->
            if (visible.isNotEmpty()) {
                cachedVisibleCount = visible.size
                cachedAverageItemSize =
                    visible.map { it.size.height }.average().toFloat().coerceAtLeast(1f)
            }
        }
    }

    fun maxScrollableIndex(): Int {
        val total = listState?.layoutInfo?.totalItemsCount
            ?: gridState?.layoutInfo?.totalItemsCount
            ?: 0
        return (total - cachedVisibleCount).coerceAtLeast(0)
    }

    fun calculateScrollProgress(): Float {
        updateLayoutCache()
        val maxIndex = maxScrollableIndex()
        if (maxIndex == 0) return 0f

        listState?.let { state ->
            val firstItemSize =
                state.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat()
                    ?: cachedAverageItemSize
            val fractional = state.firstVisibleItemIndex +
                (state.firstVisibleItemScrollOffset / firstItemSize).coerceIn(0f, 1f)
            return (fractional / maxIndex).coerceIn(0f, 1f)
        }

        gridState?.let { state ->
            val firstItemHeight =
                state.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height?.toFloat()
                    ?: cachedAverageItemSize
            val fractional = state.firstVisibleItemIndex +
                (state.firstVisibleItemScrollOffset / firstItemHeight).coerceIn(0f, 1f)
            return (fractional / maxIndex).coerceIn(0f, 1f)
        }

        return 0f
    }

    LaunchedEffect(listState, gridState) {
        snapshotFlow {
            ScrollSnapshot(
                listState?.firstVisibleItemIndex ?: 0,
                listState?.firstVisibleItemScrollOffset ?: 0,
                gridState?.firstVisibleItemIndex ?: 0,
                gridState?.firstVisibleItemScrollOffset ?: 0,
                containerHeight,
            )
        }.collect {
            if (it.containerHeight > 0 && !isDragging) {
                showScrollbar = true
                lastScrollTime = System.currentTimeMillis()
                val maxThumbTravel = (it.containerHeight - thumbHeightPx).coerceAtLeast(0f)
                val scrollProgress = calculateScrollProgress()
                thumbTarget = scrollProgress * maxThumbTravel
                initialPositionSet = true
            }
        }
    }

    fun updateFromThumbPosition(y: Float) {
        if (containerHeight <= 0 || sections.isEmpty()) return
        showScrollbar = true
        lastScrollTime = System.currentTimeMillis()
        updateLayoutCache()
        val maxThumbTravel = (containerHeight - thumbHeightPx).coerceAtLeast(0f)
        val scrollProgress = if (maxThumbTravel == 0f) {
            0f
        } else {
            (y / maxThumbTravel).coerceIn(0f, 1f)
        }
        val maxIndex = maxScrollableIndex()
        val exactPosition = scrollProgress * maxIndex
        val targetIndex = exactPosition.toInt().coerceIn(0, maxIndex)
        val fraction = exactPosition - targetIndex
        val scrollOffset = (fraction * cachedAverageItemSize).roundToInt()
        val (section) = sections
            .lastOrNull { it.second <= targetIndex }
            ?: sections.first()
        currentSection = section
        showBubble = true
        listState?.let { state ->
            if (targetIndex < state.layoutInfo.totalItemsCount) {
                coroutineScope.launch {
                    state.scrollToItem(targetIndex, scrollOffset)
                }
            }
        }
        gridState?.let { state ->
            if (targetIndex < state.layoutInfo.totalItemsCount) {
                coroutineScope.launch {
                    state.scrollToItem(targetIndex, scrollOffset)
                }
            }
        }
    }
    Box(modifier = modifier.fillMaxHeight()) {
        AnimatedVisibility(
            visible = showBubble && currentSection != null,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(end = if (isDate) 72.dp else 42.dp)
        ) {
            val textMeasurer = rememberTextMeasurer()
            val measuredWidth = with(LocalDensity.current) {
                textMeasurer.measure(
                    text = AnnotatedString(currentSection ?: ""),
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                ).size.width.toDp() + 40.dp
            }
            val boxWidth = if (isDate) measuredWidth else 50.dp
            Box(
                modifier = Modifier
                    .size(
                        width = boxWidth,
                        height = 50.dp
                    )
                    .clip(if (isDate) RoundedCornerShape(12.dp) else CircleShape)
                    .background(accentColor)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentSection ?: "",
                    fontSize = if (isDate) 14.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = backgroundColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .alpha(0f)
                .align(Alignment.CenterEnd)
                .onGloballyPositioned { coordinates ->
                    containerHeight = coordinates.size.height.toFloat()
                }
        )
        AnimatedVisibility(
            visible = showScrollbar,
            enter = slideInHorizontally(
                initialOffsetX = { it / 2 },
                animationSpec = tween(durationMillis = 500)
            ) + fadeIn(animationSpec = tween(durationMillis = 500)),
            exit = slideOutHorizontally(
                targetOffsetX = { it / 2 },
                animationSpec = tween(durationMillis = 500)
            ) + fadeOut(animationSpec = tween(durationMillis = 500)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .alpha(0.2f)
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .background(textColor, RoundedCornerShape(1.dp))
            )
            if (containerHeight > 0) {
                val maxThumbTravel = (containerHeight - thumbHeightPx).coerceAtLeast(0f)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, thumbY.roundToInt()) }
                        .padding(end = 2.dp)
                        .height(thumbHeight)
                        .width(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .alpha(if (isDragging) 1f else 0.6f)
                        .background(if (isDragging) accentColor else textColor.copy(alpha = 0.7f))
                        .pointerInput(sections, containerHeight) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    showScrollbar = true
                                    thumbTarget = thumbY
                                    if (!initialPositionSet) {
                                        val newY = offset.y - (thumbHeightPx / 2)
                                        thumbTarget = newY.coerceIn(0f, maxThumbTravel)
                                        initialPositionSet = true
                                    }
                                    updateFromThumbPosition(thumbTarget)
                                },
                                onDragEnd = {
                                    isDragging = false
                                },
                                onDragCancel = {
                                    isDragging = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newY = (thumbTarget + dragAmount.y).coerceIn(
                                        0f,
                                        maxThumbTravel
                                    )
                                    thumbTarget = newY
                                    updateFromThumbPosition(newY)
                                }
                            )
                        }
                )
            }
        }
    }
}

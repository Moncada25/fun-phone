package com.bookverse.contacts.domain.games

import kotlin.random.Random

const val SNAKE_BOARD_WIDTH = 15
const val SNAKE_BOARD_HEIGHT = 15
const val SNAKE_BASE_SPEED_MS = 150
const val SNAKE_MIN_SPEED_MS = 50

data class SnakePoint(val x: Int, val y: Int)

enum class SnakeDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    ;

    fun isOpposite(other: SnakeDirection): Boolean = when (this) {
        UP -> other == DOWN
        DOWN -> other == UP
        LEFT -> other == RIGHT
        RIGHT -> other == LEFT
    }
}

enum class SnakeBoundaryMode {
    WALLS,
    WRAP,
}

enum class SnakePhase {
    READY,
    RUNNING,
    PAUSED,
    GAME_OVER,
    WON,
}

data class SnakeGameState(
    val segments: List<SnakePoint>,
    val direction: SnakeDirection,
    val queuedDirection: SnakeDirection? = null,
    val food: SnakePoint?,
    val phase: SnakePhase,
    val score: Int = 0,
    val highScore: Int = 0,
    val speedMs: Int = SNAKE_BASE_SPEED_MS,
    val boundaryMode: SnakeBoundaryMode = SnakeBoundaryMode.WALLS,
)

fun interface SnakeFoodPicker {
    fun pick(available: List<SnakePoint>): SnakePoint
}

class RandomSnakeFoodPicker(
    private val random: Random = Random.Default,
) : SnakeFoodPicker {
    override fun pick(available: List<SnakePoint>): SnakePoint = available.random(random)
}

class SnakeGameEngine(
    private val width: Int = SNAKE_BOARD_WIDTH,
    private val height: Int = SNAKE_BOARD_HEIGHT,
    private val foodPicker: SnakeFoodPicker = RandomSnakeFoodPicker(),
) {
    init {
        require(width >= 3) { "Snake board width must be at least 3" }
        require(height >= 1) { "Snake board height must be positive" }
    }

    fun ready(
        boundaryMode: SnakeBoundaryMode = SnakeBoundaryMode.WALLS,
        highScore: Int = 0,
    ): SnakeGameState {
        val segments = initialSegments()
        return SnakeGameState(
            segments = segments,
            direction = SnakeDirection.RIGHT,
            food = findFood(segments),
            phase = SnakePhase.READY,
            highScore = highScore.coerceAtLeast(0),
            boundaryMode = boundaryMode,
        )
    }

    fun start(boundaryMode: SnakeBoundaryMode, highScore: Int): SnakeGameState =
        ready(boundaryMode, highScore).copy(phase = SnakePhase.RUNNING)

    fun queueDirection(
        state: SnakeGameState,
        requested: SnakeDirection,
    ): SnakeGameState {
        if (state.phase != SnakePhase.RUNNING || state.queuedDirection != null) return state
        if (requested == state.direction || requested.isOpposite(state.direction)) return state
        return state.copy(queuedDirection = requested)
    }

    fun togglePause(state: SnakeGameState): SnakeGameState = when (state.phase) {
        SnakePhase.RUNNING -> state.copy(phase = SnakePhase.PAUSED)
        SnakePhase.PAUSED -> state.copy(phase = SnakePhase.RUNNING)
        else -> state
    }

    fun tick(state: SnakeGameState): SnakeGameState {
        if (state.phase != SnakePhase.RUNNING) return state

        val direction = state.queuedDirection ?: state.direction
        val rawHead = state.segments.first().move(direction)
        val newHead = when (state.boundaryMode) {
            SnakeBoundaryMode.WALLS -> {
                if (rawHead.x !in 0 until width || rawHead.y !in 0 until height) {
                    return state.copy(
                        direction = direction,
                        queuedDirection = null,
                        phase = SnakePhase.GAME_OVER,
                    )
                }
                rawHead
            }

            SnakeBoundaryMode.WRAP -> SnakePoint(
                x = (rawHead.x + width) % width,
                y = (rawHead.y + height) % height,
            )
        }

        val ateFood = newHead == state.food
        val collisionBody = if (ateFood) state.segments else state.segments.dropLast(1)
        if (newHead in collisionBody) {
            return state.copy(
                direction = direction,
                queuedDirection = null,
                phase = SnakePhase.GAME_OVER,
            )
        }

        val segments = if (ateFood) {
            listOf(newHead) + state.segments
        } else {
            listOf(newHead) + state.segments.dropLast(1)
        }
        val score = state.score + if (ateFood) 1 else 0
        val highScore = maxOf(state.highScore, score)
        val speedMs = (
            SNAKE_BASE_SPEED_MS - (score / 5) * 10
        ).coerceAtLeast(SNAKE_MIN_SPEED_MS)
        val boardIsFull = segments.size == width * height

        return state.copy(
            segments = segments,
            direction = direction,
            queuedDirection = null,
            food = if (boardIsFull) null else if (ateFood) findFood(segments) else state.food,
            phase = if (boardIsFull) SnakePhase.WON else SnakePhase.RUNNING,
            score = score,
            highScore = highScore,
            speedMs = speedMs,
        )
    }

    private fun initialSegments(): List<SnakePoint> {
        val centerX = width / 2
        val centerY = height / 2
        return listOf(
            SnakePoint(centerX, centerY),
            SnakePoint(centerX - 1, centerY),
            SnakePoint(centerX - 2, centerY),
        )
    }

    private fun findFood(segments: List<SnakePoint>): SnakePoint? {
        val occupied = segments.toHashSet()
        val available = buildList {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val point = SnakePoint(x, y)
                    if (point !in occupied) add(point)
                }
            }
        }
        return available.takeIf(List<*>::isNotEmpty)?.let(foodPicker::pick)
    }
}

private fun SnakePoint.move(direction: SnakeDirection): SnakePoint = when (direction) {
    SnakeDirection.UP -> copy(y = y - 1)
    SnakeDirection.DOWN -> copy(y = y + 1)
    SnakeDirection.LEFT -> copy(x = x - 1)
    SnakeDirection.RIGHT -> copy(x = x + 1)
}

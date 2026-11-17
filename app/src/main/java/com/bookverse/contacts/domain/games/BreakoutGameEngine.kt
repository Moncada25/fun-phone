package com.bookverse.contacts.domain.games

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

const val BREAKOUT_MAX_ROWS = 14
const val BREAKOUT_INITIAL_ROWS = 6
const val BREAKOUT_COLUMNS = 8
const val BREAKOUT_PADDLE_WIDTH = 0.24f
const val BREAKOUT_PADDLE_HEIGHT = 0.035f
const val BREAKOUT_PADDLE_Y = 0.95f
const val BREAKOUT_BALL_RADIUS = 0.018f
const val BREAKOUT_BRICK_TOP = 0.02f
const val BREAKOUT_BRICK_HEIGHT = 0.048f
const val BREAKOUT_BRICK_GAP = 0.012f
const val BREAKOUT_INITIAL_SPEED = 0.52f
const val BREAKOUT_PADDLE_SPEED = 0.9f
const val BREAKOUT_ROW_INTERVAL = 5

enum class BreakoutPhase {
    READY,
    RUNNING,
    PAUSED,
    GAME_OVER,
}

enum class BreakoutPaddleInput {
    NONE,
    LEFT,
    RIGHT,
}

data class BreakoutVector(
    val x: Float,
    val y: Float,
) {
    operator fun plus(other: BreakoutVector): BreakoutVector = BreakoutVector(x + other.x, y + other.y)

    operator fun times(multiplier: Float): BreakoutVector = BreakoutVector(x * multiplier, y * multiplier)
}

data class BreakoutBall(
    val position: BreakoutVector,
    val velocity: BreakoutVector,
)

data class BreakoutBrick(
    val row: Int,
    val column: Int,
    val colorIndex: Int,
)

data class BreakoutGameState(
    val bricks: List<BreakoutBrick>,
    val ball: BreakoutBall,
    val paddleX: Float,
    val score: Int = 0,
    val highScore: Int = 0,
    val paddleHits: Int = 0,
    val rowsAdded: Int = 0,
    val speed: Float = BREAKOUT_INITIAL_SPEED,
    val paddleInput: BreakoutPaddleInput = BreakoutPaddleInput.NONE,
    val phase: BreakoutPhase = BreakoutPhase.READY,
)

class BreakoutGameEngine(
    private val initialRows: Int = BREAKOUT_INITIAL_ROWS,
    private val columns: Int = BREAKOUT_COLUMNS,
) {
    init {
        require(initialRows in 1..BREAKOUT_MAX_ROWS) { "Initial rows must fit the arena" }
        require(columns >= 2) { "Breakout requires at least two columns" }
    }

    fun ready(highScore: Int = 0): BreakoutGameState = BreakoutGameState(
        bricks = createRowList(initialRows),
        ball = initialBall(),
        paddleX = centeredPaddle(),
        highScore = highScore.coerceAtLeast(0),
    )

    fun start(highScore: Int = 0): BreakoutGameState = ready(highScore).copy(
        phase = BreakoutPhase.RUNNING,
    )

    fun setPaddleInput(
        state: BreakoutGameState,
        input: BreakoutPaddleInput,
    ): BreakoutGameState = if (state.phase == BreakoutPhase.RUNNING) {
        state.copy(paddleInput = input)
    } else {
        state
    }

    fun togglePause(state: BreakoutGameState): BreakoutGameState = when (state.phase) {
        BreakoutPhase.RUNNING -> state.copy(
            phase = BreakoutPhase.PAUSED,
            paddleInput = BreakoutPaddleInput.NONE,
        )

        BreakoutPhase.PAUSED -> state.copy(phase = BreakoutPhase.RUNNING)
        else -> state
    }

    fun tick(state: BreakoutGameState, deltaSeconds: Float): BreakoutGameState {
        if (state.phase != BreakoutPhase.RUNNING) return state

        val dt = deltaSeconds.coerceIn(0f, 0.05f)
        val paddleX = when (state.paddleInput) {
            BreakoutPaddleInput.LEFT -> state.paddleX - BREAKOUT_PADDLE_SPEED * dt
            BreakoutPaddleInput.RIGHT -> state.paddleX + BREAKOUT_PADDLE_SPEED * dt
            BreakoutPaddleInput.NONE -> state.paddleX
        }.coerceIn(0f, 1f - BREAKOUT_PADDLE_WIDTH)

        val oldPosition = state.ball.position
        var position = oldPosition + state.ball.velocity * dt
        var velocity = state.ball.velocity

        if (position.x - BREAKOUT_BALL_RADIUS <= 0f) {
            position = position.copy(x = BREAKOUT_BALL_RADIUS)
            velocity = velocity.copy(x = abs(velocity.x))
        } else if (position.x + BREAKOUT_BALL_RADIUS >= 1f) {
            position = position.copy(x = 1f - BREAKOUT_BALL_RADIUS)
            velocity = velocity.copy(x = -abs(velocity.x))
        }
        if (position.y - BREAKOUT_BALL_RADIUS <= 0f) {
            position = position.copy(y = BREAKOUT_BALL_RADIUS)
            velocity = velocity.copy(y = abs(velocity.y))
        }

        var paddleHits = state.paddleHits
        var rowsAdded = state.rowsAdded
        var speed = state.speed
        var bricks = state.bricks
        var score = state.score
        var phase = BreakoutPhase.RUNNING

        if (velocity.y > 0f && crossedPaddle(oldPosition, position, paddleX)) {
            val hitX = position.x.coerceIn(paddleX, paddleX + BREAKOUT_PADDLE_WIDTH)
            val hitRatio = ((hitX - paddleX) / BREAKOUT_PADDLE_WIDTH).coerceIn(0f, 1f)
            val angle = (Math.PI * (0.25 + 0.5 * hitRatio)).toFloat()
            paddleHits++
            velocity = BreakoutVector(
                x = cos(angle) * speed,
                y = -sin(angle) * speed,
            )
            position = position.copy(y = BREAKOUT_PADDLE_Y - BREAKOUT_BALL_RADIUS)

            if (paddleHits % BREAKOUT_ROW_INTERVAL == 0) {
                rowsAdded++
                speed = (speed * 1.08f).coerceAtMost(1.15f)
                val inserted = insertRow(bricks)
                if (inserted == null) {
                    phase = BreakoutPhase.GAME_OVER
                } else {
                    bricks = inserted
                    velocity = velocity.normalized() * speed
                }
            }
        }

        if (phase == BreakoutPhase.RUNNING) {
            val hitBrick = bricks.firstOrNull { brick ->
                intersectsBrick(position, brick)
            }
            if (hitBrick != null) {
                bricks = bricks.filterNot { it == hitBrick }
                score++
                velocity = reflectFromBrick(oldPosition, position, velocity, hitBrick)
                position = oldPosition
            }
        }

        if (position.y - BREAKOUT_BALL_RADIUS > 1f) {
            phase = BreakoutPhase.GAME_OVER
        }

        return state.copy(
            bricks = bricks,
            ball = BreakoutBall(position, velocity),
            paddleX = paddleX,
            score = score,
            highScore = maxOf(state.highScore, score),
            paddleHits = paddleHits,
            rowsAdded = rowsAdded,
            speed = speed,
            phase = phase,
        )
    }

    private fun createRowList(rows: Int, rowOffset: Int = 0): List<BreakoutBrick> = buildList {
        repeat(rows) { row ->
            repeat(columns) { column ->
                add(
                    BreakoutBrick(
                        row = row + rowOffset,
                        column = column,
                        colorIndex = (row + column + rowOffset) % 5,
                    )
                )
            }
        }
    }

    private fun insertRow(bricks: List<BreakoutBrick>): List<BreakoutBrick>? {
        if (bricks.any { it.row + 1 >= BREAKOUT_MAX_ROWS }) return null
        return bricks.map { it.copy(row = it.row + 1) } + createRowList(1, rowOffset = 0)
    }

    private fun intersectsBrick(position: BreakoutVector, brick: BreakoutBrick): Boolean {
        val left = brickLeft(brick)
        val top = brickTop(brick)
        val right = left + brickWidth()
        val bottom = top + BREAKOUT_BRICK_HEIGHT
        val closestX = position.x.coerceIn(left, right)
        val closestY = position.y.coerceIn(top, bottom)
        val dx = position.x - closestX
        val dy = position.y - closestY
        return dx * dx + dy * dy <= BREAKOUT_BALL_RADIUS * BREAKOUT_BALL_RADIUS
    }

    private fun reflectFromBrick(
        oldPosition: BreakoutVector,
        newPosition: BreakoutVector,
        velocity: BreakoutVector,
        brick: BreakoutBrick,
    ): BreakoutVector {
        val horizontalCrossing =
            oldPosition.x < brickLeft(brick) - BREAKOUT_BALL_RADIUS ||
                oldPosition.x > brickLeft(brick) + brickWidth() + BREAKOUT_BALL_RADIUS
        return if (horizontalCrossing && abs(newPosition.x - oldPosition.x) > abs(newPosition.y - oldPosition.y)) {
            velocity.copy(x = -velocity.x)
        } else {
            velocity.copy(y = -velocity.y)
        }
    }

    private fun crossedPaddle(
        oldPosition: BreakoutVector,
        newPosition: BreakoutVector,
        paddleX: Float,
    ): Boolean {
        val paddleLeft = paddleX
        val paddleRight = paddleX + BREAKOUT_PADDLE_WIDTH
        val crossedTop =
            oldPosition.y + BREAKOUT_BALL_RADIUS <= BREAKOUT_PADDLE_Y &&
                newPosition.y + BREAKOUT_BALL_RADIUS >= BREAKOUT_PADDLE_Y
        return crossedTop && newPosition.x in paddleLeft..paddleRight
    }

    private fun brickWidth(): Float = (1f - BREAKOUT_BRICK_GAP * (columns - 1)) / columns

    private fun brickLeft(brick: BreakoutBrick): Float =
        brick.column * (brickWidth() + BREAKOUT_BRICK_GAP)

    private fun brickTop(brick: BreakoutBrick): Float =
        BREAKOUT_BRICK_TOP + brick.row * (BREAKOUT_BRICK_HEIGHT + BREAKOUT_BRICK_GAP)

    private fun centeredPaddle(): Float = (1f - BREAKOUT_PADDLE_WIDTH) / 2f

    private fun initialBall(): BreakoutBall {
        val direction = BreakoutVector(0.32f, -0.45f).normalized() * BREAKOUT_INITIAL_SPEED
        return BreakoutBall(
            position = BreakoutVector(0.5f, BREAKOUT_PADDLE_Y - BREAKOUT_PADDLE_HEIGHT - 0.08f),
            velocity = direction,
        )
    }
}

private fun BreakoutVector.normalized(): BreakoutVector {
    val length = hypot(x.toDouble(), y.toDouble()).toFloat().coerceAtLeast(0.0001f)
    return BreakoutVector(x / length, y / length)
}

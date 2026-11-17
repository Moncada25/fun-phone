package com.bookverse.contacts.domain.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreakoutGameEngineTest {
    @Test
    fun startCreatesNormalizedArenaAndPreservesHighScore() {
        val engine = BreakoutGameEngine()

        val state = engine.start(highScore = 17)

        assertEquals(BreakoutPhase.RUNNING, state.phase)
        assertEquals(BREAKOUT_INITIAL_ROWS * BREAKOUT_COLUMNS, state.bricks.size)
        assertEquals(17, state.highScore)
        assertTrue(state.paddleX in 0f..(1f - BREAKOUT_PADDLE_WIDTH))
        assertTrue(state.ball.position.x in 0f..1f)
        assertTrue(state.ball.position.y in 0f..1f)
        assertTrue(state.ball.velocity.y < 0f)
    }

    @Test
    fun pauseStopsPhysicsAndClearsPaddleInput() {
        val engine = BreakoutGameEngine()
        val running = engine.setPaddleInput(engine.start(), BreakoutPaddleInput.RIGHT)
        val paused = engine.togglePause(running)

        assertEquals(BreakoutPhase.PAUSED, paused.phase)
        assertEquals(BreakoutPaddleInput.NONE, paused.paddleInput)
        assertEquals(paused, engine.tick(paused, 1f))
    }

    @Test
    fun paddleInputMovesAndClampsToArena() {
        val engine = BreakoutGameEngine()
        val moved = engine.tick(
            engine.setPaddleInput(engine.start(), BreakoutPaddleInput.RIGHT),
            0.05f,
        )

        assertTrue(moved.paddleX > (1f - BREAKOUT_PADDLE_WIDTH) / 2f)

        val atRightEdge = engine.tick(
            engine.start().copy(
                paddleX = 0.99f,
                paddleInput = BreakoutPaddleInput.RIGHT,
            ),
            0.05f,
        )
        assertEquals(1f - BREAKOUT_PADDLE_WIDTH, atRightEdge.paddleX)
    }

    @Test
    fun wallBounceReflectsBallBackIntoArena() {
        val engine = BreakoutGameEngine()
        val state = engine.start().copy(
            bricks = emptyList(),
            ball = BreakoutBall(
                position = BreakoutVector(BREAKOUT_BALL_RADIUS + 0.001f, 0.5f),
                velocity = BreakoutVector(-0.6f, 0f),
            ),
        )

        val bounced = engine.tick(state, 0.05f)

        assertTrue(bounced.ball.velocity.x > 0f)
        assertEquals(BREAKOUT_BALL_RADIUS, bounced.ball.position.x)
    }

    @Test
    fun brickCollisionRemovesBrickAndIncrementsScore() {
        val engine = BreakoutGameEngine(initialRows = 1)
        val brickWidth = (1f - BREAKOUT_BRICK_GAP * (BREAKOUT_COLUMNS - 1)) / BREAKOUT_COLUMNS
        val brick = BreakoutBrick(row = 0, column = 0, colorIndex = 0)
        val state = engine.start().copy(
            bricks = listOf(brick),
            ball = BreakoutBall(
                position = BreakoutVector(
                    x = brickWidth / 2f,
                    y = BREAKOUT_BRICK_TOP + BREAKOUT_BRICK_HEIGHT + BREAKOUT_BALL_RADIUS + 0.002f,
                ),
                velocity = BreakoutVector(0f, -0.5f),
            ),
        )

        val hit = engine.tick(state, 0.05f)

        assertTrue(hit.bricks.isEmpty())
        assertEquals(1, hit.score)
        assertTrue(hit.ball.velocity.y > 0f)
    }

    @Test
    fun fifthPaddleHitAddsRowAndAccelerates() {
        val engine = BreakoutGameEngine(initialRows = 1)
        val state = engine.start().copy(
            bricks = emptyList(),
            paddleX = 0.38f,
            paddleHits = BREAKOUT_ROW_INTERVAL - 1,
            ball = BreakoutBall(
                position = BreakoutVector(0.5f, BREAKOUT_PADDLE_Y - BREAKOUT_BALL_RADIUS - 0.002f),
                velocity = BreakoutVector(0f, 0.5f),
            ),
        )

        val hit = engine.tick(state, 0.05f)

        assertEquals(1, hit.rowsAdded)
        assertEquals(BREAKOUT_COLUMNS, hit.bricks.size)
        assertTrue(hit.speed > BREAKOUT_INITIAL_SPEED)
        assertEquals(BREAKOUT_ROW_INTERVAL, hit.paddleHits)
    }

    @Test
    fun ballLeavingBottomEndsGame() {
        val engine = BreakoutGameEngine()
        val state = engine.start().copy(
            bricks = emptyList(),
            ball = BreakoutBall(
                position = BreakoutVector(0.5f, 0.995f),
                velocity = BreakoutVector(0f, 0.5f),
            ),
        )

        assertEquals(BreakoutPhase.GAME_OVER, engine.tick(state, 0.05f).phase)
    }

    @Test
    fun gameOverStateDoesNotAdvance() {
        val engine = BreakoutGameEngine()
        val gameOver = engine.start().copy(phase = BreakoutPhase.GAME_OVER)

        assertEquals(gameOver, engine.tick(gameOver, 1f))
    }
}

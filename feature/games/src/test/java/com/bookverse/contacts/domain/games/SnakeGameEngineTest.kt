package com.bookverse.contacts.domain.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnakeGameEngineTest {
    @Test
    fun startCreatesCenteredSnakeAndFoodOutsideBody() {
        val engine = SnakeGameEngine(foodPicker = SnakeFoodPicker { it.first() })

        val state = engine.start(SnakeBoundaryMode.WALLS, highScore = 7)

        assertEquals(SnakePhase.RUNNING, state.phase)
        assertEquals(3, state.segments.size)
        assertEquals(7, state.highScore)
        assertNotEquals(null, state.food)
        assertFalse(state.food in state.segments)
    }

    @Test
    fun queueRejectsReverseAndAdditionalTurnBeforeTick() {
        val engine = SnakeGameEngine(foodPicker = SnakeFoodPicker { it.last() })
        val started = engine.start(SnakeBoundaryMode.WALLS, 0)

        val reverse = engine.queueDirection(started, SnakeDirection.LEFT)
        val upward = engine.queueDirection(reverse, SnakeDirection.UP)
        val secondTurn = engine.queueDirection(upward, SnakeDirection.LEFT)

        assertNull(reverse.queuedDirection)
        assertEquals(SnakeDirection.UP, upward.queuedDirection)
        assertEquals(SnakeDirection.UP, secondTurn.queuedDirection)
    }

    @Test
    fun wallsEndGameAndWrapReturnsOnOppositeEdge() {
        val engine = SnakeGameEngine(width = 5, height = 3, foodPicker = SnakeFoodPicker { it.last() })
        val edgeState = SnakeGameState(
            segments = listOf(SnakePoint(4, 1), SnakePoint(3, 1), SnakePoint(2, 1)),
            direction = SnakeDirection.RIGHT,
            food = SnakePoint(0, 0),
            phase = SnakePhase.RUNNING,
            boundaryMode = SnakeBoundaryMode.WALLS,
        )

        assertEquals(SnakePhase.GAME_OVER, engine.tick(edgeState).phase)

        val wrapped = engine.tick(edgeState.copy(boundaryMode = SnakeBoundaryMode.WRAP))
        assertEquals(SnakePhase.RUNNING, wrapped.phase)
        assertEquals(SnakePoint(0, 1), wrapped.segments.first())
    }

    @Test
    fun movingIntoDepartingTailCellIsAllowed() {
        val engine = SnakeGameEngine(width = 4, height = 4, foodPicker = SnakeFoodPicker { it.last() })
        val state = SnakeGameState(
            segments = listOf(
                SnakePoint(1, 1),
                SnakePoint(1, 2),
                SnakePoint(0, 2),
                SnakePoint(0, 1),
            ),
            direction = SnakeDirection.LEFT,
            food = SnakePoint(3, 3),
            phase = SnakePhase.RUNNING,
        )

        val moved = engine.tick(state)

        assertEquals(SnakePhase.RUNNING, moved.phase)
        assertEquals(SnakePoint(0, 1), moved.segments.first())
    }

    @Test
    fun eatingGrowsScoresAndAcceleratesEveryFivePoints() {
        val engine = SnakeGameEngine(width = 8, height = 3, foodPicker = SnakeFoodPicker { it.last() })
        val state = SnakeGameState(
            segments = listOf(SnakePoint(4, 1), SnakePoint(3, 1), SnakePoint(2, 1)),
            direction = SnakeDirection.RIGHT,
            food = SnakePoint(5, 1),
            phase = SnakePhase.RUNNING,
            score = 4,
            highScore = 4,
        )

        val eaten = engine.tick(state)

        assertEquals(4, eaten.segments.size)
        assertEquals(5, eaten.score)
        assertEquals(5, eaten.highScore)
        assertEquals(140, eaten.speedMs)
        assertFalse(eaten.food in eaten.segments)
    }

    @Test
    fun selfCollisionEndsGame() {
        val engine = SnakeGameEngine(width = 5, height = 5, foodPicker = SnakeFoodPicker { it.last() })
        val state = SnakeGameState(
            segments = listOf(
                SnakePoint(2, 2),
                SnakePoint(2, 1),
                SnakePoint(1, 1),
                SnakePoint(1, 2),
                SnakePoint(1, 3),
            ),
            direction = SnakeDirection.LEFT,
            food = SnakePoint(4, 4),
            phase = SnakePhase.RUNNING,
        )

        assertEquals(SnakePhase.GAME_OVER, engine.tick(state).phase)
    }

    @Test
    fun fillingBoardProducesWinWithoutRequestingAnotherFood() {
        val engine = SnakeGameEngine(width = 4, height = 1, foodPicker = SnakeFoodPicker { it.single() })

        val won = engine.tick(engine.start(SnakeBoundaryMode.WALLS, 0))

        assertEquals(SnakePhase.WON, won.phase)
        assertEquals(4, won.segments.size)
        assertEquals(1, won.score)
        assertNull(won.food)
    }

    @Test
    fun pausedStateDoesNotAdvance() {
        val engine = SnakeGameEngine(foodPicker = SnakeFoodPicker { it.last() })
        val paused = engine.togglePause(engine.start(SnakeBoundaryMode.WALLS, 0))

        assertEquals(paused, engine.tick(paused))
        assertTrue(paused.phase == SnakePhase.PAUSED)
    }
}

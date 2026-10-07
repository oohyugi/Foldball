package com.yogi.foldball

import com.yogi.foldball.domain.game.engine.GameEngine
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StandeeLoftCollisionTest {
    private fun incomingBall(height: Float): GameEngine {
        val engine = GameEngine(393f, 580f, targetScore = null)
        val standee = Vector2(196.5f, 290f)
        val players = engine.snapshot.players.map { player ->
            val position = if (player.id == "blue-left") standee else Vector2(55f, 80f)
            player.copy(position = position, home = position)
        }
        GameEngine::class.java.getDeclaredField("players").apply { isAccessible = true }
            .set(engine, players)
        GameEngine::class.java.getDeclaredField("ball").apply { isAccessible = true }
            .set(
                engine,
                engine.snapshot.ball.copy(
                    position = Vector2(standee.x, 340f),
                    velocity = Vector2(0f, -600f),
                    height = height,
                    verticalVelocity = 0f
                )
            )
        GameEngine::class.java.getDeclaredField("phase").apply { isAccessible = true }
            .set(engine, GamePhase.BALL_MOVING)
        return engine
    }

    @Test
    fun lowBallStillHitsStandee() {
        val engine = incomingBall(0f)
        repeat(10) { engine.step(1f / 120f) }

        assertTrue(engine.snapshot.soundEvents.blocks > 0)
    }

    @Test
    fun highBallPassesOverStandeeWithoutABlock() {
        val engine = incomingBall(180f)
        repeat(20) { engine.step(1f / 120f) }

        assertEquals(0, engine.snapshot.soundEvents.blocks)
        assertTrue(engine.snapshot.ball.position.y < 290f)
    }
}

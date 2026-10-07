package com.yogi.foldball

import com.yogi.foldball.domain.game.engine.AimDirection
import com.yogi.foldball.domain.game.engine.GameEngine
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchFormat
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DirectionalPassTest {
    @Test
    fun horizontalAimUsesTheOriginalForwardArc() {
        val angles = listOf(
            -AimDirection.maxAngleRadians,
            0f,
            AimDirection.maxAngleRadians
        )
        for (team in GameTeam.entries) {
            for (angle in angles) {
                val direction = AimDirection.shotVector(team, angle)
                assertTrue(direction.y * (if (team == GameTeam.RED) -1f else 1f) > 0f)
                assertTrue(abs(direction.x) < 1f)
            }
        }
        val engine = GameEngine(393f, 580f, targetScore = null)
        assertTrue(engine.finishDefense())
        engine.aimBy(1000f)
        assertEquals(AimDirection.maxAngleRadians, engine.snapshot.aimAngle, .0001f)
        engine.aimBy(-2000f)
        assertEquals(-AimDirection.maxAngleRadians, engine.snapshot.aimAngle, .0001f)
    }

    @Test
    fun angledForwardShotClearsTheShootersCollider() {
        val engine = GameEngine(393f, 580f, targetScore = null, format = MatchFormat.THREE_V_THREE)
        assertTrue(engine.finishDefense())
        engine.setAimAngle(AimDirection.maxAngleRadians)
        engine.setPower(.4f)
        val shooterX = engine.snapshot.players.first { it.id == "red-striker" }.position.x
        val ballY = engine.snapshot.ball.position.y
        assertTrue(engine.shoot())
        repeat(40) { engine.step(1f / 120f) }

        assertTrue(engine.snapshot.ball.position.x > shooterX + 38f)
        assertTrue(engine.snapshot.ball.position.y < ballY)
        assertEquals(0, engine.snapshot.soundEvents.blocks)
    }
}

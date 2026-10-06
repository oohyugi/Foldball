package com.yogi.foldball
import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*
class MatchTimeExpiryTest {
    @Test fun timeoutDuringFormationEndsInDrawAndCopiesPreserveExpiry() {
        val e = GameEngine(393f, 580f)
        e.expireMatchTime()
        assertEquals(GamePhase.MATCH_OVER, e.currentPhase)
        assertNull(e.snapshot.winner)
        val copy = e.copyForSimulation()
        copy.step(1f)
        assertEquals(e.snapshot, copy.snapshot)
    }

    @Test fun timeoutAllowsFinalShotToScoreBeforeResult() {
        val e = GameEngine(393f, 580f)
        e.finishDefense()
        GameEngine::class.java.getDeclaredField("players").apply {
            isAccessible = true
        }.set(
            e,
            e.snapshot.players.mapIndexed { i, p ->
                p.copy(position = Vector2(40f + i * 40f, 300f))
            }
        )
        GameEngine::class.java.getDeclaredField("ball").apply {
            isAccessible = true
        }.set(e, e.snapshot.ball.copy(position = Vector2(196.5f, 93f)))
        e.setPower(.5f)
        e.shoot()
        e.expireMatchTime()
        assertEquals(GamePhase.BALL_MOVING, e.currentPhase)
        repeat(1000) { if (e.currentPhase != GamePhase.MATCH_OVER) e.step(1f / 120f) }
        assertEquals(GamePhase.MATCH_OVER, e.currentPhase)
        assertEquals(1, e.snapshot.redScore)
        assertEquals(GameTeam.RED, e.snapshot.winner)
    }
}

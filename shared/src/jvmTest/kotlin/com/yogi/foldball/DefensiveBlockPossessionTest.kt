package com.yogi.foldball

import com.yogi.foldball.domain.game.engine.GameEngine
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchFormat
import com.yogi.foldball.domain.game.engine.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefensiveBlockPossessionTest {
    @Test
    fun defensiveContactOverridesNearestPlayerOnlyWhenFlagIsEnabled() {
        assertEquals(GameTeam.BLUE, nextKickerAfterBlock(enabled = true))
        assertEquals(GameTeam.RED, nextKickerAfterBlock(enabled = false))
    }

    private fun nextKickerAfterBlock(enabled: Boolean): GameTeam {
        val engine = GameEngine(
            width = 393f,
            height = 580f,
            targetScore = null,
            format = MatchFormat.THREE_V_THREE,
            defensiveBlockWinsPossession = enabled
        )
        val playersField = GameEngine::class.java.getDeclaredField("players").apply {
            isAccessible = true
        }
        val arranged = engine.snapshot.players.map { player ->
            val position = when (player.id) {
                "red-striker" -> Vector2(196.5f, 330f)
                "blue-striker" -> Vector2(196.5f, 220f)
                "red-left" -> Vector2(90f, 330f)
                "red-right" -> Vector2(310f, 330f)
                "blue-left" -> Vector2(80f, 90f)
                else -> Vector2(310f, 90f)
            }
            player.copy(position = position, home = position)
        }
        playersField.set(engine, arranged)
        assertTrue(engine.finishDefense())
        engine.setPower(.3f)
        assertTrue(engine.shoot())

        var frames = 0
        while (engine.snapshot.soundEvents.blocks == 0 && frames++ < 120) {
            engine.step(1f / 120f)
        }
        assertTrue(engine.snapshot.soundEvents.blocks > 0, "The shot should strike the blue standee")

        val ballField = GameEngine::class.java.getDeclaredField("ball").apply {
            isAccessible = true
        }
        val stop = engine.snapshot.ball.position
        ballField.set(
            engine,
            engine.snapshot.ball.copy(
                velocity = Vector2(0f, 0f),
                height = 0f,
                verticalVelocity = 0f
            )
        )
        playersField.set(
            engine,
            engine.snapshot.players.map { player ->
                if (player.id == "red-left") {
                    player.copy(position = Vector2(stop.x + 18f, stop.y), home = Vector2(stop.x + 18f, stop.y))
                } else {
                    player
                }
            }
        )
        engine.step(1f / 120f)
        assertEquals(GamePhase.KICKER_APPROACH, engine.snapshot.phase)
        val selected = engine.snapshot.players.first { it.id == engine.snapshot.focusedPlayerId }
        return selected.team
    }
}

package com.yogi.foldball

import com.yogi.foldball.domain.game.engine.GameEngine
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchTimingConfig
import com.yogi.foldball.domain.game.engine.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameEngineFlowTest {
    @Test
    fun attackTimeoutHandsBallToNearestDefenderWithoutMovingBall() {
        val engine = GameEngine(393f, 580f, timing = MatchTimingConfig(2, 2))
        repeat(8) { engine.step(.25f) }
        assertEquals(GamePhase.AIMING, engine.snapshot.phase)
        assertTrue(engine.snapshot.phaseTimeRemaining in 1.9f..2f)

        val ballBefore = engine.snapshot.ball.position
        val nearestBlue = engine.snapshot.players.filter { it.team == GameTeam.BLUE }
            .minWith(compareBy({ (it.position - ballBefore).length() }, { it.id }))
        repeat(8) { engine.step(.25f) }
        assertEquals(GamePhase.TURNOVER_RETREAT, engine.snapshot.phase)
        var frames = 0
        while (engine.snapshot.phase != GamePhase.DEFENDING && frames++ < 40) {
            engine.step(1f / 30f)
        }

        assertEquals(GamePhase.DEFENDING, engine.snapshot.phase)
        assertEquals(nearestBlue.id, engine.snapshot.focusedPlayerId)
        assertEquals(ballBefore, engine.snapshot.ball.position)
        assertTrue(engine.snapshot.phaseTimeRemaining in 1.9f..2f)
        val oldKicker = engine.snapshot.players.first { it.id == "red-striker" }
        val newKicker = engine.snapshot.players.first { it.id == nearestBlue.id }
        assertTrue((oldKicker.position - ballBefore).length() >
            (newKicker.position - ballBefore).length())
    }

    @Test
    fun goalRestoresOpeningFormationBeforeConcedingTeamKickoff() {
        val engine = GameEngine(393f, 580f, targetScore = null)
        val original = engine.snapshot.players.associate { it.id to it.position }
        assertTrue(engine.beginPlayerMoveForTest("blue-left", Vector2(8f, 0f)))
        assertTrue(engine.snapshot.players.first { it.id == "blue-left" }.position != original.getValue("blue-left"))
        assertTrue(engine.finishDefense())
        engine.setPower(1f)
        assertTrue(engine.shoot())

        // Put an in-flight ball just inside the red scoring line. The production
        // collision, goal, celebration, and kickoff transitions then run normally.
        val ballField = GameEngine::class.java.getDeclaredField("ball")
        ballField.isAccessible = true
        val ball = engine.snapshot.ball
        ballField.set(engine, ball.copy(
            position = Vector2(196.5f, 29f), velocity = Vector2(0f, -900f),
            height = 0f, verticalVelocity = 0f,
        ))
        engine.step(1f / 60f)
        assertEquals(GamePhase.GOAL, engine.snapshot.phase)
        assertEquals(1, engine.snapshot.redScore)
        assertEquals(GameTeam.BLUE, engine.snapshot.kickoffTeam)

        var frames = 0
        while (engine.snapshot.phase != GamePhase.DEFENDING && frames++ < 120) {
            engine.step(1f / 30f)
        }
        assertEquals(GamePhase.DEFENDING, engine.snapshot.phase)
        assertEquals("blue-striker", engine.snapshot.focusedPlayerId)
        assertEquals(Vector2(196.5f, 290f), engine.snapshot.ball.position)
        assertTrue(engine.snapshot.players.first { it.id == "blue-striker" }.position.y < 290f)
        assertTrue(engine.snapshot.players.first { it.id == "red-striker" }.position.y > 290f)
        for (player in engine.snapshot.players.filterNot { it.id.endsWith("striker") }) {
            assertEquals(original.getValue(player.id), player.position, player.id)
            assertEquals(original.getValue(player.id), player.home, player.id)
        }
        assertEquals(0, engine.defensiveMovesUsed)

        // The next goal swaps the kickoff direction and still restores both teams.
        assertTrue(engine.finishDefense())
        engine.setPower(1f)
        assertTrue(engine.shoot())
        val nextBall = engine.snapshot.ball
        ballField.set(engine, nextBall.copy(
            position = Vector2(196.5f, 551f), velocity = Vector2(0f, 900f),
            height = 0f, verticalVelocity = 0f,
        ))
        engine.step(1f / 60f)
        assertEquals(GamePhase.GOAL, engine.snapshot.phase)
        assertEquals(1, engine.snapshot.blueScore)
        assertEquals(GameTeam.RED, engine.snapshot.kickoffTeam)
        frames = 0
        while (engine.snapshot.phase != GamePhase.DEFENDING && frames++ < 120) {
            engine.step(1f / 30f)
        }
        assertEquals(GamePhase.DEFENDING, engine.snapshot.phase)
        assertEquals("red-striker", engine.snapshot.focusedPlayerId)
        assertEquals(Vector2(196.5f, 290f), engine.snapshot.ball.position)
        assertEquals(original.getValue("red-striker"),
            engine.snapshot.players.first { it.id == "red-striker" }.position)
        assertEquals(original.getValue("blue-striker"),
            engine.snapshot.players.first { it.id == "blue-striker" }.position)
        for (player in engine.snapshot.players.filterNot { it.id.endsWith("striker") }) {
            assertEquals(original.getValue(player.id), player.position, player.id)
        }
    }

    private fun GameEngine.beginPlayerMoveForTest(id: String, delta: Vector2): Boolean {
        val before = snapshot.players.first { it.id == id }.position
        beginPlayerMove(id)
        moveInactivePlayer(id, delta)
        endPlayerMove(id)
        return snapshot.players.first { it.id == id }.position != before
    }
}

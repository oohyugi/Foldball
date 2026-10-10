package com.yogi.foldball

import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*

class PlayerPlacementPreviewTest {
    private fun set(game: GameEngine, name: String, value: Any) {
        GameEngine::class.java.getDeclaredField(name).apply { isAccessible = true }.set(game, value)
    }
    private fun ready(game: GameEngine) {
        game.finishDefense(); game.finishDefense()
        repeat(80) { if (game.snapshot.phase != GamePhase.AIMING) game.step(1f / 120f) }
        assertEquals(GamePhase.AIMING, game.snapshot.phase)
    }
    private fun game() = GameEngine(393f, 580f, targetScore = null, format = MatchFormat.THREE_V_THREE,
        threeActionTurns = true).also { game ->
        ready(game)
        val positions = mapOf("red-left" to Vector2(80f, 500f), "red-right" to Vector2(290f, 500f),
            "red-striker" to Vector2(200f, 330f), "blue-right" to Vector2(290f, 70f),
            "blue-left" to Vector2(290f, 170f), "blue-striker" to Vector2(290f, 270f))
        set(game, "players", game.snapshot.players.map { it.copy(position = positions.getValue(it.id), home = positions.getValue(it.id)) })
        set(game, "ball", game.snapshot.ball.copy(position = Vector2(200f, 290f)))
    }
    @Test fun previewFollowsFingerThroughInvalidAreaThenCommitsASafeDropOnce() {
        val game = game()
        val players = game.snapshot.players
        game.beginPlayerMove("red-left", previewOnly = true)
        game.moveInactivePlayer("red-left", Vector2(120f, -210f))
        assertEquals(Vector2(200f, 290f), game.snapshot.playerMovePreview?.position)
        assertFalse(requireNotNull(game.snapshot.playerMovePreview).valid)
        assertEquals(players, game.snapshot.players)
        game.moveInactivePlayer("red-left", Vector2(-120f, -190f))
        assertEquals(Vector2(80f, 100f), game.snapshot.playerMovePreview?.position)
        assertTrue(requireNotNull(game.snapshot.playerMovePreview).valid)
        assertEquals(players, game.snapshot.players)
        game.endPlayerMove("red-left")
        assertNull(game.snapshot.playerMovePreview)
        assertEquals(Vector2(80f, 100f), game.snapshot.players.first { it.id == "red-left" }.position)
        assertEquals(1, game.snapshot.actionsUsed)
        game.endPlayerMove("red-left")
        assertEquals(1, game.snapshot.actionsUsed)
        assertTrue(game.undoLastPlayerMove())
        assertEquals(players, game.snapshot.players)
        assertEquals(0, game.snapshot.actionsUsed)
    }
    @Test fun invalidDropOntoBodyBallOrOutsideFieldReturnsToOriginForFree() {
        for (delta in listOf(Vector2(210f, 0f), Vector2(120f, -210f), Vector2(1000f, -1000f))) {
            val game = game()
            val before = game.snapshot.players
            game.beginPlayerMove("red-left", previewOnly = true)
            game.moveInactivePlayer("red-left", delta)
            assertEquals(Vector2(80f, 500f) + delta, game.snapshot.playerMovePreview?.position)
            assertFalse(requireNotNull(game.snapshot.playerMovePreview).valid)
            game.endPlayerMove("red-left")
            assertEquals(before, game.snapshot.players)
            assertEquals(0, game.snapshot.actionsUsed)
            assertFalse(game.canUndoPlayerMove())
            assertNull(game.snapshot.playerMovePreview)
        }
    }
    @Test fun cancelledValidPreviewDoesNotCommitOrConsumeAnAction() {
        val game = game()
        val before = game.snapshot.players
        game.beginPlayerMove("red-left", previewOnly = true)
        game.moveInactivePlayer("red-left", Vector2(0f, -400f))
        assertTrue(requireNotNull(game.snapshot.playerMovePreview).valid)
        assertTrue(game.cancelPlayerMove("red-left"))
        game.endPlayerMove("red-left")
        assertEquals(before, game.snapshot.players)
        assertEquals(0, game.snapshot.actionsUsed)
        assertNull(game.snapshot.playerMovePreview)
    }
    @Test fun classicStillRejectsFarZonesButItsPreviewNeverGetsStuck() {
        val game = GameEngine(393f, 580f, format = MatchFormat.THREE_V_THREE)
        val player = game.snapshot.players.first { it.id == "blue-left" }
        game.beginPlayerMove(player.id, previewOnly = true)
        val delta = Vector2(0f, 1000f)
        game.moveInactivePlayer(player.id, delta)
        assertEquals(player.position + delta, game.snapshot.playerMovePreview?.position)
        assertFalse(requireNotNull(game.snapshot.playerMovePreview).valid)
        game.endPlayerMove(player.id)
        assertEquals(player, game.snapshot.players.first { it.id == player.id })
        assertEquals(0, game.defensiveMovesUsed)
    }
    @Test fun practiceCarriesBallOnlyOnValidKickerDropAndUndoRestoresBoth() {
        val game = GameEngine(393f, 580f, practiceOpponents = 1)
        val before = game.snapshot
        game.beginPlayerMove("red-striker", previewOnly = true)
        game.moveInactivePlayer("red-striker", Vector2(100f, 100f))
        assertTrue(requireNotNull(game.snapshot.playerMovePreview).valid)
        assertEquals(before.ball, game.snapshot.ball)
        assertEquals(before.players, game.snapshot.players)
        game.endPlayerMove("red-striker")
        assertEquals(before.ball.position + Vector2(100f, 100f), game.snapshot.ball.position)
        assertTrue(game.undoLastPlayerMove())
        assertEquals(before.ball, game.snapshot.ball)
        assertEquals(before.players, game.snapshot.players)
        val kicker = game.snapshot.players.first { it.id == "red-striker" }
        game.beginPlayerMove(kicker.id, previewOnly = true)
        game.moveInactivePlayer(kicker.id, Vector2(0f, 50f - kicker.position.y))
        assertFalse(requireNotNull(game.snapshot.playerMovePreview).valid)
        game.endPlayerMove(kicker.id)
        assertEquals(before.ball, game.snapshot.ball)
        assertEquals(before.players, game.snapshot.players)
    }
    @Test fun initialFormationPreviewMayNotCrossHalfwayAndTimeoutDiscardsIt() {
        val game = GameEngine(393f, 580f, threeActionTurns = true)
        val before = game.snapshot.players
        val player = before.first { it.id == "red-left" }
        game.beginPlayerMove(player.id, previewOnly = true)
        game.moveInactivePlayer(player.id, Vector2(0f, -300f))
        assertFalse(requireNotNull(game.snapshot.playerMovePreview).valid)
        repeat(61) { game.step(.25f) }
        assertEquals(GameTeam.BLUE, game.snapshot.formationTeam)
        assertEquals(before, game.snapshot.players)
        assertNull(game.snapshot.playerMovePreview)
        assertEquals(0, game.snapshot.actionsUsed)
    }
    @Test fun attackTimeoutCommitsAValidPreviewBeforeHandingOver() {
        val game = game()
        game.beginPlayerMove("red-left", previewOnly = true)
        game.moveInactivePlayer("red-left", Vector2(0f, -400f))
        repeat(33) { game.step(.25f) }
        assertNull(game.snapshot.playerMovePreview)
        assertEquals(Vector2(80f, 100f), game.snapshot.players.first { it.id == "red-left" }.position)
        assertEquals(GamePhase.TURNOVER_RETREAT, game.snapshot.phase)
    }
    @Test fun copiesKeepPlacementPreviewsIndependent() {
        val game = game()
        game.beginPlayerMove("red-left", previewOnly = true)
        game.moveInactivePlayer("red-left", Vector2(0f, -100f))
        val copy = game.copyForSimulation()
        copy.moveInactivePlayer("red-left", Vector2(0f, -300f))
        copy.endPlayerMove("red-left")
        assertNotNull(game.snapshot.playerMovePreview)
        assertEquals(Vector2(80f, 400f), game.snapshot.playerMovePreview?.position)
        assertEquals(0, game.snapshot.actionsUsed)
        assertEquals(1, copy.snapshot.actionsUsed)
    }
}

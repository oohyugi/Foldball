package com.yogi.foldball

import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*

class StandeeFoulTest {
    private val center = Vector2(196f, 210f)
    private fun set(game: GameEngine, name: String, value: Any) {
        GameEngine::class.java.getDeclaredField(name).apply { isAccessible = true }.set(game, value)
    }
    private fun untilAiming(game: GameEngine) {
        var frames = 0
        while (game.snapshot.phase != GamePhase.AIMING && frames++ < 300) game.step(1f / 120f)
        assertEquals(GamePhase.AIMING, game.snapshot.phase)
    }
    private fun game(experimental: Boolean = true, practice: Boolean = false): GameEngine {
        val game = GameEngine(393f, 580f, targetScore = null, format = MatchFormat.THREE_V_THREE,
            defensiveBlockWinsPossession = !practice, threeActionTurns = experimental,
            practiceOpponents = if (practice) 1 else null)
        if (!practice) {
            game.finishDefense()
            if (experimental) game.finishDefense()
            untilAiming(game)
        }
        val positions = mapOf(
            "red-striker" to Vector2(196f, 330f), "red-left" to Vector2(65f, 500f), "red-right" to Vector2(327f, 500f),
            "blue-striker" to center, "blue-left" to Vector2(65f, 80f), "blue-right" to Vector2(327f, 80f),
            "blue-practice-0" to center
        )
        set(game, "players", game.snapshot.players.map { it.copy(position = positions.getValue(it.id), home = positions.getValue(it.id)) })
        set(game, "ball", game.snapshot.ball.copy(position = Vector2(196f, 290f)))
        game.setPower(.4f)
        assertTrue(game.shoot())
        return game
    }
    private fun contact(game: GameEngine, offsetX: Float, height: Float = 0f) {
        set(game, "ball", game.snapshot.ball.copy(position = center + Vector2(offsetX, 42f),
            velocity = Vector2(0f, -400f), height = height, verticalVelocity = 0f))
        game.step(.05f)
        game.step(1f / 120f)
    }
    private fun settle(game: GameEngine, position: Vector2 = Vector2(196f, 400f)) {
        set(game, "ball", game.snapshot.ball.copy(position = position, velocity = Vector2(0f, 0f), height = 0f, verticalVelocity = 0f))
        game.step(1f / 120f)
    }
    private fun team(game: GameEngine) = game.snapshot.players.first { it.id == game.snapshot.focusedPlayerId }.team

    @Test fun centralOrSideContactAloneIsNotFoulWhenTheBallStopsFarAway() {
        for (offset in listOf(0f, 24f)) {
            val game = game()
            contact(game, offset)
            assertTrue(game.snapshot.soundEvents.blocks > 0)
            assertEquals(0, game.snapshot.soundEvents.fouls)
            settle(game)
            untilAiming(game)
            assertEquals(0, game.snapshot.soundEvents.fouls)
            assertEquals(GameTeam.RED, team(game))
            assertEquals(1, game.snapshot.actionsUsed)
        }
    }
    @Test fun elevatedContactReboundsAndRetainsTheExperimentalTurn() {
        val game = game()
        contact(game, 0f, 30f)
        assertTrue(game.snapshot.soundEvents.blocks > 0)
        assertEquals(0, game.snapshot.soundEvents.fouls)
        settle(game)
        untilAiming(game)
        assertEquals(GameTeam.RED, team(game))
        assertEquals(1, game.snapshot.actionsUsed)
    }
    @Test fun sideReboundPushesOpponentAndItsNewPositionIsRetained() {
        val game = game()
        contact(game, 24f)
        val hit = game.snapshot.players.first { it.id == "blue-striker" }
        assertTrue(hit.velocity.length() > 0f)
        assertTrue((hit.position - center).length() > 0f)
        assertEquals(0, game.snapshot.soundEvents.fouls)
        settle(game)
        untilAiming(game)
        val settled = game.snapshot.players.first { it.id == "blue-striker" }
        assertTrue((settled.position - center).length() > 0f)
        assertEquals(settled.position, settled.home)
        assertEquals(1, game.snapshot.actionsUsed)
        assertEquals(GameTeam.RED, team(game))
        for (a in game.snapshot.players) for (b in game.snapshot.players) {
            if (a.id != b.id) assertTrue((a.position - b.position).length() >= 67.9f)
        }
    }
    @Test fun contactCausesFoulOnlyWhenBallStopsWithinOneDiameterOfCollider() {
        for (distance in listOf(57f, 59f)) {
            val game = game()
            contact(game, 24f)
            val defender = game.snapshot.players.first { it.id == "blue-striker" }
            settle(game, defender.position + Vector2(distance, 0f))
            assertEquals(if (distance < 58f) 1 else 0, game.snapshot.soundEvents.fouls)
            untilAiming(game)
            assertEquals(if (distance < 58f) GameTeam.BLUE else GameTeam.RED, team(game))
            assertEquals(if (distance < 58f) 0 else 1, game.snapshot.actionsUsed)
        }
    }
    @Test fun stoppingNearUntouchedOpponentDoesNotCauseFoul() {
        val game = game()
        settle(game, center + Vector2(50f, 0f))
        assertEquals(0, game.snapshot.soundEvents.fouls)
        untilAiming(game)
        assertEquals(GameTeam.RED, team(game))
    }
    @Test fun repeatedContactsReportOneFoulAtRestAndNextShotStartsFresh() {
        val game = game()
        contact(game, 0f)
        set(game, "contactCooldownByPlayerId", mutableMapOf<String, Float>())
        contact(game, 0f)
        assertEquals(0, game.snapshot.soundEvents.fouls)
        val defender = game.snapshot.players.first { it.id == "blue-striker" }
        settle(game, defender.position + Vector2(50f, 0f))
        assertEquals(1, game.snapshot.soundEvents.fouls)
        untilAiming(game)
        assertEquals(GameTeam.BLUE, team(game))
        game.setPower(.4f)
        assertTrue(game.shoot())
        val opponent = game.snapshot.players.first { it.team == GameTeam.RED }
        settle(game, opponent.position + Vector2(50f, 0f))
        assertEquals(1, game.snapshot.soundEvents.fouls)
    }
    @Test fun practiceReportsProximityFoulWithoutChangingKicker() {
        val game = game(practice = true)
        contact(game, 24f)
        val defender = game.snapshot.players.first { it.team == GameTeam.BLUE }
        settle(game, defender.position + Vector2(50f, 0f))
        assertEquals(1, game.snapshot.soundEvents.fouls)
        untilAiming(game)
        assertEquals(GameTeam.RED, team(game))
    }
    @Test fun flagOffKeepsDefensiveBlockPossessionForSideHits() {
        val game = game(experimental = false)
        contact(game, 24f)
        assertEquals(0, game.snapshot.soundEvents.fouls)
        settle(game)
        assertEquals(GameTeam.BLUE, team(game))
        assertFalse(game.snapshot.threeActionTurns)
    }
    @Test fun ownStandeeContactIsNeverAnOpponentFoul() {
        val game = game()
        set(game, "players", game.snapshot.players.map {
            if (it.id == "blue-striker") it.copy(team = GameTeam.RED) else it
        })
        contact(game, 0f)
        assertTrue(game.snapshot.soundEvents.blocks > 0)
        assertEquals(0, game.snapshot.soundEvents.fouls)
        val own = game.snapshot.players.first { it.id == "blue-striker" }
        settle(game, own.position + Vector2(50f, 0f))
        untilAiming(game)
        assertEquals(0, game.snapshot.soundEvents.fouls)
        assertEquals(GameTeam.RED, team(game))
    }
    @Test fun goalStillTakesPrecedenceAfterOpponentContact() {
        val game = game()
        contact(game, 0f)
        assertEquals(0, game.snapshot.soundEvents.fouls)
        set(game, "ball", game.snapshot.ball.copy(position = Vector2(196.5f, 29f),
            velocity = Vector2(0f, -900f), height = 0f, verticalVelocity = 0f))
        game.step(1f / 60f)
        assertEquals(GamePhase.GOAL, game.snapshot.phase)
        assertEquals(1, game.snapshot.redScore)
    }
}

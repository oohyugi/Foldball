package com.yogi.foldball

import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*
import com.yogi.foldball.domain.game.bot.BotPlanner
import com.yogi.foldball.domain.game.bot.BotDifficulty
import kotlinx.coroutines.runBlocking

class ThreeActionTurnsTest {
    private fun engine(team: GameTeam = GameTeam.RED) = GameEngine(
        393f, 580f, targetScore = null, format = MatchFormat.THREE_V_THREE,
        initialKickoffTeam = team, threeActionTurns = true
    )
    private fun ready(game: GameEngine) {
        assertTrue(game.finishDefense())
        assertTrue(game.finishDefense())
        untilAiming(game)
    }
    private fun untilAiming(game: GameEngine) {
        var frames = 0
        while (game.snapshot.phase != GamePhase.AIMING && frames++ < 500) game.step(1f / 60f)
        assertEquals(GamePhase.AIMING, game.snapshot.phase)
    }
    private fun set(game: GameEngine, name: String, value: Any) {
        GameEngine::class.java.getDeclaredField(name).apply { isAccessible = true }.set(game, value)
    }
    private fun team(game: GameEngine) = game.snapshot.players.first { it.id == game.snapshot.focusedPlayerId }.team
    private fun move(game: GameEngine) {
        for (player in game.snapshot.players.filter { game.canMovePlayer(it.id) }) {
            val zone = (1..GameEngine.ZONE_COUNT).firstOrNull {
                game.isAdjacentZoneForPlayer(player.id, it) && game.legalPlayerPositionInZone(player.id, it) != null
            } ?: continue
            if (game.movePlayerToZone(player.id, zone)) return
        }
        fail("No legal strategy move")
    }
    private fun settleShot(game: GameEngine) {
        set(game, "ball", game.snapshot.ball.copy(velocity = Vector2(0f, 0f), height = 0f, verticalVelocity = 0f))
        game.step(1f / 120f)
        untilAiming(game)
    }
    @Test fun setupIsTimedAndFreeInEachTeamsHalf() {
        val game = engine(GameTeam.BLUE)
        assertTrue(game.snapshot.openingFormation)
        assertEquals(GameTeam.RED, game.snapshot.formationTeam)
        assertEquals(15f, game.snapshot.phaseTimeRemaining)
        assertTrue(game.canMovePlayer("red-striker"))
        assertFalse(game.canMovePlayer("blue-striker"))
        assertTrue(game.isAdjacentZoneForPlayer("red-left", 9))
        assertFalse(game.isZoneAvailableForPlayer("red-left", 18))
        move(game)
        assertEquals(0, game.snapshot.actionsUsed)
        repeat(61) { game.step(.25f) }
        assertEquals(GameTeam.BLUE, game.snapshot.formationTeam)
        assertTrue(game.snapshot.openingFormation)
        assertTrue(game.finishDefense())
        untilAiming(game)
        assertEquals(GameTeam.BLUE, team(game))
        assertFalse(game.snapshot.openingFormation)
        assertEquals(0, game.snapshot.actionsUsed)
    }
    @Test fun threeMovesConsumeTurnAndUndoRestoresAllowance() {
        val game = engine()
        ready(game)
        move(game)
        assertEquals(1, game.snapshot.actionsUsed)
        assertTrue(game.undoLastPlayerMove())
        assertEquals(0, game.snapshot.actionsUsed)
        repeat(3) { move(game) }
        assertEquals(GamePhase.TURNOVER_RETREAT, game.snapshot.phase)
        untilAiming(game)
        assertEquals(GameTeam.BLUE, team(game))
        assertEquals(0, game.snapshot.actionsUsed)
    }
    @Test fun threeShotsRetainPossessionUntilAllowanceRunsOut() {
        for (initial in GameTeam.entries) {
            val game = engine(initial)
            ready(game)
            repeat(3) { action ->
                game.setPower(.1f)
                assertTrue(game.shoot())
                assertEquals(action + 1, game.snapshot.actionsUsed)
                settleShot(game)
                assertEquals(if (action < 2) initial else if (initial == GameTeam.RED) GameTeam.BLUE else GameTeam.RED, team(game))
            }
            assertEquals(0, game.snapshot.actionsUsed)
        }
    }
    @Test fun physicalDefenderBlockForfeitsRemainingActions() {
        val game = engine()
        ready(game)
        game.setPower(.4f)
        assertTrue(game.shoot())
        val defender = game.snapshot.players.first { it.team == GameTeam.BLUE }
        // Feed a low ball into a real defender collider, then let the normal turn resolver run.
        set(game, "ball", game.snapshot.ball.copy(position = defender.position + Vector2(0f, 42f), velocity = Vector2(0f, -400f), height = 0f, verticalVelocity = 0f))
        game.step(.03f)
        assertTrue(game.snapshot.soundEvents.blocks > 0)
        settleShot(game)
        assertEquals(GameTeam.BLUE, team(game))
        assertEquals(0, game.snapshot.actionsUsed)
    }
    @Test fun endTurnAndTimeoutHandOverWithoutAnotherFormationPhase() {
        val game = engine()
        ready(game)
        assertTrue(game.endTurn())
        untilAiming(game)
        assertEquals(GameTeam.BLUE, team(game))
        repeat(33) { game.step(.25f) }
        untilAiming(game)
        assertEquals(GameTeam.RED, team(game))
    }
    @Test fun goalResetsConfirmedFormationAndOffersTenSecondsPerTeam() {
        val game = engine()
        val positions = game.snapshot.players.associate { it.id to it.position }
        ready(game)
        move(game)
        game.setPower(.4f)
        assertTrue(game.shoot())
        set(game, "ball", game.snapshot.ball.copy(position = Vector2(196.5f, 29f), velocity = Vector2(0f, -900f), height = 0f, verticalVelocity = 0f))
        game.step(1f / 60f)
        assertEquals(GamePhase.GOAL, game.snapshot.phase)
        var frames = 0
        while (game.snapshot.phase != GamePhase.DEFENDING && frames++ < 600) game.step(1f / 120f)
        assertEquals(GamePhase.DEFENDING, game.snapshot.phase)
        assertEquals(GameTeam.RED, game.snapshot.formationTeam)
        assertEquals(10f, game.snapshot.phaseTimeTotal)
        assertTrue(game.snapshot.phaseTimeRemaining in 9.9f..10f)
        assertEquals(0, game.snapshot.actionsUsed)
        assertTrue(game.snapshot.openingFormation)
        for (player in game.snapshot.players) assertEquals(positions.getValue(player.id), player.position, player.id)
        assertTrue(game.finishDefense())
        assertEquals(GameTeam.BLUE, game.snapshot.formationTeam)
        assertEquals(10f, game.snapshot.phaseTimeRemaining)
        assertTrue(game.finishDefense())
        untilAiming(game)
        assertEquals(GameTeam.BLUE, team(game))
        assertFalse(game.snapshot.openingFormation)
    }
    @Test fun confirmedSetupSurvivesLaterKickoffsAndTenSecondExpiry() {
        val game = engine()
        move(game)
        val confirmed = game.snapshot.players.associate { it.id to it.position }
        ready(game)
        repeat(2) {
            game.setPower(.4f)
            assertTrue(game.shoot())
            set(game, "ball", game.snapshot.ball.copy(position = Vector2(196.5f, 29f),
                velocity = Vector2(0f, -900f), height = 0f, verticalVelocity = 0f))
            game.step(1f / 60f)
            var frames = 0
            while (game.snapshot.phase != GamePhase.DEFENDING && frames++ < 600) game.step(1f / 120f)
            assertTrue(game.snapshot.openingFormation)
            for (player in game.snapshot.players) assertEquals(confirmed.getValue(player.id), player.position)
            repeat(41) { game.step(.25f) }
            assertEquals(GameTeam.BLUE, game.snapshot.formationTeam)
            repeat(41) { game.step(.25f) }
            untilAiming(game)
            assertFalse(game.snapshot.openingFormation)
        }
    }

    @Test fun flagOffAndAttackDefendKeepTheirExistingFlow() {
        for (game in listOf(GameEngine(393f, 580f), GameEngine(393f, 580f, playMode = MatchPlayMode.ATTACK_DEFEND, threeActionTurns = true))) {
            assertFalse(game.snapshot.threeActionTurns)
            assertFalse(game.snapshot.openingFormation)
            assertTrue(game.finishDefense())
            assertEquals(GamePhase.AIMING, game.snapshot.phase)
            assertFalse(game.endTurn())
        }
    }
    private fun freeMovementFixture(game: GameEngine, start: Vector2) {
        val positions = mapOf(
            "red-left" to start, "red-right" to Vector2(290f, 500f),
            "red-striker" to Vector2(200f, 330f), "blue-right" to Vector2(290f, 70f),
            "blue-left" to Vector2(290f, 170f), "blue-striker" to Vector2(290f, 270f)
        )
        set(game, "players", game.snapshot.players.map {
            it.copy(position = positions.getValue(it.id), home = positions.getValue(it.id))
        })
        set(game, "ball", game.snapshot.ball.copy(position = Vector2(200f, 290f)))
    }
    @Test fun oneDragMayCrossSeveralZonesAndTheHalfwayLine() {
        val game = engine()
        ready(game)
        freeMovementFixture(game, Vector2(80f, 500f))
        game.beginPlayerMove("red-left")
        game.moveInactivePlayer("red-left", Vector2(0f, -400f))
        game.endPlayerMove("red-left")
        assertEquals(Vector2(80f, 100f), game.snapshot.players.first { it.id == "red-left" }.position)
        assertEquals(1, game.snapshot.actionsUsed)
        assertTrue(game.undoLastPlayerMove())
        assertEquals(Vector2(80f, 500f), game.snapshot.players.first { it.id == "red-left" }.position)
        assertEquals(0, game.snapshot.actionsUsed)
    }
    @Test fun twoPlayersMayShareACellWhenTheirBodiesRemainApart() {
        val game = engine()
        ready(game)
        freeMovementFixture(game, Vector2(40f, 400f))
        set(game, "players", game.snapshot.players.map {
            if (it.id == "red-right") it.copy(position = Vector2(115f, 520f), home = Vector2(115f, 520f)) else it
        })
        assertNotNull(game.legalPlayerPositionInZone("red-left", 1))
        game.beginPlayerMove("red-left")
        game.moveInactivePlayer("red-left", Vector2(0f, 120f))
        game.endPlayerMove("red-left")
        assertEquals(Vector2(40f, 520f), game.snapshot.players.first { it.id == "red-left" }.position)
        assertEquals(1, game.snapshot.actionsUsed)
    }
    @Test fun freeDragStillStopsAtBodiesBallAndFieldEdges() {
        val game = engine()
        ready(game)
        freeMovementFixture(game, Vector2(80f, 500f))
        game.beginPlayerMove("red-left")
        game.moveInactivePlayer("red-left", Vector2(0f, -1000f))
        game.endPlayerMove("red-left")
        assertTrue(game.snapshot.players.first { it.id == "red-left" }.position.y >= 25f)
        assertTrue(game.undoLastPlayerMove())
        set(game, "players", game.snapshot.players.map {
            if (it.id == "red-right") it.copy(position = Vector2(80f, 380f), home = Vector2(80f, 380f)) else it
        })
        game.beginPlayerMove("red-left")
        game.moveInactivePlayer("red-left", Vector2(0f, -300f))
        game.endPlayerMove("red-left")
        assertTrue(game.snapshot.players.first { it.id == "red-left" }.position.y >= 447.9f)
        assertTrue(game.undoLastPlayerMove())
        set(game, "ball", game.snapshot.ball.copy(position = Vector2(80f, 460f)))
        game.beginPlayerMove("red-left")
        game.moveInactivePlayer("red-left", Vector2(0f, -80f))
        game.endPlayerMove("red-left")
        assertTrue(game.snapshot.players.first { it.id == "red-left" }.position.y >= 498.9f)
    }
    @Test fun openingSetupStillCannotCrossHalfwayWithoutZoneLimits() {
        val game = engine()
        freeMovementFixture(game, Vector2(80f, 500f))
        game.beginPlayerMove("red-left")
        game.moveInactivePlayer("red-left", Vector2(0f, -400f))
        game.endPlayerMove("red-left")
        assertTrue(game.snapshot.players.first { it.id == "red-left" }.position.y >= 290f)
        assertEquals(0, game.snapshot.actionsUsed)
    }

    @Test fun freeMovementIsOptInForBothThreeAndFiveAside() {
        for (format in MatchFormat.entries) {
            for (enabled in listOf(false, true)) {
                val game = GameEngine(393f, 580f, format = format, threeActionTurns = enabled)
                assertEquals(if (format == MatchFormat.THREE_V_THREE) 6 else 10, game.snapshot.players.size)
                if (enabled) ready(game) else set(game, "openingKickoffPending", false)
                val movingId = if (enabled) "red-left" else "blue-left"
                val slots = listOf(60f, 145f, 230f, 500f).flatMap { y ->
                    listOf(Vector2(250f, y), Vector2(335f, y))
                }.iterator()
                set(game, "players", game.snapshot.players.map {
                    val position = when (it.id) {
                        movingId -> Vector2(80f, 500f)
                        "red-striker" -> Vector2(200f, 330f)
                        else -> slots.next()
                    }
                    it.copy(position = position, home = position)
                })
                set(game, "ball", game.snapshot.ball.copy(position = Vector2(200f, 290f)))
                assertTrue(game.canMovePlayer(movingId))
                game.beginPlayerMove(movingId)
                game.moveInactivePlayer(movingId, Vector2(0f, -400f))
                game.endPlayerMove(movingId)
                val destination = game.snapshot.players.first { it.id == movingId }.position
                if (enabled) {
                    assertEquals(Vector2(80f, 100f), destination, "$format with toggle ON")
                    assertEquals(1, game.snapshot.actionsUsed)
                } else {
                    assertTrue(destination.y > 300f, "$format with toggle OFF must retain neighboring-zone limits")
                    assertFalse(game.snapshot.threeActionTurns)
                }
            }
        }
    }

    @Test fun botUsesTheSameSetupAndThreeShotAllowance() = runBlocking {
        val game = engine(GameTeam.BLUE)
        val bot = BotPlanner(BotDifficulty.EASY)
        assertFalse(bot.controls(game.snapshot, GameTeam.BLUE))
        game.finishDefense()
        assertTrue(bot.controls(game.snapshot, GameTeam.BLUE))
        for (move in bot.chooseDefensePlan(game.copyForSimulation(), GameTeam.BLUE)) {
            assertTrue(game.movePlayerToZone(move.playerId, move.zone))
            assertEquals(0, game.snapshot.actionsUsed)
        }
        game.finishDefense()
        untilAiming(game)
        repeat(3) { action ->
            assertTrue(bot.controls(game.snapshot, GameTeam.BLUE))
            val shot = bot.chooseShot(game.copyForSimulation())
            game.setAimAngle(shot.angle)
            game.setPower(shot.power)
            assertTrue(game.shoot())
            assertEquals(action + 1, game.snapshot.actionsUsed)
            settleShot(game)
        }
        assertEquals(GameTeam.RED, team(game))
        assertFalse(bot.controls(game.snapshot, GameTeam.BLUE))
    }

    @Test fun simulationCopiesKeepSetupAndActionAllowanceIndependent() {
        val game = engine()
        val setup = game.copyForSimulation()
        setup.finishDefense()
        assertEquals(GameTeam.RED, game.snapshot.formationTeam)
        assertEquals(GameTeam.BLUE, setup.snapshot.formationTeam)
        ready(game)
        move(game)
        val copy = game.copyForSimulation()
        assertEquals(1, copy.snapshot.actionsUsed)
        move(copy)
        assertEquals(1, game.snapshot.actionsUsed)
        assertEquals(2, copy.snapshot.actionsUsed)
    }
}

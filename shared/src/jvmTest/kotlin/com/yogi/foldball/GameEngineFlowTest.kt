package com.yogi.foldball

import com.yogi.foldball.domain.game.engine.GameEngine
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchFormat
import com.yogi.foldball.domain.game.engine.MatchPlayer
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
        assertTrue(
            (oldKicker.position - ballBefore).length() >
                (newKicker.position - ballBefore).length()
        )
    }

    @Test
    fun defendingTeamMayMoveInItsHalfButCannotEnterKickingTeamsHalfBeforeKickoff() {
        val engine = GameEngine(393f, 580f, targetScore = null, initialKickoffTeam = GameTeam.RED)
        val defendingPlayers = engine.snapshot.players.filter { it.team == GameTeam.BLUE }
        assertTrue(engine.snapshot.openingKickoffPending)
        assertTrue(defendingPlayers.all { engine.canMovePlayer(it.id) })

        val bluePlayer = defendingPlayers.first { it.id == "blue-striker" }
        val start = bluePlayer.position
        assertTrue(engine.isZoneAvailableForPlayer(bluePlayer.id, 14))
        assertTrue(!engine.isZoneAvailableForPlayer(bluePlayer.id, 8))
        engine.beginPlayerMove(bluePlayer.id)
        engine.moveInactivePlayer(bluePlayer.id, Vector2(0f, 180f))
        engine.endPlayerMove(bluePlayer.id)

        val moved = engine.snapshot.players.first { it.id == bluePlayer.id }
        assertTrue(
            moved.position != start,
            "The defender can still reposition during opening kickoff"
        )
        assertTrue(moved.position.y < 580f / 2f, "Blue must remain in its own half")
        assertEquals(1, engine.defensiveMovesUsed)
        assertTrue(engine.snapshot.openingKickoffPending)
    }

    @Test
    fun defenseTimeoutCommitsCurrentLegalDragPositionOnce() {
        val engine = GameEngine(393f, 580f, targetScore = null, timing = MatchTimingConfig(1, 1))
        beginNextDefenseAfterOpeningKick(engine)

        val movable = engine.snapshot.players.first { engine.canMovePlayer(it.id) }
        val towardCenter = if (movable.position.x < 196f) Vector2(8f, 0f) else Vector2(-8f, 0f)
        engine.beginPlayerMove(movable.id)
        engine.moveInactivePlayer(movable.id, towardCenter)
        val draggedPosition = engine.snapshot.players.first { it.id == movable.id }.position
        assertTrue(draggedPosition != movable.position)

        repeat(4) { engine.step(.25f) }
        assertEquals(GamePhase.AIMING, engine.snapshot.phase)
        assertEquals(
            draggedPosition,
            engine.snapshot.players.first {
                it.id == movable.id
            }.position
        )
        assertEquals(1, engine.defensiveMovesUsed)

        engine.endPlayerMove(movable.id)
        assertEquals(1, engine.defensiveMovesUsed)
        engine.moveInactivePlayer(movable.id, towardCenter)
        assertEquals(
            draggedPosition,
            engine.snapshot.players.first {
                it.id == movable.id
            }.position
        )
    }

    @Test
    fun classicDefenderMoveCanBeUndoneBeforeConfirmation() {
        for (format in MatchFormat.entries) {
            val engine = GameEngine(393f, 580f, targetScore = null, format = format)
            val before = engine.snapshot.players
            val remaining = engine.snapshot.phaseTimeRemaining
            val player = before.first { engine.canMovePlayer(it.id) }
            val zone = (1..GameEngine.ZONE_COUNT).first { engine.legalPlayerPositionInZone(player.id, it) != null }
            assertTrue(engine.movePlayerToZone(player.id, zone))
            assertEquals(GamePhase.DEFENDING, engine.snapshot.phase)
            assertEquals(1, engine.defensiveMovesUsed)
            assertTrue(engine.canUndoPlayerMove())
            assertTrue(engine.snapshot.players.none { engine.canMovePlayer(it.id) })
            assertTrue(engine.undoLastPlayerMove())
            assertEquals(before, engine.snapshot.players)
            assertEquals(remaining, engine.snapshot.phaseTimeRemaining)
            assertEquals(0, engine.defensiveMovesUsed)
            assertTrue(engine.canMovePlayer(player.id))
            assertTrue(engine.movePlayerToZone(player.id, zone))
            assertTrue(engine.finishDefense())
            assertEquals(GamePhase.AIMING, engine.snapshot.phase)
            assertTrue(!engine.canUndoPlayerMove())
            assertTrue(!engine.undoLastPlayerMove())
        }
    }

    @Test
    fun classicMovedDefenderConfirmsWhenFormationTimerExpires() {
        for (format in MatchFormat.entries) {
            val engine = GameEngine(393f, 580f, format = format, timing = MatchTimingConfig(1, 12))
            val player = engine.snapshot.players.first { engine.canMovePlayer(it.id) }
            val zone = (1..GameEngine.ZONE_COUNT).first { engine.legalPlayerPositionInZone(player.id, it) != null }
            assertTrue(engine.movePlayerToZone(player.id, zone))
            val moved = engine.snapshot.players.first { it.id == player.id }
            repeat(4) { engine.step(.25f) }
            assertEquals(GamePhase.AIMING, engine.snapshot.phase)
            assertEquals(moved, engine.snapshot.players.first { it.id == player.id })
            assertTrue(!engine.canUndoPlayerMove())
        }
    }

    @Test
    fun ballNearGoalFinishesPlacementBeforeNextKickerApproaches() {
        val engine = GameEngine(393f, 580f, targetScore = null)
        GameEngine::class.java.getDeclaredField("focusedPlayerId").apply { isAccessible = true }
            .set(engine, "blue-striker")
        GameEngine::class.java.getDeclaredField("ball").apply { isAccessible = true }
            .set(engine, engine.snapshot.ball.copy(position = Vector2(196.5f, 28f)))
        GameEngine::class.java.getDeclaredMethod(
            "beginKickerApproach",
            Boolean::class.javaPrimitiveType
        ).apply { isAccessible = true }.invoke(engine, false)

        val kickerStart = engine.snapshot.players.first { it.id == "blue-striker" }.position
        repeat(12) { engine.step(1f / 120f) }
        assertEquals(GamePhase.KICKER_APPROACH, engine.snapshot.phase)
        assertEquals(kickerStart, engine.snapshot.players.first { it.id == "blue-striker" }.position)
        assertTrue(engine.snapshot.ball.position.y > 28f)

        repeat(14) { engine.step(1f / 120f) }
        assertEquals(kickerStart, engine.snapshot.players.first { it.id == "blue-striker" }.position)
        repeat(12) { engine.step(1f / 120f) }
        assertTrue(engine.snapshot.players.first { it.id == "blue-striker" }.position != kickerStart)
    }

    @Test
    fun chargedKickReleasesAutomaticallyWhenAimingTimesOut() {
        val engine = GameEngine(393f, 580f, targetScore = null, timing = MatchTimingConfig(1, 1))
        engine.finishDefense()
        engine.setPower(.65f) // Treat this as a partially charged kick still being held.

        repeat(4) { engine.step(.25f) }
        assertEquals(GamePhase.BALL_MOVING, engine.snapshot.phase)
        assertEquals(.65f, engine.snapshot.lastShotPower)
        assertTrue(engine.snapshot.ball.velocity.length() > 0f)
        assertTrue(!engine.shoot()) // Releasing after timeout cannot launch the ball.
        assertTrue(!engine.snapshot.openingKickoffPending)
    }

    @Test
    fun blockedDragDoesNotTeleportToAdvertisedZone() {
        for (blockerX in listOf(145f, 170f)) {
            for (incremental in listOf(false, true)) {
                val engine = GameEngine(393f, 580f, targetScore = null)
                val playerField = GameEngine::class.java.getDeclaredField("players")
                playerField.isAccessible = true
                playerField.set(
                    engine,
                    engine.snapshot.players.map { player ->
                        val position = when (player.id) {
                            "blue-left" -> Vector2(65f, 145f)

                            "blue-striker" -> Vector2(blockerX, 205f)

                            // Outside zone 14, near its center path.
                            "blue-center" -> Vector2(327f, 80f)

                            else -> player.position
                        }
                        player.copy(position = position, home = position)
                    }
                )

                val preview = engine.legalPlayerPositionInZone("blue-left", 14)
                assertTrue(preview != null)
                engine.beginPlayerMove("blue-left")
                // Both a large pointer event and a sequence of smaller events must
                // settle at the highlighted safe point when the center is obstructed.
                repeat(if (incremental) 131 else 1) {
                    engine.moveInactivePlayer(
                        "blue-left",
                        Vector2(if (incremental) 1f else 131f, 0f)
                    )
                }
                val moved = engine.snapshot.players.first { it.id == "blue-left" }.position
                assertTrue((moved - Vector2(65f, 145f)).length() <= 131.1f)
                // After entering the safe edge, further legal pointer events may
                // move freely within the target zone rather than staying snapped.
                assertEquals(145f, moved.y, .01f, "Horizontal drag must not snap vertically")
                assertTrue((moved - Vector2(blockerX, 205f)).length() >= 68f)
                engine.endPlayerMove("blue-left")
                assertEquals(1, engine.defensiveMovesUsed)
                assertTrue(!engine.canMovePlayer("blue-left"))
                assertTrue(engine.undoLastPlayerMove())
                assertEquals(
                    Vector2(65f, 145f),
                    engine.snapshot.players.first {
                        it.id == "blue-left"
                    }.position
                )
            }
        }
    }

    @Test
    fun clearDragTargetInHighlightedZoneFollowsPointerInsteadOfSnapping() {
        val engine = GameEngine(393f, 580f, targetScore = null)
        val players = GameEngine::class.java.getDeclaredField("players").apply {
            isAccessible = true
        }
        players.set(
            engine,
            engine.snapshot.players.map { player ->
                val position = when (player.id) {
                    "blue-left" -> Vector2(65f, 145f)
                    "blue-striker" -> Vector2(170f, 205f)
                    "blue-center" -> Vector2(327f, 80f)
                    else -> player.position
                }
                player.copy(position = position, home = position)
            }
        )
        val safePreview = engine.legalPlayerPositionInZone("blue-left", 14)
        assertTrue(safePreview != null)
        val pointerTarget = Vector2(196f, 115f)
        assertTrue(safePreview != pointerTarget)
        engine.beginPlayerMove("blue-left")
        engine.moveInactivePlayer("blue-left", Vector2(65f, -15f))
        engine.moveInactivePlayer("blue-left", Vector2(66f, -15f))
        assertEquals(pointerTarget, engine.snapshot.players.first { it.id == "blue-left" }.position)
        assertEquals(0, engine.defensiveMovesUsed, "Movement is committed only when released")
        engine.endPlayerMove("blue-left")
        assertEquals(1, engine.defensiveMovesUsed)
    }

    @Test
    fun emptyZoneBehindBallIsNotHighlightedWhenDragPathIsBlocked() {
        val engine = GameEngine(393f, 580f, targetScore = null)
        val playerField = GameEngine::class.java.getDeclaredField("players")
        playerField.isAccessible = true
        playerField.set(
            engine,
            engine.snapshot.players.map { player ->
                val position = when (player.id) {
                    "blue-left" -> Vector2(65f, 145f)
                    "blue-center" -> Vector2(327f, 80f)
                    "blue-striker" -> Vector2(327f, 240f)
                    else -> player.position
                }
                player.copy(position = position, home = position)
            }
        )
        val ballField = GameEngine::class.java.getDeclaredField("ball")
        ballField.isAccessible = true
        ballField.set(engine, engine.snapshot.ball.copy(position = Vector2(131f, 145f)))

        assertTrue(engine.isZoneAvailableForPlayer("blue-left", 14))
        assertEquals(null, engine.legalPlayerPositionInZone("blue-left", 14))
    }

    @Test
    fun ballImpactFramesKeepVisiblePlayerBodiesSeparated() {
        val engine = GameEngine(393f, 580f, targetScore = null)
        engine.finishDefense()
        engine.setPower(.6f)
        assertTrue(engine.shoot())
        val playerField = GameEngine::class.java.getDeclaredField("players")
        playerField.isAccessible = true
        playerField.set(
            engine,
            engine.snapshot.players.map { player ->
                val position = when (player.id) {
                    "red-center" -> Vector2(185f, 450f)
                    "red-striker" -> Vector2(220f, 450f)
                    else -> player.position
                }
                player.copy(position = position, home = position)
            }
        )

        engine.step(1f / 60f)
        assertPlayersSeparated(engine.snapshot.players)
    }

    @Test
    fun kickerApproachAvoidsPlayerStandingBehindBall() {
        val engine = GameEngine(393f, 580f, targetScore = null)
        val playerField = GameEngine::class.java.getDeclaredField("players")
        playerField.isAccessible = true
        playerField.set(
            engine,
            engine.snapshot.players.map { player ->
                val position = when (player.id) {
                    "blue-striker" -> Vector2(196f, 330f)
                    "red-center" -> Vector2(196f, 249f)
                    "red-striker" -> Vector2(65f, 330f)
                    else -> player.position
                }
                player.copy(position = position, home = position)
            }
        )
        val ballField = GameEngine::class.java.getDeclaredField("ball")
        ballField.isAccessible = true
        ballField.set(
            engine,
            engine.snapshot.ball.copy(
                position = Vector2(196f, 290f),
                velocity = Vector2(0f, 0f)
            )
        )
        val phaseField = GameEngine::class.java.getDeclaredField("phase")
        phaseField.isAccessible = true
        phaseField.set(engine, GamePhase.BALL_MOVING)

        engine.step(1f / 60f)
        assertEquals(GamePhase.KICKER_APPROACH, engine.snapshot.phase)
        repeat(20) {
            engine.step(1f / 30f)
            assertPlayersSeparated(engine.snapshot.players)
        }
        assertEquals(GamePhase.DEFENDING, engine.snapshot.phase)
        assertEquals("blue-striker", engine.snapshot.focusedPlayerId)
    }

    @Test
    fun repeatedMovesAndShotsKeepPlayersApartAndHighlightedZonesReachable() {
        val engine = GameEngine(393f, 580f, targetScore = null)
        repeat(8) { turn ->
            assertEquals(GamePhase.DEFENDING, engine.snapshot.phase)
            val movable = engine.snapshot.players.firstOrNull { engine.canMovePlayer(it.id) }
            if (movable != null) {
                val destination = (1..GameEngine.ZONE_COUNT).firstNotNullOfOrNull { zone ->
                    if (!engine.isAdjacentZoneForPlayer(movable.id, zone)) {
                        null
                    } else {
                        engine.legalPlayerPositionInZone(movable.id, zone)
                            ?.takeIf { (it - movable.position).length() > 5f }
                    }
                }
                if (destination != null) {
                    engine.beginPlayerMove(movable.id)
                    engine.moveInactivePlayer(movable.id, destination - movable.position)
                    engine.endPlayerMove(movable.id)
                    val moved = engine.snapshot.players.first { it.id == movable.id }
                    assertEquals(destination, moved.position)
                    assertPlayersSeparated(engine.snapshot.players)
                }
            }
            engine.finishDefense()
            engine.aimBy(if (turn % 2 == 0) 24f else -28f)
            engine.setPower(.45f + .05f * (turn % 3))
            assertTrue(engine.shoot())
            var frames = 0
            while (engine.snapshot.phase != GamePhase.DEFENDING && frames++ < 3_000) {
                engine.step(1f / 30f)
                assertPlayersSeparated(engine.snapshot.players)
            }
            assertTrue(frames < 3_000, "Turn $turn did not finish")
        }
    }

    @Test
    fun goalRestoresOpeningFormationBeforeConcedingTeamKickoff() {
        val engine = GameEngine(393f, 580f, targetScore = null)
        val original = engine.snapshot.players.associate { it.id to it.position }
        assertTrue(engine.finishDefense())
        engine.setPower(1f)
        assertTrue(engine.shoot())

        // Put an in-flight ball just inside the red scoring line. The production
        // collision, goal, celebration, and kickoff transitions then run normally.
        val ballField = GameEngine::class.java.getDeclaredField("ball")
        ballField.isAccessible = true
        val ball = engine.snapshot.ball
        ballField.set(
            engine,
            ball.copy(
                position = Vector2(196.5f, 29f),
                velocity = Vector2(0f, -900f),
                height = 0f,
                verticalVelocity = 0f
            )
        )
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
        ballField.set(
            engine,
            nextBall.copy(
                position = Vector2(196.5f, 551f),
                velocity = Vector2(0f, 900f),
                height = 0f,
                verticalVelocity = 0f
            )
        )
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
        assertEquals(
            original.getValue("red-striker"),
            engine.snapshot.players.first { it.id == "red-striker" }.position
        )
        assertEquals(
            original.getValue("blue-striker"),
            engine.snapshot.players.first { it.id == "blue-striker" }.position
        )
        for (player in engine.snapshot.players.filterNot { it.id.endsWith("striker") }) {
            assertEquals(original.getValue(player.id), player.position, player.id)
        }
    }

    private fun beginNextDefenseAfterOpeningKick(engine: GameEngine) {
        assertTrue(engine.snapshot.openingKickoffPending)
        assertTrue(engine.finishDefense())
        engine.setPower(1f)
        assertTrue(engine.shoot())
        var ready = false
        repeat(20_000) {
            if (!ready) {
                val state = engine.step(1f / 60f)
                ready = state.phase == GamePhase.DEFENDING && !state.openingKickoffPending
            }
        }
        assertTrue(ready, "Opening kick should lead into the first live defensive formation")
    }

    private fun assertPlayersSeparated(players: List<MatchPlayer>) {
        for (firstIndex in 0 until players.lastIndex) {
            for (secondIndex in firstIndex + 1 until players.size) {
                val first = players[firstIndex]
                val second = players[secondIndex]
                assertTrue(
                    (first.position - second.position).length() >= 67.9f,
                    "${first.id} overlaps ${second.id}"
                )
            }
        }
    }
}

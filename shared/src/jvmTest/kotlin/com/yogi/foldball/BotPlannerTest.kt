package com.yogi.foldball

import com.yogi.foldball.domain.game.bot.*
import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*
import kotlinx.coroutines.runBlocking

class BotPlannerTest {
    @Test fun planningDoesNotMutateLiveEngineAndShotCanBeReplayed() = runBlocking {
        val engine = GameEngine(393f, 580f, initialKickoffTeam = GameTeam.BLUE)
        engine.finishDefense()
        val before = engine.snapshot
        val planner = BotPlanner()
        val shot = planner.chooseShot(engine.copyForSimulation())
        assertEquals(before, engine.snapshot)
        assertTrue(shot.angle in -1.05f..1.05f)
        assertTrue(shot.power in .05f..1f)
        val prediction = engine.copyForSimulation()
        for (game in listOf(engine, prediction)) {
            game.setAimAngle(shot.angle)
            game.setPower(shot.power)
            assertTrue(game.shoot())
        }
        repeat(120) {
            assertEquals(engine.step(1f / 120f), prediction.step(1f / 120f))
        }
    }

    @Test fun defenseRespectsOwnHalfMoveLimitAndIndependentHistory() {
        val engine = GameEngine(393f, 580f)
        val bot = BotPlanner()
        val original = engine.snapshot
        val trial = engine.copyForSimulation()
        repeat(2) {
            bot.chooseDefense(trial, GameTeam.BLUE)?.let {
                assertTrue(trial.movePlayerToZone(it.playerId, it.zone))
            }
        }
        assertEquals(original, engine.snapshot)
        assertEquals(0, engine.defensiveMovesUsed)
        assertTrue(trial.defensiveMovesUsed <= 2)
        assertTrue(
            trial.snapshot.players.filter { it.team == GameTeam.BLUE }.all {
                it.position.y <=
                    290f
            }
        )
        if (trial.defensiveMovesUsed > 0) {
            val moved = trial.snapshot
            val copied = trial.copyForSimulation()
            assertTrue(copied.undoLastPlayerMove())
            assertEquals(moved, trial.snapshot)
        }
    }

    @Test fun botControlsOnlyItsOwnDefenseAndAttackPhases() {
        val bot = BotPlanner()
        for (kickoff in GameTeam.entries) {
            val engine = GameEngine(393f, 580f, initialKickoffTeam = kickoff)
            assertEquals(kickoff != GameTeam.BLUE, bot.controls(engine.snapshot, GameTeam.BLUE))
            engine.finishDefense()
            assertEquals(kickoff == GameTeam.BLUE, bot.controls(engine.snapshot, GameTeam.BLUE))
            engine.setPower(.8f)
            engine.shoot()
            assertFalse(bot.controls(engine.snapshot, GameTeam.BLUE))
        }
    }

    @Test fun everyLevelProducesLegalShotsAndDefenseWithoutChangingLiveState() = runBlocking {
        for (difficulty in BotDifficulty.entries) {
            for (kickoff in GameTeam.entries) {
                val engine = GameEngine(393f, 580f, initialKickoffTeam = kickoff)
                val defender = if (kickoff == GameTeam.RED) GameTeam.BLUE else GameTeam.RED
                val before = engine.snapshot
                val planner = BotPlanner(difficulty, kotlin.random.Random(123))
                val moves = planner.chooseDefensePlan(engine, defender)
                assertEquals(before, engine.snapshot)
                assertTrue(moves.size <= if (difficulty == BotDifficulty.EASY) 1 else 2)
                assertEquals(moves.size, moves.map { it.playerId }.distinct().size)
                for (move in moves) {
                    assertTrue(engine.canMovePlayer(move.playerId))
                    assertTrue(engine.isAdjacentZoneForPlayer(move.playerId, move.zone))
                    assertTrue(engine.movePlayerToZone(move.playerId, move.zone))
                }
                assertTrue(
                    engine.snapshot.players.filter { it.team == defender }.all {
                        if (defender ==
                            GameTeam.RED
                        ) {
                            it.position.y >= 290f
                        } else {
                            it.position.y <= 290f
                        }
                    }
                )
                engine.finishDefense()
                val beforeShot = engine.snapshot
                val shot = planner.chooseShot(engine)
                assertEquals(beforeShot, engine.snapshot)
                assertTrue(shot.angle in -1.05f..1.05f)
                assertTrue(shot.power in .05f..1f)
                engine.setAimAngle(shot.angle)
                engine.setPower(shot.power)
                assertTrue(engine.shoot())
            }
        }
    }

    @Test fun mediumAndHardFindOffCenterGoalsForBothAttackDirections() = runBlocking {
        for (difficulty in listOf(BotDifficulty.MEDIUM, BotDifficulty.HARD)) {
            for (team in GameTeam.entries) {
                val engine = GameEngine(393f, 580f, initialKickoffTeam = team)
                val players = GameEngine::class.java.getDeclaredField("players").apply {
                    isAccessible =
                        true
                }
                players.set(
                    engine,
                    engine.snapshot.players.mapIndexed { index, player ->
                        val pos = Vector2(if (index % 2 == 0) 35f else 358f, 40f + index * 65f)
                        player.copy(position = pos, home = pos)
                    }
                )
                val ball = GameEngine::class.java.getDeclaredField("ball").apply {
                    isAccessible =
                        true
                }
                ball.set(
                    engine,
                    engine.snapshot.ball.copy(
                        position = Vector2(
                            150f,
                            if (team ==
                                GameTeam.RED
                            ) {
                                90f
                            } else {
                                490f
                            }
                        )
                    )
                )
                engine.finishDefense()
                val shot = BotPlanner(difficulty).chooseShot(engine)
                engine.setAimAngle(shot.angle)
                engine.setPower(shot.power)
                engine.shoot()
                var frames = 0
                while (engine.snapshot.phase == GamePhase.BALL_MOVING &&
                    frames++ < 1440
                ) {
                    engine.step(1f / 120f)
                }
                assertEquals(
                    GamePhase.GOAL,
                    engine.snapshot.phase,
                    "$difficulty $team should score"
                )
                assertEquals(team, engine.snapshot.lastScoringTeam)
            }
        }
    }

    @Test fun easyAndMediumVaryRepeatedShotsWithoutMutatingLiveState() = runBlocking {
        val engine = GameEngine(393f, 580f, physicsTuning = com.yogi.foldball.domain.game.engine.PhysicsTuning(wallBounce = .55f, groundBounce = .18f))
        engine.finishDefense()
        val easy = BotPlanner(BotDifficulty.EASY, kotlin.random.Random(42), maxThinkingMillis = 6000)
        val easyShots = (1..4).map { easy.chooseShot(engine) }
        assertTrue(easyShots.distinct().size > 1)
        val original = engine.snapshot
        val medium = BotPlanner(BotDifficulty.MEDIUM, kotlin.random.Random(42), maxThinkingMillis = 6000)
        val mediumShots = (1..4).map { medium.chooseShot(engine) }
        assertTrue(mediumShots.distinct().size > 1)
        assertEquals(original, engine.snapshot)
    }

    @Test fun hardCanPlanRepeatedLiveRalliesWithoutTimeoutOrIllegalMoves() = runBlocking {
        val engine = GameEngine(393f, 580f, targetScore = null)
        val planner = BotPlanner(BotDifficulty.HARD)
        repeat(6) { turn ->
            assertEquals(GamePhase.DEFENDING, engine.snapshot.phase)
            val attacker = engine.snapshot.players.first {
                it.id == engine.snapshot.focusedPlayerId
            }.team
            val defender = if (attacker == GameTeam.RED) GameTeam.BLUE else GameTeam.RED
            for (move in planner.chooseDefensePlan(engine, defender)) {
                assertTrue(engine.movePlayerToZone(move.playerId, move.zone))
            }
            assertTrue(engine.defensiveMovesUsed <= 2)
            engine.finishDefense()
            val shot = planner.chooseShot(engine)
            engine.setAimAngle(shot.angle)
            engine.setPower(shot.power)
            assertTrue(engine.shoot())
            var frames = 0
            while (engine.snapshot.phase != GamePhase.DEFENDING &&
                frames++ < 3000
            ) {
                engine.step(1f / 120f)
            }
            assertTrue(frames < 3000, "Hard rally $turn did not resolve")
        }
    }

    @Test fun mediumBotChoosesGoalWhenDirectShotIsAvailable() = runBlocking {
        val engine = GameEngine(393f, 580f, initialKickoffTeam = GameTeam.BLUE)
        val players = GameEngine::class.java.getDeclaredField("players").apply {
            isAccessible = true
        }
        players.set(
            engine,
            engine.snapshot.players.mapIndexed { index, player ->
                val pos = Vector2(if (index % 2 == 0) 40f else 353f, 50f + index * 60f)
                player.copy(position = pos, home = pos)
            }
        )
        val ball = GameEngine::class.java.getDeclaredField("ball").apply { isAccessible = true }
        ball.set(engine, engine.snapshot.ball.copy(position = Vector2(196.5f, 490f)))
        engine.finishDefense()
        val shot = BotPlanner().chooseShot(engine)
        engine.setAimAngle(shot.angle)
        engine.setPower(shot.power)
        engine.shoot()
        var frames = 0
        while (engine.snapshot.phase == GamePhase.BALL_MOVING &&
            frames++ < 1440
        ) {
            engine.step(1f / 120f)
        }
        assertEquals(GamePhase.GOAL, engine.snapshot.phase)
        assertEquals(GameTeam.BLUE, engine.snapshot.lastScoringTeam)
    }
}

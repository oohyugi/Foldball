package com.yogi.foldball
import com.yogi.foldball.domain.game.bot.*
import com.yogi.foldball.domain.game.engine.*
import kotlin.random.Random
import kotlin.test.*
import kotlinx.coroutines.runBlocking
class DamageMatchTest {
    @Test fun damagedMatchCompletesWithPersistentDamageAndFreshRematch() {
        val e = GameEngine(393f, 580f, targetScore = 2, dentingEnabled = true)
        val playersField = GameEngine::class.java.getDeclaredField("players").apply {
            isAccessible = true
        }
        val ballField = GameEngine::class.java.getDeclaredField("ball").apply {
            isAccessible = true
        }
        repeat(2) { round ->
            var frames = 0
            while (e.currentPhase != GamePhase.DEFENDING && frames++ < 1000) e.step(1f / 120f)
            assertTrue(
                e.snapshot.players.all {
                    it.condition.progress == if (round ==
                        0
                    ) {
                        0f
                    } else {
                        .5f
                    }
                }
            )
            val kicker = e.snapshot.focusedPlayerId
            val team = e.snapshot.players.first { it.id == kicker }.team
            playersField.set(
                e,
                e.snapshot.players.mapIndexed { i, p ->
                    p.copy(
                        position = Vector2(40f + i * 40f, 300f),
                        home = Vector2(40f + i * 40f, 300f),
                        condition = StandeeCondition(.5f)
                    )
                }
            )
            ballField.set(
                e,
                e.snapshot.ball.copy(
                    position = Vector2(
                        196.5f,
                        if (team ==
                            GameTeam.RED
                        ) {
                            93f
                        } else {
                            487f
                        }
                    )
                )
            )
            e.finishDefense()
            e.setPower(.5f)
            assertTrue(e.shoot())
            frames = 0
            while (e.currentPhase == GamePhase.BALL_MOVING && frames++ < 2000) e.step(1f / 120f)
            assertEquals(GamePhase.GOAL, e.currentPhase)
            if (round == 0) {
                repeat(300) { e.step(1f / 120f) }
                assertTrue(e.snapshot.players.all { it.condition.progress == .5f })
                // Set the next kickoff team to RED to finish a reproducible two-goal match.
                GameEngine::class.java.getDeclaredField("focusedPlayerId").apply {
                    isAccessible = true
                }.set(e, "red-striker")
            }
        }
        repeat(300) { e.step(1f / 120f) }
        assertEquals(GamePhase.MATCH_OVER, e.currentPhase)
        assertNotNull(e.snapshot.winner)
        assertTrue(
            GameEngine(393f, 580f, dentingEnabled = true).snapshot.players.all {
                it.condition.progress ==
                    0f
            }
        )
    }

    @Test fun allStylesAndLevelsPlanDamagedKickerWithoutMutatingMatch() = runBlocking {
        for (level in BotDifficulty.entries) {
            for (style in BotStyle.entries) {
                val e =
                    GameEngine(
                        393f,
                        580f,
                        initialKickoffTeam = GameTeam.BLUE,
                        dentingEnabled = true
                    )
                GameEngine::class.java.getDeclaredField("players").apply {
                    isAccessible = true
                }.set(e, e.snapshot.players.map { it.copy(condition = StandeeCondition(.7f)) })
                e.finishDefense()
                val before = e.snapshot
                val shot = BotPlanner(level, Random(42), style = style).chooseShot(e)
                assertEquals(before, e.snapshot)
                assertEquals(.7f, e.focusedDentProgress)
                e.setAimAngle(shot.angle)
                e.setPower(shot.power)
                assertTrue(e.shoot())
            }
        }
    }

    @Test fun repeatedDamagedRalliesStayFiniteAndProgressThroughLegalPhases() = runBlocking {
        val engine = GameEngine(393f, 580f, targetScore = null, dentingEnabled = true)
        val planners = GameTeam.entries.associateWith {
            BotPlanner(BotDifficulty.MEDIUM, Random(it.ordinal + 20), style = BotStyle.BALANCED)
        }
        var shots = 0
        var dentedContacts = 0
        repeat(20) {
            var frames = 0
            while (engine.currentPhase != GamePhase.DEFENDING &&
                frames++ < 3000
            ) {
                engine.step(1f / 120f)
            }
            assertTrue(frames < 3000, "Rally must settle")
            val attacker = engine.snapshot.players.first {
                it.id == engine.snapshot.focusedPlayerId
            }.team
            val defender = if (attacker == GameTeam.RED) GameTeam.BLUE else GameTeam.RED
            val planner = planners.getValue(defender)
            for (move in planner.chooseDefensePlan(
                engine,
                defender
            )) {
                assertTrue(engine.movePlayerToZone(move.playerId, move.zone))
            }
            engine.finishDefense()
            val shot = planners.getValue(attacker).chooseShot(engine)
            val execution = BotAimExecution.sample(
                shot,
                BotDifficulty.MEDIUM,
                .25f,
                -.4f,
                engine.focusedDentProgress
            )
            engine.setAimAngle(execution.angle)
            engine.setPower(execution.power)
            assertTrue(engine.shoot())
            shots++
            frames = 0
            while (engine.currentPhase == GamePhase.BALL_MOVING && frames++ < 3000) {
                val state = engine.step(1f / 120f)
                assertTrue(state.ball.position.x.isFinite() && state.ball.position.y.isFinite())
                assertTrue(state.players.all { it.condition.progress in 0f..1f })
            }
            assertTrue(frames < 3000, "Ball must not remain stuck in motion")
            dentedContacts += engine.snapshot.players.count { it.condition.dented }
        }
        assertEquals(20, shots)
        println(
            "DAMAGE_RALLIES shots=$shots score=${engine.snapshot.redScore}:${engine.snapshot.blueScore} damagedPlayerObservations=$dentedContacts"
        )
    }
}

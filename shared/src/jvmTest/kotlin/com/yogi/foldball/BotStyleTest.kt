package com.yogi.foldball
import com.yogi.foldball.domain.game.bot.*
import com.yogi.foldball.domain.game.engine.*
import kotlin.random.Random
import kotlin.test.*
import kotlinx.coroutines.runBlocking

class BotStyleTest {
    @Test fun stylesProduceLegalMovesAndShotsWithoutChangingLiveState() = runBlocking {
        for (style in BotStyle.entries) {
            val defense = GameEngine(393f, 580f)
            val original = defense.snapshot
            val planner = BotPlanner(BotDifficulty.MEDIUM, Random(42), style = style)
            val moves = planner.chooseDefensePlan(defense, GameTeam.BLUE)
            assertEquals(original, defense.snapshot)
            val trial = defense.copyForSimulation()
            for (move in moves) assertTrue(trial.movePlayerToZone(move.playerId, move.zone))
            assertTrue(moves.size <= 2)
            val attack = GameEngine(393f, 580f, initialKickoffTeam = GameTeam.BLUE)
            attack.finishDefense()
            val before = attack.snapshot
            val shot = planner.chooseShot(attack)
            assertEquals(before, attack.snapshot)
            attack.setAimAngle(shot.angle)
            attack.setPower(shot.power)
            assertTrue(attack.shoot())
        }
    }

    @Test fun parkBusClosesActualShotLanesForBothTeamsInsteadOfRetreating() {
        for (defending in GameTeam.entries) {
            val attacker = if (defending == GameTeam.BLUE) GameTeam.RED else GameTeam.BLUE
            val engine = GameEngine(393f, 580f, initialKickoffTeam = attacker, format = MatchFormat.THREE_V_THREE)
            fun mirror(p: Vector2) = if (defending == GameTeam.BLUE) p else Vector2(p.x, 580f - p.y)
            var defenseIndex = 0
            var attackIndex = 0
            val defenders = listOf(Vector2(65f, 145f), Vector2(327f, 145f), Vector2(65f, 80f))
            val attackers = listOf(Vector2(196.5f, 270f), Vector2(65f, 435f), Vector2(327f, 435f))
            val players = engine.snapshot.players.map {
                val p = mirror(if (it.team == defending) defenders[defenseIndex++] else attackers[attackIndex++])
                it.copy(position = p, home = p)
            }
            GameEngine::class.java.getDeclaredField("players").apply { isAccessible = true }.set(engine, players)
            GameEngine::class.java.getDeclaredField("ball").apply { isAccessible = true }
                .set(engine, engine.snapshot.ball.copy(position = mirror(Vector2(196.5f, 230f))))
            val original = engine.snapshot
            val planner = BotPlanner(BotDifficulty.MEDIUM, Random(42), style = BotStyle.PARK_BUS)
            val move = assertNotNull(planner.chooseDefense(engine, defending))
            assertEquals(original, engine.snapshot)
            val moved = engine.copyForSimulation()
            assertTrue(moved.movePlayerToZone(move.playerId, move.zone))
            assertEquals(1, moved.defensiveMovesUsed)
            val directGap = moved.snapshot.players.filter { it.team == defending &&
                (if (defending == GameTeam.BLUE) it.position.y < 230f else it.position.y > 350f)
            }.minOf { kotlin.math.abs(it.position.x - 196.5f) }
            assertTrue(directGap <= moved.standeeBallContactDistance,
                "Park the Bus must close the direct shot for $defending; move=$move, gap=$directGap")
            fun directShotScores(position: GameEngine): Boolean {
                val shot = position.copyForSimulation()
                shot.finishDefense()
                shot.setAimAngle(0f)
                shot.setPower(.55f)
                assertTrue(shot.shoot())
                repeat(360) {
                    if (shot.currentPhase == GamePhase.BALL_MOVING) shot.step(1f / 120f)
                }
                return shot.snapshot.redScore + shot.snapshot.blueScore > 0
            }
            assertTrue(directShotScores(engine), "Fixture must expose a scoring shot for $defending")
            assertFalse(directShotScores(moved), "Chosen move must block that shot for $defending")
        }
    }

    @Test fun hardDefenseStopsAnExposedControlledGoalForBothTeams() = kotlinx.coroutines.runBlocking {
        for (defending in GameTeam.entries) {
            val attacker = if (defending == GameTeam.BLUE) GameTeam.RED else GameTeam.BLUE
            val engine = GameEngine(393f, 580f, initialKickoffTeam = attacker, format = MatchFormat.THREE_V_THREE)
            fun mirror(p: Vector2) = if (defending == GameTeam.BLUE) p else Vector2(p.x, 580f - p.y)
            var defenseIndex = 0
            var attackIndex = 0
            val defenders = listOf(Vector2(65f, 145f), Vector2(327f, 145f), Vector2(65f, 80f))
            val attackers = listOf(Vector2(196.5f, 270f), Vector2(65f, 435f), Vector2(327f, 435f))
            val players = engine.snapshot.players.map {
                val p = mirror(if (it.team == defending) defenders[defenseIndex++] else attackers[attackIndex++])
                it.copy(position = p, home = p)
            }
            GameEngine::class.java.getDeclaredField("players").apply { isAccessible = true }.set(engine, players)
            GameEngine::class.java.getDeclaredField("ball").apply { isAccessible = true }
                .set(engine, engine.snapshot.ball.copy(position = mirror(Vector2(196.5f, 230f))))
            val original = engine.snapshot
            val planner = BotPlanner(BotDifficulty.HARD, Random(42), maxThinkingMillis = 6000, style = BotStyle.PARK_BUS)
            val move = assertNotNull(planner.chooseDefensePlan(engine, defending).firstOrNull())
            assertEquals(original, engine.snapshot)
            val moved = engine.copyForSimulation()
            assertTrue(moved.movePlayerToZone(move.playerId, move.zone))
            assertEquals(1, moved.defensiveMovesUsed)
            val directGap = moved.snapshot.players.filter { it.team == defending &&
                (if (defending == GameTeam.BLUE) it.position.y < 230f else it.position.y > 350f)
            }.minOf { kotlin.math.abs(it.position.x - 196.5f) }
            assertTrue(directGap <= moved.standeeBallContactDistance,
                "Park the Bus must close the direct shot for $defending; move=$move, gap=$directGap")
            fun directShotScores(position: GameEngine): Boolean {
                val shot = position.copyForSimulation()
                shot.finishDefense()
                shot.setAimAngle(0f)
                shot.setPower(.55f)
                assertTrue(shot.shoot())
                repeat(360) {
                    if (shot.currentPhase == GamePhase.BALL_MOVING) shot.step(1f / 120f)
                }
                return shot.snapshot.redScore + shot.snapshot.blueScore > 0
            }
            assertTrue(directShotScores(engine), "Fixture must expose a scoring shot for $defending")
            assertFalse(directShotScores(moved), "Chosen move must block that shot for $defending")
        }
    }

    @Test fun tacticalPrioritiesChangeDefenseChoices() {
        val engine = GameEngine(393f, 580f)
        val choices = BotStyle.entries.map {
            BotPlanner(
                BotDifficulty.MEDIUM,
                Random(42),
                style = it
            ).chooseDefense(engine, GameTeam.BLUE)
        }
        assertTrue(
            choices.distinct().size > 1,
            "Styles should not all choose the same defensive move"
        )
    }
}

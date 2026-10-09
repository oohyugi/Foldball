package com.yogi.foldball
import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*

class ShotLoftTest {
    private fun shot(team: GameTeam, power: Float): GameEngine {
        val engine = GameEngine(393f, 580f, initialKickoffTeam = team)
        engine.finishDefense()
        // Isolate the goal approach, retaining the shooter but clearing the path.
        val players = engine.snapshot.players.map { player ->
            player.copy(position = Vector2(45f, 290f), home = Vector2(45f, 290f))
        }
        GameEngine::class.java.getDeclaredField("players").apply {
            isAccessible = true
        }.set(engine, players)
        val goalY = if (team == GameTeam.RED) 28f else 552f
        val ball = engine.snapshot.ball.copy(
            position = Vector2(
                196.5f,
                goalY + if (team == GameTeam.RED) 65f else -65f
            ),
            height = 0f,
            verticalVelocity = 0f
        )
        GameEngine::class.java.getDeclaredField("ball").apply {
            isAccessible = true
        }.set(engine, ball)
        engine.setPower(power)
        assertTrue(engine.shoot())
        return engine
    }

    @Test fun previewInclinationMatchesActualLaunchForBothTeams() {
        for (team in GameTeam.entries) {
            var previousLoft = 0f
            for (power in listOf(.1f, .3f, .5f, .7f, .9f, 1f)) {
                val ball = shot(team, power).snapshot.ball
                val inclination = kotlin.math.atan2(ball.verticalVelocity, ball.velocity.length())
                val fullInclination = kotlin.math.atan2(ShotLaunch.verticalSpeed(1f), ShotLaunch.speed(1f))
                assertEquals(inclination / fullInclination, ShotLaunch.loft(power), .0001f)
                assertTrue(ShotLaunch.loft(power) >= previousLoft)
                previousLoft = ShotLaunch.loft(power)
            }
        }
    }

    @Test fun defaultLaunchRetainsOriginalSpeedWithOptionalPracticeLobTradeoff() {
        assertEquals(540f, ShotLaunch.speed(.5f), .001f)
        assertEquals(724.8f, ShotLaunch.speed(.6f), .001f)
        assertEquals(1800f, ShotLaunch.speed(1f), .001f)
        assertEquals(3200f, ShotLaunch.verticalSpeed(1f), .001f)
        assertEquals(1170f, ShotLaunch.speed(1f, lobForwardRetention = .65f), .001f)
        assertEquals(ShotLaunch.speed(1f) * .7f, ShotLaunch.speed(1f, .7f), .001f)
    }

    @Test fun fullPowerCloseShotCrossesAboveGoalForBothTeams() {
        for (team in GameTeam.entries) {
            val engine = shot(team, 1f)
            var wall = false
            repeat(60) {
                if (!wall) {
                    val state = engine.step(1f / 120f)
                    assertNotEquals(GamePhase.GOAL, state.phase)
                    if (state.soundEvents.impacts > 0) {
                        wall = true
                        assertTrue(state.ball.height > 36f)
                    }
                }
            }
            assertTrue(wall)
            assertEquals(0, engine.snapshot.redScore + engine.snapshot.blueScore)
        }
    }

    @Test fun controlledPowerCloseShotCanScoreForBothTeams() {
        for (team in GameTeam.entries) {
            val engine = shot(team, .5f)
            repeat(120) { if (engine.currentPhase == GamePhase.BALL_MOVING) engine.step(1f / 120f) }
            assertEquals(GamePhase.GOAL, engine.currentPhase)
            assertEquals(team, engine.snapshot.lastScoringTeam)
        }
    }

    @Test fun mistarContactIsDistinctFromGoalAndOverbarForBothTeams() {
        for (team in GameTeam.entries) {
            for (height in listOf(22f, 60f)) {
                val engine = shot(team, .5f)
                val y = if (team == GameTeam.RED) 28.5f else 551.5f
                val incoming = engine.snapshot.ball.copy(
                    position = Vector2(196.5f, y),
                    velocity = Vector2(
                        0f,
                        if (team ==
                            GameTeam.RED
                        ) {
                            -300f
                        } else {
                            300f
                        }
                    ),
                    height = height,
                    verticalVelocity = 0f
                )
                GameEngine::class.java.getDeclaredField("ball").apply {
                    isAccessible = true
                }.set(engine, incoming)
                val state = engine.step(1f / 120f)
                assertNotEquals(GamePhase.GOAL, state.phase)
                assertEquals(if (height == 22f) 1 else 0, state.soundEvents.posts)
                if (height == 22f) assertTrue(state.ball.velocity.y * incoming.velocity.y < 0f)
            }
        }
    }
}

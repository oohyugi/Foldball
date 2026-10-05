package com.yogi.foldball

import com.yogi.foldball.domain.game.engine.GameEngine
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.Vector2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameEnginePhysicsTest {
    private val frameDurations = listOf(1f / 30f, 1f / 60f, 1f / 120f)
    // Keep players and goal walls outside the measured flight when isolating forces.
    private val isolatedFieldHeight = 5_000f

    @Test
    fun fasterPaperBallLosesMoreSpeedToAirDrag() {
        fun firstFrameSpeedLoss(power: Float): Float {
            val engine = GameEngine(393f, isolatedFieldHeight)
            assertTrue(engine.finishDefense())
            engine.setPower(power)
            assertTrue(engine.shoot())
            val speedBefore = engine.snapshot.ball.velocity.length()
            val speedAfter = engine.step(1f / 60f).ball.velocity.length()
            return speedBefore - speedAfter
        }

        val weakShotLoss = firstFrameSpeedLoss(.2f)
        val strongShotLoss = firstFrameSpeedLoss(1f)
        assertTrue(weakShotLoss > 0f)
        assertTrue(strongShotLoss > weakShotLoss * 4f)
    }

    @Test
    fun paperBallQuadraticAirDragMatchesTheoreticalSiValue() {
        val engine = GameEngine(393f, isolatedFieldHeight)
        assertTrue(engine.finishDefense())
        engine.setPower(1f)
        assertTrue(engine.shoot())

        val dt = 1f / 60f
        val metersPerDp = .0035f
        val ballMassKg = .002f
        val airDensity = 1.2f
        val dragCoefficient = .7f
        val radiusMeters = 10f * metersPerDp // 0.035 m (diameter 7 cm)
        val areaSquareMeters = PI.toFloat() * radiusMeters * radiusMeters

        val ballBefore = engine.snapshot.ball
        val v0PlanarDp = ballBefore.velocity.length()
        val v0VertDp = ballBefore.verticalVelocity
        // step() integrates at a fixed 1/120 s maximum step, so mirror its two
        // semi-implicit Euler steps for this 1/60 s theoretical comparison.
        var expectedPlanarSpeedDp = v0PlanarDp
        var expectedVerticalSpeedDp = v0VertDp
        val substep = 1f / 120f
        repeat(2) {
            val totalSpeedMps = sqrt(
                expectedPlanarSpeedDp * expectedPlanarSpeedDp +
                    expectedVerticalSpeedDp * expectedVerticalSpeedDp,
            ) * metersPerDp
            val dragForceNewtons = .5f * airDensity * dragCoefficient * areaSquareMeters *
                totalSpeedMps * totalSpeedMps
            val dragSpeedLossMps = (dragForceNewtons / ballMassKg) * substep
            val retention = 1f - dragSpeedLossMps / totalSpeedMps
            expectedPlanarSpeedDp *= retention
            expectedVerticalSpeedDp = expectedVerticalSpeedDp * retention -
                (9.81f / metersPerDp) * substep
        }

        val ballAfter = engine.step(dt).ball
        val actualPlanarSpeedDp = ballAfter.velocity.length()

        assertEquals(expectedPlanarSpeedDp, actualPlanarSpeedDp, 0.01f)
    }

    @Test
    fun airborneBallDeceleratesVerticallyByGravityAndAirDrag() {
        val engine = GameEngine(393f, isolatedFieldHeight)
        assertTrue(engine.finishDefense())
        engine.setPower(1f)
        assertTrue(engine.shoot())

        val dt = 1f / 60f
        val metersPerDp = .0035f
        val gravityDp = 9.81f / metersPerDp // ~2802.857 dp/s^2

        var peakHeight = 0f
        val maxVerticalVelocity = engine.snapshot.ball.verticalVelocity
        assertTrue(maxVerticalVelocity > 0f)

        var frame = 0
        var prevVerticalVelocity = maxVerticalVelocity

        while (engine.snapshot.ball.height > 0f || frame == 0) {
            val state = engine.step(dt)
            frame++
            if (state.ball.height > peakHeight) peakHeight = state.ball.height

            if (state.ball.height > 0f) {
                val actualLoss = prevVerticalVelocity - state.ball.verticalVelocity
                val expectedGravityLoss = gravityDp * dt
                if (prevVerticalVelocity > 0f && state.ball.verticalVelocity > 0f) {
                    // While rising, drag and gravity both act downward -> velocity loss >= gravity alone
                    assertTrue(actualLoss >= expectedGravityLoss,
                        "Rising ball vertical loss=$actualLoss expected at least $expectedGravityLoss at frame=$frame")
                } else if (prevVerticalVelocity < 0f && state.ball.verticalVelocity < 0f) {
                    // While falling, air drag opposes gravity -> acceleration is less than gravity alone
                    assertTrue(actualLoss <= expectedGravityLoss, "Falling ball vertical acceleration is slowed by air drag")
                }
            }
            prevVerticalVelocity = state.ball.verticalVelocity
            if (frame > 300) break
        }

        assertTrue(peakHeight > 10f, "Ball should reach a realistic apex height")
        assertEquals(0f, engine.snapshot.ball.height, 0.01f, "Ball should land back on ground")
    }

    @Test
    fun groundRollingFrictionMatchesDecelerationCoefficient() {
        val engine = GameEngine(393f, isolatedFieldHeight)
        assertTrue(engine.finishDefense())
        engine.setPower(.3f)
        assertTrue(engine.shoot())

        val dt = 1f / 60f
        val metersPerDp = .0035f
        val rollingFrictionCoefficient = .10f
        val gravityMps = 9.81f
        val expectedFrictionDecelDp = rollingFrictionCoefficient * gravityMps / metersPerDp // ~280.2857 dp/s^2

        // Wait until the launched ball has landed and stopped bouncing.
        repeat(120) {
            if (engine.snapshot.ball.height > 0f || engine.snapshot.ball.verticalVelocity > 0f) engine.step(dt)
        }

        val ballOnGround = engine.snapshot.ball
        assertEquals(0f, ballOnGround.height)
        val initialSpeed = ballOnGround.velocity.length()
        assertTrue(initialSpeed > 20f, "Ball should still be rolling on ground")

        val stateNext = engine.step(dt)
        val actualLoss = initialSpeed - stateNext.ball.velocity.length()

        // Rolling resistance is always present; air drag adds further loss.
        val expectedFrictionLossPerFrame = expectedFrictionDecelDp * dt
        val ballRadiusMeters = 10f * metersPerDp
        val speedMetersPerSecond = initialSpeed * metersPerDp
        val dragForceNewtons = .5f * 1.2f * .7f * PI.toFloat() * ballRadiusMeters * ballRadiusMeters *
            speedMetersPerSecond * speedMetersPerSecond
        val airDragLossUpperBound = dragForceNewtons / .002f * dt / metersPerDp
        assertTrue(actualLoss >= expectedFrictionLossPerFrame,
            "Friction loss=$actualLoss expected at least $expectedFrictionLossPerFrame, speed=$initialSpeed")
        assertTrue(actualLoss <= expectedFrictionLossPerFrame + airDragLossUpperBound + .1f,
            "Rolling friction and air drag loss bounded")
    }

    @Test
    fun physicsSimulationIsFrameRateIndependent() {
        fun simulateOneSecond(frameDuration: Float): Pair<Vector2, Vector2> {
            val engine = GameEngine(393f, 470f)
            assertTrue(engine.finishDefense())
            engine.setPower(.8f)
            assertTrue(engine.shoot())
            val steps = (1f / frameDuration).toInt()
            repeat(steps) {
                engine.step(frameDuration)
            }
            val ball = engine.snapshot.ball
            return Pair(ball.position, ball.velocity)
        }

        val (pos30, vel30) = simulateOneSecond(1f / 30f)
        val (pos60, vel60) = simulateOneSecond(1f / 60f)
        val (pos120, vel120) = simulateOneSecond(1f / 120f)

        assertEquals(pos30.x, pos60.x, 1.5f, "Position X 30fps vs 60fps")
        assertEquals(pos30.y, pos60.y, 1.5f, "Position Y 30fps vs 60fps")
        assertEquals(pos60.x, pos120.x, 1.0f, "Position X 60fps vs 120fps")
        assertEquals(pos60.y, pos120.y, 1.0f, "Position Y 60fps vs 120fps")

        assertEquals(vel30.length(), vel60.length(), 2.5f, "Velocity 30fps vs 60fps")
        assertEquals(vel60.length(), vel120.length(), 1.5f, "Velocity 60fps vs 120fps")
    }

    @Test
    fun fieldDimensionsAndWallBounceMatchRestitution() {
        val engine = GameEngine(393f, isolatedFieldHeight)
        assertTrue(engine.finishDefense())

        engine.aimBy(100f) // Coerced to max right aim angle (~1.05 rad)
        engine.setPower(1f)
        assertTrue(engine.shoot())

        val dt = 1f / 60f
        var bounced = false
        val wallBounceRestitution = .45f

        repeat(200) {
            val before = engine.snapshot.ball
            val after = engine.step(dt).ball
            if (!bounced && after.impactSideX != 0f) {
                bounced = true
                // Wall bounce should reverse horizontal direction and dissipate velocity according to wall bounce restitution
                assertTrue(before.velocity.x * after.velocity.x < 0f, "Horizontal velocity must reverse on wall bounce")
                assertTrue(abs(after.velocity.x) <= abs(before.velocity.x) * wallBounceRestitution + .1f, "Horizontal speed must be scaled by restitution")
            }
        }
        assertTrue(bounced, "Ball should bounce off the side boundary wall")
    }

    @Test
    fun redFullPowerHitOnBlueLosesEnergyWithoutImmediateRepeat() {
        for (frame in frameDurations) {
            val engine = GameEngine(393f, 470f)
            assertTrue(engine.finishDefense())
            engine.setPower(1f)
            assertTrue(engine.shoot())
            assertPlayerContactReboundsOnce(engine, GameTeam.BLUE, expectedDirection = 1f, frame = frame)
        }
    }

    @Test
    fun blueFullPowerHitOnRedLosesEnergyWithoutImmediateRepeat() {
        for (frame in frameDurations) {
            val engine = GameEngine(393f, 470f)
            assertTrue(engine.finishDefense())
            engine.setPower(.7f)
            assertTrue(engine.shoot())
            var ready = false
            repeat(1800) {
                if (!ready) {
                    val state = engine.step(frame)
                    ready = state.phase == GamePhase.DEFENDING && state.focusedPlayerId.startsWith("blue")
                }
            }
            assertTrue(ready, "Expected a blue player to receive the next shot at frame=$frame")
            assertTrue(engine.finishDefense())
            engine.setPower(1f)
            assertTrue(engine.shoot())
            assertPlayerContactReboundsOnce(engine, GameTeam.RED, expectedDirection = -1f, frame = frame)
        }
    }

    private fun assertPlayerContactReboundsOnce(engine: GameEngine, target: GameTeam, expectedDirection: Float, frame: Float) {
        var contacted = false
        var framesAfterContact = 0
        var priorImpact = 0f
        val observations = StringBuilder()
        repeat(600) { index ->
            val before = engine.snapshot.ball
            val state = engine.step(frame)
            val impact = state.players.filter { it.team == target }.maxOf { it.impactStrength }
            if (!contacted && impact > priorImpact + .05f) {
                contacted = true
                observations.append("contact frame=$index before=${before.velocity} after=${state.ball.velocity} height=${state.ball.height}\n")
                assertTrue(state.ball.velocity.y * expectedDirection > 0f, observations.toString())
                assertTrue(state.ball.velocity.length() <= before.velocity.length() * .8f, observations.toString())
                assertTrue(state.ball.velocity.length() >= before.velocity.length() * .25f, observations.toString())
                // Impact can be observed a substep later through its decaying
                // visual intensity; allow the small upward hop to have started.
                assertTrue(state.ball.height <= 20f, observations.toString())
                assertTrue(state.ball.verticalVelocity > 80f, observations.toString())
            } else if (contacted && framesAfterContact < 1) {
                framesAfterContact++
                observations.append("after=$framesAfterContact y=${state.ball.position.y} vy=${state.ball.velocity.y} height=${state.ball.height} impact=$impact\n")
                assertTrue(state.ball.velocity.y * expectedDirection >= 0f, observations.toString())
                assertTrue(impact <= priorImpact + .05f, observations.toString())
            }
            priorImpact = impact
        }
        assertTrue(contacted, "No contact with $target. $observations")
        assertEquals(1, framesAfterContact)
    }
}

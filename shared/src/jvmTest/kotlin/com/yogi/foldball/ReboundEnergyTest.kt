package com.yogi.foldball
import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*

class ReboundEnergyTest {
    private fun engine(
        height: Float,
        verticalSpeed: Float,
        position: Vector2 = Vector2(200f, 1000f),
        velocity: Vector2 = Vector2(0f, 500f)
    ): GameEngine {
        val engine = GameEngine(393f, 2000f)
        engine.finishDefense()
        engine.setPower(.5f)
        engine.shoot()
        GameEngine::class.java.getDeclaredField("players").apply {
            isAccessible = true
        }.set(engine, emptyList<MatchPlayer>())
        val ball = engine.snapshot.ball.copy(
            position = position,
            velocity = velocity,
            height = height,
            verticalVelocity = verticalSpeed
        )
        GameEngine::class.java.getDeclaredField("ball").apply {
            isAccessible = true
        }.set(engine, ball)
        return engine
    }

    @Test fun hardLandingDissipatesMorePlanarMomentumThanGentleLanding() {
        fun retention(speed: Float): Float {
            val landing = engine(.1f, -speed).step(1f / 120f)
            val airborne = engine(100f, -speed).step(1f / 120f)
            assertEquals(1, landing.soundEvents.bounces)
            return landing.ball.velocity.length() / airborne.ball.velocity.length()
        }
        val gentle = retention(50f)
        val hard = retention(500f)
        assertTrue(gentle in .96f..1f)
        assertTrue(hard < .87f && hard < gentle)
    }

    @Test fun glancingWallContactDissipatesTangentialMomentum() {
        val engine = engine(100f, 0f, Vector2(364.9f, 1000f), Vector2(100f, 500f))
        val before = engine.snapshot.ball
        val after = engine.step(1f / 120f)
        assertEquals(1, after.soundEvents.impacts)
        assertTrue(after.ball.velocity.x < 0f)
        assertTrue(after.ball.velocity.y / before.velocity.y < .73f)
    }
}

package com.yogi.foldball
import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*
class DentedReboundTest {
    private fun rebound(dent: Float, enabled: Boolean = true): GameSnapshot {
        val engine = GameEngine(393f, 580f, dentingEnabled = enabled)
        engine.finishDefense()
        engine.setPower(.5f)
        engine.shoot()
        val blocker = engine.snapshot.players.first {
            it.id == "blue-center"
        }.copy(
            position = Vector2(196.5f, 250f),
            home = Vector2(196.5f, 250f),
            condition = StandeeCondition(dent)
        )
        GameEngine::class.java.getDeclaredField("players").apply {
            isAccessible = true
        }.set(engine, listOf(blocker))
        GameEngine::class.java.getDeclaredField("ball").apply {
            isAccessible = true
        }.set(
            engine,
            engine.snapshot.ball.copy(
                position = Vector2(196.5f, 290f),
                velocity = Vector2(0f, -600f),
                height = 0f,
                verticalVelocity = 0f
            )
        )
        return engine.step(1f / 120f)
    }

    @Test fun dentAbsorbsMoreMomentumAndReducesHopContinuously() {
        val normal = rebound(0f)
        val half = rebound(.5f)
        val full = rebound(1f)
        for (state in listOf(normal, half, full)) {
            assertEquals(1, state.soundEvents.impacts)
            assertTrue(state.ball.velocity.y > 0f)
            assertTrue(state.ball.velocity.length() < 600f)
        }
        assertTrue(normal.ball.velocity.length() > half.ball.velocity.length())
        assertTrue(half.ball.velocity.length() > full.ball.velocity.length())
        assertTrue(normal.ball.verticalVelocity > half.ball.verticalVelocity)
        assertTrue(half.ball.verticalVelocity > full.ball.verticalVelocity)
    }

    @Test fun disabledPrototypeRetainsNormalContactPhysics() {
        assertEquals(rebound(0f, false).ball, rebound(1f, false).ball)
    }
}

package com.yogi.foldball
import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*
class PreviousKickerAnchorTest {
    @Test fun previousKickerDoesNotReturnToOldHomeOnNextShot() {
        val e = GameEngine(393f, 580f)
        val field = GameEngine::class.java.getDeclaredField("players").apply { isAccessible = true }
        field.set(
            e,
            e.snapshot.players.map { p ->
                val pos = when (p.id) {
                    "red-striker" -> Vector2(330f, 440f)
                    "blue-striker" -> Vector2(100f, 150f)
                    else -> p.position
                }
                p.copy(position = pos, velocity = Vector2(0f, 0f))
            }
        )
        GameEngine::class.java.getDeclaredField("focusedPlayerId").apply {
            isAccessible = true
        }.set(e, "blue-striker")
        GameEngine::class.java.getDeclaredField("ball").apply {
            isAccessible = true
        }.set(e, e.snapshot.ball.copy(position = Vector2(100f, 190f)))
        GameEngine::class.java.getDeclaredMethod(
            "beginKickerApproach",
            Boolean::class.javaPrimitiveType
        ).apply {
            isAccessible =
                true
        }.invoke(e, true)
        repeat(50) { e.step(1f / 120f) }
        val previous = e.snapshot.players.first { it.id == "red-striker" }.position
        e.finishDefense()
        e.setPower(.3f)
        e.shoot()
        e.step(1f / 120f)
        assertEquals(previous, e.snapshot.players.first { it.id == "red-striker" }.position)
    }
}

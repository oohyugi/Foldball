package com.yogi.foldball
import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*
class BlockedDragTest {
    @Test fun reversingAfterBlockedTravelRespondsImmediately() {
        val e = GameEngine(393f, 580f, targetScore = null)
        val movable = e.snapshot.players.first {
            it.id == "blue-center"
        }.copy(position = Vector2(196.5f, 50f), home = Vector2(196.5f, 50f))
        GameEngine::class.java.getDeclaredField("players").apply {
            isAccessible = true
        }.set(
            e,
            e.snapshot.players.map {
                if (it.id ==
                    movable.id
                ) {
                    movable
                } else {
                    it
                }
            }
        )
        val p = movable
        e.beginPlayerMove(p.id)
        repeat(12) { e.moveInactivePlayer(p.id, Vector2(0f, -30f)) }
        val before = e.snapshot.players.first { it.id == p.id }.position
        e.moveInactivePlayer(p.id, Vector2(0f, 10f))
        val after = e.snapshot.players.first { it.id == p.id }.position
        assertTrue(after.y > before.y, "Reverse drag must not consume accumulated blocked travel")
    }

    @Test fun playerCanEscapeOccupiedOriginZoneWithoutTeleporting() {
        val e = GameEngine(393f, 580f, targetScore = null, initialKickoffTeam = GameTeam.BLUE)
        e.finishDefense()
        e.setPower(.5f)
        e.shoot()
        // Set up a post-rally formation with two standees in one zone.
        GameEngine::class.java.getDeclaredField("phase").apply {
            isAccessible = true
        }.set(e, GamePhase.DEFENDING)
        GameEngine::class.java.getDeclaredField("phaseTimeRemaining").apply {
            isAccessible = true
        }.set(e, 10f)
        val f = GameEngine::class.java.getDeclaredField("players").apply { isAccessible = true }
        f.set(
            e,
            e.snapshot.players.filter {
                it.id in listOf("red-left", "red-striker", "blue-striker")
            }.map { p ->
                val pos = when (p.id) {
                    "red-left" -> Vector2(210f, 250f)
                    "red-striker" -> Vector2(255f, 250f)
                    else -> Vector2(330f, 100f)
                }
                p.copy(position = pos, home = pos)
            }
        )
        GameEngine::class.java.getDeclaredField("ball").apply {
            isAccessible = true
        }.set(e, e.snapshot.ball.copy(position = Vector2(100f, 400f)))
        e.beginPlayerMove("red-left")
        val before = e.snapshot.players.first { it.id == "red-left" }.position
        e.moveInactivePlayer("red-left", Vector2(-10f, 0f))
        val after = e.snapshot.players.first { it.id == "red-left" }.position
        assertEquals(before.x - 10f, after.x, .01f)
        assertEquals(before.y, after.y)
    }
}

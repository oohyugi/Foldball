package com.yogi.foldball
import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*
class StandeeConditionTest {
    @Test fun lightContactsDoNotDentAndHeavyHitsAccumulate() {
        var condition = StandeeCondition()
        repeat(20) { condition = condition.hit(200f) }
        assertFalse(condition.dented)
        condition = condition.hit(900f)
        assertTrue(condition.dented)
        assertTrue(condition.percent in 10..20)
        assertTrue(condition.shotStrength in .94f..1f)
        condition = condition.hit(900f)
        assertTrue(condition.dented)
        assertTrue(condition.percent in 20..40)
        repeat(20) { condition = condition.hit(900f) }
        assertEquals(100, condition.percent)
        assertEquals(kotlin.math.sqrt(.45f), condition.shotStrength, .0001f)
        assertEquals(condition, condition.hit(2000f))
    }

    @Test fun conditionPersistsAcrossPossessionAndKickoff() {
        val dent = StandeeCondition(1f)
        val engine = GameEngine(393f, 580f, dentingEnabled = true)
        val field = GameEngine::class.java.getDeclaredField("players").apply { isAccessible = true }
        field.set(engine, engine.snapshot.players.map { it.copy(condition = dent) })
        val approach = GameEngine::class.java.getDeclaredMethod(
            "beginKickerApproach",
            Boolean::class.javaPrimitiveType
        ).apply {
            isAccessible =
                true
        }
        repeat(4) {
            val focus = GameEngine::class.java.getDeclaredField("focusedPlayerId").apply {
                isAccessible = true
            }
            focus.set(engine, if (it % 2 == 0) "blue-striker" else "red-striker")
            approach.invoke(engine, true)
            assertTrue(engine.snapshot.players.all { it.condition.dented })
        }
        GameEngine::class.java.getDeclaredMethod("beginKickoff", GameTeam::class.java).apply {
            isAccessible =
                true
        }.invoke(engine, GameTeam.RED)
        assertTrue(engine.snapshot.players.all { it.condition == dent })
        assertTrue(
            GameEngine(393f, 580f, dentingEnabled = true).snapshot.players.all {
                it.condition ==
                    StandeeCondition()
            }
        )
    }

    @Test fun prototypeIsOptInAndSimulationCopiesKeepCondition() {
        fun make(enabled: Boolean): GameEngine {
            val e = GameEngine(393f, 580f, dentingEnabled = enabled)
            val id = e.snapshot.focusedPlayerId
            GameEngine::class.java.getDeclaredField("players").apply {
                isAccessible = true
            }.set(
                e,
                e.snapshot.players.map {
                    if (it.id ==
                        id
                    ) {
                        it.copy(condition = StandeeCondition(1f))
                    } else {
                        it
                    }
                }
            )
            e.finishDefense()
            e.setPower(1f)
            return e
        }
        val normal = make(false)
        val dented = make(true)
        val copy = dented.copyForSimulation()
        assertEquals(dented.snapshot, copy.snapshot)
        normal.shoot()
        dented.shoot()
        copy.shoot()
        assertTrue(dented.snapshot.ball.velocity.length() < normal.snapshot.ball.velocity.length())
        assertEquals(dented.snapshot, copy.snapshot)
    }

    @Test fun hitSpeedControlsDamageAndPenaltiesGrowGradually() {
        val gentle = StandeeCondition().hit(380f)
        val hard = StandeeCondition().hit(640f)
        assertTrue(gentle.percent < hard.percent)
        assertTrue(hard.percent < 20)
        assertTrue(gentle.shotStrength > hard.shotStrength)
        assertTrue(hard.hit(1000f).percent > hard.percent)
    }

    @Test fun energyTransferDeterminesVelocitySquaredAndImpactEnergyIsQuadratic() {
        val half = StandeeCondition(.5f)
        assertEquals(half.energyTransfer, half.shotStrength * half.shotStrength, .0001f)
        val slow = StandeeCondition().hit(500f).progress
        val fast = StandeeCondition().hit(1000f).progress
        assertTrue(fast > slow * 4f) // yield threshold subtracts the same work per impact
        assertEquals(StandeeCondition(), StandeeCondition().absorbEnergy(.0006f))
    }
}

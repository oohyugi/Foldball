package com.yogi.foldball
import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*
class DentDirectionTest {
    @Test fun directionAccumulatesByDamageEnergyAndRemainsStableBetweenHits() {
        val left = StandeeCondition().absorbEnergy(.0107f, -1f, 0f)
        assertEquals(-1f, left.bendX, .0001f)
        val mixed = left.absorbEnergy(.0107f, 1f, 0f)
        assertEquals(0f, mixed.bendX, .0001f)
        assertEquals(40, mixed.percent)
        assertEquals(mixed, mixed.absorbEnergy(.0001f, 1f, 1f))
        val frontal = StandeeCondition().absorbEnergy(.0107f, 0f, -1f)
        assertEquals(0f, frontal.bendX)
        assertEquals(-1f, frontal.bendY)
    }
}

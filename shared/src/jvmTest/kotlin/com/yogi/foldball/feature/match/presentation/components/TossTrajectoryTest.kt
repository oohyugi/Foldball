package com.yogi.foldball.feature.match.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TossTrajectoryTest {
    @Test fun ballisticArcAndReboundsLoseHeightAndSettle() {
        val toss = TossTrajectory(120f, 40f, -140f)
        val gravity = 9.81f / .0035f
        val apexTime = kotlin.math.sqrt(2f * gravity * 120f) / gravity
        assertEquals(120f, toss.sample(apexTime).height, .001f)
        val impactTime = apexTime * 2f
        assertEquals(0f, toss.sample(impactTime).height, .001f)
        assertEquals(120f * .18f * .18f, toss.sample(impactTime + apexTime * .18f).height, .001f)
        val final = toss.sample(toss.duration)
        assertEquals(40f, final.x, .001f)
        assertEquals(-140f, final.y, .001f)
        assertEquals(0f, final.height)
        assertEquals(final, toss.sample(toss.duration + 1f))
    }

    @Test fun travelIsContinuousAndSlowsDuringRoll() {
        val toss = TossTrajectory(120f, 40f, 140f)
        var previous = toss.sample(0f)
        for (index in 1..1000) {
            val pose = toss.sample(toss.duration * index / 1000f)
            assertTrue(pose.height >= 0f)
            assertTrue(pose.x >= previous.x)
            assertTrue(pose.y >= previous.y)
            assertTrue(kotlin.math.abs(pose.height - previous.height) < 2f)
            previous = pose
        }
        val earlyRoll = toss.sample(toss.airDuration + .1f).x - toss.sample(toss.airDuration).x
        val lateRoll = toss.sample(toss.duration).x - toss.sample(toss.duration - .1f).x
        assertTrue(lateRoll < earlyRoll)
    }
}

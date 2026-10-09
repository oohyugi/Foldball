package com.yogi.foldball
import com.yogi.foldball.domain.game.engine.GoalGeometry
import kotlin.test.*
class BallLiftProjectionTest {
    @Test fun projectionRetainsOriginalLiftAndHasNoHardPlateau() {
        assertEquals(0f, GoalGeometry.visualLift(-1f))
        assertEquals(9f, GoalGeometry.visualLift(36f), .0001f)
        val heights = listOf(0f, 36f, 100f, 288f, 350f, 600f, 1200f)
        val lifts = heights.map { GoalGeometry.visualLift(it) }
        assertTrue(lifts.zipWithNext().all { (a, b) -> b > a })
        assertTrue(lifts.all { it <= GoalGeometry.maximumVisualLift })
        val before = GoalGeometry.visualLift(36f) - GoalGeometry.visualLift(35f)
        val after = GoalGeometry.visualLift(37f) - GoalGeometry.visualLift(36f)
        assertEquals(before, after, .03f)
    }
}

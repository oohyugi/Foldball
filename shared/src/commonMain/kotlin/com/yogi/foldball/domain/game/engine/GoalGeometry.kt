@file:Suppress("ktlint:standard:property-naming")

package com.yogi.foldball.domain.game.engine

/** Simulation units are dp; height is the ball's lift above its ground position. */
object GoalGeometry {
    const val openingHeight = 36f
    const val heightProjection = .25f
    const val maximumVisualLift = 72f
    const val netDepth = 36f
    fun visualLift(height: Float): Float {
        val projected = height.coerceAtLeast(0f) * heightProjection
        val openingLift = openingHeight * heightProjection
        if (projected <= openingLift) return projected
        val remaining = maximumVisualLift - openingLift
        return openingLift +
            remaining * (1f - kotlin.math.exp(-(projected - openingLift) / remaining))
    }
    fun clearsOpening(ballHeight: Float, radius: Float) = ballHeight + radius * 2f < openingHeight
    fun touchesCrossbar(ballHeight: Float, radius: Float) = ballHeight <= openingHeight && ballHeight + radius * 2f >= openingHeight
}

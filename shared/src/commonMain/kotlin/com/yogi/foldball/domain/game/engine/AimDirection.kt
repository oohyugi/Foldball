package com.yogi.foldball.domain.game.engine

import kotlin.math.cos
import kotlin.math.sin

/** Angles are measured from each team's forward direction; screen Y grows downward. */
object AimDirection {
    val maxAngleRadians: Float = 1.05f

    fun shotVector(team: GameTeam, angle: Float): Vector2 {
        val forwardY = if (team == GameTeam.RED) -1f else 1f
        return Vector2(sin(angle) * -forwardY, cos(angle) * forwardY)
    }
}

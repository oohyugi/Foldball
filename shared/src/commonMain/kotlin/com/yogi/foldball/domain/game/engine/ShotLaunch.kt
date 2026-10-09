package com.yogi.foldball.domain.game.engine

import kotlin.math.atan2

/** Shared launch values for the engine and its visual loft cue. */
object ShotLaunch {
    fun speed(power: Float, strength: Float = 1f, lobForwardRetention: Float = 1f): Float {
        // Practice may reduce high-power forward travel; matches retain the original launch curve.
        val baseSpeed = 120f + power * power * 1_680f
        val controlledSpeedLimit = 120f + .7f * .7f * 1_680f
        val extraForwardRetention = (1_800f * lobForwardRetention - controlledSpeedLimit) / (1_800f - controlledSpeedLimit)
        val speed = if (power <= .7f) baseSpeed else
            controlledSpeedLimit + (baseSpeed - controlledSpeedLimit) * extraForwardRetention
        return speed * strength
    }

    fun verticalSpeed(power: Float, strength: Float = 1f): Float =
        power * power * power * power * power * 3_200f * strength

    /** Relative launch inclination, not a predicted landing point. */
    fun loft(power: Float): Float {
        val p = power.coerceIn(0f, 1f)
        return atan2(verticalSpeed(p), speed(p)) / atan2(verticalSpeed(1f), speed(1f))
    }
}

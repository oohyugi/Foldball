package com.yogi.foldball.feature.match.presentation.components

/** Analytic ballistic arcs followed by a friction-limited roll, in presentation dp. */
internal class TossTrajectory(
    apexHeight: Float,
    private val travelX: Float,
    private val travelY: Float,
) {
    // Match engine scale: 0.0035 meters per dp, gravity 9.81 m/s².
    private val gravity = 9.81f / .0035f
    private val restitution = .18f
    private val launchSpeed = kotlin.math.sqrt(2f * gravity * apexHeight)
    private val firstFlight = 2f * launchSpeed / gravity
    private val secondFlight = firstFlight * restitution
    private val thirdFlight = secondFlight * restitution
    val airDuration = firstFlight + secondFlight + thirdFlight
    private val rollDuration = .55f
    val duration = airDuration + rollDuration
    private val travelTime = airDuration + rollDuration / 2f

    fun sample(seconds: Float): TossPose {
        val time = seconds.coerceIn(0f, duration)
        var arcTime = time
        var speed = launchSpeed
        repeat(3) {
            val flight = 2f * speed / gravity
            if (arcTime <= flight) {
                val height = (speed * arcTime - gravity * arcTime * arcTime / 2f).coerceAtLeast(0f)
                return pose(time / travelTime, height, time)
            }
            arcTime -= flight
            speed *= restitution
        }
        val rollTime = (time - airDuration).coerceIn(0f, rollDuration)
        val distance = airDuration + rollTime - rollTime * rollTime / (2f * rollDuration)
        return pose(distance / travelTime, 0f, airDuration)
    }

    private fun pose(distance: Float, height: Float, spinTime: Float) = TossPose(
        x = travelX * distance,
        y = travelY * distance,
        height = height,
        // Airborne spin is constant; contact roll follows distance / ball radius.
        rotationX = spinTime * 10f + (travelY * distance) / 8f,
        rotationY = spinTime * 7f + (travelX * distance) / 8f,
    )
}

internal data class TossPose(
    val x: Float,
    val y: Float,
    val height: Float,
    val rotationX: Float,
    val rotationY: Float,
)

package com.yogi.foldball.domain.game.engine

/** Practice overrides; default values remain the production match physics. */
data class PhysicsTuning(
    val wallBounce: Float = .65f,
    val playerBounce: Float = .50f,
    val groundBounce: Float = .30f,
    val lobForwardRetention: Float = 1f
) {
    init {
        require(listOf(wallBounce, playerBounce, groundBounce).all { it in 0f..1f })
        require(lobForwardRetention in .55f..1f)
    }
}

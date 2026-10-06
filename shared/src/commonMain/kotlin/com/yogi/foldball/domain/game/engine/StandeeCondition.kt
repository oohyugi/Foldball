package com.yogi.foldball.domain.game.engine

/** Continuous match-local damage, cleared at kickoff; no permanent repair. */
data class StandeeCondition(
    val impactLoad: Float = 0f,
    val bendX: Float = 0f,
    val bendY: Float = 0f
) {
    val progress: Float get() = impactLoad.coerceIn(0f, 1f)
    val percent: Int get() = kotlin.math.round(progress * 100).toInt()
    val dented: Boolean get() = progress > 0f

    // Elastic work stored by an increasingly buckled standee. v scales as sqrt(E).
    val energyTransfer: Float get() = 1f - .55f * progress
    val shotStrength: Float get() = kotlin.math.sqrt(energyTransfer)
    fun hit(incomingSpeed: Float): StandeeCondition {
        val reducedMass = .002f * .020f / (.002f + .020f)
        val speedMeters = incomingSpeed.coerceAtLeast(0f) * .0035f
        val restitution = .50f * (1f - .5f * progress)
        val dissipatedEnergy =
            .5f * reducedMass * speedMeters * speedMeters * (1f - restitution * restitution)
        return absorbEnergy(dissipatedEnergy)
    }
    fun absorbEnergy(
        joules: Float,
        directionX: Float = 0f,
        directionY: Float = 0f
    ): StandeeCondition {
        if (progress >= 1f) return this
        // Yield and folding-work budget are provisional paper-material parameters.
        val foldingEnergy = (joules - .0007f).coerceAtLeast(0f)
        val added = (foldingEnergy / .05f).coerceAtMost(1f - progress)
        if (added <= 0f) return this
        val total = progress + added
        return copy(
            impactLoad = total,
            bendX = (
                (
                    bendX * progress + directionX.coerceIn(
                        -1f,
                        1f
                    ) * added
                    ) / total
                ).coerceIn(-1f, 1f),
            bendY = (
                (
                    bendY * progress + directionY.coerceIn(
                        -1f,
                        1f
                    ) * added
                    ) / total
                ).coerceIn(-1f, 1f)
        )
    }
}

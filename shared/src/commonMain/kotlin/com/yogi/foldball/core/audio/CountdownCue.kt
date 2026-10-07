package com.yogi.foldball.core.audio

/** Warns only near the end of a human turn; muted thresholds are consumed once. */
class CountdownCue {
    private var phaseKey: String? = null
    private val turnCues = mutableSetOf<Int>()
    fun update(
        phase: String,
        turnSeconds: Int,
        matchSeconds: Int,
        humanTurn: Boolean,
        active: Boolean
    ): Boolean {
        if (phase != phaseKey) {
            phaseKey = phase
            turnCues.clear()
        }
        val turnDue =
            humanTurn && matchSeconds > 0 && turnSeconds in 1..3 && turnCues.add(turnSeconds)
        return active && turnDue
    }
}

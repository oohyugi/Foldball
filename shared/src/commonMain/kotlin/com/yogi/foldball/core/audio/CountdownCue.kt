package com.yogi.foldball.core.audio

/** Consumes each threshold once, even muted; a new phase resets only turn cues. */
class CountdownCue {
    private var phaseKey: String? = null
    private val turnCues = mutableSetOf<Int>()
    private val matchCues = mutableSetOf<Int>()
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
        val matchDue = matchSeconds in setOf(10, 3, 2, 1) && matchCues.add(matchSeconds)
        val turnDue =
            humanTurn && matchSeconds > 0 && turnSeconds in 1..3 && turnCues.add(turnSeconds)
        return active && (matchDue || turnDue)
    }
}

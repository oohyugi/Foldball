package com.yogi.foldball.core.audio

import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameTeam

/** Consume phase-entry cues even while silent; pause/resume never replays them. */
class TurnCue {
    private var previous: String? = null

    fun update(phase: GamePhase, possession: GameTeam, playerId: String, ready: Boolean, active: Boolean): String? {
        if (!ready) return null
        val key = "$phase:$playerId"
        if (key == previous) return null
        previous = key
        if (!active || (phase != GamePhase.DEFENDING && phase != GamePhase.AIMING)) return null
        val acting = if (phase == GamePhase.DEFENDING) {
            if (possession == GameTeam.RED) GameTeam.BLUE else GameTeam.RED
        } else {
            possession
        }
        val side = if (acting == GameTeam.RED) "player" else "opponent"
        val action = if (phase == GamePhase.DEFENDING) "defend" else "attack"
        return "turn_${side}_$action"
    }
}

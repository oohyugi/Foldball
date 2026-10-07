package com.yogi.foldball

import com.yogi.foldball.core.audio.TurnCue
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameTeam
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TurnCueTest {
    @Test fun cuesIdentifyActingTeamAndPhaseOnce() {
        val cue = TurnCue()
        assertNull(cue.update(GamePhase.DEFENDING, GameTeam.RED, "red", false, true))
        assertEquals("turn_opponent_defend", cue.update(GamePhase.DEFENDING, GameTeam.RED, "red", true, true))
        assertNull(cue.update(GamePhase.DEFENDING, GameTeam.RED, "red", true, true))
        assertEquals("turn_player_attack", cue.update(GamePhase.AIMING, GameTeam.RED, "red", true, true))
        assertEquals("turn_player_defend", cue.update(GamePhase.DEFENDING, GameTeam.BLUE, "blue", true, true))
        assertEquals("turn_opponent_attack", cue.update(GamePhase.AIMING, GameTeam.BLUE, "blue", true, true))
    }

    @Test fun muteAndPauseConsumeWithoutResumeReplay() {
        val cue = TurnCue()
        assertNull(cue.update(GamePhase.AIMING, GameTeam.RED, "red", true, false))
        assertNull(cue.update(GamePhase.AIMING, GameTeam.RED, "red", true, true))
        assertNull(cue.update(GamePhase.GOAL, GameTeam.RED, "red", true, true))
        assertEquals("turn_opponent_defend", cue.update(GamePhase.DEFENDING, GameTeam.RED, "red", true, true))
    }
}

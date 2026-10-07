package com.yogi.foldball

import com.yogi.foldball.core.audio.CountdownCue
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TurnCountdownCueTest {
    @Test fun matchTimeAloneNeverTriggersWarning() {
        val cue = CountdownCue()
        for (remaining in listOf(10, 3, 2, 1)) {
            assertFalse(cue.update("aim", 15, remaining, humanTurn = true, active = true))
        }
    }

    @Test fun humanTurnWarningsPlayOnceAndMutedWarningsDoNotReplay() {
        val cue = CountdownCue()
        assertFalse(cue.update("aim", 3, 60, humanTurn = true, active = false))
        assertFalse(cue.update("aim", 3, 60, humanTurn = true, active = true))
        assertTrue(cue.update("aim", 2, 60, humanTurn = true, active = true))
        assertFalse(cue.update("aim", 2, 60, humanTurn = true, active = true))
        assertTrue(cue.update("next", 3, 60, humanTurn = true, active = true))
        assertFalse(cue.update("bot", 1, 60, humanTurn = false, active = true))
        assertFalse(cue.update("ended", 1, 0, humanTurn = true, active = true))
    }
}

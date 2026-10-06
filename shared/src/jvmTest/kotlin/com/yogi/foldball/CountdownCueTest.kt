package com.yogi.foldball
import com.yogi.foldball.core.audio.CountdownCue
import kotlin.test.*
class CountdownCueTest {
    @Test fun concurrentCountdownsEmitOnceAndResumeDoesNotReplay() {
        val c = CountdownCue()
        assertTrue(c.update("aim", 3, 3, true, true))
        assertFalse(c.update("aim", 3, 3, true, true))
        assertFalse(c.update("aim", 2, 2, true, false))
        assertFalse(c.update("aim", 2, 2, true, true))
        assertTrue(c.update("aim", 1, 1, true, true))
    }

    @Test fun botTurnsAreSilentButMatchReminderAppliesAndNewPhaseResetsTurn() {
        val c = CountdownCue()
        assertFalse(c.update("bot", 3, 50, false, true))
        assertTrue(c.update("bot", 2, 10, false, true))
        assertFalse(c.update("bot", 1, 10, false, true))
        assertTrue(c.update("human", 3, 9, true, true))
        assertFalse(c.update("human", 0, 0, true, true))
    }
}

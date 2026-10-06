package com.yogi.foldball

import com.yogi.foldball.core.audio.SoundDispatcher
import com.yogi.foldball.domain.game.engine.GameEngine
import com.yogi.foldball.domain.game.engine.SoundEvents
import kotlin.test.*

class SoundDispatcherTest {
    @Test fun consumesEventsOnceAndDoesNotReplayMutedEvents() {
        val snapshot = GameEngine(360f, 600f).snapshot
        val dispatcher = SoundDispatcher(snapshot.soundEvents)
        val sounds = mutableListOf<String>()
        val kicked = snapshot.copy(soundEvents = SoundEvents(kicks = 1))
        dispatcher.dispatch(kicked, 0, true) { name, _ -> sounds.add(name) }
        dispatcher.dispatch(kicked, 100, true) { name, _ -> sounds.add(name) }
        val muted = kicked.copy(soundEvents = SoundEvents(kicks = 2))
        dispatcher.dispatch(muted, 200, false) { name, _ -> sounds.add(name) }
        dispatcher.dispatch(muted, 300, true) { name, _ -> sounds.add(name) }
        assertEquals(listOf("kick"), sounds)
    }
    @Test fun goalAndPostOverrideImpactAndImpactHasCooldown() {
        val snapshot = GameEngine(360f, 600f).snapshot
        val dispatcher = SoundDispatcher(snapshot.soundEvents)
        val sounds = mutableListOf<String>()
        fun send(events: SoundEvents, time: Long) = dispatcher.dispatch(snapshot.copy(soundEvents = events), time, true) { name, _ -> sounds.add(name) }
        send(SoundEvents(impacts = 1, posts = 1), 0)
        send(SoundEvents(impacts = 2, posts = 1, goals = 1), 100)
        send(SoundEvents(impacts = 3, posts = 1, goals = 1), 200)
        send(SoundEvents(impacts = 4, posts = 1, goals = 1), 210)
        assertEquals(listOf("post", "goal", "impact"), sounds)
    }
}

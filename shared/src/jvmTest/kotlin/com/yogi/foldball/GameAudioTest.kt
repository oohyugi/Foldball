package com.yogi.foldball

import com.yogi.foldball.core.audio.GameAudio
import com.yogi.foldball.core.audio.SoundPlayer
import kotlinx.coroutines.*
import kotlin.test.*

class GameAudioTest {
    private class FakePlayer: SoundPlayer {
        val played = mutableListOf<String>()
        override suspend fun load(name: String, bytes: ByteArray) {}
        override fun play(name: String, volume: Float) { played.add(name) }
        override fun stop() {}
        override fun close() {}
    }
    @Test fun cheerFollowsNetAndMuteDoesNotReplayPendingCheer() = runBlocking {
        val player = FakePlayer()
        val audio = GameAudio(player)
        audio.play("goal", .7f)
        audio.playGoalCheer()
        assertEquals(listOf("goal", "cheer"), player.played)
        player.played.clear()
        val pending = launch(start = CoroutineStart.UNDISPATCHED) { audio.playGoalCheer() }
        audio.changeEnabled(false)
        audio.changeEnabled(true)
        pending.join()
        assertTrue(player.played.isEmpty())
    }
    @Test fun backgroundAndStopCancelPendingCheer() = runBlocking {
        val player = FakePlayer()
        val audio = GameAudio(player)
        val pending = launch(start = CoroutineStart.UNDISPATCHED) { audio.playGoalCheer() }
        audio.changeForeground(false)
        audio.changeForeground(true)
        pending.join()
        assertTrue(player.played.isEmpty())
        val stopped = launch(start = CoroutineStart.UNDISPATCHED) { audio.playGoalCheer() }
        audio.stop()
        stopped.join()
        assertTrue(player.played.isEmpty())
    }
    @Test fun finalWhistlePlaysImmediatelyAndStopCancelsDefeat() = runBlocking {
        val player = FakePlayer()
        val audio = GameAudio(player)
        val pending = launch(start = CoroutineStart.UNDISPATCHED) { audio.playMatchEnd(true) }
        assertEquals(listOf("whistle"), player.played)
        audio.stop()
        pending.join()
        assertEquals(listOf("whistle"), player.played)
        player.played.clear()
        audio.playMatchEnd(false)
        assertEquals(listOf("whistle"), player.played)
    }

    @Test fun victoryFollowsWhistleAndMutedWinStaysSilent() = runBlocking {
        val player = FakePlayer()
        val audio = GameAudio(player)
        val pending = launch(start = CoroutineStart.UNDISPATCHED) {
            audio.playMatchEnd(lostAgainstAi = false, wonAgainstAi = true)
        }
        assertEquals(listOf("whistle"), player.played)
        pending.join()
        assertEquals(listOf("whistle", "victory"), player.played)
        player.played.clear()
        audio.changeEnabled(false)
        audio.playMatchEnd(lostAgainstAi = false, wonAgainstAi = true)
        assertTrue(player.played.isEmpty())
    }

}

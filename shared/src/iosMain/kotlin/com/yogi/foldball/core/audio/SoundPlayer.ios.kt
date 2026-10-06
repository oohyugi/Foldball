@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.yogi.foldball.core.audio
import androidx.compose.runtime.*
import kotlinx.cinterop.*
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.create

@Composable actual fun rememberSoundPlayer(): SoundPlayer = remember { IosSoundPlayer() }
private class IosSoundPlayer : SoundPlayer {
    private val players = mutableMapOf<String, List<AVAudioPlayer>>()
    override suspend fun load(name: String, bytes: ByteArray) {
        val data = bytes.usePinned {
            NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
        }
        players[name] =
            (0..1).mapNotNull { AVAudioPlayer(data = data, error = null).apply { prepareToPlay() } }
    }
    override fun play(name: String, volume: Float) {
        val choices = players[name] ?: return
        val player = choices.firstOrNull { !it.playing } ?: choices.firstOrNull() ?: return
        player.stop()
        player.currentTime = 0.0
        player.volume = volume
        player.play()
    }
    override fun stop() {
        players.values.flatten().forEach { it.stop() }
    }
    override fun close() {
        stop()
        players.clear()
    }
}

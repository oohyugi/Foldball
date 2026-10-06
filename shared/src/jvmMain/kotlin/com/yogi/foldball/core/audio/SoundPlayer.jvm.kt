package com.yogi.foldball.core.audio
import androidx.compose.runtime.*
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.math.log10

@Composable actual fun rememberSoundPlayer(): SoundPlayer = remember { DesktopSoundPlayer() }
private class DesktopSoundPlayer : SoundPlayer {
    private val clips = mutableMapOf<String, List<Clip>>()
    override suspend fun load(name: String, bytes: ByteArray) {
        clips[name] = (0..1).mapNotNull {
            var clip: Clip? = null
            try {
                clip = AudioSystem.getClip()
                AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes)).use { stream ->
                    clip.open(stream)
                }
                clip
            } catch (
                _: Exception
            ) {
                clip?.close()
                null
            }
        }
    }
    override fun play(name: String, volume: Float) {
        val choices = clips[name] ?: return
        val clip = choices.firstOrNull { !it.isRunning } ?: choices.firstOrNull() ?: return
        clip.stop()
        clip.framePosition = 0
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            gain.value =
                (20 * log10(volume.coerceAtLeast(.001f))).coerceIn(gain.minimum, gain.maximum)
        }
        clip.start()
    }
    override fun stop() {
        clips.values.flatten().forEach { it.stop() }
    }
    override fun close() {
        clips.values.flatten().forEach { it.close() }
        clips.clear()
    }
}

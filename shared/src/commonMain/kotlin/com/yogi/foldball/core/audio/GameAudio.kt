package com.yogi.foldball.core.audio

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yogi.foldball.domain.game.engine.GameSnapshot
import com.yogi.foldball.domain.game.engine.SoundEvents
import foldball.shared.generated.resources.Res

private val soundAssets = listOf(
    "crumple", "kick", "bounce", "impact", "post", "goal", "defeat", "cheer", "whistle", "kickoff", "victory", "timeout", "countdown_tick",
    "turn_player_defend", "turn_player_attack", "turn_opponent_defend", "turn_opponent_attack"
)

interface SoundPlayer {
    suspend fun load(name: String, bytes: ByteArray)
    fun play(name: String, volume: Float)
    fun playLoop(name: String, volume: Float) { play(name, volume) }
    fun stop()
    fun close()
}
suspend fun readSoundAsset(name: String): ByteArray {
    require(name in soundAssets || name == "bgm")
    return Res.readBytes("files/audio/$name.wav")
}

@Composable expect fun rememberSoundPlayer(): SoundPlayer

class GameAudio(private val player: SoundPlayer) {
    var musicEnabled by mutableStateOf(true)
        private set
    var musicPaused by mutableStateOf(false)
        private set
    fun changeMusicEnabled(value: Boolean) { musicEnabled = value }
    fun pauseMusic(value: Boolean) { musicPaused = value }
    private var playbackGeneration = 0
    private val loaded = mutableStateMapOf<String, Boolean>()
    fun isLoaded(name: String) = loaded[name] == true
    internal fun markLoaded(name: String) {
        loaded[name] = true
    }
    var enabled by mutableStateOf(true)
        private set
    var foreground by mutableStateOf(true)
        private set
    fun changeForeground(value: Boolean) {
        foreground = value
        if (!value) stop()
    }
    fun changeEnabled(value: Boolean) {
        enabled = value
        if (!value) stop()
    }
    fun stop() {
        playbackGeneration += 1
        player.stop()
    }
    suspend fun playMatchEnd(lostAgainstAi: Boolean, wonAgainstAi: Boolean = false) {
        stop() // Final whistle takes precedence over any remaining crowd celebration.
        if (!enabled || !foreground) return
        val generation = playbackGeneration
        play("whistle", .55f)
        if (lostAgainstAi || wonAgainstAi) {
            kotlinx.coroutines.delay(2050)
            if (generation ==
                playbackGeneration
            ) {
                play(
                    if (lostAgainstAi) "defeat" else "victory",
                    if (lostAgainstAi) .55f else .5f
                )
            }
        }
    }
    suspend fun playGoalCheer() {
        val generation = playbackGeneration
        kotlinx.coroutines.delay(180)
        if (generation == playbackGeneration) play("cheer", .4f)
    }
    fun play(name: String, volume: Float) {
        if (enabled &&
            foreground
        ) {
            player.play(name, volume.coerceIn(0f, 1f))
        }
    }
}
val LocalGameAudio = staticCompositionLocalOf<GameAudio?> { null }

@Composable fun rememberGameAudio(): GameAudio {
    val player = rememberSoundPlayer()
    val audio = remember(player) { GameAudio(player) }
    LaunchedEffect(player) {
        soundAssets.forEach {
            player.load(it, readSoundAsset(it))
            audio.markLoaded(it)
        }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) audio.changeForeground(false)
            if (event == Lifecycle.Event.ON_START) audio.changeForeground(true)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(player) { onDispose { player.close() } }
    BackgroundMusic(audio)
    return audio
}

@Composable
private fun BackgroundMusic(audio: GameAudio) {
    val player = rememberSoundPlayer()
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(player) {
        player.load("bgm", readSoundAsset("bgm"))
        ready = true
    }
    LaunchedEffect(ready, audio.musicEnabled, audio.foreground, audio.musicPaused) {
        player.stop()
        if (ready && audio.musicEnabled && audio.foreground && !audio.musicPaused) player.playLoop("bgm", .12f)
    }
    DisposableEffect(player) { onDispose { player.close() } }
}

/** Consume event counters even while muted/paused; visual decay never retriggers audio. */
class SoundDispatcher(initial: SoundEvents) {
    private var previous = initial
    private val lastPlayed = mutableMapOf<String, Long>()
    fun dispatch(
        snapshot: GameSnapshot,
        nowMs: Long,
        active: Boolean,
        play: (String, Float) -> Unit
    ) {
        val old = previous
        val next = snapshot.soundEvents
        previous = next
        if (!active) return
        fun emit(name: String, volume: Float, cooldown: Long = 80) {
            val last = lastPlayed[name]
            if (last == null ||
                nowMs - last >= cooldown
            ) {
                lastPlayed[name] = nowMs
                play(name, volume)
            }
        }
        if (next.kicks > old.kicks) emit("kick", .45f + snapshot.lastShotPower * .25f)
        when {
            next.goals > old.goals -> emit("goal", .7f, 400)

            next.posts > old.posts -> emit("post", .6f)

            next.impacts > old.impacts -> emit("impact", .25f + snapshot.ball.impactStrength * .25f)

            next.bounces > old.bounces -> emit(
                "bounce",
                .09f + snapshot.ball.landingStrength * .11f,
                120
            )
        }
    }
}

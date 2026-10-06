package com.yogi.foldball
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.yogi.foldball.core.audio.AndroidSoundPlayer
import com.yogi.foldball.core.audio.readSoundAsset
import kotlinx.coroutines.*

/** Debug-only check of bundled decoding and native playback. */
class SoundCheckActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var player: AndroidSoundPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = AndroidSoundPlayer(cacheDir, diagnostics = true)
        scope.launch {
            val names =
                listOf("crumple", "kick", "bounce", "impact", "post", "goal", "defeat", "cheer", "whistle", "kickoff", "victory", "timeout")
            names.forEach { player.load(it, readSoundAsset(it)) }
            delay(1000)
            names.forEach {
                player.play(it, .5f)
                delay(
                    if (it ==
                        "victory"
                    ) {
                        3700
                    } else if (it ==
                        "cheer"
                    ) {
                        2700
                    } else if (it ==
                        "defeat"
                    ) {
                        2300
                    } else if (it == "whistle") {
                        2100
                    } else {
                        1300
                    }
                )
            }
            player.stop()
            Log.i("FoldballAudio", "CHECK_COMPLETE")
            finish()
        }
    }
    override fun onDestroy() {
        scope.cancel()
        player.close()
        super.onDestroy()
    }
}

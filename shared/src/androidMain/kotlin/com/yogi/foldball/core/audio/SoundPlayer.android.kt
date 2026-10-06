package com.yogi.foldball.core.audio

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable actual fun rememberSoundPlayer(): SoundPlayer {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidSoundPlayer(context.cacheDir) }
}
class AndroidSoundPlayer(private val cacheDir: File, private val diagnostics: Boolean = false): SoundPlayer {
    private val pool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val ids = mutableMapOf<String, Int>()
    private val ready = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
    private val streams = ArrayDeque<Int>()
    private val files = mutableListOf<File>()
    private var closed = false
    init { pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready.add(id); if (diagnostics) android.util.Log.i("FoldballAudio", "LOAD id=$id status=$status") } }
    override suspend fun load(name: String, bytes: ByteArray) {
        val file = withContext(Dispatchers.IO) { File.createTempFile("foldball-$name-", ".wav", cacheDir).apply { writeBytes(bytes) } }
        if (closed) { file.delete(); return }
        files.add(file)
        val id = pool.load(file.absolutePath, 1)
        ids[name] = id
        // SoundPool loading is asynchronous; do not start the opening before decode completes.
        if (id != 0) {
            repeat(200) {
                if (closed || id in ready) return
                kotlinx.coroutines.delay(10)
            }
        }
    }
    override fun play(name: String, volume: Float) {
        if (closed) return
        val id = ids[name] ?: return
        if (id !in ready) return
        val stream = pool.play(id, volume, volume, 1, 0, 1f)
        if (diagnostics) android.util.Log.i("FoldballAudio", "PLAY $name stream=$stream volume=$volume")
        if (stream != 0) { streams.addLast(stream); if (streams.size > 12) streams.removeFirst() }
    }
    override fun stop() { if (!closed) streams.forEach { pool.stop(it) }; streams.clear() }
    override fun close() { if (closed) return; stop(); closed = true; pool.release(); files.forEach { it.delete() }; files.clear() }
}

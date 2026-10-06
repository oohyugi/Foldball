package com.yogi.foldball

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yogi.foldball.domain.game.bot.BotDifficulty
import com.yogi.foldball.domain.game.bot.BotPlanner
import com.yogi.foldball.domain.game.bot.BotSearchStats
import com.yogi.foldball.domain.game.engine.*
import kotlin.random.Random
import kotlinx.coroutines.*

/** Debug-only reproducible device benchmark. No entry point in release builds. */
class BotBenchmarkActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val frameIntervals = mutableListOf<Float>()
    private var previousFrame = 0L
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(now: Long) {
            if (previousFrame != 0L) frameIntervals += (now - previousFrame) / 1_000_000f
            previousFrame = now
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SandboxMain() }
        scope.launch {
            delay(1500) // Let initial composition settle before sampling frames.
            Choreographer.getInstance().postFrameCallback(frameCallback)
            // Control window: same rendered screen with no bot search running.
            delay(3000)
            frameIntervals.forEach { Log.i(TAG, "CONTROL_FRAME,$it") }
            frameIntervals.clear()
            previousFrame = 0L
            val scenarios = withContext(Dispatchers.Default) { scenarios() }
            val samples = intent.getIntExtra("samples", 3).coerceIn(1, 10)
            Log.i(TAG, "BEGIN samples=$samples device=${android.os.Build.MODEL}")
            for (level in BotDifficulty.entries) {
                var stats: BotSearchStats? = null
                val planner = BotPlanner(level, Random(123), onSearchCompleted = { stats = it })
                // Warm-up is deliberately omitted from recorded decision samples.
                withContext(Dispatchers.Default) {
                    val warm = scenarios.first().second.copyForSimulation()
                    planner.chooseDefensePlan(warm, defender(warm))
                    warm.finishDefense()
                    planner.chooseShot(warm)
                }
                repeat(samples) { sample ->
                    for ((name, game) in scenarios) {
                        for (kind in listOf("defense", "shot")) {
                            val trial = game.copyForSimulation()
                            if (kind == "shot") trial.finishDefense()
                            val elapsed = withContext(Dispatchers.Default) {
                                val start = SystemClock.elapsedRealtimeNanos()
                                if (kind ==
                                    "defense"
                                ) {
                                    planner.chooseDefensePlan(trial, defender(trial))
                                } else {
                                    planner.chooseShot(trial)
                                }
                                (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000f
                            }
                            Log.i(TAG, "SAMPLE,${level.name},$kind,$name,$sample,$elapsed")
                            stats?.let {
                                Log.i(
                                    TAG,
                                    "METRIC,${level.name},$kind,$name,${it.simulations},${it.formations},${it.replies},${it.tacticalCandidates},${it.budgetExhausted}"
                                )
                            }
                        }
                    }
                }
            }
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            frameIntervals.forEach { Log.i(TAG, "FRAME,$it") }
            Log.i(TAG, "DONE frames=${frameIntervals.size}")
            finish()
        }
    }

    private fun defender(game: GameEngine): GameTeam = if (game.snapshot.players.first { it.id == game.snapshot.focusedPlayerId }.team ==
        GameTeam.RED
    ) {
        GameTeam.BLUE
    } else {
        GameTeam.RED
    }

    private fun scenarios(): List<Pair<String, GameEngine>> = buildList {
        for (team in GameTeam.entries) {
            val opening = GameEngine(360f, 580f, targetScore = null, initialKickoffTeam = team)
            add("opening-$team" to opening)
            val live = opening.copyForSimulation()
            live.finishDefense()
            live.setAimAngle(if (team == GameTeam.RED) .4f else -.4f)
            live.setPower(.65f)
            live.shoot()
            var frames = 0
            while (live.snapshot.phase != GamePhase.DEFENDING &&
                frames++ < 3000
            ) {
                live.step(1f / 120f)
            }
            check(live.snapshot.phase == GamePhase.DEFENDING)
            add("live-$team" to live)
        }
    }

    override fun onDestroy() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "FoldballBotBench"
    }
}

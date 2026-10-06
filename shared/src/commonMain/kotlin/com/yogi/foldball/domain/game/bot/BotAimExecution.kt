package com.yogi.foldball.domain.game.bot
import kotlin.random.Random

/** The same triangular meter drives prediction and live player/bot charging. */
object ShotPowerMeter {
    const val cycleSeconds = 1.8f
    const val minimum = .05f
    fun powerAt(seconds: Float): Float {
        val position = (seconds.coerceAtLeast(0f) / cycleSeconds) % 1f
        val progress = if (position <= .5f) position * 2f else (1f - position) * 2f
        return minimum + (1f - minimum) * progress
    }
    fun targetTime(power: Float) = (power.coerceIn(minimum, 1f) - minimum) / (1f - minimum) * cycleSeconds / 2f
}
object BotAimExecution {
    fun angleError(level: BotDifficulty) = when(level) { BotDifficulty.EASY -> .09f; BotDifficulty.MEDIUM -> .045f; BotDifficulty.HARD -> .018f }
    fun reactionSeconds(level: BotDifficulty) = when(level) { BotDifficulty.EASY -> .12f; BotDifficulty.MEDIUM -> .09f; BotDifficulty.HARD -> .06f }
    fun target(ideal: BotShot, difficulty: BotDifficulty, random: Random = Random.Default) = ideal.copy(
        angle = (ideal.angle + (random.nextFloat()*2-1)*angleError(difficulty)).coerceIn(-1.05f,1.05f),
        power = ideal.power.coerceIn(.05f,1f),
    )
    fun releaseTime(power: Float, difficulty: BotDifficulty, random: Random = Random.Default) =
        (ShotPowerMeter.targetTime(power) + (random.nextFloat()*2-1)*reactionSeconds(difficulty)).coerceAtLeast(.02f)
    fun sample(ideal: BotShot, difficulty: BotDifficulty, angleNoise: Float, reactionNoise: Float) = BotShot(
        (ideal.angle + angleNoise*angleError(difficulty)).coerceIn(-1.05f,1.05f),
        ShotPowerMeter.powerAt((ShotPowerMeter.targetTime(ideal.power)+reactionNoise*reactionSeconds(difficulty)).coerceAtLeast(.02f)),
    )
}

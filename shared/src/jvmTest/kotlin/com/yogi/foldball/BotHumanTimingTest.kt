package com.yogi.foldball
import com.yogi.foldball.domain.game.bot.*
import com.yogi.foldball.domain.game.engine.*
import kotlin.random.Random
import kotlin.test.*
import kotlinx.coroutines.runBlocking
class BotHumanTimingTest {
    @Test fun earlyAndLateReleaseUseTheSameTriangularMeter() {
        assertEquals(.05f, ShotPowerMeter.powerAt(0f), .0001f)
        assertEquals(1f, ShotPowerMeter.powerAt(.9f), .0001f)
        assertTrue(ShotPowerMeter.powerAt(.96f) < 1f)
        val ideal = BotShot(0f, .65f)
        val early = BotAimExecution.sample(ideal, BotDifficulty.MEDIUM, 0f, -1f)
        val late = BotAimExecution.sample(ideal, BotDifficulty.MEDIUM, 0f, 1f)
        assertTrue(early.power < ideal.power && late.power > ideal.power)
        for (level in BotDifficulty.entries) {
            val rng = Random(42)
            val times = (1..100).map { BotAimExecution.releaseTime(ideal.power, level, rng) }
            assertTrue(times.min() < ShotPowerMeter.targetTime(ideal.power))
            assertTrue(times.max() > ShotPowerMeter.targetTime(ideal.power))
        }
    }

    @Test fun countLimitedSearchIsRepeatableWithGenerousSafetyDeadline() = runBlocking {
        val engine = GameEngine(393f, 580f, initialKickoffTeam = GameTeam.BLUE)
        engine.finishDefense()
        val original = engine.snapshot
        var first: BotSearchStats? = null
        var second: BotSearchStats? = null
        val a = BotPlanner(BotDifficulty.MEDIUM, Random(77), 6000, {
            first = it
        }).chooseShot(engine)
        val b = BotPlanner(BotDifficulty.MEDIUM, Random(77), 6000, {
            second = it
        }).chooseShot(engine)
        assertEquals(a, b)
        assertEquals(first?.simulations, second?.simulations)
        assertTrue(requireNotNull(first).simulations <= 88)
        assertEquals(original, engine.snapshot)
    }
}

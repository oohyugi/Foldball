package com.yogi.foldball
import com.yogi.foldball.domain.game.bot.*
import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*
class DentMeterTest {
    @Test fun meterAndBotPredictionUseSameConditionCycle() {
        assertEquals(1.8f, ShotPowerMeter.cycleFor(0f), .0001f)
        assertEquals(1.55f, ShotPowerMeter.cycleFor(.5f), .0001f)
        assertEquals(1.3f, ShotPowerMeter.cycleFor(1f), .0001f)
        for (dent in listOf(0f, .5f, 1f)) {
            val target = ShotPowerMeter.targetTime(.7f, dent)
            assertEquals(.7f, ShotPowerMeter.powerAt(target, dent), .0001f)
            val sample = BotAimExecution.sample(
                BotShot(.2f, .7f),
                BotDifficulty.MEDIUM,
                0f,
                1f,
                dent
            )
            assertEquals(ShotPowerMeter.powerAt(target + .09f, dent), sample.power, .0001f)
        }
    }

    @Test fun dentDoesNotChangeChosenShotDirection() {
        for (team in GameTeam.entries) {
            val e = GameEngine(393f, 580f, initialKickoffTeam = team, dentingEnabled = true)
            val id = e.snapshot.focusedPlayerId
            GameEngine::class.java.getDeclaredField("players").apply {
                isAccessible = true
            }.set(
                e,
                e.snapshot.players.map {
                    if (it.id ==
                        id
                    ) {
                        it.copy(condition = StandeeCondition(1f))
                    } else {
                        it
                    }
                }
            )
            e.finishDefense()
            e.setAimAngle(.3f)
            e.setPower(.8f)
            e.shoot()
            val v = e.snapshot.ball.velocity
            assertEquals(kotlin.math.tan(.3f), -v.x / v.y, .0001f)
        }
    }
}

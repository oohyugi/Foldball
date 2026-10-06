package com.yogi.foldball
import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*
class ShotSpeedTest {
    @Test fun launchSpeedIsCappedAcrossFieldsAndBothTeams() {
        for (height in listOf(470f, 580f, 1200f)) {
            for (team in GameTeam.entries) {
                val e = GameEngine(393f, height, initialKickoffTeam = team)
                e.finishDefense()
                e.setPower(1f)
                e.shoot()
                assertEquals(1800f, e.snapshot.ball.velocity.length(), .01f)
            }
        }
    }

    @Test fun controlledPowerRemainsUsefulAndSpeedRisesSmoothly() {
        var previous = 0f
        for (power in listOf(.05f, .25f, .5f, .75f, 1f)) {
            val e = GameEngine(393f, 580f)
            e.finishDefense()
            e.setPower(power)
            e.shoot()
            val speed = e.snapshot.ball.velocity.length()
            assertTrue(speed > previous)
            assertEquals(120f + 1680f * power * power, speed, .01f)
            previous = speed
        }
    }
}

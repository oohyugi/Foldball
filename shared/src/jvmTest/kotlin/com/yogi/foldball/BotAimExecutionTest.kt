package com.yogi.foldball
import com.yogi.foldball.domain.game.bot.*
import kotlin.random.Random
import kotlin.test.*
class BotAimExecutionTest {
 @Test fun errorsAreBoundedAndDecreaseWithDifficulty() {
  for ((level, limits) in listOf(BotDifficulty.EASY to (.09f to .08f), BotDifficulty.MEDIUM to (.045f to .04f), BotDifficulty.HARD to (.018f to .02f))) {
   val random = Random(42)
   val shots = (1..100).map { BotAimExecution.target(BotShot(0f,.5f), level, random) }
   assertTrue(shots.distinct().size > 1)
   assertTrue(shots.all { kotlin.math.abs(it.angle) <= limits.first && kotlin.math.abs(it.power - .5f) <= limits.second + .00001f })
   val edge = BotAimExecution.target(BotShot(1.05f,1f),level,Random(1))
   assertTrue(edge.angle in -1.05f..1.05f && edge.power in .05f..1f)
  }
 }
}

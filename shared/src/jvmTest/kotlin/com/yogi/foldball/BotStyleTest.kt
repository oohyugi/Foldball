package com.yogi.foldball
import com.yogi.foldball.domain.game.bot.*
import com.yogi.foldball.domain.game.engine.*
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.*

class BotStyleTest {
 @Test fun stylesProduceLegalMovesAndShotsWithoutChangingLiveState() = runBlocking {
  for (style in BotStyle.entries) {
   val defense = GameEngine(393f,580f)
   val original = defense.snapshot
   val planner = BotPlanner(BotDifficulty.MEDIUM, Random(42), style = style)
   val moves = planner.chooseDefensePlan(defense,GameTeam.BLUE)
   assertEquals(original,defense.snapshot)
   val trial=defense.copyForSimulation()
   for(move in moves) assertTrue(trial.movePlayerToZone(move.playerId,move.zone))
   assertTrue(moves.size<=2)
   val attack = GameEngine(393f,580f,initialKickoffTeam=GameTeam.BLUE)
   attack.finishDefense()
   val before=attack.snapshot
   val shot=planner.chooseShot(attack)
   assertEquals(before,attack.snapshot)
   attack.setAimAngle(shot.angle);attack.setPower(shot.power)
   assertTrue(attack.shoot())
  }
 }
 @Test fun tacticalPrioritiesChangeDefenseChoices() {
  val engine=GameEngine(393f,580f)
  val choices=BotStyle.entries.map { BotPlanner(BotDifficulty.MEDIUM,Random(42),style=it).chooseDefense(engine,GameTeam.BLUE) }
  assertTrue(choices.distinct().size>1,"Styles should not all choose the same defensive move")
 }
}

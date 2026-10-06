package com.yogi.foldball
import com.yogi.foldball.domain.game.engine.*
import kotlin.test.*
class KickerStanceTest {
 @Test fun obstructedApproachKeepsKickerCloseForBothTeams() {
  for(team in GameTeam.entries) for(blocked in listOf(false,true)) {
   val e=GameEngine(393f,580f,initialKickoffTeam=team)
   val id=e.snapshot.focusedPlayerId
   val forward=if(team==GameTeam.RED) -1f else 1f
   val ball=Vector2(196.5f,290f)
   val players=e.snapshot.players.mapIndexed { index,p ->
    val pos=if(p.id==id) Vector2(196.5f,290f-forward*180f) else if(index==0 && blocked) Vector2(196.5f,290f-forward*39f) else p.position
    p.copy(position=pos,home=pos)
   }
   GameEngine::class.java.getDeclaredField("players").apply {isAccessible=true}.set(e,players)
   GameEngine::class.java.getDeclaredField("ball").apply {isAccessible=true}.set(e,e.snapshot.ball.copy(position=ball))
   GameEngine::class.java.getDeclaredMethod("beginKickerApproach",Boolean::class.javaPrimitiveType).apply {isAccessible=true}.invoke(e,true)
   repeat(50) { e.step(1f/120f) }
   val state=e.snapshot
   val kicker=state.players.first {it.id==id}
   assertEquals(GamePhase.DEFENDING,state.phase)
   assertEquals(39f,(kicker.position-state.ball.position).length(),.1f)
   assertTrue(state.players.filter {it.id!=id}.all {(it.position-kicker.position).length()>=67.9f})
  }
 }
 @Test fun nearBackWallUsesCloseLegalArcInsteadOfDistantFallback() {
  for(team in GameTeam.entries) {
   val e=GameEngine(393f,580f,initialKickoffTeam=team)
   val id=e.snapshot.focusedPlayerId
   val forward=if(team==GameTeam.RED) -1f else 1f
   val ball=Vector2(28f,if(team==GameTeam.RED)552f else 28f)
   val fn=GameEngine::class.java.getDeclaredMethod("clearKickerPosition",String::class.java,Vector2::class.java,Float::class.javaPrimitiveType).apply {isAccessible=true}
   val stance=fn.invoke(e,id,ball,forward) as Vector2
   assertEquals(39f,(stance-ball).length(),.1f)
   assertTrue(stance.x in 25f..368f && stance.y in 25f..555f)
  }
 }

}

package com.yogi.foldball

import com.yogi.foldball.domain.game.bot.*
import com.yogi.foldball.domain.game.engine.*
import kotlinx.coroutines.*
import kotlin.test.*

class BotTacticsTest {
    @Test fun simulationAdvanceIsIdenticalToPresentationStepThroughWholeRallies() {
        val visible = GameEngine(393f, 580f, targetScore = null)
        val prediction = visible.copyForSimulation()
        repeat(4) { turn ->
            for (game in listOf(visible, prediction)) {
                game.finishDefense()
                game.setAimAngle(if (turn % 2 == 0) .7f else -.35f)
                game.setPower(.75f)
                game.shoot()
            }
            var frames = 0
            do {
                visible.step(1f / 120f)
                prediction.advanceSimulation(1f / 120f)
                assertEquals(visible.snapshot, prediction.snapshot)
            } while (visible.currentPhase != GamePhase.DEFENDING && frames++ < 3000)
            assertTrue(frames < 3000)
        }
    }

    @Test fun tinyThinkingBudgetStillReturnsLegalDecisionWithoutMutatingEngine() = runBlocking {
        val engine = GameEngine(360f, 580f)
        engine.finishDefense()
        val original = engine.snapshot
        var stats: BotSearchStats? = null
        val shot = BotPlanner(BotDifficulty.HARD, maxThinkingMillis = 1, onSearchCompleted = { stats = it }).chooseShot(engine)
        assertEquals(original, engine.snapshot)
        assertTrue(shot.angle in -1.05f..1.05f && shot.power in .05f..1f)
        assertTrue(stats != null)
        engine.setAimAngle(shot.angle)
        engine.setPower(shot.power)
        assertTrue(engine.shoot())
    }

    @Test fun canceledSearchStopsBeforeApplyingAnyDecision() = runBlocking {
        val engine = GameEngine(360f, 580f)
        engine.finishDefense()
        val original = engine.snapshot
        val canceled = Job().apply { cancel() }
        assertFailsWith<CancellationException> {
            withContext(canceled) { BotPlanner(BotDifficulty.HARD).chooseShot(engine) }
        }
        assertEquals(original, engine.snapshot)
    }

    @Test fun hardPredictsLegalFormationChangesAndRepliesOnBlockedGoal() = runBlocking {
        val engine = GameEngine(393f, 1800f, targetScore = null)
        val field = GameEngine::class.java.getDeclaredField("players").apply { isAccessible = true }
        field.set(engine, engine.snapshot.players.mapIndexed { index, player ->
            val pos = if (player.team == GameTeam.BLUE) listOf(Vector2(80f, 815f), Vector2(196f, 815f), Vector2(312f, 815f), Vector2(196f, 600f))[index - 4]
                else Vector2(70f + index * 84f, 1510f)
            player.copy(position = pos, home = pos)
        })
        engine.finishDefense()
        val original = engine.snapshot
        var stats: BotSearchStats? = null
        BotPlanner(BotDifficulty.HARD, maxThinkingMillis = 6000, onSearchCompleted = { stats = it }).chooseShot(engine)

        assertEquals(original, engine.snapshot)
        assertTrue(requireNotNull(stats).formations > 0, "Hard must consider repositioning before the next shot")
        assertTrue(requireNotNull(stats).replies > 0, "Hard must evaluate a reply after repositioning")
        assertTrue(requireNotNull(stats).tacticalCandidates >= 2, "Hard must compare complete tactical outcomes")
    }
}

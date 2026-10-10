package com.yogi.foldball

import com.yogi.foldball.domain.game.bot.BotDifficulty
import com.yogi.foldball.domain.game.bot.BotStyle
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchPlayMode
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import com.yogi.foldball.domain.game.engine.MatchTimingConfig
import com.yogi.foldball.domain.team.TeamProfile
import com.yogi.foldball.feature.match.presentation.viewmodel.MatchIntent
import com.yogi.foldball.feature.match.presentation.viewmodel.MatchOptions
import com.yogi.foldball.feature.match.presentation.viewmodel.MatchViewModel
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MatchViewModelTest {
    private fun match(full: Boolean = true, duration: Int = 180, practice: Boolean = false, threeActionTurns: Boolean = false): MatchViewModel {
        val teams =
            MatchTeamConfiguration(
                red = requireNotNull(TeamProfile.create("My Club", 5)).toMatchIdentity()
            )
        return MatchViewModel(
            MatchOptions(
                full,
                false,
                BotDifficulty.MEDIUM,
                BotStyle.BALANCED,
                true,
                MatchTimingConfig(matchSeconds = duration),
                teams,
                practice = practice,
                threeActionTurns = threeActionTurns
            )
        ).apply { onIntent(MatchIntent.ConfigureArena(340f, 580f)) }
    }

    @Test fun experimentalSetupDoesNotSpendMatchTimeAndPauseStopsItsTimer() {
        val vm = match(threeActionTurns = true)
        assertTrue(vm.onIntent(MatchIntent.KickoffSelected(GameTeam.BLUE)))
        assertTrue(vm.snapshot.openingFormation)
        repeat(50) { vm.advanceFrame(.1f) }
        assertEquals(0f, vm.uiState.value.elapsedSeconds)
        val remaining = vm.snapshot.phaseTimeRemaining
        vm.onIntent(MatchIntent.SetPaused(true))
        repeat(20) { vm.advanceFrame(.1f) }
        assertEquals(remaining, vm.snapshot.phaseTimeRemaining)
        vm.onIntent(MatchIntent.SetPaused(false))
        assertTrue(vm.onIntent(MatchIntent.FinishDefense))
        assertEquals(GameTeam.BLUE, vm.snapshot.formationTeam)
        repeat(50) { vm.advanceFrame(.1f) }
        assertEquals(0f, vm.uiState.value.elapsedSeconds)
        assertTrue(vm.onIntent(MatchIntent.FinishDefense))
        repeat(40) { vm.advanceFrame(1f / 60f) }
        assertEquals(GamePhase.AIMING, vm.snapshot.phase)
        assertTrue(vm.uiState.value.elapsedSeconds > 0f)
        assertTrue(vm.onIntent(MatchIntent.EndTurn))
        repeat(90) { vm.advanceFrame(1f / 60f) }
        assertEquals(GameTeam.RED, vm.uiState.value.activeTeam)
    }

    @Test fun pausingCancelsPlacementPreviewWithoutMovingPlayer() {
        val vm = match(threeActionTurns = true)
        vm.onIntent(MatchIntent.KickoffSelected(GameTeam.RED))
        val before = vm.snapshot.players
        vm.onIntent(MatchIntent.BeginPlayerMove("red-left", previewOnly = true))
        vm.onIntent(MatchIntent.MovePlayer("red-left", com.yogi.foldball.domain.game.engine.Vector2(20f, 0f)))
        assertNotNull(vm.snapshot.playerMovePreview)
        vm.onIntent(MatchIntent.SetPaused(true))
        assertNull(vm.snapshot.playerMovePreview)
        assertEquals(before, vm.snapshot.players)
        assertEquals(0, vm.snapshot.actionsUsed)
    }

    @Test fun practiceCountResetAndTimersKeepOneHumanKicker() {
        val vm = match(full = false, practice = true)
        assertEquals(1, vm.snapshot.players.size)
        for (count in 0..5) {
            assertTrue(vm.onIntent(MatchIntent.SetPracticeOpponents(count)))
            assertEquals(count + 1, vm.snapshot.players.size)
            assertEquals(1, vm.snapshot.players.count { it.team == GameTeam.RED })
            assertEquals(count, vm.snapshot.players.count { it.team == GameTeam.BLUE })
            repeat(200) { vm.advanceFrame(.1f) }
            assertEquals(GamePhase.AIMING, vm.snapshot.phase)
            assertFalse(vm.onIntent(MatchIntent.FinishDefense))
            repeat(200) { vm.advanceFrame(.1f) }
            assertEquals(GamePhase.AIMING, vm.snapshot.phase)
            vm.onIntent(MatchIntent.SetPower(.5f))
            assertTrue(vm.onIntent(MatchIntent.Shoot))
            repeat(1200) { vm.advanceFrame(.1f) }
            assertEquals("red-striker", vm.snapshot.focusedPlayerId)
            vm.onIntent(MatchIntent.Rematch)
            assertEquals(count, vm.uiState.value.practiceOpponents)
            assertEquals(count + 1, vm.snapshot.players.size)
            assertEquals(GamePhase.AIMING, vm.snapshot.phase)
        }
        assertFalse(vm.onIntent(MatchIntent.SetPracticeOpponents(6)))
        assertFalse(match().onIntent(MatchIntent.SetPracticeOpponents(0)))
    }

    @Test fun practiceKickerPlacementMovesBallAndUndoRestoresBoth() {
        val vm = match(full = false, practice = true)
        val before = vm.snapshot
        val kicker = before.players.first { it.id == before.focusedPlayerId }
        val delta = com.yogi.foldball.domain.game.engine.Vector2(35f, 60f)
        assertTrue(vm.canMovePlayer(kicker.id))
        vm.onIntent(MatchIntent.BeginPlayerMove(kicker.id))
        vm.onIntent(MatchIntent.MovePlayer(kicker.id, delta))
        vm.onIntent(MatchIntent.EndPlayerMove(kicker.id))
        val moved = vm.snapshot.players.first { it.id == kicker.id }
        assertTrue((moved.position - kicker.position).length() > 1f)
        assertEquals(before.ball.position + (moved.position - kicker.position), vm.snapshot.ball.position)
        assertTrue(vm.onIntent(MatchIntent.Undo))
        assertEquals(kicker.position, vm.snapshot.players.first { it.id == kicker.id }.position)
        assertEquals(before.ball.position, vm.snapshot.ball.position)
        vm.onIntent(MatchIntent.SetPower(.5f))
        assertFalse(vm.canMovePlayer(kicker.id))
        assertTrue(vm.onIntent(MatchIntent.Shoot))
    }

    @Test fun practiceTuningChangesLaunchAndSurvivesResetWithoutLeakingToMatches() {
        val tuning = com.yogi.foldball.domain.game.engine.PhysicsTuning(wallBounce = .8f, playerBounce = .6f,
            groundBounce = .3f, lobForwardRetention = 1f)
        val vm = match(full = false, practice = true)
        assertTrue(vm.onIntent(MatchIntent.SetPhysicsTuning(tuning)))
        vm.onIntent(MatchIntent.SetPracticeOpponents(3))
        vm.onIntent(MatchIntent.Rematch)
        assertEquals(tuning, vm.uiState.value.physicsTuning)
        assertEquals(4, vm.snapshot.players.size)
        vm.onIntent(MatchIntent.SetPower(1f))
        assertTrue(vm.onIntent(MatchIntent.Shoot))
        assertEquals(1800f, vm.snapshot.ball.velocity.length(), .01f)
        assertFalse(match().onIntent(MatchIntent.SetPhysicsTuning(tuning)))
        assertFalse(match(full = false).onIntent(MatchIntent.SetPhysicsTuning(tuning)))
    }

    @Test fun tossAndHandoffBlockClockThenPauseBlocksCommandsAndClock() {
        val vm = match()
        vm.advanceFrame(.1f)
        assertEquals(0f, vm.uiState.value.elapsedSeconds)
        vm.onIntent(MatchIntent.KickoffSelected(GameTeam.BLUE))
        assertEquals("blue-striker", vm.snapshot.focusedPlayerId)
        vm.advanceFrame(.1f, presentationBlocked = true)
        assertEquals(0f, vm.uiState.value.elapsedSeconds)
        vm.advanceFrame(.1f)
        val beforePause = vm.snapshot
        vm.onIntent(MatchIntent.SetPaused(true))
        vm.advanceFrame(.1f)
        assertFalse(vm.onIntent(MatchIntent.FinishDefense))
        assertEquals(beforePause, vm.snapshot)
        assertEquals(.1f, vm.uiState.value.elapsedSeconds)
        vm.onIntent(MatchIntent.SetPaused(false))
        assertTrue(vm.onIntent(MatchIntent.FinishDefense))
        assertEquals(GamePhase.AIMING, vm.snapshot.phase)
    }

    @Test fun rematchResetsClockTossAndEngineWithoutReusingOpponentsBadge() {
        val vm = match()
        val originalTeams = vm.uiState.value.teams
        vm.onIntent(MatchIntent.KickoffSelected(GameTeam.RED))
        vm.advanceFrame(.1f)
        vm.onIntent(MatchIntent.SetPaused(true))
        vm.onIntent(MatchIntent.Rematch)
        assertEquals(originalTeams, vm.uiState.value.teams)
        assertEquals(1, vm.uiState.value.sessionId)
        assertFalse(vm.uiState.value.paused)
        assertEquals(0f, vm.uiState.value.elapsedSeconds)
        assertEquals(null, vm.uiState.value.openingKickoffTeam)
        assertEquals(0, vm.snapshot.redScore)
        assertNotEquals(vm.uiState.value.teams.red.logoId, vm.uiState.value.teams.blue.logoId)
    }

    @Test fun aiRematchPreservesIdentityAcrossAllStylesAndDifficulties() {
        for (style in BotStyle.entries) {
            for (difficulty in BotDifficulty.entries) {
                val vm = MatchViewModel(
                    MatchOptions(
                        true,
                        true,
                        difficulty,
                        style,
                        true,
                        MatchTimingConfig(),
                        MatchTeamConfiguration()
                    )
                )
                val teams = vm.uiState.value.teams
                repeat(3) {
                    vm.onIntent(MatchIntent.Rematch)
                    assertEquals(teams, vm.uiState.value.teams)
                }
            }
        }
    }

    @Test fun configuredDurationEndsMatchAndSandboxDoesNotConsumeMatchTime() {
        val vm = match(duration = 1)
        vm.onIntent(MatchIntent.KickoffSelected(GameTeam.RED))
        repeat(11) { vm.advanceFrame(.1f) }
        assertEquals(0, vm.uiState.value.secondsRemaining)
        assertEquals(GamePhase.MATCH_OVER, vm.snapshot.phase)
        val sandbox = match(full = false, duration = 1)
        repeat(11) { sandbox.advanceFrame(.1f) }
        assertEquals(0f, sandbox.uiState.value.elapsedSeconds)
        assertNotEquals(GamePhase.MATCH_OVER, sandbox.snapshot.phase)
    }

    @Test fun attackDefendSwitchesAttackerAtHalfTimeAndEndsAfterTwoHalves() {
        val vm = MatchViewModel(
            MatchOptions(
                true,
                false,
                BotDifficulty.MEDIUM,
                BotStyle.BALANCED,
                true,
                MatchTimingConfig(matchSeconds = 120),
                MatchTeamConfiguration(),
                playMode = MatchPlayMode.ATTACK_DEFEND
            )
        ).apply { onIntent(MatchIntent.ConfigureArena(340f, 580f)) }
        vm.onIntent(MatchIntent.KickoffSelected(GameTeam.RED))
        repeat(601) { vm.advanceFrame(.1f) }
        assertEquals(2, vm.currentHalf)
        assertEquals(GameTeam.BLUE, vm.snapshot.players.first { it.id == vm.snapshot.focusedPlayerId }.team)
        assertEquals(60f, vm.uiState.value.elapsedSeconds)
        repeat(601) { vm.advanceFrame(.1f) }
        assertEquals(GamePhase.MATCH_OVER, vm.snapshot.phase)
        assertEquals(120f, vm.uiState.value.elapsedSeconds)
    }

    @Test fun attackDefendAiDefendsFirstAndAttacksAfterHalfTime() {
        val vm = MatchViewModel(
            MatchOptions(
                true,
                true,
                BotDifficulty.MEDIUM,
                BotStyle.BALANCED,
                true,
                MatchTimingConfig(matchSeconds = 120),
                MatchTeamConfiguration(),
                playMode = MatchPlayMode.ATTACK_DEFEND
            )
        ).apply { onIntent(MatchIntent.ConfigureArena(340f, 580f)) }
        vm.onIntent(MatchIntent.KickoffSelected(GameTeam.RED))
        repeat(10) { vm.advanceFrame(.1f) }
        assertEquals(GamePhase.DEFENDING, vm.snapshot.phase)
        assertTrue(vm.isBotTurn)
        repeat(591) { vm.advanceFrame(.1f) }
        assertEquals(2, vm.currentHalf)
        repeat(30) { vm.advanceFrame(.1f) }
        assertEquals(GamePhase.DEFENDING, vm.snapshot.phase)
        assertFalse(vm.isBotTurn)
        assertTrue(vm.onIntent(MatchIntent.FinishDefense))
        assertTrue(vm.isBotTurn)
    }

    @Test fun attackDefendLetsAttackerMoveDuringTheSameAimTimer() {
        val vm = MatchViewModel(
            MatchOptions(
                true,
                false,
                BotDifficulty.MEDIUM,
                BotStyle.BALANCED,
                true,
                MatchTimingConfig(matchSeconds = 120),
                MatchTeamConfiguration(),
                playMode = MatchPlayMode.ATTACK_DEFEND
            )
        ).apply { onIntent(MatchIntent.ConfigureArena(340f, 580f)) }
        vm.onIntent(MatchIntent.KickoffSelected(GameTeam.RED))
        repeat(10) { vm.advanceFrame(.1f) }
        assertEquals(GameTeam.BLUE, vm.snapshot.formationTeam)
        val defender = vm.snapshot.players.first { it.team == GameTeam.BLUE && vm.canMovePlayer(it.id) }
        val defenderZone = (1..18).first {
            vm.isAdjacentZoneForPlayer(defender.id, it) && vm.legalPlayerPositionInZone(defender.id, it) != null
        }
        assertTrue(vm.onIntent(MatchIntent.MoveToZone(defender.id, defenderZone)))
        assertEquals(GamePhase.DEFENDING, vm.snapshot.phase)
        assertTrue(vm.canUndoPlayerMove())
        assertTrue(vm.onIntent(MatchIntent.Undo))
        assertEquals(defender.position, vm.snapshot.players.first { it.id == defender.id }.position)
        assertTrue(vm.onIntent(MatchIntent.MoveToZone(defender.id, defenderZone)))
        assertTrue(vm.onIntent(MatchIntent.FinishDefense))
        assertEquals(GamePhase.AIMING, vm.snapshot.phase)
        assertTrue(vm.snapshot.attackerPositioning)
        assertFalse(vm.canMovePlayer(defender.id))
        repeat(10) { vm.advanceFrame(.1f) }
        val remainingBeforeMove = vm.snapshot.phaseTimeRemaining
        val attacker = vm.snapshot.players.first { it.team == GameTeam.RED && vm.canMovePlayer(it.id) }
        val attackerZone = (1..18).first {
            vm.isAdjacentZoneForPlayer(attacker.id, it) && vm.legalPlayerPositionInZone(attacker.id, it) != null
        }
        assertTrue(vm.onIntent(MatchIntent.MoveToZone(attacker.id, attackerZone)))
        assertEquals(GamePhase.AIMING, vm.snapshot.phase)
        assertEquals(remainingBeforeMove, vm.snapshot.phaseTimeRemaining)
        assertFalse(vm.canMovePlayer(attacker.id))
        assertTrue(vm.canUndoPlayerMove())
        assertTrue(vm.onIntent(MatchIntent.Undo))
        assertEquals(attacker.position, vm.snapshot.players.first { it.id == attacker.id }.position)
        assertEquals(remainingBeforeMove, vm.snapshot.phaseTimeRemaining)
        assertTrue(vm.canMovePlayer(attacker.id))
        assertTrue(vm.onIntent(MatchIntent.MoveToZone(attacker.id, attackerZone)))
        vm.onIntent(MatchIntent.SetPower(.6f))
        assertFalse(vm.canUndoPlayerMove())
        assertFalse(vm.onIntent(MatchIntent.Undo))
        assertTrue(vm.onIntent(MatchIntent.Shoot))
        assertFalse(vm.canUndoPlayerMove())
    }

    @Test fun attackDefendCanKickWithoutMovingAnAttacker() {
        val vm = MatchViewModel(
            MatchOptions(
                true,
                false,
                BotDifficulty.MEDIUM,
                BotStyle.BALANCED,
                true,
                MatchTimingConfig(matchSeconds = 120),
                MatchTeamConfiguration(),
                playMode = MatchPlayMode.ATTACK_DEFEND
            )
        ).apply { onIntent(MatchIntent.ConfigureArena(340f, 580f)) }
        vm.onIntent(MatchIntent.KickoffSelected(GameTeam.RED))
        repeat(10) { vm.advanceFrame(.1f) }
        assertTrue(vm.onIntent(MatchIntent.FinishDefense))
        assertEquals(GamePhase.AIMING, vm.snapshot.phase)
        assertTrue(vm.onIntent(MatchIntent.SetPower(.6f)))
        assertTrue(vm.onIntent(MatchIntent.Shoot))
        assertEquals(GamePhase.BALL_MOVING, vm.snapshot.phase)
    }

    @Test fun validMovementAndUndoUseSameEngineRulesThroughIntents() {
        val vm = match(full = false, practice = true)
        vm.onIntent(MatchIntent.SetPracticeOpponents(2))
        val player = vm.snapshot.players.first { vm.canMovePlayer(it.id) }
        vm.onIntent(MatchIntent.BeginPlayerMove(player.id))
        vm.onIntent(MatchIntent.MovePlayer(player.id, com.yogi.foldball.domain.game.engine.Vector2(10f, 10f)))
        vm.onIntent(MatchIntent.EndPlayerMove(player.id))
        assertEquals(1, vm.defensiveMovesUsed)
        assertTrue(vm.onIntent(MatchIntent.Undo))
        assertEquals(0, vm.defensiveMovesUsed)
        assertEquals(player.position, vm.snapshot.players.first { it.id == player.id }.position)
    }
}

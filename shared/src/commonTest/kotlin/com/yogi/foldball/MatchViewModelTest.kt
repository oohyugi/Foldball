package com.yogi.foldball

import com.yogi.foldball.domain.game.bot.BotDifficulty
import com.yogi.foldball.domain.game.bot.BotStyle
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import com.yogi.foldball.domain.game.engine.MatchTimingConfig
import com.yogi.foldball.domain.team.TeamProfile
import com.yogi.foldball.feature.match.presentation.viewmodel.MatchIntent
import com.yogi.foldball.feature.match.presentation.viewmodel.MatchOptions
import com.yogi.foldball.feature.match.presentation.viewmodel.MatchViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MatchViewModelTest {
    private fun match(full: Boolean = true, duration: Int = 180): MatchViewModel {
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
                teams
            )
        ).apply { onIntent(MatchIntent.ConfigureArena(340f, 580f)) }
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

    @Test fun validMovementAndUndoUseSameEngineRulesThroughIntents() {
        val vm = match(full = false)
        val player = vm.snapshot.players.first { vm.canMovePlayer(it.id) }
        val zone = (1..18).first { vm.legalPlayerPositionInZone(player.id, it) != null }
        assertTrue(vm.onIntent(MatchIntent.MoveToZone(player.id, zone)))
        assertEquals(1, vm.defensiveMovesUsed)
        assertTrue(vm.onIntent(MatchIntent.Undo))
        assertEquals(0, vm.defensiveMovesUsed)
        assertEquals(player.position, vm.snapshot.players.first { it.id == player.id }.position)
    }
}

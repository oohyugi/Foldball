package com.yogi.foldball.feature.match.presentation.viewmodel

import com.yogi.foldball.domain.game.bot.BotDifficulty
import com.yogi.foldball.domain.game.bot.BotShot
import com.yogi.foldball.domain.game.bot.BotStyle
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameSnapshot
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import com.yogi.foldball.domain.game.engine.MatchTimingConfig
import com.yogi.foldball.domain.game.engine.Vector2
import kotlin.math.ceil

data class MatchOptions(
    val fullMatch: Boolean,
    val playAgainstBot: Boolean,
    val difficulty: BotDifficulty,
    val style: BotStyle,
    val standeeDamage: Boolean,
    val timing: MatchTimingConfig,
    val teams: MatchTeamConfiguration
)

data class MatchUiState(
    val snapshot: GameSnapshot? = null,
    val teams: MatchTeamConfiguration = MatchTeamConfiguration.Default,
    val openingKickoffTeam: GameTeam? = null,
    val paused: Boolean = false,
    val dentingEnabled: Boolean = false,
    val sessionId: Int = 0,
    val elapsedSeconds: Float = 0f,
    val durationSeconds: Int = 180
) {
    val secondsRemaining: Int get() = (durationSeconds - elapsedSeconds.toInt()).coerceAtLeast(0)
    val phaseSeconds: Int get() = ceil(snapshot?.phaseTimeRemaining ?: 0f).toInt().coerceAtLeast(0)
    val activeTeam: GameTeam get() {
        val current = snapshot ?: return GameTeam.RED
        val attacker = current.players.first { it.id == current.focusedPlayerId }.team
        return if (current.phase == GamePhase.DEFENDING) {
            if (attacker == GameTeam.RED) GameTeam.BLUE else GameTeam.RED
        } else {
            attacker
        }
    }
}

sealed interface MatchIntent {
    data class ConfigureArena(val width: Float, val height: Float) : MatchIntent
    data class KickoffSelected(val team: GameTeam) : MatchIntent
    data class SetPaused(val paused: Boolean) : MatchIntent
    data class SetDamage(val enabled: Boolean) : MatchIntent
    data class BeginPlayerMove(val id: String) : MatchIntent
    data class MovePlayer(val id: String, val delta: Vector2) : MatchIntent
    data class EndPlayerMove(val id: String) : MatchIntent
    data class MoveToZone(val id: String, val zone: Int) : MatchIntent
    data class AimBy(val delta: Float) : MatchIntent
    data class SetAim(val angle: Float) : MatchIntent
    data class SetPower(val power: Float) : MatchIntent
    data object Undo : MatchIntent
    data object FinishDefense : MatchIntent
    data object Shoot : MatchIntent
    data object Rematch : MatchIntent
}

data class PlannedBotShot(
    val shot: BotShot,
    val alternativeAngle: Float?,
    val playerId: String,
    val releaseAt: Float
)

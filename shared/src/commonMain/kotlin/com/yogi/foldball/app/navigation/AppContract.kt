package com.yogi.foldball.app.navigation

import com.yogi.foldball.domain.game.bot.BotDifficulty
import com.yogi.foldball.domain.game.bot.BotStyle
import com.yogi.foldball.domain.game.engine.PhysicsTuning
import com.yogi.foldball.domain.game.engine.MatchFormat
import com.yogi.foldball.domain.game.engine.MatchPlayMode
import com.yogi.foldball.domain.team.BallStyle
import com.yogi.foldball.domain.team.TeamProfile

sealed interface AppRoute {
    data object Loading : AppRoute
    data object Welcome : AppRoute
    data object Home : AppRoute
    data object Club : AppRoute
    data object Sandbox : AppRoute
    data object Match : AppRoute
}

enum class SandboxTab { MOVEMENT, PRACTICE, BALL, PLAYER }

data class AppUiState(
    val initialRoute: AppRoute = AppRoute.Loading,
    val profile: TeamProfile? = null,
    val playAgainstBot: Boolean = true,
    val difficulty: BotDifficulty = BotDifficulty.MEDIUM,
    val style: BotStyle = BotStyle.BALANCED,
    val matchFormat: MatchFormat = MatchFormat.THREE_V_THREE,
    val matchPlayMode: MatchPlayMode = MatchPlayMode.CLASSIC,
    val ballStyle: BallStyle = BallStyle.CLASSIC,
    val sandboxTab: SandboxTab = SandboxTab.MOVEMENT,
    val tuning: PhysicsTuning = PhysicsTuning(),
    val useTuningForMatches: Boolean = false,
    val threeActionTurns: Boolean = false,
    val settingsLoaded: Boolean = false,
    val settingsSaving: Boolean = false,
    val settingsError: String? = null,
    val error: String? = null
)

sealed interface AppIntent {
    data class SaveSettings(val tuning: PhysicsTuning, val useForMatches: Boolean, val threeActionTurns: Boolean = false) : AppIntent
    data object RetrySettings : AppIntent
    data object RetryProfile : AppIntent
    data object EditClub : AppIntent
    data class ClubSaved(val profile: TeamProfile) : AppIntent
    data class Play(val againstBot: Boolean) : AppIntent
    data class DifficultySelected(val difficulty: BotDifficulty) : AppIntent
    data class StyleSelected(val style: BotStyle) : AppIntent
    data class MatchFormatSelected(val format: MatchFormat) : AppIntent
    data class MatchPlayModeSelected(val mode: MatchPlayMode) : AppIntent
    data class BallStyleSelected(val style: BallStyle) : AppIntent
    data class OpenSandbox(val tab: SandboxTab = SandboxTab.MOVEMENT) : AppIntent
    data class SelectSandboxTab(val tab: SandboxTab) : AppIntent
}

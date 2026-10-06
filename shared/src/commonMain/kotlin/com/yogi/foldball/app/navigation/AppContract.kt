package com.yogi.foldball.app.navigation

import com.yogi.foldball.domain.game.bot.BotDifficulty
import com.yogi.foldball.domain.game.bot.BotStyle
import com.yogi.foldball.domain.team.TeamProfile

sealed interface AppRoute {
    data object Loading : AppRoute
    data object Home : AppRoute
    data object Team : AppRoute
    data object Sandbox : AppRoute
    data object Match : AppRoute
}

enum class SandboxTab { MOVEMENT, BALL, PLAYER }

data class AppUiState(
    val route: AppRoute = AppRoute.Loading,
    val entryId: Int = 0,
    val profile: TeamProfile? = null,
    val playAgainstBot: Boolean = true,
    val difficulty: BotDifficulty = BotDifficulty.MEDIUM,
    val style: BotStyle = BotStyle.BALANCED,
    val sandboxTab: SandboxTab = SandboxTab.MOVEMENT,
    val error: String? = null
)

sealed interface AppIntent {
    data object RetryProfile : AppIntent
    data object Home : AppIntent
    data object EditTeam : AppIntent
    data class TeamSaved(val profile: TeamProfile) : AppIntent
    data class Play(val againstBot: Boolean) : AppIntent
    data class DifficultySelected(val difficulty: BotDifficulty) : AppIntent
    data class StyleSelected(val style: BotStyle) : AppIntent
    data class OpenSandbox(val tab: SandboxTab = SandboxTab.MOVEMENT) : AppIntent
    data class SelectSandboxTab(val tab: SandboxTab) : AppIntent
}

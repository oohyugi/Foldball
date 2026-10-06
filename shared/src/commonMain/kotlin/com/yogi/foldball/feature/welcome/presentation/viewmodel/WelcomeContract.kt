package com.yogi.foldball.feature.welcome.presentation.viewmodel

import com.yogi.foldball.domain.team.ClubBadges
import com.yogi.foldball.domain.team.TeamProfile

// Form state belongs to this feature; match-only identities are mapped at the app boundary.
data class WelcomeUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val name: String = "",
    val shortName: String = "",
    val badgeId: Int = ClubBadges.ids.first(),
    val saving: Boolean = false,
    val error: String? = null
) {
    val profile: TeamProfile? get() = TeamProfile.create(
        name,
        badgeId,
        shortName.takeIf {
            it.isNotBlank()
        }
    )
    val canSave: Boolean get() = !loading && !loadFailed && !saving && profile != null
}

sealed interface WelcomeIntent {
    data class NameChanged(val name: String) : WelcomeIntent
    data class ShortNameChanged(val shortName: String) : WelcomeIntent
    data class BadgeSelected(val id: Int) : WelcomeIntent
    data object Save : WelcomeIntent
    data object Retry : WelcomeIntent
}

sealed interface WelcomeEffect {
    data class Saved(val profile: TeamProfile) : WelcomeEffect
}

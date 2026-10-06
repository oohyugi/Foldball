package com.yogi.foldball.feature.team.presentation.viewmodel

import com.yogi.foldball.domain.team.ClubBadges
import com.yogi.foldball.domain.team.TeamProfile

// Form state belongs to this feature; match-only identities are mapped at the app boundary.
data class TeamUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val name: String = "",
    val shortName: String = "",
    val badgeId: Int = ClubBadges.ids.first(),
    val editing: Boolean = false,
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

sealed interface TeamIntent {
    data class NameChanged(val name: String) : TeamIntent
    data class ShortNameChanged(val shortName: String) : TeamIntent
    data class BadgeSelected(val id: Int) : TeamIntent
    data object Save : TeamIntent
    data object Retry : TeamIntent
    data object Cancel : TeamIntent
}

sealed interface TeamEffect {
    data class Saved(val profile: TeamProfile) : TeamEffect
    data object Cancelled : TeamEffect
}

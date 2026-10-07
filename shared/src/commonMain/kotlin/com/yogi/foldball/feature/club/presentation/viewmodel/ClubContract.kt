package com.yogi.foldball.feature.club.presentation.viewmodel

import com.yogi.foldball.domain.team.ClubBadges
import com.yogi.foldball.domain.team.JerseyPattern
import com.yogi.foldball.domain.team.TeamProfile

// Form state belongs to this feature; match-only identities are mapped at the app boundary.
data class ClubUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val name: String = "",
    val shortName: String = "",
    val badgeId: Int = ClubBadges.ids.first(),
    val jerseyColorCount: Int = 1,
    val jerseyPattern: JerseyPattern? = null,
    val editing: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null
) {
    val profile: TeamProfile? get() = TeamProfile.create(
        name,
        badgeId,
        shortName.takeIf {
            it.isNotBlank()
        },
        jerseyPattern,
        jerseyColorCount
    )
    val canSave: Boolean get() = !loading && !loadFailed && !saving && profile != null
}

sealed interface ClubIntent {
    data class ColorCountSelected(val count: Int) : ClubIntent
    data class PatternSelected(val pattern: JerseyPattern?) : ClubIntent
    data class NameChanged(val name: String) : ClubIntent
    data class ShortNameChanged(val shortName: String) : ClubIntent
    data class BadgeSelected(val id: Int) : ClubIntent
    data object Save : ClubIntent
    data object Retry : ClubIntent
    data object Cancel : ClubIntent
}

sealed interface ClubEffect {
    data class Saved(val profile: TeamProfile) : ClubEffect
    data object Cancelled : ClubEffect
}

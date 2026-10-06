package com.yogi.foldball.domain.team

/** Team presentation data stays separate from the RED/BLUE gameplay sides. */
data class TeamIdentity(
    val name: String,
    val jerseyLabel: String = name.filter(
        Char::isLetterOrDigit
    ).uppercase().take(MAX_JERSEY_LABEL_LENGTH),
    val logoId: Int? = null
) {
    init {
        require(logoId == null || logoId in ClubBadges.ids) { "Unknown club badge" }
        require(name.isNotBlank()) { "Team name must not be blank" }
        require(jerseyLabel.isNotBlank() && jerseyLabel.length <= MAX_JERSEY_LABEL_LENGTH) {
            "Jersey label must contain 1–$MAX_JERSEY_LABEL_LENGTH characters"
        }
    }

    companion object {
        const val MAX_JERSEY_LABEL_LENGTH = 4
    }
}

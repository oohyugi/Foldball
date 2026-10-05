package com.yogi.foldball.domain.game.engine

/** Team presentation data stays separate from the RED/BLUE gameplay sides. */
data class TeamIdentity(
    val name: String,
    val jerseyLabel: String = name.filter(Char::isLetterOrDigit).uppercase().take(MAX_JERSEY_LABEL_LENGTH),
) {
    init {
        require(name.isNotBlank()) { "Team name must not be blank" }
        require(jerseyLabel.isNotBlank() && jerseyLabel.length <= MAX_JERSEY_LABEL_LENGTH) {
            "Jersey label must contain 1–$MAX_JERSEY_LABEL_LENGTH characters"
        }
    }

    companion object {
        const val MAX_JERSEY_LABEL_LENGTH = 4
    }
}

data class MatchTeamConfiguration(
    val red: TeamIdentity = TeamIdentity("INDO"),
    val blue: TeamIdentity = TeamIdentity("THAI"),
) {
    fun forTeam(team: GameTeam): TeamIdentity = when (team) {
        GameTeam.RED -> red
        GameTeam.BLUE -> blue
    }

    companion object {
        val Default = MatchTeamConfiguration()
    }
}

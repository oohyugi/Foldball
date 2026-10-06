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
    val red: TeamIdentity = TeamIdentity("Player", "PLYR"),
    val blue: TeamIdentity = TeamIdentity("Player 2", "P2"),
) {
    fun forTeam(team: GameTeam): TeamIdentity = when (team) {
        GameTeam.RED -> red
        GameTeam.BLUE -> blue
    }

    companion object {
        val Default = MatchTeamConfiguration()
    }
}

/** Pick an opponent once per match; gameplay still uses the BLUE side. */
fun MatchTeamConfiguration.withRandomAiOpponent(random: kotlin.random.Random = kotlin.random.Random.Default): MatchTeamConfiguration {
    val choices = listOf(
        "Northchester City", "Royal Montara", "Catalora FC", "Bavern Athletic",
        "Turinova FC", "Merseyvale FC", "Rosavelli FC", "Blueford FC",
    )
        .map { TeamIdentity(it) }.filter { !it.name.equals(red.name, ignoreCase = true) && it.jerseyLabel != red.jerseyLabel }
    return copy(blue = choices.random(random))
}

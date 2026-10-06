package com.yogi.foldball.domain.team

import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import com.yogi.foldball.domain.team.TeamIdentity

/** Curated fictional club names; badge IDs are chosen without replacement within a match. */
fun MatchTeamConfiguration.withRandomAiOpponent(
    random: kotlin.random.Random = kotlin.random.Random.Default
): MatchTeamConfiguration {
    val choices = listOf(
        "Northchester City",
        "Royal Montara",
        "Catalora FC",
        "Bavern Athletic",
        "Turinova FC",
        "Merseyvale FC",
        "Rosavelli FC",
        "Blueford FC"
    )
        .map { TeamIdentity(it) }.filter {
            !it.name.equals(red.name, ignoreCase = true) &&
                it.jerseyLabel != red.jerseyLabel
        }
    return copy(
        blue = choices.random(random).copy(
            logoId = ClubBadges.ids.filter {
                it != red.logoId
            }.random(random)
        )
    )
}

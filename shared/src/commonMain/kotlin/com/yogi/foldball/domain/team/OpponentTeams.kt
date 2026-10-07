package com.yogi.foldball.domain.team

import com.yogi.foldball.domain.game.bot.BotStyle
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration

/** Difficulty changes skill; tactical style determines a stable fictional club identity. */
fun MatchTeamConfiguration.withAiOpponent(style: BotStyle): MatchTeamConfiguration {
    val opponent = when (style) {
        BotStyle.BALANCED -> TeamIdentity("Paperford FC", "PAPF", 20, JerseyPattern.STRIPES, 2)
        BotStyle.PARK_BUS -> TeamIdentity("Ironwood FC", "IRON", 18, JerseyPattern.CHEVRON, 2)
        BotStyle.KICK_RUSH -> TeamIdentity("Falcon United", "FALC", 19, JerseyPattern.PLAIN, 2)
        BotStyle.TOTAL_FOOTBALL -> TeamIdentity("Aurora Athletic", "AURA", 24, JerseyPattern.HOOPS, 2)
    }
    val alternateBadge = when (style) {
        BotStyle.BALANCED -> 9
        BotStyle.PARK_BUS -> 4
        BotStyle.KICK_RUSH -> 22
        BotStyle.TOTAL_FOOTBALL -> 8
    }
    return copy(blue = if (opponent.logoId == red.logoId) opponent.copy(logoId = alternateBadge) else opponent)
}

/** Local Player 2 keeps its identity and receives a distinct badge when none was supplied. */
fun MatchTeamConfiguration.withLocalOpponentBadge(): MatchTeamConfiguration = copy(
    blue = if (blue.logoId == null || blue.logoId == red.logoId) {
        blue.copy(logoId = ClubBadges.ids.first { it != red.logoId })
    } else {
        blue
    }
)

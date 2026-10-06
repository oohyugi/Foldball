package com.yogi.foldball.domain.game.engine

import com.yogi.foldball.domain.team.TeamIdentity

data class MatchTeamConfiguration(
    val red: TeamIdentity = TeamIdentity("Player", "PLYR"),
    val blue: TeamIdentity = TeamIdentity("Player 2", "P2")
) {
    fun forTeam(team: GameTeam): TeamIdentity = when (team) {
        GameTeam.RED -> red
        GameTeam.BLUE -> blue
    }

    companion object {
        val Default = MatchTeamConfiguration()
    }
}

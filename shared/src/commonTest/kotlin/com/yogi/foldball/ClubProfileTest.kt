package com.yogi.foldball
import com.yogi.foldball.domain.game.engine.*
import com.yogi.foldball.domain.team.TeamProfile
import com.yogi.foldball.domain.team.withRandomAiOpponent
import kotlin.random.Random
import kotlin.test.*
class ClubProfileTest {
    @Test fun invalidNamesAndBadgesCannotCreateProfile() {
        assertEquals("PUFC", TeamProfile.create("Paper United", 1, "pufc")?.shortName)
        assertNull(TeamProfile.create("Paper United", 1, "TOOLONG"))
        assertNull(TeamProfile.create("Paper United", 1, "A B"))
        assertNull(TeamProfile.create(" ", 1))
        assertNull(TeamProfile.create("x".repeat(21), 1))
        assertNull(TeamProfile.create("My Club", 0))
        assertEquals("My Club", TeamProfile.create(" My Club ", 1)?.name)
        assertEquals("CLUB", TeamProfile.create("⚽", 1)?.shortName)
    }

    @Test fun everyPlayerBadgeGetsDifferentOpponentBadge() {
        for (id in 1..16) {
            repeat(30) { seed ->
                val team = requireNotNull(TeamProfile.create("My Club", id)).toMatchIdentity()
                val config = MatchTeamConfiguration(red = team).withRandomAiOpponent(Random(seed))
                assertEquals(team, config.red)
                assertNotEquals(team.logoId, config.blue.logoId)
                assertTrue(requireNotNull(config.blue.logoId) in 1..16)
            }
        }
    }
}

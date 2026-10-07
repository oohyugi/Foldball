package com.yogi.foldball

import com.yogi.foldball.domain.team.ClubBadges
import com.yogi.foldball.domain.team.JerseyPattern
import com.yogi.foldball.domain.team.TeamProfile
import com.yogi.foldball.gamekit.presentation.team.ClubKit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClubKitTest {
    @Test fun paletteHasFourStableColorsForEveryLogo() {
        for (id in ClubBadges.ids) {
            val palette = ClubKit.palette(id)
            assertEquals(4, palette.size)
            assertEquals(ClubKit.primary(id), palette.first())
            assertTrue(palette.distinct().size >= 3)
        }
    }

    @Test fun colorCountsAreValidatedAndLegacyTwoToneKeepsTwoColors() {
        assertNull(TeamProfile.create("Club", 1, jerseyColorCount = 0))
        assertNull(TeamProfile.create("Club", 1, jerseyColorCount = 5))
        assertEquals(2, TeamProfile.create("Club", 1, jerseyPattern = JerseyPattern.TWO_TONE)?.jerseyColorCount)
        assertEquals(4, TeamProfile.create("Club", 20, jerseyPattern = JerseyPattern.STRIPES, jerseyColorCount = 4)?.toMatchIdentity()?.jerseyColorCount)
    }

    @Test fun everyCrestPairGetsDistinctMatchKits() {
        for (player in ClubBadges.ids) {
            for (opponent in ClubBadges.ids) {
                val home = ClubKit.primary(player)
                val away = ClubKit.opponent(home, opponent)
                assertTrue(ClubKit.distance(home, away) >= .09f, "Kit clash: $player vs $opponent")
            }
        }
    }
}

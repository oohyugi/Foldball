package com.yogi.foldball.gamekit.presentation.team

import androidx.compose.ui.graphics.Color
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration

/** Curated crest colors: stable across runs, applied only to jersey panels. */
object ClubKit {
    private val navy = Color(0xFF17416A)
    private val green = Color(0xFF17634B)
    private val teal = Color(0xFF12646A)
    private val purple = Color(0xFF6738A4)
    private val orange = Color(0xFFA54122)
    private val burgundy = Color(0xFF9E2834)
    private val blue = Color(0xFF205F97)
    private val gold = Color(0xFF936400)
    private val colors = listOf(
        navy, green, navy, green, blue, teal, green, gold,
        purple, blue, green, orange, teal, orange, purple, burgundy,
        burgundy, green, blue, purple, teal, green, purple, teal, orange
    )

    /** Crest-specific accents, padded with matching neutrals when needed. */
    fun palette(badgeId: Int?): List<Color> {
        val accents = when (badgeId) {
            1, 3 -> listOf(0xFFD64545, 0xFFF6F1E4, 0xFF17416A)
            2, 4, 7 -> listOf(0xFFF6F1E4, 0xFF17416A, 0xFF32B79A)
            5, 6, 10, 19 -> listOf(0xFF32C7DC, 0xFFF6F1E4, 0xFF17416A)
            8 -> listOf(0xFFF2BC55, 0xFF5C9A96, 0xFFF6F1E4)
            9 -> listOf(0xFF8ECB9C, 0xFFF6F1E4, 0xFF42216D)
            11 -> listOf(0xFFC5DC4D, 0xFFF6F1E4, 0xFF17416A)
            12, 14 -> listOf(0xFFF0C68A, 0xFF3A7881, 0xFF17416A)
            13, 21 -> listOf(0xFFEF2D83, 0xFF32DDD3, 0xFF172D40)
            15, 20, 23 -> listOf(0xFFF4D34A, 0xFFB6A2F2, 0xFF42216D)
            16, 25 -> listOf(0xFFF4D34A, 0xFF76BEB9, 0xFFD64545)
            17 -> listOf(0xFFF2AD28, 0xFFEA6023, 0xFF601F27)
            18 -> listOf(0xFFF6F1E4, 0xFF154A49, 0xFFB4CDBB)
            22 -> listOf(0xFF38A8C8, 0xFFF6F1E4, 0xFF17416A)
            24 -> listOf(0xFFE87724, 0xFFF6F1E4, 0xFF17534C)
            else -> listOf(0xFFF6F1E4, 0xFF17416A, 0xFFF4D34A)
        }
        return listOf(primary(badgeId)) + accents.map { Color(it) }
    }

    /** Dark crest-compatible companion keeps the paper printing readable. */
    fun accent(primary: Color): Color = if (distance(primary, navy) >= .09f) navy else gold

    fun primary(badgeId: Int?): Color = colors.getOrNull((badgeId ?: 0) - 1) ?: FoldballColor.TeamRed

    fun distance(first: Color, second: Color): Float = (first.red - second.red) * (first.red - second.red) +
        (first.green - second.green) * (first.green - second.green) +
        (first.blue - second.blue) * (first.blue - second.blue)

    fun opponent(player: Color, badgeId: Int?): Color {
        val preferred = if (badgeId == null) FoldballColor.InkBlue else primary(badgeId)
        if (distance(player, preferred) >= .09f) return preferred
        return listOf(navy, green, teal, purple, orange, burgundy, blue, gold)
            .maxBy { distance(player, it) }
    }
}

fun MatchTeamConfiguration.kitColor(team: GameTeam): Color {
    val player = if (red.logoId == null) FoldballColor.TeamRed else ClubKit.primary(red.logoId)
    return if (team == GameTeam.RED) player else ClubKit.opponent(player, blue.logoId)
}

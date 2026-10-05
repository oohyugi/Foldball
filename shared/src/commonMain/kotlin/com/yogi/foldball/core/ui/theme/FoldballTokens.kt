package com.yogi.foldball.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.patrick_hand
import foldball.shared.generated.resources.permanent_marker
import org.jetbrains.compose.resources.Font

object FoldballColor {
    val Wood = Color(0xFF9A6A3C)
    val WoodDark = Color(0xFF6F4524)
    val Paper = Color(0xFFF6F1E4)
    val PaperLine = Color(0xFFC9D4EA)
    val PaperEdge = Color(0xFFC9B895)
    val Ink = Color(0xFF1C1C22)
    val InkBlue = Color(0xFF1F4FC4)
    val TeamRed = Color(0xFFD64545)
    val Tape = Color(0xFFF4D34A)
    val Grass = Color(0xFF3F8F43)
    val Cardboard = Color(0xFFC99A62)
    val InkMuted = Color(0xFF777A80)
    val FieldPaper = Color(0xFFE8E1D4)
    val FieldLine = Color(0xFFC7BFAF)
    val FieldHighlight = Color(0xFFF1EBE0)
    val BallHighlight = Color(0xFFF9F6F0)
    val ShadowBrown = Color(0xFF51483D)
    val ShadowGrass = Color(0xFF0B2A0E)
    val Shadow = Color(0xFF352213)
    val ShadowBlue = Color(0xFF173C94)
}

object FoldballSpace {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

/** Shared paper rotation recipes, in degrees. Keep layout-specific rotation out of screens. */
object FoldballTilt {
    const val Hero = -1.6f
    const val MenuCardOne = 1.0f
    const val MenuCardTwo = -1.2f
    const val MenuCardThree = -0.8f
    const val MenuCardFour = 1.3f
    const val PaperCard = 1.0f
    const val Banner = -0.6f
    const val Scoreboard = -0.6f
    const val StatusPanel = 0.5f
    const val StatusPanelAlternate = -0.8f
    const val GoalCelebration = -0.8f
    const val OpeningToss = -0.8f
    const val MatchMenu = 0.6f
    const val TapeLabel = -1.0f
    const val Toast = -0.8f
}

/** Shared shadow geometry and colors for paper surfaces. */
object FoldballShadow {
    val offsetY = 4.dp
    const val paperOpacity = .45f
    val paperColor = FoldballColor.Shadow
    val primaryColor = FoldballColor.ShadowBlue
}

object FoldballType {
    @Composable
    private fun marker() = FontFamily(Font(Res.font.permanent_marker, FontWeight.Normal))

    @Composable
    private fun hand() = FontFamily(Font(Res.font.patrick_hand, FontWeight.Normal))

    val Display: TextStyle
        @Composable get() = TextStyle(fontFamily = marker(), fontSize = 58.sp, letterSpacing = (-1).sp)
    val LargeTitle: TextStyle
        @Composable get() = TextStyle(fontFamily = marker(), fontSize = 40.sp)
    val Title: TextStyle
        @Composable get() = TextStyle(fontFamily = marker(), fontSize = 26.sp)
    val Label: TextStyle
        @Composable get() = TextStyle(fontFamily = hand(), fontSize = 20.sp, letterSpacing = 1.2.sp)
    val Body: TextStyle
        @Composable get() = TextStyle(fontFamily = hand(), fontSize = 17.sp, lineHeight = 24.sp)
    val Caption: TextStyle
        @Composable get() = TextStyle(fontFamily = hand(), fontSize = 14.sp, letterSpacing = 1.1.sp)
}

@Composable
fun FoldballTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = FoldballColor.InkBlue,
            onPrimary = Color.White,
            surface = FoldballColor.Paper,
            onSurface = FoldballColor.Ink,
            error = FoldballColor.TeamRed,
            background = FoldballColor.Wood,
        ),
        content = content,
    )
}

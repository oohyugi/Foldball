package com.yogi.foldball.feature.home.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.CrumpledBall
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import com.yogi.foldball.domain.team.BallStyle
import com.yogi.foldball.domain.team.JerseyPattern
import com.yogi.foldball.gamekit.presentation.player.PaperPlayer
import com.yogi.foldball.gamekit.presentation.player.PlayerFacing
import com.yogi.foldball.gamekit.presentation.team.ClubKit
import com.yogi.foldball.gamekit.presentation.team.ClubLogo
import com.yogi.foldball.gamekit.presentation.team.kitColor

/** Decorative portrait-pitch crop, using the same paper standees as Match. */
@Composable
fun MenuField(modifier: Modifier = Modifier, ballStyle: BallStyle = BallStyle.CLASSIC, teams: MatchTeamConfiguration = MatchTeamConfiguration.Default) {
    val idle by rememberInfiniteTransition(label = "home-ball").animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "paper-rock"
    )
    PaperCard(
        modifier = modifier.fillMaxWidth().height(150.dp).clearAndSetSemantics { },
        tilt = FoldballTilt.StatusPanelAlternate,
        ruled = false,
        color = FoldballColor.Grass
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                val rail = 8.dp.toPx()
                val line = 1.dp.toPx()
                val markings = FoldballColor.Paper.copy(alpha = .8f)
                // Quiet paper bands retain contrast beneath the standees.
                repeat(6) { band ->
                    if (band % 2 == 0) {
                        drawRect(
                            FoldballColor.Paper.copy(alpha = .035f),
                            Offset(rail, band * size.height / 6f),
                            Size(size.width - rail * 2, size.height / 6f)
                        )
                    }
                }
                val left = rail + 8.dp.toPx()
                val right = size.width - left
                drawLine(markings, Offset(left, 0f), Offset(left, size.height), line)
                drawLine(markings, Offset(right, 0f), Offset(right, size.height), line)
                drawLine(markings, Offset(left, center.y), Offset(right, center.y), line)
                drawCircle(markings, 26.dp.toPx(), center, style = Stroke(line))
                drawCircle(markings, 2.dp.toPx(), center)
                drawRect(FoldballColor.Cardboard, size = Size(rail, size.height))
                drawRect(FoldballColor.Cardboard, Offset(size.width - rail, 0f), Size(rail, size.height))
                drawLine(FoldballColor.ShadowBrown.copy(alpha = .4f), Offset(rail, 0f), Offset(rail, size.height), line)
                drawLine(FoldballColor.ShadowBrown.copy(alpha = .4f), Offset(size.width - rail, 0f), Offset(size.width - rail, size.height), line)
            }
            val playerSize = 52.dp
            val positions = listOf(
                Triple(.28f, .27f, "9"),
                Triple(.72f, .27f, "7"),
                Triple(.28f, .73f, "10"),
                Triple(.72f, .73f, "8")
            )
            positions.forEachIndexed { index, (x, y, identity) ->
                val blue = index < 2
                val side = if (blue) GameTeam.BLUE else GameTeam.RED
                val club = teams.forTeam(side)
                PaperPlayer(
                    modifier = Modifier
                        .offset(maxWidth * x - playerSize / 2, maxHeight * y - playerSize / 2)
                        .size(playerSize)
                        .graphicsLayer(rotationZ = if (index % 2 == 0) -2f else 2f),
                    facing = if (blue) PlayerFacing.FRONT else PlayerFacing.BACK,
                    jerseyColorCount = club.jerseyColorCount,
                    jerseyPalette = ClubKit.palette(club.logoId),
                    jerseyPattern = club.jerseyPattern ?: JerseyPattern.PLAIN,
                    jerseyColor = teams.kitColor(side),
                    number = identity,
                    name = club.jerseyLabel
                )
            }
            ClubLogo(teams.red.logoId, "Club badge", Modifier.align(Alignment.TopCenter).offset(y = 18.dp).size(24.dp))
            CrumpledBall(
                modifier = Modifier.align(Alignment.Center).graphicsLayer(rotationZ = idle),
                size = 28.dp,
                ballStyle = ballStyle
            )
        }
    }
}

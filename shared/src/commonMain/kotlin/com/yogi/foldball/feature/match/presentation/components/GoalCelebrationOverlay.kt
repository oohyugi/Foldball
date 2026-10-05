package com.yogi.foldball.feature.match.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballShapes
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.PaperCard
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GoalCelebrationOverlay(
    visible: Boolean,
    scoringTeam: GameTeam?,
    teamConfiguration: MatchTeamConfiguration = MatchTeamConfiguration.Default,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(140)) + scaleIn(
                initialScale = .55f,
                animationSpec = spring(dampingRatio = .48f, stiffness = 360f),
            ),
            exit = fadeOut(tween(180)) + scaleOut(
                targetScale = 1.12f,
                animationSpec = tween(180),
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                val teamColor = if (scoringTeam == GameTeam.RED) {
                    FoldballColor.TeamRed
                } else {
                    FoldballColor.InkBlue
                }
                Canvas(Modifier.size(216.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val innerRadius = size.minDimension * .33f
                    val outerRadius = size.minDimension * .49f
                    repeat(12) { index ->
                        val angle = index * (2f * PI.toFloat() / 12f)
                        drawLine(
                            color = FoldballColor.Tape.copy(alpha = .7f),
                            start = center + Offset(cos(angle), sin(angle)) * innerRadius,
                            end = center + Offset(cos(angle), sin(angle)) * outerRadius,
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                    drawCircle(
                        color = FoldballColor.Tape.copy(alpha = .7f),
                        radius = size.minDimension * .39f,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
                PaperCard(
                    modifier = Modifier.padding(horizontal = 28.dp),
                    shape = FoldballShapes.Banner,
                    tilt = FoldballTilt.GoalCelebration,
                    ruled = false,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 17.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "GOOOL!",
                            color = teamColor,
                            style = FoldballType.LargeTitle,
                            letterSpacing = .5.sp,
                        )
                        Text(
                            text = "TEAM ${scoringTeam?.let { teamConfiguration.forTeam(it).name }.orEmpty()}",
                            color = FoldballColor.Ink,
                            style = FoldballType.Label,
                        )
                    }
                }
            }
        }
    }
}

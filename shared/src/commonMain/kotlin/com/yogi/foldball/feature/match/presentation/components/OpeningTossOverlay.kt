package com.yogi.foldball.feature.match.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import com.yogi.foldball.gamekit.presentation.ball.PaperBall
import com.yogi.foldball.gamekit.presentation.ball.MatchBallVisualSize
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballShapes
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.PaperButton
import com.yogi.foldball.core.ui.theme.PaperCard
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.paper_crumple_sheet_v1
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.imageResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.random.Random

/** One opening toss per match. The chosen team is independent of bounce decoration. */
@Composable
fun OpeningTossOverlay(
    onWinner: (GameTeam) -> Unit,
    modifier: Modifier = Modifier,
    teamConfiguration: MatchTeamConfiguration = MatchTeamConfiguration.Default,
) {
    val crumple = remember { Animatable(0f) }
    val bounce = remember { Animatable(0f) }
    val crumpleSheet = imageResource(Res.drawable.paper_crumple_sheet_v1)
    var winner by remember { mutableStateOf<GameTeam?>(null) }
    var horizontalLanding by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) { crumple.animateTo(1f, tween(1200, easing = LinearEasing)) }
    LaunchedEffect(winner) {
        val selected = winner ?: return@LaunchedEffect
        bounce.animateTo(1f, tween(2100))
        delay(950)
        onWinner(selected)
    }

    BoxWithConstraints(modifier.fillMaxSize().background(Color(0x55281C13))) {
        val progress = bounce.value
        val landingDirection = if (winner == GameTeam.BLUE) -1f else 1f
        val centerX = maxWidth * (.5f + horizontalLanding * progress +
            .11f * sin(progress * PI.toFloat() * 4f) * (1f - progress))
        val groundY = maxHeight * (.5f + landingDirection * .24f * progress)
        val hop = maxHeight * (.17f * abs(sin(progress * PI.toFloat() * 3.5f)) * (1f - progress))
        val ballY = groundY - hop
        val ballSize = MatchBallVisualSize

        PaperCard(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 46.dp),
            shape = FoldballShapes.Banner,
            tilt = FoldballTilt.OpeningToss,
            ruled = false,
        ) {
            Column(
                Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text("KICKOFF TOSS", style = FoldballType.Label, color = FoldballColor.Ink)
                if (winner != null) {
                    Text(
                        when {
                            progress < 1f -> "The paper ball is flipping..."
                            else -> "TEAM ${winner?.let { teamConfiguration.forTeam(it).name }.orEmpty()} KICKS OFF!"
                        },
                        color = when {
                            progress < 1f -> FoldballColor.Ink
                            winner == GameTeam.BLUE -> FoldballColor.InkBlue
                            else -> FoldballColor.TeamRed
                        },
                        style = FoldballType.Caption,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (crumple.value < 1f) {
            val frame = (crumple.value * 8f).toInt().coerceIn(0, 7)
            val frameSize = 80.dp + (ballSize - 80.dp) * crumple.value
            val col = frame % 4
            val row = frame / 4
            val srcLeft = col * crumpleSheet.width / 4
            val srcTop = row * crumpleSheet.height / 2
            val srcRight = (col + 1) * crumpleSheet.width / 4
            val srcBottom = (row + 1) * crumpleSheet.height / 2
            Canvas(
                Modifier.align(Alignment.TopStart)
                    .padding(start = maxWidth / 2 - frameSize / 2, top = maxHeight / 2 - frameSize / 2)
                    .size(frameSize)
                    .alpha(((1f - crumple.value) / .18f).coerceIn(0f, 1f)),
            ) {
                drawImage(
                    image = crumpleSheet,
                    srcOffset = IntOffset(srcLeft, srcTop),
                    srcSize = IntSize(srcRight - srcLeft, srcBottom - srcTop),
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.Medium,
                )
            }
        }

        if (crumple.value > .82f) {
            Canvas(
                Modifier.align(Alignment.TopStart)
                    .padding(start = centerX - 12.dp, top = groundY + 6.dp)
                    .size(24.dp, 6.dp),
            ) {
                drawOval(
                    Brush.radialGradient(listOf(Color(0x88402F20), Color.Transparent)),
                    size = size,
                )
            }
            PaperBall(
                modifier = Modifier.align(Alignment.TopStart)
                    .padding(start = centerX - ballSize / 2, top = ballY - ballSize / 2)
                    .size(ballSize)
                    .alpha(((crumple.value - .82f) / .18f).coerceIn(0f, 1f)),
                rotationX = progress * 17f,
                rotationY = progress * 9f,
            )
        }

        if (winner == null && crumple.value >= 1f) {
            PaperTossButton(
                onClick = {
                    winner = if (Random.nextBoolean()) GameTeam.RED else GameTeam.BLUE
                    horizontalLanding = Random.nextFloat() * .34f - .17f
                },
                modifier = Modifier.align(Alignment.Center).offset(y = 52.dp),
            )
        }
    }
}

@Composable
private fun PaperTossButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PaperButton("Start", onClick, modifier = modifier)
}

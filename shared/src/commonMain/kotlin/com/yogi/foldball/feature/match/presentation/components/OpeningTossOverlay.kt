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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogi.foldball.core.audio.LocalGameAudio
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballShapes
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.PaperButton
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import com.yogi.foldball.gamekit.presentation.ball.MatchBallVisualSize
import com.yogi.foldball.gamekit.presentation.ball.PaperBall
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.paper_crumple_sheet_v1
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.imageResource

/** One opening toss per match. The chosen team is independent of bounce decoration. */
@Composable
fun OpeningTossOverlay(
    onWinner: (GameTeam) -> Unit,
    modifier: Modifier = Modifier,
    teamConfiguration: MatchTeamConfiguration = MatchTeamConfiguration.Default
) {
    val crumple = remember { Animatable(0f) }
    val bounce = remember { Animatable(0f) }
    val crumpleSheet = imageResource(Res.drawable.paper_crumple_sheet_v1)
    var winner by remember { mutableStateOf<GameTeam?>(null) }
    var horizontalLanding by remember { mutableFloatStateOf(0f) }
    var trajectory by remember { mutableStateOf<TossTrajectory?>(null) }

    val audio = LocalGameAudio.current
    val crumpleLoaded = audio?.isLoaded("crumple") ?: true
    LaunchedEffect(crumpleLoaded) {
        if (!crumpleLoaded) return@LaunchedEffect
        audio?.play("crumple", .55f)
        crumple.animateTo(1f, tween(1200, easing = LinearEasing))
    }
    LaunchedEffect(winner) {
        val selected = winner ?: return@LaunchedEffect
        val toss = trajectory ?: return@LaunchedEffect
        bounce.animateTo(
            toss.duration,
            tween((toss.duration * 1000f).roundToInt(), easing = LinearEasing)
        )
        delay(950)
        onWinner(selected)
    }

    BoxWithConstraints(modifier.fillMaxSize().background(Color(0x55281C13))) {
        val toss = trajectory
        val pose = toss?.sample(bounce.value) ?: TossPose(0f, 0f, 0f, 0f, 0f)
        val settled = toss != null && bounce.value >= toss.duration
        val centerX = maxWidth / 2 + pose.x.dp
        val groundY = maxHeight / 2 + pose.y.dp
        val ballSize = MatchBallVisualSize
        val ballY = groundY - pose.height.dp
        val elevation = (pose.height / 120f).coerceIn(0f, 1f)
        val shadowWidth = ballSize * (1f + elevation * .8f)

        PaperCard(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 46.dp),
            shape = FoldballShapes.Banner,
            tilt = FoldballTilt.OpeningToss,
            ruled = false
        ) {
            Column(
                Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("KICKOFF TOSS", style = FoldballType.Label, color = FoldballColor.Ink)
                if (winner != null) {
                    Text(
                        when {
                            !settled -> "The paper ball is in the air..."

                            else -> "TEAM ${winner?.let {
                                teamConfiguration.forTeam(it).name
                            }.orEmpty()} KICKS OFF!"
                        },
                        color = when {
                            !settled -> FoldballColor.Ink
                            winner == GameTeam.BLUE -> FoldballColor.InkBlue
                            else -> FoldballColor.TeamRed
                        },
                        style = FoldballType.Caption,
                        textAlign = TextAlign.Center
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
                    .padding(
                        start = maxWidth / 2 - frameSize / 2,
                        top =
                            maxHeight / 2 - frameSize / 2
                    )
                    .size(frameSize)
                    .alpha(((1f - crumple.value) / .18f).coerceIn(0f, 1f))
            ) {
                drawImage(
                    image = crumpleSheet,
                    srcOffset = IntOffset(srcLeft, srcTop),
                    srcSize = IntSize(srcRight - srcLeft, srcBottom - srcTop),
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.Medium
                )
            }
        }

        if (crumple.value > .82f) {
            Canvas(
                Modifier.align(Alignment.TopStart)
                    .offset(x = centerX - shadowWidth / 2, y = groundY + ballSize / 2 - 2.dp)
                    .size(shadowWidth, 5.dp + 3.dp * elevation)
                    .alpha(1f - elevation * .65f)
            ) {
                drawOval(
                    Brush.radialGradient(
                        listOf(FoldballColor.Shadow.copy(alpha = .5f), Color.Transparent)
                    ),
                    size = size
                )
            }
            PaperBall(
                modifier = Modifier.align(Alignment.TopStart)
                    .offset(x = centerX - ballSize / 2, y = ballY - ballSize / 2)
                    .size(ballSize)
                    .alpha(((crumple.value - .82f) / .18f).coerceIn(0f, 1f)),
                rotationX = pose.rotationX,
                rotationY = pose.rotationY
            )
        }

        if (winner == null && crumple.value >= 1f) {
            PaperTossButton(
                onClick = {
                    winner = if (Random.nextBoolean()) GameTeam.RED else GameTeam.BLUE
                    horizontalLanding = Random.nextFloat() * .24f - .12f
                    trajectory = TossTrajectory(
                        apexHeight = minOf(maxHeight.value * .19f, 150f),
                        travelX = maxWidth.value * horizontalLanding,
                        travelY = maxHeight.value * if (winner == GameTeam.BLUE) -.20f else .20f
                    )
                },
                modifier = Modifier.align(Alignment.Center).offset(y = 52.dp)
            )
        }
    }
}

@Composable
private fun PaperTossButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PaperButton("Start", onClick, modifier = modifier)
}

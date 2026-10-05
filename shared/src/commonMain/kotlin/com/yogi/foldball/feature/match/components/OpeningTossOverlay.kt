package com.yogi.foldball.feature.match.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.presentation.ball.PaperBall
import com.yogi.foldball.presentation.ball.MatchBallVisualSize
import com.yogi.foldball.presentation.theme.ClassroomTheme
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

        Column(
            Modifier.align(Alignment.TopCenter).padding(top = 46.dp)
                .background(ClassroomTheme.Paper, RoundedCornerShape(6.dp))
                .padding(horizontal = 18.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("UNDIAN KICKOFF", color = ClassroomTheme.Ink,
                fontSize = 15.sp, fontWeight = FontWeight.Black)
            if (winner != null) {
                Text(
                    when {
                        progress < 1f -> "Bola kertas sedang memantul..."
                        winner == GameTeam.BLUE -> "TIM BIRU MULAI!"
                        else -> "TIM MERAH MULAI!"
                    },
                    color = when {
                        progress < 1f -> ClassroomTheme.Ink
                        winner == GameTeam.BLUE -> ClassroomTheme.TeamBlue
                        else -> ClassroomTheme.TeamRed
                    },
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
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
fun OpeningTossStatusPanel(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().heightIn(min = 91.dp), contentAlignment = Alignment.CenterStart) {
        Canvas(Modifier.fillMaxSize()) {
            val paper = Path().apply {
                moveTo(7.dp.toPx(), 2.dp.toPx())
                lineTo(size.width * .48f, 0f)
                lineTo(size.width - 6.dp.toPx(), 3.dp.toPx())
                lineTo(size.width - 3.dp.toPx(), size.height - 4.dp.toPx())
                lineTo(4.dp.toPx(), size.height - 2.dp.toPx())
                close()
            }
            translate(top = 3.dp.toPx()) { drawPath(paper, ClassroomTheme.PaperShadow) }
            drawPath(paper, ClassroomTheme.Paper)
            drawPath(paper, ClassroomTheme.PaperEdge, style = Stroke(1.dp.toPx()))
        }
        Column(Modifier.padding(horizontal = 22.dp, vertical = 13.dp)) {
            Text("OPENING TOSS", color = ClassroomTheme.Ink,
                fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text("Bola berhenti di sisi tim yang mendapat kickoff pertama.",
                color = ClassroomTheme.Ink.copy(alpha = .7f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun PaperTossButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(140.dp, 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val paper = Path().apply {
                moveTo(5.dp.toPx(), 3.dp.toPx())
                lineTo(size.width * .42f, 1.dp.toPx())
                lineTo(size.width - 5.dp.toPx(), 4.dp.toPx())
                lineTo(size.width - 2.dp.toPx(), size.height - 6.dp.toPx())
                lineTo(size.width * .55f, size.height - 2.dp.toPx())
                lineTo(4.dp.toPx(), size.height - 4.dp.toPx())
                close()
            }
            translate(top = 3.dp.toPx()) { drawPath(paper, ClassroomTheme.PaperShadow) }
            drawPath(paper, ClassroomTheme.Paper)
            drawPath(paper, ClassroomTheme.PaperEdge, style = Stroke(1.dp.toPx()))
        }
        Text("Start", color = ClassroomTheme.Ink,
            fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

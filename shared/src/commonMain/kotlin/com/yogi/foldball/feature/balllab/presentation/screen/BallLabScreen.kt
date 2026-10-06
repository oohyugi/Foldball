package com.yogi.foldball.feature.balllab.presentation.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballScaffold
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.PaperButton
import com.yogi.foldball.core.ui.theme.PaperButtonStyle
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.core.ui.theme.PaperChip
import com.yogi.foldball.gamekit.presentation.ball.BallEffect
import com.yogi.foldball.gamekit.presentation.ball.PaperBall
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.confetti_shapes_4
import foldball.shared.generated.resources.paper_flake
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.compose.resources.imageResource

private val Ink = com.yogi.foldball.core.ui.theme.FoldballColor.Ink
private val Muted = com.yogi.foldball.core.ui.theme.FoldballColor.InkMuted

@Composable
fun BallLabScreen() {
    var selectedEffect by remember { mutableStateOf(BallEffect.ROLL) }
    var replayId by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var power by remember { mutableFloatStateOf(.72f) }

    LaunchedEffect(selectedEffect, replayId) {
        progress = 0f
        val durationNanos = selectedEffect.durationMillis * 1_000_000L
        // Keep the preview looping so short sprite effects remain inspectable
        // after a tab change or a slower emulator frame.
        val cycleNanos = durationNanos
        var startedAt = 0L
        while (true) {
            withFrameNanos { now ->
                if (startedAt == 0L) startedAt = now
                val cycleProgressNanos = (now - startedAt) % cycleNanos
                progress = (cycleProgressNanos.toFloat() / durationNanos).coerceIn(0f, 1f)
            }
        }
    }

    FoldballScaffold {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("FOLDBALL  /  BALL PRESENTATION", color = Muted, style = FoldballType.Caption)
                Text("Ball animation", color = Ink, style = FoldballType.LargeTitle)
                Text(
                    "Procedural paper ball · physics-based VFX",
                    color = Muted,
                    style = FoldballType.Body
                )
            }

            BallStage(selectedEffect = selectedEffect, progress = progress, power = power)

            PaperCard(Modifier.fillMaxWidth(), ruled = false, tilt = FoldballTilt.StatusPanel) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                selectedEffect.label.uppercase(),
                                color = Ink,
                                style = FoldballType.Label
                            )
                            Text(
                                selectedEffect.description,
                                color = Muted,
                                style = FoldballType.Caption
                            )
                        }
                        PaperButton("Replay", { replayId++ }, style = PaperButtonStyle.Secondary)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("INTENSITY", color = Muted, style = FoldballType.Caption)
                        Text("${(power * 100).toInt()}%", color = Ink, style = FoldballType.Caption)
                    }
                    Slider(value = power, onValueChange = { power = it })
                }
            }

            Text("CHOOSE ANIMATION", color = Muted, style = FoldballType.Caption)
            BallEffect.entries.chunked(2).forEach { rowEffects ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowEffects.forEach { effect ->
                        val selected = selectedEffect == effect
                        PaperChip(
                            text = effect.label,
                            onClick = {
                                selectedEffect = effect
                                replayId++
                            },
                            contentDescription = "Preview ${effect.label}",
                            modifier = Modifier.weight(1f),
                            selected = selected
                        )
                    }
                    if (rowEffects.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Text(
                "The paper surface and creases rotate with travel. The mask, highlights, inner shadow, and collider stay round; height and squash only affect the rendering.",
                color = Muted,
                style = FoldballType.Body
            )
        }
    }
}

@Composable
private fun BallStage(selectedEffect: BallEffect, progress: Float, power: Float) {
    PaperCard(
        Modifier.fillMaxWidth().height(290.dp),
        ruled = false,
        tilt = FoldballTilt.StatusPanel
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().clip(
                com.yogi.foldball.core.ui.theme.FoldballShapes.Card
            ).background(FoldballColor.FieldPaper)
        ) {
            val paperFlake = imageResource(Res.drawable.paper_flake)
            val confetti = imageResource(Res.drawable.confetti_shapes_4)
            val stageInset = 15f
            val ballSize = 94.dp
            val ballRadiusDp = ballSize.value * .5f
            val wallMin = (stageInset + ballRadiusDp) / maxWidth.value
            val wallMax = 1f - wallMin
            val traveling =
                selectedEffect in
                    setOf(BallEffect.ROLL, BallEffect.SPEED_TRAIL, BallEffect.SKID, BallEffect.GOAL)
            val ballXProgress = when (selectedEffect) {
                BallEffect.WALL_BOUNCE ->
                    if (progress <
                        .48f
                    ) {
                        .5f + (wallMax - .5f) * (progress / .48f)
                    } else {
                        wallMax -
                            (wallMax - .5f) * ((progress - .48f) / .52f)
                    }

                else -> if (traveling) .18f + .64f * progress else .5f
            }
            val ballYProgress = when (selectedEffect) {
                BallEffect.WALL_BOUNCE -> .44f
                BallEffect.SKID -> .52f + sin(progress * PI.toFloat()) * .045f
                else -> .52f
            }
            val ballX = maxWidth * ballXProgress
            val ballY = maxHeight * ballYProgress
            val totalTurns = 1.1f + power * 1.6f
            val turns = when (selectedEffect) {
                BallEffect.WALL_BOUNCE ->
                    if (progress <
                        .48f
                    ) {
                        totalTurns * progress / .48f
                    } else {
                        totalTurns *
                            (1f - .8f * (progress - .48f) / .52f)
                    }

                BallEffect.SKID -> progress * (1.7f + power * 1.4f)

                else -> progress * totalTurns
            }
            val rotationY = -turns * 2f * PI.toFloat()
            val rotationX = sin(turns * .35f) * .38f
            val maxLift = 34f + power * 58f
            val liftProgress = when (selectedEffect) {
                BallEffect.AIR_BOUNCE -> sin(PI.toFloat() * progress).coerceAtLeast(0f)

                BallEffect.WALL_BOUNCE, BallEffect.PLAYER_BOUNCE -> sin(
                    PI.toFloat() * progress
                ).coerceAtLeast(0f) *
                    .45f

                BallEffect.LANDING ->
                    if (progress <
                        .72f
                    ) {
                        sin(PI.toFloat() * progress / .72f).coerceAtLeast(0f) * .6f
                    } else {
                        0f
                    }

                else -> 0f
            }
            val ballHeight = maxLift * liftProgress
            val contactAt = when (selectedEffect) {
                BallEffect.WALL_BOUNCE, BallEffect.PLAYER_BOUNCE -> .48f
                BallEffect.LANDING -> .72f
                BallEffect.GOAL -> .68f
                else -> .12f
            }
            val eventAge = ((progress - contactAt + 1f) % 1f)
            val pulse = if (eventAge < .42f) (1f - eventAge / .42f).coerceIn(0f, 1f) else 0f
            val isHardImpact =
                selectedEffect == BallEffect.HIT_HARD || selectedEffect == BallEffect.PLAYER_BOUNCE
            val impactSquash = pulse * power * if (isHardImpact) 1f else .48f
            val scaleX = 1f + .14f * impactSquash + .045f * liftProgress
            val scaleY = 1f - .11f * impactSquash - .035f * liftProgress
            val wallImpact = if (selectedEffect ==
                BallEffect.WALL_BOUNCE
            ) {
                pulse * (.55f + power * .45f)
            } else {
                0f
            }
            val impactSideX = if (selectedEffect == BallEffect.WALL_BOUNCE) 1f else 0f

            Canvas(Modifier.fillMaxSize()) {
                val inset = 15.dp.toPx()
                val ballCenter = Offset(size.width * ballXProgress, size.height * ballYProgress)
                val radius = ballSize.toPx() * .5f
                val lift = (ballHeight / maxLift).coerceIn(0f, 1f)
                val shadowWidth = radius * 1.65f * (1f - .40f * lift)
                val shadowHeight = radius * .42f * (1f - .38f * lift)

                drawRoundRect(
                    FoldballColor.FieldHighlight,
                    Offset(inset, inset),
                    Size(size.width - inset * 2, size.height - inset * 2),
                    androidx.compose.ui.geometry.CornerRadius(22.dp.toPx())
                )
                drawRoundRect(
                    FoldballColor.FieldLine,
                    Offset(inset, inset),
                    Size(
                        size.width - inset * 2,
                        size.height - inset * 2
                    ),
                    androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()),
                    style = Stroke(1.2.dp.toPx())
                )
                drawLine(
                    FoldballColor.FieldLine,
                    Offset(size.width * .5f, inset),
                    Offset(size.width * .5f, size.height - inset),
                    1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
                )
                drawCircle(
                    FoldballColor.BallHighlight,
                    size.height * .22f,
                    Offset(
                        size.width * .5f,
                        size.height * .5f
                    ),
                    style = Stroke(1.dp.toPx())
                )

                drawOval(
                    color = FoldballColor.ShadowBrown.copy(
                        alpha = (.25f - .16f * lift).coerceAtLeast(.06f)
                    ),
                    topLeft = Offset(
                        ballCenter.x - shadowWidth * .5f,
                        ballCenter.y + radius * .52f - shadowHeight * .5f
                    ),
                    size = Size(shadowWidth, shadowHeight)
                )

                if (pulse > 0f &&
                    selectedEffect !in
                    setOf(BallEffect.ROLL, BallEffect.SPEED_TRAIL, BallEffect.SKID)
                ) {
                    val impactCenter = if (selectedEffect == BallEffect.WALL_BOUNCE) {
                        Offset(size.width - inset, ballCenter.y)
                    } else {
                        Offset(ballCenter.x, ballCenter.y + radius * .48f)
                    }
                    val ringRadius = radius * (1.04f + eventAge * 1.15f)
                    if (selectedEffect == BallEffect.WALL_BOUNCE) {
                        val arcSize = ringRadius * 2f
                        drawArc(
                            color = Color(0xFF8A715E).copy(alpha = pulse * .68f),
                            startAngle = 108f,
                            sweepAngle = 144f,
                            useCenter = false,
                            topLeft = Offset(
                                impactCenter.x - ringRadius,
                                impactCenter.y - ringRadius
                            ),
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = (1.6f + power * 1.8f).dp.toPx())
                        )
                        drawArc(
                            color = Color(0xFFE4B858).copy(alpha = pulse * .5f),
                            startAngle = 118f,
                            sweepAngle = 124f,
                            useCenter = false,
                            topLeft = Offset(
                                impactCenter.x - ringRadius * .72f,
                                impactCenter.y - ringRadius * .72f
                            ),
                            size = Size(ringRadius * 1.44f, ringRadius * 1.44f),
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(5.dp.toPx(), 4.dp.toPx())
                                )
                            )
                        )
                    } else {
                        drawCircle(
                            color = Color(0xFF8A715E).copy(alpha = pulse * .62f),
                            radius = ringRadius,
                            center = impactCenter,
                            style = Stroke(width = (1.6f + power * 1.8f).dp.toPx())
                        )
                        drawCircle(
                            color = Color(0xFFE4B858).copy(alpha = pulse * .45f),
                            radius = ringRadius * .72f,
                            center = impactCenter,
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(5.dp.toPx(), 4.dp.toPx())
                                )
                            )
                        )
                    }
                }

                if (selectedEffect == BallEffect.SPEED_TRAIL || selectedEffect == BallEffect.ROLL) {
                    val trailCount = if (selectedEffect == BallEffect.SPEED_TRAIL) 6 else 3
                    repeat(trailCount) { index ->
                        val t = (index + 1) / (trailCount + 1f)
                        val trailX =
                            ballCenter.x -
                                (if (ballXProgress > .5f) 1f else -1f) * radius *
                                (t * (1.25f + power))
                        val trailRadius = radius * (.43f - .26f * t)
                        drawCircle(
                            Color.White.copy(
                                alpha =
                                    (.14f - .10f * t) *
                                        (
                                            if (selectedEffect ==
                                                BallEffect.SPEED_TRAIL
                                            ) {
                                                1f
                                            } else {
                                                .42f
                                            }
                                            )
                            ),
                            trailRadius,
                            Offset(trailX, ballCenter.y)
                        )
                    }
                }
                if (selectedEffect == BallEffect.SKID) {
                    fun skidPoint(t: Float): Offset = Offset(
                        x = size.width * (.18f + .64f * t),
                        y = size.height * (.52f + sin(t * PI.toFloat()) * .045f)
                    )
                    val trailStartProgress = (progress - .3f).coerceAtLeast(0f)
                    val trailStart = skidPoint(trailStartProgress)
                    val trailEnd = skidPoint(progress)
                    val trailPath = Path().apply {
                        moveTo(trailStart.x, trailStart.y)
                        val samples = 16
                        for (sample in 1..samples) {
                            val t =
                                trailStartProgress +
                                    (progress - trailStartProgress) * sample / samples
                            val point = skidPoint(t)
                            lineTo(point.x, point.y)
                        }
                    }
                    if (progress > .015f) {
                        drawPath(
                            path = trailPath,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF826854).copy(
                                        alpha =
                                            .68f + power * .22f
                                    )
                                ),
                                start = trailStart,
                                end = trailEnd
                            ),
                            style = Stroke(
                                width = radius * (.22f + power * .12f),
                                cap = StrokeCap.Round
                            )
                        )
                        drawPath(
                            path = trailPath,
                            color = Color(0xFF725C4D).copy(alpha = .62f),
                            style = Stroke(
                                width = 1.15.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(
                                        radius * .24f,
                                        radius * .18f
                                    ),
                                    progress * radius
                                )
                            )
                        )
                        repeat(4) { index ->
                            val t = (progress - index * .065f).coerceIn(
                                trailStartProgress,
                                progress
                            )
                            val point = skidPoint(t)
                            val particleSize = radius * (.12f + (index % 2) * .045f)
                            rotate(progress * 360f + index * 59f, point) {
                                drawImage(
                                    image = paperFlake,
                                    dstOffset = IntOffset(
                                        (point.x - particleSize * .5f).toInt(),
                                        (
                                            point.y -
                                                particleSize * .5f
                                            ).toInt()
                                    ),
                                    dstSize = IntSize(
                                        particleSize.toInt().coerceAtLeast(1),
                                        (
                                            particleSize *
                                                .7f
                                            ).toInt().coerceAtLeast(1)
                                    ),
                                    alpha = (.76f - index * .13f).coerceIn(0f, 1f),
                                    filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium
                                )
                            }
                        }
                    }
                }
                if (selectedEffect == BallEffect.WALL_BOUNCE && pulse > 0f) {
                    val contactX = size.width - inset
                    repeat(4) { index ->
                        val spread = (index - 1.5f) * radius * .42f
                        drawLine(
                            color = Color(0xFF8A715E).copy(alpha = pulse * .9f),
                            start = Offset(contactX, ballCenter.y + spread),
                            end = Offset(
                                contactX - radius * (.45f + power * .55f),
                                ballCenter.y + spread * 1.6f
                            ),
                            strokeWidth = (1.2f + power * 1.5f).dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            PaperBall(
                rotationX = rotationX,
                rotationY = rotationY,
                scaleX = scaleX,
                scaleY = scaleY,
                impactSideX = impactSideX,
                impactStrength = wallImpact,
                modifier = Modifier.offset(
                    x = ballX - ballSize / 2,
                    y =
                        ballY - ballSize / 2 - ballHeight.dp
                ).size(ballSize)
            )

            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(
                    size.width * ballXProgress,
                    size.height * ballYProgress - ballHeight.dp.toPx()
                )
                val radius = ballSize.toPx() * .5f

                if ((
                        selectedEffect == BallEffect.LANDING ||
                            selectedEffect == BallEffect.WALL_BOUNCE ||
                            selectedEffect == BallEffect.PLAYER_BOUNCE
                        ) &&
                    pulse > 0f
                ) {
                    val particleAge = (1f - pulse)
                    repeat(7) { index ->
                        val angle = index * (2f * PI.toFloat() / 7f) + particleAge * 1.1f
                        val distance = radius * (1.05f + particleAge * 1.2f)
                        val particleCenter = Offset(
                            center.x + cos(angle) * distance,
                            center.y + sin(angle) * distance + particleAge * radius * .35f
                        )
                        val particleSize =
                            radius * (.075f + power * .035f) * (1f - particleAge * .35f)
                        rotate(particleAge * 260f + index * 31f, particleCenter) {
                            drawImage(
                                image = paperFlake,
                                dstOffset = IntOffset(
                                    (particleCenter.x - particleSize * .5f).toInt(),
                                    (
                                        particleCenter.y -
                                            particleSize * .5f
                                        ).toInt()
                                ),
                                dstSize = IntSize(
                                    particleSize.toInt().coerceAtLeast(1),
                                    (
                                        particleSize *
                                            .8f
                                        ).toInt().coerceAtLeast(1)
                                ),
                                alpha = pulse,
                                filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium
                            )
                        }
                    }
                }

                if (selectedEffect == BallEffect.GOAL && progress >= .68f && confetti.width >= 4) {
                    val goalProgress = ((progress - .68f) / .32f).coerceIn(0f, 1f)
                    val sourceWidth = confetti.width / 4
                    repeat(12) { index ->
                        val angle = index * (2f * PI.toFloat() / 12f) - PI.toFloat() * .5f
                        val distance = radius * (1.1f + goalProgress * 2.2f)
                        val particleCenter = Offset(
                            center.x + cos(angle) * distance,
                            center.y + sin(angle) * distance
                        )
                        val particleSize = (radius * (.16f + (index % 3) * .035f)).toInt().coerceAtLeast(
                            1
                        )
                        rotate(goalProgress * 390f + index * 47f, particleCenter) {
                            drawImage(
                                image = confetti,
                                srcOffset = IntOffset((index % 4) * sourceWidth, 0),
                                srcSize = IntSize(sourceWidth, confetti.height),
                                dstOffset = IntOffset(
                                    (particleCenter.x - particleSize * .5f).toInt(),
                                    (
                                        particleCenter.y -
                                            particleSize * .5f
                                        ).toInt()
                                ),
                                dstSize = IntSize(particleSize, particleSize),
                                alpha = (1f - goalProgress * .72f).coerceIn(0f, 1f),
                                filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium
                            )
                        }
                    }
                }
            }

            Text(
                "PAPER SURFACE · PROCEDURAL",
                Modifier.align(Alignment.BottomStart).padding(start = 26.dp, bottom = 23.dp),
                color = Muted,
                style = FoldballType.Caption
            )
            Text(
                "${(power * 100).toInt()}% POWER",
                Modifier.align(Alignment.BottomEnd).padding(end = 26.dp, bottom = 23.dp),
                color = Muted,
                style = FoldballType.Caption
            )
        }
    }
}

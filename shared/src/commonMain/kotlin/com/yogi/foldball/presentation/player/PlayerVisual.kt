package com.yogi.foldball.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI

/** Shared paper standee renderer. Gameplay position and collision stay outside this visual. */
enum class PlayerFocusState { INACTIVE, FOCUSED, KICKING }
enum class PlayerFacing { FRONT, BACK }

@Composable
fun PaperPlayer(
    modifier: Modifier = Modifier,
    facing: PlayerFacing = PlayerFacing.FRONT,
    focusState: PlayerFocusState = PlayerFocusState.INACTIVE,
    powerProgress: Float = 0f,
    powerPressProgress: Float = 0f,
    aimAngle: Float = 0f,
    kickProgress: Float = 0f,
    impactDirectionX: Float = 0f,
    impactDirectionY: Float = 0f,
    impactStrength: Float = 0f,
    jerseyColor: Color = PlayerRed,
    number: String = "10",
    name: String = "SATO",
) {
    val isFocused = focusState == PlayerFocusState.FOCUSED
    val isKicking = focusState == PlayerFocusState.KICKING
    val isActive = isFocused || isKicking
    val normalizedPower = if (isActive) powerProgress.coerceIn(0f, 1f) else 0f
    val focusTransition = rememberInfiniteTransition()
    val focusPulse by focusTransition.animateFloat(
        initialValue = .72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), repeatMode = RepeatMode.Reverse),
    )
    BoxWithConstraints(modifier) {
        val labelScale = (maxWidth.value / 148f).coerceIn(.4f, 1.2f)
        Canvas(Modifier.fillMaxSize()) {
            val sx = size.width / 512f
            val sy = size.height / 512f
            fun p(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(x * sx, y * sy)
            val castShadow = Path().apply {
                moveTo(p(119f, 400f).x, p(119f, 400f).y)
                lineTo(p(390f, 400f).x, p(390f, 400f).y)
                lineTo(p(502f, 456f).x, p(502f, 456f).y)
                lineTo(p(233f, 456f).x, p(233f, 456f).y)
                close()
            }
            drawPath(
                castShadow,
                Brush.linearGradient(
                    colors = listOf(Ink.copy(alpha = .30f), Ink.copy(alpha = .17f), Ink.copy(alpha = .02f)),
                    start = p(250f, 400f),
                    end = p(502f, 456f),
                ),
            )
            val contactShadow = Path().apply {
                moveTo(p(122f, 399f).x, p(122f, 399f).y)
                lineTo(p(390f, 399f).x, p(390f, 399f).y)
                lineTo(p(421f, 418f).x, p(421f, 418f).y)
                lineTo(p(148f, 418f).x, p(148f, 418f).y)
                close()
            }
            drawPath(contactShadow, Ink.copy(alpha = .18f))
        }
        if (isFocused) {
            Canvas(Modifier.fillMaxSize()) {
                drawOval(
                    color = Color(0xFFE94F43).copy(alpha = .12f * focusPulse),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * .12f, size.height * .72f),
                    size = androidx.compose.ui.geometry.Size(size.width * .76f, size.height * .17f),
                )
                drawOval(
                    color = Color(0xFFE94F43).copy(alpha = .34f * focusPulse),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * .12f, size.height * .72f),
                    size = androidx.compose.ui.geometry.Size(size.width * .76f, size.height * .17f),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Box(
            Modifier.fillMaxSize()
                .alpha(if (isActive) 1f - .38f * normalizedPower else .78f)
                .graphicsLayer {
                    val press = if (isActive) powerPressProgress.coerceIn(0f, 1f) else 0f
                    val impact = impactStrength.coerceIn(0f, 1f)
                    scaleX = 1f + .035f * normalizedPower
                    scaleY = (1f - .105f * normalizedPower - .025f * press - .075f * impact) *
                        (if (isFocused) .997f + .006f * focusPulse else 1f)
                    val aimTilt = (aimAngle * (180f / PI.toFloat()) * .24f).coerceIn(-8f, 8f)
                    val impactLean = impactDirectionX.coerceIn(-1f, 1f) * 8f +
                        impactDirectionY.coerceIn(-1f, 1f) * 2.5f
                    rotationZ = aimTilt - aimTilt * .16f * kickProgress.coerceIn(0f, 1.1f) + impactLean * impact
                    val bodyLean = aimAngle * (180f / PI.toFloat()) * .025f
                    rotationX = bodyLean
                    rotationY = bodyLean * .65f
                    transformOrigin = TransformOrigin(.5f, .797f)
                },
        ) {
        Canvas(Modifier.fillMaxSize()) {
            val sx = size.width / 512f
            val sy = size.height / 512f
            fun p(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(x * sx, y * sy)
            val back = Path().apply {
                moveTo(p(170f, 92f).x, p(170f, 92f).y)
                lineTo(p(352f, 92f).x, p(352f, 92f).y)
                lineTo(p(420f, 392f).x, p(420f, 392f).y)
                lineTo(p(380f, 405f).x, p(380f, 405f).y)
                lineTo(p(318f, 118f).x, p(318f, 118f).y)
                lineTo(p(194f, 118f).x, p(194f, 118f).y)
                lineTo(p(132f, 405f).x, p(132f, 405f).y)
                lineTo(p(92f, 392f).x, p(92f, 392f).y)
                close()
            }
            val front = Path().apply {
                moveTo(p(160f, 90f).x, p(160f, 90f).y)
                lineTo(p(352f, 90f).x, p(352f, 90f).y)
                lineTo(p(390f, 408f).x, p(390f, 408f).y)
                lineTo(p(122f, 408f).x, p(122f, 408f).y)
                close()
            }
            val topFold = Path().apply {
                moveTo(p(190f, 61f).x, p(190f, 61f).y)
                lineTo(p(322f, 61f).x, p(322f, 61f).y)
                lineTo(p(352f, 90f).x, p(352f, 90f).y)
                lineTo(p(160f, 90f).x, p(160f, 90f).y)
                close()
            }
            drawPath(topFold, if (facing == PlayerFacing.FRONT) Color(0xFFE9E5DA) else Color(0xFFA92F2B))
            drawPath(topFold, Ink, style = Stroke(width = 7f * sx))
            if (facing == PlayerFacing.FRONT) {
                drawPath(back, Color(0xFFE1E0DC))
                drawPath(back, Ink, style = Stroke(width = 8f * sx))
            }
            drawPath(front, Color(0xFFFFFEFA))
            clipPath(front) {
                drawRect(jerseyColor)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = .15f), Color.Transparent, Ink.copy(alpha = .19f)),
                        startY = p(0f, 90f).y,
                        endY = p(0f, 408f).y,
                    ),
                )
                if (facing == PlayerFacing.FRONT) {
                    for (stripe in 0..4) {
                        val x = 143f + stripe * 57f
                        drawLine(Color.White.copy(alpha = .76f), p(x, 92f), p(x + 8f, 408f), strokeWidth = 7f * sx)
                    }
                    drawLine(Color.White.copy(alpha = .45f), p(178f, 116f), p(337f, 116f), strokeWidth = 4f * sx)
                } else {
                    drawLine(Ink.copy(alpha = .25f), p(161f, 94f), p(123f, 406f), strokeWidth = 13f * sx)
                    drawLine(Ink.copy(alpha = .25f), p(351f, 94f), p(389f, 406f), strokeWidth = 13f * sx)
                    drawLine(Color.White.copy(alpha = .28f), p(181f, 111f), p(141f, 399f), strokeWidth = 5f * sx)
                    drawLine(Color.White.copy(alpha = .28f), p(331f, 111f), p(371f, 399f), strokeWidth = 5f * sx)
                    drawLine(Color.White.copy(alpha = .55f), p(256f, 112f), p(256f, 398f), strokeWidth = 4f * sx)
                    drawLine(Color.White.copy(alpha = .75f), p(190f, 155f), p(322f, 155f), strokeWidth = 5f * sx)
                }
            }
            drawPath(front, Ink, style = Stroke(width = 8f * sx))
            drawLine(Ink.copy(alpha = .42f), p(129f, 408f), p(383f, 408f), strokeWidth = 10f * sx, cap = StrokeCap.Round)
            drawLine(Color(0xFF77736F), p(160f, 90f), p(352f, 90f), strokeWidth = 4f * sx, cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = .48f), p(177f, 112f), p(335f, 112f), strokeWidth = 5f * sx, cap = StrokeCap.Round)
        }
        Text(number, Modifier.align(Alignment.TopCenter).offset(y = 65.dp * labelScale), color = Color.White,
            fontSize = 25.sp * labelScale, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 25.sp * labelScale)
        Text(name, Modifier.align(Alignment.TopCenter).offset(y = (if (facing == PlayerFacing.BACK) 38.dp else 91.dp) * labelScale), color = Color.White,
            fontSize = 9.sp * labelScale, fontWeight = FontWeight.ExtraBold, letterSpacing = .8.sp * labelScale, textAlign = TextAlign.Center)
        }
    }
}

private val Ink = Color(0xFF252831)
private val PlayerRed = Color(0xFFD94038)

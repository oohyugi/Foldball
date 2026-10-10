package com.yogi.foldball.gamekit.presentation.player

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.domain.team.JerseyPattern
import com.yogi.foldball.gamekit.presentation.team.ClubKit
import kotlin.math.PI

/** Shared paper standee renderer. Gameplay position and collision stay outside this visual. */
enum class PlayerFocusState { INACTIVE, FORMATION, FOCUSED, KICKING }
enum class PlayerFacing { FRONT, BACK }

@Composable
fun PaperPlayer(
    modifier: Modifier = Modifier,
    facing: PlayerFacing = PlayerFacing.FRONT,
    focusState: PlayerFocusState = PlayerFocusState.INACTIVE,
    placementValid: Boolean? = null,
    showTurnMarker: Boolean = false,
    powerProgress: Float = 0f,
    dented: Boolean = false,
    dentProgress: Float = if (dented) 1f else 0f,
    dentDirectionX: Float = 0f,
    dentDirectionY: Float = 0f,
    powerPressProgress: Float = 0f,
    aimAngle: Float = 0f,
    kickProgress: Float = 0f,
    impactDirectionX: Float = 0f,
    impactDirectionY: Float = 0f,
    impactStrength: Float = 0f,
    jerseyColor: Color = PlayerRed,
    jerseyPattern: JerseyPattern = JerseyPattern.PLAIN,
    jerseyColorCount: Int = if (jerseyPattern == JerseyPattern.TWO_TONE) 2 else 1,
    jerseyPalette: List<Color> = listOf(jerseyColor, ClubKit.accent(jerseyColor), FoldballColor.Paper, FoldballColor.Tape),
    number: String = "10",
    name: String = "SATO"
) {
    val tones = jerseyColorCount.coerceIn(1, 4)
    val secondColor = jerseyPalette.getOrElse(1) { ClubKit.accent(jerseyColor) }
    val thirdColor = jerseyPalette.getOrElse(2) { FoldballColor.Paper }
    val fourthColor = jerseyPalette.getOrElse(3) { FoldballColor.Tape }
    val dentAmount = dentProgress.coerceIn(0f, 1f)
    val bendSide = dentDirectionX.coerceIn(-1f, 1f)
    val bendDepth = dentDirectionY.coerceIn(-1f, 1f)
    val visiblyDented = dentAmount > 0f
    val isFocused = focusState == PlayerFocusState.FOCUSED
    val isFormation = focusState == PlayerFocusState.FORMATION
    val isKicking = focusState == PlayerFocusState.KICKING
    val isActive = isFocused || isKicking
    val normalizedPower = if (isActive) powerProgress.coerceIn(0f, 1f) else 0f
    val focusPulse = if (isFocused && placementValid == null) {
        val focusTransition = rememberInfiniteTransition()
        val pulse by focusTransition.animateFloat(
            initialValue = .72f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), repeatMode = RepeatMode.Reverse)
        )
        pulse
    } else 1f
    BoxWithConstraints(modifier) {
        val labelScale = (maxWidth.value / 148f).coerceIn(.4f, 1.2f)
        Canvas(Modifier.fillMaxSize()) {
            placementValid?.let { valid ->
                // Inset visual feedback around the body; keep the full touch area.
                val background = if (valid) FoldballColor.Grass else FoldballColor.TeamRed
                val highlightOrigin = androidx.compose.ui.geometry.Offset(size.width * .15f, size.height * .10f)
                val highlightSize = androidx.compose.ui.geometry.Size(size.width * .70f, size.height * .72f)
                drawRect(background.copy(alpha = .52f), highlightOrigin, highlightSize)
                drawRect(FoldballColor.Paper.copy(alpha = .9f), highlightOrigin, highlightSize, style = Stroke(4.dp.toPx()))
                drawRect(background, highlightOrigin, highlightSize, style = Stroke(2.dp.toPx()))
            }
            if (showTurnMarker) {
                val marker = Path().apply {
                    moveTo(size.width * .42f, size.height * .015f)
                    lineTo(size.width * .58f, size.height * .015f)
                    lineTo(size.width * .50f, size.height * .105f)
                    close()
                }
                drawPath(marker, FoldballColor.Paper, style = Stroke(4.dp.toPx()))
                drawPath(marker, jerseyColor)
                drawPath(marker, FoldballColor.Ink, style = Stroke(1.dp.toPx()))
            }
            val sx = size.width / 512f
            val sy = size.height / 512f
            fun p(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(x * sx, y * sy)

            // Flatten and shear the full standee silhouette onto the grass, anchored
            // at the feet like the match-screen reference.
            fun shadowPoint(x: Float, y: Float) = p(
                x + (408f - y) * .35f,
                408f + (408f - y) * .30f
            )
            val shadow = Path().apply {
                run {
                    moveTo(shadowPoint(92f, 392f).x, shadowPoint(92f, 392f).y)
                    lineTo(shadowPoint(170f, 92f).x, shadowPoint(170f, 92f).y)
                    lineTo(shadowPoint(352f, 92f).x, shadowPoint(352f, 92f).y)
                    lineTo(shadowPoint(420f, 392f).x, shadowPoint(420f, 392f).y)
                    lineTo(shadowPoint(390f, 408f).x, shadowPoint(390f, 408f).y)
                    lineTo(shadowPoint(122f, 408f).x, shadowPoint(122f, 408f).y)
                    close()
                    moveTo(shadowPoint(160f, 90f).x, shadowPoint(160f, 90f).y)
                    lineTo(shadowPoint(352f, 90f).x, shadowPoint(352f, 90f).y)
                    lineTo(shadowPoint(390f, 408f).x, shadowPoint(390f, 408f).y)
                    lineTo(shadowPoint(122f, 408f).x, shadowPoint(122f, 408f).y)
                }
                close()
            }
            drawPath(shadow, FoldballColor.ShadowGrass.copy(alpha = .34f))
            drawPath(
                shadow,
                FoldballColor.ShadowGrass.copy(alpha = .12f),
                style = Stroke(width = 4.dp.toPx())
            )
        }
        if ((isFocused || isFormation) && placementValid == null) {
            Canvas(Modifier.fillMaxSize()) {
                val focusPad = Path().apply {
                    // FRONT faces down-screen: extend behind the foot pivot, not behind the head.
                    fun focusY(fraction: Float) = size.height * if (facing == PlayerFacing.FRONT) {
                        .85f - (fraction - .72f) / .21f * .23f
                    } else {
                        fraction
                    }
                    moveTo(size.width * .20f, focusY(.72f))
                    lineTo(size.width * .80f, focusY(.72f))
                    lineTo(size.width * .72f, focusY(.93f))
                    lineTo(size.width * .28f, focusY(.93f))
                    close()
                }
                if (isFocused) {
                    drawPath(
                        focusPad,
                        color = FoldballColor.ShadowBrown.copy(alpha = .36f),
                        style = Stroke(width = 5.dp.toPx())
                    )
                    drawPath(
                        focusPad,
                        color = FoldballColor.Tape.copy(alpha = .72f + .24f * focusPulse)
                    )
                    drawPath(
                        focusPad,
                        color = FoldballColor.Ink.copy(alpha = .82f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawPath(
                        focusPad,
                        color = FoldballColor.Tape.copy(alpha = focusPulse),
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(6.dp.toPx(), 4.dp.toPx())
                            )
                        )
                    )
                } else {
                    drawPath(
                        focusPad,
                        color = FoldballColor.FieldHighlight.copy(alpha = .24f)
                    )
                    drawPath(
                        focusPad,
                        color = FoldballColor.FieldHighlight.copy(alpha = .92f),
                        style = Stroke(
                            width = 1.7.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                            )
                        )
                    )
                }
            }
        }
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    val press = if (isActive) powerPressProgress.coerceIn(0f, 1f) else 0f
                    val impact = impactStrength.coerceIn(0f, 1f)
                    scaleX = 1f + .035f * normalizedPower + .10f * dentAmount
                    scaleY =
                        (
                            1f - .105f * normalizedPower - .025f * press - .075f * impact -
                                .27f * dentAmount
                            ) *
                        (if (isFocused) .997f + .006f * focusPulse else 1f)
                    val aimTilt = (aimAngle * (180f / PI.toFloat()) * .24f).coerceIn(-8f, 8f)
                    val impactLean = impactDirectionX.coerceIn(-1f, 1f) * 8f +
                        impactDirectionY.coerceIn(-1f, 1f) * 2.5f
                    rotationZ =
                        (bendSide * 10f * dentAmount) + aimTilt -
                        aimTilt * .16f * kickProgress.coerceIn(0f, 1.1f) +
                        impactLean * impact
                    val bodyLean = aimAngle * (180f / PI.toFloat()) * .025f
                    rotationX = bodyLean
                    rotationY = bodyLean * .65f
                    transformOrigin = TransformOrigin(.5f, .797f)
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val sx = size.width / 512f
                val sy = size.height / 512f
                fun p(x: Float, y: Float): androidx.compose.ui.geometry.Offset {
                    // Buckle the upper sheet sideways around a crease; the base stays planted.
                    val bend = ((290f - y) / 200f).coerceIn(0f, 1f) * dentAmount
                    val sideCrease = (x - 256f) / 256f * bendSide * 24f * bend
                    return androidx.compose.ui.geometry.Offset(
                        (x + bend * 65f * bendSide) * sx,
                        (y + bend * (35f + bendDepth * 22f) + sideCrease) * sy
                    )
                }
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
                    lineTo(p(352f, 96f).x, p(352f, 96f).y)
                    lineTo(p(160f, 96f).x, p(160f, 96f).y)
                    close()
                }
                val backPanel = Path().apply {
                    moveTo(p(160f, 90f).x, p(160f, 90f).y)
                    lineTo(p(352f, 90f).x, p(352f, 90f).y)
                    lineTo(p(420f, 392f).x, p(420f, 392f).y)
                    lineTo(p(390f, 408f).x, p(390f, 408f).y)
                    lineTo(p(122f, 408f).x, p(122f, 408f).y)
                    lineTo(p(92f, 392f).x, p(92f, 392f).y)
                    close()
                }
                val frontShape = if (facing == PlayerFacing.FRONT) front else backPanel
                drawPath(
                    topFold,
                    if (facing ==
                        PlayerFacing.FRONT
                    ) {
                        FoldballColor.Paper
                    } else {
                        androidx.compose.ui.graphics.lerp(
                            FoldballColor.Paper,
                            FoldballColor.PaperEdge,
                            .45f
                        )
                    }
                )
                drawPath(topFold, Ink.copy(alpha = .85f), style = Stroke(width = 3.5f * sx))
                run {
                    drawPath(back, Color(0xFFE1E0DC))
                    drawPath(back, Ink.copy(alpha = .8f), style = Stroke(width = 4f * sx))
                }
                // Thin paper side visible along the lower/right cut edge.
                translate(left = 2.5f * sx, top = 3f * sy) {
                    drawPath(frontShape, FoldballColor.PaperEdge)
                    drawPath(
                        frontShape,
                        FoldballColor.WoodDark.copy(alpha = .45f),
                        style = Stroke(2f * sx)
                    )
                }
                drawPath(frontShape, Color(0xFFFFFEFA))
                clipPath(frontShape) {
                    drawRect(jerseyColor.copy(alpha = 1f))
                    if (tones >= 2 && (jerseyPattern == JerseyPattern.PLAIN || jerseyPattern == JerseyPattern.TWO_TONE)) {
                        drawRect(
                            secondColor,
                            topLeft = p(256f, 0f),
                            size = androidx.compose.ui.geometry.Size(size.width / 2, size.height)
                        )
                    }
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = .15f),
                                Color.Transparent,
                                Ink.copy(alpha = .19f)
                            ),
                            startY = p(0f, 90f).y,
                            endY = p(0f, 408f).y
                        )
                    )
                    val patternInk = if (tones >= 2) secondColor else Color.White.copy(alpha = .52f)
                    when (jerseyPattern) {
                        JerseyPattern.PLAIN, JerseyPattern.TWO_TONE -> Unit

                        JerseyPattern.STRIPES -> repeat(4) { stripe ->
                            val x = 162f + stripe * 62f
                            drawLine(patternInk, p(x, 90f), p(x, 410f), strokeWidth = 20f * sx)
                        }

                        JerseyPattern.HOOPS -> repeat(4) { hoop ->
                            val y = 135f + hoop * 72f
                            drawLine(patternInk, p(95f, y), p(420f, y), strokeWidth = 20f * sy)
                        }

                        JerseyPattern.CHEVRON -> {
                            val chevron = Path().apply {
                                moveTo(p(105f, 158f).x, p(105f, 158f).y)
                                lineTo(p(256f, 235f).x, p(256f, 235f).y)
                                lineTo(p(405f, 158f).x, p(405f, 158f).y)
                            }
                            drawPath(chevron, patternInk, style = Stroke(24f * sx))
                        }
                    }
                    if (tones >= 3) {
                        drawLine(thirdColor, p(110f, 370f), p(410f, 370f), 18f * sy)
                        drawLine(thirdColor, p(145f, 98f), p(367f, 98f), 14f * sy)
                    }
                    if (tones >= 4) {
                        drawLine(fourthColor, p(122f, 125f), p(122f, 390f), 16f * sx)
                        drawLine(fourthColor, p(390f, 125f), p(390f, 390f), 16f * sx)
                    }
                    if (facing == PlayerFacing.FRONT) {
                        drawLine(
                            Color.White.copy(alpha = .45f),
                            p(178f, 116f),
                            p(337f, 116f),
                            strokeWidth =
                                4f * sx
                        )
                    } else {
                        drawLine(
                            Ink.copy(alpha = .25f),
                            p(161f, 94f),
                            p(123f, 406f),
                            strokeWidth =
                                13f * sx
                        )
                        drawLine(
                            Ink.copy(alpha = .25f),
                            p(351f, 94f),
                            p(389f, 406f),
                            strokeWidth =
                                13f * sx
                        )
                        drawLine(
                            Color.White.copy(alpha = .28f),
                            p(181f, 111f),
                            p(141f, 399f),
                            strokeWidth =
                                5f * sx
                        )
                        drawLine(
                            Color.White.copy(alpha = .28f),
                            p(331f, 111f),
                            p(371f, 399f),
                            strokeWidth =
                                5f * sx
                        )
                        drawLine(
                            Color.White.copy(alpha = .75f),
                            p(190f, 155f),
                            p(322f, 155f),
                            strokeWidth =
                                5f * sx
                        )
                    }
                }
                drawPath(frontShape, FoldballColor.PaperEdge, style = Stroke(width = 5f * sx))
                drawPath(frontShape, Ink.copy(alpha = .85f), style = Stroke(width = 2.8f * sx))
                val edgeLeft = 122f
                val edgeRight = 390f
                // Small cut-edge irregularities stay attached to the same warped sheet.
                drawLine(
                    FoldballColor.Paper,
                    p(edgeLeft + 2f, 392f),
                    p(edgeLeft + 6f, 401f),
                    2.2f * sx
                )
                drawLine(
                    FoldballColor.Paper,
                    p(edgeRight - 2f, 395f),
                    p(edgeRight - 6f, 404f),
                    2.2f * sx
                )
                if (visiblyDented) {
                    clipPath(frontShape) {
                        val leftY = 270f + bendSide * 32f
                        val rightY = 270f - bendSide * 32f
                        val upperFacet = Path().apply {
                            moveTo(p(90f, leftY - 24f).x, p(90f, leftY - 24f).y)
                            lineTo(p(425f, rightY - 24f).x, p(425f, rightY - 24f).y)
                            lineTo(p(425f, rightY).x, p(425f, rightY).y)
                            lineTo(p(90f, leftY).x, p(90f, leftY).y)
                            close()
                        }
                        val lowerFacet = Path().apply {
                            moveTo(p(90f, leftY).x, p(90f, leftY).y)
                            lineTo(p(425f, rightY).x, p(425f, rightY).y)
                            lineTo(p(425f, rightY + 40f).x, p(425f, rightY + 40f).y)
                            lineTo(p(90f, leftY + 40f).x, p(90f, leftY + 40f).y)
                            close()
                        }
                        drawPath(
                            upperFacet,
                            Brush.linearGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = .17f * dentAmount)
                                ),
                                p(256f, 245f),
                                p(256f, 270f)
                            )
                        )
                        drawPath(
                            lowerFacet,
                            Brush.linearGradient(
                                listOf(Ink.copy(alpha = .20f * dentAmount), Color.Transparent),
                                p(256f, 270f),
                                p(256f, 310f)
                            )
                        )
                        drawLine(
                            Ink.copy(alpha = .22f * dentAmount),
                            p(90f, leftY),
                            p(425f, rightY),
                            1.8f * sx
                        )
                    }
                }
                if (facing == PlayerFacing.FRONT) {
                    val vCollar = Path().apply {
                        moveTo(p(204f, 90f).x, p(204f, 90f).y)
                        lineTo(p(256f, 150f).x, p(256f, 150f).y)
                        lineTo(p(308f, 90f).x, p(308f, 90f).y)
                        close()
                    }
                    drawPath(vCollar, Color(0xFFFFFEFA))
                    drawPath(vCollar, Ink.copy(alpha = .65f), style = Stroke(width = 3f * sx))
                    drawLine(
                        Ink.copy(alpha = .42f),
                        p(129f, 408f),
                        p(383f, 408f),
                        strokeWidth =
                            10f * sx,
                        cap = StrokeCap.Round
                    )
                } else {
                    val nape = Path().apply {
                        moveTo(p(210f, 110f).x, p(210f, 110f).y)
                        quadraticTo(
                            p(256f, 158f).x,
                            p(256f, 158f).y,
                            p(302f, 110f).x,
                            p(302f, 110f).y
                        )
                        close()
                    }
                    drawPath(nape, Ink.copy(alpha = .38f * dentAmount))
                }
                val foldEdgeY = 90f
                val foldEdgeLeft = 160f
                val foldEdgeRight = 352f
                drawLine(
                    Color(0xFF77736F),
                    p(foldEdgeLeft, foldEdgeY),
                    p(foldEdgeRight, foldEdgeY),
                    strokeWidth =
                        4f * sx,
                    cap = StrokeCap.Round
                )
                drawLine(
                    Color.White.copy(alpha = .48f),
                    p(177f, 112f),
                    p(335f, 112f),
                    strokeWidth =
                        5f * sx,
                    cap = StrokeCap.Round
                )
            }
            fun labelDeformation(yDp: Float): Modifier {
                val canvasY = yDp / 148f * 512f
                val bend = ((290f - canvasY) / 200f).coerceIn(0f, 1f) * dentAmount
                return Modifier.offset(
                    x = (bend * 65f / 512f * 148f).dp * labelScale * bendSide,
                    y = (bend * (35f + bendDepth * 22f) / 512f * 148f).dp * labelScale
                )
                    .graphicsLayer {
                        rotationZ = bendSide * bend * 9f
                        scaleY = 1f - bend * .12f
                    }
            }
            StandeePrint(
                number,
                Modifier.align(
                    Alignment.TopCenter
                ).offset(y = 65.dp * labelScale).then(labelDeformation(65f)),
                outlined = tones > 1,
                fontSize = 25.sp * labelScale,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight =
                    25.sp * labelScale
            )
            StandeePrint(
                name,
                Modifier.align(
                    Alignment.TopCenter
                ).offset(y = 38.dp * labelScale).then(labelDeformation(38f)),
                outlined = tones > 1,
                fontSize = 9.sp * labelScale,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing =
                    .8.sp * labelScale,
                textAlign = TextAlign.Center
            )
        }
        // Only movable formation players have a pointer; always above the body.
        if (isFormation) {
            Canvas(Modifier.fillMaxSize()) {
                val x = size.width * .5f
                val tip = size.height * .105f
                val half = size.width * .09f
                val marker = Path().apply {
                    moveTo(x - half, tip - size.height * .055f)
                    lineTo(x + half, tip - size.height * .055f)
                    lineTo(x, tip)
                    close()
                }
                drawPath(marker, FoldballColor.Paper)
                drawPath(marker, FoldballColor.Ink, style = Stroke(1.5.dp.toPx()))
            }
        }
    }
}

private val Ink = FoldballColor.Ink
private val PlayerRed = FoldballColor.TeamRed

/** White jersey printing with a fine ink outline, never an opaque patch. */
@Composable
private fun StandeePrint(
    text: String,
    modifier: Modifier,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    textAlign: TextAlign,
    outlined: Boolean,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified
) {
    val style = TextStyle(
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        textAlign = textAlign
    )
    if (outlined) {
        val outlineWidth = with(LocalDensity.current) { (fontSize.value * .08f).dp.toPx() }
        Text(text, modifier, color = FoldballColor.Ink, style = style.copy(drawStyle = Stroke(outlineWidth)), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
    Text(text, modifier, color = Color.White, style = style, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
}

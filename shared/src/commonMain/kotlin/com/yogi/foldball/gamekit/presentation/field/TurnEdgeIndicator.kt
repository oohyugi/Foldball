package com.yogi.foldball.gamekit.presentation.field

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.domain.game.engine.GameTeam

/** Fixed edge cues, independent of standee positions and without an input surface. */
@Composable
fun TurnEdgeIndicator(team: GameTeam, redColor: Color, blueColor: Color,
    description: String, modifier: Modifier = Modifier) {
    val emphasis = remember { Animatable(1f) }
    LaunchedEffect(team) {
        emphasis.snapTo(1.12f)
        emphasis.animateTo(1f, tween(260))
    }
    Canvas(modifier.semantics { contentDescription = description }) {
        val left = 12.dp.toPx()
        val width = 88.dp.toPx()
        val height = 24.dp.toPx()
        for (side in GameTeam.entries) {
            val active = side == team
            val top = if (side == GameTeam.BLUE) 4.dp.toPx() else size.height - height - 4.dp.toPx()
            val color = if (side == GameTeam.RED) redColor else blueColor
            val origin = Offset(left, top)
            val padSize = Size(width, height)
            scale(if (active) emphasis.value else 1f, pivot = origin + Offset(width / 2, height / 2)) {
                drawRect(FoldballColor.Paper.copy(alpha = if (active) .95f else .15f), origin, padSize)
                drawRect(color.copy(alpha = if (active) .95f else .18f), origin + Offset(2.dp.toPx(), 2.dp.toPx()),
                    Size(width - 4.dp.toPx(), height - 4.dp.toPx()))
                if (active) {
                    drawRect(FoldballColor.Ink, origin, padSize, style = Stroke(1.dp.toPx()))
                    // Raised index finger and palm, outlined for light and dark club kits.
                    translate(left + 6.dp.toPx(), top + 2.dp.toPx()) {
                        val unit = 1.dp.toPx()
                        val hand = Path().apply {
                            moveTo(6 * unit, 10 * unit); lineTo(6 * unit, 3 * unit)
                            cubicTo(6 * unit, 0f, 10 * unit, 0f, 10 * unit, 3 * unit)
                            lineTo(10 * unit, 8 * unit); lineTo(17 * unit, 10 * unit)
                            lineTo(17 * unit, 15 * unit); lineTo(14 * unit, 19 * unit)
                            lineTo(7 * unit, 19 * unit); lineTo(2 * unit, 12 * unit)
                            lineTo(3 * unit, 10 * unit); lineTo(6 * unit, 12 * unit); close()
                        }
                        drawPath(hand, FoldballColor.Paper)
                        drawPath(hand, FoldballColor.Ink, style = Stroke(1.dp.toPx()))
                    }
                    drawLine(FoldballColor.Paper, origin + Offset(34.dp.toPx(), height / 2),
                        origin + Offset(width - 8.dp.toPx(), height / 2), 4.dp.toPx(), StrokeCap.Round)
                }
            }
        }
    }
}

package com.yogi.foldball.gamekit.presentation.field

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.FoldballColor

/** Fixed pool-style aim track with integrated power and a fading, unmarked end. */
fun DrawScope.drawPaperAimGuide(start: Offset, end: Offset, power: Float = 0f) {
    val requested = end - start
    val inset = 6.dp.toPx()
    var fraction = 1f
    if (requested.x > 0f) fraction = minOf(fraction, (size.width - inset - start.x) / requested.x)
    if (requested.x < 0f) fraction = minOf(fraction, (inset - start.x) / requested.x)
    if (requested.y > 0f) fraction = minOf(fraction, (size.height - inset - start.y) / requested.y)
    if (requested.y < 0f) fraction = minOf(fraction, (inset - start.y) / requested.y)
    val delta = requested * fraction.coerceIn(0f, 1f)
    val target = start + delta
    val length = delta.getDistance()
    if (length < 12.dp.toPx()) return
    drawLine(
        Brush.linearGradient(
            0f to FoldballColor.Paper.copy(alpha = .16f),
            .78f to FoldballColor.Paper.copy(alpha = .16f),
            1f to Color.Transparent,
            start = start, end = target
        ), start, target, 8.dp.toPx(), cap = StrokeCap.Round
    )
    val charge = power.coerceIn(0f, 1f)
    if (charge > .01f) {
        drawLine(
            Brush.linearGradient(
                0f to FoldballColor.Grass.copy(alpha = .85f),
                .5f to FoldballColor.Tape.copy(alpha = .85f),
                .85f to FoldballColor.TeamRed.copy(alpha = .85f),
                1f to FoldballColor.TeamRed.copy(alpha = 0f),
                start = start, end = target
            ), start, start + delta * charge, 5.dp.toPx(), cap = StrokeCap.Round
        )
    }
    val dots = (length / 9.dp.toPx()).toInt().coerceAtLeast(3)
    for (i in 1 until dots) {
        val t = i.toFloat() / dots
        // Dots in the unfilled track retain the aim direction; the last ones fade out.
        if (t > charge) {
            val fade = ((1f - t) / .22f).coerceIn(0f, 1f)
            drawCircle(FoldballColor.Paper.copy(alpha = .48f * fade),
                1.2.dp.toPx(), start + delta * t)
        }
    }
}

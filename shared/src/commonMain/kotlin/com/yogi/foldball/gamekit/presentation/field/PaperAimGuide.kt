package com.yogi.foldball.gamekit.presentation.field

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.FoldballColor
import kotlin.math.sqrt

/** A paper-and-ink direction marker. Drawing it never changes the shot vector. */
fun DrawScope.drawPaperAimGuide(start: Offset, end: Offset) {
    val delta = end - start
    val length = sqrt(delta.x * delta.x + delta.y * delta.y)
    if (length <= 0f) return
    val forward = delta / length
    val side = Offset(-forward.y, forward.x)
    val tipLength = 14.dp.toPx()
    val tipHalfWidth = 7.dp.toPx()
    val arrowBase = end - forward * tipLength
    val dashedEnd = arrowBase - forward * 2.dp.toPx()
    val dash = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 6.dp.toPx()))

    drawLine(
        color = FoldballColor.Ink,
        start = start,
        end = dashedEnd,
        strokeWidth = 5.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = dash
    )
    drawLine(
        color = FoldballColor.Tape,
        start = start,
        end = dashedEnd,
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = dash
    )
    val stemStart = arrowBase - forward * 8.dp.toPx()
    drawLine(FoldballColor.Ink, stemStart, arrowBase, 5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(FoldballColor.Tape, stemStart, arrowBase, 3.dp.toPx(), cap = StrokeCap.Round)

    val head = Path().apply {
        moveTo(end.x, end.y)
        lineTo(arrowBase.x + side.x * tipHalfWidth, arrowBase.y + side.y * tipHalfWidth)
        lineTo(arrowBase.x - side.x * tipHalfWidth, arrowBase.y - side.y * tipHalfWidth)
        close()
    }
    drawPath(head, FoldballColor.Tape)
    drawPath(head, FoldballColor.Ink, style = Stroke(width = 1.5.dp.toPx()))
}

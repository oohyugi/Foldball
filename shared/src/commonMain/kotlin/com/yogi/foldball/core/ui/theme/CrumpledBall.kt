package com.yogi.foldball.core.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BallOutline = Path().apply {
    moveTo(30f, 3f)
    lineTo(43f, 6f)
    lineTo(53f, 15f)
    lineTo(57f, 29f)
    lineTo(52f, 44f)
    lineTo(41f, 54f)
    lineTo(26f, 57f)
    lineTo(13f, 51f)
    lineTo(5f, 39f)
    lineTo(6f, 22f)
    lineTo(15f, 10f)
    close()
}

private val Facets = listOf(
    Path().apply { moveTo(30f, 3f); lineTo(43f, 6f); lineTo(36f, 22f); lineTo(22f, 17f); lineTo(15f, 10f); close() } to Color(0xFFF6F3EA),
    Path().apply { moveTo(43f, 6f); lineTo(53f, 15f); lineTo(57f, 29f); lineTo(41f, 30f); lineTo(36f, 22f); close() } to Color(0xFFDFE0E8),
    Path().apply { moveTo(57f, 29f); lineTo(52f, 44f); lineTo(41f, 54f); lineTo(34f, 40f); lineTo(41f, 30f); close() } to Color(0xFFC3C7D6),
    Path().apply { moveTo(41f, 54f); lineTo(26f, 57f); lineTo(22f, 41f); lineTo(34f, 40f); close() } to Color(0xFFD6D8E3),
    Path().apply { moveTo(26f, 57f); lineTo(13f, 51f); lineTo(5f, 39f); lineTo(18f, 34f); lineTo(22f, 41f); close() } to Color(0xFFEBE8DD),
    Path().apply { moveTo(5f, 39f); lineTo(6f, 22f); lineTo(15f, 10f); lineTo(22f, 17f); lineTo(18f, 34f); close() } to Color(0xFFE2E2EA),
    Path().apply { moveTo(22f, 17f); lineTo(36f, 22f); lineTo(41f, 30f); lineTo(34f, 40f); lineTo(22f, 41f); lineTo(18f, 34f); close() } to Color(0xFFF9F7F0)
)

@Composable
fun CrumpledBall(modifier: Modifier = Modifier, size: Dp = 48.dp, tint: Color = Color.Unspecified) {
    Canvas(modifier.size(size)) {
        scale(this.size.width / 60f, Offset.Zero) {
            clipPath(BallOutline) {
                Facets.forEach { (p, c) ->
                    drawPath(p, c)
                    drawPath(p, FoldballColor.InkBlue.copy(alpha = 0.55f), style = Stroke(.9f, join = StrokeJoin.Round))
                }
                for (y in listOf(18f, 31f, 44f)) {
                    drawLine(FoldballColor.InkBlue.copy(alpha = 0.55f), Offset(2f, y), Offset(58f, y - 3f), 1f)
                }
                val scribble = Path().apply {
                    moveTo(19f, 27f)
                    quadraticTo(22f, 23f, 25f, 27f)
                    quadraticTo(28f, 31f, 31f, 27f)
                    quadraticTo(34f, 23f, 37f, 27f)
                }
                drawPath(scribble, FoldballColor.InkBlue.copy(alpha = 0.8f), style = Stroke(1.3f, cap = StrokeCap.Round))

                if (tint != Color.Unspecified) {
                    drawPath(BallOutline, tint, blendMode = BlendMode.Multiply)
                }
            }
            drawPath(BallOutline, FoldballColor.Ink, style = Stroke(2.4f, join = StrokeJoin.Round))
        }
    }
}

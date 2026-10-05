package com.yogi.foldball.feature.match.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.TapeLabel

private val powerSegments = listOf(
    Color(0xFFE53935),
    Color(0xFFFF5722),
    Color(0xFFFF8A00),
    Color(0xFFFFC928),
    Color(0xFFE9F22E),
    Color(0xFFA6E52B),
    Color(0xFF39D83A),
)

/** Vertical timing meter for the active shot; the parent owns its animated power value. */
@Composable
fun PowerTimingMeter(
    power: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.width(44.dp).height(164.dp)) {
            val outline = 2.dp.toPx()
            val frameRadius = 9.dp.toPx()
            drawRoundRect(
                color = FoldballColor.Ink,
                topLeft = Offset(outline, outline),
                size = Size(size.width - outline * 2, size.height - outline * 2),
                cornerRadius = CornerRadius(frameRadius),
            )
            drawRoundRect(
                color = FoldballColor.Paper,
                topLeft = Offset(outline, outline),
                size = Size(size.width - outline * 2, size.height - outline * 2),
                cornerRadius = CornerRadius(frameRadius),
                style = Stroke(width = 2.dp.toPx()),
            )

            val innerTop = 8.dp.toPx()
            val innerBottom = size.height - 8.dp.toPx()
            val segmentGap = 1.5.dp.toPx()
            val segmentHeight = (innerBottom - innerTop - segmentGap * (powerSegments.size - 1)) /
                powerSegments.size
            val segmentLeft = 6.dp.toPx()
            val segmentWidth = size.width - segmentLeft - 6.dp.toPx()
            powerSegments.forEachIndexed { index, color ->
                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        segmentLeft,
                        innerTop + index * (segmentHeight + segmentGap),
                    ),
                    size = Size(segmentWidth, segmentHeight),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
            }

            val markerY = innerBottom - (innerBottom - innerTop) * power.coerceIn(0f, 1f)
            val marker = Path().apply {
                moveTo(size.width - 1.dp.toPx(), markerY)
                lineTo(size.width - 13.dp.toPx(), markerY - 8.dp.toPx())
                lineTo(size.width - 13.dp.toPx(), markerY + 8.dp.toPx())
                close()
            }
            drawPath(marker, color = FoldballColor.Ink)
            drawPath(marker, color = FoldballColor.Ink, style = Stroke(width = 1.4.dp.toPx()))
            val whiteMarker = Path().apply {
                moveTo(size.width - 2.dp.toPx(), markerY)
                lineTo(size.width - 13.dp.toPx(), markerY - 6.dp.toPx())
                lineTo(size.width - 13.dp.toPx(), markerY + 6.dp.toPx())
                close()
            }
            drawPath(whiteMarker, color = FoldballColor.Paper)
        }

        TapeLabel("Power", Modifier.padding(top = 3.dp), compact = true)
    }
}

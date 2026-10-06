package com.yogi.foldball.gamekit.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.FoldballColor

/** Unlabelled visual status; fill length conveys remaining condition as well as color. */
@Composable
fun StandeeConditionBar(damage: Float, modifier: Modifier = Modifier) {
    val remaining = 1f - damage.coerceIn(0f, 1f)
    val color = if (remaining >=
        .5f
    ) {
        lerp(FoldballColor.Tape, FoldballColor.Grass, (remaining - .5f) * 2f)
    } else {
        lerp(FoldballColor.TeamRed, FoldballColor.Tape, remaining * 2f)
    }
    Canvas(modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo(remaining, 0f..1f) }) {
        val inset = 1.dp.toPx()
        val radius = CornerRadius(2.dp.toPx())
        drawRoundRect(FoldballColor.Paper, cornerRadius = radius)
        val innerWidth = (size.width - inset * 2).coerceAtLeast(0f)
        val innerHeight = (size.height - inset * 2).coerceAtLeast(0f)
        if (remaining >
            0f
        ) {
            drawRoundRect(
                color,
                Offset(inset, inset),
                Size(innerWidth * remaining, innerHeight),
                radius
            )
        }
        drawRoundRect(FoldballColor.Ink, cornerRadius = radius, style = Stroke(.8.dp.toPx()))
    }
}

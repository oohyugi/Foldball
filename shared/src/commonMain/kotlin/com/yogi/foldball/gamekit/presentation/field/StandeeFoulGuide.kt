package com.yogi.foldball.gamekit.presentation.field

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.FoldballColor

/** Practice-only foul range after opponent contact; does not change the collider. */
fun DrawScope.drawStandeeFoulGuide(center: Offset, ballRadius: Float, contactRadius: Float) {
    val radius = contactRadius + ballRadius * 2f
    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
    drawCircle(FoldballColor.TeamRed.copy(alpha = .06f), radius, center)
    drawCircle(FoldballColor.Paper.copy(alpha = .8f), radius, center, style = Stroke(2.4.dp.toPx(), pathEffect = dash))
    drawCircle(FoldballColor.TeamRed, radius, center, style = Stroke(1.2.dp.toPx(), pathEffect = dash))
}

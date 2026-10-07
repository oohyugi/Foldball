package com.yogi.foldball.feature.home.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.FoldballColor

/** Small decorative equipment sketches; card text supplies accessibility. */
@Composable
fun DeskMenuArt(notebook: Boolean) {
    Canvas(Modifier.size(64.dp).clearAndSetSemantics { }) {
        val inset = 6.dp.toPx()
        val origin = Offset(inset, inset)
        val paperSize = Size(size.width - inset * 2, size.height - inset * 2)
        drawRect(FoldballColor.Shadow.copy(alpha = .25f), origin + Offset(2.dp.toPx(), 3.dp.toPx()), paperSize)
        drawRect(if (notebook) FoldballColor.FieldHighlight else FoldballColor.Grass, origin, paperSize)
        drawRect(FoldballColor.Ink.copy(alpha = .7f), origin, paperSize, style = Stroke(1.dp.toPx()))
        if (notebook) {
            repeat(4) { index ->
                val y = inset + (index + 1) * paperSize.height / 5
                drawLine(FoldballColor.PaperLine, Offset(inset + 4.dp.toPx(), y), Offset(size.width - inset - 4.dp.toPx(), y), 1.dp.toPx())
            }
            drawLine(FoldballColor.TeamRed.copy(alpha = .6f), Offset(inset + 8.dp.toPx(), inset), Offset(inset + 8.dp.toPx(), size.height - inset), 1.dp.toPx())
            drawLine(FoldballColor.InkBlue, Offset(size.width * .4f, size.height * .64f), Offset(size.width * .7f, size.height * .36f), 2.dp.toPx())
            drawLine(FoldballColor.InkBlue, Offset(size.width * .7f, size.height * .36f), Offset(size.width * .7f, size.height * .52f), 2.dp.toPx())
            drawLine(FoldballColor.InkBlue, Offset(size.width * .7f, size.height * .36f), Offset(size.width * .54f, size.height * .36f), 2.dp.toPx())
        } else {
            val border = inset + 5.dp.toPx()
            drawRect(FoldballColor.Paper, Offset(border, border), Size(size.width - border * 2, size.height - border * 2), style = Stroke(1.dp.toPx()))
            drawLine(FoldballColor.Paper, Offset(border, center.y), Offset(size.width - border, center.y), 1.dp.toPx())
            drawCircle(FoldballColor.Paper, 8.dp.toPx(), center, style = Stroke(1.dp.toPx()))
            drawCircle(FoldballColor.TeamRed, 3.dp.toPx(), Offset(size.width * .38f, size.height * .7f))
            drawCircle(FoldballColor.InkBlue, 3.dp.toPx(), Offset(size.width * .62f, size.height * .3f))
        }
    }
}

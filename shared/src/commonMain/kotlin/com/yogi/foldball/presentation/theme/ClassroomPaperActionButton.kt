package com.yogi.foldball.presentation.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.icon_book
import foldball.shared.generated.resources.icon_paper_plane
import foldball.shared.generated.resources.icon_pause
import foldball.shared.generated.resources.icon_play
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class ClassroomPaperIcon(
    internal val resource: DrawableResource,
) {
    BOOK(Res.drawable.icon_book),
    PAPER_PLANE(Res.drawable.icon_paper_plane),
    PAUSE(Res.drawable.icon_pause),
    PLAY(Res.drawable.icon_play),
}

/** Small paper button used by the classroom match HUD. */
@Composable
fun ClassroomPaperActionButton(
    icon: ClassroomPaperIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val paper = Path().apply {
                moveTo(w * .12f, h * .19f)
                lineTo(w * .27f, h * .12f)
                lineTo(w * .48f, h * .15f)
                lineTo(w * .67f, h * .10f)
                lineTo(w * .88f, h * .17f)
                lineTo(w * .91f, h * .39f)
                lineTo(w * .87f, h * .64f)
                lineTo(w * .91f, h * .83f)
                lineTo(w * .70f, h * .89f)
                lineTo(w * .47f, h * .85f)
                lineTo(w * .27f, h * .90f)
                lineTo(w * .11f, h * .81f)
                lineTo(w * .15f, h * .56f)
                close()
            }
            translate(top = 2.dp.toPx()) { drawPath(paper, ClassroomTheme.PaperShadow) }
            drawPath(paper, ClassroomTheme.Paper)
            drawPath(paper, ClassroomTheme.PaperEdge, style = Stroke(1.dp.toPx()))
        }
        Image(
            painter = painterResource(icon.resource),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 8.dp),
        )
    }
}

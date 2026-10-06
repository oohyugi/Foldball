package com.yogi.foldball.gamekit.presentation.ball

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.FoldballColor
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.origami_ball_facets_v1
import foldball.shared.generated.resources.paper_crumple_texture
import foldball.shared.generated.resources.paper_texture_512
import kotlin.math.abs
import kotlin.math.sin
import org.jetbrains.compose.resources.imageResource

/** Keep the opening toss ball and the in-match ball at the same visual size. */
val MatchBallVisualSize = 16.dp

/** Motion cases shown by the standalone ball presentation. */
enum class BallEffect(val label: String, val description: String, val durationMillis: Int) {
    ROLL("Smooth roll", "Texture rotates with travel distance", 1100),
    AIR_BOUNCE("Air bounce", "Visual height and shadow separate", 1000),
    WALL_BOUNCE("Wall bounce", "Short streak follows the rebound", 950),
    PLAYER_BOUNCE("Player bounce", "Ring and paper flecks on contact", 950),
    HIT_SOFT("Soft impact", "Light squash and a thin ring", 800),
    HIT_HARD("Hard impact", "Stronger deformation on contact", 800),
    LANDING("Landing", "Ball drops, shadow sharpens, dust spreads", 1000),
    SKID("Skid", "Friction trail fades behind the ball", 1000),
    SPEED_TRAIL("Speed trail", "Trail follows the path and speed", 1000),
    GOAL("Goal burst", "Paper flakes and confetti on goal", 1250)
}

/**
 * Procedural paper ball. Physics stays external; the two angles only move surface layers.
 * The mask, light, and inner edge shade stay fixed relative to the camera.
 */
@Composable
fun PaperBall(
    modifier: Modifier = Modifier,
    rotationX: Float,
    rotationY: Float,
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    impactSideX: Float = 0f,
    impactSideY: Float = 0f,
    impactStrength: Float = 0f
) {
    val paper = imageResource(Res.drawable.paper_texture_512)
    val facets = imageResource(Res.drawable.origami_ball_facets_v1)
    val paperCrinkles = imageResource(Res.drawable.paper_crumple_texture)
    Canvas(
        modifier = modifier.graphicsLayer {
            val impact = impactStrength.coerceIn(0f, 1f)
            this.scaleX = scaleX * (1f - abs(impactSideX) * impact * .2f)
            this.scaleY =
                scaleY * (1f - abs(impactSideY) * impact * .2f + abs(impactSideX) * impact * .06f)
            transformOrigin = TransformOrigin(
                pivotFractionX = when {
                    impactSideX > 0f -> 1f
                    impactSideX < 0f -> 0f
                    else -> .5f
                },
                pivotFractionY = when {
                    impactSideY > 0f -> 1f
                    impactSideY < 0f -> 0f
                    else -> .5f
                }
            )
        }.clipToBounds().semantics { contentDescription = "Foldball paper ball" }
    ) {
        val radius = minOf(size.width, size.height) * .5f
        val center = Offset(size.width * .5f, size.height * .5f)
        if (radius <= 0f) return@Canvas

        drawCircle(FoldballColor.Paper, radius, center)
        val ballMask = Path().apply {
            addOval(
                Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
            )
        }
        val paperScale = 1.08f
        // Roll the surface along the travel axis: vertical travel scrolls folds
        // vertically, horizontal travel scrolls them horizontally. The surface
        // moves opposite the ball's travel, as it does for a rolling object.
        val surfaceOffset = Offset(
            sin(rotationY) * radius * .42f,
            -sin(rotationX) * radius * .42f
        )
        clipPath(ballMask) {
            drawOffsetTexture(paper, center, radius, paperScale, surfaceOffset, alpha = 1f)
            // Overscan the folded surface so directional scrolling never exposes
            // an edge at the circular mask.
            drawBallSurface(
                bitmap = facets,
                center = center,
                radius = radius,
                scale = 1.5f,
                offset = surfaceOffset,
                alpha = .98f
            )
            drawPaperCrinkleTexture(
                bitmap = paperCrinkles,
                center = center,
                radius = radius,
                scale = 1.5f,
                offset = surfaceOffset,
                alpha = .07f
            )

            val impact = impactStrength.coerceIn(0f, 1f)
            if (impact > .01f && (impactSideX != 0f || impactSideY != 0f)) {
                val contact = Offset(
                    center.x + impactSideX * radius * .78f,
                    center.y + impactSideY * radius * .78f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            FoldballColor.WoodDark.copy(alpha = impact * .28f),
                            Color.Transparent
                        ),
                        center = contact,
                        radius = radius * .62f
                    ),
                    radius = radius,
                    center = center
                )

                val x = center.x + impactSideX * radius * .94f
                val y = center.y + impactSideY * radius * .94f
                repeat(3) { index ->
                    val tangent = (index - 1) * radius * .22f
                    val start = Offset(
                        x - impactSideX * radius * .025f - impactSideY * tangent,
                        y - impactSideY * radius * .025f + impactSideX * tangent
                    )
                    val end = Offset(
                        center.x - impactSideX * radius * (.24f + index * .07f),
                        center.y - impactSideY * radius * (.24f + index * .07f) + tangent * .42f
                    )
                    drawLine(
                        color = FoldballColor.WoodDark.copy(alpha = impact * .52f),
                        start = start,
                        end = end,
                        strokeWidth = (1f + impact * 1.2f).dp.toPx()
                    )
                }
            }
        }

        // Fixed camera lighting: the paper moves under these layers.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = .42f), Color.Transparent),
                center = Offset(center.x - radius * .34f, center.y - radius * .39f),
                radius = radius * .98f
            ),
            radius = radius,
            center = center
        )
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    .64f to Color.Transparent,
                    .88f to Color.Black.copy(alpha = .035f),
                    1f to Color.Black.copy(alpha = .22f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        drawCircle(Color.White.copy(alpha = .30f), radius, center, style = Stroke(1.dp.toPx()))
    }
}

/** Draws a retained surface map larger than the mask so UV-style offsets never expose an edge. */
private fun DrawScope.drawOffsetTexture(
    bitmap: ImageBitmap,
    center: Offset,
    radius: Float,
    scale: Float,
    offset: Offset,
    alpha: Float
) {
    val side = (radius * 3f * scale).toInt().coerceAtLeast(1)
    val topLeft = IntOffset(
        (center.x - side * .5f + offset.x).toInt(),
        (center.y - side * .5f + offset.y).toInt()
    )
    drawImage(
        image = bitmap,
        dstOffset = topLeft,
        dstSize = IntSize(side, side),
        alpha = alpha,
        filterQuality = FilterQuality.Medium
    )
}

/** Draw a complete circular surface map at ball scale rather than magnifying its center. */
private fun DrawScope.drawBallSurface(
    bitmap: ImageBitmap,
    center: Offset,
    radius: Float,
    scale: Float,
    offset: Offset,
    alpha: Float
) {
    val side = (radius * 2f * scale).toInt().coerceAtLeast(1)
    drawImage(
        image = bitmap,
        dstOffset = IntOffset(
            (center.x - side * .5f + offset.x).toInt(),
            (
                center.y - side * .5f +
                    offset.y
                ).toInt()
        ),
        dstSize = IntSize(side, side),
        alpha = alpha,
        filterQuality = FilterQuality.Medium
    )
}

/** Preserve the wide paper texture's aspect ratio while covering the circular mask. */
private fun DrawScope.drawPaperCrinkleTexture(
    bitmap: ImageBitmap,
    center: Offset,
    radius: Float,
    scale: Float,
    offset: Offset,
    alpha: Float
) {
    val diameter = (radius * 2f * scale).toInt().coerceAtLeast(1)
    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val width = (diameter * aspectRatio.coerceAtLeast(1f)).toInt().coerceAtLeast(1)
    val height = (diameter / aspectRatio.coerceAtMost(1f)).toInt().coerceAtLeast(1)
    drawImage(
        image = bitmap,
        dstOffset = IntOffset(
            (center.x - width * .5f + offset.x).toInt(),
            (
                center.y - height * .5f +
                    offset.y
                ).toInt()
        ),
        dstSize = IntSize(width, height),
        alpha = alpha,
        filterQuality = FilterQuality.Medium
    )
}

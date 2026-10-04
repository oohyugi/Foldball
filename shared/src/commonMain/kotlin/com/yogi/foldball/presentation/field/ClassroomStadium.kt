package com.yogi.foldball.presentation.field

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.desk_wood
import foldball.shared.generated.resources.field_green_paper
import foldball.shared.generated.resources.wall_cardboard_tile
import org.jetbrains.compose.resources.painterResource

/** Classroom Theme colors from classroom_theme.json */
private val FieldLineColor = Color(0xFFF4F0E6)
private val CardboardWallColor = Color(0xFFC49A6C)
private val CardboardStrokeColor = Color(0xFF6C4A2A)
private val NetBgColor = Color(0xFFF2E6CF)
private val NetMeshColor = Color(0xFFB89C77)

@Composable
fun ClassroomStadium(
    modifier: Modifier = Modifier,
    fieldWidthDp: Float = 393f,
    fieldHeightDp: Float = 470f,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(fieldHeightDp.dp)
            .shadow(4.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp)),
    ) {
        // Layer 1: Desk Wood Background
        Image(
            painter = painterResource(Res.drawable.desk_wood),
            contentDescription = "Wooden Desk",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Layer 2: Classroom Desk Decor (Notebook, ruler, pencil, paperclips outside pitch)
        ClassroomDeskDecorOverlay()

        // Layer 3: Playable Field Container (Paper surface + Cardboard walls + Canvas markings)
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Layer 3a: Green Paper Field Surface
            Image(
                painter = painterResource(Res.drawable.field_green_paper),
                contentDescription = "Green Paper Field",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )

            // Layer 3b: Cardboard Collision Wall Frame (Outer Wall)
            Image(
                painter = painterResource(Res.drawable.wall_cardboard_tile),
                contentDescription = "Cardboard Wall Frame",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                alpha = 0.85f,
            )

            // Layer 4: Field Markings Runtime (Canvas)
            Canvas(Modifier.fillMaxSize()) {
                drawClassroomFieldMarkings(
                    w = size.width,
                    h = size.height,
                    marginPx = 18.dp.toPx(),
                )
            }

            // Layer 5: Classroom Goals (Top & Bottom)
            ClassroomGoalTop(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .size(160.dp, 36.dp),
            )
            ClassroomGoalBottom(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .size(160.dp, 36.dp),
            )

            // Layer 6: Match Entities & Interactive Content (Players, Ball, VFX)
            content()
        }
    }
}

/** Draws runtime Kotlin/Compose Canvas markings for the Classroom pitch. */
private fun DrawScope.drawClassroomFieldMarkings(w: Float, h: Float, marginPx: Float) {
    val lineWidth = 1.8.dp.toPx()
    val boundaryCornerRadius = CornerRadius(20.dp.toPx())

    // Field Boundary Outline
    drawRoundRect(
        color = FieldLineColor.copy(alpha = 0.92f),
        topLeft = Offset(marginPx, marginPx),
        size = Size(w - marginPx * 2, h - marginPx * 2),
        cornerRadius = boundaryCornerRadius,
        style = Stroke(width = lineWidth),
    )

    // Inner subtle drop shadow for cardboard walls
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.12f),
        topLeft = Offset(marginPx, marginPx),
        size = Size(w - marginPx * 2, h - marginPx * 2),
        cornerRadius = boundaryCornerRadius,
        style = Stroke(width = 3.dp.toPx()),
    )

    // Half-way Line
    drawLine(
        color = FieldLineColor.copy(alpha = 0.85f),
        start = Offset(marginPx, h / 2),
        end = Offset(w - marginPx, h / 2),
        strokeWidth = lineWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
    )

    // Center Circle & Spot
    val centerCircleRadius = h * 0.105f
    val centerSpot = Offset(w / 2, h / 2)
    drawCircle(
        color = Color(0xFFF9F6F0).copy(alpha = 0.15f),
        radius = centerCircleRadius,
        center = centerSpot,
    )
    drawCircle(
        color = FieldLineColor.copy(alpha = 0.85f),
        radius = centerCircleRadius,
        center = centerSpot,
        style = Stroke(width = lineWidth),
    )
    drawCircle(
        color = FieldLineColor,
        radius = 3.dp.toPx(),
        center = centerSpot,
    )

    // Top Penalty Area
    val penaltyWidth = w * 0.48f
    val penaltyHeight = h * 0.16f
    val topPenaltyLeft = (w - penaltyWidth) / 2
    drawRect(
        color = FieldLineColor.copy(alpha = 0.82f),
        topLeft = Offset(topPenaltyLeft, marginPx),
        size = Size(penaltyWidth, penaltyHeight),
        style = Stroke(width = lineWidth),
    )
    // Top Goal Area
    val goalBoxWidth = w * 0.26f
    val goalBoxHeight = h * 0.065f
    val topGoalBoxLeft = (w - goalBoxWidth) / 2
    drawRect(
        color = FieldLineColor.copy(alpha = 0.82f),
        topLeft = Offset(topGoalBoxLeft, marginPx),
        size = Size(goalBoxWidth, goalBoxHeight),
        style = Stroke(width = lineWidth),
    )

    // Bottom Penalty Area
    drawRect(
        color = FieldLineColor.copy(alpha = 0.82f),
        topLeft = Offset(topPenaltyLeft, h - marginPx - penaltyHeight),
        size = Size(penaltyWidth, penaltyHeight),
        style = Stroke(width = lineWidth),
    )
    // Bottom Goal Area
    drawRect(
        color = FieldLineColor.copy(alpha = 0.82f),
        topLeft = Offset(topGoalBoxLeft, h - marginPx - goalBoxHeight),
        size = Size(goalBoxWidth, goalBoxHeight),
        style = Stroke(width = lineWidth),
    )

    // Corner Arcs (4 Corners)
    val arcRadius = 14.dp.toPx()
    val arcSize = Size(arcRadius * 2, arcRadius * 2)
    // Top Left
    drawArc(
        color = FieldLineColor.copy(alpha = 0.82f),
        startAngle = 0f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(marginPx, marginPx),
        size = arcSize,
        style = Stroke(width = lineWidth),
    )
    // Top Right
    drawArc(
        color = FieldLineColor.copy(alpha = 0.82f),
        startAngle = 90f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(w - marginPx - arcRadius * 2, marginPx),
        size = arcSize,
        style = Stroke(width = lineWidth),
    )
    // Bottom Left
    drawArc(
        color = FieldLineColor.copy(alpha = 0.82f),
        startAngle = 270f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(marginPx, h - marginPx - arcRadius * 2),
        size = arcSize,
        style = Stroke(width = lineWidth),
    )
    // Bottom Right
    drawArc(
        color = FieldLineColor.copy(alpha = 0.82f),
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(w - marginPx - arcRadius * 2, h - marginPx - arcRadius * 2),
        size = arcSize,
        style = Stroke(width = lineWidth),
    )
}

/** Top Goal rendering using Classroom Cardboard & Net Mesh aesthetic. */
@Composable
private fun ClassroomGoalTop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Net background
        drawRoundRect(
            color = NetBgColor,
            topLeft = Offset(w * 0.12f, h * 0.15f),
            size = Size(w * 0.76f, h * 0.70f),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )

        // Net Mesh Lines
        val netRows = 4
        val netCols = 10
        for (i in 1 until netCols) {
            val x = w * 0.12f + (w * 0.76f) * (i.toFloat() / netCols)
            drawLine(
                color = NetMeshColor,
                start = Offset(x, h * 0.15f),
                end = Offset(x, h * 0.85f),
                strokeWidth = 1.2.dp.toPx(),
            )
        }
        for (j in 1 until netRows) {
            val y = h * 0.15f + (h * 0.70f) * (j.toFloat() / netRows)
            drawLine(
                color = NetMeshColor,
                start = Offset(w * 0.12f, y),
                end = Offset(w * 0.88f, y),
                strokeWidth = 1.2.dp.toPx(),
            )
        }

        // Cardboard Frame
        drawRoundRect(
            color = CardboardWallColor,
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = Size(w * 0.84f, h * 0.84f),
            cornerRadius = CornerRadius(6.dp.toPx()),
            style = Stroke(width = 3.dp.toPx()),
        )

        // Goal Posts (Left & Right)
        drawRect(
            color = Color(0xFFC18B52),
            topLeft = Offset(w * 0.05f, 0f),
            size = Size(w * 0.09f, h),
        )
        drawRect(
            color = CardboardStrokeColor,
            topLeft = Offset(w * 0.05f, 0f),
            size = Size(w * 0.09f, h),
            style = Stroke(width = 1.5.dp.toPx()),
        )

        drawRect(
            color = Color(0xFFC18B52),
            topLeft = Offset(w * 0.86f, 0f),
            size = Size(w * 0.09f, h),
        )
        drawRect(
            color = CardboardStrokeColor,
            topLeft = Offset(w * 0.86f, 0f),
            size = Size(w * 0.09f, h),
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}

/** Bottom Goal rendering using Classroom Cardboard & Net Mesh aesthetic. */
@Composable
private fun ClassroomGoalBottom(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Net background
        drawRoundRect(
            color = NetBgColor,
            topLeft = Offset(w * 0.12f, h * 0.15f),
            size = Size(w * 0.76f, h * 0.70f),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )

        // Net Mesh Lines
        val netRows = 4
        val netCols = 10
        for (i in 1 until netCols) {
            val x = w * 0.12f + (w * 0.76f) * (i.toFloat() / netCols)
            drawLine(
                color = NetMeshColor,
                start = Offset(x, h * 0.15f),
                end = Offset(x, h * 0.85f),
                strokeWidth = 1.2.dp.toPx(),
            )
        }
        for (j in 1 until netRows) {
            val y = h * 0.15f + (h * 0.70f) * (j.toFloat() / netRows)
            drawLine(
                color = NetMeshColor,
                start = Offset(w * 0.12f, y),
                end = Offset(w * 0.88f, y),
                strokeWidth = 1.2.dp.toPx(),
            )
        }

        // Cardboard Frame
        drawRoundRect(
            color = CardboardWallColor,
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = Size(w * 0.84f, h * 0.84f),
            cornerRadius = CornerRadius(6.dp.toPx()),
            style = Stroke(width = 3.dp.toPx()),
        )

        // Goal Posts (Left & Right)
        drawRect(
            color = Color(0xFFC18B52),
            topLeft = Offset(w * 0.05f, 0f),
            size = Size(w * 0.09f, h),
        )
        drawRect(
            color = CardboardStrokeColor,
            topLeft = Offset(w * 0.05f, 0f),
            size = Size(w * 0.09f, h),
            style = Stroke(width = 1.5.dp.toPx()),
        )

        drawRect(
            color = Color(0xFFC18B52),
            topLeft = Offset(w * 0.86f, 0f),
            size = Size(w * 0.86f, 0f),
            style = Stroke(width = 1.5.dp.toPx()),
        )
        drawRect(
            color = Color(0xFFC18B52),
            topLeft = Offset(w * 0.86f, 0f),
            size = Size(w * 0.09f, h),
        )
        drawRect(
            color = CardboardStrokeColor,
            topLeft = Offset(w * 0.86f, 0f),
            size = Size(w * 0.09f, h),
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}

/** Non-playable desk props (pencil, eraser, ruler, paperclips, tape, notebook) scattered outside the pitch. */
@Composable
private fun ClassroomDeskDecorOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Pencil (top right desk margin)
        rotate(-18f, pivot = Offset(w * 0.88f, h * 0.07f)) {
            val pencilRect = Size(72.dp.toPx(), 12.dp.toPx())
            val topLeft = Offset(w * 0.88f - pencilRect.width / 2, h * 0.07f - pencilRect.height / 2)
            drawRoundRect(
                color = Color(0xFFF2C94C),
                topLeft = topLeft,
                size = pencilRect,
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
            drawRoundRect(
                color = Color(0xFF6B4F1D),
                topLeft = topLeft,
                size = pencilRect,
                cornerRadius = CornerRadius(4.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
            // Pencil tip
            val tipPath = Path().apply {
                moveTo(topLeft.x + pencilRect.width, topLeft.y)
                lineTo(topLeft.x + pencilRect.width + 12.dp.toPx(), topLeft.y + pencilRect.height / 2)
                lineTo(topLeft.x + pencilRect.width, topLeft.y + pencilRect.height)
                close()
            }
            drawPath(tipPath, Color(0xFFE8C090))
            // Eraser head
            drawRect(
                color = Color(0xFFE76F51),
                topLeft = topLeft,
                size = Size(10.dp.toPx(), pencilRect.height),
            )
        }

        // Eraser (bottom left desk margin)
        rotate(12f, pivot = Offset(w * 0.08f, h * 0.93f)) {
            val eraserSize = Size(36.dp.toPx(), 22.dp.toPx())
            val topLeft = Offset(w * 0.08f - eraserSize.width / 2, h * 0.93f - eraserSize.height / 2)
            drawRoundRect(
                color = Color(0xFFF06B6B),
                topLeft = topLeft,
                size = eraserSize,
                cornerRadius = CornerRadius(5.dp.toPx()),
            )
            drawRect(
                color = Color(0xFF5B8DEF),
                topLeft = Offset(topLeft.x + eraserSize.width * 0.5f, topLeft.y),
                size = Size(eraserSize.width * 0.5f, eraserSize.height),
            )
            drawRoundRect(
                color = Color(0xFF7A3940),
                topLeft = topLeft,
                size = eraserSize,
                cornerRadius = CornerRadius(5.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }

        // Paperclip Red (top left)
        rotate(25f, pivot = Offset(w * 0.06f, h * 0.08f)) {
            val center = Offset(w * 0.06f, h * 0.08f)
            val clipPath = Path().apply {
                moveTo(center.x - 6.dp.toPx(), center.y + 12.dp.toPx())
                cubicTo(
                    center.x - 14.dp.toPx(), center.y - 10.dp.toPx(),
                    center.x + 14.dp.toPx(), center.y - 12.dp.toPx(),
                    center.x + 8.dp.toPx(), center.y + 6.dp.toPx(),
                )
            }
            drawPath(
                path = clipPath,
                color = Color(0xFFD64545),
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // Paperclip Blue (bottom right)
        rotate(-35f, pivot = Offset(w * 0.92f, h * 0.92f)) {
            val center = Offset(w * 0.92f, h * 0.92f)
            val clipPath = Path().apply {
                moveTo(center.x - 6.dp.toPx(), center.y + 12.dp.toPx())
                cubicTo(
                    center.x - 14.dp.toPx(), center.y - 10.dp.toPx(),
                    center.x + 14.dp.toPx(), center.y - 12.dp.toPx(),
                    center.x + 8.dp.toPx(), center.y + 6.dp.toPx(),
                )
            }
            drawPath(
                path = clipPath,
                color = Color(0xFF3973C6),
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // Tape piece (bottom edge margin)
        rotate(-4f, pivot = Offset(w * 0.50f, h * 0.97f)) {
            val tapeSize = Size(48.dp.toPx(), 14.dp.toPx())
            val topLeft = Offset(w * 0.50f - tapeSize.width / 2, h * 0.97f - tapeSize.height / 2)
            drawRoundRect(
                color = Color(0xFFF3E5AB).copy(alpha = 0.72f),
                topLeft = topLeft,
                size = tapeSize,
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
            drawRoundRect(
                color = Color(0xFFD8C784),
                topLeft = topLeft,
                size = tapeSize,
                cornerRadius = CornerRadius(2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

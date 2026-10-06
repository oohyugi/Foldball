package com.yogi.foldball.feature.home.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.CrumpledBall
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.gamekit.presentation.player.PaperPlayer
import com.yogi.foldball.gamekit.presentation.player.PlayerFacing

val FieldStripShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(0f, 0.04f * h)
    lineTo(0.08f * w, 0f)
    lineTo(0.22f * w, 0.05f * h)
    lineTo(0.40f * w, 0.01f * h)
    lineTo(0.58f * w, 0.05f * h)
    lineTo(0.76f * w, 0f)
    lineTo(0.92f * w, 0.04f * h)
    lineTo(w, 0.01f * h)
    lineTo(w, 0.96f * h)
    lineTo(0.86f * w, h)
    lineTo(0.68f * w, 0.95f * h)
    lineTo(0.50f * w, h)
    lineTo(0.32f * w, 0.96f * h)
    lineTo(0.14f * w, h)
    lineTo(0f, 0.95f * h)
    close()
}

@Composable
fun MenuField(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(FieldStripShape)
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color(0xFF3F8F43))

            val white = Color.White.copy(alpha = 0.9f)
            val darkGreen = Color(0xFF2F7A34).copy(alpha = 0.5f)

            drawLine(darkGreen, androidx.compose.ui.geometry.Offset(14f * size.width / 340f, 38f * size.height / 150f), androidx.compose.ui.geometry.Offset(80f * size.width / 340f, 22f * size.height / 150f), 2f)
            drawLine(white, androidx.compose.ui.geometry.Offset(size.width / 2f, 0f), androidx.compose.ui.geometry.Offset(size.width / 2f, size.height), 2f)
            drawCircle(white, radius = 28.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f), style = Stroke(2f))

            val cardboard = Color(0xFFC99A62)
            drawCardboard(cardboard, size.width, size.height)
        }

        fun xOffset(x: Int) = (x / 340f) * containerWidth.value
        fun yOffset(y: Int) = (y / 150f) * containerHeight.value

        val playerWidth = 44.dp
        val playerHeight = 52.dp

        // Player 1: Blue (GALIH, 9) at top-left
        PaperPlayer(
            modifier = Modifier
                .size(playerWidth, playerHeight)
                .offset(x = (xOffset(98) - playerWidth.value / 2).dp, y = (yOffset(60) - playerHeight.value / 2).dp)
                .graphicsLayer(rotationZ = -3f),
            facing = PlayerFacing.BACK,
            jerseyColor = FoldballColor.InkBlue,
            number = "9",
            name = "GALIH"
        )

        // Player 2: Blue (EKO, 7) at top-right
        PaperPlayer(
            modifier = Modifier
                .size(playerWidth, playerHeight)
                .offset(x = (xOffset(242) - playerWidth.value / 2).dp, y = (yOffset(56) - playerHeight.value / 2).dp)
                .graphicsLayer(rotationZ = 2f),
            facing = PlayerFacing.BACK,
            jerseyColor = FoldballColor.InkBlue,
            number = "7",
            name = "EKO"
        )

        // Player 3: Red (YOGI, 10) at bottom-left
        PaperPlayer(
            modifier = Modifier
                .size(playerWidth, playerHeight)
                .offset(x = (xOffset(104) - playerWidth.value / 2).dp, y = (yOffset(140) - playerHeight.value / 2).dp)
                .graphicsLayer(rotationZ = 2f),
            facing = PlayerFacing.FRONT,
            jerseyColor = FoldballColor.TeamRed,
            number = "10",
            name = "YOGI"
        )

        // Player 4: Red (BUDI, 8) at bottom-right
        PaperPlayer(
            modifier = Modifier
                .size(playerWidth, playerHeight)
                .offset(x = (xOffset(238) - playerWidth.value / 2).dp, y = (yOffset(138) - playerHeight.value / 2).dp)
                .graphicsLayer(rotationZ = -2f),
            facing = PlayerFacing.FRONT,
            jerseyColor = FoldballColor.TeamRed,
            number = "8",
            name = "BUDI"
        )

        CrumpledBall(
            modifier = Modifier.align(Alignment.Center),
            size = 32.dp
        )
    }
}

private fun DrawScope.drawCardboard(cardboard: Color, width: Float, height: Float) {
    drawRect(cardboard, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Size(14f, height))
    drawRect(cardboard, androidx.compose.ui.geometry.Offset(width - 14f, 0f), androidx.compose.ui.geometry.Size(14f, height))
}

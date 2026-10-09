package com.yogi.foldball.gamekit.presentation.field

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/** One simulation space; the layer also maps pointer positions and deltas back into it. */
object ArenaViewport {
    const val width = 393f
    const val matchHeight = 580f
    const val sandboxHeight = 470f

    fun scale(availableWidth: Float, availableHeight: Float, height: Float): Float =
        minOf(availableWidth / width, availableHeight / height)
}

fun Modifier.fitArena(availableWidth: Float, availableHeight: Float, height: Float): Modifier =
    layout { measurable, _ ->
        val scale = ArenaViewport.scale(availableWidth, availableHeight, height)
        val arenaWidth = ArenaViewport.width.dp.roundToPx()
        val arenaHeight = height.dp.roundToPx()
        val placeable = measurable.measure(Constraints.fixed(arenaWidth, arenaHeight))
        layout((arenaWidth * scale).toInt(), (arenaHeight * scale).toInt()) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
                clip = false
            }
        }
    }

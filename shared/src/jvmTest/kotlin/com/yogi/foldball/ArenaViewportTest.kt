package com.yogi.foldball

import com.yogi.foldball.gamekit.presentation.field.ArenaViewport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArenaViewportTest {
    @Test fun differentScreensPreserveShotTravelAndInputInFieldCoordinates() {
        for ((width, height) in listOf(320f to 420f, 393f to 580f, 600f to 900f)) {
            val scale = ArenaViewport.scale(width, height, ArenaViewport.matchHeight)
            val displayedFieldHeight = ArenaViewport.matchHeight * scale
            val displayedTravel = 180f * scale
            assertEquals(180f / 580f, displayedTravel / displayedFieldHeight, .00001f)
            assertEquals(180f, displayedTravel / scale, .0001f)
            assertTrue(ArenaViewport.width * scale <= width + .001f)
            assertTrue(displayedFieldHeight <= height + .001f)
            assertEquals(ArenaViewport.width / ArenaViewport.matchHeight,
                ArenaViewport.width * scale / displayedFieldHeight, .00001f)
        }
    }
}

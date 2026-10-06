package com.yogi.foldball.domain.team

import androidx.compose.ui.graphics.Color

enum class BallStyle(val label: String, val description: String, val tint: Color) {
    CLASSIC("Classic Paper", "Natural off-white crumpled paper ball", Color.Unspecified),
    KRAFT("Kraft Craft", "Warm golden cardboard and kraft paper", Color(0xFFF2C230)),
    NOTEBOOK_BLUE("Notebook Blue", "Blue-tinted notebook craft paper", Color(0xFF7FA6FF))
}

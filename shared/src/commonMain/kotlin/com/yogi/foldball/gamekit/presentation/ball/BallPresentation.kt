package com.yogi.foldball.gamekit.presentation.ball

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.CrumpledBall
import com.yogi.foldball.domain.team.BallStyle

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
 * Procedural paper ball using CrumpledBall vector drawing.
 */
@Composable
fun PaperBall(
    modifier: Modifier = Modifier,
    ballStyle: BallStyle = BallStyle.CLASSIC,
    rotationX: Float,
    rotationY: Float,
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    impactSideX: Float = 0f,
    impactSideY: Float = 0f,
    impactStrength: Float = 0f
) {
    CrumpledBall(
        modifier = modifier.semantics { contentDescription = "Foldball paper ball" },
        ballStyle = ballStyle,
        rotationX = rotationX,
        rotationY = rotationY,
        scaleX = scaleX,
        scaleY = scaleY,
        impactSideX = impactSideX,
        impactSideY = impactSideY,
        impactStrength = impactStrength
    )
}

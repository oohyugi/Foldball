package com.yogi.foldball.presentation.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/** Cosmetic motion presets. These transforms must never alter gameplay position or collider. */
enum class PlayerMotionPreset { IDLE, REPOSITION, APPROACH, AIM_LEFT, AIM_RIGHT, POWER, KICK, IMPACT }

class PlayerMotionState internal constructor() {
    private val x = Animatable(0f)
    private val y = Animatable(0f)
    private val tilt = Animatable(0f)
    private val scaleX = Animatable(1f)
    private val scaleY = Animatable(1f)
    private val powerCharge = Animatable(0f)
    private val powerPress = Animatable(0f)
    private val kickForward = Animatable(0f)
    private val ballTravel = Animatable(0f)
    private var launchedPower = 0f
    private val aimAngle = Animatable(0f)

    val offsetX get() = x.value
    val offsetY get() = y.value
    val rotation get() = tilt.value
    val horizontalScale get() = scaleX.value
    val verticalScale get() = scaleY.value
    val powerProgress get() = powerCharge.value
    val powerPressProgress get() = powerPress.value
    val kickProgress get() = kickForward.value
    val ballProgress get() = ballTravel.value
    val kickPower get() = launchedPower
    val shotAngle get() = aimAngle.value

    suspend fun setPowerProgress(progress: Float) {
        powerCharge.snapTo(progress.coerceIn(0f, 1f))
    }

    suspend fun startPowerCharge() {
        ballTravel.snapTo(0f)
        kickForward.snapTo(0f)
        powerPress.animateTo(1f, tween(85, easing = FastOutSlowInEasing))
    }

    suspend fun cancelPowerCharge() {
        powerPress.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
        powerCharge.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
    }

    suspend fun pullPowerBy(deltaDp: Float) {
        setPowerProgress(powerCharge.value + deltaDp / 68f)
    }

    suspend fun setAimAngle(angle: Float) {
        aimAngle.snapTo(angle.coerceIn(-1.05f, 1.05f))
    }

    /** Horizontal striker drag changes aim without moving the focused player's stance. */
    suspend fun aimBy(deltaDp: Float) {
        setAimAngle(aimAngle.value + deltaDp / 120f)
    }

    /** Launches the ball as the player starts its forward snap, then settles the player. */
    suspend fun releaseChargedKick() = coroutineScope {
        launchedPower = powerCharge.value
        kickForward.snapTo(0f)
        ballTravel.snapTo(0f)
        launch { ballTravel.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
        launch { powerCharge.animateTo(0f, tween(210, easing = FastOutSlowInEasing)) }
        launch { powerPress.animateTo(0f, tween(180, easing = FastOutSlowInEasing)) }
        kickForward.animateTo(1.08f, tween(95, easing = FastOutSlowInEasing))
        kickForward.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }

    suspend fun reset() {
        x.snapTo(0f); y.snapTo(0f); tilt.snapTo(0f); scaleX.snapTo(1f); scaleY.snapTo(1f)
        powerCharge.snapTo(0f); powerPress.snapTo(0f); kickForward.snapTo(0f); ballTravel.snapTo(0f)
        launchedPower = 0f
        aimAngle.snapTo(0f)
    }

    suspend fun dragBy(dxDp: Float, dyDp: Float) {
        val nextX = x.value + dxDp
        val nextY = y.value + dyDp
        val radius = sqrt(nextX * nextX + nextY * nextY)
        val factor = if (radius > 74f) 74f / radius else 1f
        x.snapTo(nextX * factor)
        y.snapTo(nextY * factor)
        tilt.snapTo((dxDp * .16f).coerceIn(-9f, 9f))
    }

    suspend fun settle() = coroutineScope {
        launch { tilt.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
        launch { scaleY.animateTo(1.035f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
        scaleY.animateTo(1f, tween(110))
    }

    suspend fun play(preset: PlayerMotionPreset) = coroutineScope {
        reset()
        when (preset) {
            PlayerMotionPreset.IDLE -> {
                scaleY.animateTo(.96f, tween(100))
                scaleY.animateTo(1.035f, spring(stiffness = Spring.StiffnessMedium))
                scaleY.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            PlayerMotionPreset.REPOSITION -> {
                launch { tilt.animateTo(5f, tween(190, easing = FastOutSlowInEasing)) }
                x.animateTo(42f, tween(330, easing = FastOutSlowInEasing))
                launch { scaleY.animateTo(.94f, tween(90)) }
                tilt.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                scaleY.animateTo(1.04f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                scaleY.animateTo(1f, tween(100))
            }
            PlayerMotionPreset.APPROACH -> {
                x.snapTo(-62f); y.snapTo(22f); tilt.snapTo(-4f)
                launch { y.animateTo(0f, tween(390, easing = FastOutSlowInEasing)) }
                x.animateTo(0f, tween(390, easing = FastOutSlowInEasing))
                tilt.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                scaleY.animateTo(1.035f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                scaleY.animateTo(1f, tween(100))
            }
            PlayerMotionPreset.AIM_LEFT, PlayerMotionPreset.AIM_RIGHT -> {
                val side = if (preset == PlayerMotionPreset.AIM_LEFT) -1f else 1f
                aimAngle.snapTo(.38f * side)
                tilt.animateTo(4f * side, spring(dampingRatio = Spring.DampingRatioLowBouncy))
            }
            PlayerMotionPreset.POWER -> {
                powerCharge.animateTo(.75f, tween(220, easing = FastOutSlowInEasing))
                delay(250)
                releaseChargedKick()
            }
            PlayerMotionPreset.KICK -> {
                launch { y.animateTo(-8f, tween(150)) }; launch { tilt.animateTo(2f, tween(150)) }
                launch { scaleX.animateTo(.94f, tween(110)) }
                scaleY.animateTo(.60f, tween(125, easing = FastOutSlowInEasing))
                launch { y.animateTo(7f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
                launch { tilt.animateTo(-4f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                launch { scaleX.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                scaleY.animateTo(1.14f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                launch { y.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                tilt.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)); scaleY.animateTo(1f, tween(140))
            }
            PlayerMotionPreset.IMPACT -> {
                launch { scaleX.animateTo(1.08f, tween(80)) }; launch { scaleY.animateTo(.82f, tween(80)) }
                tilt.animateTo(13f, tween(85, easing = FastOutSlowInEasing)); x.animateTo(15f, tween(90))
                launch { scaleX.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                launch { scaleY.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                launch { x.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                tilt.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
        }
    }
}

@Composable
fun rememberPlayerMotionState(): PlayerMotionState = remember { PlayerMotionState() }

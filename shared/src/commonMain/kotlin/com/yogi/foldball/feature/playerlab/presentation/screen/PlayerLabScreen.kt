package com.yogi.foldball.feature.playerlab.presentation.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogi.foldball.gamekit.presentation.player.PaperPlayer
import com.yogi.foldball.gamekit.presentation.player.PlayerFocusState
import com.yogi.foldball.gamekit.presentation.player.PlayerMotionPreset
import com.yogi.foldball.gamekit.presentation.player.PlayerMotionState
import com.yogi.foldball.gamekit.presentation.player.rememberPlayerMotionState
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.FoldballScaffold
import com.yogi.foldball.core.ui.theme.PaperButton
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.core.ui.theme.PaperChip
import com.yogi.foldball.core.ui.theme.TapeLabel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class PlayerDemo(val title: String, val caption: String, val motion: PlayerMotionPreset) {
    IDLE("Idle", "Idle and settle", PlayerMotionPreset.IDLE),
    REPOSITION("Move", "Reposition", PlayerMotionPreset.REPOSITION),
    AUTO_MOVE("Approach", "Kicker moves to the ball", PlayerMotionPreset.APPROACH),
    AIM_LEFT("Aim left", "Aim left", PlayerMotionPreset.AIM_LEFT),
    AIM_RIGHT("Aim right", "Aim right", PlayerMotionPreset.AIM_RIGHT),
    POWER("Power", "Pull and press", PlayerMotionPreset.POWER),
    KICK("Kick", "Spring and recoil", PlayerMotionPreset.KICK),
    COLLISION("Impact", "Ball or player collision", PlayerMotionPreset.IMPACT),
}

@Composable
fun PlayerLabScreen() {
    val motion = rememberPlayerMotionState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    var selected by remember { mutableStateOf(PlayerDemo.IDLE) }
    var replayKey by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var focusState by remember { mutableStateOf(PlayerFocusState.FOCUSED) }

    LaunchedEffect(selected, replayKey) {
        if (!dragging) motion.play(selected.motion)
    }

    FoldballScaffold {
      Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(Coral))
            Spacer(Modifier.width(8.dp))
            Text("FOLDBALL  /  PLAYER MOTION", color = MutedInk, style = FoldballType.Caption)
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Paper players,\nspring motion.", color = FoldballColor.Ink, style = FoldballType.LargeTitle, lineHeight = 39.sp)
            Text("Preview Foldball player animations", color = MutedInk, style = FoldballType.Body)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(PlayerFocusState.FOCUSED to "Active / focused", PlayerFocusState.INACTIVE to "Inactive").forEach { (state, label) ->
                PaperChip(
                    text = label,
                    onClick = {
                        focusState = state
                        dragging = false
                        selected = PlayerDemo.IDLE
                        replayKey++
                    },
                    contentDescription = "Set player focus to $label",
                    modifier = Modifier.weight(1f),
                    selected = focusState == state,
                )
            }
        }

        PlayerLabStage(
            selected = selected,
            motion = motion,
            focusState = focusState,
            onDragStart = { dragging = true; selected = PlayerDemo.REPOSITION },
            onDrag = { dx, dy -> scope.launch { motion.dragBy(dx / density, dy / density) } },
            onDragEnd = { dragging = false; scope.launch { motion.settle() } },
            onPowerPullStart = { scope.launch { motion.setPowerProgress(0f); motion.startPowerCharge() } },
            onPowerPull = { dy -> scope.launch { motion.pullPowerBy(dy / density) } },
            onAimDrag = { dx -> scope.launch { motion.aimBy(dx / density) } },
            onPowerPullEnd = {
                scope.launch {
                    if (motion.powerProgress > .01f) motion.releaseChargedKick()
                    else motion.cancelPowerCharge()
                }
            },
        )

        if (focusState == PlayerFocusState.FOCUSED) {
            PaperCard(Modifier.fillMaxWidth(), ruled = false, tilt = FoldballTilt.StatusPanel) {
                Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("POWER CHARGE", color = Ink, style = FoldballType.Caption)
                        Text("${(motion.powerProgress * 100).toInt()}%", color = Coral, style = FoldballType.Label)
                    }
                    Slider(
                        value = motion.powerProgress,
                        onValueChange = { progress -> scope.launch { motion.setPowerProgress(progress) } },
                        valueRange = 0f..1f,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Pull down to charge", Modifier.weight(1f), maxLines = 1,
                            overflow = TextOverflow.Ellipsis, color = MutedInk, style = FoldballType.Caption)
                        PaperButton(
                            text = "Kick",
                            enabled = motion.powerProgress > .01f,
                            onClick = { scope.launch { motion.releaseChargedKick() } },
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PLAYER ANIMATION", color = Ink, style = FoldballType.Caption)
                Spacer(Modifier.width(8.dp)); Text("•", color = Coral, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp)); Text("inactive: drag · focused: drag left/right to aim", color = MutedInk, style = FoldballType.Caption)
            }
            PlayerDemo.entries.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { demo ->
                        PaperChip(
                            text = demo.title,
                            onClick = {
                                dragging = false
                                focusState = when (demo) {
                                    PlayerDemo.REPOSITION -> PlayerFocusState.INACTIVE
                                    PlayerDemo.AUTO_MOVE, PlayerDemo.AIM_LEFT, PlayerDemo.AIM_RIGHT,
                                    PlayerDemo.POWER, PlayerDemo.KICK -> PlayerFocusState.FOCUSED
                                    else -> focusState
                                }
                                selected = demo
                                replayKey++
                            },
                            contentDescription = "Preview ${demo.caption}",
                            modifier = Modifier.weight(1f),
                            selected = selected == demo,
                        )
                    }
                }
            }
        }

        PaperCard(Modifier.fillMaxWidth(), ruled = false, tilt = FoldballTilt.StatusPanel) {
            Row(Modifier.padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("✦", color = Coral, fontSize = 17.sp)
                Column {
                    Text(selected.caption, color = Ink, style = FoldballType.Label)
                    Text("Foot pivot · physics and collider unchanged", color = MutedInk, style = FoldballType.Caption)
                }
            }
        }
      }
    }
}

@Composable
private fun PlayerLabStage(
    selected: PlayerDemo,
    motion: PlayerMotionState,
    focusState: PlayerFocusState,
    onDragStart: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onPowerPullStart: () -> Unit,
    onPowerPull: (Float) -> Unit,
    onPowerPullEnd: () -> Unit,
    onAimDrag: (Float) -> Unit,
) {
    PaperCard(Modifier.fillMaxWidth().height(336.dp), ruled = false, tilt = FoldballTilt.StatusPanel) {
      BoxWithConstraints(Modifier.fillMaxSize().clip(com.yogi.foldball.core.ui.theme.FoldballShapes.Card).background(Field)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val ball = Offset(w * .51f, h * .48f)
            drawRoundRect(FoldballColor.FieldHighlight, Offset(w * .09f, h * .14f), Size(w * .82f, h * .70f), CornerRadius(24.dp.toPx()))
            drawRoundRect(FieldLine, Offset(w * .09f, h * .14f), Size(w * .82f, h * .70f), CornerRadius(24.dp.toPx()), style = Stroke(1.2.dp.toPx()))
            drawLine(FieldLine.copy(alpha = .8f), Offset(w * .10f, h * .50f), Offset(w * .90f, h * .50f), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 6.dp.toPx())))
            drawCircle(FoldballColor.BallHighlight, h * .125f, Offset(w * .5f, h * .5f))
            drawCircle(FieldLine.copy(alpha = .7f), h * .125f, Offset(w * .5f, h * .5f), style = Stroke(1.dp.toPx()))
            if (focusState == PlayerFocusState.FOCUSED || selected in listOf(PlayerDemo.AIM_LEFT, PlayerDemo.AIM_RIGHT, PlayerDemo.POWER) || motion.powerProgress > .01f) {
                val angle = motion.shotAngle
                val end = Offset(ball.x + sin(angle) * h * .27f, ball.y - cos(angle) * h * .27f)
                drawLine(Coral.copy(alpha = .88f), ball, end, 3.dp.toPx(), cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())))
                drawCircle(Coral, 3.5.dp.toPx(), end)
            }
        }
        val px = maxWidth * .51f + motion.offsetX.dp
        val py = maxHeight * .48f + 104.dp + motion.offsetY.dp
        Canvas(Modifier.offset(px - 53.dp, py - 10.dp).size(106.dp, 19.dp)) {
            drawOval(FoldballColor.ShadowBrown.copy(alpha = .18f))
        }
        PaperPlayer(
            modifier = Modifier.offset(
                px - 74.dp + 8.dp * (sin(motion.shotAngle) * motion.kickProgress),
                py - 118.dp - 20.dp * (cos(motion.shotAngle) * motion.kickProgress),
            ).size(148.dp).graphicsLayer {
                rotationZ = motion.rotation; scaleX = motion.horizontalScale; scaleY = motion.verticalScale
                transformOrigin = TransformOrigin(.5f, .797f)
            }.pointerInput(focusState) {
                var activeDragMode = 0 // undecided, horizontal aim, or downward power
                var accumulatedX = 0f
                var accumulatedY = 0f
                detectDragGestures(
                    onDragStart = {
                        if (focusState == PlayerFocusState.INACTIVE) onDragStart()
                        else {
                            activeDragMode = 0
                            accumulatedX = 0f
                            accumulatedY = 0f
                        }
                    },
                    onDragEnd = {
                        if (focusState == PlayerFocusState.INACTIVE) onDragEnd()
                        else if (activeDragMode == 2) onPowerPullEnd()
                    },
                    onDragCancel = {
                        if (focusState == PlayerFocusState.INACTIVE) onDragEnd()
                        else if (activeDragMode == 2) onPowerPullEnd()
                    },
                ) { change, drag ->
                    change.consume()
                    if (focusState == PlayerFocusState.INACTIVE) onDrag(drag.x, drag.y)
                    else {
                        accumulatedX += drag.x
                        accumulatedY += drag.y
                        if (activeDragMode == 0) {
                            activeDragMode = when {
                                kotlin.math.abs(accumulatedX) > kotlin.math.abs(accumulatedY) -> 1
                                accumulatedY > 0f && kotlin.math.abs(accumulatedY) >= kotlin.math.abs(accumulatedX) -> 2
                                else -> 0
                            }
                            if (activeDragMode == 2) onPowerPullStart()
                        }
                        when (activeDragMode) {
                            1 -> onAimDrag(drag.x)
                            2 -> onPowerPull(drag.y)
                        }
                    }
                }
            },
            focusState = focusState,
            powerProgress = motion.powerProgress,
            powerPressProgress = motion.powerPressProgress,
            aimAngle = motion.shotAngle,
            kickProgress = motion.kickProgress,
        )
        val ballDistance = maxHeight * (.16f + .15f * motion.kickPower) * motion.ballProgress
        Canvas(Modifier.offset(
            maxWidth * .51f - 14.dp + ballDistance * sin(motion.shotAngle),
            maxHeight * .48f - 14.dp - ballDistance * cos(motion.shotAngle),
        ).size(28.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(Color(0xFFB8AA97).copy(alpha = .34f), 14.dp.toPx(), center + Offset(2.dp.toPx(), 4.dp.toPx()))
            drawCircle(Color(0xFFFCFBF7), 12.dp.toPx(), center)
            drawCircle(Ink, 12.dp.toPx(), center, style = Stroke(1.1.dp.toPx()))
            val pentagon = Path().apply {
                val r = 4.2.dp.toPx()
                repeat(5) { i ->
                    val angle = (i * 72.0 - 90.0) * PI / 180.0
                    val point = Offset(center.x + cos(angle).toFloat() * r, center.y + sin(angle).toFloat() * r)
                    if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
                close()
            }
            drawPath(pentagon, Ink)
        }
        TapeLabel(selected.title, Modifier.align(Alignment.TopStart).padding(15.dp))
        Text("CHARGE  •  SNAP  •  SETTLE", Modifier.align(Alignment.BottomCenter).padding(bottom = 13.dp),
            color = MutedInk, style = FoldballType.Caption)
      }
    }
}

private val Ink = com.yogi.foldball.core.ui.theme.FoldballColor.Ink
private val MutedInk = com.yogi.foldball.core.ui.theme.FoldballColor.InkMuted
private val Coral = com.yogi.foldball.core.ui.theme.FoldballColor.TeamRed
private val Field = com.yogi.foldball.core.ui.theme.FoldballColor.FieldPaper
private val FieldLine = com.yogi.foldball.core.ui.theme.FoldballColor.FieldLine

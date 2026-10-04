package com.yogi.foldball.feature.playerlab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.yogi.foldball.presentation.player.PaperPlayer
import com.yogi.foldball.presentation.player.PlayerFocusState
import com.yogi.foldball.presentation.player.PlayerMotionPreset
import com.yogi.foldball.presentation.player.PlayerMotionState
import com.yogi.foldball.presentation.player.rememberPlayerMotionState
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sin

private enum class PlayerDemo(val title: String, val caption: String, val motion: PlayerMotionPreset) {
    IDLE("Diam", "Idle dan settle", PlayerMotionPreset.IDLE),
    REPOSITION("Geser", "Reposition", PlayerMotionPreset.REPOSITION),
    AUTO_MOVE("Ke bola", "Kicker bergerak", PlayerMotionPreset.APPROACH),
    AIM_LEFT("Bidik kiri", "Aim kiri", PlayerMotionPreset.AIM_LEFT),
    AIM_RIGHT("Bidik kanan", "Aim kanan", PlayerMotionPreset.AIM_RIGHT),
    POWER("Power", "Tarik dan tekan", PlayerMotionPreset.POWER),
    KICK("Tendang", "Spring dan recoil", PlayerMotionPreset.KICK),
    COLLISION("Benturan", "Kena bola / pemain", PlayerMotionPreset.IMPACT),
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

    Column(
        Modifier.fillMaxSize().background(Paper).safeDrawingPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(Coral))
            Spacer(Modifier.width(8.dp))
            Text("FOLDBALL  /  PLAYER MOTION", color = MutedInk, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.15.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Bidak kertas,\ngerak pegas.", color = Ink, fontSize = 32.sp, lineHeight = 35.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = (-.7).sp)
            Text("Preview animasi player Foldball", color = MutedInk, fontSize = 14.sp)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(PlayerFocusState.FOCUSED to "Aktif / fokus", PlayerFocusState.INACTIVE to "Nonaktif").forEach { (state, label) ->
                Surface(
                    onClick = {
                        focusState = state
                        dragging = false
                        selected = PlayerDemo.IDLE
                        replayKey++
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (focusState == state) Ink else Color(0xFFFFFCF7),
                    contentColor = if (focusState == state) Color.White else Ink,
                    border = if (focusState == state) null else BorderStroke(1.dp, Color(0xFFE5DED2)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
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
            Surface(Modifier.fillMaxWidth(), color = Color(0xFFECE5D9), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("POWER CHARGE", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                        Text("${(motion.powerProgress * 100).toInt()}%", color = Coral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = motion.powerProgress,
                        onValueChange = { progress -> scope.launch { motion.setPowerProgress(progress) } },
                        valueRange = 0f..1f,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Tarik ke bawah untuk charge", Modifier.weight(1f), maxLines = 1,
                            overflow = TextOverflow.Ellipsis, color = MutedInk, fontSize = 11.sp)
                        TextButton(
                            enabled = motion.powerProgress > .01f,
                            onClick = { scope.launch { motion.releaseChargedKick() } },
                        ) { Text("Tendang") }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ANIMASI PLAYER", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.width(8.dp)); Text("•", color = Coral, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp)); Text("nonaktif: geser · fokus: geser kiri/kanan untuk bidik", color = MutedInk, fontSize = 11.sp)
            }
            PlayerDemo.entries.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { demo ->
                        Surface(
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
                            modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(14.dp),
                            color = if (selected == demo) Ink else Color(0xFFFFFCF7),
                            contentColor = if (selected == demo) Color.White else Ink,
                            border = if (selected == demo) null else BorderStroke(1.dp, Color(0xFFE5DED2)),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(demo.title, Modifier.padding(horizontal = 3.dp), maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        Surface(Modifier.fillMaxWidth(), color = Color(0xFFECE5D9), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Text("✦", color = Coral, fontSize = 17.sp)
                Column {
                    Text(selected.caption, color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Pivot di kaki · physics/collider tetap", color = MutedInk, fontSize = 11.sp)
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
    BoxWithConstraints(Modifier.fillMaxWidth().height(336.dp).shadow(3.dp, RoundedCornerShape(28.dp))
        .clip(RoundedCornerShape(28.dp)).background(Field)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val ball = Offset(w * .51f, h * .48f)
            drawRoundRect(Color(0xFFF1EBE0), Offset(w * .09f, h * .14f), Size(w * .82f, h * .70f), CornerRadius(24.dp.toPx()))
            drawRoundRect(FieldLine, Offset(w * .09f, h * .14f), Size(w * .82f, h * .70f), CornerRadius(24.dp.toPx()), style = Stroke(1.2.dp.toPx()))
            drawLine(FieldLine.copy(alpha = .8f), Offset(w * .10f, h * .50f), Offset(w * .90f, h * .50f), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 6.dp.toPx())))
            drawCircle(Color(0xFFF9F6F0), h * .125f, Offset(w * .5f, h * .5f))
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
            drawOval(Color(0xFF51483D).copy(alpha = .18f))
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
        Surface(Modifier.align(Alignment.TopStart).padding(15.dp), color = Color(0xFFFFFCF7), shape = RoundedCornerShape(50)) {
            Text(selected.title.uppercase(), Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = Ink,
                fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
        }
        Text("CHARGE  •  SNAP  •  SETTLE", Modifier.align(Alignment.BottomCenter).padding(bottom = 13.dp),
            color = MutedInk, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

private val Paper = Color(0xFFF5F0E6)
private val Ink = Color(0xFF24314A)
private val MutedInk = Color(0xFF777A80)
private val Coral = Color(0xFFE94F43)
private val Field = Color(0xFFE8E1D4)
private val FieldLine = Color(0xFFC7BFAF)

package com.yogi.foldball.feature.match

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogi.foldball.domain.game.engine.GameEngine
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameSnapshot
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.Vector2
import com.yogi.foldball.presentation.ball.PaperBall
import com.yogi.foldball.presentation.field.ClassroomStadium
import com.yogi.foldball.presentation.player.PaperPlayer
import com.yogi.foldball.presentation.player.PlayerFocusState
import com.yogi.foldball.presentation.player.rememberPlayerMotionState
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MatchSandboxScreen() {
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val playerMotion = rememberPlayerMotionState()
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF5F0E6)).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("FOLDBALL  /  MATCH MOTION", color = Color(0xFF777A80), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text("Gerak formasi & bola", color = Color(0xFF24314A), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("Geser pemain · bidik · charge · lihat pantulan", color = Color(0xFF777A80), fontSize = 13.sp)
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val fieldWidth = maxWidth.value
            val fieldHeight = 470f
            val engine = remember(fieldWidth, fieldHeight) { GameEngine(fieldWidth, fieldHeight) }
            var state by remember(engine) { mutableStateOf(engine.snapshot) }
            var kickingPlayerId by remember(engine) { mutableStateOf<String?>(null) }
            fun refresh() { state = engine.snapshot }
            fun shoot() {
                val shotState = engine.snapshot
                val chargedPower = shotState.power
                val shooterId = shotState.focusedPlayerId
                if (engine.shoot()) {
                    kickingPlayerId = shooterId
                    refresh()
                    scope.launch {
                        playerMotion.setPowerProgress(chargedPower)
                        playerMotion.setAimAngle(shotState.aimAngle)
                        playerMotion.releaseChargedKick()
                        if (kickingPlayerId == shooterId) kickingPlayerId = null
                    }
                }
            }

            LaunchedEffect(engine) {
                var previous = 0L
                while (true) {
                    withFrameNanos { now ->
                        if (previous != 0L && engine.snapshot.phase != GamePhase.AIMING) {
                            state = engine.step(((now - previous) / 1_000_000_000f).coerceAtMost(1f / 30f))
                        }
                        previous = now
                    }
                }
            }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ClassroomStadium(
                fieldWidthDp = fieldWidth,
                fieldHeightDp = fieldHeight,
            ) {
            Canvas(Modifier.fillMaxSize()) {
                val h = size.height
                val ball = state.ball.position
                val ballX = ball.x * density
                val ballY = ball.y * density
                val player = state.players.first { it.id == state.focusedPlayerId }
                val forward = if (player.team == GameTeam.RED) -1f else 1f
                val end = Offset(ballX + sin(state.aimAngle) * -forward * h * .29f, ballY + cos(state.aimAngle) * forward * h * .29f)
                if (state.phase == GamePhase.AIMING) {
                    drawLine(Color(0xFFE94F43).copy(alpha = .88f), Offset(ballX, ballY), end, 3.dp.toPx(), cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())))
                    drawCircle(Color(0xFFE94F43), 3.5.dp.toPx(), end)
                }
            }

            state.players.forEach { player ->
                val focused = player.id == state.focusedPlayerId
                val isKicking = player.id == kickingPlayerId
                val showFocus = focused && state.phase == GamePhase.AIMING
                val inputBlocked = state.phase != GamePhase.AIMING
                val playerSize = 76.dp
                val visualAimAngle = if (player.team == GameTeam.RED) state.aimAngle else -state.aimAngle
                val displayedX by animateDpAsState(
                    targetValue = player.position.x.dp,
                    animationSpec = spring(dampingRatio = .92f, stiffness = 1200f),
                )
                val displayedY by animateDpAsState(
                    targetValue = player.position.y.dp,
                    animationSpec = spring(dampingRatio = .92f, stiffness = 1200f),
                )
                val kickProgress = if (isKicking) playerMotion.kickProgress else 0f
                PaperPlayer(
                    modifier = Modifier.offset(
                        displayedX - playerSize / 2 + 8.dp * (sin(visualAimAngle) * kickProgress),
                        displayedY - playerSize * .80f - 20.dp - 20.dp * (cos(visualAimAngle) * kickProgress),
                    )
                        .size(playerSize)
                        .pointerInput(player.id, focused, inputBlocked) {
                            if (!focused && !inputBlocked) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    engine.moveInactivePlayer(player.id, Vector2(drag.x / density, drag.y / density))
                                    refresh()
                                }
                            } else if (focused && !inputBlocked) {
                                var mode = 0 // horizontal aim or downward power
                                var totalX = 0f
                                var totalY = 0f
                                detectDragGestures(
                                    onDragStart = { mode = 0; totalX = 0f; totalY = 0f },
                                    onDragEnd = { if (mode == 2) shoot() },
                                    onDragCancel = { if (mode == 2) shoot() },
                                ) { change, drag ->
                                    change.consume()
                                    totalX += drag.x; totalY += drag.y
                                    val wasUndecided = mode == 0
                                    if (wasUndecided) mode = when {
                                        kotlin.math.abs(totalX) > kotlin.math.abs(totalY) -> 1
                                        totalY > 0f && kotlin.math.abs(totalY) >= kotlin.math.abs(totalX) -> 2
                                        else -> 0
                                    }
                                    if (wasUndecided && mode == 2) scope.launch { playerMotion.startPowerCharge() }
                                    when (mode) {
                                        1 -> engine.aimBy(drag.x / density)
                                        2 -> {
                                            engine.chargeBy(drag.y / density)
                                            scope.launch { playerMotion.setPowerProgress(engine.snapshot.power) }
                                        }
                                    }
                                    refresh()
                                }
                            }
                        },
                    focusState = when {
                        showFocus -> PlayerFocusState.FOCUSED
                        isKicking -> PlayerFocusState.KICKING
                        else -> PlayerFocusState.INACTIVE
                    },
                    powerProgress = when {
                        showFocus -> state.power
                        isKicking -> playerMotion.powerProgress
                        else -> 0f
                    },
                    powerPressProgress = if (showFocus || isKicking) playerMotion.powerPressProgress else 0f,
                    aimAngle = if ((showFocus || isKicking) && player.team == GameTeam.RED) state.aimAngle
                        else if (showFocus || isKicking) -state.aimAngle else 0f,
                    kickProgress = kickProgress,
                    impactDirectionX = player.impactDirectionX,
                    impactDirectionY = player.impactDirectionY,
                    impactStrength = player.impactStrength,
                    jerseyColor = if (player.team == GameTeam.RED) Color(0xFFD94038) else Color(0xFF3269A8),
                    number = if (player.team == GameTeam.RED) player.id.removePrefix("red-").take(1).uppercase() else player.id.removePrefix("blue-").take(1).uppercase(),
                    name = if (showFocus || isKicking) "KICKER" else if (player.team == GameTeam.RED) "RED" else "BLUE",
                )
            }

            val ballSpeed = (state.ball.velocity.length() / 670f).coerceIn(0f, 1f)
            val ballLift = (state.ball.height / 36f).coerceIn(0f, 1f)
            val groundContact = (1f - state.ball.height / 5f).coerceIn(0f, 1f)
            val shadowWidth = 30.dp + 18.dp * ballLift
            val shadowHeight = 9.dp + 8.dp * ballLift
            Canvas(
                Modifier.offset(
                    state.ball.position.x.dp + 7.dp * ballLift - shadowWidth / 2,
                    state.ball.position.y.dp + 12.dp + 10.dp * ballLift - shadowHeight / 2,
                ).size(shadowWidth, shadowHeight),
            ) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF51483D).copy(alpha = .17f * groundContact + .045f * ballLift),
                            Color.Transparent,
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width * .52f,
                    ),
                )
                if (groundContact > .01f) {
                    val contactWidth = 20.dp.toPx()
                    val contactHeight = 4.dp.toPx()
                    drawOval(
                        color = Color(0xFF51483D).copy(alpha = .26f * groundContact),
                        topLeft = Offset((size.width - contactWidth) / 2f, (size.height - contactHeight) / 2f),
                        size = Size(contactWidth, contactHeight),
                    )
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                val velocity = state.ball.velocity
                val speed = velocity.length()
                if (speed > 65f) {
                    val direction = Offset(velocity.x / speed, velocity.y / speed)
                    val perpendicular = Offset(-direction.y, direction.x)
                    val airborne = state.ball.height >= 3f
                    val center = Offset(
                        state.ball.position.x * density,
                        state.ball.position.y * density,
                    )
                    val skid = ((speed - 65f) / 420f).coerceIn(0f, 1f)
                    val length = if (airborne) 12.dp.toPx() + 25.dp.toPx() * skid
                        else 10.dp.toPx() + 13.dp.toPx() * skid
                    repeat(3) { index ->
                        val side = (index - 1) * 2.4f.dp.toPx()
                        val start = center - direction * (7.dp.toPx() + index * 1.5f.dp.toPx()) + perpendicular * side
                        val end = center - direction * length + perpendicular * (side * 1.8f)
                        val trailAlpha = if (airborne) .10f + skid * .16f else .15f + skid * .27f
                        drawLine(
                            color = Color(0xFF796F61).copy(alpha = trailAlpha),
                            start = start,
                            end = end,
                            strokeWidth = (1f + skid * 1.5f - index * .25f).dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }

                val impact = state.ball.impactStrength.coerceIn(0f, 1f)
                if (impact > .025f && (state.ball.impactSideX != 0f || state.ball.impactSideY != 0f)) {
                    val center = Offset(
                        state.ball.position.x * density,
                        state.ball.position.y * density,
                    )
                    val contact = Offset(state.ball.impactSideX, state.ball.impactSideY)
                    val outgoing = Offset(-contact.x, -contact.y)
                    val tangent = Offset(-outgoing.y, outgoing.x)
                    val progress = 1f - impact
                    val contactPoint = center + contact * 9.dp.toPx()
                    repeat(3) { index ->
                        val size = (1.8f + index * .35f).dp.toPx()
                        val offset = (index - 1) * 2.3f.dp.toPx()
                        val fragment = contactPoint + outgoing * (3.dp.toPx() + progress * 12.dp.toPx()) + tangent * offset
                        val tip = fragment + outgoing * size
                        val wingA = fragment - outgoing * (size * .35f) + tangent * (size * .55f)
                        val wingB = fragment - outgoing * (size * .35f) - tangent * (size * .55f)
                        val flake = Color(0xFF9A836A).copy(alpha = impact * .78f)
                        drawLine(flake, wingA, tip, strokeWidth = .9.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(flake, wingB, tip, strokeWidth = .9.dp.toPx(), cap = StrokeCap.Round)
                    }
                }

                val landing = state.ball.landingStrength.coerceIn(0f, 1f)
                if (landing > .025f) {
                    val ground = Offset(state.ball.position.x * density, state.ball.position.y * density)
                    val directionLength = state.ball.velocity.length().coerceAtLeast(1f)
                    val direction = Offset(state.ball.velocity.x / directionLength, state.ball.velocity.y / directionLength)
                    val tangent = Offset(-direction.y, direction.x)
                    val spread = (3.dp.toPx() + (1f - landing) * 9.dp.toPx())
                    repeat(4) { index ->
                        val side = (index - 1.5f) * spread * .58f
                        val center = ground - direction * (4.dp.toPx() + index % 2 * 3.dp.toPx()) + tangent * side
                        drawOval(
                            color = Color(0xFF8A7963).copy(alpha = landing * .17f),
                            topLeft = Offset(center.x - 2.dp.toPx(), center.y - 1.dp.toPx()),
                            size = Size(4.dp.toPx(), 2.dp.toPx()),
                        )
                    }
                }
            }
            val ballStretch = (state.ball.velocity.length() / 700f).coerceIn(0f, .12f)
            val airborneScale = ballLift * .14f
            PaperBall(
                rotationX = state.ball.rotationX,
                rotationY = state.ball.rotationY,
                scaleX = 1f + ballStretch + airborneScale + .025f * groundContact,
                scaleY = 1f - ballStretch * .55f - ballLift * .08f - .055f * groundContact,
                impactSideX = state.ball.impactSideX,
                impactSideY = state.ball.impactSideY,
                impactStrength = state.ball.impactStrength,
                modifier = Modifier.offset(
                    state.ball.position.x.dp - 12.dp,
                    state.ball.position.y.dp - 12.dp,
                ).size(24.dp),
            )
            }

        val active = state.players.first { it.id == state.focusedPlayerId }
        val aimDegrees = (state.aimAngle * 180f / PI.toFloat()).toInt()
        Surface(Modifier.fillMaxWidth(), color = Color(0xFFECE5D9), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(when (state.phase) {
                    GamePhase.BALL_MOVING -> "BOLA BERGERAK"
                    GamePhase.KICKER_APPROACH -> "KICKER MENUJU BOLA"
                    GamePhase.AIMING -> "GILIRAN ${active.team.name} · ${aimDegrees}° AIM"
                },
                    color = Color(0xFF24314A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Slider(value = state.power, onValueChange = {
                    engine.setPower(it)
                    scope.launch { playerMotion.setPowerProgress(it) }
                    refresh()
                }, enabled = state.phase == GamePhase.AIMING)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (state.phase == GamePhase.AIMING) "Bidik geser · tarik ke bawah" else "Tunggu bola berhenti", color = Color(0xFF777A80), fontSize = 11.sp)
                    TextButton(enabled = state.phase == GamePhase.AIMING && state.power > .01f, onClick = { shoot() }) { Text("Tendang") }
                }
            }
        }
        }
        }
    }
}

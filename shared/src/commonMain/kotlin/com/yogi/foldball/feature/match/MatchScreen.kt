package com.yogi.foldball.feature.match

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
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
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogi.foldball.domain.game.engine.GameEngine
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameSnapshot
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchTimingConfig
import com.yogi.foldball.feature.match.components.MatchStatusPanel
import com.yogi.foldball.feature.match.components.OpeningTossOverlay
import com.yogi.foldball.feature.match.components.OpeningTossStatusPanel
import com.yogi.foldball.domain.game.engine.Vector2
import com.yogi.foldball.feature.match.components.PowerTimingMeter
import com.yogi.foldball.feature.match.components.MatchScoreboard
import com.yogi.foldball.feature.match.components.GoalCelebrationOverlay
import com.yogi.foldball.presentation.ball.PaperBall
import com.yogi.foldball.presentation.ball.MatchBallVisualSize
import com.yogi.foldball.presentation.field.ClassroomStadium
import com.yogi.foldball.presentation.player.PaperPlayer
import com.yogi.foldball.presentation.player.PlayerFacing
import com.yogi.foldball.presentation.player.PlayerFocusState
import com.yogi.foldball.presentation.player.rememberPlayerMotionState
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.home_desk_classroom
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val powerMeterCycleSeconds = 1.8f
private const val minimumTimingPower = .05f

@Composable
fun MatchSandboxScreen(timing: MatchTimingConfig = MatchTimingConfig()) =
    MatchExperienceScreen(fullMatch = false, timing = timing)

@Composable
fun FullMatchScreen(
    timing: MatchTimingConfig = MatchTimingConfig(),
    onOpenTools: (String) -> Unit,
) = MatchExperienceScreen(
    fullMatch = true,
    timing = timing,
    onOpenTools = onOpenTools,
)

@Composable
private fun MatchExperienceScreen(
    fullMatch: Boolean,
    timing: MatchTimingConfig,
    onOpenTools: (String) -> Unit = {},
) {
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val playerMotion = rememberPlayerMotionState()
    var isPaused by remember { mutableStateOf(false) }
    var isMatchFinished by remember { mutableStateOf(false) }
    var matchKey by remember { mutableIntStateOf(0) }
    var openingKickoffTeam by remember(matchKey) {
        mutableStateOf<GameTeam?>(if (fullMatch) null else GameTeam.RED)
    }
    var redScore by remember { mutableIntStateOf(0) }
    var blueScore by remember { mutableIntStateOf(0) }
    var matchElapsedSeconds by remember { mutableIntStateOf(0) }
    var showMatchMenu by remember { mutableStateOf(false) }
    var showShareSummary by remember { mutableStateOf(false) }
    LaunchedEffect(fullMatch, isPaused, isMatchFinished, openingKickoffTeam) {
        if (fullMatch && !isMatchFinished && openingKickoffTeam != null) {
            while (true) {
                delay(1_000)
                if (!isPaused) matchElapsedSeconds += 1
            }
        }
    }
    Column(
        if (fullMatch) Modifier.fillMaxSize()
            .paint(
                painter = painterResource(Res.drawable.home_desk_classroom),
                contentScale = ContentScale.Crop,
            )
            .padding(horizontal = 6.dp, vertical = 5.dp)
        else Modifier.fillMaxSize().background(Color(0xFFF5F0E6))
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (fullMatch) {
            MatchScoreboard(
                elapsedSeconds = matchElapsedSeconds,
                isPaused = isPaused,
                onMenuClick = { showMatchMenu = true },
                onShareClick = { showShareSummary = true },
                onPauseClick = { isPaused = !isPaused },
                blueScore = blueScore,
                redScore = redScore,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "FOLDBALL  /  MATCH MOTION",
                    color = Color(0xFF777A80),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    "Gerak formasi & bola",
                    color = Color(0xFF24314A),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Geser pemain · bidik · timing power · lihat pantulan",
                    color = Color(0xFF777A80),
                    fontSize = 13.sp
                )
            }
        }

        BoxWithConstraints(
            Modifier.fillMaxWidth().then(if (fullMatch) Modifier.weight(1f) else Modifier)
        ) {
            val fieldWidth = maxWidth.value
            val fieldHeight = if (fullMatch) {
                (maxHeight.value - 88f).coerceAtLeast(240f)
            } else {
                470f
            }
            val engine = remember(fieldWidth, fieldHeight, matchKey, openingKickoffTeam) {
                GameEngine(fieldWidth, fieldHeight,
                    targetScore = if (fullMatch) 3 else null,
                    timing = timing,
                    initialKickoffTeam = openingKickoffTeam ?: GameTeam.RED,
                )
            }
            var state by remember(engine) { mutableStateOf(engine.snapshot) }
            val goalShake = remember(engine) { Animatable(0f) }
            var kickingPlayerId by remember(engine) { mutableStateOf<String?>(null) }
            var movingPlayerId by remember(engine) { mutableStateOf<String?>(null) }
            var showAimGuide by remember(engine) { mutableStateOf(false) }
            var isPowerTiming by remember(engine) { mutableStateOf(false) }
            LaunchedEffect(state.phase) {
                if (state.phase == GamePhase.TURNOVER_RETREAT) {
                    isPowerTiming = false
                    showAimGuide = false
                    playerMotion.cancelPowerCharge()
                }
            }
            LaunchedEffect(engine, state.phase, state.lastScoringTeam) {
                if (state.phase == GamePhase.GOAL) {
                    goalShake.snapTo(0f)
                    goalShake.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            durationMillis = 520
                            .95f at 45
                            -.72f at 110
                            .54f at 185
                            -.34f at 270
                            .16f at 365
                            0f at 500
                        },
                    )
                } else {
                    goalShake.snapTo(0f)
                }
            }
            fun refresh() {
                state = engine.snapshot
            }

            fun shoot() {
                val shotState = engine.snapshot
                val chargedPower = shotState.power
                val shooterId = shotState.focusedPlayerId
                if (engine.shoot()) {
                    showAimGuide = false
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

            LaunchedEffect(engine, isPaused) {
                var previous = 0L
                while (true) {
                    withFrameNanos { now ->
                        if (!isPaused && openingKickoffTeam != null && previous != 0L &&
                            engine.snapshot.phase != GamePhase.MATCH_OVER) {
                            val nextState = engine.step(
                                (now - previous) / 1_000_000_000f
                            )
                            state = nextState
                            redScore = nextState.redScore
                            blueScore = nextState.blueScore
                            if (nextState.winner != null) isMatchFinished = true
                        }
                        previous = now
                    }
                }
            }

            LaunchedEffect(engine, isPowerTiming, isPaused) {
                if (isPowerTiming && !isPaused && engine.snapshot.phase == GamePhase.AIMING) {
                    var previous = 0L
                    var elapsedSeconds = 0f
                    while (isPowerTiming && !isPaused && engine.snapshot.phase == GamePhase.AIMING) {
                        val now = withFrameNanos { it }
                        if (previous != 0L) {
                            elapsedSeconds += (now - previous) / 1_000_000_000f
                        }
                        previous = now
                        val cyclePosition = (elapsedSeconds / powerMeterCycleSeconds) % 1f
                        val meterProgress = if (cyclePosition <= .5f) {
                            cyclePosition * 2f
                        } else {
                            (1f - cyclePosition) * 2f
                        }
                        val power = minimumTimingPower + (1f - minimumTimingPower) * meterProgress
                        engine.setPower(power)
                        playerMotion.setPowerProgress(power)
                        state = engine.snapshot
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().then(if (fullMatch) Modifier.fillMaxHeight() else Modifier),
                verticalArrangement = if (fullMatch) Arrangement.SpaceBetween else Arrangement.spacedBy(
                    14.dp
                ),
            ) {
                ClassroomStadium(
                    fieldWidthDp = fieldWidth,
                    fieldHeightDp = fieldHeight,
                    goalShakeOffset = goalShake.value,
                    shakeTopGoal = state.phase == GamePhase.GOAL && state.lastScoringTeam == GameTeam.RED,
                    shakeBottomGoal = state.phase == GamePhase.GOAL && state.lastScoringTeam == GameTeam.BLUE,
                ) {
                    if (fullMatch && openingKickoffTeam == null) {
                        OpeningTossOverlay(onWinner = { openingKickoffTeam = it })
                    } else {
                    Canvas(Modifier.fillMaxSize()) {
                        val h = size.height
                        if (state.phase == GamePhase.DEFENDING && movingPlayerId != null) {
                            val cellWidth = size.width / GameEngine.ZONE_COLUMNS
                            val cellHeight = size.height / GameEngine.ZONE_ROWS
                            val movingId = movingPlayerId!!
                            val fieldInset = 18.dp.toPx()
                            clipRect(
                                left = fieldInset,
                                top = fieldInset,
                                right = size.width - fieldInset,
                                bottom = size.height - fieldInset,
                            ) {
                                for (zone in 1..GameEngine.ZONE_COUNT) {
                                    val index = zone - 1
                                    val column = index % GameEngine.ZONE_COLUMNS
                                    val rowFromTop = GameEngine.ZONE_ROWS - 1 - index / GameEngine.ZONE_COLUMNS
                                    val topLeft = Offset(column * cellWidth, rowFromTop * cellHeight)
                                    val color = when {
                                        engine.isZoneAvailableForPlayer(movingId, zone) -> Color(0x2B3CA66B)
                                        engine.isZoneOccupied(zone, exceptPlayerId = movingId) -> Color(0x223E2723)
                                        else -> Color.Transparent
                                    }
                                    drawRect(color, topLeft, Size(cellWidth, cellHeight))
                                }
                            }
                        }
                        val ball = state.ball.position
                        val ballX = ball.x * density
                        val ballY = ball.y * density
                        val player = state.players.first { it.id == state.focusedPlayerId }
                        val forward = if (player.team == GameTeam.RED) -1f else 1f
                        val end = Offset(
                            ballX + sin(state.aimAngle) * -forward * h * .29f,
                            ballY + cos(state.aimAngle) * forward * h * .29f
                        )
                        if (state.phase == GamePhase.AIMING && showAimGuide) {
                            drawLine(
                                Color(0xFFE94F43).copy(alpha = .88f),
                                Offset(ballX, ballY),
                                end,
                                3.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(
                                        7.dp.toPx(),
                                        5.dp.toPx()
                                    )
                                )
                            )
                            drawCircle(Color(0xFFE94F43), 3.5.dp.toPx(), end)
                        }
                    }

                    state.players.forEach { player ->
                        val focused = player.id == state.focusedPlayerId
                        val activeTeam = state.players.first { it.id == state.focusedPlayerId }.team
                        val canMove = engine.canMovePlayer(player.id)
                        val isKicking = player.id == kickingPlayerId
                        val showFocus = focused && (state.phase == GamePhase.DEFENDING || state.phase == GamePhase.AIMING)
                        val playerSize = 76.dp
                        val visualAimAngle =
                            if (player.team == GameTeam.RED) state.aimAngle else -state.aimAngle
                        val displayedX by animateDpAsState(
                            targetValue = player.position.x.dp,
                            animationSpec = if (movingPlayerId == player.id) snap() else spring(dampingRatio = .92f, stiffness = 1200f),
                        )
                        val displayedY by animateDpAsState(
                            targetValue = player.position.y.dp,
                            animationSpec = if (movingPlayerId == player.id) snap() else spring(dampingRatio = .92f, stiffness = 1200f),
                        )
                        val kickProgress = if (isKicking) playerMotion.kickProgress else 0f
                        val kickDirectionY = if (player.team == GameTeam.RED) -1f else 1f
                        PaperPlayer(
                            modifier = Modifier.offset(
                                displayedX - playerSize / 2 + 8.dp * (sin(visualAimAngle) * kickProgress),
                                displayedY - playerSize / 2 + 20.dp * (kickDirectionY * cos(visualAimAngle) * kickProgress),
                            )
                                .size(playerSize)
                                .pointerInput(player.id, focused, state.phase, isPaused, activeTeam, canMove) {
                                    if (!focused && canMove && !isPaused && state.phase == GamePhase.DEFENDING && player.team != activeTeam) {
                                        detectDragGestures(
                                            onDragStart = {
                                                movingPlayerId = player.id
                                                engine.beginPlayerMove(player.id)
                                                refresh()
                                            },
                                            onDragEnd = {
                                                engine.endPlayerMove(player.id)
                                                movingPlayerId = null
                                                refresh()
                                            },
                                            onDragCancel = {
                                                engine.endPlayerMove(player.id)
                                                movingPlayerId = null
                                                refresh()
                                            },
                                        ) { change, drag ->
                                            change.consume()
                                            engine.moveInactivePlayer(player.id, Vector2(drag.x / density, drag.y / density))
                                            refresh()
                                        }
                                    } else if (focused && !isPaused && state.phase == GamePhase.AIMING) {
                                        detectDragGestures(
                                            onDragStart = {
                                                showAimGuide = true
                                                engine.setPower(minimumTimingPower)
                                                refresh()
                                                isPowerTiming = true
                                                scope.launch { playerMotion.startPowerCharge() }
                                            },
                                            onDragEnd = {
                                                isPowerTiming = false
                                                shoot()
                                            },
                                            onDragCancel = {
                                                isPowerTiming = false
                                                engine.setPower(0f)
                                                scope.launch { playerMotion.cancelPowerCharge() }
                                                refresh()
                                            },
                                        ) { change, drag ->
                                            change.consume()
                                            engine.aimBy(drag.x / density)
                                            refresh()
                                        }
                                    }
                                },
                            facing = if (player.team == GameTeam.RED) PlayerFacing.BACK else PlayerFacing.FRONT,
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
                            jerseyColor = if (player.team == GameTeam.RED) Color(0xFFD94038) else Color(
                                0xFF3269A8
                            ),
                            number = if (player.team == GameTeam.RED) player.id.removePrefix("red-")
                                .take(1).uppercase() else player.id.removePrefix("blue-").take(1)
                                .uppercase(),
                            name = if (showFocus || isKicking) "KICKER" else if (player.team == GameTeam.RED) "RED" else "BLUE",
                        )
                    }

                    val ballSpeed = (state.ball.velocity.length() / 670f).coerceIn(0f, 1f)
                    val ballLift = (state.ball.height / 36f).coerceIn(0f, 1f)
                    val groundContact = (1f - state.ball.height / 5f).coerceIn(0f, 1f)
                    val shadowWidth = 21.dp + 12.dp * ballLift
                    val shadowHeight = 6.dp + 5.dp * ballLift
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
                            val contactWidth = 14.dp.toPx()
                            val contactHeight = 2.5.dp.toPx()
                            drawOval(
                                color = Color(0xFF51483D).copy(alpha = .26f * groundContact),
                                topLeft = Offset(
                                    (size.width - contactWidth) / 2f,
                                    (size.height - contactHeight) / 2f
                                ),
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
                                val start =
                                    center - direction * (7.dp.toPx() + index * 1.5f.dp.toPx()) + perpendicular * side
                                val end =
                                    center - direction * length + perpendicular * (side * 1.8f)
                                val trailAlpha =
                                    if (airborne) .10f + skid * .16f else .15f + skid * .27f
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
                                val fragment =
                                    contactPoint + outgoing * (3.dp.toPx() + progress * 12.dp.toPx()) + tangent * offset
                                val tip = fragment + outgoing * size
                                val wingA =
                                    fragment - outgoing * (size * .35f) + tangent * (size * .55f)
                                val wingB =
                                    fragment - outgoing * (size * .35f) - tangent * (size * .55f)
                                val flake = Color(0xFF9A836A).copy(alpha = impact * .78f)
                                drawLine(
                                    flake,
                                    wingA,
                                    tip,
                                    strokeWidth = .9.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    flake,
                                    wingB,
                                    tip,
                                    strokeWidth = .9.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        val postImpact = state.ball.goalPostImpactStrength.coerceIn(0f, 1f)
                        if (postImpact > .025f) {
                            val center = Offset(
                                state.ball.position.x * density + state.ball.impactSideX * 7.dp.toPx(),
                                state.ball.position.y * density + state.ball.impactSideY * 7.dp.toPx(),
                            )
                            val expansion = (1f - postImpact) * 7.dp.toPx()
                            drawCircle(
                                color = Color(0xFFD6A568).copy(alpha = postImpact * .8f),
                                radius = 2.5.dp.toPx() + expansion,
                                center = center,
                                style = Stroke(width = (1f + postImpact).dp.toPx()),
                            )
                            repeat(5) { index ->
                                val angle = index * (2f * PI.toFloat() / 5f) +
                                    if (state.ball.impactSideX < 0f) PI.toFloat() else 0f
                                val direction = Offset(cos(angle), sin(angle))
                                val start = center + direction * (2.dp.toPx() + expansion)
                                val end = start + direction * (2.dp.toPx() + postImpact * 3.dp.toPx())
                                drawLine(
                                    color = Color(0xFFE8C38F).copy(alpha = postImpact * .9f),
                                    start = start,
                                    end = end,
                                    strokeWidth = 1.dp.toPx(),
                                    cap = StrokeCap.Round,
                                )
                            }
                        }

                        val landing = state.ball.landingStrength.coerceIn(0f, 1f)
                        if (landing > .025f) {
                            val ground = Offset(
                                state.ball.position.x * density,
                                state.ball.position.y * density
                            )
                            val directionLength = state.ball.velocity.length().coerceAtLeast(1f)
                            val direction = Offset(
                                state.ball.velocity.x / directionLength,
                                state.ball.velocity.y / directionLength
                            )
                            val tangent = Offset(-direction.y, direction.x)
                            val spread = (3.dp.toPx() + (1f - landing) * 9.dp.toPx())
                            repeat(4) { index ->
                                val side = (index - 1.5f) * spread * .58f
                                val center =
                                    ground - direction * (4.dp.toPx() + index % 2 * 3.dp.toPx()) + tangent * side
                                drawOval(
                                    color = Color(0xFF8A7963).copy(alpha = landing * .17f),
                                    topLeft = Offset(
                                        center.x - 2.dp.toPx(),
                                        center.y - 1.dp.toPx()
                                    ),
                                    size = Size(4.dp.toPx(), 2.dp.toPx()),
                                )
                            }
                        }
                    }
                    val ballStretch = (state.ball.velocity.length() / 700f).coerceIn(0f, .08f)
                    val airborneScale = ballLift * .08f
                    val ballVisualSize = MatchBallVisualSize
                    PaperBall(
                        rotationX = state.ball.rotationX,
                        rotationY = state.ball.rotationY,
                        scaleX = 1f + ballStretch + airborneScale + .015f * groundContact,
                        scaleY = 1f - ballStretch * .4f - ballLift * .05f - .035f * groundContact,
                        impactSideX = state.ball.impactSideX,
                        impactSideY = state.ball.impactSideY,
                        impactStrength = state.ball.impactStrength,
                        modifier = Modifier.offset(
                            state.ball.position.x.dp - ballVisualSize / 2,
                            state.ball.position.y.dp - ballVisualSize / 2,
                        ).size(ballVisualSize),
                    )

                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                        if (isPowerTiming) {
                            PowerTimingMeter(
                                power = state.power,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                        }
                    }
                    GoalCelebrationOverlay(
                        visible = state.phase == GamePhase.GOAL,
                        scoringTeam = state.lastScoringTeam,
                    )
                    }
                }

                if (fullMatch && openingKickoffTeam == null) {
                    OpeningTossStatusPanel()
                } else {
                    MatchStatusPanel(
                        snapshot = state,
                        defensiveMovesUsed = engine.defensiveMovesUsed,
                        canUndo = engine.canUndoPlayerMove(),
                        isPaused = isPaused,
                        showPower = !fullMatch,
                        onUndo = { engine.undoLastPlayerMove(); refresh() },
                        onFinishDefense = { engine.finishDefense(); refresh() },
                    )
                }
            }

            if (showMatchMenu) {
                AlertDialog(
                    onDismissRequest = { showMatchMenu = false },
                    title = { Text("MENU PERTANDINGAN") },
                    text = {
                        Column {
                            TextButton(onClick = {
                                showMatchMenu = false; onOpenTools("home")
                            }) { Text("Beranda") }
                            TextButton(onClick = {
                                showMatchMenu = false; onOpenTools("movement")
                            }) { Text("Movement Lab") }
                            TextButton(onClick = {
                                showMatchMenu = false; onOpenTools("ball")
                            }) { Text("Ball Lab") }
                            TextButton(onClick = {
                                showMatchMenu = false; onOpenTools("player")
                            }) { Text("Player Lab") }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showMatchMenu = false
                        }) { Text("Lanjut") }
                    },
                )
            }
            if (showShareSummary) {
                AlertDialog(
                    onDismissRequest = { showShareSummary = false },
                    title = { Text("RINGKASAN MATCH") },
                    text = {
                        Text(
                            "BIRU $blueScore – $redScore MERAH\n${matchElapsedSeconds / 60}:${
                                (matchElapsedSeconds % 60).toString().padStart(2, '0')
                            } · FIRST TO 3"
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showShareSummary = false
                        }) { Text("Tutup") }
                    },
                )
            }
            if (fullMatch && state.phase == GamePhase.MATCH_OVER) {
                val winningTeam = state.winner
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("PERTANDINGAN SELESAI") },
                    text = {
                        Text(
                            "TIM ${winningTeam?.name} MENANG\nBIRU ${state.blueScore} – ${state.redScore} MERAH"
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            matchKey += 1
                            isMatchFinished = false
                            isPaused = false
                            matchElapsedSeconds = 0
                            redScore = 0
                            blueScore = 0
                            showShareSummary = false
                        }) { Text("Main lagi") }
                    },
                    dismissButton = {
                        TextButton(onClick = { onOpenTools("home") }) { Text("Beranda") }
                    },
                )
            }
        }
    }
}

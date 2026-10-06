package com.yogi.foldball.feature.match.presentation.screen

import com.yogi.foldball.domain.game.bot.ShotPowerMeter
import com.yogi.foldball.domain.game.bot.BotStyle
import com.yogi.foldball.domain.game.bot.BotAimExecution
import com.yogi.foldball.core.audio.LocalGameAudio
import com.yogi.foldball.core.audio.SoundDispatcher
import com.yogi.foldball.core.ui.theme.PaperToggle

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.yogi.foldball.domain.game.engine.GoalGeometry
import com.yogi.foldball.domain.game.engine.TeamIdentity
import com.yogi.foldball.domain.game.engine.withRandomAiOpponent
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import com.yogi.foldball.feature.match.presentation.components.MatchStatusPanel
import com.yogi.foldball.feature.match.presentation.components.OpeningTossOverlay
import com.yogi.foldball.domain.game.engine.Vector2
import com.yogi.foldball.feature.match.presentation.components.PowerTimingMeter
import com.yogi.foldball.feature.match.presentation.components.MatchScoreboard
import com.yogi.foldball.feature.match.presentation.components.GoalCelebrationOverlay
import com.yogi.foldball.gamekit.presentation.ball.PaperBall
import com.yogi.foldball.gamekit.presentation.ball.MatchBallVisualSize
import com.yogi.foldball.gamekit.presentation.field.ClassroomStadium
import com.yogi.foldball.gamekit.presentation.player.PaperPlayer
import com.yogi.foldball.gamekit.presentation.player.PlayerFacing
import com.yogi.foldball.gamekit.presentation.player.PlayerFocusState
import com.yogi.foldball.gamekit.presentation.player.rememberPlayerMotionState
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballShapes
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.PaperButton
import com.yogi.foldball.core.ui.theme.PaperButtonStyle
import com.yogi.foldball.core.ui.theme.PaperChip
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.core.ui.theme.PaperDialog
import com.yogi.foldball.core.ui.theme.TapeLabel
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.home_desk_classroom
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.yogi.foldball.domain.game.bot.BotDifficulty
import com.yogi.foldball.domain.game.bot.BotPlanner
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

private const val minimumTimingPower = ShotPowerMeter.minimum
private const val formationOpponentAlpha = .68f

@Composable
fun MatchSandboxScreen(
    timing: MatchTimingConfig = MatchTimingConfig(),
    teamConfiguration: MatchTeamConfiguration = MatchTeamConfiguration.Default,
) = MatchExperienceScreen(fullMatch = false, timing = timing, baseTeamConfiguration = teamConfiguration)

@Composable
fun FullMatchScreen(
    playAgainstBot: Boolean = true,
    botDifficulty: BotDifficulty = BotDifficulty.MEDIUM,
    botStyle: BotStyle = BotStyle.BALANCED,
    timing: MatchTimingConfig = MatchTimingConfig(),
    teamConfiguration: MatchTeamConfiguration = MatchTeamConfiguration.Default,
    onOpenTools: (String) -> Unit,
) = MatchExperienceScreen(
    fullMatch = true,
    playAgainstBot = playAgainstBot,
    botDifficulty = botDifficulty,
    botStyle = botStyle,
    timing = timing,
    baseTeamConfiguration = teamConfiguration,
    onOpenTools = onOpenTools,
)

@Composable
private fun MatchExperienceScreen(
    fullMatch: Boolean,
    playAgainstBot: Boolean = false,
    botDifficulty: BotDifficulty = BotDifficulty.MEDIUM,
    botStyle: BotStyle = BotStyle.BALANCED,
    timing: MatchTimingConfig,
    baseTeamConfiguration: MatchTeamConfiguration,
    onOpenTools: (String) -> Unit = {},
) {
    val density = LocalDensity.current.density
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val playerMotion = rememberPlayerMotionState()
    val audio = LocalGameAudio.current
    var isPaused by remember { mutableStateOf(false) }
    var isMatchFinished by remember { mutableStateOf(false) }
    var matchKey by remember { mutableIntStateOf(0) }
    val teamConfiguration = remember(matchKey, fullMatch, playAgainstBot, baseTeamConfiguration) {
        when {
            fullMatch && playAgainstBot -> baseTeamConfiguration.copy(red = TeamIdentity("Player", "PLYR")).withRandomAiOpponent()
            fullMatch -> baseTeamConfiguration.copy(red = TeamIdentity("Player 1", "P1"), blue = TeamIdentity("Player 2", "P2"))
            else -> baseTeamConfiguration
        }
    }
    var lastWhistledKickoffScore by remember(matchKey) { mutableStateOf(-1) }
    var openingKickoffTeam by remember(matchKey) {
        mutableStateOf<GameTeam?>(if (fullMatch) null else GameTeam.RED)
    }
    var redScore by remember { mutableIntStateOf(0) }
    var blueScore by remember { mutableIntStateOf(0) }
    var matchElapsedSeconds by remember { mutableIntStateOf(0) }
    var phaseSeconds by remember { mutableIntStateOf(timing.defenseSeconds) }
    var activeTeam by remember { mutableStateOf(GameTeam.RED) }
    var showMatchMenu by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 6.dp, top = 5.dp, end = 6.dp, bottom = 16.dp)
        else Modifier.fillMaxSize().background(Color(0xFFF5F0E6))
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (fullMatch) {
            MatchScoreboard(
                isPaused = isPaused,
                onPauseClick = {
                    if (isPaused) {
                        isPaused = false
                        showMatchMenu = false
                    } else {
                        isPaused = true
                        showMatchMenu = true
                    }
                },
                phaseSeconds = phaseSeconds,
                activeTeam = activeTeam,
                teamConfiguration = teamConfiguration,
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
                    "Player and ball movement",
                    color = Color(0xFF24314A),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Move players · aim · time your shot · watch the bounce",
                    color = Color(0xFF777A80),
                    fontSize = 13.sp
                )
            }
        }

        BoxWithConstraints(
            Modifier.fillMaxWidth().then(if (fullMatch) Modifier.weight(1f) else Modifier)
        ) {
            val fieldWidth = maxWidth.value
            // Keep arena geometry stable through toss, defense, and aim phases.
            val fieldHeight = remember(fullMatch, fieldWidth) {
                if (fullMatch) (maxHeight.value - 88f).coerceAtLeast(240f) else 470f
            }
            val engine = remember(fieldWidth, fieldHeight, matchKey, openingKickoffTeam) {
                GameEngine(fieldWidth, fieldHeight,
                    targetScore = if (fullMatch) 3 else null,
                    timing = timing,
                    initialKickoffTeam = openingKickoffTeam ?: GameTeam.RED,
                )
            }
            var state by remember(engine) { mutableStateOf(engine.snapshot) }
            val soundDispatcher = remember(engine) { SoundDispatcher(state.soundEvents) }
            LaunchedEffect(engine, state.phase, openingKickoffTeam) {
                val score = state.redScore + state.blueScore
                if (fullMatch && openingKickoffTeam != null && state.phase == GamePhase.DEFENDING &&
                    score != lastWhistledKickoffScore) {
                    lastWhistledKickoffScore = score
                    audio?.play("kickoff", .5f)
                }
            }

            LaunchedEffect(engine, state.phase) {
                if (fullMatch && state.phase == GamePhase.MATCH_OVER) {
                    audio?.playMatchEnd(
                        lostAgainstAi = playAgainstBot && state.winner == GameTeam.BLUE,
                        wonAgainstAi = playAgainstBot && state.winner == GameTeam.RED,
                    )
                }
            }

            LaunchedEffect(engine, state.soundEvents, isPaused) {
                var goalPlayed = false
                soundDispatcher.dispatch(state, audioClock.elapsedNow().inWholeMilliseconds, !isPaused && audio?.enabled == true && audio.foreground) { name, volume ->
                    audio?.play(name, volume)
                    if (name == "goal") goalPlayed = true
                }
                if (isPaused) audio?.stop()
                if (goalPlayed) audio?.playGoalCheer()
            }
            DisposableEffect(engine) { onDispose { audio?.stop() } }

            val bot = remember(engine, botDifficulty, botStyle) { BotPlanner(botDifficulty, style = botStyle) }
            val botTurn = fullMatch && playAgainstBot && bot.controls(state, GameTeam.BLUE)
            val formationOpen = state.phase == GamePhase.DEFENDING && state.phaseTimeRemaining > 0f
            var previousHapticPhase by remember(engine) { mutableStateOf(state.phase) }
            val goalShake = remember(engine) { Animatable(0f) }
            var kickingPlayerId by remember(engine) { mutableStateOf<String?>(null) }
            var movingPlayerId by remember(engine) { mutableStateOf<String?>(null) }
            var movingPlayerOrigin by remember(engine) { mutableStateOf<Vector2?>(null) }
            var selectedFormationPlayerId by remember(engine) { mutableStateOf<String?>(null) }
            var showAimGuide by remember(engine) { mutableStateOf(false) }
            var isPowerTiming by remember(engine) { mutableStateOf(false) }
            var powerTimingElapsed by remember(engine) { mutableFloatStateOf(0f) }
            LaunchedEffect(state.phase, formationOpen) {
                val oldPhase = previousHapticPhase
                if (oldPhase != state.phase) {
                    when (state.phase) {
                        GamePhase.DEFENDING, GamePhase.AIMING ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        GamePhase.BALL_MOVING -> if (oldPhase == GamePhase.AIMING) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        GamePhase.GOAL ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        else -> Unit
                    }
                    if (state.phase == GamePhase.BALL_MOVING && oldPhase == GamePhase.AIMING &&
                        kickingPlayerId != state.focusedPlayerId
                    ) {
                        val shooterId = state.focusedPlayerId
                        kickingPlayerId = shooterId
                        playerMotion.setPowerProgress(state.lastShotPower)
                        playerMotion.setAimAngle(state.aimAngle)
                        playerMotion.releaseChargedKick()
                        if (kickingPlayerId == shooterId) kickingPlayerId = null
                    }
                }
                previousHapticPhase = state.phase
                if (!formationOpen) {
                    selectedFormationPlayerId = null
                    movingPlayerId = null
                    movingPlayerOrigin = null
                }
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

            fun finishPlayerMove(playerId: String) {
                val movesBefore = engine.defensiveMovesUsed
                engine.endPlayerMove(playerId)
                if (engine.defensiveMovesUsed > movesBefore) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                            phaseSeconds = ceil(nextState.phaseTimeRemaining).toInt().coerceAtLeast(0)
                            val possessionTeam = nextState.players.first { it.id == nextState.focusedPlayerId }.team
                            activeTeam = when (nextState.phase) {
                                GamePhase.DEFENDING -> if (possessionTeam == GameTeam.RED) GameTeam.BLUE else GameTeam.RED
                                else -> possessionTeam
                            }
                            if (nextState.winner != null) isMatchFinished = true
                        }
                        previous = now
                    }
                }
            }

            LaunchedEffect(engine, bot, playAgainstBot, state.phase, state.focusedPlayerId, isPaused, openingKickoffTeam) {
                if (!fullMatch || !playAgainstBot || isPaused || openingKickoffTeam == null || !botTurn) return@LaunchedEffect
                val thinkMillis = kotlin.random.Random.nextLong(1100, 2600)
                delay(minOf(thinkMillis, ((engine.snapshot.phaseTimeRemaining - 2.2f) * 1000).toLong().coerceAtLeast(0L)))
                if (engine.snapshot.phase == GamePhase.DEFENDING) {
                    val planningEngine = engine.copyForSimulation()
                    val moves = withContext(Dispatchers.Default) {
                        bot.chooseDefensePlan(planningEngine, GameTeam.BLUE)
                    }
                    val plannedMoves = if (botDifficulty != BotDifficulty.HARD && moves.size > 1 && kotlin.random.Random.nextFloat() < .2f) moves.take(1) else moves
                    for (move in plannedMoves) {
                        if (engine.snapshot.phase != GamePhase.DEFENDING) break
                        engine.movePlayerToZone(move.playerId, move.zone)
                        refresh()
                        delay(minOf(kotlin.random.Random.nextLong(400, 850), ((engine.snapshot.phaseTimeRemaining - .3f) * 1000).toLong().coerceAtLeast(0L)))
                    }
                    engine.finishDefense()
                    refresh()
                } else if (engine.snapshot.phase == GamePhase.AIMING) {
                    val planningEngine = engine.copyForSimulation()
                    val shot = withContext(Dispatchers.Default) { bot.chooseShot(planningEngine) }
                    if (engine.snapshot.phase == GamePhase.AIMING && bot.controls(engine.snapshot, GameTeam.BLUE)) {
                        val target = BotAimExecution.target(shot, botDifficulty)
                        isPowerTiming = false
                        val startingAngle = engine.snapshot.aimAngle
                        val consideredAngle = bot.alternativeShot?.angle?.takeIf { kotlin.random.Random.nextFloat() < .25f }
                        val overshoot = if (kotlin.random.Random.nextBoolean()) .025f else -.025f
                        val aimSeconds = minOf(1.05f, (engine.snapshot.phaseTimeRemaining - 1.1f).coerceAtLeast(.05f))
                        var elapsed = 0f
                        var previousFrame = withFrameNanos { it }
                        while (elapsed < aimSeconds && engine.currentPhase == GamePhase.AIMING) {
                            val now = withFrameNanos { it }
                            elapsed += (now - previousFrame) / 1_000_000_000f
                            previousFrame = now
                            val progress = (elapsed / aimSeconds).coerceIn(0f, 1f)
                            val smooth = progress * progress * (3f - 2f * progress)
                            val hesitation = kotlin.math.sin(progress * kotlin.math.PI.toFloat()) * overshoot
                            val aimedAngle = if (consideredAngle != null && progress < .3f) {
                                startingAngle + (consideredAngle - startingAngle) * (progress / .3f)
                            } else if (consideredAngle != null) {
                                val correction = ((progress - .3f) / .7f).coerceIn(0f, 1f)
                                consideredAngle + (target.angle - consideredAngle) * correction * correction * (3f - 2f * correction)
                            } else startingAngle + (target.angle - startingAngle) * smooth
                            engine.setAimAngle(aimedAngle + hesitation)
                            playerMotion.setAimAngle(engine.snapshot.aimAngle)
                            refresh()
                        }
                        if (engine.currentPhase == GamePhase.AIMING) {
                            engine.setPower(minimumTimingPower)
                            refresh()
                            powerTimingElapsed = 0f
                            val releaseAt = BotAimExecution.releaseTime(target.power, botDifficulty)
                            isPowerTiming = true
                            try {
                                playerMotion.startPowerCharge()
                                while (engine.currentPhase == GamePhase.AIMING &&
                                    powerTimingElapsed < releaseAt && engine.snapshot.phaseTimeRemaining > .12f) {
                                    withFrameNanos { }
                                }
                            } finally {
                                isPowerTiming = false
                            }
                            if (engine.snapshot.focusedPlayerId == planningEngine.snapshot.focusedPlayerId &&
                                engine.currentPhase == GamePhase.AIMING) shoot()
                        }
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
                        powerTimingElapsed = elapsedSeconds
                        val power = ShotPowerMeter.powerAt(elapsedSeconds)
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
                val possessionTeam = state.players.first { it.id == state.focusedPlayerId }.team
                val turnTeam = if (state.phase == GamePhase.DEFENDING) {
                    if (possessionTeam == GameTeam.RED) GameTeam.BLUE else GameTeam.RED
                } else possessionTeam
                val localTwoPlayers = fullMatch && !playAgainstBot
                val turnName = teamConfiguration.forTeam(turnTeam).name
                val turnLabel = when {
                    localTwoPlayers && state.phase == GamePhase.DEFENDING ->
                        "$turnName TURN · DEFEND" + if (state.openingKickoffPending) " · OWN HALF" else ""
                    localTwoPlayers && state.phase == GamePhase.AIMING -> "$turnName TURN · KICK"
                    localTwoPlayers && state.phase == GamePhase.KICKER_APPROACH -> "UP NEXT · $turnName"
                    else -> when (state.phase) {
                    GamePhase.DEFENDING -> if (state.openingKickoffPending) {
                        "FORMATION · ${teamConfiguration.forTeam(turnTeam).name} · OWN HALF"
                    } else {
                        "SET FORMATION · ${teamConfiguration.forTeam(turnTeam).name} · TAP PLAYER"
                    }
                    GamePhase.AIMING -> "KICK · ${teamConfiguration.forTeam(turnTeam).name}"
                    GamePhase.KICKER_APPROACH -> "KICK NEXT · ${teamConfiguration.forTeam(turnTeam).name}"
                    GamePhase.BALL_MOVING -> "SHOT IN PLAY · ${teamConfiguration.forTeam(possessionTeam).name}"
                    GamePhase.TURNOVER_RETREAT -> "TURNOVER · ${teamConfiguration.forTeam(possessionTeam).name}"
                    GamePhase.GOAL -> "GOAL · ${state.lastScoringTeam?.let { teamConfiguration.forTeam(it).name }.orEmpty()}"
                    GamePhase.MATCH_OVER -> "MATCH COMPLETE"
                    }
                }
                if (!(fullMatch && openingKickoffTeam == null)) {
                    TapeLabel(turnLabel, Modifier.align(Alignment.CenterHorizontally)
                        .semantics { liveRegion = LiveRegionMode.Polite })
                }
                ClassroomStadium(
                    fieldWidthDp = fieldWidth,
                    fieldHeightDp = fieldHeight,
                    goalShakeOffset = goalShake.value,
                    shakeTopGoal = state.phase == GamePhase.GOAL && state.lastScoringTeam == GameTeam.RED,
                    shakeBottomGoal = state.phase == GamePhase.GOAL && state.lastScoringTeam == GameTeam.BLUE,
                ) {
                    if (fullMatch && openingKickoffTeam == null) {
                        OpeningTossOverlay(
                            teamConfiguration = teamConfiguration,
                            onWinner = { openingKickoffTeam = it },
                        )
                    } else {
                    Canvas(Modifier.fillMaxSize()) {
                        val h = size.height
                        if (formationOpen) {
                            val cellWidth = size.width / GameEngine.ZONE_COLUMNS
                            val cellHeight = size.height / GameEngine.ZONE_ROWS
                            fun zoneAt(position: Vector2): Int {
                                val column = (position.x / (fieldWidth / GameEngine.ZONE_COLUMNS))
                                    .toInt().coerceIn(0, GameEngine.ZONE_COLUMNS - 1)
                                val rowFromTop = (position.y / (fieldHeight / GameEngine.ZONE_ROWS))
                                    .toInt().coerceIn(0, GameEngine.ZONE_ROWS - 1)
                                val rowFromBottom = GameEngine.ZONE_ROWS - 1 - rowFromTop
                                return rowFromBottom * GameEngine.ZONE_COLUMNS + column + 1
                            }
                            val selectedMoveId = movingPlayerId ?: selectedFormationPlayerId
                                ?.takeIf(engine::canMovePlayer)
                            val selectedPlayer = state.players.firstOrNull { it.id == selectedMoveId }
                            val originPosition = movingPlayerOrigin ?: selectedPlayer?.position
                            val originZones = originPosition?.let { setOf(zoneAt(it)) }.orEmpty()
                            val legalDestinations = if (selectedMoveId == null) emptyMap() else
                                (1..GameEngine.ZONE_COUNT).associateWith { zone ->
                                    if (zone in originZones) null
                                    else engine.legalPlayerPositionInZone(selectedMoveId, zone)
                                }
                            val adjacentZones = if (selectedMoveId == null) emptySet() else
                                (1..GameEngine.ZONE_COUNT).filterTo(mutableSetOf()) { zone ->
                                    engine.isAdjacentZoneForPlayer(selectedMoveId, zone)
                                }
                            val movementDash = PathEffect.dashPathEffect(
                                floatArrayOf(7.dp.toPx(), 5.dp.toPx()),
                            )
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
                                    val available = legalDestinations[zone] != null
                                    if (available || zone in adjacentZones) {
                                        drawRect(
                                            color = if (available) {
                                                selectedPlayer?.let { player ->
                                                    if (player.team == GameTeam.RED) FoldballColor.TeamRed
                                                    else FoldballColor.InkBlue
                                                }?.copy(alpha = .12f) ?: FoldballColor.BallHighlight.copy(alpha = .12f)
                                            } else {
                                                Color(0xFFB8BCC1).copy(alpha = .10f)
                                            },
                                            topLeft = topLeft,
                                            size = Size(cellWidth, cellHeight),
                                        )
                                        drawRect(
                                            color = if (available) FoldballColor.BallHighlight.copy(alpha = .72f)
                                            else Color(0xFFB8BCC1).copy(alpha = .42f),
                                            topLeft,
                                            Size(cellWidth, cellHeight),
                                            style = Stroke(
                                                width = if (available) 1.1.dp.toPx() else .8.dp.toPx(),
                                                pathEffect = if (available) movementDash else null,
                                            ),
                                        )
                                    }
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
                        val canMove = !botTurn && engine.canMovePlayer(player.id)
                        val isKicking = player.id == kickingPlayerId
                        val showFocus = focused && (
                            state.phase == GamePhase.KICKER_APPROACH || state.phase == GamePhase.AIMING
                        )
                        val showFormation = formationOpen && player.team == turnTeam && canMove
                        val playerSize = minOf(
                            76f,
                            fieldHeight / GameEngine.ZONE_ROWS * 1.5f,
                        ).dp
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
                        // FRONT's lower contact edge is inset more than BACK's upper fold.
                        // Align their visible kicking gap without moving engine positions/hitboxes.
                        val kickerVisualOffsetY = if (player.team == GameTeam.BLUE && ((focused &&
                            state.phase in listOf(GamePhase.KICKER_APPROACH, GamePhase.DEFENDING, GamePhase.AIMING)) || isKicking)) playerSize * .086f else 0.dp
                        val kickProgress = if (isKicking) playerMotion.kickProgress else 0f
                        val kickDirectionY = if (player.team == GameTeam.RED) -1f else 1f
                        val targetPlayerAlpha = when (state.phase) {
                            GamePhase.DEFENDING -> if (player.team == turnTeam) 1f else formationOpponentAlpha
                            else -> 1f
                        }
                        val playerAlpha by animateFloatAsState(
                            targetValue = targetPlayerAlpha,
                            animationSpec = tween(durationMillis = 360),
                            label = "${player.team.name.lowercase()}-turn-alpha",
                        )
                        PaperPlayer(
                            modifier = Modifier.offset(
                                displayedX - playerSize / 2 + 8.dp * (sin(visualAimAngle) * kickProgress),
                                displayedY - playerSize / 2 + kickerVisualOffsetY + 20.dp * (kickDirectionY * cos(visualAimAngle) * kickProgress),
                            )
                                .size(playerSize)
                                .graphicsLayer { alpha = playerAlpha }
                                .clickable(
                                    enabled = !focused && canMove && !isPaused &&
                                        formationOpen && player.team == turnTeam,
                                    onClick = { selectedFormationPlayerId = player.id },
                                )
                                .pointerInput(player.id, focused, formationOpen, isPaused, turnTeam, canMove, botTurn) {
                                    if (!focused && canMove && !isPaused && formationOpen && player.team == turnTeam) {
                                        detectDragGestures(
                                            onDragStart = {
                                                selectedFormationPlayerId = player.id
                                                movingPlayerId = player.id
                                                movingPlayerOrigin = player.position
                                                engine.beginPlayerMove(player.id)
                                                refresh()
                                            },
                                            onDragEnd = {
                                                finishPlayerMove(player.id)
                                                movingPlayerId = null
                                                movingPlayerOrigin = null
                                                if (!engine.canMovePlayer(player.id)) {
                                                    selectedFormationPlayerId = null
                                                }
                                                refresh()
                                            },
                                            onDragCancel = {
                                                finishPlayerMove(player.id)
                                                movingPlayerId = null
                                                movingPlayerOrigin = null
                                                if (!engine.canMovePlayer(player.id)) {
                                                    selectedFormationPlayerId = null
                                                }
                                                refresh()
                                            },
                                        ) { change, drag ->
                                            change.consume()
                                            engine.moveInactivePlayer(player.id, Vector2(drag.x / density, drag.y / density))
                                            refresh()
                                        }
                                    } else if (focused && !isPaused && !botTurn && state.phase == GamePhase.AIMING) {
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
                                showFormation -> PlayerFocusState.FORMATION
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
                            inactiveAlpha = 1f,
                            jerseyColor = if (player.team == GameTeam.RED) FoldballColor.TeamRed else FoldballColor.InkBlue,
                            number = when (player.id.substringAfter("-")) {
                                "center" -> "1"
                                "left" -> "2"
                                "right" -> "3"
                                else -> "9"
                            },
                            name = teamConfiguration.forTeam(player.team).jerseyLabel,
                        )
                    }

                    val ballSpeed = (state.ball.velocity.length() / 670f).coerceIn(0f, 1f)
                    val ballLift = (state.ball.height / GoalGeometry.openingHeight).coerceIn(0f, 1f)
                    val groundContact = (1f - state.ball.height / 5f).coerceIn(0f, 1f)
                    val shadowWidth = 15.dp + 15.dp * ballLift
                    val shadowHeight = 3.dp + 4.dp * ballLift
                    Canvas(
                        Modifier.offset(
                            state.ball.position.x.dp + 4.dp * ballLift - shadowWidth / 2,
                            state.ball.position.y.dp + MatchBallVisualSize / 2 + 1.dp + 8.dp * ballLift - shadowHeight / 2,
                        ).size(shadowWidth, shadowHeight),
                    ) {
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    FoldballColor.ShadowBrown.copy(alpha = .20f * groundContact + .035f * ballLift),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width * .52f,
                            ),
                        )
                        if (groundContact > .01f) {
                            val contactWidth = 10.dp.toPx()
                            val contactHeight = 1.8.dp.toPx()
                            drawOval(
                                color = FoldballColor.ShadowBrown.copy(alpha = .22f * groundContact),
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
                    val visualBallLift = GoalGeometry.visualLift(state.ball.height).dp
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
                            state.ball.position.y.dp - ballVisualSize / 2 - visualBallLift,
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
                        teamConfiguration = teamConfiguration,
                    )
                    }
                }

                if (!(fullMatch && openingKickoffTeam == null)) {
                    MatchStatusPanel(
                        snapshot = state,
                        defensiveMovesUsed = engine.defensiveMovesUsed,
                        canUndo = !botTurn && engine.canUndoPlayerMove(),
                        isBotTurn = botTurn,
                        localTwoPlayers = fullMatch && !playAgainstBot,
                        isPaused = isPaused,
                        showPower = !fullMatch,
                        teamConfiguration = teamConfiguration,
                        onUndo = { engine.undoLastPlayerMove(); refresh() },
                        onFinishDefense = { engine.finishDefense(); refresh() },
                        compactMatchBar = fullMatch,
                    )
                }
            }

            if (showMatchMenu) {
                Dialog(onDismissRequest = { showMatchMenu = false; isPaused = false }) {
                    PaperCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = FoldballShapes.Banner,
                        tilt = FoldballTilt.MatchMenu,
                        ruled = false,
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            audio?.let { PaperToggle("Sound effects", it.enabled, it::changeEnabled, Modifier.fillMaxWidth()) }
                            Text("MATCH PAUSED", style = FoldballType.Title, color = FoldballColor.Ink)
                            PaperButton("Resume", { showMatchMenu = false; isPaused = false }, Modifier.fillMaxWidth())
                            PaperButton("Match summary", { showMatchMenu = false; showShareSummary = true; isPaused = false }, Modifier.fillMaxWidth(), style = PaperButtonStyle.Secondary)
                            PaperButton("Home", { showMatchMenu = false; showExitConfirmation = true }, Modifier.fillMaxWidth(), style = PaperButtonStyle.Danger)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PaperChip("Move", { isPaused = false; showMatchMenu = false; onOpenTools("movement") }, "Open Movement Lab", Modifier.weight(1f))
                                PaperChip("Ball", { isPaused = false; showMatchMenu = false; onOpenTools("ball") }, "Open Ball Lab", Modifier.weight(1f))
                                PaperChip("Player", { isPaused = false; showMatchMenu = false; onOpenTools("player") }, "Open Player Lab", Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            if (showExitConfirmation) {
                PaperDialog(
                    title = "RETURN HOME?",
                    message = "The current match will end. Are you sure you want to leave?",
                    onDismiss = { showExitConfirmation = false; isPaused = false },
                    confirmText = "Leave match",
                    onConfirm = {
                        showExitConfirmation = false
                        onOpenTools("home")
                    },
                    dismissText = "Keep playing",
                    stackedActions = true,
                    danger = true,
                )
            }
            if (showShareSummary) {
                PaperDialog(
                    title = "MATCH SUMMARY",
                    message = "${teamConfiguration.forTeam(GameTeam.RED).name} $redScore – $blueScore ${teamConfiguration.forTeam(GameTeam.BLUE).name}\n${matchElapsedSeconds / 60}:${
                        (matchElapsedSeconds % 60).toString().padStart(2, '0')
                    } · FIRST TO 3",
                    onDismiss = { showShareSummary = false },
                    confirmText = "Close",
                    onConfirm = { showShareSummary = false },
                    dismissText = null,
                )
            }
            if (fullMatch && state.phase == GamePhase.MATCH_OVER) {
                val winningTeam = state.winner
                PaperDialog(
                    title = "MATCH COMPLETE",
                    message = "TEAM ${winningTeam?.let { teamConfiguration.forTeam(it).name }} WINS\n${teamConfiguration.forTeam(GameTeam.RED).name} ${state.redScore} – ${state.blueScore} ${teamConfiguration.forTeam(GameTeam.BLUE).name}",
                    confirmText = "Play again",
                    onConfirm = {
                            matchKey += 1
                            isMatchFinished = false
                            isPaused = false
                            matchElapsedSeconds = 0
                            redScore = 0
                            blueScore = 0
                            showShareSummary = false
                    },
                    dismissText = "Home",
                    onDismiss = { onOpenTools("home") },
                )
            }
        }
    }
}

private val audioClock = kotlin.time.TimeSource.Monotonic.markNow()

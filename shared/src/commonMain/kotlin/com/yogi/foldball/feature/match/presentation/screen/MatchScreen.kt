package com.yogi.foldball.feature.match.presentation.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.yogi.foldball.core.audio.CountdownCue
import com.yogi.foldball.core.audio.LocalGameAudio
import com.yogi.foldball.core.audio.SoundDispatcher
import com.yogi.foldball.core.audio.TurnCue
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballShapes
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.PaperButton
import com.yogi.foldball.core.ui.theme.PaperButtonStyle
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.core.ui.theme.PaperDialog
import com.yogi.foldball.core.ui.theme.PaperToggle
import com.yogi.foldball.core.ui.theme.TapeLabel
import com.yogi.foldball.domain.game.bot.BotDifficulty
import com.yogi.foldball.domain.game.bot.BotStyle
import com.yogi.foldball.domain.game.bot.ShotPowerMeter
import com.yogi.foldball.domain.game.engine.AimDirection
import com.yogi.foldball.domain.game.engine.GameEngine
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.GoalGeometry
import com.yogi.foldball.domain.game.engine.MatchFormat
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import com.yogi.foldball.domain.game.engine.MatchTimingConfig
import com.yogi.foldball.domain.game.engine.Vector2
import com.yogi.foldball.domain.team.BallStyle
import com.yogi.foldball.domain.team.JerseyPattern
import com.yogi.foldball.feature.match.presentation.components.GoalCelebrationOverlay
import com.yogi.foldball.feature.match.presentation.components.MatchScoreboard
import com.yogi.foldball.feature.match.presentation.components.MatchStatusPanel
import com.yogi.foldball.feature.match.presentation.components.OpeningTossOverlay
import com.yogi.foldball.feature.match.presentation.components.PowerTimingMeter
import com.yogi.foldball.feature.match.presentation.components.TurnTimer
import com.yogi.foldball.feature.match.presentation.viewmodel.MatchIntent
import com.yogi.foldball.feature.match.presentation.viewmodel.MatchOptions
import com.yogi.foldball.feature.match.presentation.viewmodel.MatchViewModel
import com.yogi.foldball.gamekit.presentation.ball.MatchBallVisualSize
import com.yogi.foldball.gamekit.presentation.ball.PaperBall
import com.yogi.foldball.gamekit.presentation.field.ClassroomStadium
import com.yogi.foldball.gamekit.presentation.field.drawPaperAimGuide
import com.yogi.foldball.gamekit.presentation.player.PaperPlayer
import com.yogi.foldball.gamekit.presentation.player.PlayerFacing
import com.yogi.foldball.gamekit.presentation.player.PlayerFocusState
import com.yogi.foldball.gamekit.presentation.player.StandeeConditionBar
import com.yogi.foldball.gamekit.presentation.player.rememberPlayerMotionState
import com.yogi.foldball.gamekit.presentation.team.ClubKit
import com.yogi.foldball.gamekit.presentation.team.kitColor
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.home_desk_classroom
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val MINIMUM_TIMING_POWER = ShotPowerMeter.minimum

@Composable
fun MatchSandboxScreen(
    timing: MatchTimingConfig = MatchTimingConfig(),
    teamConfiguration: MatchTeamConfiguration = MatchTeamConfiguration.Default
) = MatchExperienceScreen(
    fullMatch = false,
    timing = timing,
    baseTeamConfiguration = teamConfiguration
)

@Composable
fun FullMatchScreen(
    playAgainstBot: Boolean = true,
    botDifficulty: BotDifficulty = BotDifficulty.MEDIUM,
    botStyle: BotStyle = BotStyle.BALANCED,
    matchFormat: MatchFormat = MatchFormat.THREE_V_THREE,
    ballStyle: BallStyle = BallStyle.CLASSIC,
    standeeDamage: Boolean = true,
    timing: MatchTimingConfig = MatchTimingConfig(),
    teamConfiguration: MatchTeamConfiguration = MatchTeamConfiguration.Default,
    onOpenTools: (String) -> Unit
) = MatchExperienceScreen(
    fullMatch = true,
    playAgainstBot = playAgainstBot,
    botDifficulty = botDifficulty,
    botStyle = botStyle,
    matchFormat = matchFormat,
    ballStyle = ballStyle,
    standeeDamage = standeeDamage,
    timing = timing,
    baseTeamConfiguration = teamConfiguration,
    onOpenTools = onOpenTools
)

@Composable
private fun MatchExperienceScreen(
    fullMatch: Boolean,
    playAgainstBot: Boolean = false,
    botDifficulty: BotDifficulty = BotDifficulty.MEDIUM,
    botStyle: BotStyle = BotStyle.BALANCED,
    matchFormat: MatchFormat = MatchFormat.THREE_V_THREE,
    ballStyle: BallStyle = BallStyle.CLASSIC,
    standeeDamage: Boolean = false,
    timing: MatchTimingConfig,
    baseTeamConfiguration: MatchTeamConfiguration,
    onOpenTools: (String) -> Unit = {}
) {
    val density = LocalDensity.current.density
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val playerMotion = rememberPlayerMotionState()
    val audio = LocalGameAudio.current
    val viewModel: MatchViewModel = koinViewModel(
        key = "match:$fullMatch:$playAgainstBot:$botDifficulty:$botStyle:$matchFormat:$standeeDamage:$timing",
        parameters = {
            parametersOf(
                MatchOptions(
                    fullMatch,
                    playAgainstBot,
                    botDifficulty,
                    botStyle,
                    standeeDamage,
                    timing,
                    baseTeamConfiguration,
                    matchFormat
                )
            )
        }
    )
    val matchState by viewModel.uiState.collectAsStateWithLifecycle()
    val dentingEnabled = matchState.dentingEnabled
    val isPaused = matchState.paused
    val matchKey = matchState.sessionId
    val teamConfiguration = matchState.teams
    val openingKickoffTeam = matchState.openingKickoffTeam
    val redScore = matchState.snapshot?.redScore ?: 0
    val blueScore = matchState.snapshot?.blueScore ?: 0
    val matchElapsedSeconds = matchState.elapsedSeconds.toInt()
    val phaseSeconds = matchState.phaseSeconds
    var showMatchMenu by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var showShareSummary by remember { mutableStateOf(false) }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = fullMatch && !showMatchMenu && !showExitConfirmation && !showShareSummary,
        onBackCompleted = {
            viewModel.onIntent(MatchIntent.SetPaused(true))
            showExitConfirmation = true
        }
    )
    Column(
        if (fullMatch) {
            Modifier.fillMaxSize()
                .paint(
                    painter = painterResource(Res.drawable.home_desk_classroom),
                    contentScale = ContentScale.Crop
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 6.dp, top = 5.dp, end = 6.dp, bottom = 16.dp)
        } else {
            Modifier.fillMaxSize().background(FoldballColor.Paper)
                .verticalScroll(rememberScrollState()).padding(16.dp)
        },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (fullMatch) {
            MatchScoreboard(
                isPaused = isPaused,
                onPauseClick = {
                    if (isPaused) {
                        viewModel.onIntent(MatchIntent.SetPaused(false))
                        showMatchMenu = false
                    } else {
                        viewModel.onIntent(MatchIntent.SetPaused(true))
                        showMatchMenu = true
                    }
                },
                matchSecondsRemaining = matchState.secondsRemaining,
                teamConfiguration = teamConfiguration,
                blueScore = blueScore,
                redScore = redScore
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                PaperToggle("Temporary dents (resets Sandbox)", dentingEnabled, {
                    viewModel.onIntent(MatchIntent.SetDamage(it))
                })
                Text(
                    "FOLDBALL  /  MATCH MOTION",
                    color = FoldballColor.InkMuted,
                    style = FoldballType.Caption
                )
                Text(
                    "Player and ball movement",
                    color = FoldballColor.Ink,
                    style = FoldballType.Title
                )
                Text(
                    "Move players · aim · time your shot · watch the bounce",
                    color = FoldballColor.InkMuted,
                    style = FoldballType.Body
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
            LaunchedEffect(fieldWidth, fieldHeight) {
                viewModel.onIntent(MatchIntent.ConfigureArena(fieldWidth, fieldHeight))
            }
            val state = matchState.snapshot ?: return@BoxWithConstraints

            val countdownCue = remember(viewModel, matchKey) { CountdownCue() }
            val turnCue = remember(viewModel, matchKey) { TurnCue() }
            val soundDispatcher = remember(viewModel, matchKey) { SoundDispatcher(state.soundEvents) }
            var commentary by remember(viewModel, matchKey) { mutableStateOf<String?>(null) }
            var previousCommentEvents by remember(viewModel, matchKey) {
                mutableStateOf(state.soundEvents)
            }
            var lastCommentMs by remember(viewModel, matchKey) { mutableLongStateOf(-3000L) }
            LaunchedEffect(viewModel, matchKey, state.soundEvents, isPaused) {
                val old = previousCommentEvents
                val events = state.soundEvents
                previousCommentEvents = events
                if (isPaused || state.phase == GamePhase.GOAL ||
                    state.phase == GamePhase.MATCH_OVER
                ) {
                    commentary = null
                    return@LaunchedEffect
                }
                val now = audioClock.elapsedNow().inWholeMilliseconds
                val message = when {
                    events.overbars > old.overbars -> "OVER THE BAR!"
                    events.crossbars > old.crossbars -> "OFF THE CROSSBAR!"
                    events.posts > old.posts -> "OFF THE POST!"
                    events.blocks > old.blocks -> "BLOCKED!"
                    else -> null
                }
                if (message != null && now - lastCommentMs >= 2500) {
                    commentary = message
                    lastCommentMs = now
                }
            }
            LaunchedEffect(commentary) {
                if (commentary != null) {
                    delay(1400)
                    commentary = null
                }
            }

            LaunchedEffect(
                viewModel,
                matchKey,
                state.phase,
                state.focusedPlayerId,
                openingKickoffTeam,
                isPaused,
                audio?.enabled,
                audio?.foreground
            ) {
                val possession = state.players.first { it.id == state.focusedPlayerId }.team
                val cue = turnCue.update(
                    state.phase,
                    possession,
                    state.focusedPlayerId,
                    ready = fullMatch && openingKickoffTeam != null,
                    active = !isPaused && audio?.enabled == true && audio.foreground
                )
                cue?.let { audio?.play(it, .3f) }
            }

            LaunchedEffect(viewModel, matchKey, state.phase) {
                if (fullMatch && state.phase == GamePhase.MATCH_OVER) {
                    audio?.playMatchEnd(
                        lostAgainstAi = playAgainstBot && state.winner == GameTeam.BLUE,
                        wonAgainstAi = playAgainstBot && state.winner == GameTeam.RED
                    )
                }
            }

            LaunchedEffect(viewModel, matchKey, state.soundEvents, isPaused) {
                var goalPlayed = false
                soundDispatcher.dispatch(
                    state,
                    audioClock.elapsedNow().inWholeMilliseconds,
                    !isPaused && audio?.enabled == true && audio.foreground
                ) { name, volume ->
                    audio?.play(name, volume)
                    if (name == "goal") goalPlayed = true
                }
                if (isPaused) audio?.stop()
                if (goalPlayed) audio?.playGoalCheer()
            }
            DisposableEffect(viewModel, matchKey) { onDispose { audio?.stop() } }

            val botTurn = viewModel.isBotTurn
            val formationOpen = state.phase == GamePhase.DEFENDING && state.phaseTimeRemaining > 0f
            var previousHapticPhase by remember(viewModel, matchKey) { mutableStateOf(state.phase) }
            val goalShake = remember(viewModel, matchKey) { Animatable(0f) }
            var kickingPlayerId by remember(viewModel, matchKey) { mutableStateOf<String?>(null) }
            var movingPlayerId by remember(viewModel, matchKey) { mutableStateOf<String?>(null) }
            var movingPlayerOrigin by remember(viewModel, matchKey) { mutableStateOf<Vector2?>(null) }
            var selectedFormationPlayerId by remember(viewModel, matchKey) {
                mutableStateOf<String?>(null)
            }
            var showAimGuide by remember(viewModel, matchKey) { mutableStateOf(false) }
            var isPowerTiming by remember(viewModel, matchKey) { mutableStateOf(false) }
            var powerTimingElapsed by remember(viewModel, matchKey) { mutableFloatStateOf(0f) }
            LaunchedEffect(state.phase, formationOpen) {
                val oldPhase = previousHapticPhase
                if (oldPhase != state.phase) {
                    if (oldPhase == GamePhase.AIMING) {
                        isPowerTiming = false
                        powerTimingElapsed = 0f
                    }
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
                showAimGuide = state.phase == GamePhase.AIMING
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
            LaunchedEffect(viewModel, matchKey, state.phase, state.lastScoringTeam) {
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
                        }
                    )
                } else {
                    goalShake.snapTo(0f)
                }
            }
            fun shoot() {
                val shotState = viewModel.snapshot
                val chargedPower = shotState.power
                val shooterId = shotState.focusedPlayerId
                if (viewModel.onIntent(MatchIntent.Shoot)) {
                    showAimGuide = false
                    kickingPlayerId = shooterId

                    scope.launch {
                        playerMotion.setPowerProgress(chargedPower)
                        playerMotion.setAimAngle(shotState.aimAngle)
                        playerMotion.releaseChargedKick()
                        if (kickingPlayerId == shooterId) kickingPlayerId = null
                    }
                }
            }

            fun finishPlayerMove(playerId: String) {
                val movesBefore = viewModel.defensiveMovesUsed
                viewModel.onIntent(MatchIntent.EndPlayerMove(playerId))
                if (viewModel.defensiveMovesUsed > movesBefore) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }

            LaunchedEffect(viewModel, matchKey, isPaused, openingKickoffTeam) {
                var previous = 0L
                while (true) {
                    withFrameNanos { now ->
                        if (!isPaused && openingKickoffTeam != null &&
                            previous != 0L &&
                            viewModel.snapshot.phase != GamePhase.MATCH_OVER
                        ) {
                            val frameSeconds = ((now - previous) / 1_000_000_000f).coerceAtMost(.1f)
                            viewModel.advanceFrame(frameSeconds)
                            val nextState = viewModel.snapshot
                            if (fullMatch) {
                                val timedPhase =
                                    nextState.phase == GamePhase.DEFENDING ||
                                        nextState.phase == GamePhase.AIMING
                                val humanTurn = timedPhase && !(playAgainstBot && viewModel.isBotTurn)
                                val key = "${nextState.phase}:${nextState.focusedPlayerId}:${nextState.soundEvents.kicks}"
                                if (countdownCue.update(
                                        key,
                                        viewModel.uiState.value.phaseSeconds,
                                        viewModel.uiState.value.secondsRemaining,
                                        humanTurn,
                                        nextState.phase != GamePhase.MATCH_OVER &&
                                            audio?.enabled == true &&
                                            audio.foreground
                                    )
                                ) {
                                    audio?.play("countdown_tick", .25f)
                                }
                            }
                        }
                        previous = now
                    }
                }
            }

            LaunchedEffect(
                viewModel,
                matchKey,
                playAgainstBot,
                state.phase,
                state.focusedPlayerId,
                isPaused,
                openingKickoffTeam
            ) {
                if (!fullMatch || !playAgainstBot || isPaused ||
                    openingKickoffTeam == null ||
                    !botTurn
                ) {
                    return@LaunchedEffect
                }
                val thinkMillis = kotlin.random.Random.nextLong(1100, 2600)
                delay(
                    minOf(
                        thinkMillis,
                        ((viewModel.snapshot.phaseTimeRemaining - 2.2f) * 1000).toLong().coerceAtLeast(
                            0L
                        )
                    )
                )
                if (viewModel.snapshot.phase == GamePhase.DEFENDING) {
                    val moves = viewModel.planDefense()
                    for (move in moves) {
                        if (viewModel.snapshot.phase != GamePhase.DEFENDING) break
                        viewModel.onIntent(MatchIntent.MoveToZone(move.playerId, move.zone))

                        delay(
                            minOf(
                                kotlin.random.Random.nextLong(400, 850),
                                (
                                    (
                                        viewModel.snapshot.phaseTimeRemaining -
                                            .3f
                                        ) *
                                        1000
                                    ).toLong().coerceAtLeast(0L)
                            )
                        )
                    }
                    viewModel.onIntent(MatchIntent.FinishDefense)
                } else if (viewModel.snapshot.phase == GamePhase.AIMING) {
                    val plan = viewModel.planShot()
                    val shot = plan.shot
                    if (viewModel.snapshot.phase == GamePhase.AIMING && viewModel.isBotTurn) {
                        val target = shot
                        isPowerTiming = false
                        val startingAngle = viewModel.snapshot.aimAngle
                        val consideredAngle = plan.alternativeAngle?.takeIf {
                            kotlin.random.Random.nextFloat() <
                                .25f
                        }
                        val overshoot = if (kotlin.random.Random.nextBoolean()) .025f else -.025f
                        val aimSeconds =
                            minOf(
                                1.05f,
                                (viewModel.snapshot.phaseTimeRemaining - 1.1f).coerceAtLeast(.05f)
                            )
                        var elapsed = 0f
                        var previousFrame = withFrameNanos { it }
                        while (elapsed < aimSeconds && viewModel.currentPhase == GamePhase.AIMING) {
                            val now = withFrameNanos { it }
                            elapsed += (now - previousFrame) / 1_000_000_000f
                            previousFrame = now
                            val progress = (elapsed / aimSeconds).coerceIn(0f, 1f)
                            val smooth = progress * progress * (3f - 2f * progress)
                            val hesitation =
                                kotlin.math.sin(progress * kotlin.math.PI.toFloat()) * overshoot
                            val aimedAngle = if (consideredAngle != null && progress < .3f) {
                                startingAngle + (consideredAngle - startingAngle) * (progress / .3f)
                            } else if (consideredAngle != null) {
                                val correction = ((progress - .3f) / .7f).coerceIn(0f, 1f)
                                consideredAngle +
                                    (target.angle - consideredAngle) * correction * correction *
                                    (3f - 2f * correction)
                            } else {
                                startingAngle + (target.angle - startingAngle) * smooth
                            }
                            viewModel.onIntent(MatchIntent.SetAim(aimedAngle + hesitation))
                            playerMotion.setAimAngle(viewModel.snapshot.aimAngle)
                        }
                        if (viewModel.currentPhase == GamePhase.AIMING) {
                            viewModel.onIntent(MatchIntent.SetPower(MINIMUM_TIMING_POWER))

                            powerTimingElapsed = 0f
                            val releaseAt = plan.releaseAt
                            isPowerTiming = true
                            try {
                                playerMotion.startPowerCharge()
                                while (viewModel.currentPhase == GamePhase.AIMING &&
                                    powerTimingElapsed < releaseAt &&
                                    viewModel.snapshot.phaseTimeRemaining > .12f
                                ) {
                                    withFrameNanos { }
                                }
                            } finally {
                                isPowerTiming = false
                            }
                            if (viewModel.snapshot.focusedPlayerId == plan.playerId &&
                                viewModel.currentPhase == GamePhase.AIMING
                            ) {
                                shoot()
                            }
                        }
                    }
                }
            }

            LaunchedEffect(viewModel, matchKey, isPowerTiming, isPaused) {
                if (isPowerTiming && !isPaused && viewModel.snapshot.phase == GamePhase.AIMING) {
                    var previous = 0L
                    var elapsedSeconds = 0f
                    while (isPowerTiming && !isPaused &&
                        viewModel.snapshot.phase == GamePhase.AIMING
                    ) {
                        val now = withFrameNanos { it }
                        if (previous != 0L) {
                            elapsedSeconds += (now - previous) / 1_000_000_000f
                        }
                        previous = now
                        powerTimingElapsed = elapsedSeconds
                        val power = ShotPowerMeter.powerAt(
                            elapsedSeconds,
                            viewModel.focusedDentProgress
                        )
                        viewModel.onIntent(MatchIntent.SetPower(power))
                        playerMotion.setPowerProgress(power)
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().then(if (fullMatch) Modifier.fillMaxHeight() else Modifier),
                verticalArrangement = if (fullMatch) {
                    Arrangement.SpaceBetween
                } else {
                    Arrangement.spacedBy(
                        14.dp
                    )
                }
            ) {
                val possessionTeam = state.players.first { it.id == state.focusedPlayerId }.team
                val turnTeam = if (state.phase == GamePhase.DEFENDING) {
                    if (possessionTeam == GameTeam.RED) GameTeam.BLUE else GameTeam.RED
                } else {
                    possessionTeam
                }
                val localTwoPlayers = fullMatch && !playAgainstBot
                val turnName = teamConfiguration.forTeam(turnTeam).jerseyLabel
                val turnLabel = when {
                    localTwoPlayers && state.phase == GamePhase.DEFENDING ->
                        "$turnName TURN · DEFEND" +
                            if (state.openingKickoffPending) " · OWN HALF" else ""

                    localTwoPlayers && state.phase == GamePhase.AIMING -> "$turnName TURN · KICK"

                    localTwoPlayers && state.phase == GamePhase.KICKER_APPROACH -> "UP NEXT · $turnName"

                    else -> when (state.phase) {
                        GamePhase.DEFENDING -> if (state.openingKickoffPending) {
                            "FORMATION · ${teamConfiguration.forTeam(turnTeam).jerseyLabel} · OWN HALF"
                        } else {
                            "SET FORMATION · ${teamConfiguration.forTeam(
                                turnTeam
                            ).jerseyLabel} · TAP PLAYER"
                        }

                        GamePhase.AIMING -> "KICK · ${teamConfiguration.forTeam(turnTeam).jerseyLabel}"

                        GamePhase.KICKER_APPROACH -> "KICK NEXT · ${teamConfiguration.forTeam(
                            turnTeam
                        ).jerseyLabel}"

                        GamePhase.BALL_MOVING -> "SHOT IN PLAY · ${teamConfiguration.forTeam(
                            possessionTeam
                        ).jerseyLabel}"

                        GamePhase.TURNOVER_RETREAT -> "TURNOVER · ${teamConfiguration.forTeam(
                            possessionTeam
                        ).jerseyLabel}"

                        GamePhase.GOAL -> "GOAL · ${state.lastScoringTeam?.let {
                            teamConfiguration.forTeam(it).jerseyLabel
                        }.orEmpty()}"

                        GamePhase.MATCH_OVER -> "MATCH COMPLETE"
                    }
                }
                val statusTeam = if (state.phase == GamePhase.GOAL) state.lastScoringTeam ?: turnTeam else turnTeam
                val tossing = fullMatch && openingKickoffTeam == null
                TapeLabel(
                    commentary ?: turnLabel,
                    emphasized = true,
                    outlined = true,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                        .then(
                            if (tossing) {
                                Modifier.alpha(0f).clearAndSetSemantics { }
                            } else {
                                Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                            }
                        ),
                    backgroundColor = if (state.phase == GamePhase.MATCH_OVER) FoldballColor.Tape else teamConfiguration.kitColor(statusTeam)
                )
                ClassroomStadium(
                    fieldWidthDp = fieldWidth,
                    fieldHeightDp = fieldHeight,
                    goalShakeOffset = goalShake.value,
                    shakeTopGoal =
                        state.phase == GamePhase.GOAL && state.lastScoringTeam == GameTeam.RED,
                    shakeBottomGoal =
                        state.phase == GamePhase.GOAL && state.lastScoringTeam == GameTeam.BLUE
                ) {
                    if (fullMatch && openingKickoffTeam == null) {
                        OpeningTossOverlay(
                            ballStyle = ballStyle,
                            teamConfiguration = teamConfiguration,
                            onWinner = { viewModel.onIntent(MatchIntent.KickoffSelected(it)) }
                        )
                    } else {
                        val selectedOverlayId = (movingPlayerId ?: selectedFormationPlayerId)?.takeIf(
                            viewModel::canMovePlayer
                        )
                        val overlayObstacles = state.players.filter { it.id != selectedOverlayId }
                        val selectedLegalDestinations =
                            remember(
                                viewModel,
                                selectedOverlayId,
                                overlayObstacles,
                                formationOpen,
                                viewModel.defensiveMovesUsed
                            ) {
                                if (!formationOpen || selectedOverlayId == null) {
                                    emptyMap<Int, Vector2?>()
                                } else {
                                    (1..GameEngine.ZONE_COUNT).associateWith {
                                        viewModel.legalPlayerPositionInZone(selectedOverlayId, it)
                                    }
                                }
                            }
                        val selectedAdjacentZones =
                            remember(
                                viewModel,
                                selectedOverlayId,
                                formationOpen,
                                viewModel.defensiveMovesUsed
                            ) {
                                if (selectedOverlayId == null || !formationOpen) {
                                    emptySet<Int>()
                                } else {
                                    (1..GameEngine.ZONE_COUNT).filterTo(mutableSetOf()) {
                                        viewModel.isAdjacentZoneForPlayer(selectedOverlayId, it)
                                    }
                                }
                            }
                        val teamReachableZones =
                            remember(
                                viewModel,
                                state.players,
                                formationOpen,
                                selectedOverlayId,
                                viewModel.defensiveMovesUsed
                            ) {
                                if (!formationOpen ||
                                    selectedOverlayId != null
                                ) {
                                    emptySet<Int>()
                                } else {
                                    state.players
                                        .filter {
                                            it.team == turnTeam && viewModel.canMovePlayer(it.id)
                                        }
                                        .flatMap { player ->
                                            (1..GameEngine.ZONE_COUNT).filter {
                                                viewModel.legalPlayerPositionInZone(player.id, it) !=
                                                    null
                                            }
                                        }
                                        .toSet()
                                }
                            }
                        Canvas(Modifier.fillMaxSize()) {
                            val h = size.height
                            if (formationOpen) {
                                val cellWidth = size.width / GameEngine.ZONE_COLUMNS
                                val cellHeight = size.height / GameEngine.ZONE_ROWS
                                fun zoneAt(position: Vector2): Int {
                                    val column = (
                                        position.x /
                                            (fieldWidth / GameEngine.ZONE_COLUMNS)
                                        )
                                        .toInt().coerceIn(0, GameEngine.ZONE_COLUMNS - 1)
                                    val rowFromTop = (
                                        position.y /
                                            (fieldHeight / GameEngine.ZONE_ROWS)
                                        )
                                        .toInt().coerceIn(0, GameEngine.ZONE_ROWS - 1)
                                    val rowFromBottom = GameEngine.ZONE_ROWS - 1 - rowFromTop
                                    return rowFromBottom * GameEngine.ZONE_COLUMNS + column + 1
                                }
                                val selectedMoveId = movingPlayerId ?: selectedFormationPlayerId
                                    ?.takeIf(viewModel::canMovePlayer)
                                val selectedPlayer = state.players.firstOrNull {
                                    it.id == selectedMoveId
                                }
                                val originPosition = movingPlayerOrigin ?: selectedPlayer?.position
                                val originZones = originPosition?.let {
                                    setOf(zoneAt(it))
                                }.orEmpty()
                                val legalDestinations = selectedLegalDestinations.filterKeys {
                                    it !in
                                        originZones
                                }
                                val adjacentZones = selectedAdjacentZones
                                val movementDash = PathEffect.dashPathEffect(
                                    floatArrayOf(7.dp.toPx(), 5.dp.toPx())
                                )
                                val fieldInset = 18.dp.toPx()
                                clipRect(
                                    left = fieldInset,
                                    top = fieldInset,
                                    right = size.width - fieldInset,
                                    bottom = size.height - fieldInset
                                ) {
                                    for (zone in 1..GameEngine.ZONE_COUNT) {
                                        val index = zone - 1
                                        val column = index % GameEngine.ZONE_COLUMNS
                                        val rowFromTop =
                                            GameEngine.ZONE_ROWS - 1 -
                                                index / GameEngine.ZONE_COLUMNS
                                        val topLeft =
                                            Offset(column * cellWidth, rowFromTop * cellHeight)
                                        val available = legalDestinations[zone] != null
                                        val teamColor = if (turnTeam ==
                                            GameTeam.RED
                                        ) {
                                            teamConfiguration.kitColor(GameTeam.RED)
                                        } else {
                                            teamConfiguration.kitColor(GameTeam.BLUE)
                                        }
                                        if (selectedMoveId == null && zone in teamReachableZones) {
                                            drawRect(
                                                teamColor.copy(
                                                    alpha = if (botTurn) .065f else .10f
                                                ),
                                                topLeft,
                                                Size(cellWidth, cellHeight)
                                            )
                                            drawRect(
                                                teamColor.copy(alpha = if (botTurn) .20f else .35f),
                                                topLeft,
                                                Size(cellWidth, cellHeight),
                                                style = Stroke(
                                                    .8.dp.toPx(),
                                                    pathEffect = movementDash
                                                )
                                            )
                                        }
                                        if (available || zone in adjacentZones) {
                                            drawRect(
                                                color = if (available) {
                                                    selectedPlayer?.let { player ->
                                                        if (player.team ==
                                                            GameTeam.RED
                                                        ) {
                                                            teamConfiguration.kitColor(GameTeam.RED)
                                                        } else {
                                                            teamConfiguration.kitColor(GameTeam.BLUE)
                                                        }
                                                    }?.copy(alpha = .12f)
                                                        ?: FoldballColor.BallHighlight.copy(
                                                            alpha = .12f
                                                        )
                                                } else {
                                                    Color(0xFFB8BCC1).copy(alpha = .10f)
                                                },
                                                topLeft = topLeft,
                                                size = Size(cellWidth, cellHeight)
                                            )
                                            drawRect(
                                                color = if (available) {
                                                    FoldballColor.BallHighlight.copy(alpha = .72f)
                                                } else {
                                                    Color(0xFFB8BCC1).copy(alpha = .42f)
                                                },
                                                topLeft,
                                                Size(cellWidth, cellHeight),
                                                style = Stroke(
                                                    width = if (available) 1.1.dp.toPx() else .8.dp.toPx(),
                                                    pathEffect = if (available) movementDash else null
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            val ball = state.ball.position
                            val ballX = ball.x * density
                            val ballY = ball.y * density
                            val player = state.players.first { it.id == state.focusedPlayerId }
                            val aimDirection = AimDirection.shotVector(player.team, state.aimAngle)
                            val end = Offset(
                                ballX + aimDirection.x * h * .29f,
                                ballY + aimDirection.y * h * .29f
                            )
                            if (state.phase == GamePhase.AIMING && showAimGuide) {
                                drawPaperAimGuide(Offset(ballX, ballY), end)
                            }
                        }

                        state.players.forEach { player ->
                            val focused = player.id == state.focusedPlayerId
                            val canMove = !botTurn && viewModel.canMovePlayer(player.id)
                            val isKicking = player.id == kickingPlayerId
                            val showFocus = focused && (
                                state.phase == GamePhase.KICKER_APPROACH ||
                                    state.phase == GamePhase.AIMING
                                )
                            val showFormation = formationOpen && player.team == turnTeam && canMove
                            val playerSize = minOf(
                                76f,
                                fieldHeight / GameEngine.ZONE_ROWS * 1.5f
                            ).dp
                            val visualAimAngle =
                                if (player.team == GameTeam.RED) state.aimAngle else -state.aimAngle
                            val displayedX by animateDpAsState(
                                targetValue = player.position.x.dp,
                                animationSpec = if (movingPlayerId ==
                                    player.id
                                ) {
                                    snap()
                                } else {
                                    spring(dampingRatio = .92f, stiffness = 1200f)
                                }
                            )
                            val displayedY by animateDpAsState(
                                targetValue = player.position.y.dp,
                                animationSpec = if (movingPlayerId ==
                                    player.id
                                ) {
                                    snap()
                                } else {
                                    spring(dampingRatio = .92f, stiffness = 1200f)
                                }
                            )
                            // FRONT's lower contact edge is inset more than BACK's upper fold.
                            // Align their visible kicking gap without moving viewModel positions/hitboxes.
                            val kickerVisualOffsetY = if (player.team == GameTeam.BLUE &&
                                (
                                    (
                                        focused &&
                                            state.phase in
                                            listOf(
                                                GamePhase.KICKER_APPROACH,
                                                GamePhase.DEFENDING,
                                                GamePhase.AIMING
                                            )
                                        ) ||
                                        isKicking
                                    )
                            ) {
                                playerSize * .086f
                            } else {
                                0.dp
                            }
                            val kickProgress = if (isKicking) playerMotion.kickProgress else 0f
                            val kickDirectionY = if (player.team == GameTeam.RED) -1f else 1f
                            PaperPlayer(
                                modifier = Modifier.offset(
                                    displayedX - playerSize / 2 +
                                        8.dp * (sin(visualAimAngle) * kickProgress),
                                    displayedY - playerSize / 2 + kickerVisualOffsetY +
                                        20.dp *
                                        (kickDirectionY * cos(visualAimAngle) * kickProgress)
                                )
                                    .size(playerSize)
                                    .clickable(
                                        enabled =
                                            !focused && canMove && !isPaused &&
                                                formationOpen && player.team == turnTeam,
                                        onClick = { selectedFormationPlayerId = player.id }
                                    )
                                    .pointerInput(player.id, state.phase, focused, formationOpen, isPaused, turnTeam, canMove, botTurn) {
                                        if (!focused && canMove &&
                                            !isPaused &&
                                            formationOpen &&
                                            player.team == turnTeam
                                        ) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    selectedFormationPlayerId = player.id
                                                    movingPlayerId = player.id
                                                    movingPlayerOrigin = player.position
                                                    viewModel.onIntent(
                                                        MatchIntent.BeginPlayerMove(player.id)
                                                    )
                                                },
                                                onDragEnd = {
                                                    finishPlayerMove(player.id)
                                                    movingPlayerId = null
                                                    movingPlayerOrigin = null
                                                    if (!viewModel.canMovePlayer(player.id)) {
                                                        selectedFormationPlayerId = null
                                                    }
                                                },
                                                onDragCancel = {
                                                    finishPlayerMove(player.id)
                                                    movingPlayerId = null
                                                    movingPlayerOrigin = null
                                                    if (!viewModel.canMovePlayer(player.id)) {
                                                        selectedFormationPlayerId = null
                                                    }
                                                }
                                            ) { change, drag ->
                                                change.consume()
                                                viewModel.onIntent(
                                                    MatchIntent.MovePlayer(
                                                        player.id,
                                                        Vector2(drag.x / density, drag.y / density)
                                                    )
                                                )
                                            }
                                        } else if (focused && !isPaused &&
                                            !botTurn &&
                                            state.phase == GamePhase.AIMING
                                        ) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    showAimGuide = true
                                                    viewModel.onIntent(
                                                        MatchIntent.SetPower(MINIMUM_TIMING_POWER)
                                                    )

                                                    isPowerTiming = true
                                                    scope.launch { playerMotion.startPowerCharge() }
                                                },
                                                onDragEnd = {
                                                    isPowerTiming = false
                                                    shoot()
                                                },
                                                onDragCancel = {
                                                    isPowerTiming = false
                                                    viewModel.onIntent(MatchIntent.SetPower(0f))
                                                    scope.launch {
                                                        playerMotion.cancelPowerCharge()
                                                    }
                                                }
                                            ) { change, drag ->
                                                change.consume()
                                                viewModel.onIntent(MatchIntent.AimBy(drag.x / density))
                                            }
                                        }
                                    },
                                facing = if (player.team ==
                                    GameTeam.RED
                                ) {
                                    PlayerFacing.BACK
                                } else {
                                    PlayerFacing.FRONT
                                },
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
                                dentProgress = player.condition.progress,
                                dentDirectionX = player.condition.bendX,
                                dentDirectionY = player.condition.bendY,
                                powerPressProgress = if (showFocus ||
                                    isKicking
                                ) {
                                    playerMotion.powerPressProgress
                                } else {
                                    0f
                                },
                                aimAngle = if ((showFocus || isKicking) &&
                                    player.team == GameTeam.RED
                                ) {
                                    state.aimAngle
                                } else if (showFocus || isKicking) {
                                    -state.aimAngle
                                } else {
                                    0f
                                },
                                kickProgress = kickProgress,
                                impactDirectionX = player.impactDirectionX,
                                impactDirectionY = player.impactDirectionY,
                                impactStrength = player.impactStrength,
                                jerseyColorCount = teamConfiguration.forTeam(player.team).jerseyColorCount,
                                jerseyPalette = ClubKit.palette(teamConfiguration.forTeam(player.team).logoId),
                                jerseyPattern = teamConfiguration.forTeam(player.team).let {
                                    it.jerseyPattern ?: JerseyPattern.PLAIN
                                },
                                jerseyColor = if (player.team ==
                                    GameTeam.RED
                                ) {
                                    teamConfiguration.kitColor(GameTeam.RED)
                                } else {
                                    teamConfiguration.kitColor(GameTeam.BLUE)
                                },
                                number = when (player.id.substringAfter("-")) {
                                    "center" -> "1"
                                    "left" -> "2"
                                    "right" -> "3"
                                    "midfielder" -> "4"
                                    else -> "9"
                                },
                                name = teamConfiguration.forTeam(player.team).jerseyLabel
                            )
                            if (dentingEnabled) {
                                StandeeConditionBar(
                                    damage = player.condition.progress,
                                    modifier = Modifier.offset(
                                        displayedX - 17.dp,
                                        if (player.team ==
                                            GameTeam.BLUE
                                        ) {
                                            displayedY - playerSize * .381f +
                                                kickerVisualOffsetY -
                                                7.dp
                                        } else {
                                            displayedY + playerSize * .297f + 2.dp
                                        }
                                    ).size(34.dp, 5.dp)
                                )
                            }
                        }

                        val ballSpeed = (state.ball.velocity.length() / 670f).coerceIn(0f, 1f)
                        val ballLift = (
                            GoalGeometry.visualLift(state.ball.height) /
                                GoalGeometry.maximumVisualLift
                            ).coerceIn(0f, 1f)
                        val groundContact = (1f - state.ball.height / 5f).coerceIn(0f, 1f)
                        val shadowWidth = 15.dp + 23.dp * ballLift
                        val shadowHeight = 3.dp + 7.dp * ballLift
                        Canvas(
                            Modifier.offset(
                                state.ball.position.x.dp - shadowWidth / 2,
                                state.ball.position.y.dp + MatchBallVisualSize / 2 + 1.dp -
                                    shadowHeight / 2
                            ).size(shadowWidth, shadowHeight)
                        ) {
                            drawOval(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        FoldballColor.ShadowBrown.copy(
                                            alpha =
                                                .17f * (1f - .8f * ballLift)
                                        ),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.width * .52f
                                )
                            )
                            if (groundContact > .01f) {
                                val contactWidth = 10.dp.toPx()
                                val contactHeight = 1.8.dp.toPx()
                                drawOval(
                                    color = FoldballColor.ShadowBrown.copy(
                                        alpha = .22f * groundContact
                                    ),
                                    topLeft = Offset(
                                        (size.width - contactWidth) / 2f,
                                        (size.height - contactHeight) / 2f
                                    ),
                                    size = Size(contactWidth, contactHeight)
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
                                    state.ball.position.y * density
                                )
                                val skid = ((speed - 65f) / 420f).coerceIn(0f, 1f)
                                val length = if (airborne) {
                                    12.dp.toPx() + 25.dp.toPx() * skid
                                } else {
                                    10.dp.toPx() + 13.dp.toPx() * skid
                                }
                                repeat(3) { index ->
                                    val side = (index - 1) * 2.4f.dp.toPx()
                                    val start =
                                        center - direction * (7.dp.toPx() + index * 1.5f.dp.toPx()) +
                                            perpendicular * side
                                    val end =
                                        center - direction * length + perpendicular * (side * 1.8f)
                                    val trailAlpha =
                                        if (airborne) .10f + skid * .16f else .15f + skid * .27f
                                    drawLine(
                                        color = Color(0xFF796F61).copy(alpha = trailAlpha),
                                        start = start,
                                        end = end,
                                        strokeWidth = (1f + skid * 1.5f - index * .25f).dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }

                            val impact = state.ball.impactStrength.coerceIn(0f, 1f)
                            if (impact > .025f &&
                                (state.ball.impactSideX != 0f || state.ball.impactSideY != 0f)
                            ) {
                                val center = Offset(
                                    state.ball.position.x * density,
                                    state.ball.position.y * density
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
                                        contactPoint +
                                            outgoing * (3.dp.toPx() + progress * 12.dp.toPx()) +
                                            tangent * offset
                                    val tip = fragment + outgoing * size
                                    val wingA =
                                        fragment - outgoing * (size * .35f) +
                                            tangent * (size * .55f)
                                    val wingB =
                                        fragment - outgoing * (size * .35f) -
                                            tangent * (size * .55f)
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
                                    state.ball.position.x * density +
                                        state.ball.impactSideX * 7.dp.toPx(),
                                    state.ball.position.y * density +
                                        state.ball.impactSideY * 7.dp.toPx()
                                )
                                val expansion = (1f - postImpact) * 7.dp.toPx()
                                drawCircle(
                                    color = Color(0xFFD6A568).copy(alpha = postImpact * .8f),
                                    radius = 2.5.dp.toPx() + expansion,
                                    center = center,
                                    style = Stroke(width = (1f + postImpact).dp.toPx())
                                )
                                repeat(5) { index ->
                                    val angle = index * (2f * PI.toFloat() / 5f) +
                                        if (state.ball.impactSideX < 0f) PI.toFloat() else 0f
                                    val direction = Offset(cos(angle), sin(angle))
                                    val start = center + direction * (2.dp.toPx() + expansion)
                                    val end =
                                        start + direction * (2.dp.toPx() + postImpact * 3.dp.toPx())
                                    drawLine(
                                        color = Color(0xFFE8C38F).copy(alpha = postImpact * .9f),
                                        start = start,
                                        end = end,
                                        strokeWidth = 1.dp.toPx(),
                                        cap = StrokeCap.Round
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
                                        ground - direction * (4.dp.toPx() + index % 2 * 3.dp.toPx()) +
                                            tangent * side
                                    drawOval(
                                        color = Color(0xFF8A7963).copy(alpha = landing * .17f),
                                        topLeft = Offset(
                                            center.x - 2.dp.toPx(),
                                            center.y - 1.dp.toPx()
                                        ),
                                        size = Size(4.dp.toPx(), 2.dp.toPx())
                                    )
                                }
                            }
                        }
                        val ballStretch = (state.ball.velocity.length() / 700f).coerceIn(0f, .08f)
                        val airborneScale = ballLift * .16f
                        val landingSquash = state.ball.landingStrength * groundContact * .18f
                        val visualBallLift = GoalGeometry.visualLift(state.ball.height).dp
                        val ballVisualSize = MatchBallVisualSize
                        PaperBall(
                            ballStyle = ballStyle,
                            rotationX = state.ball.rotationX,
                            rotationY = state.ball.rotationY,
                            scaleX = 1f + ballStretch + airborneScale + landingSquash,
                            scaleY = 1f - ballStretch * .4f + airborneScale - landingSquash,
                            impactSideX = state.ball.impactSideX,
                            impactSideY = state.ball.impactSideY,
                            impactStrength = state.ball.impactStrength,
                            modifier = Modifier.offset(
                                state.ball.position.x.dp - ballVisualSize / 2,
                                state.ball.position.y.dp - ballVisualSize / 2 - visualBallLift
                            ).size(ballVisualSize)
                        )

                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                            if (isPowerTiming) {
                                PowerTimingMeter(
                                    power = state.power,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }
                        }
                        GoalCelebrationOverlay(
                            visible = state.phase == GamePhase.GOAL,
                            scoringTeam = state.lastScoringTeam,
                            teamConfiguration = teamConfiguration
                        )
                    }
                }

                if (tossing) {
                    Spacer(Modifier.fillMaxWidth().height(56.dp))
                } else {
                    Row(
                        Modifier.fillMaxWidth().then(if (fullMatch) Modifier.height(56.dp) else Modifier),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (fullMatch &&
                            (state.phase == GamePhase.DEFENDING || state.phase == GamePhase.AIMING)
                        ) {
                            TurnTimer(phaseSeconds, teamConfiguration.forTeam(turnTeam).jerseyLabel)
                        }
                        Box(Modifier.weight(1f)) {
                            MatchStatusPanel(
                                snapshot = state,
                                defensiveMovesUsed = viewModel.defensiveMovesUsed,
                                canUndo = !botTurn && viewModel.canUndoPlayerMove(),
                                isBotTurn = botTurn,
                                isPaused = isPaused,
                                showPower = !fullMatch,
                                teamConfiguration = teamConfiguration,
                                onUndo = { viewModel.onIntent(MatchIntent.Undo) },
                                onFinishDefense = { viewModel.onIntent(MatchIntent.FinishDefense) },
                                compactMatchBar = fullMatch
                            )
                        }
                    }
                }
            }

            if (showMatchMenu) {
                Dialog(onDismissRequest = {
                    showMatchMenu = false
                    viewModel.onIntent(MatchIntent.SetPaused(false))
                }) {
                    PaperCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = FoldballShapes.Banner,
                        tilt = FoldballTilt.MatchMenu,
                        ruled = false
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            audio?.let {
                                PaperToggle(
                                    "Sound effects",
                                    it.enabled,
                                    it::changeEnabled,
                                    Modifier.fillMaxWidth()
                                )
                            }
                            Text(
                                "MATCH PAUSED",
                                style = FoldballType.Title,
                                color = FoldballColor.Ink
                            )
                            PaperButton("Resume", {
                                showMatchMenu = false
                                viewModel.onIntent(MatchIntent.SetPaused(false))
                            }, Modifier.fillMaxWidth())
                            PaperButton("Match summary", {
                                showMatchMenu = false
                                showShareSummary =
                                    true
                                viewModel.onIntent(MatchIntent.SetPaused(false))
                            }, Modifier.fillMaxWidth(), style = PaperButtonStyle.Secondary)
                            PaperButton("Home", {
                                showMatchMenu = false
                                showExitConfirmation = true
                            }, Modifier.fillMaxWidth(), style = PaperButtonStyle.Danger)
                        }
                    }
                }
            }
            if (showExitConfirmation) {
                PaperDialog(
                    title = "RETURN HOME?",
                    message = "The current match will end. Are you sure you want to leave?",
                    onDismiss = {
                        showExitConfirmation = false
                        viewModel.onIntent(MatchIntent.SetPaused(false))
                    },
                    confirmText = "Leave match",
                    onConfirm = {
                        showExitConfirmation = false
                        onOpenTools("home")
                    },
                    dismissText = "Keep playing",
                    stackedActions = true,
                    danger = true
                )
            }
            if (showShareSummary) {
                PaperDialog(
                    title = "MATCH SUMMARY",
                    message = "${teamConfiguration.red.name} vs ${teamConfiguration.blue.name}\n$redScore – $blueScore\n${matchElapsedSeconds / 60}:${
                        (matchElapsedSeconds % 60).toString().padStart(2, '0')
                    } · 3 MIN / FIRST TO 3",
                    onDismiss = { showShareSummary = false },
                    confirmText = "Close",
                    onConfirm = { showShareSummary = false },
                    dismissText = null
                )
            }
            if (fullMatch && state.phase == GamePhase.MATCH_OVER) {
                val winningTeam = state.winner
                PaperDialog(
                    title = "MATCH COMPLETE",
                    message =
                        winningTeam?.let {
                            "${teamConfiguration.forTeam(it).name} wins!"
                        } ?: "DRAW",
                    content = {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    teamConfiguration.red.name,
                                    style = FoldballType.Caption,
                                    color = teamConfiguration.kitColor(GameTeam.RED),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    state.redScore.toString(),
                                    style = FoldballType.LargeTitle,
                                    color = teamConfiguration.kitColor(GameTeam.RED)
                                )
                            }
                            Text("–", style = FoldballType.Title, color = FoldballColor.Ink)
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    teamConfiguration.blue.name,
                                    style = FoldballType.Caption,
                                    color = teamConfiguration.kitColor(GameTeam.BLUE),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    state.blueScore.toString(),
                                    style = FoldballType.LargeTitle,
                                    color = teamConfiguration.kitColor(GameTeam.BLUE)
                                )
                            }
                        }
                    },
                    stackedActions = true,
                    confirmText = "Play again",
                    onConfirm = {
                        viewModel.onIntent(MatchIntent.Rematch)
                        showShareSummary = false
                    },
                    dismissText = "Home",
                    onDismiss = { onOpenTools("home") }
                )
            }
        }
    }
}

private val audioClock = kotlin.time.TimeSource.Monotonic.markNow()

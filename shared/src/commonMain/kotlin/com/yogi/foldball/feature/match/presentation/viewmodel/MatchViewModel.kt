package com.yogi.foldball.feature.match.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.yogi.foldball.domain.game.bot.BotAimExecution
import com.yogi.foldball.domain.game.bot.BotDifficulty
import com.yogi.foldball.domain.game.bot.BotMove
import com.yogi.foldball.domain.game.bot.BotPlanner
import com.yogi.foldball.domain.game.engine.PhysicsTuning
import com.yogi.foldball.domain.game.engine.GameEngine
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchPlayMode
import com.yogi.foldball.domain.game.engine.MatchRuleFlags
import com.yogi.foldball.domain.game.engine.Vector2
import com.yogi.foldball.domain.team.withAiOpponent
import com.yogi.foldball.domain.team.withLocalOpponentBadge
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Owns the live engine. UI supplies frame time and animation/input commands, never physics rules. */
class MatchViewModel(private val options: MatchOptions) : ViewModel() {
    private var practiceOpponents = 0
    private var physicsTuning = options.physicsTuning
    private var arena: MatchIntent.ConfigureArena? = null
    private var game: GameEngine? = null
    private var bot = newBot()
    private val matchTeams = when {
        !options.fullMatch -> options.teams
        options.playAgainstBot -> options.teams.withAiOpponent(options.style)
        else -> options.teams.withLocalOpponentBadge()
    }
    private val state = MutableStateFlow(newState())
    val uiState = state.asStateFlow()
    val snapshot get() = requireNotNull(state.value.snapshot)
    val defensiveMovesUsed get() = game?.defensiveMovesUsed ?: 0
    val currentPhase get() = snapshot.phase
    val currentHalf get() = game?.currentHalf ?: 1
    val standeeBallContactDistance get() = game?.standeeBallContactDistance ?: 38f
    val focusedDentProgress get() = game?.focusedDentProgress ?: 0f
    val isBotTurn get() = options.fullMatch && options.playAgainstBot &&
        bot.controls(snapshot, GameTeam.BLUE)

    fun onIntent(intent: MatchIntent): Boolean {
        when (intent) {
            is MatchIntent.ConfigureArena -> {
                require(intent.width > 0 && intent.height > 0)
                if (arena != intent || game == null) {
                    arena = intent
                    createEngine()
                }
                return true
            }

            is MatchIntent.KickoffSelected -> {
                if (state.value.openingKickoffTeam != null) return false
                state.value = state.value.copy(openingKickoffTeam = intent.team)
                createEngine()
                return true
            }

            is MatchIntent.SetPaused -> {
                if (intent.paused) state.value.snapshot?.playerMovePreview?.let { game?.cancelPlayerMove(it.id) }
                state.value = state.value.copy(paused = intent.paused, snapshot = game?.snapshot)
                return true
            }

            is MatchIntent.SetPhysicsTuning -> {
                if (!options.practice || options.fullMatch) return false
                physicsTuning = intent.tuning
                state.value = newState(state.value.sessionId + 1)
                createEngine()
                return true
            }

            is MatchIntent.SetPracticeOpponents -> {
                if (!options.practice || options.fullMatch || intent.count !in 0..5) return false
                practiceOpponents = intent.count
                state.value = newState(state.value.sessionId + 1)
                createEngine()
                return true
            }

            is MatchIntent.SetDamage -> {
                if (options.fullMatch || state.value.dentingEnabled == intent.enabled) return false
                state.value =
                    newState(state.value.sessionId + 1).copy(dentingEnabled = intent.enabled)
                bot = newBot()
                createEngine()
                return true
            }

            MatchIntent.Rematch -> {
                state.value = newState(state.value.sessionId + 1)
                bot = newBot()
                createEngine()
                return true
            }

            else -> Unit
        }
        val engine = game ?: return false
        if (state.value.paused || state.value.openingKickoffTeam == null ||
            currentPhase == GamePhase.MATCH_OVER
        ) {
            return false
        }
        val accepted = when (intent) {
            is MatchIntent.BeginPlayerMove -> {
                engine.beginPlayerMove(intent.id, intent.previewOnly)
                true
            }

            is MatchIntent.MovePlayer -> {
                engine.moveInactivePlayer(intent.id, intent.delta)
                true
            }

            is MatchIntent.CancelPlayerMove -> engine.cancelPlayerMove(intent.id)

            is MatchIntent.EndPlayerMove -> {
                engine.endPlayerMove(intent.id)
                true
            }

            is MatchIntent.MoveToZone -> engine.movePlayerToZone(intent.id, intent.zone)

            is MatchIntent.AimBy -> {
                engine.aimBy(intent.delta)
                true
            }

            is MatchIntent.SetAim -> {
                engine.setAimAngle(intent.angle)
                true
            }

            is MatchIntent.SetPower -> {
                engine.setPower(intent.power)
                true
            }

            MatchIntent.Undo -> engine.undoLastPlayerMove()

            MatchIntent.FinishDefense -> engine.finishDefense()
            MatchIntent.EndTurn -> engine.endTurn()

            MatchIntent.Shoot -> engine.shoot()
        }
        publish()
        return accepted
    }

    /** One published render snapshot per frame; the engine retains its internal simulation substeps. */
    fun advanceFrame(deltaSeconds: Float, presentationBlocked: Boolean = false) {
        val current = state.value
        val engine = game ?: return
        if (current.paused || presentationBlocked || current.openingKickoffTeam == null ||
            engine.currentPhase == GamePhase.MATCH_OVER
        ) {
            return
        }
        val dt = deltaSeconds.coerceIn(0f, .1f)
        val elapsed = if (engine.snapshot.openingFormation) current.elapsedSeconds else if (options.fullMatch) {
            val limit = if (options.playMode == MatchPlayMode.ATTACK_DEFEND &&
                engine.currentHalf == 1
            ) {
                60f
            } else {
                options.timing.matchSeconds.toFloat()
            }
            (current.elapsedSeconds + dt).coerceAtMost(limit)
        } else {
            0f
        }
        if (options.playMode == MatchPlayMode.ATTACK_DEFEND && elapsed >= 60f) {
            engine.requestSecondHalf()
        }
        if (options.fullMatch && elapsed >= options.timing.matchSeconds) engine.expireMatchTime()
        state.value = current.copy(snapshot = engine.step(dt), elapsedSeconds = elapsed)
    }

    fun canMovePlayer(id: String) = game?.canMovePlayer(id) == true
    fun canUndoPlayerMove() = game?.canUndoPlayerMove() == true
    fun legalPlayerPositionInZone(id: String, zone: Int): Vector2? = game?.legalPlayerPositionInZone(id, zone)
    fun isAdjacentZoneForPlayer(id: String, zone: Int) = game?.isAdjacentZoneForPlayer(id, zone) == true

    suspend fun planDefense(): List<BotMove> {
        val simulation = requireNotNull(game).copyForSimulation()
        val planner = bot
        return withContext(Dispatchers.Default) {
            val moves = planner.chooseDefensePlan(simulation, GameTeam.BLUE)
            if (options.difficulty != BotDifficulty.HARD && moves.size > 1 &&
                Random.nextFloat() < .2f
            ) {
                moves.take(1)
            } else {
                moves
            }
        }
    }

    suspend fun planAttackMove(): BotMove? {
        val simulation = requireNotNull(game).copyForSimulation()
        val planner = bot
        return withContext(Dispatchers.Default) {
            planner.chooseAttackMove(simulation, GameTeam.BLUE)
        }
    }

    suspend fun planShot(): PlannedBotShot {
        val simulation = requireNotNull(game).copyForSimulation()
        val planner = bot
        return withContext(Dispatchers.Default) {
            val target = BotAimExecution.target(planner.chooseShot(simulation), options.difficulty)
            PlannedBotShot(
                target,
                planner.alternativeShot?.angle,
                simulation.snapshot.focusedPlayerId,
                BotAimExecution.releaseTime(
                    target.power,
                    options.difficulty,
                    dentProgress = simulation.focusedDentProgress
                )
            )
        }
    }

    private fun createEngine() {
        val dimensions = arena ?: return
        game = GameEngine(
            dimensions.width,
            dimensions.height,
            targetScore = if (options.fullMatch && options.playMode == MatchPlayMode.CLASSIC) 3 else null,
            timing = options.timing,
            initialKickoffTeam = state.value.openingKickoffTeam ?: GameTeam.RED,
            dentingEnabled = state.value.dentingEnabled,
            format = if (options.fullMatch) options.format else null,
            defensiveBlockWinsPossession = options.fullMatch &&
                options.playMode == MatchPlayMode.CLASSIC &&
                MatchRuleFlags.DEFENSIVE_BLOCK_WINS_POSSESSION,
            playMode = options.playMode,
            practiceOpponents = if (options.practice && !options.fullMatch) practiceOpponents else null,
            threeActionTurns = options.fullMatch && options.threeActionTurns,
            physicsTuning = if (options.practice && !options.fullMatch) physicsTuning else options.physicsTuning
        )
        publish()
    }

    private fun publish() {
        state.value = state.value.copy(snapshot = game?.snapshot)
    }
    private fun newBot() = BotPlanner(options.difficulty, style = options.style)
    private fun newState(sessionId: Int = 0) = MatchUiState(
        teams = matchTeams,
        openingKickoffTeam = if (options.fullMatch) null else GameTeam.RED,
        dentingEnabled = options.standeeDamage,
        sessionId = sessionId,
        durationSeconds = options.timing.matchSeconds,
        practiceOpponents = practiceOpponents,
        physicsTuning = physicsTuning
    )
}

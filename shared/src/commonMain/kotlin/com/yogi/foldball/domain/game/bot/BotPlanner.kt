package com.yogi.foldball.domain.game.bot

import com.yogi.foldball.domain.game.engine.*
import kotlin.random.Random
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.time.TimeSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

enum class BotDifficulty(val label: String, val description: String) {
    EASY("Easy", "Relaxed defense, less accurate shots"),
    MEDIUM("Medium", "Accurate shots and balanced defense"),
    HARD("Hard", "Bank shots, possession and opponent replies"),
}
data class BotShot(val angle: Float, val power: Float)
data class BotMove(val playerId: String, val zone: Int)

/** Search counters for tests/device profiling; never rendered in the player HUD. */
data class BotSearchStats(
    val phase: GamePhase,
    val elapsedMillis: Double,
    val simulations: Int,
    val formations: Int,
    val replies: Int,
    val tacticalCandidates: Int,
    val budgetExhausted: Boolean,
)

/** All candidates use isolated engines, legal actions and the production physics. */
class BotPlanner(
    private val difficulty: BotDifficulty = BotDifficulty.MEDIUM,
    private val random: Random = Random.Default,
    private val maxThinkingMillis: Long? = null,
    private val onSearchCompleted: ((BotSearchStats) -> Unit)? = null,
    private val style: BotStyle = BotStyle.BALANCED,
) {
    var alternativeShot: BotShot? = null
        private set
    private val recentShots = mutableListOf<BotShot>()
    init { require(maxThinkingMillis == null || maxThinkingMillis > 0) }

    fun controls(state: GameSnapshot, team: GameTeam): Boolean {
        val kickerTeam = state.players.first { it.id == state.focusedPlayerId }.team
        return when (state.phase) {
            GamePhase.DEFENDING -> kickerTeam != team
            GamePhase.AIMING -> kickerTeam == team
            else -> false
        }
    }

    private data class Candidate(val shot: BotShot, val result: GameEngine, val value: Float)
    private data class Formation(val engine: GameEngine, val moves: List<BotMove>)
    private class SearchBudget(val limitMillis: Long, val phase: GamePhase, val simulationLimit: Int) {
        private val start = TimeSource.Monotonic.markNow()
        val elapsedMillis: Double get() = start.elapsedNow().inWholeMicroseconds / 1000.0
        val expired: Boolean get() = elapsedMillis >= limitMillis || simulations >= simulationLimit
        var simulations = 0
        var formations = 0
        var replies = 0
        var tacticalCandidates = 0
        fun stats() = BotSearchStats(phase, elapsedMillis, simulations, formations, replies, tacticalCandidates, expired)
    }

    private fun budget(engine: GameEngine): SearchBudget {
        val levelLimit = when (difficulty) { BotDifficulty.EASY -> 120L; BotDifficulty.MEDIUM -> 250L; BotDifficulty.HARD -> 650L }
        // Reserve time to apply the decision before the live phase clock expires.
        val remaining = (engine.snapshot.phaseTimeRemaining * 1000 - 300).toLong().coerceAtLeast(1L)
        return SearchBudget(minOf(maxThinkingMillis ?: levelLimit, remaining), engine.currentPhase, when (difficulty) { BotDifficulty.EASY -> 48; BotDifficulty.MEDIUM -> 88; BotDifficulty.HARD -> 140 })
    }

    suspend fun chooseShot(engine: GameEngine): BotShot {
        require(engine.currentPhase == GamePhase.AIMING)
        val budget = budget(engine)
        try {
            val chosen = chooseShot(engine, budget)
            currentCoroutineContext().ensureActive()
            recentShots.add(chosen)
            if (recentShots.size > 3) recentShots.removeAt(0)
            return chosen
        }
        finally { onSearchCompleted?.invoke(budget.stats()) }
    }

    private suspend fun chooseShot(engine: GameEngine, budget: SearchBudget): BotShot {
        currentCoroutineContext().ensureActive()
        val team = engine.snapshot.players.first { it.id == engine.snapshot.focusedPlayerId }.team
        val angles = when (difficulty) { BotDifficulty.EASY -> 8; BotDifficulty.MEDIUM -> 14; BotDifficulty.HARD -> 16 }
        val powers = when (difficulty) {
            BotDifficulty.EASY -> listOf(.75f, .5f, 1f)
            else -> listOf(.8f, .65f, 1f, .5f, .35f)
        }
        val directions = (targetAngles(engine, team, banks = difficulty != BotDifficulty.EASY) +
            (0..angles).map { -1.05f + 2.1f * it / angles }).distinct()
        val candidates = mutableListOf<Candidate>()
        val initialLimit = when (difficulty) { BotDifficulty.EASY -> 30; BotDifficulty.MEDIUM -> 58; BotDifficulty.HARD -> 44 }
        search@ for (angle in directions) for (power in powers) {
            if (budget.expired || (candidates.isNotEmpty() && budget.simulations >= initialLimit)) break@search
            val shot = BotShot(angle, power)
            val result = simulate(engine, shot, budget) ?: break@search
            val value = evaluate(result, team) - shotPenalty(engine, result, shot)
            candidates += Candidate(shot, result, value)
            // Compare goal routes too, so repeated ricochets do not win by search order.
        }
        if (candidates.isEmpty()) return BotShot(0f, .65f)
        if (difficulty == BotDifficulty.HARD) {
            val refinementLimit = 62
            val seeds = candidates.sortedByDescending { it.value }.take(3)
            refine@ for (seed in seeds) for (angleDelta in listOf(-.035f, 0f, .035f)) {
                for (powerDelta in listOf(-.04f, .04f)) {
                    if (budget.simulations >= refinementLimit) break@refine
                    val shot = BotShot((seed.shot.angle + angleDelta).coerceIn(-1.05f, 1.05f),
                        (seed.shot.power + powerDelta).coerceIn(.05f, 1f))
                    val result = simulate(engine, shot, budget) ?: break@refine
                    val value = evaluate(result, team) - shotPenalty(engine, result, shot)
                    candidates += Candidate(shot, result, value)

                }
            }
        }
        val robust = mutableListOf<Candidate>()
        for (candidate in candidates.sortedByDescending { it.value }.take(4)) {
            val outcomes = mutableListOf<Float>()
            for ((angleNoise, reactionNoise) in listOf(-.8f to .7f, .7f to -.8f, .15f to .25f)) {
                val execution = BotAimExecution.sample(candidate.shot, difficulty, angleNoise, reactionNoise)
                val outcome = simulate(engine, execution, budget) ?: break
                outcomes += evaluate(outcome, team) - shotPenalty(engine, outcome, execution)
            }
            if (outcomes.size == 3) {
                // Prefer broad opportunities; a lucky goal cannot hide high outcome variance.
                val utility = outcomes.average().toFloat() - (outcomes.max() - outcomes.min()) * .12f
                robust += candidate.copy(value = utility)
            }
            if (budget.expired) break
        }
        val ranked = (if (robust.isNotEmpty()) robust else candidates).sortedByDescending { it.value }
        var best = ranked.first()
        if (difficulty == BotDifficulty.HARD) {
            // Compare only completed tactical evaluations on the same candidate pool.
            val assessed = mutableListOf<Pair<Candidate, Float>>()
            for (candidate in ranked.take(2)) {
                if (budget.expired) break
                val reply = readyForDefense(candidate.result)
                if (reply.currentPhase != GamePhase.DEFENDING) continue
                val nextTeam = reply.snapshot.players.first { it.id == reply.snapshot.focusedPlayerId }.team
                val defender = opposite(nextTeam)
                val formations = formationBeam(reply, defender, budget, width = 2).take(2)
                if (budget.expired) break
                var formationValue: Float? = null
                var complete = true
                for (formation in formations) {
                    val attack = formation.engine.copyForSimulation()
                    attack.finishDefense()
                    val response = strongestReply(attack, nextTeam, budget)
                    if (response == null) { complete = false; break }
                    val signedResponse = if (nextTeam == team) response else -response
                    val utility = candidate.value + signedResponse * .65f
                    // The team controlling defense chooses its own best outcome.
                    formationValue = formationValue?.let {
                        if (defender == team) maxOf(it, utility) else minOf(it, utility)
                    } ?: utility
                }
                if (complete && formationValue != null) {
                    assessed += candidate to requireNotNull(formationValue)
                    budget.tacticalCandidates++
                }
            }
            if (assessed.size >= 2) best = assessed.maxBy { it.second }.first
        }
        alternativeShot = ranked.firstOrNull { it.shot != best.shot }?.shot
        val selected = when (difficulty) {
            BotDifficulty.EASY -> ranked[random.nextInt(minOf(5, ranked.size))].shot
            BotDifficulty.MEDIUM -> ranked.filter { it.value >= best.value - 40f }.take(3).let { it[random.nextInt(it.size)].shot }
            BotDifficulty.HARD -> best.shot
        }
        return selected
    }

    private fun shotPenalty(initial: GameEngine, result: GameEngine, shot: BotShot): Float {
        val contacts = (result.snapshot.soundEvents.impacts - initial.snapshot.soundEvents.impacts).coerceAtLeast(0)
        val contactCost = when (difficulty) { BotDifficulty.EASY -> 180f; BotDifficulty.MEDIUM -> 80f; BotDifficulty.HARD -> 35f }
        val repeats = recentShots.count { abs(it.angle - shot.angle) < .18f && abs(it.power - shot.power) < .15f }
        val styleCost = when (style) {
            BotStyle.PARK_BUS -> contacts * 50f + shot.power * 30f
            BotStyle.KICK_RUSH -> (1f - shot.power) * 80f
            BotStyle.TOTAL_FOOTBALL -> contacts * 20f
            BotStyle.BALANCED -> 0f
        }
        return contacts * contactCost + repeats * 90f + styleCost
    }

    /** Mirror target across the actual ball/wall contact line to seed bank shots. */
    private fun targetAngles(engine: GameEngine, team: GameTeam, banks: Boolean): List<Float> {
        val forward = if (team == GameTeam.RED) -1f else 1f
        val goalY = if (team == GameTeam.RED) engine.ballWallContactX else engine.fieldHeight - engine.ballWallContactX
        val ball = engine.snapshot.ball.position
        val targets = listOf(.5f, .4f, .6f).map { engine.fieldWidth * it }
        val contactX = engine.ballWallContactX
        val bankTargets = if (banks) targets.flatMap { listOf(2 * contactX - it, 2 * (engine.fieldWidth - contactX) - it) } else emptyList()
        return (targets + bankTargets).mapNotNull { x ->
            atan2((x - ball.x) * -forward, (goalY - ball.y) * forward).takeIf { it in -1.05f..1.05f }
        }
    }

    private suspend fun simulate(engine: GameEngine, shot: BotShot, budget: SearchBudget): GameEngine? {
        currentCoroutineContext().ensureActive()
        if (budget.expired) return null
        val simulation = engine.copyForSimulation()
        simulation.setAimAngle(shot.angle)
        simulation.setPower(shot.power)
        simulation.shoot()
        var frames = 0
        while (simulation.currentPhase == GamePhase.BALL_MOVING && frames++ < 1440) {
            if (frames % 32 == 0) {
                currentCoroutineContext().ensureActive()
                if (budget.expired) return null
            }
            simulation.advanceSimulation(1f / 120f)
        }
        budget.simulations++
        return simulation
    }

    private fun readyForDefense(engine: GameEngine): GameEngine {
        val reply = engine.copyForSimulation()
        var frames = 0
        while (reply.currentPhase == GamePhase.KICKER_APPROACH && frames++ < 120) reply.advanceSimulation(1f / 120f)
        return reply
    }

    private suspend fun strongestReply(engine: GameEngine, team: GameTeam, budget: SearchBudget): Float? {
        var strongest = -Float.MAX_VALUE
        val directions = (targetAngles(engine, team, banks = true).take(5) + listOf(-.7f, .7f)).distinct()
        for (angle in directions) for (power in listOf(.8f, 1f)) {
            val result = simulate(engine, BotShot(angle, power), budget) ?: return null
            strongest = maxOf(strongest, evaluate(result, team))
            if (strongest >= 10000f) { budget.replies++; return strongest }
        }
        budget.replies++
        return strongest
    }

    /** Beam includes staying put and legal one/two-move defensive formations. */
    private suspend fun formationBeam(engine: GameEngine, team: GameTeam, budget: SearchBudget, width: Int): List<Formation> {
        val original = Formation(engine.copyForSimulation(), emptyList())
        var beam = listOf(original)
        repeat(2) {
            val expanded = beam.toMutableList()
            for (formation in beam) {
                for (player in formation.engine.snapshot.players.filter { it.team == team && formation.engine.canMovePlayer(it.id) }) {
                    for (zone in 1..GameEngine.ZONE_COUNT) {
                        currentCoroutineContext().ensureActive()
                        if (budget.expired) return (listOf(original) + beam).distinctBy { it.moves }
                        if (!formation.engine.isAdjacentZoneForPlayer(player.id, zone)) continue
                        val trial = formation.engine.copyForSimulation()
                        if (trial.movePlayerToZone(player.id, zone)) {
                            budget.formations++
                            expanded += Formation(trial, formation.moves + BotMove(player.id, zone))
                        }
                    }
                }
            }
            beam = expanded.distinctBy { it.engine.snapshot.players.map { player -> player.position } }
                .sortedByDescending { defensiveValue(it.engine, team) }.take(width)
        }
        return (listOf(original) + beam).distinctBy { it.moves }
    }

    suspend fun chooseDefensePlan(engine: GameEngine, team: GameTeam): List<BotMove> {
        if (!controls(engine.snapshot, team) || engine.currentPhase != GamePhase.DEFENDING) return emptyList()
        val budget = budget(engine)
        try {
            currentCoroutineContext().ensureActive()
            val trial = engine.copyForSimulation()
            val fallback = buildList {
                repeat(if (difficulty == BotDifficulty.EASY) 1 else 2) {
                    if (!budget.expired) chooseDefense(trial, team)?.let { move ->
                        if (trial.movePlayerToZone(move.playerId, move.zone)) add(move)
                    }
                }
            }
            if (difficulty != BotDifficulty.HARD || budget.expired) return fallback
            val options = formationBeam(engine, team, budget, width = 4)
            var best: List<BotMove>? = null
            var bestValue = -Float.MAX_VALUE
            for (formation in options) {
                val attack = formation.engine.copyForSimulation()
                attack.finishDefense()
                val threat = strongestReply(attack, opposite(team), budget) ?: break
                val value = -threat + defensiveValue(formation.engine, team) * .1f
                if (value > bestValue) { best = formation.moves; bestValue = value }
            }
            return best ?: fallback
        } finally { onSearchCompleted?.invoke(budget.stats()) }
    }

    private fun opposite(team: GameTeam) = if (team == GameTeam.RED) GameTeam.BLUE else GameTeam.RED

    fun chooseDefense(engine: GameEngine, team: GameTeam): BotMove? {
        if (!controls(engine.snapshot, team) || engine.snapshot.phase != GamePhase.DEFENDING) return null
        val baseline = defensiveValue(engine, team)
        var bestValue = baseline
        var best: BotMove? = null
        for (player in engine.snapshot.players.filter { it.team == team && engine.canMovePlayer(it.id) }) {
            for (zone in 1..GameEngine.ZONE_COUNT) {
                if (!engine.isAdjacentZoneForPlayer(player.id, zone)) continue
                val trial = engine.copyForSimulation()
                if (!trial.movePlayerToZone(player.id, zone)) continue
                val value = defensiveValue(trial, team)
                if (value > bestValue + .001f) {
                    bestValue = value
                    best = BotMove(player.id, zone)
                }
            }
        }
        return best
    }

    private fun evaluate(engine: GameEngine, team: GameTeam): Float {
        val state = engine.snapshot
        if (state.phase == GamePhase.GOAL) return if (state.lastScoringTeam == team) 10000f else -10000f
        val forwardProgress = if (team == GameTeam.RED) 1f - state.ball.position.y / engine.fieldHeight
            else state.ball.position.y / engine.fieldHeight
        val nextTeam = state.players.first { it.id == state.focusedPlayerId }.team
        val possession = if (state.phase == GamePhase.KICKER_APPROACH) {
            if (nextTeam == team) 250f else -250f
        } else -100f // Unresolved predictions are less attractive.
        val attackingGoalY = if (team == GameTeam.RED) 0f else engine.fieldHeight
        val lateralPenalty = abs(state.ball.position.x - engine.fieldWidth / 2f) / engine.fieldWidth * 35f
        val goalDistance = (state.ball.position - Vector2(engine.fieldWidth / 2f, attackingGoalY)).length() / engine.fieldHeight
        val styleBonus = when (style) {
            BotStyle.PARK_BUS -> possession * .5f - if (nextTeam != team) forwardProgress * 100f else 0f
            BotStyle.KICK_RUSH -> forwardProgress * 160f - goalDistance * 40f
            BotStyle.TOTAL_FOOTBALL -> possession * .8f + state.players.filter { it.team == team }
                .sortedBy { (it.position - state.ball.position).length() }.take(2)
                .sumOf { (1f - (it.position - state.ball.position).length() / engine.fieldHeight).coerceIn(0f, 1f).toDouble() }.toFloat() * 60f
            BotStyle.BALANCED -> 0f
        }
        val scoreLead = if (team == GameTeam.RED) state.redScore - state.blueScore else state.blueScore - state.redScore
        val momentum = scoreLead.coerceIn(-2,2) * (possession * .1f - forwardProgress * 25f)
        return momentum + styleBonus + possession + forwardProgress * 180f - goalDistance * 40f - lateralPenalty -
            (if (nextTeam != team) (1f - forwardProgress) * 200f else 0f)
    }

    private fun defensiveValue(engine: GameEngine, team: GameTeam): Float {
        val state = engine.snapshot
        val goalY = if (team == GameTeam.BLUE) 0f else engine.fieldHeight
        val defenders = state.players.filter { it.team == team }
        // Cover three goal lanes; only players between ball and goal can block them.
        val lanes = when (difficulty) {
            BotDifficulty.EASY -> listOf(.5f)
            BotDifficulty.MEDIUM -> listOf(.4f, .5f, .6f)
            BotDifficulty.HARD -> listOf(.38f, .44f, .5f, .56f, .62f)
        }
        val coverage = lanes.sumOf { goalX ->
            val end = Vector2(engine.fieldWidth * goalX, goalY)
            val segment = end - state.ball.position
            val lengthSquared = segment.x * segment.x + segment.y * segment.y
            val closest = defenders.minOf { player ->
                val offset = player.position - state.ball.position
                val t = if (lengthSquared > 0f) (offset.x * segment.x + offset.y * segment.y) / lengthSquared else 0f
                if (t !in .05f..1f) engine.fieldWidth
                else (player.position - (state.ball.position + segment * t)).length()
            }
            (1f - closest / 100f).coerceIn(0f, 1f).toDouble()
        }.toFloat()
        val anchor = defenders.minOf { (it.position - Vector2(engine.fieldWidth / 2, goalY)).length() }
        val ownGoal = Vector2(engine.fieldWidth / 2, goalY)
        val depth = defenders.sumOf { ((it.position - ownGoal).length() / engine.fieldHeight).toDouble() }.toFloat() / defenders.size
        val nearestBall = defenders.minOf { (it.position - state.ball.position).length() } / engine.fieldHeight
        val spread = (defenders.maxOf { it.position.x } - defenders.minOf { it.position.x }) / engine.fieldWidth
        val styleBonus = when (style) {
            BotStyle.PARK_BUS -> coverage * 55f - depth * 120f
            BotStyle.KICK_RUSH -> -nearestBall * 100f + depth * 35f
            BotStyle.TOTAL_FOOTBALL -> -nearestBall * 75f + spread * 35f + coverage * 20f
            BotStyle.BALANCED -> 0f
        }
        val nextBallPosition = if (difficulty == BotDifficulty.EASY) 0f else (1f - nearestBall).coerceIn(0f,1f) * 30f
        return nextBallPosition + styleBonus + coverage * 100f - anchor / engine.fieldHeight * 25f
    }
}

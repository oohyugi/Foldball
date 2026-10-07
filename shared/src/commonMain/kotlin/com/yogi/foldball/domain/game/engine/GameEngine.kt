package com.yogi.foldball.domain.game.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector2(val x: Float, val y: Float) {
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(scale: Float) = Vector2(x * scale, y * scale)
    fun length() = sqrt(x * x + y * y)
}

enum class GameTeam { RED, BLUE }

enum class MatchFormat(val playersPerTeam: Int, val label: String, val description: String) {
    THREE_V_THREE(3, "3v3", "More space and faster attacks"),
    FIVE_V_FIVE(5, "5v5", "More players and tighter defense")
}
enum class GamePhase { DEFENDING, AIMING, BALL_MOVING, TURNOVER_RETREAT, KICKER_APPROACH, GOAL, MATCH_OVER }

/** Chosen when a match starts; a future settings screen can supply different values. */
data class MatchTimingConfig(
    val defenseSeconds: Int = 10,
    val attackSeconds: Int = 8,
    val matchSeconds: Int = 180
) {
    init {
        require(matchSeconds in 1..3600) { "Match time must be 1–3600 seconds" }
        require(defenseSeconds in 1..120) { "Defense time must be 1–120 seconds" }
        require(attackSeconds in 1..120) { "Attack time must be 1–120 seconds" }
    }
}

data class MatchPlayer(
    val id: String,
    val team: GameTeam,
    val position: Vector2,
    val home: Vector2,
    val velocity: Vector2 = Vector2(0f, 0f),
    val impactDirectionX: Float = 0f,
    val impactDirectionY: Float = 0f,
    val impactStrength: Float = 0f,
    val condition: StandeeCondition = StandeeCondition()
)

data class MatchBall(
    val position: Vector2,
    val velocity: Vector2,
    val radius: Float,
    val height: Float,
    val verticalVelocity: Float,
    val rotationX: Float,
    val rotationY: Float,
    val impactSideX: Float = 0f,
    val impactSideY: Float = 0f,
    val impactStrength: Float = 0f,
    val landingStrength: Float = 0f,
    val goalPostImpactStrength: Float = 0f
)

data class SoundEvents(
    val kicks: Int = 0,
    val impacts: Int = 0,
    val bounces: Int = 0,
    val posts: Int = 0,
    val goals: Int = 0,
    val blocks: Int = 0,
    val crossbars: Int = 0,
    val overbars: Int = 0
)

data class GameSnapshot(
    val players: List<MatchPlayer>,
    val ball: MatchBall,
    val phase: GamePhase,
    val phaseTimeRemaining: Float,
    val phaseTimeTotal: Float,
    val focusedPlayerId: String,
    val aimAngle: Float,
    val power: Float,
    val redScore: Int,
    val blueScore: Int,
    val lastScoringTeam: GameTeam?,
    val kickoffTeam: GameTeam,
    val winner: GameTeam?,
    val openingKickoffPending: Boolean,
    val lastShotPower: Float,
    val soundEvents: SoundEvents = SoundEvents()
)

private data class PlayerCollision(
    val player: MatchPlayer,
    val time: Float,
    val center: Vector2,
    val normal: Vector2,
    val contactPoint: Vector2
)

private data class BallMotion(
    val position: Vector2,
    val velocity: Vector2,
    val height: Float,
    val verticalVelocity: Float
)

private data class GoalLineCrossing(
    val team: GameTeam,
    val lineY: Float,
    val x: Float,
    val ballHeight: Float
)

/** Deterministic, platform-independent movement and ball physics for the match feature. */
class GameEngine(
    private val width: Float,
    private val height: Float,
    private val targetScore: Int? = 3,
    private val timing: MatchTimingConfig = MatchTimingConfig(),
    initialKickoffTeam: GameTeam = GameTeam.RED,
    private val dentingEnabled: Boolean = false,
    private val format: MatchFormat? = null,
    private val defensiveBlockWinsPossession: Boolean = false
) {
    private var matchTimeExpired = false
    private val ballRadius = 10f
    private val playerRadius = 25f
    private val standeeCollisionHeight = 52f

    // The standee artwork is wider than its ball collider. Keep visible bodies apart
    // without changing how the ball contacts a player.
    private val playerSpacing = 68f
    private val playerCollisionOverlap = 3f

    // Physical model: a 7 cm, 2 g loose paper ball on a roughly 1.65 m field.
    // Rendering and collision coordinates use dp; forces below use SI units.
    private val metersPerDp = .0035f
    private val ballMassKg = .002f
    private val standeeMassKg = .020f
    private val airDensityKgPerCubicMeter = 1.2f
    private val dragCoefficient = .7f
    private val gravityMetersPerSecondSquared = 9.81f
    private val rollingResistanceCoefficient = .10f
    private val ballRadiusMeters = ballRadius * metersPerDp
    private val crossSectionAreaSquareMeters = PI.toFloat() * ballRadiusMeters * ballRadiusMeters

    // Creased paper still rebounds, but it loses more energy than a hard ball.
    private val playerBounceRestitution = .50f
    private val playerBounceTangentialRetention = .78f
    private val playerMassRatio = standeeMassKg / ballMassKg
    private val playerContactCooldown = .12f
    private val playerImpactHopFraction = .45f
    private val maxPlayerImpactHopSpeed = 280f
    private val groundBounceRestitution = .18f
    private val wallBounceRestitution = .45f
    private val wallTangentialRetention = .72f
    private val kickerVisualGap = 1f
    private val playerCollisionDistance = ballRadius + playerRadius + playerCollisionOverlap
    private val possessionTieThreshold = 6f
    private val maxDefensiveMoves = if (format == null) 2 else 1
    private val movementRadius = 100f
    private val wallInset = 18f
    private val redAttack = -1f
    private val referenceFieldHeight = 580f
    private val goalCelebrationDuration = 2.2f
    private val retreatDuration = .4f
    private val ballPreparationDuration = .22f
    private val approachDuration = .36f

    private var players = createRoster()

    private fun createRoster(): List<MatchPlayer> {
        val zones = when (format) {
            null -> return listOf(
                player("red-left", GameTeam.RED, 4, offsetTowardAttackRows = .2f),
                player("red-center", GameTeam.RED, 2, offsetTowardAttackRows = .33f),
                player("red-right", GameTeam.RED, 6, offsetTowardAttackRows = .2f),
                player("red-striker", GameTeam.RED, 8),
                player("blue-left", GameTeam.BLUE, 13, offsetTowardAttackRows = .2f),
                player("blue-center", GameTeam.BLUE, 17, offsetTowardAttackRows = .33f),
                player("blue-right", GameTeam.BLUE, 15, offsetTowardAttackRows = .2f),
                player("blue-striker", GameTeam.BLUE, 11)
            )

            MatchFormat.THREE_V_THREE -> listOf("left" to 4, "right" to 6, "striker" to 8)

            MatchFormat.FIVE_V_FIVE -> listOf(
                "left" to 4,
                "center" to 2,
                "right" to 6,
                "midfielder" to 5,
                "striker" to 8
            )
        }
        return GameTeam.entries.flatMap { team ->
            zones.map { (role, redZone) ->
                val zone = if (team == GameTeam.RED) redZone else ZONE_COUNT + 1 - redZone
                val offset = when (role) {
                    "left", "right" -> .2f
                    "center" -> .33f
                    else -> 0f
                }
                player("${team.name.lowercase()}-$role", team, zone, offsetTowardAttackRows = offset)
            }
        }
    }

    // Immutable formation anchors. A player's mutable home follows defensive moves
    // during a rally, but every kickoff starts from these original positions.
    private val startingFormationHomeById = players.associate { it.id to it.home }
    private var ball =
        MatchBall(Vector2(width * .5f, height * .5f), Vector2(0f, 0f), ballRadius, 0f, 0f, 0f, 0f)
    private var phase = GamePhase.DEFENDING
    private var phaseTimeRemaining = timing.defenseSeconds.toFloat()
    private var focusedPlayerId = if (initialKickoffTeam ==
        GameTeam.RED
    ) {
        "red-striker"
    } else {
        "blue-striker"
    }
    private var lastKickingTeam: GameTeam? = null
    private var defenderBlockedShot = false
    private var launchingKickerId: String? = null
    private var aimAngle = 0f
    private var power = 0f
    private var soundEvents = SoundEvents()
    private var lastShotPower = 0f
    private var redScore = 0
    private var blueScore = 0
    private var lastScoringTeam: GameTeam? = null
    private var kickoffTeam = initialKickoffTeam
    private var openingKickoffPending = true
    private var winner: GameTeam? = null
    private var goalCelebrationElapsed = 0f
    private var approachFrom: Vector2? = null
    private var approachTo: Vector2? = null
    private var approachBallFrom: Vector2? = null
    private var approachBallTo: Vector2? = null
    private var approachElapsed = 0f
    private var retreatFrom: Vector2? = null
    private var retreatTo: Vector2? = null
    private var retreatElapsed = 0f
    private var turnoverNextKickerId: String? = null
    private val contactCooldownByPlayerId = mutableMapOf<String, Float>()
    private var shotKickerAnchor: Vector2? = null
    private var playerDragId: String? = null
    private var playerDragStart = Vector2(0f, 0f)
    private var playerDragStartState: MatchPlayer? = null
    private var playerDragDelta = Vector2(0f, 0f)
    private var playerDragStartZone: Int? = null
    private val playersMovedThisTurn = mutableSetOf<String>()
    private val defensiveMoveHistory = mutableListOf<MatchPlayer>()

    init {
        players = players.map { player ->
            val kickoffHome = when (player.id) {
                "red-striker" -> strikerKickoffPosition(
                    8,
                    GameTeam.RED,
                    initialKickoffTeam == GameTeam.RED
                )

                "blue-striker" -> strikerKickoffPosition(
                    11,
                    GameTeam.BLUE,
                    initialKickoffTeam == GameTeam.BLUE
                )

                else -> player.home
            }
            player.copy(position = kickoffHome, home = kickoffHome)
        }
        moveFocusedBehindBall()
    }

    /** Independent copy for bot planning; immutable player/ball values may be shared. */
    fun copyForSimulation(): GameEngine {
        val copy = GameEngine(
            width,
            height,
            targetScore,
            timing,
            kickoffTeam,
            dentingEnabled,
            format,
            defensiveBlockWinsPossession
        )
        copy.matchTimeExpired = matchTimeExpired
        copy.players = players
        copy.ball = ball
        copy.phase = phase
        copy.phaseTimeRemaining = phaseTimeRemaining
        copy.focusedPlayerId = focusedPlayerId
        copy.lastKickingTeam = lastKickingTeam
        copy.defenderBlockedShot = defenderBlockedShot
        copy.launchingKickerId = launchingKickerId
        copy.aimAngle = aimAngle
        copy.power = power
        copy.soundEvents = soundEvents
        copy.lastShotPower = lastShotPower
        copy.redScore = redScore
        copy.blueScore = blueScore
        copy.lastScoringTeam = lastScoringTeam
        copy.kickoffTeam = kickoffTeam
        copy.openingKickoffPending = openingKickoffPending
        copy.winner = winner
        copy.goalCelebrationElapsed = goalCelebrationElapsed
        copy.approachFrom = approachFrom
        copy.approachTo = approachTo
        copy.approachBallFrom = approachBallFrom
        copy.approachBallTo = approachBallTo
        copy.approachElapsed = approachElapsed
        copy.retreatFrom = retreatFrom
        copy.retreatTo = retreatTo
        copy.retreatElapsed = retreatElapsed
        copy.turnoverNextKickerId = turnoverNextKickerId
        copy.shotKickerAnchor = shotKickerAnchor
        copy.playerDragId = playerDragId
        copy.playerDragStart = playerDragStart
        copy.playerDragStartState = playerDragStartState
        copy.playerDragDelta = playerDragDelta
        copy.playerDragStartZone = playerDragStartZone
        copy.contactCooldownByPlayerId.putAll(contactCooldownByPlayerId)
        copy.playersMovedThisTurn.addAll(playersMovedThisTurn)
        copy.defensiveMoveHistory.addAll(defensiveMoveHistory)
        return copy
    }

    val ballWallContactX: Float get() = wallInset + ballRadius
    val fieldWidth: Float get() = width
    val fieldHeight: Float get() = height

    fun setAimAngle(value: Float) {
        if (phase == GamePhase.AIMING) {
            aimAngle = value.coerceIn(-AimDirection.maxAngleRadians, AimDirection.maxAngleRadians)
        }
    }

    /** Use the same reachable destination and move accounting as a human drag. */
    fun movePlayerToZone(id: String, zone: Int): Boolean {
        if (!isAdjacentZoneForPlayer(id, zone)) return false
        val destination = legalPlayerPositionInZone(id, zone) ?: return false
        val before = defensiveMovesUsed
        val origin = players.first { it.id == id }.position
        beginPlayerMove(id)
        moveInactivePlayer(id, destination - origin)
        endPlayerMove(id)
        return defensiveMovesUsed > before
    }

    val snapshot: GameSnapshot
        get() = GameSnapshot(
            players = players.toList(),
            ball = ball,
            phase = phase,
            phaseTimeRemaining = phaseTimeRemaining,
            phaseTimeTotal = when (phase) {
                GamePhase.DEFENDING -> timing.defenseSeconds.toFloat()
                GamePhase.AIMING -> timing.attackSeconds.toFloat()
                else -> 0f
            },
            focusedPlayerId = focusedPlayerId,
            aimAngle = aimAngle,
            power = power,
            redScore = redScore,
            blueScore = blueScore,
            lastScoringTeam = lastScoringTeam,
            kickoffTeam = kickoffTeam,
            winner = winner,
            openingKickoffPending = openingKickoffPending,
            lastShotPower = lastShotPower,
            soundEvents = soundEvents
        )

    fun beginPlayerMove(id: String) {
        val player = players.firstOrNull { it.id == id } ?: return
        if (!canMovePlayer(id)) return
        playerDragId = id
        playerDragStart = player.position
        playerDragStartState = player
        playerDragDelta = Vector2(0f, 0f)
        playerDragStartZone = zoneAt(player.position)
    }

    fun moveInactivePlayer(id: String, delta: Vector2) {
        if (phase != GamePhase.DEFENDING || playerDragId != id) return
        val player = players.firstOrNull { it.id == id } ?: return
        if (player.team == focusedPlayer().team) return
        val blockedTravel = playerDragStart + playerDragDelta - player.position
        if (blockedTravel.x * delta.x + blockedTravel.y * delta.y < 0f) {
            playerDragDelta = player.position - playerDragStart
        }
        playerDragDelta += delta
        val proposed = playerDragStart + playerDragDelta
        val movement = proposed - player.position
        val steps = ceil(movement.length() / 3f).toInt().coerceAtLeast(1)
        var safe = player.position
        for (step in 1..steps) {
            val candidate = player.position + movement * (step.toFloat() / steps)
            if (!isLegalPlayerDragStep(id, safe, candidate)) break
            safe = candidate
        }
        if (safe != player.position) {
            players = players.map {
                if (it.id ==
                    id
                ) {
                    it.copy(position = safe, home = safe, velocity = Vector2(0f, 0f))
                } else {
                    it
                }
            }
        }
    }

    fun endPlayerMove(id: String) {
        if (playerDragId == id) {
            val finalPosition = players.firstOrNull { it.id == id }?.position
            if (finalPosition != null && (finalPosition - playerDragStart).length() > .5f) {
                playersMovedThisTurn += id
                playerDragStartState?.let { defensiveMoveHistory += it }
            }
            playerDragId = null
            playerDragStartState = null
            playerDragDelta = Vector2(0f, 0f)
            playerDragStartZone = null
            if (format != null && phase == GamePhase.DEFENDING &&
                defensiveMovesUsed >= maxDefensiveMoves
            ) {
                finishDefense()
            }
        }
    }

    val defensiveMovesUsed: Int get() = defensiveMoveHistory.size
    val defensiveMoveLimit: Int get() = maxDefensiveMoves

    fun canUndoPlayerMove(): Boolean = phase == GamePhase.DEFENDING && playerDragId == null && defensiveMoveHistory.isNotEmpty()

    fun undoLastPlayerMove(): Boolean {
        if (!canUndoPlayerMove()) return false
        val previous = defensiveMoveHistory.removeAt(defensiveMoveHistory.lastIndex)
        players = players.map { if (it.id == previous.id) previous else it }
        playersMovedThisTurn.remove(previous.id)
        return true
    }

    fun canMovePlayer(id: String): Boolean {
        val player = players.firstOrNull { it.id == id } ?: return false
        return phase == GamePhase.DEFENDING && phaseTimeRemaining > 0f && id != focusedPlayerId &&
            player.team != focusedPlayer().team &&
            playersMovedThisTurn.size < maxDefensiveMoves && id !in playersMovedThisTurn
    }

    fun finishDefense(): Boolean {
        if (phase != GamePhase.DEFENDING) return false
        playerDragId?.let {
            endPlayerMove(it)
            if (phase != GamePhase.DEFENDING) return true
        }
        phase = GamePhase.AIMING
        phaseTimeRemaining = timing.attackSeconds.toFloat()
        return true
    }

    fun isZoneAvailableForPlayer(id: String, zone: Int): Boolean {
        val player = players.firstOrNull { it.id == id } ?: return false
        if (!canMovePlayer(id) && playerDragId != id) return false
        if (openingKickoffPending && !isInOwnHalf(player.team, zone)) return false
        val originZone = if (playerDragId == id) playerDragStartZone else zoneAt(player.position)
        if (originZone == null ||
            (zone != originZone && !areAdjacentZones(originZone, zone))
        ) {
            return false
        }
        return players.none { it.id != id && zoneAt(it.position) == zone }
    }

    fun isAdjacentZoneForPlayer(id: String, zone: Int): Boolean {
        if (zone !in 1..ZONE_COUNT) return false
        val player = players.firstOrNull { it.id == id } ?: return false
        val originZone = if (playerDragId == id) playerDragStartZone else zoneAt(player.position)
        return originZone != null && zone != originZone && areAdjacentZones(originZone, zone)
    }

    /** Returns a collision-safe destination inside the requested zone, if one exists. */
    fun legalPlayerPositionInZone(id: String, zone: Int): Vector2? {
        if (!isZoneAvailableForPlayer(id, zone)) return null
        val player = players.first { it.id == id }
        val index = zone - 1
        val column = index % ZONE_COLUMNS
        val rowFromBottom = index / ZONE_COLUMNS
        val cellWidth = width / ZONE_COLUMNS
        val cellHeight = height / ZONE_ROWS
        val left = column * cellWidth
        val top = height - (rowFromBottom + 1) * cellHeight
        val pathStart = if (playerDragId == id) playerDragStart else player.position
        val samples = floatArrayOf(.5f, .35f, .65f, .2f, .8f)
        for (xFraction in samples) {
            for (yFraction in samples) {
                val point = Vector2(left + cellWidth * xFraction, top + cellHeight * yFraction)
                if (isLegalPlayerDragPosition(id, point) &&
                    hasClearPlayerDragPath(id, pathStart, point)
                ) {
                    return point
                }
            }
        }
        return null
    }

    fun canMovePlayerToZone(id: String, zone: Int): Boolean = legalPlayerPositionInZone(id, zone) != null

    fun isZoneOccupied(zone: Int, exceptPlayerId: String? = null): Boolean = players.any { it.id != exceptPlayerId && zoneAt(it.position) == zone }

    private fun isInOwnHalf(team: GameTeam, zone: Int): Boolean {
        if (zone !in 1..ZONE_COUNT) return false
        val rowFromBottom = (zone - 1) / ZONE_COLUMNS
        return if (team == GameTeam.RED) {
            rowFromBottom < ZONE_ROWS / 2
        } else {
            rowFromBottom >= ZONE_ROWS / 2
        }
    }

    private fun isLegalPlayerDragPosition(id: String, position: Vector2): Boolean {
        val zone = zoneAt(position) ?: return false
        if (!isZoneAvailableForPlayer(id, zone)) return false
        if (position.x !in playerRadius..(width - playerRadius) ||
            position.y !in playerRadius..(height - playerRadius)
        ) {
            return false
        }
        if ((position - ball.position).length() <
            playerCollisionDistance + kickerVisualGap
        ) {
            return false
        }
        return players.none { it.id != id && (it.position - position).length() < playerSpacing }
    }

    /** Permit escaping an already overlapping start, never moving deeper into it. */
    private fun isLegalPlayerDragStep(id: String, from: Vector2, to: Vector2): Boolean {
        val zone = zoneAt(to) ?: return false
        if (positionOutsidePlayerBounds(to)) return false
        if (zone != playerDragStartZone && !isZoneAvailableForPlayer(id, zone)) return false
        fun clearsOrEscapes(center: Vector2, required: Float): Boolean {
            val before = (from - center).length()
            val after = (to - center).length()
            return after >= required || (before < required && after > before + .0001f)
        }
        if (!clearsOrEscapes(ball.position, playerCollisionDistance + kickerVisualGap)) return false
        return players.all { it.id == id || clearsOrEscapes(it.position, playerSpacing) }
    }
    private fun positionOutsidePlayerBounds(point: Vector2) = point.x !in playerRadius..(width - playerRadius) ||
        point.y !in playerRadius..(height - playerRadius)

    private fun hasClearPlayerDragPath(id: String, from: Vector2, to: Vector2): Boolean {
        val displacement = to - from
        val steps = ceil(displacement.length() / 3f).toInt().coerceAtLeast(1)
        return (1..steps).all { step ->
            isLegalPlayerDragPosition(id, from + displacement * (step.toFloat() / steps))
        }
    }

    fun aimBy(deltaX: Float) {
        if (phase == GamePhase.AIMING) {
            aimAngle = (aimAngle + deltaX / 120f)
                .coerceIn(-AimDirection.maxAngleRadians, AimDirection.maxAngleRadians)
        }
    }

    fun chargeBy(deltaY: Float) {
        if (phase == GamePhase.AIMING) power = (power + deltaY / 68f).coerceIn(0f, 1f)
    }

    fun setPower(value: Float) {
        if (phase == GamePhase.AIMING) power = value.coerceIn(0f, 1f)
    }

    fun shoot(): Boolean {
        if (phase != GamePhase.AIMING || power <= .01f) return false
        val player = players.first { it.id == focusedPlayerId }
        soundEvents = soundEvents.copy(kicks = soundEvents.kicks + 1)
        lastShotPower = power
        openingKickoffPending = false
        lastKickingTeam = player.team
        defenderBlockedShot = false
        launchingKickerId = player.id
        shotKickerAnchor = player.position
        // The ball starts only 1 dp outside the kicker's collision radius. Ignore
        // that player briefly so the launch cannot immediately rebound backward.
        contactCooldownByPlayerId[player.id] = playerContactCooldown
        val direction = AimDirection.shotVector(player.team, aimAngle)
        // Cap horizontal launch speed independently of field length; high power adds loft.
        val strength = if (dentingEnabled) player.condition.shotStrength else 1f
        val appliedPower = power * power
        val speed = (120f + appliedPower * 1_680f) * strength
        ball = ball.copy(
            velocity = direction * speed,
            // Soft touches roll; high power increasingly turns into loft.
            // Full power close to goal can cross above the opening instead of scoring.
            verticalVelocity = power * power * power * power * power * 3_200f * strength,
            impactSideX = 0f,
            impactSideY = 0f,
            impactStrength = 0f,
            goalPostImpactStrength = 0f
        )
        phase = GamePhase.BALL_MOVING
        phaseTimeRemaining = 0f
        power = 0f
        return true
    }

    /** Stop new turns; an in-flight final shot and its goal celebration may finish. */
    fun expireMatchTime() {
        matchTimeExpired = true
        if (phase != GamePhase.BALL_MOVING && phase != GamePhase.GOAL) finishTimedMatch()
    }
    private fun finishTimedMatch() {
        winner =
            when {
                redScore > blueScore -> GameTeam.RED
                blueScore > redScore -> GameTeam.BLUE
                else -> null
            }
        phase = GamePhase.MATCH_OVER
        phaseTimeRemaining = 0f
        playerDragId = null
    }

    /** Advances simulation using seconds; callers should use a frame clock, not an animation timer. */
    fun step(deltaSeconds: Float): GameSnapshot {
        advanceSimulation(deltaSeconds)
        return snapshot
    }

    val currentPhase: GamePhase get() = phase
    val focusedDentProgress: Float get() = if (dentingEnabled) focusedPlayer().condition.progress else 0f

    /** Same integration as step, without allocating a presentation snapshot. */
    fun advanceSimulation(deltaSeconds: Float) {
        if (phase == GamePhase.DEFENDING || phase == GamePhase.AIMING) {
            advanceStep(deltaSeconds.coerceIn(0f, .25f))
            return
        }
        val frameTime = deltaSeconds.coerceIn(0f, 1f / 30f)
        if (frameTime <= 0f) return
        val substepCount = ceil(frameTime / maxPhysicsStep).toInt().coerceAtLeast(1)
        val dt = frameTime / substepCount
        repeat(substepCount) { advanceStep(dt) }
    }

    private val maxPhysicsStep = 1f / 120f

    private fun focusedPlayer() = players.first { it.id == focusedPlayerId }

    private fun zoneAt(position: Vector2): Int? {
        if (position.x !in 0f..width || position.y !in 0f..height) return null
        val column = (position.x / (width / ZONE_COLUMNS)).toInt().coerceIn(0, ZONE_COLUMNS - 1)
        val rowFromTop = (position.y / (height / ZONE_ROWS)).toInt().coerceIn(0, ZONE_ROWS - 1)
        val rowFromBottom = ZONE_ROWS - 1 - rowFromTop
        return rowFromBottom * ZONE_COLUMNS + column + 1
    }

    private fun zoneCenter(zone: Int): Vector2 {
        val index = zone - 1
        val column = index % ZONE_COLUMNS
        val rowFromBottom = index / ZONE_COLUMNS
        return Vector2(
            (column + .5f) * width / ZONE_COLUMNS,
            height - (rowFromBottom + .5f) * height / ZONE_ROWS
        )
    }

    private fun areAdjacentZones(first: Int, second: Int): Boolean {
        if (first !in 1..ZONE_COUNT || second !in 1..ZONE_COUNT) return false
        val firstIndex = first - 1
        val secondIndex = second - 1
        val columnDistance = kotlin.math.abs(firstIndex % ZONE_COLUMNS - secondIndex % ZONE_COLUMNS)
        val rowDistance = kotlin.math.abs(firstIndex / ZONE_COLUMNS - secondIndex / ZONE_COLUMNS)
        // One move may reach any of the eight surrounding zones, including diagonals.
        return columnDistance <= 1 && rowDistance <= 1
    }

    private fun advanceStep(dt: Float) {
        if (matchTimeExpired && phase != GamePhase.BALL_MOVING && phase != GamePhase.GOAL) {
            finishTimedMatch()
            return
        }
        if (phase == GamePhase.DEFENDING) {
            phaseTimeRemaining = (phaseTimeRemaining - dt).coerceAtLeast(0f)
            if (phaseTimeRemaining == 0f) finishDefense()
            return
        }
        if (phase == GamePhase.AIMING) {
            phaseTimeRemaining = (phaseTimeRemaining - dt).coerceAtLeast(0f)
            if (phaseTimeRemaining == 0f) {
                if (power > .01f) shoot() else beginTimeoutTurnover()
            }
            return
        }
        if (phase == GamePhase.GOAL) {
            goalCelebrationElapsed += dt
            if (goalCelebrationElapsed >= goalCelebrationDuration) {
                if (matchTimeExpired) {
                    finishTimedMatch()
                } else if (winner != null) {
                    phase = GamePhase.MATCH_OVER
                } else {
                    beginKickoff(kickoffTeam)
                }
            }
            return
        }
        if (phase == GamePhase.TURNOVER_RETREAT) {
            retreatElapsed += dt
            val progress = (retreatElapsed / retreatDuration).coerceIn(0f, 1f)
            val eased = progress * progress * (3f - 2f * progress)
            val from = retreatFrom ?: return
            val to = retreatTo ?: return
            players = players.map {
                if (it.id ==
                    focusedPlayerId
                ) {
                    it.copy(position = from + (to - from) * eased, velocity = Vector2(0f, 0f))
                } else {
                    it
                }
            }
            separatePlayerBodies(protectedId = focusedPlayerId)
            if (progress >= 1f) {
                players = players.map { if (it.id == focusedPlayerId) it.copy(home = to) else it }
                focusedPlayerId = turnoverNextKickerId ?: return
                turnoverNextKickerId = null
                retreatFrom = null
                retreatTo = null
                beginKickerApproach(preserveBallPosition = true)
            }
            return
        }
        if (phase == GamePhase.KICKER_APPROACH) {
            approachElapsed += dt
            val ballProgress = (approachElapsed / ballPreparationDuration).coerceIn(0f, 1f)
            val ballEased = ballProgress * ballProgress * (3f - 2f * ballProgress)
            val progress = ((approachElapsed - ballPreparationDuration) / approachDuration)
                .coerceIn(0f, 1f)
            val playerEased = progress * progress * (3f - 2f * progress)
            val from = approachFrom ?: return
            val to = approachTo ?: return
            val position = from + (to - from) * playerEased
            players = players.map {
                if (it.id ==
                    focusedPlayerId
                ) {
                    it.copy(position = position, velocity = Vector2(0f, 0f))
                } else {
                    it
                }
            }
            separatePlayerBodies(protectedId = focusedPlayerId)
            val ballFrom = approachBallFrom ?: return
            val ballTo = approachBallTo ?: return
            ball = ball.copy(position = ballFrom + (ballTo - ballFrom) * ballEased)
            if (progress >= 1f) {
                // Formation starts at the settled positions, including the previous
                // kicker and bodies displaced during approach. Stale home anchors
                // must not pull them back when the next shot activates physics.
                players = players.map { it.copy(home = it.position, velocity = Vector2(0f, 0f)) }
                phase = GamePhase.DEFENDING
                phaseTimeRemaining = timing.defenseSeconds.toFloat()
                approachFrom = null
                approachTo = null
                approachBallFrom = null
                approachBallTo = null
            }
            return
        }
        if (phase != GamePhase.BALL_MOVING) return

        advancePlayers(dt)
        contactCooldownByPlayerId.keys.toList().forEach { id ->
            val remaining = (contactCooldownByPlayerId[id] ?: 0f) - dt
            if (remaining > 0f) {
                contactCooldownByPlayerId[id] = remaining
            } else {
                contactCooldownByPlayerId.remove(id)
            }
        }

        val previousPosition = ball.position
        val motion = integrateBallMotion(ball, dt)
        var position = motion.position
        var velocity = motion.velocity
        var heightAboveGround = motion.height
        var verticalVelocity = motion.verticalVelocity
        val rotationX = ball.rotationX + velocity.y / ballRadius * dt
        val rotationY = ball.rotationY - velocity.x / ballRadius * dt
        var impactSideX = ball.impactSideX
        var impactSideY = ball.impactSideY
        var impactStrength = (ball.impactStrength - dt * 3.2f).coerceAtLeast(0f)
        var landingStrength = (ball.landingStrength - dt * 4.8f).coerceAtLeast(0f)
        var goalPostImpactStrength = (ball.goalPostImpactStrength - dt * 4f).coerceAtLeast(0f)
        if (impactStrength == 0f) {
            impactSideX = 0f
            impactSideY = 0f
        }

        val minX = wallInset + ballRadius
        val maxX = width - wallInset - ballRadius
        val minY = wallInset + ballRadius
        val maxY = height - wallInset - ballRadius
        if (position.x < minX || position.x > maxX) {
            soundEvents = soundEvents.copy(impacts = soundEvents.impacts + 1)
            impactSideX = if (position.x > maxX) 1f else -1f
            impactSideY = 0f
            impactStrength = (abs(velocity.x) / 600f).coerceIn(.25f, 1f)
            position = position.copy(x = position.x.coerceIn(minX, maxX))
            velocity =
                Vector2(-velocity.x * wallBounceRestitution, velocity.y * wallTangentialRetention)
            verticalVelocity *= wallTangentialRetention
        }
        if (heightAboveGround < 0f) {
            heightAboveGround = 0f
            val landingSpeed = abs(verticalVelocity)
            val landedFromAir = ball.height > .01f && ball.verticalVelocity < 0f
            val bounceSpeed = landingSpeed * groundBounceRestitution
            verticalVelocity = if (bounceSpeed * metersPerDp > .18f) bounceSpeed else 0f
            if (landedFromAir) {
                soundEvents = soundEvents.copy(bounces = soundEvents.bounces + 1)
                landingStrength = (landingSpeed / 260f).coerceIn(.18f, 1f)
                // Landing dissipates planar energy too; gentle hops retain more momentum.
                val landingRetention = 1f - (landingSpeed / 600f).coerceIn(0f, 1f) * .18f
                velocity = velocity * landingRetention
            }
        }

        // Height is separate from field position. Sweep the same coordinates
        // used to draw the ball, so both teams make contact at equal distances.
        val topGoalLine = wallInset + ballRadius
        val bottomGoalLine = height - wallInset - ballRadius
        val goalCrossing =
            goalLineCrossing(
                previousPosition,
                position,
                ball.height,
                heightAboveGround,
                topGoalLine,
                bottomGoalLine
            )
        val goalHalfWidth = width * .16f - ballRadius
        val crossingOffsetX = goalCrossing?.let { it.x - width * .5f }
        val isGoal = crossingOffsetX != null && abs(crossingOffsetX) <= goalHalfWidth &&
            GoalGeometry.clearsOpening(goalCrossing.ballHeight, ballRadius)
        if (goalCrossing != null && crossingOffsetX != null && abs(
                crossingOffsetX
            ) <= goalHalfWidth &&
            goalCrossing.ballHeight > GoalGeometry.openingHeight
        ) {
            soundEvents = soundEvents.copy(overbars = soundEvents.overbars + 1)
        }
        if (isGoal) {
            soundEvents = soundEvents.copy(goals = soundEvents.goals + 1)
            val scoringTeam = goalCrossing.team
            lastScoringTeam = scoringTeam
            kickoffTeam = if (scoringTeam == GameTeam.RED) GameTeam.BLUE else GameTeam.RED
            if (scoringTeam == GameTeam.RED) redScore += 1 else blueScore += 1
            winner = when {
                targetScore == null -> null
                redScore >= targetScore -> GameTeam.RED
                blueScore >= targetScore -> GameTeam.BLUE
                else -> null
            }
            goalCelebrationElapsed = 0f
            val goalY = if (scoringTeam == GameTeam.RED) topGoalLine else bottomGoalLine
            ball = ball.copy(
                position = Vector2(position.x, goalY),
                velocity = Vector2(0f, 0f),
                height = 0f,
                verticalVelocity = 0f
            )
            phase = GamePhase.GOAL
            phaseTimeRemaining = 0f
            return
        }

        val hitCrossbar = goalCrossing != null && crossingOffsetX != null &&
            abs(crossingOffsetX) <= goalHalfWidth &&
            GoalGeometry.touchesCrossbar(goalCrossing.ballHeight, ballRadius)
        if (hitCrossbar) {
            soundEvents = soundEvents.copy(crossbars = soundEvents.crossbars + 1)
            val crossing = requireNotNull(goalCrossing)
            soundEvents = soundEvents.copy(posts = soundEvents.posts + 1)
            impactSideX = 0f
            impactSideY = if (crossing.team == GameTeam.RED) -1f else 1f
            impactStrength = (abs(velocity.y) / 600f).coerceIn(.35f, 1f)
            goalPostImpactStrength = impactStrength
            position = Vector2(crossing.x, crossing.lineY)
            heightAboveGround = crossing.ballHeight
            velocity =
                Vector2(velocity.x * wallTangentialRetention, -velocity.y * wallBounceRestitution)
            verticalVelocity = -abs(verticalVelocity) * wallBounceRestitution
        } else if (goalCrossing != null && crossingOffsetX != null &&
            goalCrossing.ballHeight <= GoalGeometry.openingHeight &&
            abs(crossingOffsetX) <= width * .18f + ballRadius
        ) {
            // The cardboard posts sit on either side of the goal opening. A glancing
            // impact sends the ball back into the field and adds a small sideways kick.
            val postNormalX = if (crossingOffsetX < 0f) 1f else -1f
            impactSideX = -postNormalX
            impactSideY = if (goalCrossing.team == GameTeam.RED) -1f else 1f
            impactStrength = (abs(velocity.y) / 600f).coerceIn(.35f, 1f)
            soundEvents = soundEvents.copy(posts = soundEvents.posts + 1)
            goalPostImpactStrength = impactStrength
            position = Vector2(goalCrossing.x, goalCrossing.lineY)
            velocity = Vector2(
                velocity.x * wallTangentialRetention + postNormalX * abs(velocity.y) * .16f,
                -velocity.y * wallBounceRestitution
            )
            verticalVelocity *= wallTangentialRetention
        }

        if (position.y < minY || position.y > maxY) {
            soundEvents = soundEvents.copy(impacts = soundEvents.impacts + 1)
            impactSideY = if (position.y > maxY) 1f else -1f
            impactSideX = 0f
            impactStrength = (abs(velocity.y) / 600f).coerceIn(.25f, 1f)
            position = position.copy(y = position.y.coerceIn(minY, maxY))
            velocity =
                Vector2(velocity.x * wallTangentialRetention, -velocity.y * wallBounceRestitution)
            verticalVelocity *= wallTangentialRetention
        }

        val movement = position - previousPosition
        val travelVelocity = if (dt > 0f) movement * (1f / dt) else Vector2(0f, 0f)

        // A backward pass crosses the shooter's collider. Restore normal contact
        // only after the launch cooldown and after the ball has cleared that body.
        launchingKickerId?.let { id ->
            val kicker = players.firstOrNull { it.id == id }
            if (kicker == null ||
                (
                    (contactCooldownByPlayerId[id] ?: 0f) <= 0f &&
                        (position - kicker.position).length() > playerCollisionDistance + 1f
                    )
            ) {
                launchingKickerId = null
            }
        }

        val collision = players.mapNotNull { player ->
            if (player.id == launchingKickerId) return@mapNotNull null
            if ((contactCooldownByPlayerId[player.id] ?: 0f) > 0f) return@mapNotNull null
            val center = player.position
            val time =
                firstCircleContactTime(previousPosition, position, center, playerCollisionDistance)
                    ?: return@mapNotNull null
            val heightAtContact = ball.height + (motion.height - ball.height) * time
            if (heightAtContact >= standeeCollisionHeight) return@mapNotNull null
            val contactPoint = previousPosition + movement * time
            val difference = contactPoint - center
            val distance = difference.length()
            val normal = if (distance > .001f) {
                difference * (1f / distance)
            } else {
                val speed = travelVelocity.length().coerceAtLeast(.001f)
                Vector2(-travelVelocity.x / speed, -travelVelocity.y / speed)
            }
            val towardPlayer = travelVelocity.x * normal.x + travelVelocity.y * normal.y
            if (towardPlayer >= 0f) return@mapNotNull null
            val relativeVelocity = velocity - player.velocity
            if (relativeVelocity.x * normal.x + relativeVelocity.y * normal.y >=
                -8f
            ) {
                return@mapNotNull null
            }
            PlayerCollision(player, time, center, normal, contactPoint)
        }.minByOrNull { it.time }

        if (collision != null && velocity.length() > 12f) {
            soundEvents = soundEvents.copy(blocks = soundEvents.blocks + 1)
            soundEvents = soundEvents.copy(impacts = soundEvents.impacts + 1)
            val player = collision.player
            if (defensiveBlockWinsPossession && lastKickingTeam != null && player.team != lastKickingTeam) {
                defenderBlockedShot = true
            }
            val normal = collision.normal
            val previousSpeed = velocity.length()
            val relativeVelocity = velocity - player.velocity
            val relativeNormalSpeed = relativeVelocity.x * normal.x + relativeVelocity.y * normal.y
            val incomingSpeed = (-relativeNormalSpeed).coerceAtLeast(0f)
            val tangentialVelocity = relativeVelocity - normal * relativeNormalSpeed
            // Use condition before this contact; newly accumulated damage affects subsequent hits.
            val dent = if (dentingEnabled) player.condition.progress else 0f
            val restitution = playerBounceRestitution * (1f - .5f * dent)
            val tangentialRetention = playerBounceTangentialRetention * (1f - .25f * dent)
            val reducedMass = ballMassKg * standeeMassKg / (ballMassKg + standeeMassKg)
            val normalSpeedMeters = incomingSpeed * metersPerDp
            val tangentSpeedMeters = tangentialVelocity.length() * metersPerDp
            val dissipatedEnergy = .5f * reducedMass * (
                normalSpeedMeters * normalSpeedMeters * (1f - restitution * restitution) +
                    tangentSpeedMeters * tangentSpeedMeters *
                    (1f - tangentialRetention * tangentialRetention)
                )
            val impulse = if (incomingSpeed > 8f) {
                (1f + restitution) * incomingSpeed * playerMassRatio / (playerMassRatio + 1f)
            } else {
                0f
            }
            if (impulse > 0f) {
                velocity = velocity + normal * impulse -
                    tangentialVelocity * (1f - tangentialRetention)
                val maxReboundSpeed = previousSpeed * tangentialRetention
                if (velocity.length() >
                    maxReboundSpeed
                ) {
                    velocity = velocity * (maxReboundSpeed / velocity.length())
                }
            }
            // The standee absorbs most existing loft, then kicks a small amount
            // upward on impact, like a light crumpled ball hitting stiff paper.
            heightAboveGround = (heightAboveGround * .25f).coerceAtMost(6f)
            verticalVelocity = (incomingSpeed * playerImpactHopFraction * (1f - .5f * dent))
                .coerceAtMost(maxPlayerImpactHopSpeed)
            val playerVelocity = (player.velocity - normal * (impulse / playerMassRatio)).let { pushed ->
                val maxSpeed = 190f
                if (pushed.length() > maxSpeed) pushed * (maxSpeed / pushed.length()) else pushed
            }
            impactSideX = if (abs(normal.x) > abs(normal.y)) -normal.x.coerceIn(-1f, 1f) else 0f
            impactSideY = if (abs(normal.y) >= abs(normal.x)) -normal.y.coerceIn(-1f, 1f) else 0f
            impactStrength = (incomingSpeed / 600f).coerceIn(.25f, 1f)
            players = players.map {
                if (it.id == player.id) {
                    it.copy(
                        velocity = playerVelocity,
                        condition = if (dentingEnabled) {
                            it.condition.absorbEnergy(
                                dissipatedEnergy,
                                -normal.x,
                                -normal.y
                            )
                        } else {
                            it.condition
                        },
                        impactDirectionX = -normal.x,
                        impactDirectionY = -normal.y,
                        impactStrength = (incomingSpeed / 460f).coerceIn(.12f, 1f)
                    )
                } else {
                    it
                }
            }
            val distance = (collision.contactPoint - collision.center).length()
            val correction = (playerCollisionDistance - distance).coerceAtLeast(0f) + .5f
            position = collision.contactPoint + normal * correction
            contactCooldownByPlayerId[player.id] = playerContactCooldown
        }

        ball = ball.copy(
            position = position, velocity = velocity, height = heightAboveGround,
            verticalVelocity = verticalVelocity, rotationX = rotationX, rotationY = rotationY,
            impactSideX = impactSideX, impactSideY = impactSideY, impactStrength = impactStrength,
            landingStrength = landingStrength, goalPostImpactStrength = goalPostImpactStrength
        )
        if (velocity.length() < 14f && heightAboveGround == 0f && verticalVelocity == 0f) {
            ball = ball.copy(velocity = Vector2(0f, 0f))
            aimAngle = 0f
            focusedPlayerId = nextKicker().id
            beginKickerApproach()
        }
    }

    /** Resolve possession from physical positions, comparing one nearest player per team. */
    private fun nextKicker(): MatchPlayer {
        fun nearest(team: GameTeam) = players.asSequence()
            .filter { it.team == team }
            .minWith(
                compareBy<MatchPlayer> {
                    (it.position - ball.position).length()
                }.thenBy { it.id }
            )

        val red = nearest(GameTeam.RED)
        val blue = nearest(GameTeam.BLUE)
        if (defensiveBlockWinsPossession && defenderBlockedShot && lastKickingTeam != null) {
            return if (lastKickingTeam == GameTeam.RED) blue else red
        }
        val redDistance = (red.position - ball.position).length()
        val blueDistance = (blue.position - ball.position).length()
        if (abs(redDistance - blueDistance) < possessionTieThreshold) {
            return when (lastKickingTeam) {
                GameTeam.RED -> blue

                GameTeam.BLUE -> red

                null ->
                    if (redDistance < blueDistance ||
                        (redDistance == blueDistance && red.id < blue.id)
                    ) {
                        red
                    } else {
                        blue
                    }
            }
        }
        return if (redDistance < blueDistance) red else blue
    }

    /** Semi-implicit Euler integration of quadratic air drag, gravity and rolling resistance. */
    private fun integrateBallMotion(current: MatchBall, dt: Float): BallMotion {
        val airborne = current.height > 0f || current.verticalVelocity > 0f
        val verticalSpeedDp = if (airborne) current.verticalVelocity else 0f
        val speedDp = sqrt(
            current.velocity.x * current.velocity.x +
                current.velocity.y * current.velocity.y + verticalSpeedDp * verticalSpeedDp
        )
        val speedMetersPerSecond = speedDp * metersPerDp
        val dragForceNewtons = .5f * airDensityKgPerCubicMeter * dragCoefficient *
            crossSectionAreaSquareMeters * speedMetersPerSecond * speedMetersPerSecond
        val dragSpeedLossMetersPerSecond = dragForceNewtons / ballMassKg * dt
        val dragRetention = if (speedMetersPerSecond > 0f) {
            (1f - dragSpeedLossMetersPerSecond / speedMetersPerSecond).coerceAtLeast(0f)
        } else {
            1f
        }
        var planarVelocity = current.velocity * dragRetention
        if (!airborne) {
            val planarSpeed = planarVelocity.length()
            if (planarSpeed > 0f) {
                val frictionSpeedLossDp = rollingResistanceCoefficient *
                    gravityMetersPerSecondSquared / metersPerDp * dt
                planarVelocity = planarVelocity *
                    (1f - frictionSpeedLossDp / planarSpeed).coerceAtLeast(0f)
            }
        }
        val verticalVelocity = if (airborne) {
            verticalSpeedDp * dragRetention - gravityMetersPerSecondSquared / metersPerDp * dt
        } else {
            0f
        }
        return BallMotion(
            position = current.position + planarVelocity * dt,
            velocity = planarVelocity,
            height = if (airborne) current.height + verticalVelocity * dt else 0f,
            verticalVelocity = verticalVelocity
        )
    }

    private fun player(
        id: String,
        team: GameTeam,
        initialZone: Int,
        offsetTowardAttackRows: Float = 0f
    ): MatchPlayer {
        val zonePosition = zoneCenter(initialZone)
        val position = zonePosition.copy(
            y =
                zonePosition.y +
                    attackDirectionY(team) * height / ZONE_ROWS * offsetTowardAttackRows
        )
        return MatchPlayer(id, team, position, position)
    }

    /** Screen Y starts at the top: red advances toward smaller Y, blue toward larger Y. */
    private fun attackDirectionY(team: GameTeam) = if (team ==
        GameTeam.RED
    ) {
        redAttack
    } else {
        -redAttack
    }

    companion object {
        const val ZONE_COLUMNS = 3
        const val ZONE_ROWS = 6
        const val ZONE_COUNT = ZONE_COLUMNS * ZONE_ROWS
    }

    /** Integrates ball-hit player motion with drag and the same setup movement boundary. */
    private fun advancePlayers(dt: Float) {
        players = players.map { player ->
            val nextPosition = player.position + player.velocity * dt
            // A kicker may approach a ball far outside its home zone. Keep it at
            // the shot location instead of snapping it back into the ball's path.
            val movementCenter = if (player.id ==
                focusedPlayerId
            ) {
                shotKickerAnchor ?: player.home
            } else {
                player.home
            }
            val fromHome = nextPosition - movementCenter
            val distanceFromHome = fromHome.length()
            val limitedPosition = if (distanceFromHome > movementRadius) {
                movementCenter + fromHome * (movementRadius / distanceFromHome)
            } else {
                nextPosition
            }
            val boundedPosition = Vector2(
                limitedPosition.x.coerceIn(playerRadius, width - playerRadius),
                limitedPosition.y.coerceIn(playerRadius, height - playerRadius)
            )
            val velocity = if (boundedPosition !=
                nextPosition
            ) {
                Vector2(0f, 0f)
            } else {
                player.velocity * exp(-7.5f * dt)
            }
            val impactStrength = (player.impactStrength - dt * 3.4f).coerceAtLeast(0f)
            player.copy(
                position = boundedPosition,
                velocity = velocity,
                impactDirectionX = if (impactStrength > 0f) player.impactDirectionX else 0f,
                impactDirectionY = if (impactStrength > 0f) player.impactDirectionY else 0f,
                impactStrength = impactStrength
            )
        }
        separatePlayerBodies()
    }

    private fun separatePlayerBodies(protectedId: String? = null) {
        val separated = players.toMutableList()
        for (pass in 0 until 12) {
            var corrected = false
            for (firstIndex in 0 until separated.lastIndex) {
                for (secondIndex in firstIndex + 1 until separated.size) {
                    val first = separated[firstIndex]
                    val second = separated[secondIndex]
                    val delta = second.position - first.position
                    val distance = delta.length()
                    if (distance >= playerSpacing - .01f) continue
                    val direction = if (distance > .001f) {
                        delta * (1f / distance)
                    } else if (first.id < second.id) {
                        Vector2(1f, 0f)
                    } else {
                        Vector2(-1f, 0f)
                    }
                    val overlap = playerSpacing - distance + .01f
                    val firstShare = if (first.id == protectedId) {
                        0f
                    } else if (second.id == protectedId) {
                        1f
                    } else {
                        .5f
                    }
                    val secondShare = 1f - firstShare
                    val firstPosition =
                        clampPlayerPosition(first.position - direction * (overlap * firstShare))
                    val secondPosition =
                        clampPlayerPosition(second.position + direction * (overlap * secondShare))
                    if (firstPosition != first.position ||
                        secondPosition != second.position
                    ) {
                        corrected = true
                    }
                    separated[firstIndex] =
                        first.copy(position = firstPosition, velocity = Vector2(0f, 0f))
                    separated[secondIndex] =
                        second.copy(position = secondPosition, velocity = Vector2(0f, 0f))
                }
            }
            if (!corrected) break
        }
        players = separated
    }

    private fun clampPlayerPosition(position: Vector2) = Vector2(
        position.x.coerceIn(playerRadius, width - playerRadius),
        position.y.coerceIn(playerRadius, height - playerRadius)
    )

    private fun goalLineCrossing(
        start: Vector2,
        end: Vector2,
        startHeight: Float,
        ballHeight: Float,
        topGoalLine: Float,
        bottomGoalLine: Float
    ): GoalLineCrossing? {
        val crossing = when {
            start.y >= topGoalLine && end.y < topGoalLine -> GameTeam.RED to topGoalLine
            start.y <= bottomGoalLine && end.y > bottomGoalLine -> GameTeam.BLUE to bottomGoalLine
            else -> return null
        }
        val progress = ((crossing.second - start.y) / (end.y - start.y)).coerceIn(0f, 1f)
        val crossingHeight = startHeight + (ballHeight - startHeight) * progress
        val crossingX = start.x + (end.x - start.x) * progress
        return GoalLineCrossing(crossing.first, crossing.second, crossingX, crossingHeight)
    }

    private fun beginTimeoutTurnover() {
        val outgoing = focusedPlayer()
        val defendingTeam = if (outgoing.team == GameTeam.RED) GameTeam.BLUE else GameTeam.RED
        val incoming = players.asSequence().filter { it.team == defendingTeam }
            .minWith(
                compareBy<MatchPlayer> {
                    (it.position - ball.position).length()
                }.thenBy { it.id }
            )
        val awayFromBall = outgoing.position - ball.position
        val awayLength = awayFromBall.length()
        val away = if (awayLength > .001f) {
            awayFromBall * (1f / awayLength)
        } else {
            Vector2(0f, -attackDirectionY(outgoing.team))
        }
        val sideways = Vector2(-away.y, away.x)
        val offsets = listOf(
            away * 60f,
            away * 85f + sideways * 30f,
            away * 85f - sideways * 30f,
            outgoing.home - outgoing.position
        )
        val retreat = offsets.asSequence().map { offset ->
            val target = outgoing.position + offset
            Vector2(
                target.x.coerceIn(playerRadius, width - playerRadius),
                target.y.coerceIn(playerRadius, height - playerRadius)
            )
        }.firstOrNull { candidate ->
            val path = candidate - outgoing.position
            val steps = ceil(path.length() / 5f).toInt().coerceAtLeast(1)
            (candidate - outgoing.position).length() > 1f &&
                (candidate - ball.position).length() >= playerCollisionDistance + kickerVisualGap &&
                players.none { it.id != outgoing.id && zoneAt(it.position) == zoneAt(candidate) } &&
                (1..steps).all { step ->
                    val point = outgoing.position + path * (step.toFloat() / steps)
                    players.none {
                        it.id != outgoing.id &&
                            (it.position - point).length() < playerRadius * 2f + 2f
                    }
                }
        } ?: outgoing.position
        turnoverNextKickerId = incoming.id
        retreatFrom = outgoing.position
        retreatTo = retreat
        retreatElapsed = 0f
        aimAngle = 0f
        power = 0f
        phaseTimeRemaining = 0f
        phase = GamePhase.TURNOVER_RETREAT
    }

    private fun beginKickoff(team: GameTeam) {
        defenderBlockedShot = false
        launchingKickerId = null
        players = players.map { player ->
            val kickoffHome = when {
                player.id == "red-striker" -> strikerKickoffPosition(
                    zone = 8,
                    team = GameTeam.RED,
                    isKickingOff = team == GameTeam.RED
                )

                player.id == "blue-striker" -> strikerKickoffPosition(
                    zone = 11,
                    team = GameTeam.BLUE,
                    isKickingOff = team == GameTeam.BLUE
                )

                else -> startingFormationHomeById.getValue(player.id)
            }
            player.copy(
                position = kickoffHome,
                home = kickoffHome,
                velocity = Vector2(0f, 0f),
                impactDirectionX = 0f,
                impactDirectionY = 0f,
                impactStrength = 0f
            )
        }
        focusedPlayerId = players.first { it.team == team && it.id.endsWith("striker") }.id
        ball = ball.copy(
            position = Vector2(width * .5f, height * .5f),
            velocity = Vector2(0f, 0f),
            height = 0f,
            verticalVelocity = 0f,
            impactSideX = 0f,
            impactSideY = 0f,
            impactStrength = 0f,
            landingStrength = 0f,
            goalPostImpactStrength = 0f
        )
        aimAngle = 0f
        power = 0f
        contactCooldownByPlayerId.clear()
        shotKickerAnchor = null
        playerDragId = null
        playerDragStartState = null
        playerDragDelta = Vector2(0f, 0f)
        playerDragStartZone = null
        beginKickerApproach()
    }

    private fun strikerKickoffPosition(zone: Int, team: GameTeam, isKickingOff: Boolean): Vector2 {
        val position = zoneCenter(zone)
        if (isKickingOff) return position
        return position.copy(y = position.y - attackDirectionY(team) * height / ZONE_ROWS * .48f)
    }

    private fun firstCircleContactTime(
        start: Vector2,
        end: Vector2,
        center: Vector2,
        radius: Float
    ): Float? {
        val movement = end - start
        val offset = start - center
        val a = movement.x * movement.x + movement.y * movement.y
        val c = offset.x * offset.x + offset.y * offset.y - radius * radius
        if (a < .0001f) return if (c <= 0f) 0f else null
        if (c <= 0f) return 0f

        val b = 2f * (offset.x * movement.x + offset.y * movement.y)
        if (b >= 0f) return null
        val discriminant = b * b - 4f * a * c
        if (discriminant < 0f) return null
        val time = (-b - sqrt(discriminant)) / (2f * a)
        return time.takeIf { it in 0f..1f }
    }

    private fun moveFocusedBehindBall() {
        val focused = players.first { it.id == focusedPlayerId }
        val forward = if (focused.team == GameTeam.RED) redAttack else -redAttack
        val readyBallPosition = shotReadyBallPosition(forward)
        ball = ball.copy(position = readyBallPosition)
        val target = Vector2(readyBallPosition.x, readyBallPosition.y - forward * kickerStandOff())
        val safe =
            Vector2(
                target.x.coerceIn(playerRadius, width - playerRadius),
                target.y.coerceIn(
                    playerRadius,
                    height - playerRadius
                )
            )
        players = players.map { if (it.id == focusedPlayerId) it.copy(position = safe) else it }
    }

    private fun beginKickerApproach(preserveBallPosition: Boolean = false) {
        playersMovedThisTurn.clear()
        defensiveMoveHistory.clear()
        playerDragStartState = null
        shotKickerAnchor = null
        val focused = players.first { it.id == focusedPlayerId }
        val forward = if (focused.team == GameTeam.RED) redAttack else -redAttack
        val readyBallPosition = if (preserveBallPosition) {
            ball.position
        } else {
            shotReadyBallPosition(
                forward
            )
        }
        val target = clearKickerPosition(focused.id, readyBallPosition, forward)
        approachFrom = focused.position
        approachTo = target
        approachBallFrom = ball.position
        approachBallTo = readyBallPosition
        approachElapsed = 0f
        players = players.map {
            if (it.id == focusedPlayerId) it.copy(velocity = Vector2(0f, 0f)) else it
        }
        phase = GamePhase.KICKER_APPROACH
        phaseTimeRemaining = 0f
    }

    private fun clearKickerPosition(id: String, ballPosition: Vector2, forward: Float): Vector2 {
        val standOff = kickerStandOff()
        // Keep the kicker on a fixed-radius arc. The protected approach resolves
        // overlapping bodies instead of moving the kicker far away from the ball.
        val angles = listOf(0f, -15f, 15f, -30f, 30f, -45f, 45f, -60f, 60f, -75f, 75f, -90f, 90f)
        return angles.asSequence().map { degrees ->
            val angle = degrees * PI.toFloat() / 180f
            Vector2(
                ballPosition.x + sin(angle) * standOff,
                ballPosition.y - forward * cos(angle) * standOff
            )
        }.firstOrNull { point ->
            point.x in playerRadius..(width - playerRadius) &&
                point.y in playerRadius..(height - playerRadius)
        } ?: players.first { it.id == id }.position
    }

    /** Make room behind a resting ball before the next shot, including at the back wall. */
    private fun shotReadyBallPosition(forward: Float): Vector2 {
        val standOff = kickerStandOff()
        val minY = if (forward > 0f) wallInset + playerRadius + standOff else wallInset + ballRadius
        val maxY = if (forward <
            0f
        ) {
            height - wallInset - playerRadius - standOff
        } else {
            height - wallInset - ballRadius
        }
        return ball.position.copy(y = ball.position.y.coerceIn(minY, maxY))
    }

    /** Keep both kickers just outside their own hit area while facing the ball. */
    private fun kickerStandOff(): Float = playerCollisionDistance + kickerVisualGap
}

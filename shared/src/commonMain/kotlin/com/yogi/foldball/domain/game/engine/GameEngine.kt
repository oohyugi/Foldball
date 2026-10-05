package com.yogi.foldball.domain.game.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.ceil

data class Vector2(val x: Float, val y: Float) {
    operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
    operator fun times(scale: Float) = Vector2(x * scale, y * scale)
    fun length() = sqrt(x * x + y * y)
}

enum class GameTeam { RED, BLUE }
enum class GamePhase { DEFENDING, AIMING, BALL_MOVING, TURNOVER_RETREAT, KICKER_APPROACH, GOAL, MATCH_OVER }

/** Chosen when a match starts; a future settings screen can supply different values. */
data class MatchTimingConfig(
    val defenseSeconds: Int = 10,
    val attackSeconds: Int = 8,
) {
    init {
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
    val goalPostImpactStrength: Float = 0f,
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
)

private data class PlayerCollision(
    val player: MatchPlayer,
    val time: Float,
    val center: Vector2,
    val normal: Vector2,
    val contactPoint: Vector2,
)

private data class BallMotion(
    val position: Vector2,
    val velocity: Vector2,
    val height: Float,
    val verticalVelocity: Float,
)

private data class GoalLineCrossing(val team: GameTeam, val lineY: Float, val x: Float)

/** Deterministic, platform-independent movement and ball physics for the match feature. */
class GameEngine(
    private val width: Float,
    private val height: Float,
    private val targetScore: Int? = 3,
    private val timing: MatchTimingConfig = MatchTimingConfig(),
    initialKickoffTeam: GameTeam = GameTeam.RED,
) {
    private val ballRadius = 10f
    private val playerRadius = 25f
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
    private val wallTangentialRetention = .82f
    private val kickerVisualGap = 1f
    private val playerCollisionDistance = ballRadius + playerRadius + playerCollisionOverlap
    private val possessionTieThreshold = 6f
    private val maxDefensiveMoves = 2
    private val movementRadius = 100f
    private val wallInset = 18f
    private val redAttack = -1f
    private val referenceFieldHeight = 580f
    private val goalCelebrationDuration = 2.2f
    private val retreatDuration = .4f

    private var players = listOf(
        player("red-left", GameTeam.RED, 4, offsetTowardAttackRows = .2f),
        player("red-center", GameTeam.RED, 2, offsetTowardAttackRows = .33f),
        player("red-right", GameTeam.RED, 6, offsetTowardAttackRows = .2f),
        player("red-striker", GameTeam.RED, 8),
        player("blue-left", GameTeam.BLUE, 13, offsetTowardAttackRows = .2f),
        player("blue-center", GameTeam.BLUE, 17, offsetTowardAttackRows = .33f),
        player("blue-right", GameTeam.BLUE, 15, offsetTowardAttackRows = .2f),
        player("blue-striker", GameTeam.BLUE, 11),
    )
    // Immutable formation anchors. A player's mutable home follows defensive moves
    // during a rally, but every kickoff starts from these original positions.
    private val startingFormationHomeById = players.associate { it.id to it.home }
    private var ball = MatchBall(Vector2(width * .5f, height * .5f), Vector2(0f, 0f), ballRadius, 0f, 0f, 0f, 0f)
    private var phase = GamePhase.DEFENDING
    private var phaseTimeRemaining = timing.defenseSeconds.toFloat()
    private var focusedPlayerId = if (initialKickoffTeam == GameTeam.RED) "red-striker" else "blue-striker"
    private var lastKickingTeam: GameTeam? = null
    private var aimAngle = 0f
    private var power = 0f
    private var redScore = 0
    private var blueScore = 0
    private var lastScoringTeam: GameTeam? = null
    private var kickoffTeam = initialKickoffTeam
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
                "red-striker" -> strikerKickoffPosition(8, GameTeam.RED, initialKickoffTeam == GameTeam.RED)
                "blue-striker" -> strikerKickoffPosition(11, GameTeam.BLUE, initialKickoffTeam == GameTeam.BLUE)
                else -> player.home
            }
            player.copy(position = kickoffHome, home = kickoffHome)
        }
        moveFocusedBehindBall()
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
        playerDragDelta += delta
        val proposed = playerDragStart + playerDragDelta
        val movement = proposed - player.position
        val steps = ceil(movement.length() / 3f).toInt().coerceAtLeast(1)
        var safe = player.position
        for (step in 1..steps) {
            val candidate = player.position + movement * (step.toFloat() / steps)
            if (!isLegalPlayerDragPosition(id, candidate)) break
            safe = candidate
        }
        if (safe != player.position) {
            players = players.map {
                if (it.id == id) it.copy(position = safe, home = safe, velocity = Vector2(0f, 0f)) else it
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
        }
    }

    val defensiveMovesUsed: Int get() = defensiveMoveHistory.size

    fun canUndoPlayerMove(): Boolean =
        phase == GamePhase.DEFENDING && playerDragId == null && defensiveMoveHistory.isNotEmpty()

    fun undoLastPlayerMove(): Boolean {
        if (!canUndoPlayerMove()) return false
        val previous = defensiveMoveHistory.removeAt(defensiveMoveHistory.lastIndex)
        players = players.map { if (it.id == previous.id) previous else it }
        playersMovedThisTurn.remove(previous.id)
        return true
    }

    fun canMovePlayer(id: String): Boolean {
        val player = players.firstOrNull { it.id == id } ?: return false
        return phase == GamePhase.DEFENDING && id != focusedPlayerId &&
            player.team != focusedPlayer().team &&
            playersMovedThisTurn.size < maxDefensiveMoves && id !in playersMovedThisTurn
    }

    fun finishDefense(): Boolean {
        if (phase != GamePhase.DEFENDING) return false
        playerDragId?.let(::endPlayerMove)
        phase = GamePhase.AIMING
        phaseTimeRemaining = timing.attackSeconds.toFloat()
        return true
    }

    fun isZoneAvailableForPlayer(id: String, zone: Int): Boolean {
        val player = players.firstOrNull { it.id == id } ?: return false
        if (!canMovePlayer(id) && playerDragId != id) return false
        val originZone = if (playerDragId == id) playerDragStartZone else zoneAt(player.position)
        if (originZone == null || (zone != originZone && !areAdjacentZones(originZone, zone))) return false
        return players.none { it.id != id && zoneAt(it.position) == zone }
    }

    fun isZoneOccupied(zone: Int, exceptPlayerId: String? = null): Boolean =
        players.any { it.id != exceptPlayerId && zoneAt(it.position) == zone }

    private fun isLegalPlayerDragPosition(id: String, position: Vector2): Boolean {
        val zone = zoneAt(position) ?: return false
        if (!isZoneAvailableForPlayer(id, zone)) return false
        if (position.x !in playerRadius..(width - playerRadius) ||
            position.y !in playerRadius..(height - playerRadius)) return false
        if ((position - ball.position).length() < playerCollisionDistance + kickerVisualGap) return false
        return players.none { it.id != id && (it.position - position).length() < playerRadius * 2f + 2f }
    }

    fun aimBy(deltaX: Float) {
        if (phase == GamePhase.AIMING) aimAngle = (aimAngle + deltaX / 120f).coerceIn(-1.05f, 1.05f)
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
        lastKickingTeam = player.team
        shotKickerAnchor = player.position
        // The ball starts only 1 dp outside the kicker's collision radius. Ignore
        // that player briefly so the launch cannot immediately rebound backward.
        contactCooldownByPlayerId[player.id] = playerContactCooldown
        val forward = if (player.team == GameTeam.RED) redAttack else -redAttack
        val direction = Vector2(sin(aimAngle) * -forward, cos(aimAngle) * forward)
        // Tune a full-power shot to travel roughly goal-to-goal despite paper-ball drag.
        val fieldScale = (height / referenceFieldHeight).coerceIn(1f, 1.25f)
        val appliedPower = power * power * power
        val speed = 150f + appliedPower * 3_350f * fieldScale
        ball = ball.copy(
            velocity = direction * speed,
            verticalVelocity = 120f + appliedPower * 280f,
            impactSideX = 0f,
            impactSideY = 0f,
            impactStrength = 0f,
            goalPostImpactStrength = 0f,
        )
        phase = GamePhase.BALL_MOVING
        phaseTimeRemaining = 0f
        power = 0f
        return true
    }

    /** Advances simulation using seconds; callers should use a frame clock, not an animation timer. */
    fun step(deltaSeconds: Float): GameSnapshot {
        if (phase == GamePhase.DEFENDING || phase == GamePhase.AIMING) {
            // Countdown follows elapsed frame time even when rendering drops below 30 fps.
            advanceStep(deltaSeconds.coerceIn(0f, .25f))
            return snapshot
        }
        val frameTime = deltaSeconds.coerceIn(0f, 1f / 30f)
        if (frameTime <= 0f) return snapshot
        val substepCount = ceil(frameTime / maxPhysicsStep).toInt().coerceAtLeast(1)
        val dt = frameTime / substepCount
        repeat(substepCount) { advanceStep(dt) }
        return snapshot
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
            height - (rowFromBottom + .5f) * height / ZONE_ROWS,
        )
    }

    private fun areAdjacentZones(first: Int, second: Int): Boolean {
        if (first !in 1..ZONE_COUNT || second !in 1..ZONE_COUNT) return false
        val firstIndex = first - 1
        val secondIndex = second - 1
        val columnDistance = kotlin.math.abs(firstIndex % ZONE_COLUMNS - secondIndex % ZONE_COLUMNS)
        val rowDistance = kotlin.math.abs(firstIndex / ZONE_COLUMNS - secondIndex / ZONE_COLUMNS)
        return columnDistance <= 1 && rowDistance <= 1
    }

    private fun advanceStep(dt: Float) {
        if (phase == GamePhase.DEFENDING) {
            phaseTimeRemaining = (phaseTimeRemaining - dt).coerceAtLeast(0f)
            if (phaseTimeRemaining == 0f) finishDefense()
            return
        }
        if (phase == GamePhase.AIMING) {
            phaseTimeRemaining = (phaseTimeRemaining - dt).coerceAtLeast(0f)
            if (phaseTimeRemaining == 0f) beginTimeoutTurnover()
            return
        }
        if (phase == GamePhase.GOAL) {
            goalCelebrationElapsed += dt
            if (goalCelebrationElapsed >= goalCelebrationDuration) {
                if (winner != null) {
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
                if (it.id == focusedPlayerId) it.copy(position = from + (to - from) * eased, velocity = Vector2(0f, 0f)) else it
            }
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
            val progress = (approachElapsed / .36f).coerceIn(0f, 1f)
            val eased = progress * progress * (3f - 2f * progress)
            val from = approachFrom ?: return
            val to = approachTo ?: return
            val position = from + (to - from) * eased
            players = players.map {
                if (it.id == focusedPlayerId) it.copy(position = position, velocity = Vector2(0f, 0f)) else it
            }
            val ballFrom = approachBallFrom ?: return
            val ballTo = approachBallTo ?: return
            ball = ball.copy(position = ballFrom + (ballTo - ballFrom) * eased)
            if (progress >= 1f) {
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
            if (remaining > 0f) contactCooldownByPlayerId[id] = remaining
            else contactCooldownByPlayerId.remove(id)
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
            impactSideX = if (position.x > maxX) 1f else -1f
            impactSideY = 0f
            impactStrength = (abs(velocity.x) / 600f).coerceIn(.25f, 1f)
            position = position.copy(x = position.x.coerceIn(minX, maxX))
            velocity = Vector2(-velocity.x * wallBounceRestitution, velocity.y * wallTangentialRetention)
            verticalVelocity *= wallTangentialRetention
        }
        if (heightAboveGround < 0f) {
            heightAboveGround = 0f
            val landingSpeed = abs(verticalVelocity)
            val landedFromAir = ball.height > .01f && ball.verticalVelocity < 0f
            val bounceSpeed = landingSpeed * groundBounceRestitution
            verticalVelocity = if (bounceSpeed * metersPerDp > .18f) bounceSpeed else 0f
            if (landedFromAir) landingStrength = (landingSpeed / 260f).coerceIn(.18f, 1f)
        }

        // Height is separate from field position. Sweep the same coordinates
        // used to draw the ball, so both teams make contact at equal distances.
        val topGoalLine = wallInset + ballRadius
        val bottomGoalLine = height - wallInset - ballRadius
        val goalCrossing = goalLineCrossing(previousPosition, position, heightAboveGround, topGoalLine, bottomGoalLine)
        val goalHalfWidth = width * .16f - ballRadius
        val crossingOffsetX = goalCrossing?.let { it.x - width * .5f }
        val isGoal = crossingOffsetX != null && abs(crossingOffsetX) <= goalHalfWidth
        if (isGoal) {
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
                verticalVelocity = 0f,
            )
            phase = GamePhase.GOAL
            phaseTimeRemaining = 0f
            return
        }

        if (goalCrossing != null && crossingOffsetX != null &&
            abs(crossingOffsetX) <= width * .18f + ballRadius
        ) {
            // The cardboard posts sit on either side of the goal opening. A glancing
            // impact sends the ball back into the field and adds a small sideways kick.
            val postNormalX = if (crossingOffsetX < 0f) 1f else -1f
            impactSideX = -postNormalX
            impactSideY = if (goalCrossing.team == GameTeam.RED) -1f else 1f
            impactStrength = (abs(velocity.y) / 600f).coerceIn(.35f, 1f)
            goalPostImpactStrength = impactStrength
            position = Vector2(goalCrossing.x, goalCrossing.lineY)
            velocity = Vector2(
                velocity.x * wallTangentialRetention + postNormalX * abs(velocity.y) * .16f,
                -velocity.y * wallBounceRestitution,
            )
            verticalVelocity *= wallTangentialRetention
        }

        if (position.y < minY || position.y > maxY) {
            impactSideY = if (position.y > maxY) 1f else -1f
            impactSideX = 0f
            impactStrength = (abs(velocity.y) / 600f).coerceIn(.25f, 1f)
            position = position.copy(y = position.y.coerceIn(minY, maxY))
            velocity = Vector2(velocity.x * wallTangentialRetention, -velocity.y * wallBounceRestitution)
            verticalVelocity *= wallTangentialRetention
        }

        val movement = position - previousPosition
        val travelVelocity = if (dt > 0f) movement * (1f / dt) else Vector2(0f, 0f)

        val collision = players.mapNotNull { player ->
            if ((contactCooldownByPlayerId[player.id] ?: 0f) > 0f) return@mapNotNull null
            val center = player.position
            val time = firstCircleContactTime(previousPosition, position, center, playerCollisionDistance)
                ?: return@mapNotNull null
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
            if (relativeVelocity.x * normal.x + relativeVelocity.y * normal.y >= -8f) return@mapNotNull null
            PlayerCollision(player, time, center, normal, contactPoint)
        }.minByOrNull { it.time }

        if (collision != null && velocity.length() > 12f) {
            val player = collision.player
            val normal = collision.normal
            val previousSpeed = velocity.length()
            val relativeVelocity = velocity - player.velocity
            val relativeNormalSpeed = relativeVelocity.x * normal.x + relativeVelocity.y * normal.y
            val incomingSpeed = (-relativeNormalSpeed).coerceAtLeast(0f)
            val tangentialVelocity = relativeVelocity - normal * relativeNormalSpeed
            val impulse = if (incomingSpeed > 8f) {
                (1f + playerBounceRestitution) * incomingSpeed * playerMassRatio / (playerMassRatio + 1f)
            } else 0f
            if (impulse > 0f) {
                velocity = velocity + normal * impulse -
                    tangentialVelocity * (1f - playerBounceTangentialRetention)
                val maxReboundSpeed = previousSpeed * playerBounceTangentialRetention
                if (velocity.length() > maxReboundSpeed) velocity = velocity * (maxReboundSpeed / velocity.length())
            }
            // The standee absorbs most existing loft, then kicks a small amount
            // upward on impact, like a light crumpled ball hitting stiff paper.
            heightAboveGround = (heightAboveGround * .25f).coerceAtMost(6f)
            verticalVelocity = (incomingSpeed * playerImpactHopFraction)
                .coerceAtMost(maxPlayerImpactHopSpeed)
            val playerVelocity = (player.velocity - normal * (impulse / playerMassRatio)).let { pushed ->
                val maxSpeed = 190f
                if (pushed.length() > maxSpeed) pushed * (maxSpeed / pushed.length()) else pushed
            }
            impactSideX = if (abs(normal.x) > abs(normal.y)) -normal.x.coerceIn(-1f, 1f) else 0f
            impactSideY = if (abs(normal.y) >= abs(normal.x)) -normal.y.coerceIn(-1f, 1f) else 0f
            impactStrength = (incomingSpeed / 600f).coerceIn(.25f, 1f)
            players = players.map {
                if (it.id == player.id) it.copy(
                    velocity = playerVelocity,
                    impactDirectionX = -normal.x,
                    impactDirectionY = -normal.y,
                    impactStrength = (incomingSpeed / 460f).coerceIn(.12f, 1f),
                ) else it
            }
            val distance = (collision.contactPoint - collision.center).length()
            val correction = (playerCollisionDistance - distance).coerceAtLeast(0f) + .5f
            position = collision.contactPoint + normal * correction
            contactCooldownByPlayerId[player.id] = playerContactCooldown
        }

        ball = ball.copy(position = position, velocity = velocity, height = heightAboveGround,
            verticalVelocity = verticalVelocity, rotationX = rotationX, rotationY = rotationY,
            impactSideX = impactSideX, impactSideY = impactSideY, impactStrength = impactStrength,
            landingStrength = landingStrength, goalPostImpactStrength = goalPostImpactStrength)
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
            .minWith(compareBy<MatchPlayer> { (it.position - ball.position).length() }.thenBy { it.id })

        val red = nearest(GameTeam.RED)
        val blue = nearest(GameTeam.BLUE)
        val redDistance = (red.position - ball.position).length()
        val blueDistance = (blue.position - ball.position).length()
        if (abs(redDistance - blueDistance) < possessionTieThreshold) {
            return when (lastKickingTeam) {
                GameTeam.RED -> blue
                GameTeam.BLUE -> red
                null -> if (redDistance < blueDistance ||
                    (redDistance == blueDistance && red.id < blue.id)) red else blue
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
                current.velocity.y * current.velocity.y + verticalSpeedDp * verticalSpeedDp,
        )
        val speedMetersPerSecond = speedDp * metersPerDp
        val dragForceNewtons = .5f * airDensityKgPerCubicMeter * dragCoefficient *
            crossSectionAreaSquareMeters * speedMetersPerSecond * speedMetersPerSecond
        val dragSpeedLossMetersPerSecond = dragForceNewtons / ballMassKg * dt
        val dragRetention = if (speedMetersPerSecond > 0f) {
            (1f - dragSpeedLossMetersPerSecond / speedMetersPerSecond).coerceAtLeast(0f)
        } else 1f
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
        } else 0f
        return BallMotion(
            position = current.position + planarVelocity * dt,
            velocity = planarVelocity,
            height = if (airborne) current.height + verticalVelocity * dt else 0f,
            verticalVelocity = verticalVelocity,
        )
    }

    private fun player(
        id: String,
        team: GameTeam,
        initialZone: Int,
        offsetTowardAttackRows: Float = 0f,
    ): MatchPlayer {
        val zonePosition = zoneCenter(initialZone)
        val position = zonePosition.copy(
            y = zonePosition.y + attackDirectionY(team) * height / ZONE_ROWS * offsetTowardAttackRows,
        )
        return MatchPlayer(id, team, position, position)
    }

    /** Screen Y starts at the top: red advances toward smaller Y, blue toward larger Y. */
    private fun attackDirectionY(team: GameTeam) = if (team == GameTeam.RED) redAttack else -redAttack

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
            val movementCenter = if (player.id == focusedPlayerId) shotKickerAnchor ?: player.home else player.home
            val fromHome = nextPosition - movementCenter
            val distanceFromHome = fromHome.length()
            val limitedPosition = if (distanceFromHome > movementRadius) {
                movementCenter + fromHome * (movementRadius / distanceFromHome)
            } else {
                nextPosition
            }
            val boundedPosition = Vector2(
                limitedPosition.x.coerceIn(playerRadius, width - playerRadius),
                limitedPosition.y.coerceIn(playerRadius, height - playerRadius),
            )
            val velocity = if (boundedPosition != nextPosition) Vector2(0f, 0f) else player.velocity * exp(-7.5f * dt)
            val impactStrength = (player.impactStrength - dt * 3.4f).coerceAtLeast(0f)
            player.copy(
                position = boundedPosition,
                velocity = velocity,
                impactDirectionX = if (impactStrength > 0f) player.impactDirectionX else 0f,
                impactDirectionY = if (impactStrength > 0f) player.impactDirectionY else 0f,
                impactStrength = impactStrength,
            )
        }
    }

    private fun goalLineCrossing(
        start: Vector2,
        end: Vector2,
        ballHeight: Float,
        topGoalLine: Float,
        bottomGoalLine: Float,
    ): GoalLineCrossing? {
        if (ballHeight > 36f) return null

        val crossing = when {
            start.y >= topGoalLine && end.y < topGoalLine -> GameTeam.RED to topGoalLine
            start.y <= bottomGoalLine && end.y > bottomGoalLine -> GameTeam.BLUE to bottomGoalLine
            else -> return null
        }
        val progress = ((crossing.second - start.y) / (end.y - start.y)).coerceIn(0f, 1f)
        val crossingX = start.x + (end.x - start.x) * progress
        return GoalLineCrossing(crossing.first, crossing.second, crossingX)
    }

    private fun beginTimeoutTurnover() {
        val outgoing = focusedPlayer()
        val defendingTeam = if (outgoing.team == GameTeam.RED) GameTeam.BLUE else GameTeam.RED
        val incoming = players.asSequence().filter { it.team == defendingTeam }
            .minWith(compareBy<MatchPlayer> { (it.position - ball.position).length() }.thenBy { it.id })
        val awayFromBall = outgoing.position - ball.position
        val awayLength = awayFromBall.length()
        val away = if (awayLength > .001f) awayFromBall * (1f / awayLength)
            else Vector2(0f, -attackDirectionY(outgoing.team))
        val sideways = Vector2(-away.y, away.x)
        val offsets = listOf(
            away * 60f,
            away * 85f + sideways * 30f,
            away * 85f - sideways * 30f,
            outgoing.home - outgoing.position,
        )
        val retreat = offsets.asSequence().map { offset ->
            val target = outgoing.position + offset
            Vector2(target.x.coerceIn(playerRadius, width - playerRadius),
                target.y.coerceIn(playerRadius, height - playerRadius))
        }.firstOrNull { candidate ->
            val path = candidate - outgoing.position
            val steps = ceil(path.length() / 5f).toInt().coerceAtLeast(1)
            (candidate - outgoing.position).length() > 1f &&
                (candidate - ball.position).length() >= playerCollisionDistance + kickerVisualGap &&
                players.none { it.id != outgoing.id && zoneAt(it.position) == zoneAt(candidate) } &&
                (1..steps).all { step ->
                    val point = outgoing.position + path * (step.toFloat() / steps)
                    players.none { it.id != outgoing.id && (it.position - point).length() < playerRadius * 2f + 2f }
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
        players = players.map { player ->
            val kickoffHome = when {
                player.id == "red-striker" -> strikerKickoffPosition(
                    zone = 8,
                    team = GameTeam.RED,
                    isKickingOff = team == GameTeam.RED,
                )
                player.id == "blue-striker" -> strikerKickoffPosition(
                    zone = 11,
                    team = GameTeam.BLUE,
                    isKickingOff = team == GameTeam.BLUE,
                )
                else -> startingFormationHomeById.getValue(player.id)
            }
            player.copy(
                position = kickoffHome,
                home = kickoffHome,
                velocity = Vector2(0f, 0f),
                impactDirectionX = 0f,
                impactDirectionY = 0f,
                impactStrength = 0f,
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
            goalPostImpactStrength = 0f,
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

    private fun firstCircleContactTime(start: Vector2, end: Vector2, center: Vector2, radius: Float): Float? {
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
        val safe = Vector2(target.x.coerceIn(playerRadius, width - playerRadius), target.y.coerceIn(playerRadius, height - playerRadius))
        players = players.map { if (it.id == focusedPlayerId) it.copy(position = safe) else it }
    }

    private fun beginKickerApproach(preserveBallPosition: Boolean = false) {
        playersMovedThisTurn.clear()
        defensiveMoveHistory.clear()
        playerDragStartState = null
        shotKickerAnchor = null
        val focused = players.first { it.id == focusedPlayerId }
        val forward = if (focused.team == GameTeam.RED) redAttack else -redAttack
        val readyBallPosition = if (preserveBallPosition) ball.position else shotReadyBallPosition(forward)
        val targetY = (readyBallPosition.y - forward * kickerStandOff())
            .coerceIn(playerRadius, height - playerRadius)
        val sideOffset = if (preserveBallPosition) {
            sqrt((kickerStandOff() * kickerStandOff() -
                (targetY - readyBallPosition.y) * (targetY - readyBallPosition.y)).coerceAtLeast(0f))
        } else 0f
        val targetX = if (readyBallPosition.x + sideOffset <= width - playerRadius)
            readyBallPosition.x + sideOffset else readyBallPosition.x - sideOffset
        val target = Vector2(
            targetX.coerceIn(playerRadius, width - playerRadius),
            targetY,
        )
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

    /** Make room behind a resting ball before the next shot, including at the back wall. */
    private fun shotReadyBallPosition(forward: Float): Vector2 {
        val standOff = kickerStandOff()
        val minY = if (forward > 0f) wallInset + playerRadius + standOff else wallInset + ballRadius
        val maxY = if (forward < 0f) height - wallInset - playerRadius - standOff else height - wallInset - ballRadius
        return ball.position.copy(y = ball.position.y.coerceIn(minY, maxY))
    }

    /** Keep both kickers just outside their own hit area while facing the ball. */
    private fun kickerStandOff(): Float = playerCollisionDistance + kickerVisualGap
}

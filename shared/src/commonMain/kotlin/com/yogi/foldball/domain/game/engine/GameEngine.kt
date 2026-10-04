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
enum class GamePhase { AIMING, BALL_MOVING, KICKER_APPROACH }

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
)

data class GameSnapshot(
    val players: List<MatchPlayer>,
    val ball: MatchBall,
    val phase: GamePhase,
    val focusedPlayerId: String,
    val aimAngle: Float,
    val power: Float,
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

/** Deterministic, platform-independent movement and ball physics for the match feature. */
class GameEngine(private val width: Float, private val height: Float) {
    private val ballRadius = 10f
    private val playerRadius = 25f
    private val playerCollisionOffsetY = -43f
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
    private val playerVisualTopOffset = 68f
    private val playerCollisionDistance = ballRadius + playerRadius + playerCollisionOverlap
    private val movementRadius = 100f
    private val wallInset = 18f
    private val redAttack = -1f

    private var players = listOf(
        player("red-left", GameTeam.RED, .27f, .76f),
        player("red-center", GameTeam.RED, .50f, .76f),
        player("red-right", GameTeam.RED, .73f, .76f),
        player("blue-left", GameTeam.BLUE, .27f, .24f),
        player("blue-center", GameTeam.BLUE, .50f, .24f),
        player("blue-right", GameTeam.BLUE, .73f, .24f),
    )
    private var ball = MatchBall(Vector2(width * .5f, height * .5f), Vector2(0f, 0f), ballRadius, 0f, 0f, 0f, 0f)
    private var phase = GamePhase.AIMING
    private var focusedPlayerId = "red-center"
    private var aimAngle = 0f
    private var power = 0f
    private var approachFrom: Vector2? = null
    private var approachTo: Vector2? = null
    private var approachBallFrom: Vector2? = null
    private var approachBallTo: Vector2? = null
    private var approachElapsed = 0f
    private var lastContactPlayerId: String? = null
    private var contactCooldownRemaining = 0f

    init {
        moveFocusedBehindBall()
    }

    val snapshot: GameSnapshot get() = GameSnapshot(players.toList(), ball, phase, focusedPlayerId, aimAngle, power)

    fun moveInactivePlayer(id: String, delta: Vector2) {
        if (phase != GamePhase.AIMING || id == focusedPlayerId) return
        val player = players.firstOrNull { it.id == id } ?: return
        val proposed = player.position + delta
        val fromHome = proposed - player.home
        val distance = fromHome.length()
        val limited = if (distance > movementRadius) player.home + fromHome * (movementRadius / distance) else proposed
        val safe = Vector2(limited.x.coerceIn(playerRadius, width - playerRadius), limited.y.coerceIn(playerRadius, height - playerRadius))
        players = players.map { if (it.id == id) it.copy(position = safe) else it }
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
        lastContactPlayerId = null
        contactCooldownRemaining = 0f
        val player = players.first { it.id == focusedPlayerId }
        val forward = if (player.team == GameTeam.RED) redAttack else -redAttack
        val direction = Vector2(sin(aimAngle) * -forward, cos(aimAngle) * forward)
        val speed = 150f + power * 520f
        ball = ball.copy(
            velocity = direction * speed,
            verticalVelocity = 120f + power * 280f,
            impactSideX = 0f,
            impactSideY = 0f,
            impactStrength = 0f,
        )
        phase = GamePhase.BALL_MOVING
        power = 0f
        return true
    }

    /** Advances simulation using seconds; callers should use a frame clock, not an animation timer. */
    fun step(deltaSeconds: Float): GameSnapshot {
        val frameTime = deltaSeconds.coerceIn(0f, 1f / 30f)
        if (frameTime <= 0f) return snapshot
        val substepCount = ceil(frameTime / maxPhysicsStep).toInt().coerceAtLeast(1)
        val dt = frameTime / substepCount
        repeat(substepCount) { advanceStep(dt) }
        return snapshot
    }

    private val maxPhysicsStep = 1f / 120f

    private fun advanceStep(dt: Float) {
        if (phase == GamePhase.KICKER_APPROACH) {
            approachElapsed += dt
            val progress = (approachElapsed / .36f).coerceIn(0f, 1f)
            val eased = progress * progress * (3f - 2f * progress)
            val from = approachFrom ?: return
            val to = approachTo ?: return
            val position = from + (to - from) * eased
            players = players.map { if (it.id == focusedPlayerId) it.copy(position = position) else it }
            val ballFrom = approachBallFrom ?: return
            val ballTo = approachBallTo ?: return
            ball = ball.copy(position = ballFrom + (ballTo - ballFrom) * eased)
            if (progress >= 1f) {
                phase = GamePhase.AIMING
                approachFrom = null
                approachTo = null
                approachBallFrom = null
                approachBallTo = null
            }
            return
        }
        if (phase != GamePhase.BALL_MOVING) return

        advancePlayers(dt)
        contactCooldownRemaining = (contactCooldownRemaining - dt).coerceAtLeast(0f)

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
        if (position.y < minY || position.y > maxY) {
            impactSideY = if (position.y > maxY) 1f else -1f
            impactSideX = 0f
            impactStrength = (abs(velocity.y) / 600f).coerceIn(.25f, 1f)
            position = position.copy(y = position.y.coerceIn(minY, maxY))
            velocity = Vector2(velocity.x * wallTangentialRetention, -velocity.y * wallBounceRestitution)
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
        val movement = position - previousPosition
        val travelVelocity = if (dt > 0f) movement * (1f / dt) else Vector2(0f, 0f)
        val collision = players.mapNotNull { player ->
            if (player.id == lastContactPlayerId && contactCooldownRemaining > 0f) return@mapNotNull null
            val center = player.position + Vector2(0f, playerCollisionOffsetY)
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
            lastContactPlayerId = player.id
            contactCooldownRemaining = playerContactCooldown
        }

        ball = ball.copy(position = position, velocity = velocity, height = heightAboveGround,
            verticalVelocity = verticalVelocity, rotationX = rotationX, rotationY = rotationY,
            impactSideX = impactSideX, impactSideY = impactSideY, impactStrength = impactStrength,
            landingStrength = landingStrength)
        if (velocity.length() < 14f && heightAboveGround == 0f && verticalVelocity == 0f) {
            ball = ball.copy(velocity = Vector2(0f, 0f))
            aimAngle = 0f
            focusedPlayerId = players.minBy { (it.position - ball.position).length() }.id
            beginKickerApproach()
        }
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

    private fun player(id: String, team: GameTeam, xFraction: Float, yFraction: Float) = MatchPlayer(
        id, team, Vector2(width * xFraction, height * yFraction), Vector2(width * xFraction, height * yFraction),
    )

    /** Integrates ball-hit player motion with drag and the same setup movement boundary. */
    private fun advancePlayers(dt: Float) {
        players = players.map { player ->
            val nextPosition = player.position + player.velocity * dt
            val fromHome = nextPosition - player.home
            val distanceFromHome = fromHome.length()
            val limitedPosition = if (distanceFromHome > movementRadius) {
                player.home + fromHome * (movementRadius / distanceFromHome)
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
        val target = Vector2(readyBallPosition.x, readyBallPosition.y - forward * kickerStandOff(forward))
        val safe = Vector2(target.x.coerceIn(playerRadius, width - playerRadius), target.y.coerceIn(playerRadius, height - playerRadius))
        players = players.map { if (it.id == focusedPlayerId) it.copy(position = safe) else it }
    }

    private fun beginKickerApproach() {
        val focused = players.first { it.id == focusedPlayerId }
        val forward = if (focused.team == GameTeam.RED) redAttack else -redAttack
        val readyBallPosition = shotReadyBallPosition(forward)
        val target = Vector2(
            readyBallPosition.x.coerceIn(playerRadius, width - playerRadius),
            (readyBallPosition.y - forward * kickerStandOff(forward)).coerceIn(playerRadius, height - playerRadius),
        )
        approachFrom = focused.position
        approachTo = target
        approachBallFrom = ball.position
        approachBallTo = readyBallPosition
        approachElapsed = 0f
        phase = GamePhase.KICKER_APPROACH
    }

    /** Make room behind a resting ball before the next shot, including at the back wall. */
    private fun shotReadyBallPosition(forward: Float): Vector2 {
        val standOff = kickerStandOff(forward)
        val minY = if (forward > 0f) wallInset + playerVisualTopOffset + standOff else wallInset + ballRadius
        val maxY = if (forward < 0f) height - playerRadius - standOff else height - wallInset - ballRadius
        return ball.position.copy(y = ball.position.y.coerceIn(minY, maxY))
    }

    /** Keep both kickers just outside their own hit area while facing the ball. */
    private fun kickerStandOff(forward: Float): Float = if (forward < 0f) {
        -playerCollisionOffsetY + playerCollisionDistance + kickerVisualGap
    } else {
        (playerCollisionDistance + kickerVisualGap + playerCollisionOffsetY).coerceAtLeast(0f)
    }
}

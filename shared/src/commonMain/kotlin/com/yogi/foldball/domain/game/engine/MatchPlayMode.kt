package com.yogi.foldball.domain.game.engine

enum class MatchPlayMode(val label: String, val description: String) {
    CLASSIC("Classic", "Possession follows the ball."),
    ATTACK_DEFEND("Attack & Defend", "Attack for 60 seconds, then switch sides.")
}

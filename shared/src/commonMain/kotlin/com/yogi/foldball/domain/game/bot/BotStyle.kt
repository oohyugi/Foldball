package com.yogi.foldball.domain.game.bot

/** Tactical priorities are independent of search strength and execution accuracy. */
enum class BotStyle(val label: String, val description: String) {
    BALANCED("Balanced", "Balances defense, forward progress, and possession."),
    PARK_BUS("Park the Bus", "Closes goal lanes, protects its half, and favors safe possession."),
    KICK_RUSH("Kick & Rush", "Drives forward with strong shots and contests the next ball."),
    TOTAL_FOOTBALL(
        "Total Football",
        "Adapts formation to the ball and prioritizes the next possession."
    )
}

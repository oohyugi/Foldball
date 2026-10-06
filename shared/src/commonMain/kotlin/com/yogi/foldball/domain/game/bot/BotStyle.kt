package com.yogi.foldball.domain.game.bot

/** Tactical priorities are independent of search strength and execution accuracy. */
enum class BotStyle(val label: String, val description: String) {
    BALANCED("Balanced", "Seimbang antara pertahanan, progres bola, dan penguasaan."),
    PARK_BUS("Parkir Bus", "Tutup jalur gol, jaga area sendiri, dan pilih bola aman."),
    KICK_RUSH("Kick & Rush", "Dorong bola ke depan dengan tembakan kuat; siap berebut bola berikutnya."),
    TOTAL_FOOTBALL("Total Football", "Adaptasi formasi terhadap bola dan prioritaskan penguasaan berikutnya."),
}

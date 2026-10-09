package com.yogi.foldball.feature.home.presentation.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.yogi.foldball.core.ui.theme.*

private data class GuidePage(val title: String, val body: String)
private val guidePages = listOf(
    GuidePage("Score a goal", "Send the ball into the other team's goal. The opening toss chooses who kicks first automatically.\n\nIn Classic, win by reaching 3 goals, or lead when the 3-minute clock ends. A tie is a draw."),
    GuidePage("Move your defenders", "Before a shot, the defending team can move one standee. Drag it within its current zone or an empty neighboring zone. Other players, the ball and field edges block movement.\n\nTap Undo to try again, Done to confirm, or Skip to keep your formation. At opening kickoff, stay in your own half. Classic gives you 6 seconds to defend."),
    GuidePage("Aim, hold, release", "The highlighted standee is the kicker. Touch and hold it, then drag left or right to aim.\n\nThe colored fill on the aim guide rises and falls while you hold. Release when the power looks right to shoot. Green is low power; red is high power and more loft. Dragging up or down does not change power.\n\nYou have 8 seconds to shoot. If time runs out while charging, the shot releases automatically; otherwise possession changes."),
    GuidePage("Use the bounce", "Walls rebound the ball. Try an angled shot around a defender. High shots can clear standees, but must fit below the crossbar to score.\n\nIn Classic, when the ball stops, the nearest player usually becomes the next kicker. A defensive block wins possession; close distance ties favor the team that did not just kick. The team that concedes takes the next kickoff."),
    GuidePage("Attack & Defend", "Play two 60-second halves. The toss winner attacks first; teams swap roles for the second half. Most goals wins.\n\nThe defender moves one standee first. During the 8-second aiming timer, the attacker may also move one teammate other than the kicker, or shoot immediately. Undo is available before charging.\n\nThe same team keeps attacking throughout its half, even after a goal."),
    GuidePage("Try Practice", "Open Arena, then Practice. There are no turns or countdowns.\n\nUse + or − to choose 0–5 opponents. Drag opponents while the ball is still. Tap Move to place the kicker and ball anywhere safe, then Kick to aim and shoot.\n\nReset shot restarts the drill. Tuning lets you experiment with bounce and shot speed. Settings saves your preferred tuning; Use for matches applies it to your next match.\n\nHave fun, and thanks for playing!")
)

@Composable
fun HowToPlayDialog(onDismiss: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val current = guidePages[page]
    PaperDialog(
        title = "How to play",
        message = "",
        content = {
            Column(Modifier.fillMaxWidth().heightIn(max = 340.dp).verticalScroll(rememberScrollState()).semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(FoldballSpace.sm)) {
                Text("${page + 1} / ${guidePages.size} · ${current.title}", style = FoldballType.Label, color = FoldballColor.Ink)
                Text(current.body, style = FoldballType.Body, color = FoldballColor.Ink)
                if (page > 0) PaperButton("Previous", { page-- }, style = PaperButtonStyle.Secondary, compact = true)
            }
        },
        onDismiss = onDismiss,
        confirmText = if (page == guidePages.lastIndex) "Got it" else "Next",
        onConfirm = { if (page == guidePages.lastIndex) onDismiss() else page++ },
        dismissText = "Close"
    )
}

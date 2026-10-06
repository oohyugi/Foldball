package com.yogi.foldball.feature.home.presentation.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.yogi.foldball.domain.game.bot.BotStyle
import androidx.compose.foundation.layout.heightIn
import com.yogi.foldball.domain.game.bot.BotDifficulty
import com.yogi.foldball.core.ui.theme.PaperChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import com.yogi.foldball.core.ui.theme.PaperDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballScaffold
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.PaperButton
import com.yogi.foldball.core.ui.theme.PaperButtonStyle
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.core.ui.theme.TapeLabel

@Composable
fun HomeScreen(
    onMatchClick: () -> Unit,
    onLocalMatchClick: () -> Unit,
    onSandboxClick: () -> Unit,
    difficulty: BotDifficulty = BotDifficulty.MEDIUM,
    onDifficultyChange: (BotDifficulty) -> Unit = {},
    style: BotStyle = BotStyle.BALANCED,
    onStyleChange: (BotStyle) -> Unit = {},
) {
    var showDifficulty by remember { mutableStateOf(false) }
    FoldballScaffold(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TapeLabel("Paper football game")
            PaperCard(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                tilt = FoldballTilt.Hero,
                ruled = false,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("FOLDBALL", style = FoldballType.Display, color = FoldballColor.Ink, textAlign = TextAlign.Center)
                    Text("Fold the paper. Shoot for the goal.", style = FoldballType.Body, color = FoldballColor.Ink, textAlign = TextAlign.Center)
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(top = 30.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PaperButton("Play vs AI", { showDifficulty = true }, Modifier.fillMaxWidth())
                PaperButton("Play 2 Players", onLocalMatchClick, Modifier.fillMaxWidth(), style = PaperButtonStyle.Secondary)
                PaperButton("Sandbox", onSandboxClick, Modifier.fillMaxWidth(), style = PaperButtonStyle.Secondary)
            }
        }
    }
    if (showDifficulty) {
        PaperDialog(
            title = "VS AI",
            message = style.description,
            onDismiss = { showDifficulty = false },
            confirmText = "Play",
            dismissText = "Cancel",
            onConfirm = { showDifficulty = false; onMatchClick() },
            content = {
                Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Difficulty", style = FoldballType.Label, color = FoldballColor.Ink)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotDifficulty.entries.forEach { level ->
                            PaperChip(level.label, { onDifficultyChange(level) },
                                "AI difficulty ${level.label}: ${level.description}",
                                modifier = Modifier.weight(1f), selected = difficulty == level)
                        }
                    }
                    Text("Gaya bermain", style = FoldballType.Label, color = FoldballColor.Ink)
                    BotStyle.entries.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { tactic ->
                                PaperChip(tactic.label, { onStyleChange(tactic) },
                                    "AI style ${tactic.label}: ${tactic.description}",
                                    modifier = Modifier.weight(1f), selected = style == tactic)
                            }
                        }
                    }
                }
            },
        )
    }
}

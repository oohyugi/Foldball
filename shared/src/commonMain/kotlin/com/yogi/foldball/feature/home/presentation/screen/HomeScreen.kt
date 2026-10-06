package com.yogi.foldball.feature.home.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogi.foldball.core.ui.theme.CrumpledBall
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballScaffold
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.PaperButton
import com.yogi.foldball.core.ui.theme.PaperButtonStyle
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.core.ui.theme.PaperChip
import com.yogi.foldball.core.ui.theme.PaperDialog
import com.yogi.foldball.core.ui.theme.TapeLabel
import com.yogi.foldball.domain.game.bot.BotDifficulty
import com.yogi.foldball.domain.game.bot.BotStyle
import com.yogi.foldball.domain.team.TeamIdentity
import com.yogi.foldball.feature.home.presentation.components.MenuField
import com.yogi.foldball.gamekit.presentation.team.ClubLogo

@Composable
fun HomeScreen(
    onMatchClick: () -> Unit,
    onLocalMatchClick: () -> Unit,
    onSandboxClick: () -> Unit,
    difficulty: BotDifficulty = BotDifficulty.MEDIUM,
    onDifficultyChange: (BotDifficulty) -> Unit = {},
    style: BotStyle = BotStyle.BALANCED,
    onStyleChange: (BotStyle) -> Unit = {},
    club: TeamIdentity? = null,
    onEditClub: () -> Unit = {},
    onTeamClick: () -> Unit = {},
    onBallClick: () -> Unit = {},
    onArenaClick: () -> Unit = {},
    onHowToPlayClick: () -> Unit = {}
) {
    var showDifficulty by remember { mutableStateOf(false) }
    var showHowToPlayDialog by remember { mutableStateOf(false) }

    FoldballScaffold(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaperChip("📝", {}, "Edit your club")
                PaperChip("⚙️", {}, "Settings")
            }

            // Title Banner
            PaperCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                tilt = FoldballTilt.Hero,
                ruled = false
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Fold",
                            style = FoldballType.Display.copy(fontSize = 48.sp),
                            color = FoldballColor.Ink
                        )
                        Text(
                            "Ball",
                            style = FoldballType.Display.copy(fontSize = 48.sp),
                            color = FoldballColor.InkBlue
                        )
                        CrumpledBall(size = 40.dp)
                    }
                    TapeLabel("Paper football game")
                }
            }

            // Field Strip
            MenuField(Modifier.fillMaxWidth())

            // Play Mode Buttons
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PaperButton(
                    text = "PLAY VS AI",
                    onClick = { showDifficulty = true },
                    modifier = Modifier.fillMaxWidth(),
                    style = PaperButtonStyle.Primary
                )
                PaperButton(
                    text = "PLAY 2 PLAYER",
                    onClick = onLocalMatchClick,
                    modifier = Modifier.fillMaxWidth(),
                    style = PaperButtonStyle.Secondary
                )
            }

            // 2x2 Grid Cards (Team, Ball, Arena, How to play)
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PaperCard(
                        modifier = Modifier.weight(1f).clickable(onClick = onTeamClick),
                        ruled = true
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Team", style = FoldballType.Title, color = FoldballColor.Ink)
                            club?.let {
                                ClubLogo(it.logoId, it.name, Modifier.size(36.dp))
                            }
                        }
                    }
                    PaperCard(
                        modifier = Modifier.weight(1f).clickable(onClick = onBallClick),
                        ruled = true
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Ball", style = FoldballType.Title, color = FoldballColor.Ink)
                            CrumpledBall(size = 36.dp)
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PaperCard(
                        modifier = Modifier.weight(1f).clickable(onClick = onArenaClick),
                        ruled = true
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Arena", style = FoldballType.Title, color = FoldballColor.Ink)
                            Text("Sandbox", style = FoldballType.Label, color = FoldballColor.InkBlue)
                        }
                    }
                    PaperCard(
                        modifier = Modifier.weight(1f).clickable(onClick = {
                            showHowToPlayDialog = true
                            onHowToPlayClick()
                        }),
                        ruled = true
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("How to play", style = FoldballType.Title, color = FoldballColor.Ink)
                            Text("Rules & Guide", style = FoldballType.Label, color = FoldballColor.InkBlue)
                        }
                    }
                }
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
            onConfirm = {
                showDifficulty = false
                onMatchClick()
            },
            content = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Difficulty", style = FoldballType.Label, color = FoldballColor.Ink)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotDifficulty.entries.forEach { level ->
                            PaperChip(
                                level.label,
                                { onDifficultyChange(level) },
                                "AI difficulty ${level.label}: ${level.description}",
                                modifier = Modifier.weight(1f),
                                selected = difficulty == level
                            )
                        }
                    }
                    Text("Play style", style = FoldballType.Label, color = FoldballColor.Ink)
                    BotStyle.entries.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { tactic ->
                                PaperChip(
                                    tactic.label,
                                    { onStyleChange(tactic) },
                                    "AI style ${tactic.label}: ${tactic.description}",
                                    modifier = Modifier.weight(1f),
                                    selected = style == tactic
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    if (showHowToPlayDialog) {
        PaperDialog(
            title = "How to play",
            message = "Fold the paper standee, aim with your finger, and flick to shoot for the goal. Protect your half during formation phase and outsmart the AI or your opponent!",
            onDismiss = { showHowToPlayDialog = false },
            confirmText = "Got it",
            onConfirm = { showHowToPlayDialog = false }
        )
    }
}

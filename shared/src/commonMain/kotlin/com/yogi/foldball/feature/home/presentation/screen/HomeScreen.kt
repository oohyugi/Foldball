package com.yogi.foldball.feature.home.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.yogi.foldball.core.ui.theme.DeskMenuCard
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballScaffold
import com.yogi.foldball.core.ui.theme.FoldballSpace
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
import com.yogi.foldball.domain.game.engine.MatchFormat
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import com.yogi.foldball.domain.team.BallStyle
import com.yogi.foldball.domain.team.TeamIdentity
import com.yogi.foldball.domain.team.withAiOpponent
import com.yogi.foldball.feature.home.presentation.components.DeskMenuArt
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
    matchFormat: MatchFormat = MatchFormat.THREE_V_THREE,
    onMatchFormatChange: (MatchFormat) -> Unit = {},
    ballStyle: BallStyle = BallStyle.CLASSIC,
    onBallStyleChange: (BallStyle) -> Unit = {},
    club: TeamIdentity? = null,
    onClubClick: () -> Unit = {},
    onBallClick: () -> Unit = {},
    onArenaClick: () -> Unit = {},
    onHowToPlayClick: () -> Unit = {}
) {
    var showMatchSetup by remember { mutableStateOf(false) }
    var setupAgainstBot by remember { mutableStateOf(true) }
    var showHowToPlayDialog by remember { mutableStateOf(false) }
    var showBallStyleDialog by remember { mutableStateOf(false) }

    FoldballScaffold(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = FoldballSpace.sm, vertical = FoldballSpace.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FoldballSpace.md)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Fold", style = FoldballType.Display.copy(fontSize = 38.sp), color = FoldballColor.Paper)
                Text("Ball", style = FoldballType.Display.copy(fontSize = 38.sp), color = FoldballColor.Tape)
                CrumpledBall(Modifier.padding(start = FoldballSpace.sm), size = 32.dp, ballStyle = ballStyle)
            }
            Box(Modifier.fillMaxWidth()) {
                MenuField(
                    Modifier.fillMaxWidth().padding(top = FoldballSpace.sm),
                    ballStyle = ballStyle,
                    teams = MatchTeamConfiguration(red = club ?: MatchTeamConfiguration.Default.red).withAiOpponent(style)
                )
                TapeLabel("READY FOR KICKOFF", Modifier.align(Alignment.TopCenter), compact = true)
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FoldballSpace.sm)) {
                PaperButton("PLAY VS AI", {
                    setupAgainstBot = true
                    showMatchSetup = true
                }, Modifier.fillMaxWidth(), style = PaperButtonStyle.Primary)
                PaperButton("PLAY 2 PLAYER", {
                    setupAgainstBot = false
                    showMatchSetup = true
                }, Modifier.fillMaxWidth(), style = PaperButtonStyle.Secondary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FoldballSpace.md)) {
                DeskMenuCard(
                    title = club?.name ?: "Your Club",
                    subtitle = "Manage your club",
                    tilt = FoldballTilt.MenuCardOne,
                    onClick = onClubClick,
                    modifier = Modifier.weight(1f)
                ) {
                    ClubLogo(club?.logoId, "Club badge", Modifier.size(56.dp))
                }
                DeskMenuCard(
                    title = "Ball",
                    subtitle = ballStyle.label,
                    tilt = FoldballTilt.MenuCardTwo,
                    onClick = { showBallStyleDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    CrumpledBall(size = 52.dp, ballStyle = ballStyle)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FoldballSpace.md)) {
                DeskMenuCard(
                    title = "Arena",
                    subtitle = "Practice & explore",
                    tilt = FoldballTilt.MenuCardThree,
                    onClick = onArenaClick,
                    modifier = Modifier.weight(1f)
                ) { DeskMenuArt(notebook = false) }
                DeskMenuCard(
                    title = "How to play",
                    subtitle = "Learn the basics",
                    tilt = FoldballTilt.MenuCardFour,
                    onClick = {
                        showHowToPlayDialog = true
                        onHowToPlayClick()
                    },
                    modifier = Modifier.weight(1f)
                ) { DeskMenuArt(notebook = true) }
            }
        }
    }

    if (showMatchSetup) {
        PaperDialog(
            title = if (setupAgainstBot) "VS AI" else "2 Players",
            message = if (setupAgainstBot) "Choose your match format, then set up the AI." else "Choose the match format for both players.",
            onDismiss = { showMatchSetup = false },
            confirmText = "Play",
            dismissText = "Cancel",
            onConfirm = {
                showMatchSetup = false
                if (setupAgainstBot) onMatchClick() else onLocalMatchClick()
            },
            content = {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Match format", style = FoldballType.Label, color = FoldballColor.Ink)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MatchFormat.entries.forEach { option ->
                            PaperChip(
                                option.label,
                                { onMatchFormatChange(option) },
                                "${option.label}: ${option.description}",
                                modifier = Modifier.weight(1f),
                                selected = matchFormat == option
                            )
                        }
                    }
                    Text(matchFormat.description, style = FoldballType.Body, color = FoldballColor.InkMuted)
                    if (setupAgainstBot) {
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
            }
        )
    }

    if (showBallStyleDialog) {
        PaperDialog(
            title = "Choose Ball Style",
            message = "Select your paper ball style for matches and practice.",
            onDismiss = { showBallStyleDialog = false },
            confirmText = "Done",
            onConfirm = { showBallStyleDialog = false },
            content = {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BallStyle.entries.forEach { option ->
                        val selected = ballStyle == option
                        PaperCard(
                            modifier = Modifier.fillMaxWidth().clickable { onBallStyleChange(option) },
                            color = if (selected) FoldballColor.Tape else FoldballColor.Paper,
                            tilt = 0f,
                            ruled = false
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CrumpledBall(size = 40.dp, ballStyle = option)
                                Column(Modifier.weight(1f)) {
                                    Text(option.label, style = FoldballType.Label, color = FoldballColor.Ink)
                                    Text(option.description, style = FoldballType.Caption, color = FoldballColor.InkMuted)
                                }
                                Box(
                                    modifier = Modifier.size(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Text("✓", style = FoldballType.Label, color = FoldballColor.InkBlue)
                                    }
                                }
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

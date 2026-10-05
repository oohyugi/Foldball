package com.yogi.foldball.feature.match.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballShapes
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.core.ui.theme.PaperIconButton
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.icon_pause
import foldball.shared.generated.resources.icon_play
import org.jetbrains.compose.resources.painterResource

@Composable
fun MatchScoreboard(
    isPaused: Boolean,
    onPauseClick: () -> Unit,
    phaseSeconds: Int,
    activeTeam: GameTeam,
    blueScore: Int = 0,
    redScore: Int = 0,
    teamConfiguration: MatchTeamConfiguration = MatchTeamConfiguration.Default,
    modifier: Modifier = Modifier,
) {
    val activeTeamColor = if (activeTeam == GameTeam.BLUE) FoldballColor.InkBlue else FoldballColor.TeamRed
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PaperIconButton(
            painterResource(if (isPaused) Res.drawable.icon_play else Res.drawable.icon_pause),
            if (isPaused) "Resume match" else "Pause match",
            onPauseClick,
        )
        PaperCard(
            modifier = Modifier.weight(1f).heightIn(min = 54.dp),
            shape = FoldballShapes.Chip,
            tilt = FoldballTilt.Scoreboard,
            ruled = false,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(blueScore.toString(), style = FoldballType.Title, color = FoldballColor.InkBlue)
                        Text(teamConfiguration.forTeam(GameTeam.BLUE).name, style = FoldballType.Caption.copy(fontSize = 9.sp), color = FoldballColor.InkBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(":", style = FoldballType.Title, color = FoldballColor.Ink)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(redScore.toString(), style = FoldballType.Title, color = FoldballColor.TeamRed)
                        Text(teamConfiguration.forTeam(GameTeam.RED).name, style = FoldballType.Caption.copy(fontSize = 9.sp), color = FoldballColor.TeamRed, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        PaperCard(
            modifier = Modifier.width(56.dp).heightIn(min = 48.dp),
            shape = FoldballShapes.Chip,
            tilt = FoldballTilt.TapeLabel,
            ruled = false,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(phaseSeconds.toString(), style = FoldballType.Title, color = FoldballColor.Ink)
                Text(teamConfiguration.forTeam(activeTeam).jerseyLabel, style = FoldballType.Caption.copy(fontSize = 9.sp), color = activeTeamColor, maxLines = 1)
            }
        }
    }
}

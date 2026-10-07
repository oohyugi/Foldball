package com.yogi.foldball.feature.match.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.core.ui.theme.PaperIconButton
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import com.yogi.foldball.gamekit.presentation.team.ClubLogo
import com.yogi.foldball.gamekit.presentation.team.kitColor
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.icon_pause
import foldball.shared.generated.resources.icon_play
import org.jetbrains.compose.resources.painterResource

@Composable
fun MatchScoreboard(
    isPaused: Boolean,
    onPauseClick: () -> Unit,
    matchSecondsRemaining: Int = 180,
    blueScore: Int = 0,
    redScore: Int = 0,
    teamConfiguration: MatchTeamConfiguration = MatchTeamConfiguration.Default,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        PaperIconButton(
            painterResource(if (isPaused) Res.drawable.icon_play else Res.drawable.icon_pause),
            if (isPaused) "Resume match" else "Pause match",
            onPauseClick,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        PaperCard(
            modifier = Modifier.padding(horizontal = 52.dp).heightIn(min = 54.dp),
            shape = FoldballShapes.Chip,
            tilt = FoldballTilt.Scoreboard,
            ruled = false
        ) {
            Row(
                Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ClubLogo(
                        teamConfiguration.red.logoId,
                        "${teamConfiguration.red.name} club badge",
                        Modifier.size(32.dp)
                    )
                    Text(
                        teamConfiguration.red.jerseyLabel,
                        style = FoldballType.Label.copy(fontSize = 14.sp),
                        color = teamConfiguration.kitColor(GameTeam.RED),
                        maxLines = 1
                    )
                }
                Column(Modifier.width(68.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$redScore – $blueScore",
                        style = FoldballType.Title.copy(fontSize = 22.sp),
                        color = FoldballColor.Ink,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        "${matchSecondsRemaining / 60}:${(matchSecondsRemaining % 60).toString().padStart(
                            2,
                            '0'
                        )}",
                        style = FoldballType.Caption.copy(fontSize = 14.sp),
                        color = FoldballColor.Ink
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                ) {
                    Text(
                        teamConfiguration.blue.jerseyLabel,
                        style = FoldballType.Label.copy(fontSize = 14.sp),
                        color = teamConfiguration.kitColor(GameTeam.BLUE),
                        maxLines = 1
                    )
                    ClubLogo(
                        teamConfiguration.blue.logoId,
                        "${teamConfiguration.blue.name} club badge",
                        Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

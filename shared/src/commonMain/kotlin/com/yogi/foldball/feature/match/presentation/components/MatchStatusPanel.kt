package com.yogi.foldball.feature.match.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballShapes
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.PaperButton
import com.yogi.foldball.core.ui.theme.PaperButtonStyle
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.core.ui.theme.PaperChip
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameSnapshot
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import com.yogi.foldball.gamekit.presentation.team.kitColor
import kotlin.math.ceil

@Composable
fun MatchStatusPanel(
    snapshot: GameSnapshot,
    defensiveMovesUsed: Int,
    canUndo: Boolean,
    isPaused: Boolean,
    showPower: Boolean,
    onUndo: () -> Unit,
    onFinishDefense: () -> Unit,
    onEndTurn: () -> Unit = {},
    isBotTurn: Boolean = false,
    compactMatchBar: Boolean = false,
    teamConfiguration: MatchTeamConfiguration = MatchTeamConfiguration.Default,
    modifier: Modifier = Modifier
) {
    val activeTeam = snapshot.players.first { it.id == snapshot.focusedPlayerId }.team
    val defendingTeam = if (activeTeam == GameTeam.RED) GameTeam.BLUE else GameTeam.RED
    val shownTeam = if (snapshot.phase == GamePhase.DEFENDING) snapshot.formationTeam ?: defendingTeam else activeTeam
    val teamColor = if (shownTeam == GameTeam.RED) teamConfiguration.kitColor(GameTeam.RED) else teamConfiguration.kitColor(GameTeam.BLUE)
    val timed = snapshot.phase == GamePhase.DEFENDING || snapshot.phase == GamePhase.AIMING
    val seconds = ceil(snapshot.phaseTimeRemaining).toInt()
    val title = when (snapshot.phase) {
        GamePhase.DEFENDING -> if (snapshot.attackerPositioning) {
            "TEAM ${teamConfiguration.forTeam(activeTeam).name} POSITIONING"
        } else if (snapshot.openingKickoffPending) {
            "OPENING KICKOFF · ${teamConfiguration.forTeam(activeTeam).name}"
        } else {
            "TEAM ${teamConfiguration.forTeam(defendingTeam).name} DEFENDING"
        }

        GamePhase.AIMING -> "TEAM ${teamConfiguration.forTeam(activeTeam).name} ATTACKING"

        GamePhase.BALL_MOVING -> "BALL IN PLAY"

        GamePhase.TURNOVER_RETREAT -> "POSSESSION CHANGES"

        GamePhase.KICKER_APPROACH -> "KICKER APPROACHING"

        GamePhase.GOAL -> "TEAM ${snapshot.lastScoringTeam?.let {
            teamConfiguration.forTeam(it).name
        }} SCORES"

        GamePhase.MATCH_OVER -> "MATCH COMPLETE"
    }
    val detail = when {
        isPaused -> "MATCH PAUSED"
        snapshot.phase == GamePhase.DEFENDING && snapshot.attackerPositioning -> "$defensiveMovesUsed/1 attacker moved"
        snapshot.phase == GamePhase.DEFENDING && snapshot.openingKickoffPending -> "Defenders may move only within their own half"
        snapshot.phase == GamePhase.DEFENDING -> "$defensiveMovesUsed/2 players moved"
        snapshot.phase == GamePhase.AIMING -> "Aim · ${(snapshot.aimAngle * 180f / kotlin.math.PI.toFloat()).toInt()}°"
        snapshot.phase == GamePhase.TURNOVER_RETREAT -> "Previous kicker retreats"
        snapshot.phase == GamePhase.GOAL -> "Next kickoff coming up"
        else -> "Waiting for the ball to stop"
    }

    if (compactMatchBar) {
        when (snapshot.phase) {
            GamePhase.DEFENDING -> Box(
                modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        when {
                            snapshot.playerMovePreview?.valid == false -> "CANNOT PLACE HERE"
                            snapshot.playerMovePreview != null -> "RELEASE TO PLACE"
                            snapshot.openingFormation -> "ARRANGE YOUR HALF"
                            isBotTurn -> "BOT POSITIONING"
                            snapshot.attackerPositioning -> "MOVE 1 ATTACKER"
                            defensiveMovesUsed > 0 -> "DEFENDER MOVED"
                            else -> "MOVE 1 DEFENDER"
                        },
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = FoldballType.Caption.copy(fontSize = 12.sp),
                        color = FoldballColor.Ink
                    )
                    if (canUndo && !isBotTurn) {
                        PaperButton(
                            "Undo", onUndo,
                            enabled = !isPaused,
                            style = PaperButtonStyle.Secondary,
                            compact = true
                        )
                    }
                    PaperButton(
                        if (snapshot.openingFormation) "Ready" else if (defensiveMovesUsed > 0) "Done" else "Skip",
                        onFinishDefense,
                        enabled = !isPaused && !isBotTurn,
                        compact = true
                    )
                }
            }

            GamePhase.AIMING -> Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaperCard(
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp, max = 56.dp),
                        shape = FoldballShapes.Chip,
                        tilt = FoldballTilt.TapeLabel,
                        ruled = false
                    ) {
                        Text(
                            when {
                                snapshot.playerMovePreview?.valid == false -> "Cannot place here\nRelease to cancel"
                                snapshot.playerMovePreview != null -> "Release to place player"
                                isBotTurn -> "BOT IS AIMING"
                                snapshot.threeActionTurns -> "Move a teammate\nHold kicker to shoot"

                                snapshot.attackerPositioning && defensiveMovesUsed == 0 ->
                                    "Move a teammate or aim kicker\nHold for power, then release"

                                else -> "Drag kicker to aim\nHold for power, then release"
                            },
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = 12.dp),
                            style = FoldballType.Caption.copy(fontSize = 12.sp),
                            color = FoldballColor.Ink,
                            maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    if (snapshot.attackerPositioning && canUndo && !isBotTurn) {
                        PaperButton("Undo", onUndo, enabled = !isPaused,
                            style = PaperButtonStyle.Secondary, compact = true)
                    }
                    if (snapshot.threeActionTurns) {
                        PaperButton("End turn", onEndTurn, enabled = !isPaused && !isBotTurn && snapshot.power <= .01f,
                            style = PaperButtonStyle.Secondary, compact = true)
                    }
                }

            }

            else -> Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PaperCard(
                    modifier = Modifier.heightIn(min = 48.dp, max = 56.dp),
                    shape = FoldballShapes.Chip,
                    tilt = FoldballTilt.StatusPanel,
                    ruled = false
                ) {
                    Text(
                        if (isPaused) "MATCH PAUSED" else title,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
                        style = FoldballType.Caption.copy(fontSize = 12.sp),
                        color = teamColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        return
    }

    PaperCard(
        modifier = modifier.fillMaxWidth().heightIn(min = 96.dp),
        shape = FoldballShapes.Card,
        tilt = FoldballTilt.StatusPanel,
        ruled = true
    ) {
        Column(
            Modifier.fillMaxWidth().padding(
                start = 18.dp,
                end = 18.dp,
                top = 11.dp,
                bottom = 10.dp
            ),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    Modifier.weight(1f),
                    style = FoldballType.Label.copy(fontSize = 14.sp),
                    color = teamColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (timed) {
                    Text(
                        "${seconds}s",
                        style = FoldballType.Title,
                        color = if (seconds <=
                            3
                        ) {
                            teamConfiguration.kitColor(GameTeam.RED)
                        } else {
                            teamColor
                        }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    detail,
                    Modifier.weight(1f),
                    style = FoldballType.Caption.copy(fontSize = 12.sp),
                    color = FoldballColor.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showPower && snapshot.phase == GamePhase.AIMING) {
                    Text(
                        "${(snapshot.power * 100).toInt()}%",
                        style = FoldballType.Caption,
                        color = FoldballColor.Ink
                    )
                }
                if (!isPaused && canUndo) {
                    PaperChip(
                        "UNDO",
                        onUndo,
                        "Undo the last player move",
                        Modifier.padding(start = 6.dp)
                    )
                }
                if (!isPaused && snapshot.phase == GamePhase.DEFENDING) {
                    PaperChip(
                        "OK",
                        onFinishDefense,
                        if (snapshot.attackerPositioning) "Finish attacking positioning" else "Finish defensive positioning",
                        Modifier.padding(start = 6.dp)
                    )
                }
            }
            if (timed) {
                Box(Modifier.fillMaxWidth().height(5.dp)) {
                    Canvas(Modifier.matchParentSize()) {
                        drawRect(FoldballColor.PaperLine.copy(alpha = .72f))
                        val progress = (snapshot.phaseTimeRemaining / snapshot.phaseTimeTotal).coerceIn(
                            0f,
                            1f
                        )
                        drawRect(teamColor, size = size.copy(width = size.width * progress))
                    }
                }
            }
        }
    }
}

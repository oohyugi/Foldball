package com.yogi.foldball.feature.match.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogi.foldball.domain.game.engine.GamePhase
import com.yogi.foldball.domain.game.engine.GameSnapshot
import com.yogi.foldball.domain.game.engine.GameTeam
import com.yogi.foldball.presentation.theme.ClassroomTheme
import kotlin.math.PI
import kotlin.math.ceil

/** Paper note beneath the field; match decisions remain in GameEngine. */
@Composable
fun MatchStatusPanel(
    snapshot: GameSnapshot,
    defensiveMovesUsed: Int,
    canUndo: Boolean,
    isPaused: Boolean,
    showPower: Boolean,
    onUndo: () -> Unit,
    onFinishDefense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeTeam = snapshot.players.first { it.id == snapshot.focusedPlayerId }.team
    val defendingTeam = if (activeTeam == GameTeam.RED) GameTeam.BLUE else GameTeam.RED
    val shownTeam = if (snapshot.phase == GamePhase.DEFENDING) defendingTeam else activeTeam
    val teamColor = if (shownTeam == GameTeam.RED) ClassroomTheme.TeamRed else ClassroomTheme.TeamBlue
    val timed = snapshot.phase == GamePhase.DEFENDING || snapshot.phase == GamePhase.AIMING
    val seconds = ceil(snapshot.phaseTimeRemaining).toInt()
    val title = when (snapshot.phase) {
        GamePhase.DEFENDING -> "TIM ${defendingTeam.name} BERTAHAN"
        GamePhase.AIMING -> "TIM ${activeTeam.name} MENYERANG"
        GamePhase.BALL_MOVING -> "BOLA BERGERAK"
        GamePhase.TURNOVER_RETREAT -> "GANTI PENGUASAAN BOLA"
        GamePhase.KICKER_APPROACH -> "PENENDANG MENDEKATI BOLA"
        GamePhase.GOAL -> "GOL TIM ${snapshot.lastScoringTeam?.name}"
        GamePhase.MATCH_OVER -> "PERTANDINGAN SELESAI"
    }
    val detail = when {
        isPaused -> "PERTANDINGAN DIJEDA"
        snapshot.phase == GamePhase.DEFENDING -> "$defensiveMovesUsed/2 pemain dipindah"
        snapshot.phase == GamePhase.AIMING -> "Bidik · ${(snapshot.aimAngle * 180f / PI.toFloat()).toInt()}°"
        snapshot.phase == GamePhase.TURNOVER_RETREAT -> "Penendang lama mundur"
        snapshot.phase == GamePhase.GOAL -> "Kickoff berikutnya sebentar lagi"
        else -> "Tunggu bola berhenti"
    }

    Box(modifier.fillMaxWidth().heightIn(min = 91.dp)) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val paper = Path().apply {
                moveTo(9.dp.toPx(), 2.dp.toPx())
                lineTo(w * .22f, 0f)
                lineTo(w * .49f, 2.dp.toPx())
                lineTo(w * .74f, 0f)
                lineTo(w - 7.dp.toPx(), 3.dp.toPx())
                lineTo(w - 2.dp.toPx(), 13.dp.toPx())
                lineTo(w - 4.dp.toPx(), h - 10.dp.toPx())
                lineTo(w - 13.dp.toPx(), h - 2.dp.toPx())
                lineTo(w * .71f, h - 1.dp.toPx())
                lineTo(w * .47f, h - 3.dp.toPx())
                lineTo(w * .22f, h - 1.dp.toPx())
                lineTo(6.dp.toPx(), h - 4.dp.toPx())
                lineTo(2.dp.toPx(), h - 13.dp.toPx())
                lineTo(4.dp.toPx(), 12.dp.toPx())
                close()
            }
            translate(top = 3.dp.toPx()) { drawPath(paper, ClassroomTheme.PaperShadow) }
            drawPath(
                paper,
                Brush.verticalGradient(listOf(ClassroomTheme.Paper, Color(0xFFF0E5D2))),
            )
            drawPath(paper, ClassroomTheme.PaperEdge, style = Stroke(1.dp.toPx()))
            drawLine(teamColor, androidx.compose.ui.geometry.Offset(13.dp.toPx(), 14.dp.toPx()),
                androidx.compose.ui.geometry.Offset(13.dp.toPx(), h - 15.dp.toPx()), 3.dp.toPx())
        }
        Column(
            Modifier.fillMaxWidth().padding(start = 25.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), color = ClassroomTheme.Ink,
                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                if (timed) {
                    Text("${seconds}s", color = if (seconds <= 3) ClassroomTheme.TeamRed else teamColor,
                        fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(detail, Modifier.weight(1f), color = ClassroomTheme.Ink.copy(alpha = .72f),
                    fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!isPaused && canUndo) PaperStatusAction("Undo", onUndo)
                if (!isPaused && snapshot.phase == GamePhase.DEFENDING) {
                    PaperStatusAction("Selesai", onFinishDefense)
                }
                if (showPower && snapshot.phase == GamePhase.AIMING) {
                    Text("${(snapshot.power * 100).toInt()}%", color = ClassroomTheme.Ink,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (timed) {
                Canvas(Modifier.fillMaxWidth().height(5.dp)) {
                    val progress = (snapshot.phaseTimeRemaining / snapshot.phaseTimeTotal).coerceIn(0f, 1f)
                    drawRect(ClassroomTheme.PaperEdge.copy(alpha = .42f))
                    drawRect(teamColor, size = size.copy(width = size.width * progress))
                }
            }
        }
    }
}

@Composable
private fun PaperStatusAction(label: String, onClick: () -> Unit) {
    Box(
        Modifier.height(28.dp).widthIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val paper = Path().apply {
                moveTo(3.dp.toPx(), 2.dp.toPx())
                lineTo(size.width - 4.dp.toPx(), 1.dp.toPx())
                lineTo(size.width - 2.dp.toPx(), size.height - 4.dp.toPx())
                lineTo(4.dp.toPx(), size.height - 2.dp.toPx())
                close()
            }
            translate(top = 2.dp.toPx()) { drawPath(paper, ClassroomTheme.PaperShadow) }
            drawPath(paper, ClassroomTheme.Paper)
            drawPath(paper, ClassroomTheme.PaperEdge, style = Stroke(1.dp.toPx()))
        }
        Text(label, color = ClassroomTheme.Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

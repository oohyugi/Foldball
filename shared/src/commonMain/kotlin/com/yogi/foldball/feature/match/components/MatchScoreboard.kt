package com.yogi.foldball.feature.match.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogi.foldball.presentation.theme.ClassroomPaperActionButton
import com.yogi.foldball.presentation.theme.ClassroomPaperIcon
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.match_scoreboard_frame
import org.jetbrains.compose.resources.painterResource

@Composable
fun MatchScoreboard(
    elapsedSeconds: Int,
    isPaused: Boolean,
    onMenuClick: () -> Unit,
    onShareClick: () -> Unit,
    onPauseClick: () -> Unit,
    blueScore: Int = 0,
    redScore: Int = 0,
    modifier: Modifier = Modifier,
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val scoreboardWidth = maxWidth
        val frameHeight = scoreboardWidth * (140f / 1200f)
        Box(Modifier.fillMaxWidth().height(frameHeight)) {
            Image(
                painter = painterResource(Res.drawable.match_scoreboard_frame),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
            ClassroomPaperActionButton(
                icon = ClassroomPaperIcon.BOOK,
                contentDescription = "Menu pertandingan",
                onClick = onMenuClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = scoreboardWidth * .071f - 16.dp)
                    .width(32.dp)
                    .height(38.dp),
            )
            Text(
                "KLS 7A",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = scoreboardWidth * .11f)
                    .width(scoreboardWidth * .19f),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = .3.sp,
                maxLines = 1,
            )
            ScoreCard(
                score = blueScore.toString(),
                ink = Color(0xFF24314A),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = scoreboardWidth * .35f - 15.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = scoreboardWidth * .405f, y = frameHeight * .06f)
                    .width(scoreboardWidth * .19f)
                    .height(frameHeight * .54f)
                    .background(Color(0xFF26303D), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp,
                )
            }
            Text(
                text = "FIRST TO 3",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = scoreboardWidth * .405f, y = frameHeight * .66f)
                    .width(scoreboardWidth * .19f),
                color = Color(0xFF26303D),
                fontSize = 6.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = .25.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            ScoreCard(
                score = redScore.toString(),
                ink = Color(0xFFBD3031),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = scoreboardWidth * .657f - 15.dp),
            )
            Text(
                "KLS 7B",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = scoreboardWidth * .70f)
                    .width(scoreboardWidth * .16f),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = .3.sp,
                maxLines = 1,
            )
            ClassroomPaperActionButton(
                icon = ClassroomPaperIcon.PAPER_PLANE,
                contentDescription = "Bagikan pertandingan",
                onClick = onShareClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = scoreboardWidth * .895f - 15.dp)
                    .width(30.dp)
                    .height(36.dp),
            )
            ClassroomPaperActionButton(
                icon = if (isPaused) ClassroomPaperIcon.PLAY else ClassroomPaperIcon.PAUSE,
                contentDescription = if (isPaused) "Lanjutkan pertandingan" else "Jeda pertandingan",
                onClick = onPauseClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = scoreboardWidth * .973f - 15.dp)
                    .width(30.dp)
                    .height(36.dp),
            )
        }
    }
}

@Composable
private fun ScoreCard(score: String, ink: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.width(30.dp).height(38.dp)
            .background(Color(0xFFFFF8EA), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(score, color = ink, fontSize = 24.sp, fontWeight = FontWeight.Black)
    }
}

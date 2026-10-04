package com.yogi.foldball

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.yogi.foldball.feature.match.MatchSandboxScreen
import com.yogi.foldball.feature.balllab.BallLabScreen
import com.yogi.foldball.feature.playerlab.PlayerLabScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        var selectedScreen by remember { mutableStateOf("match") }
        Column(Modifier.fillMaxSize().background(Color(0xFFF5F0E6)).safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("match" to "Movement", "ball" to "Ball Lab", "player" to "Player Lab").forEach { (screen, title) ->
                    Surface(
                        onClick = { selectedScreen = screen },
                        modifier = Modifier.weight(1f).height(38.dp),
                        color = if (selectedScreen == screen) Color(0xFF24314A) else Color(0xFFFFFCF7),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(title, color = if (selectedScreen == screen) Color.White else Color(0xFF24314A), fontSize = 12.sp)
                        }
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                when (selectedScreen) {
                    "ball" -> BallLabScreen()
                    "player" -> PlayerLabScreen()
                    else -> MatchSandboxScreen()
                }
            }
        }
    }
}

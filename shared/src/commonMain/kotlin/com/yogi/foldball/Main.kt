package com.yogi.foldball

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogi.foldball.feature.balllab.BallLabScreen
import com.yogi.foldball.feature.home.HomeScreen
import com.yogi.foldball.feature.match.FullMatchScreen
import com.yogi.foldball.feature.match.MatchSandboxScreen
import com.yogi.foldball.feature.playerlab.PlayerLabScreen

/** Product entry point: start at Home, then enter Match or Sandbox. */
@Composable
@Preview
fun Main() {
    FoldballRoot(startAtHome = true)
}

/** Direct sandbox entry point for previews and developer builds. */
@Composable
fun SandboxMain() {
    FoldballRoot(startAtHome = false)
}

@Composable
private fun FoldballRoot(startAtHome: Boolean) {
    MaterialTheme {
        var route by remember { mutableStateOf(if (startAtHome) "home" else "sandbox") }
        var sandboxTab by remember { mutableStateOf("movement") }
        Surface(Modifier.fillMaxSize().safeDrawingPadding(), color = Color(0xFFF5F0E6)) {
            when (route) {
                "match" -> FullMatchScreen { destination ->
                    if (destination == "home") {
                        route = "home"
                    } else {
                        sandboxTab = destination
                        route = "sandbox"
                    }
                }
                "sandbox" -> SandboxContent(
                    selectedTab = sandboxTab,
                    onSelectTab = { sandboxTab = it },
                    onHomeClick = { route = "home" },
                )
                else -> HomeScreen(
                    onMatchClick = { route = "match" },
                    onSandboxClick = {
                        sandboxTab = "movement"
                        route = "sandbox"
                    },
                )
            }
        }
    }
}

@Composable
private fun SandboxContent(
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    onHomeClick: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F0E6))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                "home" to "Home",
                "movement" to "Movement",
                "ball" to "Ball Lab",
                "player" to "Player Lab",
            ).forEach { (tab, title) ->
                Surface(
                    onClick = { if (tab == "home") onHomeClick() else onSelectTab(tab) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    color = if (selectedTab == tab) Color(0xFF24314A) else Color(0xFFFFFCF7),
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            title,
                            color = if (selectedTab == tab) Color.White else Color(0xFF24314A),
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                "ball" -> BallLabScreen()
                "player" -> PlayerLabScreen()
                else -> MatchSandboxScreen()
            }
        }
    }
}

package com.yogi.foldball

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yogi.foldball.feature.balllab.presentation.screen.BallLabScreen
import com.yogi.foldball.feature.home.presentation.screen.HomeScreen
import com.yogi.foldball.feature.match.presentation.screen.FullMatchScreen
import com.yogi.foldball.feature.match.presentation.screen.MatchSandboxScreen
import com.yogi.foldball.feature.playerlab.presentation.screen.PlayerLabScreen
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballTheme
import com.yogi.foldball.core.ui.theme.FoldballSnackbarHost
import com.yogi.foldball.core.ui.theme.LocalFoldballSnackbarHostState
import com.yogi.foldball.core.ui.theme.PaperChip

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
    FoldballTheme {
        var route by remember { mutableStateOf(if (startAtHome) "home" else "sandbox") }
        var sandboxTab by remember { mutableStateOf("movement") }
        val snackbarHostState = remember { SnackbarHostState() }
        Surface(Modifier.fillMaxSize(), color = FoldballColor.Wood) {
            CompositionLocalProvider(LocalFoldballSnackbarHostState provides snackbarHostState) {
                Box(Modifier.fillMaxSize()) {
                    if (route == "match") {
                        FullMatchScreen { destination ->
                            if (destination == "home") route = "home" else {
                                sandboxTab = destination
                                route = "sandbox"
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                            when (route) {
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
                    FoldballSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .navigationBarsPadding().padding(bottom = 12.dp),
                    )
                }
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
    Column(Modifier.fillMaxSize().background(FoldballColor.Wood)) {
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
                PaperChip(
                    text = title,
                    onClick = { if (tab == "home") onHomeClick() else onSelectTab(tab) },
                    contentDescription = "Open $title",
                    modifier = Modifier.weight(1f),
                    selected = selectedTab == tab,
                )
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

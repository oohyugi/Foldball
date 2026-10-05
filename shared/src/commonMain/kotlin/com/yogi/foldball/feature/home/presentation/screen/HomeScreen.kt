package com.yogi.foldball.feature.home.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballScaffold
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.PaperButton
import com.yogi.foldball.core.ui.theme.PaperButtonStyle
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.core.ui.theme.TapeLabel

@Composable
fun HomeScreen(
    onMatchClick: () -> Unit,
    onSandboxClick: () -> Unit,
) {
    FoldballScaffold(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TapeLabel("Paper football game")
            PaperCard(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                tilt = FoldballTilt.Hero,
                ruled = false,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("FOLDBALL", style = FoldballType.Display, color = FoldballColor.Ink, textAlign = TextAlign.Center)
                    Text("Fold the paper. Shoot for the goal.", style = FoldballType.Body, color = FoldballColor.Ink, textAlign = TextAlign.Center)
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(top = 30.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PaperButton("Play", onMatchClick, Modifier.fillMaxWidth())
                PaperButton("Sandbox", onSandboxClick, Modifier.fillMaxWidth(), style = PaperButtonStyle.Secondary)
            }
        }
    }
}

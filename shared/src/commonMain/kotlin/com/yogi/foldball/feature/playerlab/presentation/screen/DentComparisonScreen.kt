package com.yogi.foldball.feature.playerlab.presentation.screen
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.*
import com.yogi.foldball.gamekit.presentation.player.*
@Composable fun DentComparisonScreen() {
    FoldballTheme {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Dent comparison · match size", style = FoldballType.Title)
            for (facing in PlayerFacing.entries) {
                for (direction in listOf(-1f, 1f)) {
                    Text(
                        "${facing.name} · ${if (direction < 0) "left" else "right"}",
                        style = FoldballType.Caption
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (amount in listOf(0f, .25f, .5f, 1f)) {
                            Column {
                                PaperPlayer(
                                    Modifier.size(76.dp),
                                    facing = facing,
                                    dentProgress = amount,
                                    dentDirectionX = direction,
                                    dentDirectionY = -.5f,
                                    focusState = PlayerFocusState.FOCUSED,
                                    name = "PLYR",
                                    number = "9"
                                )
                                Text("${(amount * 100).toInt()}%", style = FoldballType.Caption)
                            }
                        }
                    }
                }
            }
        }
    }
}

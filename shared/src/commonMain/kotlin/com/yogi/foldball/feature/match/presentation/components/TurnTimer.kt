package com.yogi.foldball.feature.match.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballShapes
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.PaperCard

@Composable
fun TurnTimer(seconds: Int, clubShortName: String) {
    PaperCard(
        Modifier.size(width = 52.dp, height = 48.dp).semantics {
            contentDescription = "$seconds seconds remaining in $clubShortName turn"
        },
        shape = FoldballShapes.Chip,
        tilt = FoldballTilt.StatusPanel,
        ruled = false,
        color = if (seconds <= 3) FoldballColor.Tape else FoldballColor.Paper
    ) {
        Box(
            Modifier.fillMaxSize().border(1.dp, FoldballColor.Ink, FoldballShapes.Chip),
            contentAlignment = Alignment.Center
        ) {
            Text("${seconds}s", style = FoldballType.Label.copy(fontSize = 24.sp), color = FoldballColor.Ink, maxLines = 1)
        }
    }
}

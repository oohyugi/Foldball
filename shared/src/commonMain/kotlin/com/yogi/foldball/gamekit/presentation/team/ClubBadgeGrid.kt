package com.yogi.foldball.gamekit.presentation.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballSpace
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.PaperCard

@Composable
fun ClubBadgeGrid(ids: List<Int>, selectedId: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 216.dp) 3 else 4
        Column(verticalArrangement = Arrangement.spacedBy(FoldballSpace.sm)) {
            ids.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FoldballSpace.sm)) {
                    row.forEach { id ->
                        PaperCard(
                            Modifier.weight(1f).heightIn(min = 64.dp).selectable(
                                selected = selectedId == id,
                                enabled = enabled,
                                role = Role.RadioButton,
                                onClick = { onSelect(id) }
                            ),
                            tilt = FoldballTilt.PaperCard,
                            ruled = false,
                            color = if (selectedId == id) FoldballColor.Tape else FoldballColor.Paper
                        ) {
                            ClubLogo(id, "Club badge $id", Modifier.align(Alignment.Center).padding(FoldballSpace.sm).size(48.dp))
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

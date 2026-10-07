package com.yogi.foldball.gamekit.presentation.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballSpace
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.PaperCard
import kotlin.math.floor

/** Bounded shortcuts; the owning screen supplies a separate full-catalog action. */
@Composable
fun ClubLogoStrip(
    ids: List<Int>,
    selectedId: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedId, ids) {
        val index = ids.indexOf(selectedId)
        if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none {
                it.index == index && it.offset >= 0 &&
                    it.offset + it.size <= listState.layoutInfo.viewportEndOffset
            }
        ) {
            listState.animateScrollToItem(index)
        }
    }
    BoxWithConstraints(modifier.height(80.dp)) {
        // Fit whole cards plus 35% of the next card as a persistent scroll cue.
        val usable = maxWidth.value - FoldballSpace.xs.value * 2
        val gap = FoldballSpace.sm.value
        val fullCards = floor((usable + gap) / (64f + gap) - .35f).toInt().coerceAtLeast(1)
        val cardWidth = ((usable - fullCards * gap) / (fullCards + .35f)).coerceAtLeast(64f).dp
        LazyRow(
            Modifier.fillMaxSize().selectableGroup(),
            state = listState,
            contentPadding = PaddingValues(FoldballSpace.xs),
            horizontalArrangement = Arrangement.spacedBy(FoldballSpace.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(ids, key = { it }) { id ->
                PaperCard(
                    Modifier.width(cardWidth).height(64.dp).selectable(
                        selected = selectedId == id,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(id) }
                    ),
                    tilt = FoldballTilt.PaperCard,
                    ruled = false,
                    color = if (selectedId == id) FoldballColor.Tape else FoldballColor.Paper
                ) {
                    ClubLogo(id, "Club badge $id", Modifier.align(Alignment.Center).size(48.dp))
                }
            }
        }
    }
}

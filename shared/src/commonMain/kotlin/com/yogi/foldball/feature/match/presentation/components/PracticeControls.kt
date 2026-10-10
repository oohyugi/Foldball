package com.yogi.foldball.feature.match.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yogi.foldball.core.ui.theme.*
import com.yogi.foldball.domain.game.engine.PhysicsTuning
import kotlin.math.roundToInt

@Composable
fun PracticeControls(count: Int, tuning: PhysicsTuning, onCount: (Int) -> Unit,
    onTuning: (PhysicsTuning) -> Unit, onReset: () -> Unit) {
    var showTuning by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(FoldballSpace.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FoldballSpace.sm)) {
            Text("Opponents: $count", Modifier.weight(1f), color = FoldballColor.Ink, style = FoldballType.Body)
            PaperChip("−", { onCount(count - 1) }, "Remove an opponent", enabled = count > 0)
            PaperChip("+", { onCount(count + 1) }, "Add an opponent", enabled = count < 5)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(FoldballSpace.sm)) {
            PaperButton("Reset shot", onReset, Modifier.weight(1f), style = PaperButtonStyle.Secondary, compact = true)
            PaperButton("Tuning", { showTuning = true }, Modifier.weight(1f), compact = true)
        }
        Text("Red circles: foul range after contact", color = FoldballColor.Ink, style = FoldballType.Caption)
    }
    if (showTuning) {
        var draft by remember(tuning) { mutableStateOf(tuning) }
        Dialog(onDismissRequest = { showTuning = false }) {
            PaperCard(Modifier.fillMaxWidth(), tilt = FoldballTilt.PaperCard) {
                Column(Modifier.padding(FoldballSpace.md).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(FoldballSpace.sm)) {
                    Text("Physics tuning", color = FoldballColor.Ink, style = FoldballType.Title)
                    Text("Apply resets the shot. Practice only.", color = FoldballColor.Ink, style = FoldballType.Body)
                    TuningSlider("Wall bounce", draft.wallBounce) { draft = draft.copy(wallBounce = it) }
                    TuningSlider("Player bounce", draft.playerBounce) { draft = draft.copy(playerBounce = it) }
                    TuningSlider("Ground bounce", draft.groundBounce) { draft = draft.copy(groundBounce = it) }
                    TuningSlider("Full-power forward speed", draft.lobForwardRetention, .55f) { draft = draft.copy(lobForwardRetention = it) }
                    PaperButton("Defaults", { draft = PhysicsTuning() }, Modifier.fillMaxWidth(), style = PaperButtonStyle.Secondary, compact = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(FoldballSpace.sm)) {
                        PaperButton("Cancel", { showTuning = false }, Modifier.weight(1f), style = PaperButtonStyle.Secondary, compact = true)
                        PaperButton("Apply", { onTuning(draft); showTuning = false }, Modifier.weight(1f), compact = true)
                    }
                }
            }
        }
    }
}

@Composable
fun TuningSlider(label: String, value: Float, minimum: Float = 0f, enabled: Boolean = true, onValue: (Float) -> Unit) {
    Column {
        Text("$label: ${(value * 100).roundToInt()}%", color = FoldballColor.Ink, style = FoldballType.Body)
        Slider(enabled = enabled, value = value, onValueChange = { onValue((it * 100).roundToInt() / 100f) },
            valueRange = minimum..1f, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = label })
    }
}

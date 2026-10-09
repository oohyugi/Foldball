package com.yogi.foldball.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.yogi.foldball.core.audio.LocalGameAudio
import com.yogi.foldball.app.navigation.AppUiState
import com.yogi.foldball.core.ui.theme.*
import com.yogi.foldball.domain.game.engine.PhysicsTuning
import com.yogi.foldball.feature.match.presentation.components.TuningSlider

@Composable
fun SettingsScreen(state: AppUiState, onBack: () -> Unit, onRetry: () -> Unit,
    onSave: (PhysicsTuning, Boolean) -> Unit) {
    var tuning by remember(state.settingsLoaded) { mutableStateOf(state.tuning) }
    var useForMatches by remember(state.settingsLoaded) { mutableStateOf(state.useTuningForMatches) }
    val enabled = state.settingsLoaded && !state.settingsSaving
    FoldballScaffold {
        Column(Modifier.fillMaxSize().padding(FoldballSpace.md), verticalArrangement = Arrangement.spacedBy(FoldballSpace.md)) {
            FoldballTopBar(title = "Settings", onBack = onBack)
            LocalGameAudio.current?.let { audio ->
                PaperCard(Modifier.fillMaxWidth(), tilt = FoldballTilt.PaperCard) {
                    PaperToggle("Music", audio.musicEnabled, audio::changeMusicEnabled, Modifier.padding(FoldballSpace.md))
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(FoldballSpace.md)) {
                PaperCard(Modifier.fillMaxWidth(), tilt = FoldballTilt.PaperCard) {
                    Column(Modifier.padding(FoldballSpace.md), verticalArrangement = Arrangement.spacedBy(FoldballSpace.sm)) {
                        Text("Advanced physics", color = FoldballColor.Ink, style = FoldballType.Title)
                        Text("Experimental. Practice uses these values. Match changes apply to your next match, equally for both teams and AI.", color = FoldballColor.Ink, style = FoldballType.Body)
                        if (state.settingsLoaded) {
                            TuningSlider("Wall bounce", tuning.wallBounce, enabled = enabled) { tuning = tuning.copy(wallBounce = it) }
                            TuningSlider("Player bounce", tuning.playerBounce, enabled = enabled) { tuning = tuning.copy(playerBounce = it) }
                            TuningSlider("Ground bounce", tuning.groundBounce, enabled = enabled) { tuning = tuning.copy(groundBounce = it) }
                            TuningSlider("Full-power forward speed", tuning.lobForwardRetention, .55f, enabled) { tuning = tuning.copy(lobForwardRetention = it) }
                            PaperToggle("Use for matches", useForMatches, { if (enabled) useForMatches = it })
                            PaperButton("Restore defaults", { tuning = PhysicsTuning(); useForMatches = false }, Modifier.fillMaxWidth(), enabled = enabled, style = PaperButtonStyle.Secondary, compact = true)
                        } else Text("Loading settings", color = FoldballColor.Ink, style = FoldballType.Body)
                        state.settingsError?.let {
                            Text(it, color = FoldballColor.Ink, style = FoldballType.Body)
                            if (!state.settingsLoaded) PaperButton("Retry", onRetry, compact = true)
                        }
                    }
                }
            }
            PaperButton(if (state.settingsSaving) "Saving" else "Save settings", { onSave(tuning, useForMatches) },
                Modifier.fillMaxWidth(), enabled = enabled)
        }
    }
}

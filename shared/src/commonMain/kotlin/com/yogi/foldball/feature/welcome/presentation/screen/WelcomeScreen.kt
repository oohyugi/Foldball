package com.yogi.foldball.feature.welcome.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballScaffold
import com.yogi.foldball.core.ui.theme.FoldballSpace
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.PaperButton
import com.yogi.foldball.core.ui.theme.PaperButtonStyle
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.core.ui.theme.PaperDialog
import com.yogi.foldball.domain.team.ClubBadges
import com.yogi.foldball.domain.team.TeamProfile
import com.yogi.foldball.feature.welcome.presentation.viewmodel.WelcomeEffect
import com.yogi.foldball.feature.welcome.presentation.viewmodel.WelcomeIntent
import com.yogi.foldball.feature.welcome.presentation.viewmodel.WelcomeUiState
import com.yogi.foldball.feature.welcome.presentation.viewmodel.WelcomeViewModel
import com.yogi.foldball.gamekit.presentation.team.ClubBadgeGrid
import com.yogi.foldball.gamekit.presentation.team.ClubLogo
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WelcomeRoute(onSaved: (TeamProfile) -> Unit) {
    val viewModel: WelcomeViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WelcomeEffect.Saved -> onSaved(effect.profile)
            }
        }
    }
    WelcomeScreen(state, viewModel::onIntent)
}

@Composable
fun WelcomeScreen(state: WelcomeUiState, onIntent: (WelcomeIntent) -> Unit) {
    var showAllBadges by rememberSaveable { mutableStateOf(false) }
    val editable = !state.loading && !state.saving && !state.loadFailed
    val defaults = ClubBadges.ids.take(4)
    val shortcuts = if (state.badgeId in defaults) defaults else defaults.take(3) + state.badgeId
    FoldballScaffold {
        Column(
            Modifier.fillMaxSize().imePadding().padding(vertical = FoldballSpace.md),
            verticalArrangement = Arrangement.spacedBy(FoldballSpace.md)
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = FoldballSpace.xs, vertical = FoldballSpace.xs),
                verticalArrangement = Arrangement.spacedBy(FoldballSpace.md)
            ) {
                PaperCard(Modifier.fillMaxWidth(), tilt = FoldballTilt.Banner, ruled = false) {
                    Column(
                        Modifier.fillMaxWidth().padding(FoldballSpace.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(FoldballSpace.sm)
                    ) {
                        Text("CREATE YOUR CLUB", style = FoldballType.Title, color = FoldballColor.Ink)
                        Text(
                            "Every paper legend starts with a club.",
                            style = FoldballType.Body,
                            color = FoldballColor.Ink,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(FoldballSpace.md)
                        ) {
                            ClubLogo(state.badgeId, "Selected club badge", Modifier.size(56.dp))
                            Text(
                                state.name.ifBlank { "My Club" },
                                modifier = Modifier.weight(1f),
                                style = FoldballType.Title,
                                color = FoldballColor.Ink
                            )
                        }
                    }
                }
                if (state.loading) {
                    Text("Loading…", style = FoldballType.Body, color = FoldballColor.Paper)
                } else {
                    state.error?.let { error ->
                        PaperCard(Modifier.fillMaxWidth(), ruled = false) {
                            Column(Modifier.padding(FoldballSpace.md)) {
                                Text(error, style = FoldballType.Body, color = FoldballColor.TeamRed)
                                if (state.loadFailed) {
                                    PaperButton("Retry", { onIntent(WelcomeIntent.Retry) }, enabled = !state.saving)
                                }
                            }
                        }
                    }
                    PaperCard(Modifier.fillMaxWidth(), tilt = FoldballTilt.StatusPanel, ruled = false) {
                        Column(
                            Modifier.padding(FoldballSpace.md),
                            verticalArrangement = Arrangement.spacedBy(FoldballSpace.sm)
                        ) {
                            OutlinedTextField(
                                value = state.name,
                                onValueChange = { onIntent(WelcomeIntent.NameChanged(it)) },
                                label = { Text("Club name", style = FoldballType.Caption) },
                                supportingText = {
                                    Text(
                                        "${state.name.length}/${TeamProfile.MAX_NAME_LENGTH} characters",
                                        style = FoldballType.Caption,
                                        color = FoldballColor.InkMuted
                                    )
                                },
                                singleLine = true,
                                enabled = !state.saving && !state.loadFailed,
                                textStyle = FoldballType.Body,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = state.shortName,
                                onValueChange = { onIntent(WelcomeIntent.ShortNameChanged(it)) },
                                label = {
                                    Text("Short name (up to 4 letters)", style = FoldballType.Caption)
                                },
                                placeholder = { Text("Auto", style = FoldballType.Body) },
                                singleLine = true,
                                enabled = !state.saving && !state.loadFailed,
                                textStyle = FoldballType.Body,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    ClubBadgeGrid(shortcuts, state.badgeId, editable) {
                        onIntent(WelcomeIntent.BadgeSelected(it))
                    }
                    PaperButton(
                        "See all badges",
                        { showAllBadges = true },
                        Modifier.fillMaxWidth(),
                        enabled = editable,
                        compact = true,
                        style = PaperButtonStyle.Secondary
                    )
                }
            }
            PaperButton(
                if (state.saving) "Creating club…" else "Continue to Foldball",
                { onIntent(WelcomeIntent.Save) },
                Modifier.fillMaxWidth(),
                enabled = state.canSave,
                style = PaperButtonStyle.Primary
            )
        }
    }
    if (showAllBadges) {
        PaperDialog(
            title = "Choose your badge",
            message = "Select a badge for your club.",
            onDismiss = { showAllBadges = false },
            confirmText = "Done",
            onConfirm = { showAllBadges = false },
            dismissText = null,
            content = {
                Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    ClubBadgeGrid(ClubBadges.ids, state.badgeId, editable) {
                        onIntent(WelcomeIntent.BadgeSelected(it))
                        showAllBadges = false
                    }
                }
            }
        )
    }
}

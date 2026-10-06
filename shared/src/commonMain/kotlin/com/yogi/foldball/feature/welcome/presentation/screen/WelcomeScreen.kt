package com.yogi.foldball.feature.welcome.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
import com.yogi.foldball.domain.team.ClubBadges
import com.yogi.foldball.domain.team.TeamProfile
import com.yogi.foldball.feature.welcome.presentation.viewmodel.WelcomeEffect
import com.yogi.foldball.feature.welcome.presentation.viewmodel.WelcomeIntent
import com.yogi.foldball.feature.welcome.presentation.viewmodel.WelcomeUiState
import com.yogi.foldball.feature.welcome.presentation.viewmodel.WelcomeViewModel
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
    FoldballScaffold {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(FoldballSpace.lg),
            verticalArrangement = Arrangement.spacedBy(FoldballSpace.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FoldballSpace.sm)
            ) {
                Text(
                    "WELCOME TO FOLDBALL",
                    style = FoldballType.LargeTitle,
                    color = FoldballColor.Ink,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Name your club and choose your crest badge to begin.",
                    style = FoldballType.Body,
                    color = FoldballColor.InkMuted,
                    textAlign = TextAlign.Center
                )
            }

            if (state.loading) {
                Text("Loading…", style = FoldballType.Body, color = FoldballColor.Ink)
                return@Column
            }

            state.error?.let { error ->
                Text(error, style = FoldballType.Body, color = FoldballColor.TeamRed)
                if (state.loadFailed) {
                    PaperButton("Retry", { onIntent(WelcomeIntent.Retry) }, enabled = !state.saving)
                }
            }

            // Preview Card
            PaperCard(
                modifier = Modifier.fillMaxWidth(),
                tilt = FoldballTilt.PaperCard,
                ruled = false
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(FoldballSpace.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(FoldballSpace.md)
                ) {
                    ClubLogo(
                        state.badgeId,
                        "Selected badge",
                        Modifier.size(80.dp)
                    )
                    Text(
                        state.name.ifEmpty { "My Club" },
                        style = FoldballType.Title,
                        color = FoldballColor.Ink
                    )
                }
            }

            // Details Card
            PaperCard(Modifier.fillMaxWidth(), tilt = FoldballTilt.StatusPanel, ruled = true) {
                Column(
                    Modifier.padding(FoldballSpace.lg),
                    verticalArrangement = Arrangement.spacedBy(FoldballSpace.md)
                ) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { onIntent(WelcomeIntent.NameChanged(it)) },
                        label = { Text("Team name", style = FoldballType.Caption) },
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
                    Text(
                        "${state.name.length}/${TeamProfile.MAX_NAME_LENGTH} characters",
                        style = FoldballType.Caption,
                        color = FoldballColor.InkMuted
                    )
                }
            }

            Text("CHOOSE CREST BADGE", style = FoldballType.Label, color = FoldballColor.Ink)
            ClubBadges.ids.chunked(4).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FoldballSpace.sm)
                ) {
                    row.forEach { id ->
                        PaperCard(
                            Modifier.weight(1f).heightIn(min = 64.dp).selectable(
                                selected = state.badgeId == id,
                                enabled = !state.saving && !state.loadFailed,
                                role = Role.RadioButton,
                                onClick = { onIntent(WelcomeIntent.BadgeSelected(id)) }
                            ),
                            tilt = FoldballTilt.PaperCard,
                            ruled = false,
                            color = if (state.badgeId ==
                                id
                            ) {
                                FoldballColor.Tape
                            } else {
                                FoldballColor.Paper
                            }
                        ) {
                            ClubLogo(
                                id,
                                "Club badge $id",
                                Modifier.align(
                                    Alignment.Center
                                ).padding(FoldballSpace.sm).size(52.dp)
                            )
                        }
                    }
                }
            }

            PaperButton(
                if (state.saving) "Creating Club…" else "Continue to Foldball",
                { onIntent(WelcomeIntent.Save) },
                Modifier.fillMaxWidth(),
                enabled = state.canSave,
                style = PaperButtonStyle.Primary
            )
        }
    }
}

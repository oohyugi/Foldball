package com.yogi.foldball.feature.team.presentation.screen

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballScaffold
import com.yogi.foldball.core.ui.theme.FoldballSpace
import com.yogi.foldball.core.ui.theme.FoldballTilt
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.PaperButton
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.domain.team.ClubBadges
import com.yogi.foldball.domain.team.TeamProfile
import com.yogi.foldball.feature.team.presentation.viewmodel.TeamEffect
import com.yogi.foldball.feature.team.presentation.viewmodel.TeamIntent
import com.yogi.foldball.feature.team.presentation.viewmodel.TeamUiState
import com.yogi.foldball.feature.team.presentation.viewmodel.TeamViewModel
import com.yogi.foldball.gamekit.presentation.team.ClubLogo
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TeamSetupRoute(onSaved: (TeamProfile) -> Unit, onCancel: () -> Unit) {
    val viewModel: TeamViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TeamEffect.Saved -> onSaved(effect.profile)
                TeamEffect.Cancelled -> onCancel()
            }
        }
    }
    TeamSetupScreen(state, viewModel::onIntent)
}

@Composable
fun TeamSetupScreen(state: TeamUiState, onIntent: (TeamIntent) -> Unit) {
    FoldballScaffold {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(FoldballSpace.lg),
            verticalArrangement = Arrangement.spacedBy(FoldballSpace.lg)
        ) {
            Text(
                if (state.editing) "YOUR TEAM" else "WELCOME TO FOLDBALL",
                style = FoldballType.LargeTitle,
                color = FoldballColor.Ink
            )
            Text(
                "Name your club and choose its badge.",
                style = FoldballType.Body,
                color = FoldballColor.Ink
            )
            if (state.loading) {
                Text("Loading your team…", style = FoldballType.Body, color = FoldballColor.Ink)
                return@Column
            }
            state.error?.let { error ->
                Text(error, style = FoldballType.Body, color = FoldballColor.TeamRed)
                if (state.loadFailed) {
                    PaperButton("Retry", { onIntent(TeamIntent.Retry) }, enabled = !state.saving)
                }
            }
            PaperCard(Modifier.fillMaxWidth(), tilt = FoldballTilt.StatusPanel, ruled = false) {
                Column(
                    Modifier.padding(FoldballSpace.lg),
                    verticalArrangement = Arrangement.spacedBy(FoldballSpace.sm)
                ) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { onIntent(TeamIntent.NameChanged(it)) },
                        label = { Text("Team name", style = FoldballType.Caption) },
                        singleLine = true,
                        enabled = !state.saving && !state.loadFailed,
                        textStyle = FoldballType.Body,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.shortName,
                        onValueChange = { onIntent(TeamIntent.ShortNameChanged(it)) },
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
            Text("CHOOSE YOUR BADGE", style = FoldballType.Label, color = FoldballColor.Ink)
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
                                onClick = { onIntent(TeamIntent.BadgeSelected(id)) }
                            ),
                            tilt = FoldballTilt.TapeLabel,
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
                                ).padding(FoldballSpace.sm).size(56.dp)
                            )
                        }
                    }
                }
            }
            Text(
                "Your opponent gets a different badge every match.",
                style = FoldballType.Caption,
                color = FoldballColor.Ink
            )
            PaperButton(
                if (state.saving) {
                    "Saving…"
                } else if (state.editing) {
                    "Save team"
                } else {
                    "Continue"
                },
                { onIntent(TeamIntent.Save) },
                Modifier.fillMaxWidth(),
                enabled = state.canSave
            )
            if (state.editing) {
                PaperButton("Cancel", {
                    onIntent(TeamIntent.Cancel)
                }, Modifier.fillMaxWidth(), enabled = !state.saving)
            }
        }
    }
}

package com.yogi.foldball.feature.club.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.yogi.foldball.core.ui.theme.FoldballTopBar
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.PaperButton
import com.yogi.foldball.core.ui.theme.PaperButtonStyle
import com.yogi.foldball.core.ui.theme.PaperCard
import com.yogi.foldball.core.ui.theme.PaperChip
import com.yogi.foldball.core.ui.theme.PaperDialog
import com.yogi.foldball.core.ui.theme.PaperIconButton
import com.yogi.foldball.domain.team.ClubBadges
import com.yogi.foldball.domain.team.JerseyPattern
import com.yogi.foldball.domain.team.TeamProfile
import com.yogi.foldball.feature.club.presentation.viewmodel.ClubEffect
import com.yogi.foldball.feature.club.presentation.viewmodel.ClubIntent
import com.yogi.foldball.feature.club.presentation.viewmodel.ClubUiState
import com.yogi.foldball.feature.club.presentation.viewmodel.ClubViewModel
import com.yogi.foldball.gamekit.presentation.player.PaperPlayer
import com.yogi.foldball.gamekit.presentation.team.ClubBadgeGrid
import com.yogi.foldball.gamekit.presentation.team.ClubKit
import com.yogi.foldball.gamekit.presentation.team.ClubLogo
import com.yogi.foldball.gamekit.presentation.team.ClubLogoStrip
import foldball.shared.generated.resources.Res
import foldball.shared.generated.resources.ic_edit
import foldball.shared.generated.resources.ic_grid
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ClubRoute(onSaved: (TeamProfile) -> Unit, onCancel: () -> Unit) {
    val viewModel: ClubViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ClubEffect.Saved -> onSaved(effect.profile)
                ClubEffect.Cancelled -> onCancel()
            }
        }
    }
    ClubScreen(state, viewModel::onIntent, onBack = onCancel)
}

@Composable
fun ClubScreen(
    state: ClubUiState,
    onIntent: (ClubIntent) -> Unit,
    onBack: () -> Unit = { onIntent(ClubIntent.Cancel) }
) {
    var showAllBadges by rememberSaveable { mutableStateOf(false) }
    var showEditName by rememberSaveable { mutableStateOf(false) }
    val editable = !state.loading && !state.saving && !state.loadFailed
    val defaults = ClubBadges.ids.take(10)
    val shortcuts = if (state.badgeId in defaults) defaults else defaults.take(9) + state.badgeId
    FoldballScaffold {
        Column(
            Modifier.fillMaxSize().imePadding().padding(vertical = FoldballSpace.md),
            verticalArrangement = Arrangement.spacedBy(FoldballSpace.md)
        ) {
            FoldballTopBar("Your Club", onBack = onBack, backEnabled = !state.saving)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = FoldballSpace.xs, vertical = FoldballSpace.xs),
                verticalArrangement = Arrangement.spacedBy(FoldballSpace.md)
            ) {
                if (state.loading) {
                    Text("Loading…", style = FoldballType.Body, color = FoldballColor.Paper)
                } else {
                    state.error?.let { error ->
                        PaperCard(Modifier.fillMaxWidth(), ruled = false) {
                            Column(Modifier.padding(FoldballSpace.md)) {
                                Text(error, style = FoldballType.Body, color = FoldballColor.TeamRed)
                                if (state.loadFailed) {
                                    PaperButton("Retry", { onIntent(ClubIntent.Retry) }, enabled = !state.saving)
                                }
                            }
                        }
                    }
                    PaperCard(Modifier.fillMaxWidth(), tilt = FoldballTilt.Banner, ruled = false) {
                        Column(
                            Modifier.fillMaxWidth().padding(FoldballSpace.md),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(FoldballSpace.sm)
                        ) {
                            ClubLogo(state.badgeId, "Selected club badge", Modifier.size(88.dp))
                            Text(
                                state.name.ifBlank { "My Club" },
                                style = FoldballType.Title,
                                color = FoldballColor.Ink,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                state.profile?.shortName ?: state.shortName,
                                style = FoldballType.Caption,
                                color = FoldballColor.InkMuted,
                                textAlign = TextAlign.Center
                            )
                            PaperIconButton(
                                painterResource(Res.drawable.ic_edit),
                                "Edit club name",
                                { showEditName = true },
                                enabled = editable
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FoldballSpace.sm)
                    ) {
                        ClubLogoStrip(
                            shortcuts,
                            state.badgeId,
                            editable,
                            onSelect = { onIntent(ClubIntent.BadgeSelected(it)) },
                            modifier = Modifier.weight(1f)
                        )
                        PaperIconButton(
                            painterResource(Res.drawable.ic_grid),
                            "See all badges",
                            { showAllBadges = true },
                            enabled = editable
                        )
                    }
                    PaperCard(Modifier.fillMaxWidth(), tilt = FoldballTilt.StatusPanel, ruled = false) {
                        Column(
                            Modifier.fillMaxWidth().padding(FoldballSpace.md),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(FoldballSpace.sm)
                        ) {
                            PaperPlayer(
                                modifier = Modifier.size(104.dp),
                                jerseyColor = ClubKit.primary(state.badgeId),
                                jerseyColorCount = state.jerseyColorCount,
                                jerseyPalette = ClubKit.palette(state.badgeId),
                                jerseyPattern = state.jerseyPattern ?: JerseyPattern.PLAIN,
                                name = state.shortName.ifBlank { "CLUB" },
                                number = "10"
                            )
                            Text("JERSEY COLORS", style = FoldballType.Label, color = FoldballColor.Ink)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FoldballSpace.xs)) {
                                (1..4).forEach { count ->
                                    PaperChip(
                                        "$count",
                                        { onIntent(ClubIntent.ColorCountSelected(count)) },
                                        "$count jersey colors",
                                        modifier = Modifier.weight(1f),
                                        selected = state.jerseyColorCount == count,
                                        enabled = editable
                                    )
                                }
                            }
                            Text("JERSEY PATTERN", style = FoldballType.Label, color = FoldballColor.Ink)
                            JerseyPattern.entries.filter { it != JerseyPattern.TWO_TONE }.chunked(2).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FoldballSpace.sm)) {
                                    row.forEach { pattern ->
                                        PaperChip(
                                            pattern.label,
                                            { onIntent(ClubIntent.PatternSelected(pattern)) },
                                            "Jersey pattern ${pattern.label}",
                                            modifier = Modifier.weight(1f),
                                            selected = (state.jerseyPattern?.takeUnless { it == JerseyPattern.TWO_TONE } ?: JerseyPattern.PLAIN) == pattern,
                                            enabled = editable
                                        )
                                    }
                                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FoldballSpace.sm)) {
                PaperButton(
                    "Cancel",
                    onBack,
                    Modifier.weight(1f),
                    enabled = !state.loading && !state.saving,
                    style = PaperButtonStyle.Secondary,
                    compact = true
                )
                PaperButton(
                    if (state.saving) "Saving…" else "Save club",
                    { onIntent(ClubIntent.Save) },
                    Modifier.weight(1f),
                    enabled = state.canSave,
                    style = PaperButtonStyle.Primary,
                    compact = true
                )
            }
        }
    }
    if (showEditName) {
        EditClubNameDialog(
            name = state.name,
            shortName = state.shortName,
            badgeId = state.badgeId,
            onDismiss = { showEditName = false },
            onApply = { name, shortName ->
                onIntent(ClubIntent.NameChanged(name))
                onIntent(ClubIntent.ShortNameChanged(shortName))
                showEditName = false
            }
        )
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
                        onIntent(ClubIntent.BadgeSelected(it))
                        showAllBadges = false
                    }
                }
            }
        )
    }
}

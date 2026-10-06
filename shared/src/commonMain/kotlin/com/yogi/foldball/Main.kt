package com.yogi.foldball

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.yogi.foldball.app.navigation.AppIntent
import com.yogi.foldball.app.navigation.AppRoute
import com.yogi.foldball.app.navigation.AppViewModel
import com.yogi.foldball.app.navigation.SandboxTab
import com.yogi.foldball.core.audio.LocalGameAudio
import com.yogi.foldball.core.audio.rememberGameAudio
import com.yogi.foldball.core.ui.theme.FoldballColor
import com.yogi.foldball.core.ui.theme.FoldballSnackbarHost
import com.yogi.foldball.core.ui.theme.FoldballTheme
import com.yogi.foldball.core.ui.theme.FoldballType
import com.yogi.foldball.core.ui.theme.LocalFoldballSnackbarHostState
import com.yogi.foldball.core.ui.theme.PaperButton
import com.yogi.foldball.core.ui.theme.PaperChip
import com.yogi.foldball.domain.game.engine.MatchTeamConfiguration
import com.yogi.foldball.feature.balllab.presentation.screen.BallLabScreen
import com.yogi.foldball.feature.home.presentation.screen.HomeScreen
import com.yogi.foldball.feature.match.presentation.screen.FullMatchScreen
import com.yogi.foldball.feature.match.presentation.screen.MatchSandboxScreen
import com.yogi.foldball.feature.playerlab.presentation.screen.PlayerLabScreen
import com.yogi.foldball.feature.team.presentation.screen.TeamSetupRoute
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Product entry point: start at Home, then enter Match or Sandbox. */
@Composable
fun Main() {
    FoldballRoot(startAtHome = true)
}

/** Direct sandbox entry point for previews and developer builds. */
@Composable
fun SandboxMain() {
    FoldballRoot(startAtHome = false)
}

@Composable
private fun FoldballRoot(startAtHome: Boolean) {
    FoldballTheme {
        val appViewModel: AppViewModel =
            koinViewModel(key = "app:$startAtHome", parameters = { parametersOf(startAtHome) })
        val state by appViewModel.uiState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val audio = rememberGameAudio()
        Surface(Modifier.fillMaxSize(), color = FoldballColor.Wood) {
            CompositionLocalProvider(
                LocalFoldballSnackbarHostState provides snackbarHostState,
                LocalGameAudio provides audio
            ) {
                Box(Modifier.fillMaxSize()) {
                    key(state.entryId) {
                        CompositionLocalProvider(LocalViewModelStoreOwner provides appViewModel) {
                            if (state.route == AppRoute.Match) {
                                FullMatchScreen(
                                    playAgainstBot = state.playAgainstBot,
                                    botDifficulty = state.difficulty,
                                    botStyle = state.style,
                                    teamConfiguration = MatchTeamConfiguration(
                                        red =
                                            state.profile?.toMatchIdentity()
                                                ?: MatchTeamConfiguration.Default.red
                                    )
                                ) { destination ->
                                    when (destination) {
                                        "home" -> appViewModel.onIntent(AppIntent.Home)

                                        "ball" -> appViewModel.onIntent(
                                            AppIntent.OpenSandbox(SandboxTab.BALL)
                                        )

                                        "player" -> appViewModel.onIntent(
                                            AppIntent.OpenSandbox(SandboxTab.PLAYER)
                                        )

                                        else -> appViewModel.onIntent(AppIntent.OpenSandbox())
                                    }
                                }
                            } else {
                                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                                    when (state.route) {
                                        AppRoute.Loading -> Column(Modifier.padding(24.dp)) {
                                            Text(
                                                state.error ?: "Loading your team…",
                                                style = FoldballType.Body,
                                                color = FoldballColor.Ink
                                            )
                                            if (state.error !=
                                                null
                                            ) {
                                                PaperButton("Retry", {
                                                    appViewModel.onIntent(
                                                        AppIntent.RetryProfile
                                                    )
                                                })
                                            }
                                        }

                                        AppRoute.Team -> TeamSetupRoute(
                                            onSaved = {
                                                appViewModel.onIntent(AppIntent.TeamSaved(it))
                                            },
                                            onCancel = { appViewModel.onIntent(AppIntent.Home) }
                                        )

                                        AppRoute.Sandbox -> SandboxContent(
                                            selectedTab = state.sandboxTab.name.lowercase(),
                                            onSelectTab = {
                                                appViewModel.onIntent(
                                                    AppIntent.SelectSandboxTab(
                                                        SandboxTab.valueOf(it.uppercase())
                                                    )
                                                )
                                            },
                                            onHomeClick = { appViewModel.onIntent(AppIntent.Home) }
                                        )

                                        else -> HomeScreen(
                                            club = state.profile?.toMatchIdentity(),
                                            onEditClub = {
                                                appViewModel.onIntent(AppIntent.EditTeam)
                                            },
                                            onTeamClick = {
                                                appViewModel.onIntent(AppIntent.EditTeam)
                                            },
                                            onBallClick = {
                                                appViewModel.onIntent(AppIntent.OpenSandbox(SandboxTab.BALL))
                                            },
                                            onArenaClick = {
                                                appViewModel.onIntent(AppIntent.OpenSandbox(SandboxTab.MOVEMENT))
                                            },
                                            onMatchClick = {
                                                appViewModel.onIntent(
                                                    AppIntent.Play(againstBot = true)
                                                )
                                            },
                                            onLocalMatchClick = {
                                                appViewModel.onIntent(
                                                    AppIntent.Play(againstBot = false)
                                                )
                                            },
                                            style = state.style,
                                            onStyleChange = {
                                                appViewModel.onIntent(AppIntent.StyleSelected(it))
                                            },
                                            difficulty = state.difficulty,
                                            onDifficultyChange = {
                                                appViewModel.onIntent(
                                                    AppIntent.DifficultySelected(it)
                                                )
                                            },
                                            onSandboxClick = {
                                                appViewModel.onIntent(AppIntent.OpenSandbox())
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    FoldballSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .navigationBarsPadding().padding(bottom = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SandboxContent(
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    onHomeClick: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(FoldballColor.Wood)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "home" to "Home",
                "movement" to "Movement",
                "ball" to "Ball Lab",
                "player" to "Player Lab"
            ).forEach { (tab, title) ->
                PaperChip(
                    text = title,
                    onClick = { if (tab == "home") onHomeClick() else onSelectTab(tab) },
                    contentDescription = "Open $title",
                    modifier = Modifier.weight(1f),
                    selected = selectedTab == tab
                )
            }
        }
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                "ball" -> BallLabScreen()
                "player" -> PlayerLabScreen()
                else -> MatchSandboxScreen()
            }
        }
    }
}

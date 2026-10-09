package com.yogi.foldball

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn
import com.yogi.foldball.feature.settings.presentation.SettingsScreen
import com.yogi.foldball.domain.game.engine.PhysicsTuning
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.yogi.foldball.feature.club.presentation.screen.ClubRoute
import com.yogi.foldball.feature.home.presentation.screen.HomeScreen
import com.yogi.foldball.feature.match.presentation.screen.FullMatchScreen
import com.yogi.foldball.feature.match.presentation.screen.MatchPracticeScreen
import com.yogi.foldball.feature.match.presentation.screen.MatchSandboxScreen
import com.yogi.foldball.feature.playerlab.presentation.screen.PlayerLabScreen
import com.yogi.foldball.feature.welcome.presentation.screen.WelcomeRoute
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
        val navController = rememberNavController()
        fun returnHome() {
            val destination = if (appViewModel.uiState.value.profile == null) "welcome" else "home"
            if (!navController.popBackStack(destination, inclusive = false)) {
                navController.navigate(destination) {
                    popUpTo(navController.graph.id) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
        fun openSandbox(tab: SandboxTab) {
            appViewModel.onIntent(AppIntent.OpenSandbox(tab))
            navController.navigate("sandbox") { launchSingleTop = true }
        }
        LaunchedEffect(state.initialRoute) {
            val destination = when (state.initialRoute) {
                AppRoute.Loading -> return@LaunchedEffect
                AppRoute.Home -> "home"
                AppRoute.Welcome -> "welcome"
                AppRoute.Club -> "club"
                AppRoute.Sandbox -> "sandbox"
                AppRoute.Match -> "match"
            }
            if (navController.currentDestination?.route == "loading") {
                navController.navigate(destination) {
                    popUpTo("loading") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        LaunchedEffect(appViewModel, navController) {
            appViewModel.destinations.collect { destination ->
                when (destination) {
                    AppRoute.Club -> navController.navigate("club") { launchSingleTop = true }
                    AppRoute.Welcome -> returnHome()
                    else -> Unit
                }
            }
        }
        val snackbarHostState = remember { SnackbarHostState() }
        val audio = rememberGameAudio()
        Surface(Modifier.fillMaxSize(), color = FoldballColor.Wood) {
            CompositionLocalProvider(
                LocalFoldballSnackbarHostState provides snackbarHostState,
                LocalGameAudio provides audio
            ) {
                Box(Modifier.fillMaxSize()) {
                    NavHost(
                        navController,
                        startDestination = "loading",
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                    ) {
                        composable("loading") {
                            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                                Column(Modifier.padding(24.dp)) {
                                    state.error?.let { error ->
                                        Text(error, style = FoldballType.Body, color = FoldballColor.Paper)
                                        PaperButton("Retry", {
                                            appViewModel.onIntent(
                                                AppIntent.RetryProfile
                                            )
                                        })
                                    }
                                }
                            }
                        }
                        composable("home") {
                            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                                HomeScreen(
                                    club = state.profile?.toMatchIdentity(),
                                    ballStyle = state.ballStyle,
                                    onBallStyleChange = {
                                        appViewModel.onIntent(AppIntent.BallStyleSelected(it))
                                    },
                                    onClubClick = {
                                        appViewModel.onIntent(AppIntent.EditClub)
                                    },
                                    onBallClick = {
                                        openSandbox(SandboxTab.BALL)
                                    },
                                    onArenaClick = {
                                        openSandbox(SandboxTab.MOVEMENT)
                                    },
                                    onSettingsClick = { navController.navigate("settings") { launchSingleTop = true } },
                                    onMatchClick = {
                                        appViewModel.onIntent(
                                            AppIntent.Play(againstBot = true)
                                        )
                                        navController.navigate("match") { launchSingleTop = true }
                                    },
                                    onLocalMatchClick = {
                                        appViewModel.onIntent(
                                            AppIntent.Play(againstBot = false)
                                        )
                                        navController.navigate("match") { launchSingleTop = true }
                                    },
                                    style = state.style,
                                    onStyleChange = {
                                        appViewModel.onIntent(AppIntent.StyleSelected(it))
                                    },
                                    matchFormat = state.matchFormat,
                                    matchPlayMode = state.matchPlayMode,
                                    onMatchPlayModeChange = {
                                        appViewModel.onIntent(AppIntent.MatchPlayModeSelected(it))
                                    },
                                    onMatchFormatChange = {
                                        appViewModel.onIntent(AppIntent.MatchFormatSelected(it))
                                    },
                                    difficulty = state.difficulty,
                                    onDifficultyChange = {
                                        appViewModel.onIntent(
                                            AppIntent.DifficultySelected(it)
                                        )
                                    },
                                    onSandboxClick = {
                                        openSandbox(SandboxTab.MOVEMENT)
                                    }
                                )
                            }
                        }
                        composable("welcome") {
                            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                                WelcomeRoute(onSaved = {
                                    appViewModel.onIntent(AppIntent.ClubSaved(it))
                                    navController.navigate("home") {
                                        popUpTo("welcome") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                })
                            }
                        }
                        composable("settings") {
                            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                                SettingsScreen(state, { returnHome() }, { appViewModel.onIntent(AppIntent.RetrySettings) },
                                    { tuning, use -> appViewModel.onIntent(AppIntent.SaveSettings(tuning, use)) })
                            }
                        }
                        composable("club") {
                            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                                ClubRoute(
                                    onSaved = {
                                        appViewModel.onIntent(AppIntent.ClubSaved(it))
                                        returnHome()
                                    },
                                    onCancel = { returnHome() }
                                )
                            }
                        }
                        composable("sandbox") {
                            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                                SandboxContent(
                                    tuning = state.tuning,
                                    selectedTab = state.sandboxTab.name.lowercase(),
                                    onSelectTab = {
                                        appViewModel.onIntent(
                                            AppIntent.SelectSandboxTab(
                                                SandboxTab.valueOf(it.uppercase())
                                            )
                                        )
                                    },
                                    onHomeClick = { returnHome() }
                                )
                            }
                        }
                        composable("match") {
                            val matchTuning = remember { if (state.useTuningForMatches) state.tuning else PhysicsTuning() }
                            FullMatchScreen(
                                physicsTuning = matchTuning,
                                playAgainstBot = state.playAgainstBot,
                                botDifficulty = state.difficulty,
                                botStyle = state.style,
                                matchFormat = state.matchFormat,
                                matchPlayMode = state.matchPlayMode,
                                ballStyle = state.ballStyle,
                                teamConfiguration = MatchTeamConfiguration(
                                    red =
                                        state.profile?.toMatchIdentity()
                                            ?: MatchTeamConfiguration.Default.red
                                )
                            ) { destination ->
                                when (destination) {
                                    "home" -> returnHome()
                                    "ball" -> openSandbox(SandboxTab.BALL)
                                    "player" -> openSandbox(SandboxTab.PLAYER)
                                    else -> openSandbox(SandboxTab.MOVEMENT)
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
    tuning: PhysicsTuning,
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    onHomeClick: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(FoldballColor.Wood)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "home" to "Home",
                "movement" to "Movement",
                "practice" to "Practice",
                "ball" to "Ball Lab",
                "player" to "Player Lab"
            ).forEach { (tab, title) ->
                PaperChip(
                    text = title,
                    onClick = { if (tab == "home") onHomeClick() else onSelectTab(tab) },
                    contentDescription = "Open $title",
                    modifier = Modifier.widthIn(min = 64.dp),
                    selected = selectedTab == tab
                )
            }
        }
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                "practice" -> MatchPracticeScreen(tuning)
                "ball" -> BallLabScreen()
                "player" -> PlayerLabScreen()
                else -> MatchSandboxScreen()
            }
        }
    }
}

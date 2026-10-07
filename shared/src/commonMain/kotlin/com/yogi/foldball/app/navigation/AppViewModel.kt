package com.yogi.foldball.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yogi.foldball.domain.repository.TeamProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    private val repository: TeamProfileRepository,
    private val startAtHome: Boolean
) : ViewModel() {
    private val state = MutableStateFlow(AppUiState())
    val uiState = state.asStateFlow()
    private val navigation = Channel<AppRoute>(Channel.BUFFERED)
    val destinations = navigation.receiveAsFlow()
    private var profileJob: Job? = null

    init {
        observeProfile()
    }

    fun onIntent(intent: AppIntent) {
        when (intent) {
            AppIntent.RetryProfile -> observeProfile()

            AppIntent.EditClub -> {
                navigation.trySend(if (state.value.profile == null) AppRoute.Welcome else AppRoute.Club)
            }

            is AppIntent.ClubSaved -> {
                state.update { it.copy(profile = intent.profile) }
            }

            is AppIntent.Play -> if (state.value.profile != null) {
                state.update { it.copy(playAgainstBot = intent.againstBot) }
            }

            is AppIntent.DifficultySelected -> state.update {
                it.copy(difficulty = intent.difficulty)
            }

            is AppIntent.StyleSelected -> state.update { it.copy(style = intent.style) }

            is AppIntent.BallStyleSelected -> state.update { it.copy(ballStyle = intent.style) }

            is AppIntent.OpenSandbox -> {
                state.update { it.copy(sandboxTab = intent.tab) }
            }

            is AppIntent.SelectSandboxTab -> state.update { it.copy(sandboxTab = intent.tab) }
        }
    }

    private fun observeProfile() {
        profileJob?.cancel()
        state.update { it.copy(error = null) }
        profileJob = viewModelScope.launch {
            try {
                repository.profile.collect { profile ->
                    state.update { current ->
                        current.copy(
                            profile = profile,
                            error = null,
                            initialRoute = if (current.initialRoute == AppRoute.Loading) {
                                when {
                                    !startAtHome -> AppRoute.Sandbox
                                    profile == null -> AppRoute.Welcome
                                    else -> AppRoute.Home
                                }
                            } else {
                                current.initialRoute
                            }
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update { it.copy(error = "Couldn't load your team. Please retry.") }
            }
        }
    }
}

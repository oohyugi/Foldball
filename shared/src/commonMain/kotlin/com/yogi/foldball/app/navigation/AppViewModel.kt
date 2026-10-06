package com.yogi.foldball.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.yogi.foldball.domain.repository.TeamProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    private val repository: TeamProfileRepository,
    private val startAtHome: Boolean
) : ViewModel(),
    ViewModelStoreOwner {
    // Retained with the app ViewModel, cleared on navigation and when the app owner ends.
    override val viewModelStore = ViewModelStore()
    private val state = MutableStateFlow(AppUiState())
    val uiState = state.asStateFlow()
    private var profileJob: Job? = null

    init {
        observeProfile()
    }

    fun onIntent(intent: AppIntent) {
        when (intent) {
            AppIntent.RetryProfile -> observeProfile()

            AppIntent.Home -> navigate(
                if (state.value.profile ==
                    null
                ) {
                    AppRoute.Team
                } else {
                    AppRoute.Home
                }
            )

            AppIntent.EditTeam -> navigate(AppRoute.Team)

            is AppIntent.TeamSaved -> {
                state.update { it.copy(profile = intent.profile) }
                navigate(AppRoute.Home)
            }

            is AppIntent.Play -> if (state.value.profile != null) {
                state.update { it.copy(playAgainstBot = intent.againstBot) }
                navigate(AppRoute.Match)
            }

            is AppIntent.DifficultySelected -> state.update {
                it.copy(difficulty = intent.difficulty)
            }

            is AppIntent.StyleSelected -> state.update { it.copy(style = intent.style) }

            is AppIntent.OpenSandbox -> {
                state.update { it.copy(sandboxTab = intent.tab) }
                navigate(AppRoute.Sandbox)
            }

            is AppIntent.SelectSandboxTab -> state.update { it.copy(sandboxTab = intent.tab) }
        }
    }

    private fun navigate(route: AppRoute) {
        if (state.value.route == route) return
        viewModelStore.clear()
        state.update { it.copy(route = route, entryId = it.entryId + 1) }
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
                            route = if (current.route == AppRoute.Loading) {
                                when {
                                    !startAtHome -> AppRoute.Sandbox
                                    profile == null -> AppRoute.Team
                                    else -> AppRoute.Home
                                }
                            } else {
                                current.route
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

    override fun onCleared() {
        viewModelStore.clear()
    }
}

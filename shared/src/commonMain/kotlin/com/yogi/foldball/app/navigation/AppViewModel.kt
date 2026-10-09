package com.yogi.foldball.app.navigation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.yogi.foldball.domain.game.engine.PhysicsTuning
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
    private val startAtHome: Boolean,
    private val settingsStore: DataStore<Preferences>? = null
) : ViewModel() {
    private val state = MutableStateFlow(AppUiState())
    val uiState = state.asStateFlow()
    private val navigation = Channel<AppRoute>(Channel.BUFFERED)
    val destinations = navigation.receiveAsFlow()
    private var profileJob: Job? = null

    private var settingsJob: Job? = null
    init {
        observeSettings()
        observeProfile()
    }

    fun onIntent(intent: AppIntent) {
        when (intent) {
            AppIntent.RetrySettings -> observeSettings()
            is AppIntent.SaveSettings -> {
                val store = settingsStore ?: return
                if (!state.value.settingsLoaded || state.value.settingsSaving) return
                state.update { it.copy(settingsSaving = true, settingsError = null) }
                viewModelScope.launch {
                    try {
                        store.edit {
                            it[floatPreferencesKey("physics_wall")] = intent.tuning.wallBounce
                            it[floatPreferencesKey("physics_player")] = intent.tuning.playerBounce
                            it[floatPreferencesKey("physics_ground")] = intent.tuning.groundBounce
                            it[floatPreferencesKey("physics_forward")] = intent.tuning.lobForwardRetention
                            it[booleanPreferencesKey("physics_matches")] = intent.useForMatches
                        }
                        state.update { it.copy(tuning = intent.tuning, useTuningForMatches = intent.useForMatches, settingsSaving = false) }
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { state.update { it.copy(settingsSaving = false, settingsError = "Couldn't save settings. Please retry.") } }
                }
            }
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

            is AppIntent.MatchFormatSelected -> state.update { it.copy(matchFormat = intent.format) }

            is AppIntent.MatchPlayModeSelected -> state.update { it.copy(matchPlayMode = intent.mode) }

            is AppIntent.BallStyleSelected -> state.update { it.copy(ballStyle = intent.style) }

            is AppIntent.OpenSandbox -> {
                state.update { it.copy(sandboxTab = intent.tab) }
            }

            is AppIntent.SelectSandboxTab -> state.update { it.copy(sandboxTab = intent.tab) }
        }
    }

    private fun observeSettings() {
        val store = settingsStore ?: return
        settingsJob?.cancel()
        state.update { it.copy(settingsError = null) }
        settingsJob = viewModelScope.launch {
            try {
                store.data.collect { preferences ->
                    val tuning = PhysicsTuning(
                        (preferences[floatPreferencesKey("physics_wall")] ?: PhysicsTuning().wallBounce).coerceIn(0f, 1f),
                        (preferences[floatPreferencesKey("physics_player")] ?: .5f).coerceIn(0f, 1f),
                        (preferences[floatPreferencesKey("physics_ground")] ?: PhysicsTuning().groundBounce).coerceIn(0f, 1f),
                        (preferences[floatPreferencesKey("physics_forward")] ?: 1f).coerceIn(.55f, 1f)
                    )
                    state.update { it.copy(tuning = tuning, useTuningForMatches = preferences[booleanPreferencesKey("physics_matches")] ?: false, settingsLoaded = true) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { state.update { it.copy(settingsError = "Couldn't load settings. Please retry.") } }
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

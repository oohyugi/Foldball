package com.yogi.foldball.feature.team.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yogi.foldball.domain.repository.TeamProfileRepository
import com.yogi.foldball.domain.team.ClubBadges
import com.yogi.foldball.domain.team.TeamProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeamViewModel(private val repository: TeamProfileRepository) : ViewModel() {
    private val state = MutableStateFlow(TeamUiState())
    val uiState = state.asStateFlow()
    private val events = Channel<TeamEffect>(Channel.BUFFERED)
    val effects = events.receiveAsFlow()

    init {
        load()
    }

    fun onIntent(intent: TeamIntent) {
        when (intent) {
            TeamIntent.Retry -> if (!state.value.loading && !state.value.saving) load()

            TeamIntent.Save -> save()

            TeamIntent.Cancel -> if (state.value.editing &&
                !state.value.saving
            ) {
                events.trySend(TeamEffect.Cancelled)
            }

            else -> if (!state.value.loading && !state.value.loadFailed &&
                !state.value.saving
            ) {
                state.update { current ->
                    when (intent) {
                        is TeamIntent.NameChanged -> current.copy(
                            name = intent.name.take(TeamProfile.MAX_NAME_LENGTH),
                            error = null
                        )

                        is TeamIntent.ShortNameChanged -> current.copy(
                            shortName = intent.shortName.filter(
                                Char::isLetterOrDigit
                            ).uppercase().take(TeamProfile.MAX_SHORT_NAME_LENGTH),
                            error = null
                        )

                        is TeamIntent.BadgeSelected -> if (intent.id in
                            ClubBadges.ids
                        ) {
                            current.copy(badgeId = intent.id, error = null)
                        } else {
                            current
                        }
                    }
                }
            }
        }
    }

    private fun load() {
        state.update { it.copy(loading = true, loadFailed = false, error = null) }
        viewModelScope.launch {
            try {
                val profile = repository.profile.first()
                state.value = TeamUiState(
                    loading = false,
                    name = profile?.name.orEmpty(),
                    shortName = profile?.shortName.orEmpty(),
                    badgeId = profile?.badgeId ?: ClubBadges.ids.first(),
                    editing = profile != null
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update {
                    it.copy(
                        loading = false,
                        loadFailed = true,
                        error = "Couldn't load your team. Please retry."
                    )
                }
            }
        }
    }

    private fun save() {
        if (!state.value.canSave) return
        val profile = state.value.profile ?: return
        state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                repository.save(profile)
                events.send(TeamEffect.Saved(profile))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update { it.copy(error = "Couldn't save your team. Please try again.") }
            } finally {
                state.update { it.copy(saving = false) }
            }
        }
    }
}

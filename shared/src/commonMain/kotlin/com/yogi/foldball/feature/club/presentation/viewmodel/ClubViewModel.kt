package com.yogi.foldball.feature.club.presentation.viewmodel

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

class ClubViewModel(private val repository: TeamProfileRepository) : ViewModel() {
    private val state = MutableStateFlow(ClubUiState())
    val uiState = state.asStateFlow()
    private val events = Channel<ClubEffect>(Channel.BUFFERED)
    val effects = events.receiveAsFlow()

    init {
        load()
    }

    fun onIntent(intent: ClubIntent) {
        when (intent) {
            ClubIntent.Retry -> if (!state.value.loading && !state.value.saving) load()

            ClubIntent.Save -> save()

            ClubIntent.Cancel -> if (state.value.editing &&
                !state.value.saving
            ) {
                events.trySend(ClubEffect.Cancelled)
            }

            else -> if (!state.value.loading && !state.value.loadFailed &&
                !state.value.saving
            ) {
                state.update { current ->
                    when (intent) {
                        is ClubIntent.ColorCountSelected -> if (intent.count in 1..4) {
                            current.copy(jerseyColorCount = intent.count, error = null)
                        } else {
                            current
                        }

                        is ClubIntent.PatternSelected -> current.copy(jerseyPattern = intent.pattern, error = null)

                        is ClubIntent.NameChanged -> current.copy(
                            name = intent.name.take(TeamProfile.MAX_NAME_LENGTH),
                            error = null
                        )

                        is ClubIntent.ShortNameChanged -> current.copy(
                            shortName = intent.shortName.filter(
                                Char::isLetterOrDigit
                            ).uppercase().take(TeamProfile.MAX_SHORT_NAME_LENGTH),
                            error = null
                        )

                        is ClubIntent.BadgeSelected -> if (intent.id in
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
                state.value = ClubUiState(
                    loading = false,
                    name = profile?.name.orEmpty(),
                    shortName = profile?.shortName.orEmpty(),
                    badgeId = profile?.badgeId ?: ClubBadges.ids.first(),
                    jerseyPattern = profile?.jerseyPattern,
                    jerseyColorCount = profile?.jerseyColorCount ?: 1,
                    editing = profile != null
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update {
                    it.copy(
                        loading = false,
                        loadFailed = true,
                        error = "Couldn't load your club. Please retry."
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
                events.send(ClubEffect.Saved(profile))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update { it.copy(error = "Couldn't save your club. Please try again.") }
            } finally {
                state.update { it.copy(saving = false) }
            }
        }
    }
}

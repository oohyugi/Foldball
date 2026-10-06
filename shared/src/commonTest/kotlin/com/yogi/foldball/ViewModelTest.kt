package com.yogi.foldball

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.yogi.foldball.app.navigation.AppIntent
import com.yogi.foldball.app.navigation.AppRoute
import com.yogi.foldball.app.navigation.AppViewModel
import com.yogi.foldball.domain.repository.TeamProfileRepository
import com.yogi.foldball.domain.team.TeamProfile
import com.yogi.foldball.feature.team.presentation.viewmodel.TeamEffect
import com.yogi.foldball.feature.team.presentation.viewmodel.TeamIntent
import com.yogi.foldball.feature.team.presentation.viewmodel.TeamViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    private val saved = requireNotNull(TeamProfile.create("Paper United", 4, "PUFC"))

    @Test fun existingProfileOpensHomeWithoutWelcomeAndEditorLoadsItsValues() = vmTest {
        val repository = FakeTeamRepository(saved)
        val app = keep(AppViewModel(repository, startAtHome = true))
        val team = keep(TeamViewModel(repository))
        assertEquals(AppRoute.Loading, app.uiState.value.route)
        runCurrent()
        assertEquals(AppRoute.Home, app.uiState.value.route)
        assertEquals(saved, app.uiState.value.profile)
        assertTrue(team.uiState.value.editing)
        assertEquals(saved, team.uiState.value.profile)
    }

    @Test fun newProfileRequiresValidFormAndSaveEffectFollowsPersistence() = vmTest {
        val repository = FakeTeamRepository()
        val app = keep(AppViewModel(repository, true))
        val team = keep(TeamViewModel(repository))
        runCurrent()
        assertEquals(AppRoute.Team, app.uiState.value.route)
        team.onIntent(TeamIntent.Save)
        assertEquals(0, repository.saves)
        team.onIntent(TeamIntent.NameChanged("New Club"))
        team.onIntent(TeamIntent.ShortNameChanged("nc!!"))
        team.onIntent(TeamIntent.BadgeSelected(9))
        val effect = async { team.effects.first() }
        team.onIntent(TeamIntent.Save)
        team.onIntent(TeamIntent.Save)
        team.onIntent(TeamIntent.NameChanged("Changed during save"))
        runCurrent()
        val result = assertIs<TeamEffect.Saved>(effect.await())
        assertEquals("New Club", repository.value.value?.name)
        assertEquals("NC", result.profile.shortName)
        assertEquals(1, repository.saves)
        app.onIntent(AppIntent.TeamSaved(result.profile))
        assertEquals(AppRoute.Home, app.uiState.value.route)
        assertEquals(result.profile, app.uiState.value.profile)
    }

    @Test fun saveFailureKeepsDraftAndAllowsRetryWithoutNavigation() = vmTest {
        val repository = FakeTeamRepository(saved).apply { failSave = true }
        val team = keep(TeamViewModel(repository))
        runCurrent()
        team.onIntent(TeamIntent.NameChanged("Edited Club"))
        team.onIntent(TeamIntent.Save)
        runCurrent()
        assertEquals(saved, repository.value.value)
        assertEquals("Edited Club", team.uiState.value.name)
        assertNotNull(team.uiState.value.error)
        assertFalse(team.uiState.value.saving)
        repository.failSave = false
        val effect = async { team.effects.first() }
        team.onIntent(TeamIntent.Save)
        runCurrent()
        assertEquals("Edited Club", assertIs<TeamEffect.Saved>(effect.await()).profile.name)
    }

    @Test fun loadErrorCannotOverwriteExistingProfileAndRetryRestoresIt() = vmTest {
        val repository = FakeTeamRepository(saved).apply { failRead = true }
        val app = keep(AppViewModel(repository, true))
        val team = keep(TeamViewModel(repository))
        runCurrent()
        assertEquals(AppRoute.Loading, app.uiState.value.route)
        assertNotNull(app.uiState.value.error)
        assertTrue(team.uiState.value.loadFailed)
        team.onIntent(TeamIntent.NameChanged("Overwrite"))
        assertFalse(team.uiState.value.canSave)
        assertEquals(0, repository.saves)
        repository.failRead = false
        app.onIntent(AppIntent.RetryProfile)
        team.onIntent(TeamIntent.Retry)
        runCurrent()
        assertEquals(AppRoute.Home, app.uiState.value.route)
        assertEquals(saved, team.uiState.value.profile)
    }

    @Test fun cancellingEditorPreservesStoredTeam() = vmTest {
        val repository = FakeTeamRepository(saved)
        val team = keep(TeamViewModel(repository))
        runCurrent()
        team.onIntent(TeamIntent.NameChanged("Unsaved"))
        val effect = async { team.effects.first() }
        team.onIntent(TeamIntent.Cancel)
        runCurrent()
        assertEquals(TeamEffect.Cancelled, effect.await())
        assertEquals(saved, repository.value.value)
        assertEquals(0, repository.saves)
    }

    @Test fun navigatingAwayClearsFeatureViewModelsAndPreservesAppProfile() = vmTest {
        val repository = FakeTeamRepository(saved)
        val app = keep(AppViewModel(repository, true))
        runCurrent()
        app.onIntent(AppIntent.EditTeam)
        val screen = ClearedViewModel()
        app.viewModelStore.put("team", screen)
        app.onIntent(AppIntent.Home)
        assertTrue(screen.cleared)
        assertEquals(saved, app.uiState.value.profile)
    }

    @Test fun sandboxEntryStillWorksWithoutCreatingProfile() = vmTest {
        val app = keep(AppViewModel(FakeTeamRepository(), startAtHome = false))
        runCurrent()
        assertEquals(AppRoute.Sandbox, app.uiState.value.route)
        assertNull(app.uiState.value.profile)
        app.onIntent(AppIntent.Home)
        assertEquals(AppRoute.Team, app.uiState.value.route)
    }

    private class ClearedViewModel : ViewModel() {
        var cleared = false
        override fun onCleared() {
            cleared = true
        }
    }

    private val stores = mutableListOf<ViewModelStore>()
    private fun <T : ViewModel> keep(viewModel: T): T {
        stores += ViewModelStore().apply { put("test", viewModel) }
        return viewModel
    }

    private fun vmTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            stores.forEach(ViewModelStore::clear)
            stores.clear()
            Dispatchers.resetMain()
        }
    }
}

private class FakeTeamRepository(initial: TeamProfile? = null) : TeamProfileRepository {
    val value = MutableStateFlow(initial)
    var failRead = false
    var failSave = false
    var saves = 0
    override val profile: Flow<TeamProfile?> get() = if (failRead) {
        flow {
            error("Test read failure")
        }
    } else {
        value
    }
    override suspend fun save(profile: TeamProfile) {
        if (failSave) error("Test save failure")
        saves++
        value.value = profile
    }
}

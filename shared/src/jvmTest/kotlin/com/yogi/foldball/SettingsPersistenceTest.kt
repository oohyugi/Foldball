package com.yogi.foldball

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yogi.foldball.app.navigation.*
import com.yogi.foldball.data.local.createTeamProfileDataStore
import com.yogi.foldball.data.repository.DataStoreTeamProfileRepository
import com.yogi.foldball.domain.game.engine.PhysicsTuning
import java.nio.file.Files
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsPersistenceTest {
    @Test fun tuningAndMatchOptInSurviveReopening() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val directory = Files.createTempDirectory("foldball-settings").toFile()
        val path = directory.resolve("team.preferences_pb").absolutePath
        val tuning = PhysicsTuning(.75f, .6f, .3f, .8f)
        suspend fun session(save: Boolean) {
            val job = SupervisorJob()
            val store = createTeamProfileDataStore(path, CoroutineScope(job + Dispatchers.IO))
            val models = ViewModelStore()
            try {
                val vm = ViewModelProvider.create(models, viewModelFactory {
                    initializer { AppViewModel(DataStoreTeamProfileRepository(store), true, store) }
                })[AppViewModel::class]
                withTimeout(5000) { vm.uiState.first { it.settingsLoaded } }
                if (save) {
                    assertEquals(PhysicsTuning(), vm.uiState.value.tuning)
                    assertFalse(vm.uiState.value.useTuningForMatches)
                    assertFalse(vm.uiState.value.threeActionTurns)
                    vm.onIntent(AppIntent.SaveSettings(tuning, true, true))
                }
                val state = withTimeout(5000) {
                    vm.uiState.first { it.tuning == tuning && it.useTuningForMatches && !it.settingsSaving }
                }
                assertNull(state.settingsError)
                assertTrue(state.threeActionTurns)
            } finally { models.clear(); job.cancelAndJoin() }
        }
        try { session(true); session(false) }
        finally { directory.deleteRecursively(); Dispatchers.resetMain() }
    }
}

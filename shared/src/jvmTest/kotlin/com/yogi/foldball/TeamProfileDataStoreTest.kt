package com.yogi.foldball

import androidx.datastore.preferences.core.edit
import com.yogi.foldball.data.local.TeamProfileKeys
import com.yogi.foldball.data.local.createTeamProfileDataStore
import com.yogi.foldball.data.repository.DataStoreTeamProfileRepository
import com.yogi.foldball.domain.team.TeamProfile
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TeamProfileDataStoreTest {
    @Test fun legacyProfileIsImportedOnceThenEditsSurviveStoreRecreation() = runBlocking {
        val directory = Files.createTempDirectory("foldball-team-test").toFile()
        val path = directory.resolve("team.preferences_pb").absolutePath
        val legacy = requireNotNull(TeamProfile.create("Legacy Club", 7))
        val changed = requireNotNull(TeamProfile.create("Edited Club", 11, "EDIT"))
        var reads = 0
        val job = SupervisorJob()
        val store =
            createTeamProfileDataStore(path, CoroutineScope(job + Dispatchers.IO)) {
                reads++
                legacy
            }
        try {
            val repository = DataStoreTeamProfileRepository(store)
            assertEquals(legacy, repository.profile.first())
            repository.save(changed)
            assertEquals(changed, repository.profile.first())
            assertEquals(1, reads)
        } finally {
            job.cancelAndJoin()
        }
        val reopenedJob = SupervisorJob()
        val reopened =
            createTeamProfileDataStore(path, CoroutineScope(reopenedJob + Dispatchers.IO)) {
                reads++
                legacy
            }
        try {
            assertEquals(changed, DataStoreTeamProfileRepository(reopened).profile.first())
            assertEquals(1, reads)
        } finally {
            reopenedJob.cancelAndJoin()
            directory.deleteRecursively()
        }
    }

    @Test fun existingDataStoreProfileWinsOverLegacyEvenBeforeMigrationMarker() = runBlocking {
        val directory = Files.createTempDirectory("foldball-team-existing-test").toFile()
        val path = directory.resolve("team.preferences_pb").absolutePath
        val existing = requireNotNull(TeamProfile.create("Existing Club", 2, "EXST"))
        val firstJob = SupervisorJob()
        val first =
            createTeamProfileDataStore(path, CoroutineScope(firstJob + Dispatchers.IO)) { null }
        DataStoreTeamProfileRepository(first).save(existing)
        first.edit { it.remove(TeamProfileKeys.legacyImported) }
        firstJob.cancelAndJoin()
        val secondJob = SupervisorJob()
        val second = createTeamProfileDataStore(path, CoroutineScope(secondJob + Dispatchers.IO)) {
            requireNotNull(TeamProfile.create("Outdated Club", 8))
        }
        try {
            assertEquals(existing, DataStoreTeamProfileRepository(second).profile.first())
        } finally {
            secondJob.cancelAndJoin()
            directory.deleteRecursively()
        }
    }

    @Test fun missingLegacyStartsEmptyAndDoesNotReimportLater() = runBlocking {
        val directory = Files.createTempDirectory("foldball-team-empty-test").toFile()
        val path = directory.resolve("team.preferences_pb").absolutePath
        val job = SupervisorJob()
        val store = createTeamProfileDataStore(path, CoroutineScope(job + Dispatchers.IO)) { null }
        assertNull(DataStoreTeamProfileRepository(store).profile.first())
        job.cancelAndJoin()
        val secondJob = SupervisorJob()
        val second = createTeamProfileDataStore(path, CoroutineScope(secondJob + Dispatchers.IO)) {
            error("Migration must not run a second time")
        }
        try {
            assertNull(DataStoreTeamProfileRepository(second).profile.first())
        } finally {
            secondJob.cancelAndJoin()
            directory.deleteRecursively()
        }
    }
}

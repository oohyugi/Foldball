package com.yogi.foldball

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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TeamProfileDataStoreTest {
    @Test fun savedProfileSurvivesStoreRecreation() = runBlocking {
        val directory = Files.createTempDirectory("foldball-team-test").toFile()
        val path = directory.resolve("team.preferences_pb").absolutePath
        val profile = requireNotNull(TeamProfile.create("Saved Club", 11, "SAVE"))
        val job = SupervisorJob()
        val store = createTeamProfileDataStore(path, CoroutineScope(job + Dispatchers.IO))
        try {
            val repository = DataStoreTeamProfileRepository(store)
            assertNull(repository.profile.first())
            repository.save(profile)
            assertEquals(profile, repository.profile.first())
        } finally {
            job.cancelAndJoin()
        }
        val reopenedJob = SupervisorJob()
        val reopened = createTeamProfileDataStore(path, CoroutineScope(reopenedJob + Dispatchers.IO))
        try {
            assertEquals(profile, DataStoreTeamProfileRepository(reopened).profile.first())
        } finally {
            reopenedJob.cancelAndJoin()
            directory.deleteRecursively()
        }
    }
}

package com.yogi.foldball.data

import com.yogi.foldball.data.local.createTeamProfileDataStore
import com.yogi.foldball.domain.team.TeamProfile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun platformDataModule() = module {
    single {
        val directory = NSFileManager.defaultManager.URLForDirectory(
            NSApplicationSupportDirectory,
            NSUserDomainMask,
            null,
            true,
            null
        ) ?: error("Application support directory is unavailable")
        val path = requireNotNull(directory.path) + "/Foldball/team.preferences_pb"
        createTeamProfileDataStore(
            path = path,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            readLegacy = {
                val preferences = NSUserDefaults.standardUserDefaults
                preferences.stringForKey("club_name")?.let { name ->
                    TeamProfile.create(
                        name,
                        preferences.integerForKey("club_logo").toInt(),
                        preferences.stringForKey("club_short_name")
                    )
                }
            }
        )
    }
}

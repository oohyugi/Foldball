package com.yogi.foldball.data

import com.yogi.foldball.data.local.createTeamProfileDataStore
import com.yogi.foldball.domain.team.TeamProfile
import java.io.File
import java.util.prefs.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

fun platformDataModule() = module {
    single {
        val home = System.getProperty("user.home")
        val folder = if (System.getProperty("os.name").startsWith("Mac")) {
            File(home, "Library/Application Support/Foldball")
        } else {
            File(home, ".foldball")
        }
        createTeamProfileDataStore(
            path = folder.resolve("team.preferences_pb").absolutePath,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            readLegacy = {
                val preferences = Preferences.userRoot().node("com/yogi/foldball/club")
                preferences.get("name", null)?.let { name ->
                    TeamProfile.create(
                        name,
                        preferences.getInt("logo", 0),
                        preferences.get("short_name", null)
                    )
                }
            }
        )
    }
}

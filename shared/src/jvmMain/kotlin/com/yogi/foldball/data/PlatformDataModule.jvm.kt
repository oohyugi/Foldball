package com.yogi.foldball.data

import com.yogi.foldball.data.local.createTeamProfileDataStore
import java.io.File
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
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
    }
}

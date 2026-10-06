package com.yogi.foldball.data

import android.content.Context
import com.yogi.foldball.data.local.createTeamProfileDataStore
import com.yogi.foldball.domain.team.TeamProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

fun platformDataModule(context: Context) = module {
    single {
        val appContext = context.applicationContext
        createTeamProfileDataStore(
            path = appContext.filesDir.resolve("datastore/team.preferences_pb").absolutePath,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            readLegacy = {
                val preferences = appContext.getSharedPreferences(
                    "club_profile",
                    Context.MODE_PRIVATE
                )
                preferences.getString("name", null)?.let { name ->
                    TeamProfile.create(
                        name,
                        preferences.getInt("logo", 0),
                        preferences.getString("short_name", null)
                    )
                }
            }
        )
    }
}

package com.yogi.foldball.data.local

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yogi.foldball.domain.team.TeamProfile
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toPath

internal object TeamProfileKeys {
    val name = stringPreferencesKey("name")
    val shortName = stringPreferencesKey("short_name")
    val badge = intPreferencesKey("badge")
    val legacyImported = booleanPreferencesKey("legacy_imported")
}

/** The DI graph creates one store per application, never one per screen. */
fun createTeamProfileDataStore(
    path: String,
    scope: CoroutineScope,
    readLegacy: () -> TeamProfile?
): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    scope = scope,
    migrations = listOf(LegacyTeamProfileMigration(readLegacy)),
    produceFile = { path.toPath() }
)

internal class LegacyTeamProfileMigration(private val readLegacy: () -> TeamProfile?) : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences) = currentData[TeamProfileKeys.legacyImported] != true

    override suspend fun migrate(currentData: Preferences): Preferences {
        val result = currentData.toMutablePreferences()
        // An existing DataStore profile wins. Leave legacy preferences intact for rollback.
        if (currentData[TeamProfileKeys.name] == null) {
            readLegacy()?.let { profile ->
                result[TeamProfileKeys.name] = profile.name
                result[TeamProfileKeys.shortName] = profile.shortName
                result[TeamProfileKeys.badge] = profile.badgeId
            }
        }
        result[TeamProfileKeys.legacyImported] = true
        return result
    }

    override suspend fun cleanUp() = Unit
}

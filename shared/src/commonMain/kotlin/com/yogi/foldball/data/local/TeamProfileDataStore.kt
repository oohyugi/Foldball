package com.yogi.foldball.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toPath

internal object TeamProfileKeys {
    val name = stringPreferencesKey("name")
    val shortName = stringPreferencesKey("short_name")
    val jerseyColorCount = intPreferencesKey("jersey_color_count")
    val jerseyPattern = stringPreferencesKey("jersey_pattern")
    val badge = intPreferencesKey("badge")
}

/** The DI graph creates one store per application, never one per screen. */
fun createTeamProfileDataStore(
    path: String,
    scope: CoroutineScope
): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    scope = scope,
    produceFile = { path.toPath() }
)

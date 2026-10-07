package com.yogi.foldball.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.yogi.foldball.data.local.TeamProfileKeys
import com.yogi.foldball.domain.repository.TeamProfileRepository
import com.yogi.foldball.domain.team.JerseyPattern
import com.yogi.foldball.domain.team.TeamProfile
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DataStoreTeamProfileRepository(private val store: DataStore<Preferences>) : TeamProfileRepository {
    override val profile = store.data.map { preferences ->
        val name = preferences[TeamProfileKeys.name] ?: return@map null
        val badge = preferences[TeamProfileKeys.badge] ?: return@map null
        val pattern = JerseyPattern.entries.firstOrNull { it.name == preferences[TeamProfileKeys.jerseyPattern] }
        val count = preferences[TeamProfileKeys.jerseyColorCount]?.takeIf { it in 1..4 }
            ?: if (pattern == JerseyPattern.TWO_TONE) 2 else 1
        TeamProfile.create(name, badge, preferences[TeamProfileKeys.shortName], pattern, count)
    }.distinctUntilChanged()

    override suspend fun save(profile: TeamProfile) {
        store.edit { preferences ->
            preferences[TeamProfileKeys.name] = profile.name
            preferences[TeamProfileKeys.shortName] = profile.shortName
            preferences[TeamProfileKeys.badge] = profile.badgeId
            preferences[TeamProfileKeys.jerseyColorCount] = profile.jerseyColorCount
            if (profile.jerseyPattern == null) {
                preferences.remove(TeamProfileKeys.jerseyPattern)
            } else {
                preferences[TeamProfileKeys.jerseyPattern] = profile.jerseyPattern.name
            }
        }
    }
}

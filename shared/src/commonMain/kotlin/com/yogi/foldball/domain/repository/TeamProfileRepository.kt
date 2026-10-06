package com.yogi.foldball.domain.repository

import com.yogi.foldball.domain.team.TeamProfile
import kotlinx.coroutines.flow.Flow

interface TeamProfileRepository {
    val profile: Flow<TeamProfile?>
    suspend fun save(profile: TeamProfile)
}

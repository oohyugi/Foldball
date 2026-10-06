package com.yogi.foldball.data

import com.yogi.foldball.data.repository.DataStoreTeamProfileRepository
import com.yogi.foldball.domain.repository.TeamProfileRepository
import org.koin.dsl.module

val dataModule = module {
    single<TeamProfileRepository> { DataStoreTeamProfileRepository(get()) }
}

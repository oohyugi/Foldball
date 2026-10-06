package com.yogi.foldball.app.di

import com.yogi.foldball.app.navigation.AppViewModel
import com.yogi.foldball.data.dataModule
import com.yogi.foldball.feature.match.matchModule
import com.yogi.foldball.feature.team.teamModule
import com.yogi.foldball.feature.welcome.welcomeModule
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    includes(dataModule, teamModule, welcomeModule, matchModule)
    viewModel { parameters -> AppViewModel(get(), parameters.get()) }
}

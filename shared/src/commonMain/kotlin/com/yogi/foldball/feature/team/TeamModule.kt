package com.yogi.foldball.feature.team

import com.yogi.foldball.feature.team.presentation.viewmodel.TeamViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val teamModule = module {
    viewModel { TeamViewModel(get()) }
}

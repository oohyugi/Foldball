package com.yogi.foldball.feature.match

import com.yogi.foldball.feature.match.presentation.viewmodel.MatchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val matchModule = module {
    viewModel { parameters -> MatchViewModel(parameters.get()) }
}

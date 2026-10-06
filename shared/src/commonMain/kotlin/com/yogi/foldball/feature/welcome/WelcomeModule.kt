package com.yogi.foldball.feature.welcome

import com.yogi.foldball.feature.welcome.presentation.viewmodel.WelcomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val welcomeModule = module {
    viewModel { WelcomeViewModel(get()) }
}

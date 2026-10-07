package com.yogi.foldball.feature.club

import com.yogi.foldball.feature.club.presentation.viewmodel.ClubViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val clubModule = module {
    viewModel { ClubViewModel(get()) }
}

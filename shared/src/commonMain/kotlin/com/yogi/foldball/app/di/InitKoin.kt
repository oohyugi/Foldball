package com.yogi.foldball.app.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.mp.KoinPlatformTools

/** Called by platform entry points before composing; Activity recreation reuses the graph. */
fun initKoin(platformModule: Module) {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin { modules(appModule, platformModule) }
    }
}

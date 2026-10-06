package com.yogi.foldball

import android.app.Application
import com.yogi.foldball.app.di.initKoin
import com.yogi.foldball.data.platformDataModule

class FoldballApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(platformDataModule(this))
    }
}

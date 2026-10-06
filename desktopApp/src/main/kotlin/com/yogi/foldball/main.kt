package com.yogi.foldball

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.yogi.foldball.app.di.initKoin
import com.yogi.foldball.data.platformDataModule

fun main(args: Array<String>) {
    initKoin(platformDataModule())
    application {
        Window(onCloseRequest = ::exitApplication, title = "Foldball") {
            if ("--sandbox" in args) SandboxMain() else Main()
        }
    }
}

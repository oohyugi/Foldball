package com.yogi.foldball

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main(args: Array<String>) = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Foldball",
    ) {
        if ("--sandbox" in args) SandboxMain() else Main()
    }
}

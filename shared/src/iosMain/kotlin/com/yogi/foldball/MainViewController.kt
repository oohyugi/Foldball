@file:Suppress("ktlint:standard:function-naming")

package com.yogi.foldball

import androidx.compose.ui.window.ComposeUIViewController
import com.yogi.foldball.app.di.initKoin
import com.yogi.foldball.data.platformDataModule

fun MainViewController(): platform.UIKit.UIViewController {
    initKoin(platformDataModule())
    return ComposeUIViewController { Main() }
}

fun SandboxViewController(): platform.UIKit.UIViewController {
    initKoin(platformDataModule())
    return ComposeUIViewController { SandboxMain() }
}

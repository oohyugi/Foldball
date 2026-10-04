package com.yogi.foldball

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
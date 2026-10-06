package com.yogi.foldball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yogi.foldball.core.ui.theme.FoldballTheme
import com.yogi.foldball.feature.home.presentation.screen.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            Main()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    FoldballTheme {
        HomeScreen(onMatchClick = {}, onLocalMatchClick = {}, onSandboxClick = {})
    }
}

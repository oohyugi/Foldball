package com.yogi.foldball
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yogi.foldball.feature.playerlab.presentation.screen.DentComparisonScreen
class DentPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DentComparisonScreen() }
    }
}

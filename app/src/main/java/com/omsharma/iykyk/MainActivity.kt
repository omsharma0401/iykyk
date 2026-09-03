package com.omsharma.iykyk

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.omsharma.iykyk.navigation.AppNavigation
import com.omsharma.iykyk.ui.theme.IykykTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val debugVideoUri = if (savedInstanceState == null) extractDebugVideoUri(intent) else null
        setContent {
            IykykTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        debugVideoUri = debugVideoUri,
                        contentModifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun extractDebugVideoUri(intent: Intent): Uri? =
        if (BuildConfig.DEBUG && intent.action == DEBUG_PROCESS_VIDEO_ACTION) intent.data else null

    companion object {
        private const val DEBUG_PROCESS_VIDEO_ACTION = "com.omsharma.iykyk.DEBUG_PROCESS_VIDEO"
    }
}

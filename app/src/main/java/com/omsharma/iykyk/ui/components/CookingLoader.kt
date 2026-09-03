package com.omsharma.iykyk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.omsharma.iykyk.ui.theme.AppDimensions

// Loading indicator, quirky text wavy progress and a Cancel button
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CookingLoader(
    stage: String?,
    progress: Float?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(AppDimensions.screenInset),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LoadingIndicator()
        Text(
            text = stage ?: "Preheating…",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = AppDimensions.xLarge)
        )
        if (progress != null) {
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.xLarge, start = AppDimensions.screenInset, end = AppDimensions.screenInset)
            )
        }
        IykykButton(text = "Cancel", onClick = onCancel, modifier = Modifier.padding(top = AppDimensions.captureControlsBottomInset), filled = false)
    }
}

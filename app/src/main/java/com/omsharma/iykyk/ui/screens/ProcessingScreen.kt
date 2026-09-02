package com.omsharma.iykyk.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omsharma.iykyk.state.UiState
import com.omsharma.iykyk.vm.FaceCollageViewModel

// Purely a passive observer - FaceCollageViewModel.onRecordingFinished() already
// starts processing before CaptureScreen navigates here, so there's nothing for this
// screen to trigger itself.
@Composable
fun ProcessingScreen(
    viewModel: FaceCollageViewModel,
    onCollageReady: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val processingState by viewModel.processingState.collectAsStateWithLifecycle()

    LaunchedEffect(processingState) {
        if (processingState is UiState.Success) {
            onCollageReady()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val state = processingState) {
            is UiState.Failed -> ProcessingFailed(
                message = state.message,
                onRetry = {
                    viewModel.resetProcessingState()
                    onRetry()
                }
            )

            is UiState.Loading -> ProcessingInProgress(stage = state.stage)

            is UiState.Idle, is UiState.Success -> ProcessingInProgress(stage = null)
        }
    }
}

@Composable
private fun ProcessingInProgress(stage: String?, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        CircularProgressIndicator()
        if (stage != null) {
            Text(
                text = stage,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ProcessingFailed(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(24.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text("Try Again")
        }
    }
}

package com.omsharma.iykyk.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omsharma.iykyk.state.UiState
import com.omsharma.iykyk.ui.components.CollageContent
import com.omsharma.iykyk.ui.components.CookingLoader
import com.omsharma.iykyk.ui.components.ProcessingError
import com.omsharma.iykyk.vm.CollageViewModel

@Composable
fun CollageScreen(
    videoUri: Uri,
    onNewVideo: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollageViewModel = hiltViewModel()
) {
    val processingState by viewModel.processingState.collectAsStateWithLifecycle()
    val collage by viewModel.collage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(videoUri) { viewModel.startProcessing(videoUri) }
    LaunchedEffect(Unit) {
        viewModel.exportMessage.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    LaunchedEffect(Unit) {
        viewModel.shareIntent.collect { context.startActivity(Intent.createChooser(it, "Share collage")) }
    }

    when (val state = processingState) {
        is UiState.Success -> CollageContent(
            collage = collage,
            onSave = viewModel::saveCollage,
            onShare = viewModel::shareCollage,
            onNewVideo = onNewVideo,
            modifier = modifier
        )

        is UiState.Failed -> ProcessingError(message = state.message, onRetry = onNewVideo, modifier = modifier)

        is UiState.Loading -> CookingLoader(stage = state.stage, progress = state.progress, onCancel = onNewVideo, modifier = modifier)

        UiState.Idle -> CookingLoader(stage = null, progress = null, onCancel = onNewVideo, modifier = modifier)
    }
}

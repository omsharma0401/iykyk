package com.omsharma.iykyk.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    // Android 8 and 9 need the storage permission to write to the gallery; newer versions do not
    val storagePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.saveCollage() else Toast.makeText(context, "Storage permission is needed to save", Toast.LENGTH_SHORT).show()
    }
    fun save() {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else viewModel.saveCollage()
    }
    LaunchedEffect(Unit) {
        viewModel.exportMessage.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    LaunchedEffect(Unit) {
        viewModel.shareIntent.collect { context.startActivity(Intent.createChooser(it, "Share collage")) }
    }

    when (val state = processingState) {
        is UiState.Success -> CollageContent(
            collage = collage,
            onSave = ::save,
            onShare = viewModel::shareCollage,
            onNewVideo = onNewVideo,
            modifier = modifier
        )

        is UiState.Failed -> ProcessingError(message = state.message, onRetry = onNewVideo, modifier = modifier)

        is UiState.Loading -> CookingLoader(stage = state.stage, progress = state.progress, onCancel = onNewVideo, modifier = modifier)

        UiState.Idle -> CookingLoader(stage = null, progress = null, onCancel = onNewVideo, modifier = modifier)
    }
}

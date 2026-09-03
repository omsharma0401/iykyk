package com.omsharma.iykyk.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omsharma.iykyk.state.UiState
import com.omsharma.iykyk.ui.components.CaptureIconButton
import com.omsharma.iykyk.ui.components.PermissionNotice
import com.omsharma.iykyk.ui.components.ShutterButton
import com.omsharma.iykyk.ui.theme.AppDimensions
import com.omsharma.iykyk.vm.CaptureViewModel

@Composable
fun CaptureScreen(
    onVideoReady: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val hasCameraPermission by viewModel.hasCameraPermission.collectAsStateWithLifecycle()
    val cameraPermissionBlocked by viewModel.cameraPermissionBlocked.collectAsStateWithLifecycle()
    val captureState by viewModel.captureState.collectAsStateWithLifecycle()
    val recordingProgress by viewModel.recordingProgress.collectAsStateWithLifecycle()
    val lensFacing by viewModel.lensFacing.collectAsStateWithLifecycle()
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    val isRecording = captureState is UiState.Loading
    val secondsRemaining = (captureState as? UiState.Loading)?.stage

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    // Re-check on every resume, so turning the permission on in Settings takes effect on return
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onPermissionChecked(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(captureState) {
        when (val state = captureState) {
            is UiState.Success -> {
                onVideoReady(state.data)
                viewModel.resetCaptureState()
            }

            is UiState.Failed -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetCaptureState()
            }

            else -> Unit
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // No rationale after a denial means the system has stopped asking
        val activity = context.findActivity()
        val canAskAgain = granted || activity == null ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.CAMERA
                )
        viewModel.onPermissionResult(granted, canAskAgain)
    }

    fun openAppSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.onVideoPicked(uri) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Wide phones keep roomy controls; narrow phones preserve space for the shutter button.
        val controlsInset = (maxWidth * 0.08f).coerceIn(
            AppDimensions.large,
            AppDimensions.captureControlsInset
        )
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> PreviewView(ctx).also { previewView = it } }
            )
            LaunchedEffect(previewView, lensFacing) {
                previewView?.let { viewModel.bindPreview(it, lifecycleOwner) }
            }
            DisposableEffect(Unit) {
                onDispose { viewModel.unbindPreview() }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(bottom = AppDimensions.captureControlsBottomInset),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cameraPermissionBlocked) {
                PermissionNotice(
                    message = "Camera access is turned off. Turn it on to record; picking a video still works.",
                    onOpenSettings = ::openAppSettings,
                    modifier = Modifier.padding(bottom = AppDimensions.large)
                )
            }

            if (isRecording) {
                Text(
                    text = "${secondsRemaining}s",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = AppDimensions.large)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = controlsInset),
                contentAlignment = Alignment.Center
            ) {
                CaptureIconButton(
                    icon = Icons.Default.PhotoLibrary,
                    contentDescription = "Select video from gallery",
                    modifier = Modifier.align(Alignment.CenterStart),
                    onClick = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                )

                CaptureIconButton(
                    icon = Icons.Default.Cameraswitch,
                    contentDescription = "Switch between front and back camera",
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enabled = hasCameraPermission,
                    onClick = viewModel::toggleCamera
                )

                ShutterButton(
                    hasPermission = hasCameraPermission,
                    isRecording = isRecording,
                    progress = recordingProgress,
                    onClick = {
                        when {
                            hasCameraPermission -> viewModel.onShutterTapped()
                            cameraPermissionBlocked -> openAppSettings()
                            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

package com.omsharma.iykyk.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omsharma.iykyk.ui.components.capture.VideoRecorder
import com.omsharma.iykyk.vm.FaceCollageViewModel
import kotlinx.coroutines.delay

private const val RECORDING_DURATION_SECONDS = 20

@Composable
fun CaptureScreen(
    viewModel: FaceCollageViewModel,
    onRecordingFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasCameraPermission) {
        PermissionRationale(modifier = modifier) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        return
    }

    val videoRecorder = remember { VideoRecorder(context, lifecycleOwner) }
    DisposableEffect(Unit) {
        onDispose { videoRecorder.unbind() }
    }

    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()

    LaunchedEffect(isRecording) {
        if (isRecording) {
            for (second in 1..RECORDING_DURATION_SECONDS) {
                delay(1000)
                viewModel.onRecordingTick(second)
                if (second >= RECORDING_DURATION_SECONDS) {
                    videoRecorder.stopRecording()
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { previewView -> videoRecorder.bindTo(previewView) }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (isRecording) {
                RecordingIndicator(
                    elapsedSeconds = elapsedSeconds,
                    totalSeconds = RECORDING_DURATION_SECONDS,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRecording) {
                    TextButton(onClick = {
                        videoRecorder.cancelRecording()
                        viewModel.onRecordingCancelled()
                    }) {
                        Text("Cancel")
                    }
                } else {
                    Button(onClick = {
                        viewModel.onRecordingStarted()
                        videoRecorder.startRecording(
                            onFinished = { uri ->
                                viewModel.onRecordingFinished(uri)
                                onRecordingFinished()
                            },
                            onError = {
                                viewModel.onRecordingCancelled()
                                // TODO surface this to the user once ProcessingScreen's
                                // error UI (and a shared way to show it) is designed.
                            }
                        )
                    }) {
                        Text("Start Recording")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRationale(modifier: Modifier = Modifier, onRequestPermission: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera access is needed to record the video.",
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 16.dp)) {
            Text("Grant permission")
        }
    }
}

@Composable
private fun RecordingIndicator(
    elapsedSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Red, CircleShape)
        )
        Text(
            text = "${totalSeconds - elapsedSeconds}s",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

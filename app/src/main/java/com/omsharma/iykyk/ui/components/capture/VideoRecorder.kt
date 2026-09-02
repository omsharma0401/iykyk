package com.omsharma.iykyk.ui.components.capture

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

// Thin wrapper around CameraX's Preview + VideoCapture use cases. Binding is tied to
// the LifecycleOwner passed in, but Compose Navigation doesn't destroy the Activity
// between screens, so callers must explicitly call unbind() (e.g. from a
// DisposableEffect) when the capture UI leaves composition - otherwise the camera
// stays bound and active after navigating away.
class VideoRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var isCancelled = false

    fun bindTo(previewView: PreviewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            val capture = VideoCapture.withOutput(recorder)

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture
            )
            videoCapture = capture
        }, ContextCompat.getMainExecutor(context))
    }

    // No audio - the deliverable is a still collage, so there's nothing to justify
    // requesting RECORD_AUDIO for.
    fun startRecording(onFinished: (Uri) -> Unit, onError: (String) -> Unit) {
        val capture = videoCapture ?: run {
            onError("Camera not ready yet")
            return
        }
        isCancelled = false

        val outputFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        activeRecording = capture.output
            .prepareRecording(context, outputOptions)
            .start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    when {
                        isCancelled -> outputFile.delete()
                        event.hasError() -> {
                            outputFile.delete()
                            onError(event.cause?.message ?: "Recording failed")
                        }
                        else -> onFinished(Uri.fromFile(outputFile))
                    }
                }
            }
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    fun cancelRecording() {
        isCancelled = true
        activeRecording?.stop()
        activeRecording = null
    }

    fun unbind() {
        cameraProvider?.unbindAll()
    }
}

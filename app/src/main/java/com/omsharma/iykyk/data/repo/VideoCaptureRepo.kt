package com.omsharma.iykyk.data.repo

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
import com.omsharma.iykyk.state.UiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// CameraX preview + video capture. Callers unbind() when the capture UI leaves composition.
class VideoCaptureRepo @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var activeRecording: Recording? = null
    private var bound = false

    // Created once and reused across binds so a persistent recording survives a camera switch
    private val recorder: Recorder by lazy {
        Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
    }
    private val videoCapture: VideoCapture<Recorder> by lazy { VideoCapture.withOutput(recorder) }
    private val preview: Preview by lazy { Preview.Builder().build() }

    // Bind to the requested lens; falls back to the back camera
    fun bind(previewView: PreviewView, lifecycleOwner: LifecycleOwner, lensFacing: Int) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            cameraProvider = provider
            preview.surfaceProvider = previewView.surfaceProvider

            val requested = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            val selector = if (provider.hasCamera(requested)) requested else CameraSelector.DEFAULT_BACK_CAMERA

            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, videoCapture)
            bound = true
        }, ContextCompat.getMainExecutor(context))
    }

    fun unbind() {
        cameraProvider?.unbindAll()
        bound = false
    }

    // Record until stopped or the 20 s limit. Loading carries seconds left + elapsed fraction.
    fun startRecording(): Flow<UiState<Uri>> = callbackFlow {
        if (!bound) {
            trySend(UiState.Failed("Camera not ready yet"))
            close()
            return@callbackFlow
        }
        trySend(UiState.Loading(stage = "$TOTAL_RECORDING_SECONDS", progress = 0f))

        // Private files dir, not cacheDir: a full phone purges the cache within seconds
        val outputFile = withContext(Dispatchers.IO) {
            val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
            recordingsDir.listFiles()?.forEach { it.delete() }
            File(recordingsDir, "capture_${System.currentTimeMillis()}.mp4")
        }

        activeRecording = recorder
            .prepareRecording(context, FileOutputOptions.Builder(outputFile).build())
            .asPersistentRecording() // survives the selfie toggle
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Status -> {
                        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(event.recordingStats.recordedDurationNanos)
                        val remainingSeconds = ((RECORDING_DURATION_MS - elapsedMs) / 1000L).coerceAtLeast(0)
                        trySend(
                            UiState.Loading(
                                stage = "$remainingSeconds",
                                progress = (elapsedMs.toFloat() / RECORDING_DURATION_MS).coerceIn(0f, 1f)
                            )
                        )
                        if (elapsedMs >= RECORDING_DURATION_MS) activeRecording?.stop()
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            trySend(UiState.Failed(event.cause?.message ?: "Recording failed"))
                        } else {
                            trySend(UiState.Success(Uri.fromFile(outputFile)))
                        }
                        activeRecording = null
                        close()
                    }

                    else -> Unit
                }
            }

        awaitClose { }
    }

    // Stop early and keep what was recorded
    fun stopRecording() {
        activeRecording?.stop()
    }

    companion object {
        const val RECORDING_DURATION_MS = 20_000L
        val TOTAL_RECORDING_SECONDS = (RECORDING_DURATION_MS / 1000L).toInt()
    }
}

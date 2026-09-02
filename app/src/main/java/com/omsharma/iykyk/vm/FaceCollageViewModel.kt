package com.omsharma.iykyk.vm

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omsharma.iykyk.data.model.CollageResult
import com.omsharma.iykyk.data.repo.CollageStorageRepository
import com.omsharma.iykyk.data.repo.VideoProcessingRepository
import com.omsharma.iykyk.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Shared across CaptureScreen, ProcessingScreen and CollageScreen (obtained once in
// AppNavigation and passed down as a parameter) rather than one independently-scoped
// ViewModel per screen - CollageScreen needs to display the exact Bitmap
// ProcessingScreen produced, and a Bitmap can't reasonably cross a Navigation Compose
// route as an argument the way the recorded video's Uri can between Capture and
// Processing.
@HiltViewModel
class FaceCollageViewModel @Inject constructor(
    private val videoProcessingRepository: VideoProcessingRepository,
    private val collageStorageRepository: CollageStorageRepository
) : ViewModel() {

    // --- Capture ---

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds = _elapsedSeconds.asStateFlow()

    fun onRecordingStarted() {
        _isRecording.value = true
        _elapsedSeconds.value = 0
    }

    fun onRecordingTick(seconds: Int) {
        _elapsedSeconds.value = seconds
    }

    fun onRecordingCancelled() {
        _isRecording.value = false
        _elapsedSeconds.value = 0
    }

    fun onRecordingFinished(videoUri: Uri) {
        _isRecording.value = false
        startProcessing(videoUri)
    }

    // --- Processing ---

    private val _processingState = MutableStateFlow<UiState<CollageResult>>(UiState.Idle)
    val processingState = _processingState.asStateFlow()

    private var processingJob: Job? = null

    private fun startProcessing(videoUri: Uri) {
        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            videoProcessingRepository.processVideo(videoUri).collect { state ->
                _processingState.value = state
            }
        }
    }

    // Called when navigating back to Capture, either after a processing failure or
    // after "Record again" from Collage, so re-entering Capture starts clean.
    fun resetProcessingState() {
        _processingState.value = UiState.Idle
    }

    // --- Collage ---

    private val _saveState = MutableStateFlow<UiState<Uri>>(UiState.Idle)
    val saveState = _saveState.asStateFlow()

    fun saveToGallery(bitmap: Bitmap) {
        viewModelScope.launch {
            _saveState.value = UiState.Loading()
            _saveState.value = try {
                UiState.Success(collageStorageRepository.saveToGallery(bitmap))
            } catch (e: Exception) {
                UiState.Failed(e.message ?: "Failed to save the collage")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = UiState.Idle
    }

    // Not UiState-tracked - handing an Intent to the system share sheet isn't
    // meaningfully a "success/failure" the app needs to keep showing afterward.
    suspend fun createShareIntent(bitmap: Bitmap): Intent =
        collageStorageRepository.createShareIntent(bitmap)
}

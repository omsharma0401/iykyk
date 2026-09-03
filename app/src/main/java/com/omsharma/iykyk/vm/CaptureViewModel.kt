package com.omsharma.iykyk.vm

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omsharma.iykyk.data.repo.VideoCaptureRepo
import com.omsharma.iykyk.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val videoCaptureRepo: VideoCaptureRepo
) : ViewModel() {

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission = _hasCameraPermission.asStateFlow()

    private val _captureState = MutableStateFlow<UiState<Uri>>(UiState.Idle)
    val captureState = _captureState.asStateFlow()

    private val _recordingProgress = MutableStateFlow(0f)
    val recordingProgress = _recordingProgress.asStateFlow()

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing = _lensFacing.asStateFlow()

    // Camera

    fun bindPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        videoCaptureRepo.bind(previewView, lifecycleOwner, _lensFacing.value)
    }

    fun unbindPreview() {
        videoCaptureRepo.unbind()
    }

    fun onPermissionResult(granted: Boolean) {
        _hasCameraPermission.value = granted
    }

    // Selfie toggle; the screen rebinds, the recording continues
    fun toggleCamera() {
        _lensFacing.value = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    // Recording

    // Tap to start, tap again to stop early
    fun onShutterTapped() {
        if (_captureState.value is UiState.Loading) {
            videoCaptureRepo.stopRecording()
            return
        }
        viewModelScope.launch {
            videoCaptureRepo.startRecording().collect { state ->
                _captureState.value = state
                _recordingProgress.value = (state as? UiState.Loading)?.progress ?: 0f
            }
        }
    }

    fun onVideoPicked(uri: Uri?) {
        if (uri != null) _captureState.value = UiState.Success(uri)
    }

    fun resetCaptureState() {
        _captureState.value = UiState.Idle
        _recordingProgress.value = 0f
    }
}

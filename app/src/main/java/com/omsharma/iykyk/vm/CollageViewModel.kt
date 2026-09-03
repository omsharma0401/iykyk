package com.omsharma.iykyk.vm

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omsharma.iykyk.data.model.PersonResult
import com.omsharma.iykyk.data.repo.CollageExportRepo
import com.omsharma.iykyk.data.repo.VideoProcessingRepo
import com.omsharma.iykyk.state.UiState
import com.omsharma.iykyk.utils.collage.CollageRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CollageViewModel @Inject constructor(
    private val videoProcessingRepo: VideoProcessingRepo,
    private val collageExportRepo: CollageExportRepo
) : ViewModel() {

    private val _processingState = MutableStateFlow<UiState<List<PersonResult>>>(UiState.Idle)
    val processingState = _processingState.asStateFlow()

    private val _collage = MutableStateFlow<Bitmap?>(null)
    val collage = _collage.asStateFlow()

    private val _exportMessage = Channel<String>(Channel.BUFFERED)
    val exportMessage = _exportMessage.receiveAsFlow()

    private val _shareIntent = Channel<Intent>(Channel.BUFFERED)
    val shareIntent = _shareIntent.receiveAsFlow()

    private var startedFor: Uri? = null

    // Processing

    fun startProcessing(videoUri: Uri) {
        if (videoUri == startedFor) return
        startedFor = videoUri
        viewModelScope.launch {
            videoProcessingRepo.processVideo(videoUri).collect { state ->
                if (state is UiState.Success) {
                    _collage.value = withContext(Dispatchers.Default) { CollageRenderer.render(state.data) }
                }
                _processingState.value = state
            }
        }
    }

    // Export

    fun saveCollage() {
        val bitmap = _collage.value ?: return
        viewModelScope.launch {
            val savedUri = collageExportRepo.saveToGallery(bitmap)
            _exportMessage.send(if (savedUri != null) "Saved to gallery" else "Couldn't save the collage")
        }
    }

    fun shareCollage() {
        val bitmap = _collage.value ?: return
        viewModelScope.launch {
            _shareIntent.send(collageExportRepo.buildShareIntent(bitmap))
        }
    }
}

package com.omsharma.iykyk.data.repo

import android.graphics.Bitmap
import android.net.Uri
import com.omsharma.iykyk.data.model.CollageResult
import com.omsharma.iykyk.data.model.DetectedFace
import com.omsharma.iykyk.data.model.EmbeddedFace
import com.omsharma.iykyk.processing.CollageBuilder
import com.omsharma.iykyk.processing.FaceClusterer
import com.omsharma.iykyk.processing.FaceDetector
import com.omsharma.iykyk.processing.FaceEmbedder
import com.omsharma.iykyk.processing.FrameExtractor
import com.omsharma.iykyk.state.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class VideoProcessingRepository @Inject constructor(
    private val frameExtractor: FrameExtractor,
    private val faceDetector: FaceDetector,
    private val faceEmbedder: FaceEmbedder,
    private val faceClusterer: FaceClusterer,
    private val collageBuilder: CollageBuilder
) {

    fun processVideo(videoUri: Uri): Flow<UiState<CollageResult>> = flow {
        try {
            emit(UiState.Loading("Analyzing video..."))

            // Extraction, detection and embedding are interleaved per-frame (not three
            // separate passes) so only one full-resolution frame is ever in memory at a time.
            val embeddedFaces = mutableListOf<EmbeddedFace>()
            frameExtractor.extractFrames(videoUri).collect { frame ->
                for (face in faceDetector.detect(frame)) {
                    embeddedFaces.add(
                        EmbeddedFace(
                            embedding = faceEmbedder.embed(frame, face),
                            displayCrop = cropDisplayFace(frame, face),
                            boundingBoxArea = face.boundingBox.width() * face.boundingBox.height(),
                            headEulerAngleY = face.headEulerAngleY
                        )
                    )
                }
            }

            if (embeddedFaces.isEmpty()) {
                emit(UiState.Failed("No faces detected in the recording"))
                return@flow
            }

            emit(UiState.Loading("Identifying people..."))
            val clusters = faceClusterer.cluster(embeddedFaces)

            emit(UiState.Loading("Building collage..."))
            val collage = collageBuilder.build(clusters.map { it.representative.displayCrop })

            emit(UiState.Success(CollageResult(collageBitmap = collage, peopleCount = clusters.size)))
        } catch (e: OutOfMemoryError) {
            emit(UiState.Failed("Ran out of memory processing the video - try a shorter recording or fewer people in frame"))
        } catch (e: Exception) {
            emit(UiState.Failed(e.message ?: "An unexpected error occurred while processing the video"))
        }
    }.flowOn(Dispatchers.Default)

    // A softer, padded crop for display in the collage - distinct from FaceEmbedder's
    // tightly-aligned 112x112 crop, which is optimized for the model, not for looking at.
    private fun cropDisplayFace(frame: Bitmap, face: DetectedFace): Bitmap {
        val box = face.boundingBox
        val paddingX = (box.width() * DISPLAY_CROP_PADDING).toInt()
        val paddingY = (box.height() * DISPLAY_CROP_PADDING).toInt()

        val left = (box.left - paddingX).coerceIn(0, frame.width)
        val top = (box.top - paddingY).coerceIn(0, frame.height)
        val right = (box.right + paddingX).coerceIn(0, frame.width)
        val bottom = (box.bottom + paddingY).coerceIn(0, frame.height)

        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)

        return Bitmap.createBitmap(frame, left, top, width, height)
    }

    companion object {
        private const val DISPLAY_CROP_PADDING = 0.3f
    }
}

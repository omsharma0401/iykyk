package com.omsharma.iykyk.data.repo

import android.content.Context
import android.net.Uri
import android.util.Log
import com.omsharma.iykyk.data.model.ExtractedFrame
import com.omsharma.iykyk.data.model.FaceDetection
import com.omsharma.iykyk.data.model.PersonResult
import com.omsharma.iykyk.data.model.enums.ProcessingStage
import com.omsharma.iykyk.ml.FaceDetector
import com.omsharma.iykyk.processing.FaceObserver
import com.omsharma.iykyk.processing.FaceTracker
import com.omsharma.iykyk.processing.FrameExtractor
import com.omsharma.iykyk.processing.IdentityClusterer
import com.omsharma.iykyk.processing.ObservationLog
import com.omsharma.iykyk.processing.RepresentativePicker
import com.omsharma.iykyk.state.UiState
import com.omsharma.iykyk.utils.StageTiming
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// video -> frames -> faces -> appearances -> people -> photos, with progress along the way
class VideoProcessingRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    private val frameExtractor: FrameExtractor,
    private val faceDetector: FaceDetector,
    private val faceObserver: FaceObserver,
    private val identityClusterer: IdentityClusterer,
    private val representativePicker: RepresentativePicker
) {

    private data class DetectedFrame(val frame: ExtractedFrame, val faces: List<FaceDetection>)

    fun processVideo(videoUri: Uri): Flow<UiState<List<PersonResult>>> = flow {
        try {
            emit(UiState.Loading(stage = ProcessingStage.PREHEATING.label, progress = 0f))
            val tracker = FaceTracker()
            val log = ObservationLog(context)
            val timing = StageTiming()
            var framesProcessed = 0

            // Three overlapping stages: decode on IO, detect here, embed + track in the collector
            frameExtractor.extractFrames(videoUri)
                .buffer(FRAME_BUFFER)
                .map { frame ->
                    val startNs = System.nanoTime()
                    val faces = faceDetector.detect(frame.bitmap)
                    timing.detectNs += System.nanoTime() - startNs
                    DetectedFrame(frame, faces)
                }
                .flowOn(Dispatchers.Default)
                .buffer(FRAME_BUFFER)
                .collect { (frame, faces) ->
                    val startNs = System.nanoTime()
                    val observations = faceObserver.observe(frame, faces)
                    timing.embedNs += System.nanoTime() - startNs
                    observations.forEach(log::write)
                    tracker.update(observations, frame.timestampUs)
                    frame.bitmap.recycle()
                    framesProcessed++
                    emit(UiState.Loading(stage = ProcessingStage.COOKING.label, progress = (frame.frameIndex + 1f) / frame.frameCount))
                }

            val appearances = tracker.finish()
            if (appearances.isEmpty()) {
                emit(UiState.Failed("No clearly visible faces in this video"))
                return@flow
            }

            emit(UiState.Loading(stage = ProcessingStage.SIMMERING.label, progress = 1f))
            val people = identityClusterer.cluster(appearances)

            emit(UiState.Loading(stage = ProcessingStage.PLATING.label, progress = 1f))
            val pickStartNs = System.nanoTime()
            val results = representativePicker.pickAll(videoUri, people)
            timing.pickNs = System.nanoTime() - pickStartNs

            Log.d(
                TAG,
                "SUMMARY frames=$framesProcessed appearances=${appearances.size} people=${people.size} " +
                    "counts=${people.map { it.appearanceCount }} $timing " +
                    "spans=${appearances.joinToString { "${it.startUs / 1e6}-${it.endUs / 1e6}" }}"
            )

            if (results.isEmpty()) {
                emit(UiState.Failed("Found people but could not decode a photo for any of them"))
                return@flow
            }
            emit(UiState.Success(results))
        } catch (e: CancellationException) {
            throw e // user left the screen
        } catch (e: OutOfMemoryError) {
            emit(UiState.Failed("Ran out of memory processing this video"))
        } catch (e: Exception) {
            Log.e(TAG, "Processing failed", e)
            emit(UiState.Failed(e.message ?: "Something went wrong while processing the video"))
        }
    }.flowOn(Dispatchers.Default)

    private companion object {
        const val TAG = "Pipeline"
        const val FRAME_BUFFER = 2
    }
}

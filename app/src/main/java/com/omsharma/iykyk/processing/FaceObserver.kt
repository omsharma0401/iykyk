package com.omsharma.iykyk.processing

import android.util.Log
import com.omsharma.iykyk.constants.PipelineConfig
import com.omsharma.iykyk.data.model.ExtractedFrame
import com.omsharma.iykyk.data.model.FaceDetection
import com.omsharma.iykyk.data.model.FaceObservation
import com.omsharma.iykyk.ml.FaceEmbedder
import com.omsharma.iykyk.utils.sharpness
import javax.inject.Inject

// Turns one frame's detections into embedded observations (a failing face is skipped, not the frame)
class FaceObserver @Inject constructor(
    private val faceEmbedder: FaceEmbedder
) {

    fun observe(frame: ExtractedFrame, detected: List<FaceDetection>): List<FaceObservation> {
        val bitmap = frame.bitmap
        val faces = FaceFilter.withoutDuplicates(detected)
            .filter { FaceFilter.isLargeAndVisible(it.boundingBox, bitmap.width, bitmap.height) }
            .map { it to sharpness(bitmap, it.boundingBox) }
            .filter { (_, sharpness) -> sharpness >= PipelineConfig.MIN_SHARPNESS }

        return faces.mapNotNull { (face, sharpness) ->
            try {
                FaceObservation(
                    frameIndex = frame.frameIndex,
                    timestampUs = frame.timestampUs,
                    frameWidth = bitmap.width,
                    frameHeight = bitmap.height,
                    detection = face,
                    sharpness = sharpness,
                    facesInFrame = faces.size,
                    embedding = faceEmbedder.embed(bitmap, face)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Skipping a face at ${frame.timestampUs / 1e6}s", e)
                null
            }
        }
    }

    private companion object {
        const val TAG = "FaceObserver"
    }
}

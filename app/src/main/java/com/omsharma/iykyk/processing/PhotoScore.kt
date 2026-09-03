package com.omsharma.iykyk.processing

import com.omsharma.iykyk.data.model.FaceDetection
import com.omsharma.iykyk.data.model.FaceObservation
import com.omsharma.iykyk.utils.frontalness
import kotlin.math.hypot

object PhotoScore {

    private const val SHARPNESS_FOR_FULL_SCORE = 300f
    private const val SHARED_FRAME_PENALTY = 0.4f
    private const val NATURAL_EYE_TO_MOUTH_RATIO = 0.85f
    private const val IDEAL_FACE_TO_FRAME_WIDTH = 0.45f

    fun of(observation: FaceObservation): Float {
        val face = observation.detection
        val frontal = frontalness(face)
        val sharp = (observation.sharpness / SHARPNESS_FOR_FULL_SCORE).coerceIn(0f, 1f)
        val eyes = listOfNotNull(face.leftEyeOpenProbability, face.rightEyeOpenProbability)
        val eyesOpen = if (eyes.isEmpty()) 0.5f else eyes.average().toFloat()
        val smile = 0.6f + 0.4f * (face.smilingProbability ?: 0.5f)
        val alone = if (observation.facesInFrame == 1) 1f else SHARED_FRAME_PENALTY
        return frontal * sharp * eyesOpen * smile * alone * naturalProportions(face) * headroom(observation)
    }

    private fun naturalProportions(face: FaceDetection): Float {
        val leftEye = face.leftEye ?: return 1f
        val rightEye = face.rightEye ?: return 1f
        val mouth = face.mouthCenter ?: return 1f
        val eyeDistance = hypot(rightEye.x - leftEye.x, rightEye.y - leftEye.y)
        val eyeToMouth = hypot(mouth.x - (leftEye.x + rightEye.x) / 2f, mouth.y - (leftEye.y + rightEye.y) / 2f)
        if (eyeToMouth <= 0f) return 1f
        return (eyeDistance / eyeToMouth / NATURAL_EYE_TO_MOUTH_RATIO).coerceIn(0f, 1f)
    }

    private fun headroom(observation: FaceObservation): Float {
        val faceToWidth = observation.detection.boundingBox.height().toFloat() / observation.frameWidth
        return (IDEAL_FACE_TO_FRAME_WIDTH / faceToWidth).coerceIn(0f, 1f)
    }
}

package com.omsharma.iykyk.ml

import android.graphics.Bitmap
import android.graphics.PointF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import com.omsharma.iykyk.data.model.FaceDetection
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.mlkit.vision.face.FaceDetector as MlKitFaceDetector

// ML Kit wrapper
class FaceDetector @Inject constructor(
    private val detector: MlKitFaceDetector
) {

    // Faces in an upright frame
    suspend fun detect(frame: Bitmap): List<FaceDetection> =
        detector.process(InputImage.fromBitmap(frame, 0)).await().map { it.toFaceDetection() }

    private fun Face.toFaceDetection() = FaceDetection(
        boundingBox = boundingBox,
        leftEye = landmark(FaceLandmark.LEFT_EYE),
        rightEye = landmark(FaceLandmark.RIGHT_EYE),
        mouthLeft = landmark(FaceLandmark.MOUTH_LEFT),
        mouthRight = landmark(FaceLandmark.MOUTH_RIGHT),
        yawDegrees = headEulerAngleY,
        pitchDegrees = headEulerAngleX,
        smilingProbability = smilingProbability,
        leftEyeOpenProbability = leftEyeOpenProbability,
        rightEyeOpenProbability = rightEyeOpenProbability
    )

    private fun Face.landmark(type: Int): PointF? = getLandmark(type)?.position
}

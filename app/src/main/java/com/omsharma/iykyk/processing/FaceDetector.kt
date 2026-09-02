package com.omsharma.iykyk.processing

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector as MlKitFaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.omsharma.iykyk.data.model.DetectedFace
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FaceDetector @Inject constructor() {

    private val detector: MlKitFaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        FaceDetection.getClient(options)
    }

    // `bitmap` is assumed already upright - MediaMetadataRetriever bakes the video's
    // rotation metadata into the frames it returns, so rotationDegrees is always 0 here.
    suspend fun detect(bitmap: Bitmap): List<DetectedFace> {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val faces = detector.process(inputImage).await()
        return faces.map { face ->
            DetectedFace(
                boundingBox = face.boundingBox,
                leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position,
                rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position,
                headEulerAngleY = face.headEulerAngleY
            )
        }
    }
}

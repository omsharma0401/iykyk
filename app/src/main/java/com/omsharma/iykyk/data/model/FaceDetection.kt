package com.omsharma.iykyk.data.model

import android.graphics.PointF
import android.graphics.Rect

data class FaceDetection(
    val boundingBox: Rect,
    val leftEye: PointF?,
    val rightEye: PointF?,
    val mouthLeft: PointF?,
    val mouthRight: PointF?,
    val yawDegrees: Float,
    val pitchDegrees: Float,
    val smilingProbability: Float?,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?
) {
    val mouthCenter: PointF?
        get() {
            val left = mouthLeft ?: return null
            val right = mouthRight ?: return null
            return PointF((left.x + right.x) / 2f, (left.y + right.y) / 2f)
        }
}

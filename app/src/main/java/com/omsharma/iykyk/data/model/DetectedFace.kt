package com.omsharma.iykyk.data.model

import android.graphics.PointF
import android.graphics.Rect

data class DetectedFace(
    val boundingBox: Rect,
    val leftEye: PointF?,
    val rightEye: PointF?,
    val headEulerAngleY: Float
)

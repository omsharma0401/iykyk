package com.omsharma.iykyk.data.model

import android.graphics.Bitmap

data class EmbeddedFace(
    val embedding: List<Float>,
    val displayCrop: Bitmap,
    val boundingBoxArea: Int,
    val headEulerAngleY: Float
)

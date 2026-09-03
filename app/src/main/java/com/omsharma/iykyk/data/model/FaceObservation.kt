package com.omsharma.iykyk.data.model

class FaceObservation(
    val frameIndex: Int,
    val timestampUs: Long,
    val frameWidth: Int,
    val frameHeight: Int,
    val detection: FaceDetection,
    val sharpness: Float,
    val facesInFrame: Int,
    val embedding: FloatArray
)

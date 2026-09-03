package com.omsharma.iykyk.data.model

// One usable face at one moment of the video. Plain class: identity equality on purpose.
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

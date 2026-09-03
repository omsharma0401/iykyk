package com.omsharma.iykyk.utils

import com.omsharma.iykyk.data.model.FaceDetection
import kotlin.math.cos
import kotlin.math.sqrt

// Cosine similarity of two unit vectors
fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    var dot = 0f
    for (i in a.indices) dot += a[i] * b[i]
    return dot
}

// Weighted mean of unit vectors
fun meanEmbedding(embeddings: List<FloatArray>, weights: List<Float>): FloatArray {
    val mean = FloatArray(embeddings.first().size)
    for ((k, embedding) in embeddings.withIndex()) for (i in mean.indices) mean[i] += weights[k] * embedding[i]
    val norm = sqrt(mean.sumOf { (it * it).toDouble() }).toFloat()
    if (norm > 0f) for (i in mean.indices) mean[i] /= norm
    return mean
}

fun frontalness(face: FaceDetection): Float =
    (cos(Math.toRadians(face.yawDegrees.toDouble())) * cos(Math.toRadians(face.pitchDegrees.toDouble()))).toFloat().coerceIn(0f, 1f)

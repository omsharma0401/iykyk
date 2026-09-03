package com.omsharma.iykyk.data.model

import com.omsharma.iykyk.utils.frontalness
import com.omsharma.iykyk.utils.meanEmbedding

class Appearance(val id: Int, val observations: List<FaceObservation>) {
    val startUs: Long get() = observations.first().timestampUs
    val endUs: Long get() = observations.last().timestampUs

    val embedding: FloatArray = meanEmbedding(
        observations.map { it.embedding },
        observations.map { 0.2f + frontalness(it.detection) })

    fun overlapsInTime(other: Appearance): Boolean =
        startUs <= other.endUs && other.startUs <= endUs
}

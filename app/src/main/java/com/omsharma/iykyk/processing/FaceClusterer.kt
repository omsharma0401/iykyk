package com.omsharma.iykyk.processing

import com.omsharma.iykyk.data.model.EmbeddedFace
import com.omsharma.iykyk.data.model.FaceCluster
import kotlin.math.abs
import kotlin.math.sqrt
import javax.inject.Inject

class FaceClusterer @Inject constructor() {

    fun cluster(
        faces: List<EmbeddedFace>,
        similarityThreshold: Float = DEFAULT_SIMILARITY_THRESHOLD
    ): List<FaceCluster> {
        val clusters = mutableListOf<MutableCluster>()

        for (face in faces) {
            val bestMatch = findBestCluster(clusters, face.embedding)

            if (bestMatch != null && bestMatch.similarity >= similarityThreshold) {
                bestMatch.cluster.add(face)
            } else {
                clusters.add(MutableCluster(face))
            }
        }

        return clusters.map { it.toFaceCluster() }
    }

    private fun findBestCluster(clusters: List<MutableCluster>, embedding: List<Float>): Match? {
        var best: MutableCluster? = null
        var bestSimilarity = -1f
        for (cluster in clusters) {
            val similarity = cosineSimilarity(cluster.centroid, embedding)
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                best = cluster
            }
        }
        return best?.let { Match(it, bestSimilarity) }
    }

    private class Match(val cluster: MutableCluster, val similarity: Float)

    private class MutableCluster(firstFace: EmbeddedFace) {
        private val embeddingSum: FloatArray = firstFace.embedding.toFloatArray()
        private var count = 1
        private var representative = firstFace
        private var representativeScore = representativeScore(firstFace)

        val centroid: List<Float>
            get() = embeddingSum.map { it / count }

        fun add(face: EmbeddedFace) {
            for (i in embeddingSum.indices) embeddingSum[i] += face.embedding[i]
            count++

            val score = representativeScore(face)
            if (score > representativeScore) {
                representative = face
                representativeScore = score
            }
        }

        fun toFaceCluster() = FaceCluster(representative = representative, memberCount = count)

        companion object {
            // Prefer a face that's both large (close to camera) and roughly frontal.
            // Past ~45 degrees of yaw a face reads as profile, not a usable "portrait".
            private const val MAX_USABLE_YAW_DEGREES = 45f

            private fun representativeScore(face: EmbeddedFace): Float {
                val frontality = 1f - (abs(face.headEulerAngleY) / MAX_USABLE_YAW_DEGREES).coerceIn(0f, 1f)
                return face.boundingBoxArea * frontality
            }
        }
    }

    companion object {
        // Starting point only - MobileFaceNet's cosine-similarity distribution needs to be
        // verified against real recordings and tuned from there. Document the final value
        // chosen in the README, per the assignment's deliverables.
        const val DEFAULT_SIMILARITY_THRESHOLD = 0.6f
    }
}

private fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
    var dot = 0f
    var normA = 0f
    var normB = 0f
    for (i in a.indices) {
        dot += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
    }
    val denominator = sqrt(normA) * sqrt(normB)
    return if (denominator == 0f) 0f else dot / denominator
}

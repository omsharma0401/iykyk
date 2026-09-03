package com.omsharma.iykyk.processing

import com.omsharma.iykyk.constants.PipelineConfig
import com.omsharma.iykyk.data.model.Appearance
import com.omsharma.iykyk.data.model.Person
import com.omsharma.iykyk.utils.cosineSimilarity
import javax.inject.Inject

// Average-linkage clustering of appearances into people
class IdentityClusterer @Inject constructor() {

    fun cluster(appearances: List<Appearance>): List<Person> {
        val groups = appearances.map { mutableListOf(it) }.toMutableList()
        while (groups.size > 1) {
            var bestSimilarity = -1f
            var bestA = -1
            var bestB = -1
            for (a in groups.indices) {
                for (b in a + 1 until groups.size) {
                    if (cannotLink(groups[a], groups[b])) continue
                    val similarity = averageSimilarity(groups[a], groups[b])
                    if (similarity > bestSimilarity) {
                        bestSimilarity = similarity
                        bestA = a
                        bestB = b
                    }
                }
            }
            if (bestSimilarity < PipelineConfig.IDENTITY_MERGE_THRESHOLD) break
            groups[bestA].addAll(groups[bestB])
            groups.removeAt(bestB)
        }
        return groups
            .sortedBy { group -> group.minOf { it.startUs } }
            .mapIndexed { index, group -> Person(index, group.sortedBy { it.startUs }) }
    }

    // Co-visible = different people
    private fun cannotLink(a: List<Appearance>, b: List<Appearance>): Boolean =
        a.any { x -> b.any { y -> x.overlapsInTime(y) } }

    private fun averageSimilarity(a: List<Appearance>, b: List<Appearance>): Float {
        var sum = 0f
        for (x in a) for (y in b) sum += cosineSimilarity(x.embedding, y.embedding)
        return sum / (a.size * b.size)
    }
}

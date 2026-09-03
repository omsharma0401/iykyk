package com.omsharma.iykyk.processing

import com.omsharma.iykyk.constants.PipelineConfig
import com.omsharma.iykyk.data.model.Appearance
import com.omsharma.iykyk.data.model.FaceDetection
import com.omsharma.iykyk.data.model.FaceObservation
import com.omsharma.iykyk.utils.cosineSimilarity
import kotlin.math.abs
import kotlin.math.hypot

// Links per-frame observations into appearances
class FaceTracker {

    private class Track(val id: Int, val observations: MutableList<FaceObservation>) {
        val last: FaceObservation get() = observations.last()
    }

    private val active = mutableListOf<Track>()
    private val finished = mutableListOf<Appearance>()
    private var nextTrackId = 0

    // Match this frame's faces to open tracks
    fun update(observations: List<FaceObservation>, timestampUs: Long) {
        closeStale(timestampUs)

        val candidates = ArrayList<Triple<Float, FaceObservation, Track>>()
        for (observation in observations) {
            for (track in active) {
                val similarity = cosineSimilarity(track.last.embedding, observation.embedding)
                if (canLink(track.last, observation, similarity)) candidates += Triple(similarity, observation, track)
            }
        }
        candidates.sortByDescending { it.first }

        val matchedObservations = HashSet<FaceObservation>()
        val matchedTracks = HashSet<Track>()
        for ((_, observation, track) in candidates) {
            if (observation in matchedObservations || track in matchedTracks) continue
            track.observations += observation
            matchedObservations += observation
            matchedTracks += track
        }

        for (observation in observations) {
            if (observation !in matchedObservations) active += Track(nextTrackId++, mutableListOf(observation))
        }
    }

    // Close everything
    fun finish(): List<Appearance> {
        active.forEach(::close)
        active.clear()
        return finished.sortedBy { it.startUs }
    }

    // Unseen for longer than the gap = appearance over
    private fun closeStale(timestampUs: Long) {
        val stale = active.filter { timestampUs - it.last.timestampUs > PipelineConfig.TRACK_GAP_US }
        stale.forEach(::close)
        active.removeAll(stale)
    }

    // Too short = whip-pan pass, dropped
    private fun close(track: Track) {
        if (track.observations.size >= PipelineConfig.MIN_OBSERVATIONS_PER_APPEARANCE) {
            finished += Appearance(track.id, track.observations.toList())
        }
    }

    // Same face if it looks the same and moved a little, or stayed put through a head turn
    private fun canLink(from: FaceObservation, to: FaceObservation, similarity: Float): Boolean {
        val a = from.detection.boundingBox
        val b = to.detection.boundingBox
        val move = hypot(b.exactCenterX() - a.exactCenterX(), b.exactCenterY() - a.exactCenterY()) / a.width()
        if (similarity >= PipelineConfig.TRACK_LINK_SIMILARITY && move <= PipelineConfig.TRACK_MAX_MOVE_BOX_WIDTHS) return true
        val turned = isTurned(from.detection) || isTurned(to.detection)
        return turned && similarity >= PipelineConfig.TRACK_STAY_MIN_SIMILARITY && move <= PipelineConfig.TRACK_STAY_MOVE_BOX_WIDTHS
    }

    private fun isTurned(face: FaceDetection): Boolean =
        abs(face.yawDegrees) > PipelineConfig.TRACK_TURN_DEGREES || abs(face.pitchDegrees) > PipelineConfig.TRACK_TURN_DEGREES
}

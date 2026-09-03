package com.omsharma.iykyk.constants

// Every tunable number in the pipeline. "Measured" = read off the three sample clips with tools/eval.
object PipelineConfig {

    // Sampling
    const val SAMPLE_INTERVAL_US = 333_333L      // 3 fps; the shortest appearance (~0.5 s) still gets 3-4 samples
    const val ANALYSIS_MAX_DIMENSION = 720       // longer side of the frames handed to ML Kit

    // Face filtering
    const val MIN_FACE_WIDTH_RATIO = 0.05f       // ML Kit's own minimum, fraction of frame width
    const val MIN_FACE_HEIGHT_RATIO = 0.08f      // measured: usable faces >= 0.22
    const val MIN_VISIBLE_FRACTION = 0.6f        // measured: usable faces >= 0.73 inside the frame
    const val MIN_SHARPNESS = 60f                // measured: whip-pan blur 5-11, real faces >= 590
    const val DUPLICATE_CONTAINMENT = 0.7f       // ML Kit reports huge faces twice; containment ~1.0

    // Tracking
    const val TRACK_GAP_US = 450_000L            // < 2 sample intervals: one missed sample ends the appearance
    const val TRACK_LINK_SIMILARITY = 0.5f       // measured: same person frame-to-frame >= 0.63, strangers <= 0.61
    const val TRACK_MAX_MOVE_BOX_WIDTHS = 1.5f
    const val TRACK_STAY_MOVE_BOX_WIDTHS = 0.5f  // a face that stays put continues through a head turn...
    const val TRACK_STAY_MIN_SIMILARITY = 0.3f   // ...if the embedding is not outright different...
    const val TRACK_TURN_DEGREES = 25f           // ...and one side is actually turned (else it may be a hard cut)
    const val MIN_OBSERVATIONS_PER_APPEARANCE = 2

    // Identity
    const val IDENTITY_MERGE_THRESHOLD = 0.60f   // measured: same person >= 0.63 (worst clip), strangers <= 0.57

    // Output
    const val PHOTO_HEIGHT = 1080                // full-resolution photo kept per person
}

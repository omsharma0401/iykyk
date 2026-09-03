package com.omsharma.iykyk.constants

// Every tunable number in the pipeline. "Measured" = read off the three sample clips with tools/eval.
object PipelineConfig {

    // Sampling
    // 3 fps is the practical speed/recall trade-off measured on the supplied 30 s clips.
    // It gives a typical 1 s appearance three observations while keeping on-device processing responsive.
    const val SAMPLE_INTERVAL_US = 333_333L
    const val ANALYSIS_MAX_DIMENSION = 720       // Caps ML input cost while preserving split-screen faces.

    // Face filtering
    const val MIN_FACE_WIDTH_RATIO = 0.05f       // ML Kit's input gate; avoids detector work on implausibly tiny faces.
    const val MIN_FACE_HEIGHT_RATIO = 0.08f      // Conservative quality gate; sample usable faces were >= 0.22.
    const val MIN_VISIBLE_FRACTION = 0.6f        // Rejects mostly off-screen faces; sample usable faces were >= 0.73.
    const val MIN_SHARPNESS = 60f                // 64 px Laplacian variance; sample whip-pans 5-11, usable faces >= 590.
    const val DUPLICATE_CONTAINMENT = 0.7f       // Removes nested ML Kit boxes without removing side-by-side faces.

    // Tracking
    const val TRACK_GAP_US = 450_000L            // Longer than one sample interval, shorter than two: a missed sample ends an appearance.
    const val TRACK_LINK_SIMILARITY = 0.5f       // Combined with position; lowers false splits when pose changes between samples.
    const val TRACK_MAX_MOVE_BOX_WIDTHS = 1.5f   // Allows normal hand-held camera motion, rejects a cut to a distant face.
    const val TRACK_STAY_MOVE_BOX_WIDTHS = 0.5f  // A stationary face may continue through a head turn...
    const val TRACK_STAY_MIN_SIMILARITY = 0.3f   // ...but only when its position and pose indicate that special case.
    const val TRACK_TURN_DEGREES = 25f           // Distinguishes a real head turn from two frontal faces after a cut.
    const val MIN_OBSERVATIONS_PER_APPEARANCE = 2 // Suppresses one-frame detector noise and whip-pan passes.

    // Identity
    // Selected from the observed 0.58 (highest different-person) to 0.63 (lowest same-person) gap.
    // Recalibrate this value if the embedding model, alignment, or target video domain changes.
    const val IDENTITY_MERGE_THRESHOLD = 0.60f

    // Output
    const val PHOTO_HEIGHT = 1080                // Keeps a story tile sharp without retaining full camera-sized bitmaps.
}

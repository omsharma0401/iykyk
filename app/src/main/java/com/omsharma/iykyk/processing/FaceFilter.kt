package com.omsharma.iykyk.processing

import android.graphics.Rect
import com.omsharma.iykyk.constants.PipelineConfig
import com.omsharma.iykyk.data.model.FaceDetection

// Cheap geometric checks before a face is worth embedding.
object FaceFilter {

    // Big enough and not cut off by the frame edge
    fun isLargeAndVisible(box: Rect, frameWidth: Int, frameHeight: Int): Boolean {
        if (box.width() <= 0 || box.height() <= 0) return false
        if (box.height() < frameHeight * PipelineConfig.MIN_FACE_HEIGHT_RATIO) return false
        val visibleWidth = (minOf(box.right, frameWidth) - maxOf(box.left, 0)).coerceAtLeast(0)
        val visibleHeight = (minOf(box.bottom, frameHeight) - maxOf(box.top, 0)).coerceAtLeast(0)
        val visibleFraction = visibleWidth.toFloat() * visibleHeight / (box.width().toFloat() * box.height())
        return visibleFraction >= PipelineConfig.MIN_VISIBLE_FRACTION
    }

    // Drop a box that mostly sits inside a larger one
    fun withoutDuplicates(faces: List<FaceDetection>): List<FaceDetection> {
        val kept = mutableListOf<FaceDetection>()
        for (face in faces.sortedByDescending { it.boundingBox.width().toLong() * it.boundingBox.height() }) {
            val duplicate = kept.any { containment(it.boundingBox, face.boundingBox) >= PipelineConfig.DUPLICATE_CONTAINMENT }
            if (!duplicate) kept += face
        }
        return kept
    }

    // Fraction of small covered by big
    private fun containment(big: Rect, small: Rect): Float {
        val width = (minOf(big.right, small.right) - maxOf(big.left, small.left)).coerceAtLeast(0)
        val height = (minOf(big.bottom, small.bottom) - maxOf(big.top, small.top)).coerceAtLeast(0)
        val smallArea = small.width().toFloat() * small.height()
        return if (smallArea <= 0f) 0f else width.toFloat() * height / smallArea
    }
}

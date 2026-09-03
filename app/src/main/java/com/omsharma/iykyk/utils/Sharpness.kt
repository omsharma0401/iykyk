package com.omsharma.iykyk.utils

import android.graphics.Bitmap
import android.graphics.Rect

private const val SAMPLE_SIZE = 64

// Laplacian variance of the face region
fun sharpness(bitmap: Bitmap, boundingBox: Rect): Float {
    val crop = safeCrop(bitmap, boundingBox) ?: return 0f
    val scaled = Bitmap.createScaledBitmap(crop, SAMPLE_SIZE, SAMPLE_SIZE, true)
    if (scaled !== crop) crop.recycle()
    val gray = toGrayscale(scaled)
    scaled.recycle()
    return laplacianVariance(gray, SAMPLE_SIZE, SAMPLE_SIZE)
}

// Crop clamped to the bitmap
private fun safeCrop(bitmap: Bitmap, boundingBox: Rect): Bitmap? {
    val left = boundingBox.left.coerceIn(0, bitmap.width)
    val top = boundingBox.top.coerceIn(0, bitmap.height)
    val width = boundingBox.right.coerceIn(0, bitmap.width) - left
    val height = boundingBox.bottom.coerceIn(0, bitmap.height) - top
    if (width <= 0 || height <= 0) return null
    return Bitmap.createBitmap(bitmap, left, top, width, height)
}

private fun toGrayscale(bitmap: Bitmap): IntArray {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return IntArray(pixels.size) { i ->
        val p = pixels[i]
        (0.299f * (p shr 16 and 0xFF) + 0.587f * (p shr 8 and 0xFF) + 0.114f * (p and 0xFF)).toInt()
    }
}

private fun laplacianVariance(gray: IntArray, width: Int, height: Int): Float {
    val responses = IntArray((width - 2) * (height - 2))
    var n = 0
    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val i = y * width + x
            responses[n++] =
                4 * gray[i] - gray[i - width] - gray[i + width] - gray[i - 1] - gray[i + 1]
        }
    }
    if (n == 0) return 0f
    val mean = responses.sumOf { it.toDouble() } / n
    return (responses.sumOf { (it - mean) * (it - mean) } / n).toFloat()
}

package com.omsharma.iykyk.utils

import android.graphics.Bitmap
import android.media.Image

object YuvToBitmap {

    fun convert(image: Image, outWidth: Int, outHeight: Int, rotation: Int): Bitmap {
        val y = image.planes[0].buffer
        val u = image.planes[1].buffer
        val v = image.planes[2].buffer
        val yRowStride = image.planes[0].rowStride
        val uvRowStride = image.planes[1].rowStride
        val uvPixelStride = image.planes[1].pixelStride
        val srcWidth = image.width
        val srcHeight = image.height

        val sideways = rotation == 90 || rotation == 270
        val scaleX = (if (sideways) srcHeight else srcWidth).toFloat() / outWidth
        val scaleY = (if (sideways) srcWidth else srcHeight).toFloat() / outHeight

        val pixels = IntArray(outWidth * outHeight)
        var index = 0
        for (oy in 0 until outHeight) {
            for (ox in 0 until outWidth) {
                val ux = (ox * scaleX).toInt()
                val uy = (oy * scaleY).toInt()
                // Upright coordinate -> stored (rotated) frame coordinate
                val sx: Int
                val sy: Int
                when (rotation) {
                    90 -> { sx = uy; sy = srcHeight - 1 - ux }
                    180 -> { sx = srcWidth - 1 - ux; sy = srcHeight - 1 - uy }
                    270 -> { sx = srcWidth - 1 - uy; sy = ux }
                    else -> { sx = ux; sy = uy }
                }
                val yy = (y.get(sy * yRowStride + sx).toInt() and 0xFF) - 16
                val uvIndex = (sy / 2) * uvRowStride + (sx / 2) * uvPixelStride
                val uu = (u.get(uvIndex).toInt() and 0xFF) - 128
                val vv = (v.get(uvIndex).toInt() and 0xFF) - 128
                // BT.601, fixed point
                val yScaled = 1192 * maxOf(yy, 0)
                val r = ((yScaled + 1634 * vv) shr 10).coerceIn(0, 255)
                val g = ((yScaled - 833 * vv - 400 * uu) shr 10).coerceIn(0, 255)
                val b = ((yScaled + 2066 * uu) shr 10).coerceIn(0, 255)
                pixels[index++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return Bitmap.createBitmap(pixels, outWidth, outHeight, Bitmap.Config.ARGB_8888)
    }
}

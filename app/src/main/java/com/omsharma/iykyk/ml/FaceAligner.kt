package com.omsharma.iykyk.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.omsharma.iykyk.data.model.FaceDetection

object FaceAligner {

    const val SIZE = 112

    // left eye, right eye, mouth centre
    private val TEMPLATE = floatArrayOf(38.2946f, 51.6963f, 73.5318f, 51.5014f, 56.1396f, 92.2848f)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun align(frame: Bitmap, face: FaceDetection): Bitmap {
        val matrix = Matrix()
        val le = face.leftEye
        val re = face.rightEye
        val mouth = face.mouthCenter
        val fitted = when {
            le != null && re != null && mouth != null ->
                matrix.setPolyToPoly(floatArrayOf(le.x, le.y, re.x, re.y, mouth.x, mouth.y), 0, TEMPLATE, 0, 3)
            le != null && re != null ->
                matrix.setPolyToPoly(floatArrayOf(le.x, le.y, re.x, re.y), 0, TEMPLATE, 0, 2)
            else -> false
        }
        if (!fitted) {
            val box = face.boundingBox
            val scale = SIZE.toFloat() / maxOf(box.width(), box.height()).coerceAtLeast(1)
            matrix.setTranslate(-box.left.toFloat(), -box.top.toFloat())
            matrix.postScale(scale, scale)
        }
        val aligned = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        Canvas(aligned).drawBitmap(frame, matrix, paint)
        return aligned
    }
}

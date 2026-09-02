package com.omsharma.iykyk.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.sqrt

class CollageBuilder @Inject constructor() {

    fun build(
        faces: List<Bitmap>,
        cellSize: Int = DEFAULT_CELL_SIZE,
        spacing: Int = DEFAULT_SPACING,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        require(faces.isNotEmpty()) { "Cannot build a collage from zero faces" }

        val columns = ceil(sqrt(faces.size.toDouble())).toInt()
        val rows = ceil(faces.size.toDouble() / columns).toInt()

        val width = columns * cellSize + (columns + 1) * spacing
        val height = rows * cellSize + (rows + 1) * spacing

        val collage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(collage)
        canvas.drawColor(backgroundColor)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        faces.forEachIndexed { index, face ->
            val column = index % columns
            val row = index / columns

            val left = spacing + column * (cellSize + spacing)
            val top = spacing + row * (cellSize + spacing)
            val destRect = Rect(left, top, left + cellSize, top + cellSize)

            canvas.drawBitmap(face, centerCropSquare(face), destRect, paint)
        }

        return collage
    }

    // Crop to the largest centered square so faces fill their cell without distortion,
    // regardless of the source crop's original aspect ratio.
    private fun centerCropSquare(bitmap: Bitmap): Rect {
        val size = minOf(bitmap.width, bitmap.height)
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        return Rect(left, top, left + size, top + size)
    }

    companion object {
        private const val DEFAULT_CELL_SIZE = 400
        private const val DEFAULT_SPACING = 8
    }
}

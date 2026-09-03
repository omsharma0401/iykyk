package com.omsharma.iykyk.utils.collage

import android.graphics.Rect
import android.graphics.RectF
import com.omsharma.iykyk.constants.CollageSpec
import kotlin.math.ceil
import kotlin.math.roundToInt

object CollageLayout {

    fun cells(count: Int): List<RectF> {
        val columns = if (count == 1) 1 else 2
        val rows = ceil(count / columns.toFloat()).toInt()
        val areaWidth = CollageSpec.WIDTH - 2 * CollageSpec.MARGIN
        val areaHeight = CollageSpec.HEIGHT - CollageSpec.HEADER_HEIGHT - CollageSpec.FOOTER_HEIGHT
        val cellWidth = (areaWidth - CollageSpec.GUTTER * (columns - 1)) / columns
        val cellHeight = minOf((areaHeight - CollageSpec.GUTTER * (rows - 1)) / rows, cellWidth / CollageSpec.MIN_TILE_ASPECT)
        val gridHeight = rows * cellHeight + (rows - 1) * CollageSpec.GUTTER
        val gridTop = CollageSpec.HEADER_HEIGHT + (areaHeight - gridHeight) / 2f
        return List(count) { index ->
            val row = index / columns
            val column = index % columns
            val cellsInRow = minOf(columns, count - row * columns)
            val rowWidth = cellsInRow * cellWidth + (cellsInRow - 1) * CollageSpec.GUTTER // short last row centred
            val left = CollageSpec.MARGIN + (areaWidth - rowWidth) / 2f + column * (cellWidth + CollageSpec.GUTTER)
            val top = gridTop + row * (cellHeight + CollageSpec.GUTTER)
            RectF(left, top, left + cellWidth, top + cellHeight)
        }
    }

    fun fitCrop(photoWidth: Int, photoHeight: Int, faceBox: Rect, aspect: Float): Rect {
        var cropWidth = photoWidth.toFloat()
        var cropHeight = cropWidth / aspect
        if (cropHeight > photoHeight) {
            cropHeight = photoHeight.toFloat()
            cropWidth = cropHeight * aspect
        }
        val left = (faceBox.exactCenterX() - cropWidth / 2f).coerceIn(0f, photoWidth - cropWidth)
        val top = (faceBox.exactCenterY() - cropHeight * CollageSpec.FACE_LINE).coerceIn(0f, photoHeight - cropHeight)
        return Rect(left.roundToInt(), top.roundToInt(), (left + cropWidth).roundToInt(), (top + cropHeight).roundToInt())
    }
}

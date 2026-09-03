package com.omsharma.iykyk.data.model

import android.graphics.Bitmap

data class ExtractedFrame(
    val frameIndex: Int,
    val frameCount: Int,
    val timestampUs: Long,
    val bitmap: Bitmap
)

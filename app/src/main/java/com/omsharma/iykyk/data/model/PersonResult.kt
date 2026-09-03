package com.omsharma.iykyk.data.model

import android.graphics.Bitmap
import android.graphics.Rect

class PersonResult(
    val label: String,
    val appearanceCount: Int,
    val photo: Bitmap,
    val faceBox: Rect
)

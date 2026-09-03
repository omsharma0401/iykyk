package com.omsharma.iykyk.data.model

import android.graphics.Bitmap
import android.graphics.Rect

// One person for the UI: a full-frame photo (never a face crop) plus where the face is in it.
class PersonResult(
    val label: String,
    val appearanceCount: Int,
    val photo: Bitmap,
    val faceBox: Rect
)

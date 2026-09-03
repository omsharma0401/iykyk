package com.omsharma.iykyk.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.omsharma.iykyk.constants.PipelineConfig
import com.omsharma.iykyk.data.model.Person
import com.omsharma.iykyk.data.model.PersonResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.roundToInt

// Picks each person's best-looking frame and re-decodes it at full resolution.
class RepresentativePicker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun pickAll(videoUri: Uri, people: List<Person>): List<PersonResult> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)
            people.mapNotNull { person -> pick(retriever, person) }
        } finally {
            retriever.release()
        }
    }

    private fun pick(retriever: MediaMetadataRetriever, person: Person): PersonResult? {
        val best = person.observations.maxByOrNull { PhotoScore.of(it) } ?: return null
        val frame = retriever.getFrameAtTime(best.timestampUs, MediaMetadataRetriever.OPTION_CLOSEST) ?: return null
        val photo = if (frame.height > PipelineConfig.PHOTO_HEIGHT) {
            val scale = PipelineConfig.PHOTO_HEIGHT.toFloat() / frame.height
            Bitmap.createScaledBitmap(frame, (frame.width * scale).roundToInt().coerceAtLeast(1), PipelineConfig.PHOTO_HEIGHT, true)
                .also { frame.recycle() }
        } else {
            frame
        }
        // Analysis-frame box -> photo pixels
        val toPhotoX = photo.width.toFloat() / best.frameWidth
        val toPhotoY = photo.height.toFloat() / best.frameHeight
        val box = best.detection.boundingBox
        val faceBox = Rect(
            (box.left * toPhotoX).roundToInt(), (box.top * toPhotoY).roundToInt(),
            (box.right * toPhotoX).roundToInt(), (box.bottom * toPhotoY).roundToInt()
        )
        return PersonResult("Person ${person.id + 1}", person.appearanceCount, photo, faceBox)
    }
}

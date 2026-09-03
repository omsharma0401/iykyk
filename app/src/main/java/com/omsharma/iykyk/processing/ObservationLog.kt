package com.omsharma.iykyk.processing

import android.content.Context
import android.graphics.PointF
import com.omsharma.iykyk.BuildConfig
import com.omsharma.iykyk.data.model.FaceObservation
import java.io.File

// Debug builds only: one JSON line per usable face, replayed offline by tools/eval
class ObservationLog(context: Context) {

    private val file: File? =
        if (BuildConfig.DEBUG) File(context.filesDir, "observations.jsonl").also { it.delete() } else null

    fun write(observation: FaceObservation) {
        val file = file ?: return
        val face = observation.detection
        val box = face.boundingBox
        fun point(p: PointF?) = p?.let { "[${it.x},${it.y}]" } ?: "null"
        file.appendText(
            "{\"frame\":${observation.frameIndex},\"tUs\":${observation.timestampUs}," +
                "\"box\":[${box.left},${box.top},${box.right},${box.bottom}]," +
                "\"fw\":${observation.frameWidth},\"fh\":${observation.frameHeight}," +
                "\"yaw\":${face.yawDegrees},\"pitch\":${face.pitchDegrees}," +
                "\"smile\":${face.smilingProbability},\"leftEye\":${face.leftEyeOpenProbability},\"rightEye\":${face.rightEyeOpenProbability}," +
                "\"lm\":{\"le\":${point(face.leftEye)},\"re\":${point(face.rightEye)},\"ml\":${point(face.mouthLeft)},\"mr\":${point(face.mouthRight)}}," +
                "\"sharp\":${observation.sharpness},\"faces\":${observation.facesInFrame}," +
                "\"emb\":${observation.embedding.joinToString(",", "[", "]")}}\n"
        )
    }
}

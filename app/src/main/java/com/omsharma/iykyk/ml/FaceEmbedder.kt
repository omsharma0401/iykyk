package com.omsharma.iykyk.ml

import android.graphics.Bitmap
import com.omsharma.iykyk.data.model.FaceDetection
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.math.sqrt

// MobileFaceNet 192-d unit vector
class FaceEmbedder @Inject constructor(
    private val interpreter: Interpreter
) {

    private val input: ByteBuffer =
        ByteBuffer.allocateDirect(BATCH_SIZE * FaceAligner.SIZE * FaceAligner.SIZE * 3 * 4).order(ByteOrder.nativeOrder())
    private val pixels = IntArray(FaceAligner.SIZE * FaceAligner.SIZE)

    // Embed one face
    fun embed(frame: Bitmap, face: FaceDetection): FloatArray {
        val aligned = FaceAligner.align(frame, face)
        fillInput(aligned)
        aligned.recycle()
        val output = Array(BATCH_SIZE) { FloatArray(EMBEDDING_DIM) }
        interpreter.run(input, output)
        return l2Normalize(output[0])
    }

    private fun fillInput(bitmap: Bitmap) {
        bitmap.getPixels(pixels, 0, FaceAligner.SIZE, 0, 0, FaceAligner.SIZE, FaceAligner.SIZE)
        input.rewind()
        repeat(BATCH_SIZE) {
            for (pixel in pixels) {
                input.putFloat(((pixel shr 16 and 0xFF) - 127.5f) / 128f)
                input.putFloat(((pixel shr 8 and 0xFF) - 127.5f) / 128f)
                input.putFloat(((pixel and 0xFF) - 127.5f) / 128f)
            }
        }
        input.rewind()
    }

    // L2 Normalization
    private fun l2Normalize(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
        return if (norm > 0f) FloatArray(vector.size) { vector[it] / norm } else vector
    }

    private companion object {
        const val EMBEDDING_DIM = 192
        const val BATCH_SIZE = 2
    }
}

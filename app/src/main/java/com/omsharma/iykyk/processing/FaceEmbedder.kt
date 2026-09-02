package com.omsharma.iykyk.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.annotation.WorkerThread
import com.omsharma.iykyk.data.model.DetectedFace
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.math.sqrt

// MobileFaceNet, converted and redistributed by
// github.com/syaringan357/Android-MobileFaceNet-MTCNN-FaceAntiSpoofing (MIT), originally
// trained via github.com/sirius-ai/MobileFaceNet_TF (Apache-2.0) with ArcFace/InsightFace
// angular-margin loss on MS1M-refine-v2.
// Input: 112x112x3 RGB, normalized to ~[-1, 1] via (pixel - 127.5) / 128.
// Output: 192-dim embedding - L2-normalized here so cosine similarity is well-behaved
// regardless of the model's raw output scale.
//
// TODO: this model's input tensor has a FIXED batch size of 2 - it was exported for
// pairwise face verification ("are these two faces the same person"), not single-image
// embedding extraction. We only ever have one face crop at a time, so embed() duplicates
// it into both batch slots and reads just the first output (see BATCH_SIZE below). Works
// correctly but costs one wasted inference per face. Revisit if we swap to a model with
// a batch-1 or dynamic-batch input.
class FaceEmbedder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val interpreter: Interpreter by lazy {
        Interpreter(FileUtil.loadMappedFile(context, MODEL_FILENAME))
    }

    private val imageProcessor: ImageProcessor by lazy {
        ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(127.5f, 128f))
            .build()
    }

    @WorkerThread
    fun embed(frame: Bitmap, face: DetectedFace): List<Float> {
        val aligned = alignFace(frame, face)

        val tensorImage = TensorImage(DataType.FLOAT32).apply { load(aligned) }
        val processedImage = imageProcessor.process(tensorImage)

        val batchedInput = duplicateForBatch(processedImage.buffer)
        val output = Array(BATCH_SIZE) { FloatArray(EMBEDDING_DIM) }
        interpreter.run(batchedInput, output)

        return l2Normalize(output[0])
    }

    // Fills both of the model's fixed batch slots with the same image - see the TODO
    // at the top of this file for why.
    private fun duplicateForBatch(singleImageBuffer: ByteBuffer): ByteBuffer {
        val batched = ByteBuffer.allocateDirect(singleImageBuffer.capacity() * BATCH_SIZE)
            .order(ByteOrder.nativeOrder())
        repeat(BATCH_SIZE) {
            singleImageBuffer.rewind()
            batched.put(singleImageBuffer)
        }
        batched.rewind()
        return batched
    }

    // Rotates/scales the source frame so the detected eyes land on MobileFaceNet's
    // canonical eye positions inside a 112x112 crop (the standard ArcFace alignment
    // template). Falls back to a plain bounding-box crop when eye landmarks are missing
    // (e.g. a strongly profile face) - degraded alignment beats dropping the face.
    private fun alignFace(frame: Bitmap, face: DetectedFace): Bitmap {
        val leftEye = face.leftEye
        val rightEye = face.rightEye

        val matrix = if (leftEye != null && rightEye != null) {
            Matrix().apply {
                setPolyToPoly(
                    floatArrayOf(leftEye.x, leftEye.y, rightEye.x, rightEye.y), 0,
                    CANONICAL_EYE_POSITIONS, 0,
                    2
                )
            }
        } else {
            val box = face.boundingBox
            val scale = INPUT_SIZE.toFloat() / maxOf(box.width(), box.height())
            Matrix().apply {
                setTranslate(-box.left.toFloat(), -box.top.toFloat())
                postScale(scale, scale)
            }
        }

        val aligned = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        Canvas(aligned).drawBitmap(frame, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return aligned
    }

    private fun l2Normalize(embedding: FloatArray): List<Float> {
        val norm = sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
        return if (norm > 0f) embedding.map { it / norm } else embedding.toList()
    }

    companion object {
        private const val MODEL_FILENAME = "MobileFaceNet.tflite"
        private const val INPUT_SIZE = 112
        private const val EMBEDDING_DIM = 192
        private const val BATCH_SIZE = 2

        // Standard 112x112 ArcFace alignment template eye positions.
        private val CANONICAL_EYE_POSITIONS = floatArrayOf(
            38.2946f, 51.6963f, // left eye
            73.5318f, 51.5014f  // right eye
        )
    }
}

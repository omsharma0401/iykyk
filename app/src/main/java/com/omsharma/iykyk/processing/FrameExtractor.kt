package com.omsharma.iykyk.processing

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import com.omsharma.iykyk.constants.PipelineConfig
import com.omsharma.iykyk.data.model.ExtractedFrame
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import kotlin.math.roundToInt

// Samples a video at 3 fps as downscaled upright frames
class FrameExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun extractFrames(videoUri: Uri): Flow<ExtractedFrame> = flow {
        val video = probe(videoUri)
        try {
            VideoDecoder(context).decode(videoUri, video) { emit(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Sequential decode failed, seeking frame by frame instead", e)
            seekFrames(videoUri, video) { emit(it) }
        }
    }.flowOn(Dispatchers.IO)

    // Upright size, rotation, duration
    private fun probe(videoUri: Uri): VideoInfo {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            fun meta(key: Int) = retriever.extractMetadata(key)
            val storedWidth = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1080
            val storedHeight = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1920
            val rotation = ((meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0) % 360 + 360) % 360
            val sideways = rotation == 90 || rotation == 270
            return VideoInfo(
                width = if (sideways) storedHeight else storedWidth,
                height = if (sideways) storedWidth else storedHeight,
                rotation = rotation,
                durationUs = (meta(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) * 1_000
            )
        } finally {
            retriever.release()
        }
    }

    private suspend fun seekFrames(videoUri: Uri, video: VideoInfo, emit: suspend (ExtractedFrame) -> Unit) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            for (index in 0 until video.frameCount) {
                val timestampUs = index * PipelineConfig.SAMPLE_INTERVAL_US
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(timestampUs, MediaMetadataRetriever.OPTION_CLOSEST, video.targetWidth, video.targetHeight)
                } else {
                    retriever.getFrameAtTime(timestampUs, MediaMetadataRetriever.OPTION_CLOSEST)
                }
                if (bitmap != null) emit(ExtractedFrame(index, video.frameCount, timestampUs, bitmap))
            }
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val TAG = "FrameExtractor"
    }
}

class VideoInfo(val width: Int, val height: Int, val rotation: Int, val durationUs: Long) {
    val frameCount = ((durationUs + PipelineConfig.SAMPLE_INTERVAL_US - 1) / PipelineConfig.SAMPLE_INTERVAL_US).toInt().coerceAtLeast(1)
    private val scale = minOf(1f, PipelineConfig.ANALYSIS_MAX_DIMENSION.toFloat() / maxOf(width, height))
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
}

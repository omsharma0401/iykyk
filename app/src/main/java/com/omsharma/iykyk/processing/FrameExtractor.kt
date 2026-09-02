package com.omsharma.iykyk.processing

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FrameExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // Cold Flow so the caller can process (detect + embed) and discard each full-resolution
    // frame one at a time, rather than holding every extracted frame in memory at once -
    // ~40 frames at camera resolution would otherwise be hundreds of MB.
    fun extractFrames(
        videoUri: Uri,
        sampleIntervalUs: Long = DEFAULT_SAMPLE_INTERVAL_US
    ): Flow<Bitmap> = flow {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)

            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val durationUs = durationMs * 1_000

            var timestampUs = 0L
            while (timestampUs < durationUs) {
                val frame = retriever.getFrameAtTime(timestampUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (frame != null) emit(frame)
                timestampUs += sampleIntervalUs
            }
        } finally {
            retriever.release()
        }
    }

    companion object {
        private const val DEFAULT_SAMPLE_INTERVAL_US = 500_000L // ~2fps
    }
}

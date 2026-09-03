package com.omsharma.iykyk.processing

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.omsharma.iykyk.constants.PipelineConfig
import com.omsharma.iykyk.data.model.ExtractedFrame
import com.omsharma.iykyk.utils.YuvToBitmap

class VideoDecoder(private val context: Context) {

    suspend fun decode(videoUri: Uri, video: VideoInfo, emit: suspend (ExtractedFrame) -> Unit) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, videoUri, null)
            val track = (0 until extractor.trackCount).first {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
            }
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!).apply {
                configure(format, null, null, 0)
                start()
            }

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var sampleIndex = 0
            while (!outputDone && sampleIndex < video.frameCount) {
                if (!inputDone) inputDone = feedInput(codec, extractor)

                val outputIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                if (outputIndex < 0) continue
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true

                val sampleUs = sampleIndex * PipelineConfig.SAMPLE_INTERVAL_US
                if (info.size > 0 && info.presentationTimeUs + HALF_FRAME_US >= sampleUs) {
                    // First decoded frame at or past the sample time is that sample
                    val image = codec.getOutputImage(outputIndex)
                    val bitmap = image?.let { YuvToBitmap.convert(it, video.targetWidth, video.targetHeight, video.rotation) }
                    image?.close()
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bitmap != null) emit(ExtractedFrame(sampleIndex, video.frameCount, sampleUs, bitmap))
                    sampleIndex++
                    while (sampleIndex < video.frameCount &&
                        info.presentationTimeUs + HALF_FRAME_US >= sampleIndex * PipelineConfig.SAMPLE_INTERVAL_US
                    ) sampleIndex++ // dropped frames in the stream
                } else {
                    codec.releaseOutputBuffer(outputIndex, false)
                }
            }
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            codec?.release()
            extractor.release()
        }
    }

    private fun feedInput(codec: MediaCodec, extractor: MediaExtractor): Boolean {
        val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
        if (inputIndex < 0) return false
        val buffer = codec.getInputBuffer(inputIndex)!!
        val size = extractor.readSampleData(buffer, 0)
        if (size < 0) {
            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            return true
        }
        codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
        extractor.advance()
        return false
    }

    private companion object {
        const val TIMEOUT_US = 10_000L
        const val HALF_FRAME_US = 18_000L
    }
}

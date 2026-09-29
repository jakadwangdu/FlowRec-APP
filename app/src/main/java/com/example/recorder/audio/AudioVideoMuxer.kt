package com.example.recorder.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * Muxes video track and audio track into a single finalized MP4 container
 * without re-encoding video frames, ensuring zero quality loss and blazing speed.
 */
object AudioVideoMuxer {

    private const val TAG = "AudioVideoMuxer"
    private const val DEFAULT_BUFFER_SIZE = 1024 * 1024 // 1 MB

    fun mux(videoFile: File, audioFile: File, outputFile: File): Boolean {
        if (!videoFile.exists() || videoFile.length() == 0L) {
            Log.e(TAG, "Video file is missing or empty: ${videoFile.absolutePath}")
            return false
        }

        if (!audioFile.exists() || audioFile.length() == 0L) {
            Log.w(TAG, "Audio file is missing or empty, copying video directly: ${audioFile.absolutePath}")
            return try {
                if (outputFile.exists()) outputFile.delete()
                videoFile.copyTo(outputFile, overwrite = true)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy video to output: ${e.message}")
                false
            }
        }

        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        val tempOut = File(outputFile.parentFile, "mux_tmp_${System.currentTimeMillis()}.mp4")

        try {
            videoExtractor.setDataSource(videoFile.absolutePath)
            audioExtractor.setDataSource(audioFile.absolutePath)

            var videoTrackIndex = -1
            var videoMuxerTrackIndex = -1
            var audioTrackIndex = -1
            var audioMuxerTrackIndex = -1

            // 1. Locate video track
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    break
                }
            }

            // 2. Locate audio track
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (videoTrackIndex < 0) {
                Log.e(TAG, "No video track found in ${videoFile.name}")
                return false
            }

            muxer = MediaMuxer(tempOut.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Add video track to muxer
            videoExtractor.selectTrack(videoTrackIndex)
            val videoFormat = videoExtractor.getTrackFormat(videoTrackIndex)
            videoMuxerTrackIndex = muxer.addTrack(videoFormat)

            // Add audio track to muxer if present
            if (audioTrackIndex >= 0) {
                audioExtractor.selectTrack(audioTrackIndex)
                val audioFormat = audioExtractor.getTrackFormat(audioTrackIndex)
                audioMuxerTrackIndex = muxer.addTrack(audioFormat)
            }

            muxer.start()

            val buffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            // 3. Pump video samples
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }
                bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                bufferInfo.flags = videoExtractor.sampleFlags
                muxer.writeSampleData(videoMuxerTrackIndex, buffer, bufferInfo)
                videoExtractor.advance()
            }

            // 4. Pump audio samples
            if (audioTrackIndex >= 0 && audioMuxerTrackIndex >= 0) {
                while (true) {
                    bufferInfo.offset = 0
                    bufferInfo.size = audioExtractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) {
                        break
                    }
                    bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                    bufferInfo.flags = audioExtractor.sampleFlags
                    muxer.writeSampleData(audioMuxerTrackIndex, buffer, bufferInfo)
                    audioExtractor.advance()
                }
            }

            muxer.stop()
            muxer.release()
            muxer = null

            // Safely swap to target output file
            if (outputFile.exists()) {
                outputFile.delete()
            }
            tempOut.renameTo(outputFile)

            // Cleanup intermediate files
            try { videoFile.delete() } catch (_: Exception) {}
            try { audioFile.delete() } catch (_: Exception) {}

            Log.i(TAG, "Muxing finished successfully: ${outputFile.name} (${outputFile.length()} bytes)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Muxing error: ${e.message}", e)
            try { tempOut.delete() } catch (_: Exception) {}
            return false
        } finally {
            try { videoExtractor.release() } catch (_: Exception) {}
            try { audioExtractor.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }
}

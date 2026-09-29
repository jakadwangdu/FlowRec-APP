package com.example.editor.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.editor.model.EditorProjectState
import com.example.editor.timeline.TimelineManager
import com.example.recorder.touch.FlowTouchEvent
import com.example.util.GalleryExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hardware-accelerated video rendering and export engine.
 * Renders non-destructive cuts, trims, zoom keyframes, text overlays, logo watermarks,
 * touch effect ripples, and FaceCam into a pristine final MP4 file.
 */
class ExportEngine(
    private val context: Context
) {
    companion object {
        private const val TAG = "ExportEngine"
        private const val TIMEOUT_USEC = 10_000L
        private const val AUDIO_SAMPLE_RATE = 44100
        private const val AUDIO_CHANNEL_COUNT = 2
        private const val AUDIO_BITRATE = 128_000
    }

    private val isCancelled = AtomicBoolean(false)
    private var activeMuxer: MediaMuxer? = null
    private var tempOutputFile: File? = null

    /**
     * Cancels any currently running export job and removes incomplete artifacts.
     */
    fun cancel() {
        Log.i(TAG, "Export cancellation requested")
        isCancelled.set(true)
    }

    /**
     * Executes the video rendering pipeline.
     */
    suspend fun exportVideo(
        sourceVideoFile: File,
        editorState: EditorProjectState,
        touchEvents: List<FlowTouchEvent>,
        config: ExportConfig,
        onProgress: (ExportProgress) -> Unit
    ): ExportResult = withContext(Dispatchers.IO) {
        isCancelled.set(false)
        val startTime = System.currentTimeMillis()

        onProgress(
            ExportProgress(
                state = ExportState.PREPARING,
                progressPercent = 0f,
                currentStepMessage = "Validating timeline and media source..."
            )
        )

        // 1. Validation
        if (!sourceVideoFile.exists() || sourceVideoFile.length() == 0L) {
            val err = "Source video file not found or is empty: ${sourceVideoFile.absolutePath}"
            Log.e(TAG, err)
            onProgress(ExportProgress(state = ExportState.FAILED, errorMessage = err))
            throw IllegalArgumentException(err)
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(sourceVideoFile.absolutePath)
        } catch (e: Exception) {
            val err = "Failed to inspect source video metadata: ${e.message}"
            Log.e(TAG, err, e)
            onProgress(ExportProgress(state = ExportState.FAILED, errorMessage = err))
            throw IllegalStateException(err, e)
        }

        val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1080
        val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1920
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        retriever.release()

        val isRotated = rotation == 90 || rotation == 270
        val sourceWidth = if (isRotated) rawHeight else rawWidth
        val sourceHeight = if (isRotated) rawWidth else rawHeight

        // 2. Output Dimensions & Encoding Target
        val (outWidth, outHeight) = config.resolution.calculateDimensions(sourceWidth, sourceHeight)
        val outFps = config.fps.fps
        val outBitrate = config.quality.calculateBitrate(outWidth, outHeight, outFps)

        val activeSegments = editorState.segments.filter { !it.isDeleted }
        val totalEffectiveDurationMs = editorState.effectiveDurationMs.coerceAtLeast(1000L)

        Log.i(TAG, "Export initialized: ${outWidth}x${outHeight} @ ${outFps}fps, bitrate: $outBitrate bps, duration: ${totalEffectiveDurationMs}ms")

        // 3. Prepare temporary working directory & file and validate storage
        val tempDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }

        // Storage pre-flight check: ensure sufficient free space for the rendered export
        val estimatedBytes = ((outBitrate.toLong() + AUDIO_BITRATE.toLong()) * (totalEffectiveDurationMs / 1000L) / 8L) * 12L / 10L
        val usableSpace = tempDir.usableSpace
        if (usableSpace in 1 until (estimatedBytes + 50L * 1024L * 1024L)) {
            val neededMb = (estimatedBytes + 50L * 1024L * 1024L) / (1024L * 1024L)
            val availMb = usableSpace / (1024L * 1024L)
            val err = "Insufficient storage space for export. Need ~$neededMb MB, but only $availMb MB available."
            Log.e(TAG, err)
            onProgress(ExportProgress(state = ExportState.FAILED, errorMessage = err))
            throw IllegalStateException(err)
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val defaultName = "FlowRec_$timestamp.mp4"
        val fileName = config.outputFileName?.let {
            if (it.endsWith(".mp4", ignoreCase = true)) it else "$it.mp4"
        } ?: defaultName
        val tempOutput = File(tempDir, "temp_$fileName")
        tempOutputFile = tempOutput
        if (tempOutput.exists()) tempOutput.delete()

        val overlayRenderer = FrameOverlayRenderer(
            context = context,
            editorState = editorState,
            touchEvents = touchEvents,
            config = config,
            outputWidth = outWidth,
            outputHeight = outHeight
        )

        var muxer: MediaMuxer? = null
        try {
            muxer = MediaMuxer(tempOutput.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            activeMuxer = muxer

            onProgress(
                ExportProgress(
                    state = ExportState.EXPORTING,
                    progressPercent = 5f,
                    currentStepMessage = "Rendering video frames and compositing overlays...",
                    totalDurationMs = totalEffectiveDurationMs
                )
            )

            // Execute hardware transcoding and overlay compositing
            renderVideoAndAudio(
                sourceVideoFile = sourceVideoFile,
                editorState = editorState,
                activeSegments = activeSegments,
                overlayRenderer = overlayRenderer,
                config = config,
                muxer = muxer,
                outWidth = outWidth,
                outHeight = outHeight,
                outFps = outFps,
                outBitrate = outBitrate,
                totalEffectiveDurationMs = totalEffectiveDurationMs,
                startTime = startTime,
                onProgress = onProgress
            )

            if (isCancelled.get()) {
                throw CancellationException("Export was cancelled by user")
            }

            // 4. Finalize Muxer
            onProgress(
                ExportProgress(
                    state = ExportState.FINALIZING,
                    progressPercent = 95f,
                    currentStepMessage = "Saving video to Gallery (Movies/FlowRec)...",
                    totalDurationMs = totalEffectiveDurationMs
                )
            )

            muxer.stop()
            muxer.release()
            muxer = null
            activeMuxer = null

            // 5. Save to Android Public MediaStore
            val savedUri = GalleryExporter.saveVideoToGallery(
                context = context,
                sourceVideoFile = tempOutput,
                displayName = fileName
            )

            val finalFile = File(tempDir, fileName)
            tempOutput.renameTo(finalFile)

            val result = ExportResult(
                outputFile = finalFile,
                contentUri = savedUri,
                durationMs = totalEffectiveDurationMs,
                fileSizeBytes = finalFile.length(),
                width = outWidth,
                height = outHeight,
                fps = outFps
            )

            Log.i(TAG, "Export completed successfully: ${finalFile.absolutePath}, uri: $savedUri, size: ${result.fileSizeBytes} bytes")

            onProgress(
                ExportProgress(
                    state = ExportState.COMPLETED,
                    progressPercent = 100f,
                    currentStepMessage = "Export complete! Saved to Gallery.",
                    elapsedTimeMs = System.currentTimeMillis() - startTime,
                    renderedDurationMs = totalEffectiveDurationMs,
                    totalDurationMs = totalEffectiveDurationMs
                )
            )

            return@withContext result
        } catch (e: CancellationException) {
            Log.w(TAG, "Export cancelled: ${e.message}")
            cleanup(muxer, tempOutput)
            onProgress(ExportProgress(state = ExportState.CANCELLED, currentStepMessage = "Export cancelled"))
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Export failed: ${e.message}", e)
            cleanup(muxer, tempOutput)
            onProgress(ExportProgress(state = ExportState.FAILED, errorMessage = e.message ?: "Unknown export failure"))
            throw e
        } finally {
            overlayRenderer.release()
            activeMuxer = null
        }
    }

    private fun renderVideoAndAudio(
        sourceVideoFile: File,
        editorState: EditorProjectState,
        activeSegments: List<com.example.editor.model.TimelineSegment>,
        overlayRenderer: FrameOverlayRenderer,
        config: ExportConfig,
        muxer: MediaMuxer,
        outWidth: Int,
        outHeight: Int,
        outFps: Int,
        outBitrate: Int,
        totalEffectiveDurationMs: Long,
        startTime: Long,
        onProgress: (ExportProgress) -> Unit
    ) {
        val extractor = MediaExtractor()
        extractor.setDataSource(sourceVideoFile.absolutePath)

        var videoTrackIndex = -1
        var audioTrackIndex = -1
        var videoFormat: MediaFormat? = null
        var audioFormat: MediaFormat? = null

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/") && videoTrackIndex == -1) {
                videoTrackIndex = i
                videoFormat = format
            } else if (mime.startsWith("audio/") && audioTrackIndex == -1) {
                audioTrackIndex = i
                audioFormat = format
            }
        }

        if (videoTrackIndex == -1 || videoFormat == null) {
            extractor.release()
            throw IllegalStateException("No video track found in source file")
        }

        // Setup Output Video Format
        val outputVideoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, outWidth, outHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, outBitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, outFps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 second keyframe interval
        }

        // Configure Video Encoder
        val videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        videoEncoder.configure(outputVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        videoEncoder.start()

        // Configure Video Decoder
        extractor.selectTrack(videoTrackIndex)
        val decoderMime = videoFormat.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_VIDEO_AVC
        val videoDecoder = MediaCodec.createDecoderByType(decoderMime)
        videoDecoder.configure(videoFormat, null, null, 0)
        videoDecoder.start()

        var muxerVideoTrack = -1
        var muxerAudioTrack = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        var currentTimelinePositionMs = 0L
        var totalInputFramesProcessed = 0
        var totalOutputFramesEncoded = 0

        // Software compositing canvas and bitmap buffers
        val frameBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val frameCanvas = Canvas(frameBitmap)
        val bgPaint = Paint().apply { color = Color.BLACK }

        try {
            for (segment in activeSegments) {
                if (isCancelled.get()) break

                val segStartUs = segment.sourceStartMs * 1000L
                val segEndUs = segment.sourceEndMs * 1000L
                extractor.seekTo(segStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                var segmentDone = false
                while (!segmentDone && !isCancelled.get()) {
                    // Feed Decoder
                    val inIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_USEC)
                    if (inIndex >= 0) {
                        val inputBuffer = videoDecoder.getInputBuffer(inIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            val sampleTimeUs = extractor.sampleTime

                            if (sampleSize < 0 || sampleTimeUs > segEndUs) {
                                segmentDone = true
                            } else if (sampleTimeUs >= segStartUs) {
                                videoDecoder.queueInputBuffer(inIndex, 0, sampleSize, sampleTimeUs, 0)
                                extractor.advance()
                            } else {
                                extractor.advance()
                            }
                        }
                    }

                    // Drain Decoder
                    var outIndex = videoDecoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                    while (outIndex >= 0) {
                        val presentationTimeUs = bufferInfo.presentationTimeUs
                        if (presentationTimeUs in segStartUs..segEndUs) {
                            val segOffsetMs = ((presentationTimeUs - segStartUs) / 1000L).coerceAtLeast(0L)
                            val effectiveSegOffsetMs = (segOffsetMs.toFloat() / segment.speed.coerceAtLeast(0.1f)).toLong()
                            val frameTimelineMs = currentTimelinePositionMs + effectiveSegOffsetMs

                            // Clear Canvas
                            frameCanvas.drawRect(0f, 0f, outWidth.toFloat(), outHeight.toFloat(), bgPaint)

                            // Apply Zoom Matrix & Overlays
                            frameCanvas.save()
                            val zoomMatrix = overlayRenderer.calculateZoomMatrix(frameTimelineMs)
                            frameCanvas.concat(zoomMatrix)

                            // Render text, image, touch ripples, and FaceCam overlays
                            overlayRenderer.renderOverlays(frameCanvas, frameTimelineMs)
                            frameCanvas.restore()

                            // Feed frame to Encoder
                            val encInIndex = videoEncoder.dequeueInputBuffer(TIMEOUT_USEC)
                            if (encInIndex >= 0) {
                                val encBuffer = videoEncoder.getInputBuffer(encInIndex)
                                if (encBuffer != null) {
                                    encBuffer.clear()
                                    // Convert ARGB to YUV420
                                    encodeBitmapToYuv(frameBitmap, encBuffer, outWidth, outHeight)
                                    val ptsUs = (totalOutputFramesEncoded * 1_000_000L / outFps)
                                    videoEncoder.queueInputBuffer(encInIndex, 0, encBuffer.position(), ptsUs, 0)
                                    totalOutputFramesEncoded++
                                }
                            }

                            totalInputFramesProcessed++

                            // Update progress
                            val progressPct = ((frameTimelineMs.toFloat() / totalEffectiveDurationMs.toFloat()) * 90f).coerceIn(5f, 90f)
                            val elapsed = System.currentTimeMillis() - startTime
                            val estimatedTotal = if (progressPct > 5f) (elapsed / (progressPct / 100f)).toLong() else 0L
                            val remaining = (estimatedTotal - elapsed).coerceAtLeast(0L)

                            onProgress(
                                ExportProgress(
                                    state = ExportState.EXPORTING,
                                    progressPercent = progressPct,
                                    currentStepMessage = "Rendering frame #$totalOutputFramesEncoded (${(frameTimelineMs / 1000)}s / ${(totalEffectiveDurationMs / 1000)}s)...",
                                    elapsedTimeMs = elapsed,
                                    estimatedRemainingTimeMs = remaining,
                                    renderedDurationMs = frameTimelineMs,
                                    totalDurationMs = totalEffectiveDurationMs
                                )
                            )
                        }

                        videoDecoder.releaseOutputBuffer(outIndex, false)
                        outIndex = videoDecoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)

                        // Drain Encoder Outputs
                        var encOutIndex = videoEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                        while (encOutIndex >= 0) {
                            val encodedData = videoEncoder.getOutputBuffer(encOutIndex)
                            if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                if (!muxerStarted) {
                                    val newFormat = videoEncoder.outputFormat
                                    muxerVideoTrack = muxer.addTrack(newFormat)
                                    if (audioTrackIndex != -1 && audioFormat != null && config.includeOriginalAudio && !editorState.audioConfig.originalAudioMuted) {
                                        muxerAudioTrack = muxer.addTrack(audioFormat)
                                    }
                                    muxer.start()
                                    muxerStarted = true
                                    Log.d(TAG, "MediaMuxer started with video track: $muxerVideoTrack, audio track: $muxerAudioTrack")
                                }
                                if (muxerStarted && bufferInfo.size > 0) {
                                    muxer.writeSampleData(muxerVideoTrack, encodedData, bufferInfo)
                                }
                            }
                            videoEncoder.releaseOutputBuffer(encOutIndex, false)
                            encOutIndex = videoEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                        }
                    }
                }

                currentTimelinePositionMs += segment.durationMs
            }

            // Signal End of Stream to Video Encoder
            val eosInIndex = videoEncoder.dequeueInputBuffer(TIMEOUT_USEC)
            if (eosInIndex >= 0) {
                videoEncoder.queueInputBuffer(eosInIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }

            // Drain remaining frames from encoder
            var encOutIndex = videoEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            while (encOutIndex >= 0) {
                val encodedData = videoEncoder.getOutputBuffer(encOutIndex)
                if (encodedData != null && muxerStarted && bufferInfo.size > 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    muxer.writeSampleData(muxerVideoTrack, encodedData, bufferInfo)
                }
                videoEncoder.releaseOutputBuffer(encOutIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                encOutIndex = videoEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
            }

            // Mux audio track for active segments
            if (muxerStarted && muxerAudioTrack != -1) {
                val audioExtractor = MediaExtractor()
                try {
                    audioExtractor.setDataSource(sourceVideoFile.absolutePath)
                    audioExtractor.selectTrack(audioTrackIndex)
                    val audioBuffer = ByteBuffer.allocateDirect(128 * 1024)
                    val audioBufferInfo = MediaCodec.BufferInfo()
                    var audioPtsOffsetUs = 0L

                    for (segment in activeSegments) {
                        if (isCancelled.get()) break
                        val segStartUs = segment.sourceStartMs * 1000L
                        val segEndUs = segment.sourceEndMs * 1000L
                        audioExtractor.seekTo(segStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                        while (!isCancelled.get()) {
                            val sampleSize = audioExtractor.readSampleData(audioBuffer, 0)
                            val sampleTimeUs = audioExtractor.sampleTime

                            if (sampleSize < 0 || sampleTimeUs > segEndUs) break
                            if (sampleTimeUs >= segStartUs) {
                                val relativeTimeUs = sampleTimeUs - segStartUs
                                val outputPtsUs = audioPtsOffsetUs + (relativeTimeUs.toFloat() / segment.speed.coerceAtLeast(0.1f)).toLong()
                                audioBufferInfo.offset = 0
                                audioBufferInfo.size = sampleSize
                                audioBufferInfo.presentationTimeUs = outputPtsUs
                                audioBufferInfo.flags = audioExtractor.sampleFlags
                                muxer.writeSampleData(muxerAudioTrack, audioBuffer, audioBufferInfo)
                                audioExtractor.advance()
                            } else {
                                audioExtractor.advance()
                            }
                        }
                        audioPtsOffsetUs += segment.durationMs * 1000L
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Audio muxing warning (non-fatal): ${e.message}")
                } finally {
                    audioExtractor.release()
                }
            }
        } finally {
            frameBitmap.recycle()
            try {
                videoDecoder.stop()
                videoDecoder.release()
            } catch (_: Exception) {}
            try {
                videoEncoder.stop()
                videoEncoder.release()
            } catch (_: Exception) {}
            extractor.release()
        }
    }

    /**
     * Converts an ARGB Bitmap into YUV420SemiPlanar / NV12 format for the video encoder.
     */
    private fun encodeBitmapToYuv(bitmap: Bitmap, outputBuffer: ByteBuffer, width: Int, height: Int) {
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)

        val ySize = width * height
        val uvSize = width * height / 2

        val yuvBytes = ByteArray(ySize + uvSize)
        var yIndex = 0
        var uvIndex = ySize

        for (j in 0 until height) {
            for (i in 0 until width) {
                val pixel = argb[j * width + i]
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = pixel and 0xff

                // RGB to YUV formula
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yuvBytes[yIndex++] = y.coerceIn(0, 255).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    yuvBytes[uvIndex++] = u.coerceIn(0, 255).toByte()
                    yuvBytes[uvIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }

        outputBuffer.put(yuvBytes)
    }

    private fun cleanup(muxer: MediaMuxer?, tempFile: File?) {
        try {
            muxer?.stop()
        } catch (_: Exception) {}
        try {
            muxer?.release()
        } catch (_: Exception) {}
        try {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete()
                Log.d(TAG, "Cleaned up incomplete temporary export file: ${tempFile.absolutePath}")
            }
        } catch (_: Exception) {}
    }
}

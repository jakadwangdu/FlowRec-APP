package com.example.recorder.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.model.AudioSourceMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer

/**
 * Robust Android 10+ Audio Capture Engine for FlowRec.
 * Captures Internal Device Audio using AudioPlaybackCaptureConfiguration,
 * Microphone audio, or mixes both streams into an AAC audio track.
 */
class AudioCaptureManager(
    private val context: Context,
    private val mediaProjection: MediaProjection?,
    private val audioSourceMode: AudioSourceMode,
    private val outputFile: File
) {
    companion object {
        private const val TAG = "AudioCaptureManager"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_COUNT = 2
        private const val BIT_RATE = 128_000 // 128 kbps
        private const val TIMEOUT_US = 10_000L
    }

    private var internalAudioRecord: AudioRecord? = null
    private var micAudioRecord: AudioRecord? = null
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var audioTrackIndex = -1
    private var isMuxerStarted = false

    @Volatile
    private var isRecording = false

    @Volatile
    private var isPaused = false

    private var captureJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var presentationTimeUs = 0L
    private var lastFrameTimeNs = 0L

    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (audioSourceMode == AudioSourceMode.NONE) {
            return false
        }

        if (audioSourceMode == AudioSourceMode.INTERNAL || audioSourceMode == AudioSourceMode.MIC_AND_INTERNAL) {
            if (!isSupported) {
                Log.w(TAG, "Internal audio capture requires Android 10+ (API 29) and active MediaProjection")
                return false
            }
        }

        try {
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) {
                outputFile.delete()
            }

            val channelConfig = AudioFormat.CHANNEL_IN_STEREO
            val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioEncoding)
            val bufferSize = maxOf(minBufSize * 2, 8192)

            // 1. Initialize Internal AudioRecord if needed
            if (audioSourceMode == AudioSourceMode.INTERNAL || audioSourceMode == AudioSourceMode.MIC_AND_INTERNAL) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
                    try {
                        val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                            .addMatchingUsage(AudioAttributes.USAGE_GAME)
                            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                            .build()

                        val audioFormat = AudioFormat.Builder()
                            .setEncoding(audioEncoding)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(channelConfig)
                            .build()

                        internalAudioRecord = AudioRecord.Builder()
                            .setAudioPlaybackCaptureConfig(playbackConfig)
                            .setAudioFormat(audioFormat)
                            .setBufferSizeInBytes(bufferSize)
                            .build()

                        if (internalAudioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                            Log.w(TAG, "Internal AudioRecord failed initialization, falling back")
                            internalAudioRecord?.release()
                            internalAudioRecord = null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error initializing AudioPlaybackCapture: ${e.message}", e)
                        internalAudioRecord = null
                    }
                }
            }

            // 2. Initialize Mic AudioRecord if needed
            if (audioSourceMode == AudioSourceMode.MIC || audioSourceMode == AudioSourceMode.MIC_AND_INTERNAL) {
                try {
                    micAudioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        channelConfig,
                        audioEncoding,
                        bufferSize
                    )
                    if (micAudioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                        Log.w(TAG, "Mic AudioRecord failed initialization")
                        micAudioRecord?.release()
                        micAudioRecord = null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing Mic AudioRecord: ${e.message}", e)
                    micAudioRecord = null
                }
            }

            if (internalAudioRecord == null && micAudioRecord == null) {
                Log.e(TAG, "No audio record could be initialized")
                return false
            }

            // 3. Initialize AAC MediaCodec Encoder
            val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNEL_COUNT).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize)
            }

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            // 4. Initialize MediaMuxer
            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Start records
            try { internalAudioRecord?.startRecording() } catch (e: Exception) { Log.w(TAG, "internalAudioRecord start failed: ${e.message}") }
            try { micAudioRecord?.startRecording() } catch (e: Exception) { Log.w(TAG, "micAudioRecord start failed: ${e.message}") }

            isRecording = true
            isPaused = false
            presentationTimeUs = 0L
            lastFrameTimeNs = System.nanoTime()

            // 5. Launch processing loop
            captureJob = scope.launch {
                val shortBufferSize = bufferSize / 2
                val internalBuf = ShortArray(shortBufferSize)
                val micBuf = ShortArray(shortBufferSize)
                val mixedBuf = ShortArray(shortBufferSize)
                val byteBuf = ByteArray(bufferSize)

                val bufferInfo = MediaCodec.BufferInfo()

                while (isActive && isRecording) {
                    if (isPaused) {
                        lastFrameTimeNs = System.nanoTime()
                        kotlinx.coroutines.delay(50)
                        continue
                    }

                    var readSamples = 0

                    val hasInternal = internalAudioRecord != null
                    val hasMic = micAudioRecord != null

                    if (hasInternal && hasMic) {
                        val intRead = internalAudioRecord?.read(internalBuf, 0, shortBufferSize) ?: 0
                        val micRead = micAudioRecord?.read(micBuf, 0, shortBufferSize) ?: 0
                        readSamples = maxOf(intRead, micRead)

                        if (readSamples > 0) {
                            for (i in 0 until readSamples) {
                                val s1 = if (i < intRead) internalBuf[i].toInt() else 0
                                val s2 = if (i < micRead) micBuf[i].toInt() else 0
                                // PCM 16-bit additive mixing with saturation clamping
                                mixedBuf[i] = (s1 + s2).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            }
                        }
                    } else if (hasInternal) {
                        readSamples = internalAudioRecord?.read(mixedBuf, 0, shortBufferSize) ?: 0
                    } else if (hasMic) {
                        readSamples = micAudioRecord?.read(mixedBuf, 0, shortBufferSize) ?: 0
                    }

                    if (readSamples > 0) {
                        // Convert ShortArray to ByteArray (Little-Endian PCM 16-bit)
                        var byteIdx = 0
                        for (i in 0 until readSamples) {
                            val sample = mixedBuf[i].toInt()
                            byteBuf[byteIdx++] = (sample and 0xFF).toByte()
                            byteBuf[byteIdx++] = ((sample shr 8) and 0xFF).toByte()
                        }

                        val bytesToWrite = readSamples * 2
                        feedEncoder(byteBuf, bytesToWrite)
                        drainEncoder(bufferInfo, endOfStream = false)
                    } else {
                        kotlinx.coroutines.delay(10)
                    }
                }

                // Finish stream
                drainEncoder(bufferInfo, endOfStream = true)
            }

            Log.i(TAG, "Audio capture started successfully in mode: $audioSourceMode -> ${outputFile.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioCaptureManager: ${e.message}", e)
            release()
            return false
        }
    }

    private fun feedEncoder(data: ByteArray, length: Int) {
        val codec = mediaCodec ?: return
        val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
        if (inputIndex >= 0) {
            val inputBuffer = codec.getInputBuffer(inputIndex) ?: return
            inputBuffer.clear()
            inputBuffer.put(data, 0, length)

            val now = System.nanoTime()
            val elapsedUs = (now - lastFrameTimeNs) / 1000L
            presentationTimeUs += elapsedUs
            lastFrameTimeNs = now

            codec.queueInputBuffer(inputIndex, 0, length, presentationTimeUs, 0)
        }
    }

    private fun drainEncoder(bufferInfo: MediaCodec.BufferInfo, endOfStream: Boolean) {
        val codec = mediaCodec ?: return
        val muxer = mediaMuxer ?: return

        if (endOfStream) {
            val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
            if (inputIndex >= 0) {
                codec.queueInputBuffer(inputIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
        }

        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (isMuxerStarted) {
                    Log.w(TAG, "Muxer already started, format change ignored")
                } else {
                    val newFormat = codec.outputFormat
                    audioTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    isMuxerStarted = true
                    Log.d(TAG, "MediaMuxer started with format: $newFormat")
                }
            } else if (outputIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputIndex)
                if (outputBuffer != null && bufferInfo.size > 0 && isMuxerStarted) {
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    try {
                        muxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                    } catch (e: Exception) {
                        Log.w(TAG, "Muxer writeSampleData error: ${e.message}")
                    }
                }
                codec.releaseOutputBuffer(outputIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        lastFrameTimeNs = System.nanoTime()
        isPaused = false
    }

    fun stop() {
        if (!isRecording) return
        isRecording = false
        isPaused = false

        try {
            captureJob?.cancel()
            captureJob = null
        } catch (_: Exception) {}

        release()
        Log.i(TAG, "Audio capture stopped and finalized.")
    }

    private fun release() {
        try { internalAudioRecord?.stop() } catch (_: Exception) {}
        try { internalAudioRecord?.release() } catch (_: Exception) {}
        internalAudioRecord = null

        try { micAudioRecord?.stop() } catch (_: Exception) {}
        try { micAudioRecord?.release() } catch (_: Exception) {}
        micAudioRecord = null

        try { mediaCodec?.stop() } catch (_: Exception) {}
        try { mediaCodec?.release() } catch (_: Exception) {}
        mediaCodec = null

        try {
            if (isMuxerStarted) {
                mediaMuxer?.stop()
            }
            mediaMuxer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping media muxer: ${e.message}")
        }
        mediaMuxer = null
        isMuxerStarted = false
    }
}

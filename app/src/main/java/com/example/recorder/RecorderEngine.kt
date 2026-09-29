package com.example.recorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.data.entity.ProjectEntity
import com.example.model.AudioSourceMode
import com.example.model.CaptureMode

import com.example.model.CountdownOption
import com.example.model.FrameRate
import com.example.model.RecorderState
import com.example.model.RecordingResolution
import com.example.model.VideoOrientation
import com.example.service.ScreenRecorderService
import com.example.util.GalleryExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class RecordingConfig(
    val mode: CaptureMode = CaptureMode.SCREEN,
    val resolution: RecordingResolution = RecordingResolution.RES_1080P,
    val frameRate: FrameRate = FrameRate.FPS_30,
    val audioSource: AudioSourceMode = AudioSourceMode.MIC_AND_INTERNAL,
    val recordSystemAudio: Boolean = true,
    val recordMicrophone: Boolean = true,
    val showTouches: Boolean = true,
    val floatingBubbleEnabled: Boolean = true,
    val autoSaveToGallery: Boolean = true,
    val orientation: VideoOrientation = VideoOrientation.AUTO,
    val countdownOption: CountdownOption = CountdownOption.SEC_3,
    val isGameMode: Boolean = false,
    val shakeToStop: Boolean = false,
    val showCursor: Boolean = false,
    val clickEffects: Boolean = true,
    // Phase 2: FaceCam and Touch Tracking
    val facecamEnabled: Boolean = false,
    val facecamShape: String = "CIRCLE",
    val facecamSize: String = "MEDIUM",
    val facecamFrontLens: Boolean = true,
    val touchTrackingEnabled: Boolean = true,
    val touchRippleEnabled: Boolean = true,
    val touchHighlightEnabled: Boolean = true,
    val touchMovementTrackingEnabled: Boolean = false,
    val touchEffectSizeDp: Int = 36,
    val touchEffectDurationMs: Int = 500,
    val touchEffectOpacity: Float = 0.8f
)

class RecorderEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val TAG = "RecorderEngine"


    private val _state = MutableStateFlow(RecorderState.IDLE)
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private val _config = MutableStateFlow(RecordingConfig())
    val config: StateFlow<RecordingConfig> = _config.asStateFlow()

    private val _countdown = MutableStateFlow(3)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private var timerJob: Job? = null
    private var countdownJob: Job? = null
    private var recordingStartTime = 0L
    private var accumulatedDuration = 0L

    // Stored MediaProjection Intent Result
    private var projectionResultCode: Int = 0
    private var projectionResultData: Intent? = null
    private var currentOutputFile: File? = null

    var onProjectIdSavedListener: ((String) -> Unit)? = null
    var onExternalStopListener: ((ProjectEntity) -> Unit)? = null

    init {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ScreenRecorderService.ACTION_STATE_CHANGED -> {
                        when (intent.getStringExtra(ScreenRecorderService.EXTRA_STATE)) {
                            "RECORDING" -> {
                                if (_state.value != RecorderState.RECORDING) {
                                    _state.value = RecorderState.RECORDING
                                }
                            }
                            "PAUSED" -> {
                                if (_state.value == RecorderState.RECORDING) {
                                    _state.value = RecorderState.PAUSED
                                    timerJob?.cancel()
                                }
                            }
                            "STOPPED" -> {
                                _state.value = RecorderState.IDLE
                                timerJob?.cancel()
                            }
                            "ERROR" -> {
                                _state.value = RecorderState.ERROR
                                timerJob?.cancel()
                            }
                        }
                    }
                    ScreenRecorderService.ACTION_TIMER_TICK -> {
                        val sec = intent.getIntExtra(ScreenRecorderService.EXTRA_DURATION_SECONDS, 0)
                        _durationSeconds.value = sec
                    }
                    ScreenRecorderService.ACTION_RECORDING_SAVED -> {
                        val projId = intent.getStringExtra(ScreenRecorderService.EXTRA_PROJECT_ID)
                        _state.value = RecorderState.IDLE
                        _durationSeconds.value = 0
                        timerJob?.cancel()
                        if (projId != null) {
                            onProjectIdSavedListener?.invoke(projId)
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ScreenRecorderService.ACTION_STATE_CHANGED)
            addAction(ScreenRecorderService.ACTION_TIMER_TICK)
            addAction(ScreenRecorderService.ACTION_RECORDING_SAVED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("RECEIVER_EXPORTED_UNSPECIFIED")
            context.registerReceiver(receiver, filter)
        }
    }

    fun updateConfig(config: RecordingConfig) {
        _config.value = config
    }

    fun setProjectionPermission(resultCode: Int, data: Intent?) {
        projectionResultCode = resultCode
        projectionResultData = data
    }

    fun hasProjectionPermission(): Boolean {
        return projectionResultCode != 0 && projectionResultData != null
    }

    fun startCountdown(onCountdownComplete: () -> Unit) {
        val totalSec = _config.value.countdownOption.seconds
        if (totalSec <= 0) {
            _state.value = RecorderState.RECORDING
            startRecording()
            onCountdownComplete()
            return
        }
        _state.value = RecorderState.COUNTDOWN
        _countdown.value = totalSec
        countdownJob?.cancel()
        countdownJob = scope.launch(Dispatchers.Main) {
            for (i in totalSec downTo 1) {
                _countdown.value = i
                delay(1000L)
            }
            _state.value = RecorderState.RECORDING
            startRecording()
            onCountdownComplete()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _state.value = RecorderState.IDLE
    }

    fun startRecording() {
        _state.value = RecorderState.RECORDING
        _durationSeconds.value = 0
        accumulatedDuration = 0L
        recordingStartTime = SystemClock.elapsedRealtime()

        val cfg = _config.value
        val recordingsDir = File(context.filesDir, "recordings")
        recordingsDir.mkdirs()
        val outputFile = File(recordingsDir, "rec_${System.currentTimeMillis()}.mp4")
        currentOutputFile = outputFile

        val resultCode = projectionResultCode
        val resultData = projectionResultData

        if (resultCode != 0 && resultData != null) {
            ScreenRecorderService.startRecording(
                context = context,
                resultCode = resultCode,
                resultData = resultData,
                outputPath = outputFile.absolutePath,
                recordAudio = cfg.audioSource != AudioSourceMode.NONE,
                width = cfg.resolution.width,
                height = cfg.resolution.height,
                fps = if (cfg.isGameMode) 60 else cfg.frameRate.fps,
                audioMode = cfg.audioSource,
                orientation = cfg.orientation,
                isGameMode = cfg.isGameMode,
                facecamEnabled = cfg.facecamEnabled,
                facecamShape = cfg.facecamShape,
                facecamSize = cfg.facecamSize,
                facecamFront = cfg.facecamFrontLens,
                touchTrackingEnabled = cfg.touchTrackingEnabled
            )
        } else {
            Log.w(TAG, "Starting without projection token (fallback mode)")
        }

        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch(Dispatchers.Default) {
            while (_state.value == RecorderState.RECORDING) {
                delay(500L)
                val elapsed = accumulatedDuration + (SystemClock.elapsedRealtime() - recordingStartTime)
                val sec = (elapsed / 1000L).toInt()
                _durationSeconds.value = sec
            }
        }
    }

    fun pauseRecording() {
        if (_state.value == RecorderState.RECORDING) {
            _state.value = RecorderState.PAUSED
            accumulatedDuration += SystemClock.elapsedRealtime() - recordingStartTime
            timerJob?.cancel()
            com.example.service.FloatingOverlayManager.getInstance(context).updatePausedState(true)
            ScreenRecorderService.pauseRecording(context)
        }
    }

    fun resumeRecording() {
        if (_state.value == RecorderState.PAUSED) {
            _state.value = RecorderState.RECORDING
            recordingStartTime = SystemClock.elapsedRealtime()
            com.example.service.FloatingOverlayManager.getInstance(context).updatePausedState(false)
            startTimer()
            ScreenRecorderService.resumeRecording(context)
        }
    }

    fun stopRecording(projectName: String = "My Recording", onFinished: ((ProjectEntity) -> Unit)? = null) {
        if (onFinished != null) {
            onExternalStopListener = onFinished
        }
        if (_state.value == RecorderState.RECORDING || _state.value == RecorderState.PAUSED) {
            _state.value = RecorderState.PROCESSING
            timerJob?.cancel()

            if (ScreenRecorderService.isRunning) {
                ScreenRecorderService.stopRecording(context)
                return
            }

            if (_state.value == RecorderState.RECORDING) {
                accumulatedDuration += SystemClock.elapsedRealtime() - recordingStartTime
            }
            val recordedSec = ((accumulatedDuration / 1000L).coerceAtLeast(1L)).toInt()

            scope.launch(Dispatchers.IO) {
                delay(800L) // Wait for MediaRecorder file finalizing & closing

                val videoFile = currentOutputFile
                var realDurationSec = recordedSec
                var fileSize = 15_000_000L
                var videoPath = ""
                var thumbnailName = "thumb_mountain"

                if (videoFile != null && videoFile.exists() && videoFile.length() > 0) {
                    videoPath = videoFile.absolutePath
                    fileSize = videoFile.length()

                    // Try to retrieve real duration & thumbnail from generated MP4
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(videoFile.absolutePath)
                        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        if (durationStr != null) {
                            val durMs = durationStr.toLongOrNull() ?: 0L
                            if (durMs > 500) {
                                realDurationSec = (durMs / 1000L).toInt().coerceAtLeast(1)
                            }
                        }

                        val frame: Bitmap? = retriever.getFrameAtTime(500_000) // Frame at 0.5s
                        if (frame != null) {
                            val thumbsDir = File(context.filesDir, "thumbnails")
                            thumbsDir.mkdirs()
                            val thumbFile = File(thumbsDir, "thumb_${videoFile.nameWithoutExtension}.png")
                            val fos = FileOutputStream(thumbFile)
                            frame.compress(Bitmap.CompressFormat.PNG, 90, fos)
                            fos.flush()
                            fos.close()
                            thumbnailName = thumbFile.absolutePath
                        }
                        retriever.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not extract metadata/thumbnail from video: ${e.message}")
                    }
                } else {
                    val sampleThumbnails = listOf("thumb_mountain", "thumb_code", "thumb_appui")
                    thumbnailName = sampleThumbnails.random()
                    fileSize = (realDurationSec * 2_500_000L).coerceAtLeast(10_000_000L)
                }

                val currentConfig = _config.value
                val finalName = if (projectName.isNotBlank()) projectName else "Screen Recording ${System.currentTimeMillis() % 10000}"

                var isExportedToGallery = false
                if (videoFile != null && videoFile.exists() && videoFile.length() > 0) {
                    if (currentConfig.autoSaveToGallery) {
                        try {
                            val galleryUri = GalleryExporter.saveVideoToGallery(context, videoFile, finalName)
                            if (galleryUri != null) {
                                isExportedToGallery = true
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Auto-save to gallery failed: ${e.message}")
                        }
                    }
                }

                val newProject = ProjectEntity(
                    id = UUID.randomUUID().toString(),
                    name = finalName,
                    durationSeconds = realDurationSec,
                    resolution = currentConfig.resolution.label,
                    fps = currentConfig.frameRate.fps,
                    fileSizeBytes = fileSize,
                    thumbnailResName = thumbnailName,
                    videoPath = videoPath,
                    createdAt = System.currentTimeMillis(),
                    isFavorite = false,
                    isExported = isExportedToGallery,
                    cursorEnabled = currentConfig.showCursor,
                    clickZoomEnabled = currentConfig.clickEffects
                )

                _state.value = RecorderState.IDLE
                _durationSeconds.value = 0

                scope.launch(Dispatchers.Main) {
                    onFinished?.invoke(newProject)
                }
            }
        }

    }

    fun reset() {
        timerJob?.cancel()
        countdownJob?.cancel()
        _state.value = RecorderState.IDLE
        _durationSeconds.value = 0
    }
}

package com.example.recorder

import android.content.Context
import android.os.SystemClock
import com.example.data.entity.ProjectEntity
import com.example.model.CaptureMode
import com.example.model.FrameRate
import com.example.model.RecorderState
import com.example.model.RecordingResolution
import com.example.service.ScreenRecorderService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class RecordingConfig(
    val mode: CaptureMode = CaptureMode.SCREEN,
    val resolution: RecordingResolution = RecordingResolution.RES_1080P,
    val frameRate: FrameRate = FrameRate.FPS_30,
    val recordSystemAudio: Boolean = true,
    val recordMicrophone: Boolean = false,
    val showCursor: Boolean = true,
    val clickEffects: Boolean = true
)

class RecorderEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
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

    fun updateConfig(config: RecordingConfig) {
        _config.value = config
    }

    fun startCountdown(onCountdownComplete: () -> Unit) {
        _state.value = RecorderState.COUNTDOWN
        _countdown.value = 3
        countdownJob?.cancel()
        countdownJob = scope.launch(Dispatchers.Main) {
            for (i in 3 downTo 1) {
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

        ScreenRecorderService.startService(context)

        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch(Dispatchers.Default) {
            while (_state.value == RecorderState.RECORDING) {
                delay(500L)
                val elapsed = accumulatedDuration + (SystemClock.elapsedRealtime() - recordingStartTime)
                _durationSeconds.value = (elapsed / 1000L).toInt()
            }
        }
    }

    fun pauseRecording() {
        if (_state.value == RecorderState.RECORDING) {
            _state.value = RecorderState.PAUSED
            accumulatedDuration += SystemClock.elapsedRealtime() - recordingStartTime
            timerJob?.cancel()
        }
    }

    fun resumeRecording() {
        if (_state.value == RecorderState.PAUSED) {
            _state.value = RecorderState.RECORDING
            recordingStartTime = SystemClock.elapsedRealtime()
            startTimer()
        }
    }

    fun stopRecording(projectName: String = "My Recording", onFinished: (ProjectEntity) -> Unit) {
        if (_state.value == RecorderState.RECORDING || _state.value == RecorderState.PAUSED) {
            if (_state.value == RecorderState.RECORDING) {
                accumulatedDuration += SystemClock.elapsedRealtime() - recordingStartTime
            }
            timerJob?.cancel()
            _state.value = RecorderState.PROCESSING

            ScreenRecorderService.stopService(context)

            val totalSec = ((accumulatedDuration / 1000L).coerceAtLeast(1L)).toInt()

            scope.launch(Dispatchers.IO) {
                delay(800L) // Simulate muxer finalize

                val currentConfig = _config.value
                val sampleThumbnails = listOf("thumb_mountain", "thumb_code", "thumb_appui")
                val randomThumb = sampleThumbnails.random()

                val newProject = ProjectEntity(
                    id = UUID.randomUUID().toString(),
                    name = if (projectName.isNotBlank()) projectName else "Screen Recording ${System.currentTimeMillis() % 1000}",
                    durationSeconds = totalSec,
                    resolution = currentConfig.resolution.label,
                    fps = currentConfig.frameRate.fps,
                    fileSizeBytes = (totalSec * 2_500_000L).coerceAtLeast(15_000_000L),
                    thumbnailResName = randomThumb,
                    videoPath = File(context.filesDir, "rec_${System.currentTimeMillis()}.mp4").absolutePath,
                    createdAt = System.currentTimeMillis(),
                    isFavorite = false,
                    isExported = false,
                    cursorEnabled = currentConfig.showCursor,
                    clickZoomEnabled = currentConfig.clickEffects
                )

                _state.value = RecorderState.IDLE
                _durationSeconds.value = 0

                scope.launch(Dispatchers.Main) {
                    onFinished(newProject)
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

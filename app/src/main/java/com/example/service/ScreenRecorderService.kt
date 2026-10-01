package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.FlowRecDatabase
import com.example.data.entity.ProjectEntity
import com.example.model.AudioSourceMode
import com.example.model.VideoOrientation
import com.example.recorder.audio.AudioCaptureManager
import com.example.recorder.audio.AudioVideoMuxer
import com.example.ui.components.formatSeconds
import com.example.util.GalleryExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.provider.Settings
import com.example.recorder.camera.FloatingFacecamManager
import com.example.recorder.touch.TouchTracker

class ScreenRecorderService : Service() {

    companion object {
        private const val TAG = "ScreenRecorderService"
        const val CHANNEL_ID = "flowrec_recording_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        const val ACTION_STATE_CHANGED = "com.example.service.ACTION_STATE_CHANGED"
        const val ACTION_RECORDING_SAVED = "com.example.service.ACTION_RECORDING_SAVED"
        const val ACTION_TIMER_TICK = "com.example.service.ACTION_TIMER_TICK"

        const val EXTRA_STATE = "extra_state" // "RECORDING", "PAUSED", "STOPPED", "ERROR"
        const val EXTRA_PROJECT_ID = "extra_project_id"
        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_OUTPUT_PATH = "extra_output_path"
        const val EXTRA_RECORD_AUDIO = "extra_record_audio"
        const val EXTRA_WIDTH = "extra_width"
        const val EXTRA_HEIGHT = "extra_height"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_AUDIO_MODE = "extra_audio_mode"
        const val EXTRA_ORIENTATION = "extra_orientation"
        const val EXTRA_IS_GAME_MODE = "extra_is_game_mode"
        const val EXTRA_FACECAM_ENABLED = "extra_facecam_enabled"
        const val EXTRA_FACECAM_SHAPE = "extra_facecam_shape"
        const val EXTRA_FACECAM_SIZE = "extra_facecam_size"
        const val EXTRA_FACECAM_FRONT = "extra_facecam_front"
        const val EXTRA_TOUCH_TRACKING_ENABLED = "extra_touch_tracking_enabled"

        var isRunning = false
            private set

        var isPaused = false
            private set

        var activeTouchTracker: TouchTracker? = null
            private set

        fun startRecording(
            context: Context,
            resultCode: Int,
            resultData: Intent,
            outputPath: String,
            recordAudio: Boolean,
            width: Int,
            height: Int,
            fps: Int,
            audioMode: AudioSourceMode = AudioSourceMode.MIC_AND_INTERNAL,
            orientation: VideoOrientation = VideoOrientation.AUTO,
            isGameMode: Boolean = false,
            facecamEnabled: Boolean = false,
            facecamShape: String = "CIRCLE",
            facecamSize: String = "MEDIUM",
            facecamFront: Boolean = true,
            touchTrackingEnabled: Boolean = true
        ) {
            val intent = Intent(context, ScreenRecorderService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
                putExtra(EXTRA_OUTPUT_PATH, outputPath)
                putExtra(EXTRA_RECORD_AUDIO, recordAudio)
                putExtra(EXTRA_WIDTH, width)
                putExtra(EXTRA_HEIGHT, height)
                putExtra(EXTRA_FPS, fps)
                putExtra(EXTRA_AUDIO_MODE, audioMode.name)
                putExtra(EXTRA_ORIENTATION, orientation.name)
                putExtra(EXTRA_IS_GAME_MODE, isGameMode)
                putExtra(EXTRA_FACECAM_ENABLED, facecamEnabled)
                putExtra(EXTRA_FACECAM_SHAPE, facecamShape)
                putExtra(EXTRA_FACECAM_SIZE, facecamSize)
                putExtra(EXTRA_FACECAM_FRONT, facecamFront)
                putExtra(EXTRA_TOUCH_TRACKING_ENABLED, touchTrackingEnabled)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseRecording(context: Context) {
            val intent = Intent(context, ScreenRecorderService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeRecording(context: Context) {
            val intent = Intent(context, ScreenRecorderService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, ScreenRecorderService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputPath: String? = null

    // Audio capture & muxing fields for Android 10+ internal/dual audio
    private var audioCaptureManager: AudioCaptureManager? = null
    private var tempRawVideoFile: File? = null
    private var tempRawAudioFile: File? = null
    private var isDualAudioMuxing = false

    private var floatingOverlayManager: FloatingOverlayManager? = null
    private var touchTracker: TouchTracker? = null
    private var floatingFacecamManager: FloatingFacecamManager? = null
    private var isFacecamActive = false
    private var lastFacecamShape = "CIRCLE"
    private var lastFacecamSize = "MEDIUM"
    private var lastFacecamFront = true
    private var lastTouchCompanionPath: String? = null

    private var recordingElapsedSeconds = 0
    private var serviceStartTime = 0L
    private var serviceAccumulatedTime = 0L
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRunning && !isPaused) {
                val elapsedMs = serviceAccumulatedTime + (android.os.SystemClock.elapsedRealtime() - serviceStartTime)
                val sec = (elapsedMs / 1000L).toInt()
                if (sec != recordingElapsedSeconds) {
                    recordingElapsedSeconds = sec
                    floatingOverlayManager?.updateDuration(sec)
                    val formatted = formatSeconds(sec)
                    updateNotification("Recording: $formatted", paused = false)
                    broadcastTimerTick(sec)
                }
            }
            if (isRunning) {
                timerHandler.postDelayed(this, 250)
            }
        }
    }

    private fun broadcastTimerTick(seconds: Int) {
        val intent = Intent(ACTION_TIMER_TICK).apply {
            putExtra(EXTRA_DURATION_SECONDS, seconds)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        createNotificationChannel()
        floatingOverlayManager = FloatingOverlayManager.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH)
                val recordAudio = intent.getBooleanExtra(EXTRA_RECORD_AUDIO, false)
                val width = intent.getIntExtra(EXTRA_WIDTH, 0)
                val height = intent.getIntExtra(EXTRA_HEIGHT, 0)
                val fps = intent.getIntExtra(EXTRA_FPS, 30)

                val audioModeStr = intent.getStringExtra(EXTRA_AUDIO_MODE) ?: AudioSourceMode.MIC_AND_INTERNAL.name
                val audioMode = AudioSourceMode.fromString(audioModeStr)
                val orientationStr = intent.getStringExtra(EXTRA_ORIENTATION) ?: VideoOrientation.AUTO.name
                val orientation = try { VideoOrientation.valueOf(orientationStr) } catch (_: Exception) { VideoOrientation.AUTO }
                val isGameMode = intent.getBooleanExtra(EXTRA_IS_GAME_MODE, false)
                val facecamEnabled = intent.getBooleanExtra(EXTRA_FACECAM_ENABLED, false)
                val facecamShape = intent.getStringExtra(EXTRA_FACECAM_SHAPE) ?: "CIRCLE"
                val facecamSize = intent.getStringExtra(EXTRA_FACECAM_SIZE) ?: "MEDIUM"
                val facecamFront = intent.getBooleanExtra(EXTRA_FACECAM_FRONT, true)
                val touchTrackingEnabled = intent.getBooleanExtra(EXTRA_TOUCH_TRACKING_ENABLED, true)

                lastFacecamShape = facecamShape
                lastFacecamSize = facecamSize
                lastFacecamFront = facecamFront

                // Android 14+ requires starting foreground service with type MEDIA_PROJECTION BEFORE projection initialization
                startForegroundServiceNotification(if (isGameMode) "Recording Game..." else "Recording screen...", withCamera = facecamEnabled)

                if (resultCode != 0 && resultData != null && outputPath != null) {
                    currentOutputPath = outputPath
                    val success = startMediaProjectionRecording(
                        resultCode = resultCode,
                        resultData = resultData,
                        outputPath = outputPath,
                        recordAudio = recordAudio,
                        reqWidth = width,
                        reqHeight = height,
                        fps = fps,
                        audioMode = audioMode,
                        orientation = orientation,
                        isGameMode = isGameMode
                    )
                    if (success) {
                        isRunning = true
                        isPaused = false
                        serviceStartTime = android.os.SystemClock.elapsedRealtime()
                        serviceAccumulatedTime = 0L
                        recordingElapsedSeconds = 0
                        broadcastState("RECORDING")
                        floatingOverlayManager?.show()
                        floatingOverlayManager?.updateDuration(0)

                        // Start Phase 2: Touch Tracking
                        if (touchTrackingEnabled) {
                            try {
                                val companionTouchFile = File(outputPath.substringBeforeLast(".") + ".flowtouch")
                                lastTouchCompanionPath = companionTouchFile.absolutePath
                                val tracker = TouchTracker(serviceScope)
                                touchTracker = tracker
                                activeTouchTracker = tracker
                                tracker.start(
                                    recordingId = UUID.randomUUID().toString(),
                                    videoWidth = if (width > 0) width else 1080,
                                    videoHeight = if (height > 0) height else 1920,
                                    companionFile = companionTouchFile
                                )
                                floatingOverlayManager?.onTouchEventListener = { ev ->
                                    tracker.recordMotionEvent(ev)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "TouchTracker initialization error: ${e.message}")
                            }
                        }

                        // Start Phase 2: Floating FaceCam Overlay
                        if (facecamEnabled) {
                            val hasCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            val canOverlay = Settings.canDrawOverlays(this)
                            if (hasCamera && canOverlay) {
                                isFacecamActive = true
                                val facecam = FloatingFacecamManager.getInstance(this)
                                floatingFacecamManager = facecam
                                facecam.onTouchEventListener = { ev ->
                                    touchTracker?.recordMotionEvent(ev)
                                }
                                facecam.show(
                                    shape = facecamShape,
                                    size = facecamSize,
                                    useFrontCamera = facecamFront
                                )
                            } else {
                                isFacecamActive = false
                                Log.w(TAG, "FaceCam requested but permissions missing (Camera: $hasCamera, Overlay: $canOverlay)")
                            }
                        } else {
                            isFacecamActive = false
                        }

                        timerHandler.removeCallbacks(timerRunnable)
                        timerHandler.postDelayed(timerRunnable, 250)
                    } else {
                        broadcastState("ERROR")
                        stopSelf()
                    }
                } else {
                    Log.e(TAG, "Invalid params for start recording")
                    broadcastState("ERROR")
                    stopSelf()
                }
            }
            ACTION_PAUSE -> {
                pauseMediaRecorder()
                touchTracker?.pause()
                isPaused = true
                serviceAccumulatedTime += android.os.SystemClock.elapsedRealtime() - serviceStartTime
                updateNotification("Recording paused", paused = true)
                broadcastState("PAUSED")
                floatingOverlayManager?.updatePausedState(true)
            }
            ACTION_RESUME -> {
                resumeMediaRecorder()
                touchTracker?.resume()
                isPaused = false
                serviceStartTime = android.os.SystemClock.elapsedRealtime()
                updateNotification("Recording screen...", paused = false)
                broadcastState("RECORDING")
                floatingOverlayManager?.updatePausedState(false)
            }
            ACTION_STOP -> {
                handleStopRecording()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun broadcastState(state: String) {
        val intent = Intent(ACTION_STATE_CHANGED).apply {
            putExtra(EXTRA_STATE, state)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun startForegroundServiceNotification(statusText: String, withCamera: Boolean = false) {
        val notification = buildNotification(statusText, paused = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                if (withCamera && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                }
                type
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
            startForeground(
                NOTIFICATION_ID,
                notification,
                serviceType
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startMediaProjectionRecording(
        resultCode: Int,
        resultData: Intent,
        outputPath: String,
        recordAudio: Boolean,
        reqWidth: Int,
        reqHeight: Int,
        fps: Int,
        audioMode: AudioSourceMode = AudioSourceMode.MIC_AND_INTERNAL,
        orientation: VideoOrientation = VideoOrientation.AUTO,
        isGameMode: Boolean = false
    ): Boolean {
        try {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = windowManager.currentWindowMetrics
                val bounds = windowMetrics.bounds
                metrics.widthPixels = bounds.width()
                metrics.heightPixels = bounds.height()
                metrics.densityDpi = resources.configuration.densityDpi
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(metrics)
            }

            val isDevicePortrait = metrics.heightPixels >= metrics.widthPixels
            val isPortrait = when (orientation) {
                VideoOrientation.PORTRAIT -> true
                VideoOrientation.LANDSCAPE -> false
                VideoOrientation.AUTO -> isDevicePortrait
            }

            val nativeMin = minOf(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(320)
            val nativeMax = maxOf(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(480)

            var targetW: Int
            var targetH: Int

            if (reqWidth > 0 && reqHeight > 0) {
                val reqMin = minOf(reqWidth, reqHeight) // e.g. 480, 720, 1080
                val scale = reqMin.toFloat() / nativeMin.toFloat()
                val scaledMax = (nativeMax.toFloat() * scale).toInt()

                if (isPortrait) {
                    targetW = reqMin
                    targetH = scaledMax
                } else {
                    targetW = scaledMax
                    targetH = reqMin
                }
            } else {
                // Native
                if (isPortrait) {
                    targetW = nativeMin
                    targetH = nativeMax
                } else {
                    targetW = nativeMax
                    targetH = nativeMin
                }
            }

            // Bound dimensions within device display bounds
            if (targetW > metrics.widthPixels || targetH > metrics.heightPixels) {
                val scale = minOf(metrics.widthPixels.toFloat() / targetW, metrics.heightPixels.toFloat() / targetH)
                targetW = (targetW * scale).toInt()
                targetH = (targetH * scale).toInt()
            }

            // Ensure dimensions are even numbers (H.264 hardware encoder requirement)
            targetW = (targetW / 2) * 2
            targetH = (targetH / 2) * 2
            if (targetW < 160) targetW = (metrics.widthPixels / 2) * 2
            if (targetH < 160) targetH = (metrics.heightPixels / 2) * 2

            val finalFps = if (isGameMode) 60 else fps.coerceIn(24, 60)
            val bitrate = when {
                isGameMode -> 12_000_000 // 12 Mbps for high-action gameplay
                minOf(targetW, targetH) <= 480 -> 2_500_000 // 2.5 Mbps for 480p
                minOf(targetW, targetH) <= 720 -> 5_000_000 // 5 Mbps for 720p
                else -> 8_000_000 // 8 Mbps for 1080p
            }

            val dpi = if (metrics.densityDpi > 0) metrics.densityDpi else DisplayMetrics.DENSITY_DEFAULT

            val finalOutputFile = File(outputPath)
            finalOutputFile.parentFile?.mkdirs()

            // Check if Android 10+ Internal Audio capture is active
            val needsCaptureManager = (audioMode == AudioSourceMode.INTERNAL || audioMode == AudioSourceMode.MIC_AND_INTERNAL) &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

            val videoTargetFile: File
            if (needsCaptureManager) {
                isDualAudioMuxing = true
                val parentDir = finalOutputFile.parentFile ?: filesDir
                tempRawVideoFile = File(parentDir, "rec_raw_vid_${System.currentTimeMillis()}.mp4")
                tempRawAudioFile = File(parentDir, "rec_raw_aud_${System.currentTimeMillis()}.m4a")
                videoTargetFile = tempRawVideoFile!!
            } else {
                isDualAudioMuxing = false
                tempRawVideoFile = null
                tempRawAudioFile = null
                videoTargetFile = finalOutputFile
            }

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            val hasAudioPermission = recordAudio && (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)

            var audioConfiguredInRecorder = false
            if (!needsCaptureManager && (audioMode == AudioSourceMode.MIC || (audioMode != AudioSourceMode.NONE && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q))) {
                if (hasAudioPermission) {
                    try {
                        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
                        audioConfiguredInRecorder = true
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not set audio source MIC: ${e.message}")
                        audioConfiguredInRecorder = false
                    }
                }
            }

            mediaRecorder?.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (audioConfiguredInRecorder) {
                    try {
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setAudioEncodingBitRate(128000)
                        setAudioSamplingRate(44100)
                    } catch (e: Exception) {
                        Log.w(TAG, "Audio encoder AAC setup failed: ${e.message}")
                    }
                }
                setVideoSize(targetW, targetH)
                setVideoFrameRate(finalFps)
                setVideoEncodingBitRate(bitrate)
                setOutputFile(videoTargetFile.absolutePath)
                prepare()
            }

            // Acquire MediaProjection
            try {
                mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, resultData)
            } catch (e: Exception) {
                Log.e(TAG, "Error acquiring MediaProjection: ${e.message}", e)
                mediaProjection = null
            }
            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection is null")
                broadcastState("ERROR")
                return false
            }

            // Android 14+ requires registering a MediaProjection.Callback
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    Log.d(TAG, "MediaProjection stopped by system")
                    stopMediaProjectionRecording()
                    isRunning = false
                    isPaused = false
                    broadcastState("STOPPED")
                }
            }, null)

            // Start AudioCaptureManager if needed for Internal/Dual audio
            if (needsCaptureManager && tempRawAudioFile != null) {
                audioCaptureManager = AudioCaptureManager(
                    context = this,
                    mediaProjection = mediaProjection,
                    audioSourceMode = audioMode,
                    outputFile = tempRawAudioFile!!
                )
                val started = audioCaptureManager?.start() ?: false
                if (!started) {
                    Log.w(TAG, "AudioCaptureManager could not start, proceeding with screen capture")
                }
            }

            val surface = mediaRecorder?.surface
            if (surface != null) {
                virtualDisplay = mediaProjection?.createVirtualDisplay(
                    "FlowRecCapture",
                    targetW,
                    targetH,
                    dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    surface,
                    null,
                    null
                )
                mediaRecorder?.start()
                Log.d(TAG, "Screen recording started successfully to: ${videoTargetFile.name} ($targetW x $targetH @ ${finalFps}fps, audio: $audioMode)")
                return true
            } else {
                Log.e(TAG, "MediaRecorder surface is null")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting screen recording: ${e.message}", e)
            broadcastState("ERROR")
            return false
        }
    }

    private fun pauseMediaRecorder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
            } catch (e: Exception) {
                Log.w(TAG, "MediaRecorder pause failed: ${e.message}")
            }
        }
        audioCaptureManager?.pause()
    }

    private fun resumeMediaRecorder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
            } catch (e: Exception) {
                Log.w(TAG, "MediaRecorder resume failed: ${e.message}")
            }
        }
        audioCaptureManager?.resume()
    }

    private fun stopMediaProjectionRecording() {
        try {
            audioCaptureManager?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping audio capture: ${e.message}")
        }
        audioCaptureManager = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRunning && !isPaused) {
                try { mediaRecorder?.pause() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing virtual display: ${e.message}")
        }
        virtualDisplay = null

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping media recorder: ${e.message}")
        }
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing media recorder: ${e.message}")
        }
        mediaRecorder = null

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping media projection: ${e.message}")
        }
        mediaProjection = null
    }

    private fun handleStopRecording() {
        if (!isRunning && !isPaused) {
            Log.w(TAG, "Stop requested but recording is not active")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        timerHandler.removeCallbacks(timerRunnable)
        floatingOverlayManager?.remove()

        // Stop Phase 2: TouchTracker & FaceCam
        activeTouchTracker = null
        floatingOverlayManager?.onTouchEventListener = null
        val touchFile = touchTracker?.stop()
        touchTracker = null
        val touchPath = if (touchFile != null && touchFile.exists()) touchFile.absolutePath else lastTouchCompanionPath

        val wasFacecam = isFacecamActive
        floatingFacecamManager?.onTouchEventListener = null
        floatingFacecamManager?.remove()
        floatingFacecamManager = null
        isFacecamActive = false

        val outputPath = currentOutputPath
        val recordedSec = recordingElapsedSeconds.coerceAtLeast(1)

        isRunning = false
        isPaused = false

        // 1. Release VirtualDisplay, AudioCapture & MediaRecorder
        stopMediaProjectionRecording()

        // 2. Broadcast STOPPED status
        broadcastState("STOPPED")

        // 3. Process the file, mux if needed, and insert into Room Database in background scope
        serviceScope.launch {
            try {
                delay(600L) // Ensure OS closes file handles cleanly

                // Perform muxing if dual/internal audio was captured
                if (isDualAudioMuxing && outputPath != null && tempRawVideoFile != null && tempRawAudioFile != null) {
                    val finalFile = File(outputPath)
                    val rawVid = tempRawVideoFile!!
                    val rawAud = tempRawAudioFile!!

                    if (rawVid.exists() && rawVid.length() > 0) {
                        val muxSuccess = AudioVideoMuxer.mux(rawVid, rawAud, finalFile)
                        if (!muxSuccess) {
                            Log.w(TAG, "AudioVideoMuxer returned false, using raw video directly")
                            if (rawVid.exists()) {
                                rawVid.copyTo(finalFile, overwrite = true)
                            }
                        }
                    }
                    try { rawVid.delete() } catch (_: Exception) {}
                    try { rawAud.delete() } catch (_: Exception) {}
                    isDualAudioMuxing = false
                }

                val videoFile = outputPath?.let { File(it) }
                var realDurationSec = recordedSec
                var fileSize = 0L
                var validVideoPath = ""
                var thumbnailPath = "thumb_mountain"

                if (videoFile != null && videoFile.exists() && videoFile.length() > 0) {
                    validVideoPath = videoFile.absolutePath
                    fileSize = videoFile.length()

                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(videoFile.absolutePath)
                        val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val durMs = durStr?.toLongOrNull() ?: 0L
                        if (durMs > 500) {
                            realDurationSec = (durMs / 1000L).toInt().coerceAtLeast(1)
                        }

                        val frameTimeUs = minOf(500_000L, if (durMs > 0) (durMs * 500L) else 500_000L)
                        val frame: Bitmap? = retriever.getFrameAtTime(frameTimeUs) ?: retriever.getFrameAtTime(0)
                        if (frame != null) {
                            val thumbsDir = File(filesDir, "thumbnails")
                            thumbsDir.mkdirs()
                            val thumbFile = File(thumbsDir, "thumb_${videoFile.nameWithoutExtension}.png")
                            val fos = FileOutputStream(thumbFile)
                            frame.compress(Bitmap.CompressFormat.PNG, 90, fos)
                            fos.flush()
                            fos.close()
                            thumbnailPath = thumbFile.absolutePath
                        }
                        retriever.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Metadata extraction failed: ${e.message}")
                    }
                } else {
                    Log.w(TAG, "Recorded video file is missing or empty: $outputPath")
                }

                // Read user preferences
                val prefs = getSharedPreferences("flowrec_settings", Context.MODE_PRIVATE)
                val autoSaveToGallery = prefs.getBoolean("auto_save_gallery", true)
                val defaultRes = prefs.getString("default_res", "1080p") ?: "1080p"
                val defaultFps = prefs.getInt("default_fps", 30)

                val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                val projectName = "Screen Recording ${sdf.format(Date())}"

                var isExportedToGallery = false
                if (videoFile != null && videoFile.exists() && videoFile.length() > 0) {
                    try {
                        val galleryUri = GalleryExporter.saveVideoToGallery(applicationContext, videoFile, projectName)
                        if (galleryUri != null) {
                            isExportedToGallery = true
                            Log.d(TAG, "Successfully saved video to device gallery: $galleryUri")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Auto-save to gallery failed: ${e.message}")
                    }
                }

                // Write companion .flowcam metadata file if FaceCam was enabled
                var facecamMetaPath: String? = null
                if (wasFacecam && outputPath != null) {
                    try {
                        val camFile = File(outputPath.substringBeforeLast(".") + ".flowcam")
                        camFile.writeText("{\"flowcam_v\":1,\"active\":true,\"shape\":\"$lastFacecamShape\",\"size\":\"$lastFacecamSize\",\"front_lens\":$lastFacecamFront,\"timestamp\":${System.currentTimeMillis()}}\n")
                        facecamMetaPath = camFile.absolutePath
                    } catch (e: Exception) {
                        Log.w(TAG, "Error saving .flowcam metadata: ${e.message}")
                    }
                }

                val project = ProjectEntity(
                    id = UUID.randomUUID().toString(),
                    name = projectName,
                    durationSeconds = realDurationSec,
                    resolution = defaultRes,
                    fps = defaultFps,
                    fileSizeBytes = fileSize,
                    thumbnailResName = thumbnailPath,
                    videoPath = validVideoPath,
                    createdAt = System.currentTimeMillis(),
                    isFavorite = false,
                    isExported = isExportedToGallery,
                    cursorEnabled = prefs.getBoolean("show_touches", true),
                    clickZoomEnabled = true,
                    touchMetadataPath = touchPath,
                    facecamEnabled = wasFacecam,
                    facecamMetadataPath = facecamMetaPath
                )

                // Save directly to Room Database
                val db = FlowRecDatabase.getDatabase(applicationContext)
                db.projectDao().insertProject(project)
                Log.d(TAG, "Project saved to Room Database successfully: ${project.id}")

                // Broadcast saved event with project id
                val savedIntent = Intent(ACTION_RECORDING_SAVED).apply {
                    putExtra(EXTRA_PROJECT_ID, project.id)
                    setPackage(packageName)
                }
                sendBroadcast(savedIntent)

                // Show notification that recording has saved
                showRecordingSavedNotification(project)

                // Directly launch editor screen for user
                try {
                    val directOpenIntent = Intent(this@ScreenRecorderService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("open_project_id", project.id)
                    }
                    startActivity(directOpenIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not directly launch MainActivity: ${e.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error finalizing recording: ${e.message}", e)
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun showRecordingSavedNotification(project: ProjectEntity) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_project_id", project.id)
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            1002,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Recording Saved")
            .setContentText("Saved to Gallery • Tap to edit (${formatSeconds(project.durationSeconds)})")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingOpenApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(1002, notification)
    }

    private fun updateNotification(statusText: String, paused: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(statusText, paused))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active recording status and controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String, paused: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action: Pause / Resume
        val toggleActionIntent = Intent(this, ScreenRecorderService::class.java).apply {
            action = if (paused) ACTION_RESUME else ACTION_PAUSE
        }
        val pendingToggle = PendingIntent.getService(
            this,
            1,
            toggleActionIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action: Stop
        val stopActionIntent = Intent(this, ScreenRecorderService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            2,
            stopActionIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FlowRec Screen Recorder")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (paused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (paused) "Resume" else "Pause",
                pendingToggle
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                pendingStop
            )

        return builder.build()
    }

    override fun onDestroy() {
        timerHandler.removeCallbacks(timerRunnable)
        floatingOverlayManager?.remove()
        stopMediaProjectionRecording()
        isRunning = false
        isPaused = false
        super.onDestroy()
    }
}

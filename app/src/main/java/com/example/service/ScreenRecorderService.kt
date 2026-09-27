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
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import java.io.File

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
        const val EXTRA_STATE = "extra_state" // "RECORDING", "PAUSED", "STOPPED", "ERROR"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_OUTPUT_PATH = "extra_output_path"
        const val EXTRA_RECORD_AUDIO = "extra_record_audio"
        const val EXTRA_WIDTH = "extra_width"
        const val EXTRA_HEIGHT = "extra_height"
        const val EXTRA_FPS = "extra_fps"

        var isRunning = false
            private set

        var isPaused = false
            private set

        fun startRecording(
            context: Context,
            resultCode: Int,
            resultData: Intent,
            outputPath: String,
            recordAudio: Boolean,
            width: Int,
            height: Int,
            fps: Int
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

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        createNotificationChannel()
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

                // Android 14+ requires starting foreground service with type MEDIA_PROJECTION BEFORE projection initialization
                startForegroundServiceNotification("Recording screen...")

                if (resultCode != 0 && resultData != null && outputPath != null) {
                    currentOutputPath = outputPath
                    val success = startMediaProjectionRecording(resultCode, resultData, outputPath, recordAudio, width, height, fps)
                    if (success) {
                        isRunning = true
                        isPaused = false
                        broadcastState("RECORDING")
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
                isPaused = true
                updateNotification("Recording paused", paused = true)
                broadcastState("PAUSED")
            }
            ACTION_RESUME -> {
                resumeMediaRecorder()
                isPaused = false
                updateNotification("Recording screen...", paused = false)
                broadcastState("RECORDING")
            }
            ACTION_STOP -> {
                stopMediaProjectionRecording()
                isRunning = false
                isPaused = false
                broadcastState("STOPPED")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
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

    private fun startForegroundServiceNotification(statusText: String) {
        val notification = buildNotification(statusText, paused = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
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
        fps: Int
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

            val isPortrait = metrics.heightPixels >= metrics.widthPixels
            var targetW = if (reqWidth > 0) reqWidth else metrics.widthPixels
            var targetH = if (reqHeight > 0) reqHeight else metrics.heightPixels

            // Match orientation: if device is portrait but dimensions were given in landscape (e.g. 1920x1080), swap to 1080x1920
            if (isPortrait && targetW > targetH) {
                val tmp = targetW
                targetW = targetH
                targetH = tmp
            } else if (!isPortrait && targetH > targetW) {
                val tmp = targetW
                targetW = targetH
                targetH = tmp
            }

            // Bound dimensions within device display
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

            val dpi = if (metrics.densityDpi > 0) metrics.densityDpi else DisplayMetrics.DENSITY_DEFAULT

            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            val hasAudioPermission = recordAudio && (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)

            var audioConfigured = false
            if (hasAudioPermission) {
                try {
                    mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
                    audioConfigured = true
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set audio source MIC: ${e.message}")
                    audioConfigured = false
                }
            }

            mediaRecorder?.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                if (audioConfigured) {
                    try {
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setAudioEncodingBitRate(128000)
                        setAudioSamplingRate(44100)
                    } catch (e: Exception) {
                        Log.w(TAG, "Audio encoder AAC setup failed: ${e.message}")
                    }
                }
                setVideoSize(targetW, targetH)
                setVideoFrameRate(fps.coerceIn(24, 60))
                setVideoEncodingBitRate(6_000_000) // 6 Mbps bitrate
                setOutputFile(outputFile.absolutePath)
                prepare()
            }

            // Acquire MediaProjection
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, resultData)
            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection is null")
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
                Log.d(TAG, "Screen recording started successfully to: $outputPath ($targetW x $targetH @ ${fps}fps)")
                return true
            } else {
                Log.e(TAG, "MediaRecorder surface is null")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting screen recording: ${e.message}", e)
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
    }

    private fun resumeMediaRecorder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
            } catch (e: Exception) {
                Log.w(TAG, "MediaRecorder resume failed: ${e.message}")
            }
        }
    }

    private fun stopMediaProjectionRecording() {
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
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing virtual display: ${e.message}")
        }
        virtualDisplay = null

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping media projection: ${e.message}")
        }
        mediaProjection = null
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
        stopMediaProjectionRecording()
        isRunning = false
        isPaused = false
        super.onDestroy()
    }
}

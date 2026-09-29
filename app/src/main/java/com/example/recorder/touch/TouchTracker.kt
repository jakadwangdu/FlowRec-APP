package com.example.recorder.touch

import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Thread-safe touch event tracking engine.
 * Records touch gestures during screen recording and streams them in buffered batches
 * to a companion `.flowtouch` file alongside the video, preventing unbounded RAM consumption.
 */
class TouchTracker(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "TouchTracker"
        private const val BATCH_FLUSH_THRESHOLD = 30
    }

    private var targetFile: File? = null
    private var writer: BufferedWriter? = null

    private var recordingId: String = ""
    private var videoWidth: Int = 1080
    private var videoHeight: Int = 1920
    private var startTimeMs: Long = 0L
    private var isRecording: Boolean = false
    private var isPaused: Boolean = false
    private var totalEventsCount: Int = 0

    // Memory-bounded sliding queue for real-time UI visualization
    private val _recentTouches = MutableStateFlow<List<FlowTouchEvent>>(emptyList())
    val recentTouches: StateFlow<List<FlowTouchEvent>> = _recentTouches.asStateFlow()

    // Thread-safe buffer for batched disk writing
    private val pendingBuffer = ConcurrentLinkedQueue<FlowTouchEvent>()
    private var flushJob: Job? = null

    /**
     * Start tracking touches for a new recording session.
     */
    @Synchronized
    fun start(
        recordingId: String,
        videoWidth: Int,
        videoHeight: Int,
        companionFile: File
    ) {
        this.recordingId = recordingId
        this.videoWidth = videoWidth.coerceAtLeast(1)
        this.videoHeight = videoHeight.coerceAtLeast(1)
        this.targetFile = companionFile
        this.startTimeMs = SystemClock.elapsedRealtime()
        this.isRecording = true
        this.isPaused = false
        this.totalEventsCount = 0
        this.pendingBuffer.clear()
        this._recentTouches.value = emptyList()

        try {
            companionFile.parentFile?.mkdirs()
            val fileWriter = FileWriter(companionFile, false)
            writer = BufferedWriter(fileWriter)

            // Write metadata header line
            val header = "{\"flowtouch_v\":1,\"rec_id\":\"$recordingId\",\"w\":$videoWidth,\"h\":$videoHeight,\"start_epoch\":${System.currentTimeMillis()}}\n"
            writer?.write(header)
            writer?.flush()
            Log.d(TAG, "TouchTracker started, saving to: ${companionFile.name}")
        } catch (e: IOException) {
            Log.e(TAG, "Error initializing companion touch file: ${e.message}", e)
        }
    }

    /**
     * Records a MotionEvent (from flowrec overlay or interactive surface).
     */
    fun recordMotionEvent(event: MotionEvent) {
        if (!isRecording || isPaused) return

        val nowMs = SystemClock.elapsedRealtime() - startTimeMs
        val actionString = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> "DOWN"
            MotionEvent.ACTION_MOVE -> "MOVE"
            MotionEvent.ACTION_UP -> "UP"
            MotionEvent.ACTION_CANCEL -> "CANCEL"
            MotionEvent.ACTION_POINTER_DOWN -> "POINTER_DOWN"
            MotionEvent.ACTION_POINTER_UP -> "POINTER_UP"
            else -> return
        }

        val pointerCount = event.pointerCount
        for (i in 0 until pointerCount) {
            val px = event.getX(i)
            val py = event.getY(i)
            val pid = event.getPointerId(i)
            val pressure = try { event.getPressure(i) } catch (_: Exception) { 1.0f }

            val touchEvent = FlowTouchEvent(
                timestampMs = nowMs,
                x = px,
                y = py,
                action = actionString,
                pointerId = pid,
                normalizedX = (px / videoWidth).coerceIn(0f, 1f),
                normalizedY = (py / videoHeight).coerceIn(0f, 1f),
                pressure = pressure
            )

            recordEventInternal(touchEvent)
        }
    }

    /**
     * Records an explicit touch coordinate event.
     */
    fun recordRawTouch(x: Float, y: Float, action: String, pointerId: Int = 0, pressure: Float = 1.0f) {
        if (!isRecording || isPaused) return

        val nowMs = SystemClock.elapsedRealtime() - startTimeMs
        val touchEvent = FlowTouchEvent(
            timestampMs = nowMs,
            x = x,
            y = y,
            action = action,
            pointerId = pointerId,
            normalizedX = (x / videoWidth).coerceIn(0f, 1f),
            normalizedY = (y / videoHeight).coerceIn(0f, 1f),
            pressure = pressure
        )

        recordEventInternal(touchEvent)
    }

    private fun recordEventInternal(event: FlowTouchEvent) {
        totalEventsCount++
        pendingBuffer.add(event)

        // Update real-time visualization sliding window (max 6 active points)
        val current = _recentTouches.value.toMutableList()
        if (event.action == "UP" || event.action == "CANCEL") {
            current.removeAll { it.pointerId == event.pointerId }
        } else {
            current.removeAll { it.pointerId == event.pointerId }
            current.add(event)
            if (current.size > 8) {
                current.removeAt(0)
            }
        }
        _recentTouches.value = current

        // Flush in batches to prevent memory growth
        if (pendingBuffer.size >= BATCH_FLUSH_THRESHOLD) {
            triggerFlush()
        }
    }

    private fun triggerFlush() {
        scope.launch(Dispatchers.IO) {
            flushBufferToDisk()
        }
    }

    @Synchronized
    private fun flushBufferToDisk() {
        val w = writer ?: return
        try {
            while (pendingBuffer.isNotEmpty()) {
                val ev = pendingBuffer.poll() ?: break
                w.write(ev.toJson())
                w.newLine()
            }
            w.flush()
        } catch (e: Exception) {
            Log.w(TAG, "Error writing touch buffer: ${e.message}")
        }
    }

    fun pause() {
        isPaused = true
        _recentTouches.value = emptyList()
        triggerFlush()
    }

    fun resume() {
        isPaused = false
    }

    /**
     * Flushes remaining events, writes summary footer, and closes file handle safely.
     */
    @Synchronized
    fun stop(): File? {
        isRecording = false
        isPaused = false
        _recentTouches.value = emptyList()

        try {
            flushBufferToDisk()
            val w = writer
            if (w != null) {
                val durationMs = SystemClock.elapsedRealtime() - startTimeMs
                val footer = "{\"footer\":true,\"total_events\":$totalEventsCount,\"duration_ms\":$durationMs}\n"
                w.write(footer)
                w.flush()
                w.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing touch writer: ${e.message}", e)
        } finally {
            writer = null
        }

        Log.d(TAG, "TouchTracker stopped. Total events recorded: $totalEventsCount")
        return targetFile
    }

    companion object Helper {
        /**
         * Reads companion `.flowtouch` file into structured `FlowTouchMetadata`.
         */
        fun readMetadata(file: File): FlowTouchMetadata? {
            if (!file.exists() || file.length() == 0L) return null

            return try {
                val lines = file.readLines()
                if (lines.isEmpty()) return null

                val firstLine = lines.first()
                val recId = firstLine.substringAfter("\"rec_id\":\"", "").substringBefore("\"")
                val width = firstLine.substringAfter("\"w\":", "1080").substringBefore(",").toIntOrNull() ?: 1080
                val height = firstLine.substringAfter("\"h\":", "1920").substringBefore(",").toIntOrNull() ?: 1920
                val startEpoch = firstLine.substringAfter("\"start_epoch\":", "0").substringBefore("}").toLongOrNull() ?: 0L

                val events = mutableListOf<FlowTouchEvent>()
                var durationMs = 0L

                for (i in 1 until lines.size) {
                    val line = lines[i].trim()
                    if (line.isEmpty()) continue
                    if (line.contains("\"footer\":true")) {
                        durationMs = line.substringAfter("\"duration_ms\":", "0").substringBefore("}").toLongOrNull() ?: 0L
                    } else {
                        FlowTouchEvent.fromJson(line)?.let { events.add(it) }
                    }
                }

                FlowTouchMetadata(
                    version = 1,
                    recordingId = recId,
                    videoWidth = width,
                    videoHeight = height,
                    startTimeEpochMs = startEpoch,
                    durationMs = durationMs,
                    totalEvents = events.size,
                    events = events
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error reading .flowtouch file: ${e.message}")
                null
            }
        }
    }
}

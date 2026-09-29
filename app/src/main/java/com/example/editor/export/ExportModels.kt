package com.example.editor.export

import android.net.Uri
import java.io.File

/**
 * Target export resolution options for the rendered MP4.
 */
enum class ExportResolution(
    val label: String,
    val targetHeight: Int,
    val targetWidthLandscape: Int,
    val targetWidthPortrait: Int
) {
    SOURCE("Source (Original)", 0, 0, 0),
    HD_720P("720p HD", 720, 1280, 720),
    FHD_1080P("1080p Full HD", 1080, 1920, 1080);

    /**
     * Calculates output dimensions preserving source aspect ratio without distortion.
     */
    fun calculateDimensions(sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
        if (this == SOURCE || sourceWidth <= 0 || sourceHeight <= 0) {
            val w = if (sourceWidth % 2 != 0) sourceWidth - 1 else sourceWidth
            val h = if (sourceHeight % 2 != 0) sourceHeight - 1 else sourceHeight
            return Pair(w.coerceAtLeast(320), h.coerceAtLeast(240))
        }

        val isLandscape = sourceWidth >= sourceHeight
        val targetLongEdge = if (this == FHD_1080P) 1920 else 1280
        val targetShortEdge = if (this == FHD_1080P) 1080 else 720

        val aspectRatio = sourceWidth.toFloat() / sourceHeight.toFloat()

        var outW: Int
        var outH: Int

        if (isLandscape) {
            outH = targetShortEdge
            outW = (outH * aspectRatio).toInt()
            if (outW > targetLongEdge) {
                outW = targetLongEdge
                outH = (outW / aspectRatio).toInt()
            }
        } else {
            outW = targetShortEdge
            outH = (outW / aspectRatio).toInt()
            if (outH > targetLongEdge) {
                outH = targetLongEdge
                outW = (outH * aspectRatio).toInt()
            }
        }

        // Ensure even dimensions required by H.264 encoders
        if (outW % 2 != 0) outW -= 1
        if (outH % 2 != 0) outH -= 1

        return Pair(outW.coerceAtLeast(320), outH.coerceAtLeast(240))
    }
}

/**
 * Output framerate options.
 */
enum class ExportFps(val label: String, val fps: Int) {
    FPS_30("30 FPS", 30),
    FPS_60("60 FPS", 60)
}

/**
 * Computes target video bitrate in bps based on resolution, framerate, and quality.
 */
fun com.example.model.ExportQuality.calculateBitrate(width: Int, height: Int, fps: Int): Int {
    val pixelCount = width * height
    val baseBps = when {
        pixelCount >= 1920 * 1080 -> 8_000_000
        pixelCount >= 1280 * 720 -> 4_500_000
        else -> 2_500_000
    }
    val multiplier = when (this) {
        com.example.model.ExportQuality.MAXIMUM -> 1.5f
        com.example.model.ExportQuality.HIGH -> 1.1f
        com.example.model.ExportQuality.BALANCED -> 0.7f
    }
    val fpsFactor = if (fps >= 60) 1.35f else 1.0f
    return (baseBps * multiplier * fpsFactor).toInt().coerceIn(1_500_000, 20_000_000)
}

/**
 * State lifecycle of an active or completed export job.
 */
enum class ExportState {
    IDLE,
    PREPARING,
    EXPORTING,
    FINALIZING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Configuration parameters for a video export job.
 */
data class ExportConfig(
    val resolution: ExportResolution = ExportResolution.FHD_1080P,
    val fps: ExportFps = ExportFps.FPS_30,
    val quality: com.example.model.ExportQuality = com.example.model.ExportQuality.HIGH,
    val burnTouchEffects: Boolean = true,
    val burnFaceCam: Boolean = true,
    val includeOriginalAudio: Boolean = true,
    val originalAudioVolume: Float = 1.0f,
    val includeBgm: Boolean = true,
    val bgmVolume: Float = 0.5f,
    val includeVoiceover: Boolean = true,
    val voiceoverVolume: Float = 1.0f,
    val outputFileName: String? = null
)

/**
 * Real-time progress update emitted during export.
 */
data class ExportProgress(
    val state: ExportState = ExportState.IDLE,
    val progressPercent: Float = 0f, // 0.0 to 100.0
    val currentStepMessage: String = "",
    val elapsedTimeMs: Long = 0L,
    val estimatedRemainingTimeMs: Long = 0L,
    val renderedDurationMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Final result returned upon successful export.
 */
data class ExportResult(
    val outputFile: File,
    val contentUri: Uri?,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val width: Int,
    val height: Int,
    val fps: Int,
    val format: String = "MP4"
)

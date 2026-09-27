package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.AccentRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.KeyframeAmber
import com.example.ui.theme.WaveformBlue

@Composable
fun WaveformCanvas(
    durationSeconds: Int,
    playheadSeconds: Int,
    onSeek: (Int) -> Unit,
    zoomScale: Float = 1.0f,
    cursorTrackEnabled: Boolean = true,
    zoomTrackEnabled: Boolean = true,
    effectsTrackEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val maxDuration = durationSeconds.coerceAtLeast(10)
    val progress = playheadSeconds.toFloat() / maxDuration.toFloat()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .pointerInput(maxDuration) {
                detectTapGestures { offset ->
                    val targetProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    val targetSec = (targetProgress * maxDuration).toInt()
                    onSeek(targetSec)
                }
            }
            .pointerInput(maxDuration) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val targetProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    val targetSec = (targetProgress * maxDuration).toInt()
                    onSeek(targetSec)
                }
            }
            .testTag("waveform_timeline_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val trackHeight = height / 5f

            // Track Dividers
            for (i in 1..4) {
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, i * trackHeight),
                    end = Offset(width, i * trackHeight),
                    strokeWidth = 1f
                )
            }

            // Track 1: Video Segment blocks
            val segWidth = width * 0.95f
            drawRoundRect(
                color = AccentBlue.copy(alpha = 0.35f),
                topLeft = Offset(width * 0.02f, trackHeight * 0.15f),
                size = Size(segWidth, trackHeight * 0.7f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )

            // Track 2: Cursor Track Keyframes (Dots)
            if (cursorTrackEnabled) {
                val cursorKeyframes = listOf(0.18f, 0.35f, 0.52f, 0.72f, 0.88f)
                cursorKeyframes.forEach { kf ->
                    val cx = width * kf
                    val cy = trackHeight * 1.5f
                    drawCircle(
                        color = Color.White,
                        radius = 4.5f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = AccentBlue.copy(alpha = 0.4f),
                        radius = 8f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )
                }
            }

            // Track 3: Zoom Track Curves / Ramps
            if (zoomTrackEnabled) {
                val zoomStart = width * 0.28f
                val zoomRampIn = width * 0.34f
                val zoomRampOut = width * 0.58f
                val zoomEnd = width * 0.64f
                val topY = trackHeight * 2.2f
                val botY = trackHeight * 2.8f

                val zoomPath = Path().apply {
                    moveTo(width * 0.02f, botY)
                    lineTo(zoomStart, botY)
                    cubicTo(zoomStart + 15f, botY, zoomRampIn - 15f, topY, zoomRampIn, topY)
                    lineTo(zoomRampOut, topY)
                    cubicTo(zoomRampOut + 15f, topY, zoomEnd - 15f, botY, zoomEnd, botY)
                    lineTo(width * 0.98f, botY)
                }
                drawPath(
                    path = zoomPath,
                    color = KeyframeAmber,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )

                // Fill under zoom peak
                val fillPath = Path().apply {
                    moveTo(zoomStart, botY)
                    cubicTo(zoomStart + 15f, botY, zoomRampIn - 15f, topY, zoomRampIn, topY)
                    lineTo(zoomRampOut, topY)
                    cubicTo(zoomRampOut + 15f, topY, zoomEnd - 15f, botY, zoomEnd, botY)
                    close()
                }
                drawPath(
                    path = fillPath,
                    color = KeyframeAmber.copy(alpha = 0.18f)
                )
            }

            // Track 4: Effects Diamonds
            if (effectsTrackEnabled) {
                val effectPoints = listOf(0.12f, 0.44f, 0.78f)
                effectPoints.forEach { ep ->
                    val cx = width * ep
                    val cy = trackHeight * 3.5f
                    val diamondPath = Path().apply {
                        moveTo(cx, cy - 6f)
                        lineTo(cx + 6f, cy)
                        lineTo(cx, cy + 6f)
                        lineTo(cx - 6f, cy)
                        close()
                    }
                    drawPath(path = diamondPath, color = AccentOrange)
                }
            }

            // Track 5: Audio Waveform Bars
            val audioBaseY = trackHeight * 4.5f
            val barCount = (width / 5f).toInt().coerceAtLeast(30)
            val barStep = width / barCount.toFloat()
            for (i in 0 until barCount) {
                val x = i * barStep + 2f
                val normalizedIdx = (i.toFloat() / barCount.toFloat()) * 30f
                val amplitude = (Math.sin(normalizedIdx.toDouble()) * 0.4 +
                        Math.cos((normalizedIdx * 2.5).toDouble()) * 0.3 +
                        Math.sin((normalizedIdx * 0.8).toDouble()) * 0.3).toFloat().coerceIn(-1f, 1f)
                val barH = (Math.abs(amplitude) * (trackHeight * 0.7f)).coerceAtLeast(3f)

                drawLine(
                    color = WaveformBlue.copy(alpha = 0.75f),
                    start = Offset(x, audioBaseY - barH / 2f),
                    end = Offset(x, audioBaseY + barH / 2f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }

            // Playhead Cursor Needle
            val playheadX = width * progress
            drawLine(
                color = AccentRed,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, height),
                strokeWidth = 2f
            )

            // Playhead Top Pointer Cap
            val capPath = Path().apply {
                moveTo(playheadX - 6f, 0f)
                lineTo(playheadX + 6f, 0f)
                lineTo(playheadX, 10f)
                close()
            }
            drawPath(path = capPath, color = AccentRed)
        }
    }
}

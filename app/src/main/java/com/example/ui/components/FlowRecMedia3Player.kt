package com.example.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.editor.model.EditorProjectState
import com.example.editor.timeline.TimelineManager
import com.example.recorder.touch.FlowTouchEvent
import kotlinx.coroutines.delay
import java.io.File

/**
 * AndroidX Media3 / ExoPlayer video playback engine for FlowRec video editor.
 * Provides frame-accurate scrubbing, non-destructive segment skipping, playback speed,
 * and live compositing of text, image overlays, touch effects, zoom keyframes, and FaceCam.
 */
@OptIn(UnstableApi::class)
@Composable
fun FlowRecMedia3Player(
    videoPath: String,
    thumbnailResName: String,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    timelinePositionMs: Long,
    onTimelinePositionChanged: (Long) -> Unit,
    editorState: EditorProjectState,
    playbackSpeed: Float = 1.0f,
    touchEvents: List<FlowTouchEvent> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoFile = remember(videoPath) {
        if (videoPath.isNotBlank()) File(videoPath) else null
    }
    val hasValidVideo = remember(videoFile) {
        videoFile != null && videoFile.exists() && videoFile.length() > 0
    }

    // Initialize ExoPlayer
    val exoPlayer = remember(context, videoPath) {
        if (hasValidVideo) {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(Uri.fromFile(videoFile))
                setMediaItem(mediaItem)
                prepare()
                repeatMode = Player.REPEAT_MODE_OFF
            }
        } else null
    }

    // Release player on disposal
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    // Playback Speed & Volume synchronization
    LaunchedEffect(playbackSpeed, editorState.audioConfig) {
        exoPlayer?.let { player ->
            player.playbackParameters = PlaybackParameters(playbackSpeed)
            val isMuted = editorState.audioConfig.originalAudioMuted
            val vol = editorState.audioConfig.originalAudioVolume
            player.volume = if (isMuted) 0f else vol.coerceIn(0f, 2f)
        }
    }

    // Play / Pause synchronization
    LaunchedEffect(isPlaying) {
        exoPlayer?.let { player ->
            if (isPlaying && !player.isPlaying) {
                player.play()
            } else if (!isPlaying && player.isPlaying) {
                player.pause()
            }
        }
    }

    // Seek synchronization when timeline scrubbed externally while paused
    LaunchedEffect(timelinePositionMs) {
        exoPlayer?.let { player ->
            if (!isPlaying) {
                val (_, sourceTargetMs) = TimelineManager.mapTimelineToSource(editorState, timelinePositionMs)
                if (kotlin.math.abs(player.currentPosition - sourceTargetMs) > 100L) {
                    player.seekTo(sourceTargetMs)
                }
            }
        }
    }

    // Real-time playback synchronization loop
    LaunchedEffect(isPlaying, exoPlayer, editorState) {
        if (isPlaying && exoPlayer != null) {
            while (isPlaying && exoPlayer.isPlaying) {
                val currentSourceMs = exoPlayer.currentPosition
                val (segIdx, _) = TimelineManager.mapTimelineToSource(editorState, timelinePositionMs)

                // Check segment boundaries for non-destructive playback
                val activeSegments = editorState.segments.filter { !it.isDeleted }
                val currentSeg = editorState.segments.getOrNull(segIdx)

                if (currentSeg != null && currentSourceMs >= currentSeg.sourceEndMs) {
                    // Current segment ended! Find next active segment
                    val nextSeg = activeSegments.firstOrNull { it.sourceStartMs >= currentSeg.sourceEndMs }
                    if (nextSeg != null) {
                        exoPlayer.seekTo(nextSeg.sourceStartMs)
                        val nextTl = TimelineManager.mapSourceToTimeline(editorState, nextSeg.sourceStartMs)
                        onTimelinePositionChanged(nextTl)
                    } else {
                        // Reached end of timeline, loop back to start
                        val firstSeg = activeSegments.firstOrNull()
                        val loopStart = firstSeg?.sourceStartMs ?: 0L
                        exoPlayer.seekTo(loopStart)
                        onTimelinePositionChanged(0L)
                        onPlayToggle()
                        break
                    }
                } else {
                    val currentTimelineMs = TimelineManager.mapSourceToTimeline(editorState, currentSourceMs)
                    onTimelinePositionChanged(currentTimelineMs)
                }

                delay(40L) // ~25 fps position synchronization
            }
        }
    }

    // Calculate Active Zoom
    val (zoomScale, focalX, focalY) = remember(editorState, timelinePositionMs) {
        TimelineManager.getActiveZoom(editorState, timelinePositionMs)
    }

    // Calculate Active Overlays
    val activeTextOverlays = remember(editorState, timelinePositionMs) {
        TimelineManager.getActiveTextOverlays(editorState, timelinePositionMs)
    }
    val activeImageOverlays = remember(editorState, timelinePositionMs) {
        TimelineManager.getActiveImageOverlays(editorState, timelinePositionMs)
    }

    // Find Active Touch Events for current playhead
    val currentTouchEvents = remember(touchEvents, timelinePositionMs) {
        touchEvents.filter {
            val delta = timelinePositionMs - it.timestampMs
            delta in 0..500
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
            .clickable { onPlayToggle() }
    ) {
        // 1. VIDEO LAYER (with non-destructive zoom graphicsLayer)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = -(focalX - 0.5f) * size.width * (zoomScale - 1f)
                    translationY = -(focalY - 0.5f) * size.height * (zoomScale - 1f)
                }
        ) {
            if (hasValidVideo && exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback Thumbnail
                val isCustomFile = thumbnailResName.startsWith("/") || thumbnailResName.startsWith("file:")
                val thumbModel: Any = remember(thumbnailResName) {
                    if (isCustomFile && File(thumbnailResName).exists()) {
                        File(thumbnailResName)
                    } else {
                        val res = context.resources.getIdentifier(thumbnailResName, "drawable", context.packageName)
                        if (res != 0) res else com.example.R.drawable.ic_flowrec_logo
                    }
                }
                AsyncImage(
                    model = thumbModel,
                    contentDescription = "Video Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. TOUCH OVERLAY LAYER (visualizes recorded .flowtouch events)
        if (editorState.touchConfig.enabled && currentTouchEvents.isNotEmpty()) {
            TouchVisualizationLayer(
                config = com.example.recorder.touch.TouchEffectConfig(
                    enabled = true,
                    rippleEnabled = editorState.touchConfig.rippleEnabled,
                    highlightEnabled = editorState.touchConfig.highlightEnabled,
                    movementTrackingEnabled = editorState.touchConfig.movementTrackingEnabled,
                    sizeDp = editorState.touchConfig.sizeDp,
                    durationMs = editorState.touchConfig.durationMs,
                    opacity = editorState.touchConfig.opacity,
                    colorHex = editorState.touchConfig.colorHex
                ),
                activeTouches = currentTouchEvents,
                interactive = false,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. TEXT OVERLAYS LAYER
        activeTextOverlays.forEach { textOverlay ->
            val textColor = try { Color(android.graphics.Color.parseColor(textOverlay.textColorHex)) } catch (_: Exception) { Color.White }
            val bgColor = textOverlay.backgroundColorHex?.let {
                try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { Color(0x80000000) }
            } ?: Color.Transparent

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (textOverlay.xPercent * 500f).toInt(),
                                (textOverlay.yPercent * 300f).toInt()
                            )
                        }
                        .rotate(textOverlay.rotationDeg)
                        .clip(RoundedCornerShape(6.dp))
                        .background(bgColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = textOverlay.text,
                        color = textColor,
                        fontSize = textOverlay.fontSizeSp.sp,
                        fontWeight = if (textOverlay.fontBold) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // 4. IMAGE / LOGO OVERLAYS LAYER
        activeImageOverlays.forEach { imageOverlay ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (imageOverlay.xPercent * 500f).toInt(),
                                (imageOverlay.yPercent * 300f).toInt()
                            )
                        }
                        .rotate(imageOverlay.rotationDeg)
                        .size((imageOverlay.sizePercent * 250f).dp)
                        .graphicsLayer { alpha = imageOverlay.opacity }
                ) {
                    AsyncImage(
                        model = imageOverlay.imageUriOrPath,
                        contentDescription = "Overlay Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 5. FACECAM PREVIEW BUBBLE OVERLAY
        val facecam = editorState.faceCamTrack
        if (facecam.enabled && timelinePositionMs in facecam.startTimeMs..facecam.endTimeMs) {
            val cornerRadius = when (facecam.shape.uppercase()) {
                "CIRCLE" -> 50.dp
                "SQUARE", "RECT" -> 6.dp
                else -> 18.dp // ROUNDED_RECT
            }
            val bubbleSize = (facecam.sizePercent * 260f).dp
            val borderColor = try { Color(android.graphics.Color.parseColor(facecam.borderColorHex)) } catch (_: Exception) { Color.White }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (facecam.xPercent * 450f).toInt(),
                                (facecam.yPercent * 250f).toInt()
                            )
                        }
                        .size(bubbleSize)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(Color(0xFF22222A))
                        .border(facecam.borderWidthDp.dp, borderColor, RoundedCornerShape(cornerRadius)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "FaceCam Bubble",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(bubbleSize * 0.4f)
                    )
                }
            }
        }

        // 6. PLAY / PAUSE CENTER ICON
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

package com.example.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FlowRecVideoPlayer(
    videoPath: String,
    thumbnailResName: String,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    modifier: Modifier = Modifier,
    durationText: String? = null,
    targetZoomLevel: Float = 1.8f,
    enableClickZoom: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val videoFile = remember(videoPath) {
        if (videoPath.isNotBlank()) File(videoPath) else null
    }
    val hasValidVideo = remember(videoFile) {
        videoFile != null && videoFile.exists() && videoFile.length() > 0
    }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    // Click Zoom & Focal Point State
    var isZoomed by remember { mutableStateOf(false) }
    var focalPivot by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var tapPixelOffset by remember { mutableStateOf<Offset?>(null) }
    var showTapRing by remember { mutableStateOf(false) }
    var userPinchScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val animatedZoomScale by animateFloatAsState(
        targetValue = if (isZoomed) targetZoomLevel * userPinchScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "click_zoom_anim"
    )

    LaunchedEffect(isPlaying, hasValidVideo) {
        if (hasValidVideo && videoViewRef != null) {
            if (isPlaying) {
                if (!videoViewRef!!.isPlaying) {
                    videoViewRef!!.start()
                }
            } else {
                if (videoViewRef!!.isPlaying) {
                    videoViewRef!!.pause()
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
            .pointerInput(enableClickZoom, targetZoomLevel) {
                if (!enableClickZoom) return@pointerInput

                detectTapGestures(
                    onDoubleTap = {
                        // Double tap resets zoom
                        isZoomed = false
                        userPinchScale = 1f
                        panOffset = Offset.Zero
                        tapPixelOffset = null
                    },
                    onTap = { offset ->
                        val widthPx = size.width.toFloat()
                        val heightPx = size.height.toFloat()
                        if (widthPx > 0 && heightPx > 0) {
                            val normX = (offset.x / widthPx).coerceIn(0.05f, 0.95f)
                            val normY = (offset.y / heightPx).coerceIn(0.05f, 0.95f)
                            focalPivot = Offset(normX, normY)
                            tapPixelOffset = offset
                            showTapRing = true
                            isZoomed = !isZoomed
                            if (!isZoomed) {
                                userPinchScale = 1f
                                panOffset = Offset.Zero
                            }
                            coroutineScope.launch {
                                delay(1200L)
                                showTapRing = false
                            }
                        }
                    }
                )
            }
            .pointerInput(isZoomed) {
                if (!isZoomed) return@pointerInput
                detectTransformGestures { _, pan, zoom, _ ->
                    userPinchScale = (userPinchScale * zoom).coerceIn(0.8f, 3.5f)
                    panOffset += pan
                }
            }
    ) {
        // Zoomable Content Layer (Video / Image)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedZoomScale
                    scaleY = animatedZoomScale
                    translationX = panOffset.x
                    translationY = panOffset.y
                    transformOrigin = TransformOrigin(focalPivot.x, focalPivot.y)
                }
        ) {
            if (hasValidVideo && isPlaying) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.fromFile(videoFile))
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                if (isPlaying) {
                                    start()
                                }
                            }
                            setOnCompletionListener {
                                onPlayToggle()
                            }
                            videoViewRef = this
                        }
                    },
                    update = { view ->
                        videoViewRef = view
                        if (isPlaying && !view.isPlaying) {
                            view.start()
                        } else if (!isPlaying && view.isPlaying) {
                            view.pause()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Thumbnail Preview
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

        // Tap Location Focal Ring Reticle
        if (showTapRing && tapPixelOffset != null) {
            val tap = tapPixelOffset!!
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = tap.x - 24.dp.toPx()
                        translationY = tap.y - 24.dp.toPx()
                    }
                    .size(48.dp)
                    .border(2.dp, Color(0xFFFF3B30), CircleShape)
                    .background(Color(0x33FF3B30), CircleShape)
            )
        }

        // Center Play / Pause Indicator overlay (when paused and not zoomed)
        if ((!isPlaying || !hasValidVideo) && !isZoomed) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onPlayToggle() }
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Zoom Active Floating Controls (Top Left Chip)
        AnimatedVisibility(
            visible = isZoomed,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.ZoomIn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = String.format("Zoom %.1fx", targetZoomLevel * userPinchScale),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable {
                            isZoomed = false
                            userPinchScale = 1f
                            panOffset = Offset.Zero
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Reset Zoom",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Tap hint banner at bottom when idle
        if (!isZoomed && enableClickZoom) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Tap anywhere to zoom in",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Duration chip at bottom right
        if (!durationText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White
                )
            }
        }
    }
}

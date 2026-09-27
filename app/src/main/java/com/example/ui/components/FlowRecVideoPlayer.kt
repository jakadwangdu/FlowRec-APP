package com.example.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import java.io.File

@Composable
fun FlowRecVideoPlayer(
    videoPath: String,
    thumbnailResName: String,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    modifier: Modifier = Modifier,
    durationText: String? = null
) {
    val context = LocalContext.current
    val videoFile = remember(videoPath) {
        if (videoPath.isNotBlank()) File(videoPath) else null
    }
    val hasValidVideo = remember(videoFile) {
        videoFile != null && videoFile.exists() && videoFile.length() > 0
    }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

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

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
            .clickable { onPlayToggle() }
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
            if (isCustomFile && File(thumbnailResName).exists()) {
                AsyncImage(
                    model = File(thumbnailResName),
                    contentDescription = "Video Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val resId = remember(thumbnailResName) {
                    val res = context.resources.getIdentifier(thumbnailResName, "drawable", context.packageName)
                    if (res != 0) res else android.R.drawable.ic_menu_gallery
                }
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = "Video Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Center Play / Pause Indicator overlay
        if (!isPlaying || !hasValidVideo) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
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

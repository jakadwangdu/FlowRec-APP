package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecorderState
import com.example.ui.components.formatSeconds
import com.example.ui.theme.AccentRed
import com.example.ui.viewmodel.FlowRecViewModel

@Composable
fun RecordingHudScreen(
    viewModel: FlowRecViewModel,
    onRecordingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        // Confirm stop or minimize
    }

    val context = LocalContext.current
    val recorderState by viewModel.recorderEngine.state.collectAsState()
    val durationSeconds by viewModel.recorderEngine.durationSeconds.collectAsState()
    var isHudMinimized by remember { mutableStateOf(false) }

    val formattedDuration = formatSeconds(durationSeconds)

    // Red recording pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    val mountainThumbRes = remember {
        val res = context.resources.getIdentifier("thumb_mountain", "drawable", context.packageName)
        if (res != 0) res else android.R.drawable.ic_menu_gallery
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Simulated Device/Desktop Screen Background
        Image(
            painter = painterResource(id = mountainThumbRes),
            contentDescription = "Screen content being recorded",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle dark vignette overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        )

        // Top Floating Pill HUD
        AnimatedVisibility(
            visible = !isHudMinimized,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141416).copy(alpha = 0.92f))
                    .border(1.dp, Color(0xFF2C2C30), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("floating_recording_hud"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pulsing Red REC Indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (recorderState == RecorderState.RECORDING)
                                AccentRed.copy(alpha = pulseAlpha)
                            else Color.Gray
                        )
                )

                Text(
                    text = if (recorderState == RecorderState.PAUSED) "PAUSED $formattedDuration" else "REC  $formattedDuration",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Inline Pause/Resume Icon
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF28282C))
                        .clickable {
                            if (recorderState == RecorderState.RECORDING) {
                                viewModel.recorderEngine.pauseRecording()
                            } else {
                                viewModel.recorderEngine.resumeRecording()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (recorderState == RecorderState.PAUSED) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = "Pause or Resume",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Inline Stop Icon
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AccentRed)
                        .clickable {
                            viewModel.recorderEngine.stopRecording("Screen Recording") { newProject ->
                                viewModel.onRecordingFinished(newProject)
                                onRecordingComplete()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop Recording",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Bottom Controls HUD
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            if (isHudMinimized) {
                // Minimized floating bubble
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF141416).copy(alpha = 0.9f))
                        .clickable { isHudMinimized = false }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentRed.copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = "Show FlowRec HUD ($formattedDuration)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color.White
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF141416).copy(alpha = 0.95f))
                        .border(1.dp, Color(0xFF2C2C30), RoundedCornerShape(20.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudActionButton(
                        label = if (recorderState == RecorderState.PAUSED) "Resume" else "Pause",
                        icon = if (recorderState == RecorderState.PAUSED) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        tint = Color.White,
                        onClick = {
                            if (recorderState == RecorderState.RECORDING) {
                                viewModel.recorderEngine.pauseRecording()
                            } else {
                                viewModel.recorderEngine.resumeRecording()
                            }
                        }
                    )

                    HudActionButton(
                        label = "Stop",
                        icon = Icons.Filled.Stop,
                        tint = AccentRed,
                        onClick = {
                            viewModel.recorderEngine.stopRecording("Screen Recording") { newProject ->
                                viewModel.onRecordingFinished(newProject)
                                onRecordingComplete()
                            }
                        }
                    )

                    HudActionButton(
                        label = "Hide",
                        icon = Icons.Filled.VisibilityOff,
                        tint = Color(0xFFAAAAAE),
                        onClick = {
                            isHudMinimized = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HudActionButton(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF242428)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Color.White
        )
    }
}

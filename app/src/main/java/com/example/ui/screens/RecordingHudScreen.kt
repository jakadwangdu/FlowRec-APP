package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
    onBackClick: () -> Unit,
    onRecordingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recorderState by viewModel.recorderEngine.state.collectAsState()
    val durationSeconds by viewModel.recorderEngine.durationSeconds.collectAsState()
    val config by viewModel.recorderEngine.config.collectAsState()

    var isHudMinimized by remember { mutableStateOf(false) }
    var showFacecam by remember { mutableStateOf(false) }
    var micMuted by remember { mutableStateOf(!config.recordMicrophone) }

    // Intercept hardware/system back button to safely return to previous screen
    BackHandler {
        onBackClick()
    }

    val formattedDuration = formatSeconds(durationSeconds)

    // Pulsing REC animation (lightweight tween)
    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    val mountainThumbRes = remember {
        val res = context.resources.getIdentifier("thumb_mountain", "drawable", context.packageName)
        if (res != 0) res else android.R.drawable.ic_menu_gallery
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Horizontal swipe right gesture to go back to previous screen
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount > 40f) {
                        change.consume()
                        onBackClick()
                    }
                }
            }
            .pointerInput(Unit) {
                // Vertical swipe down to minimize HUD
                detectVerticalDragGestures { change, dragAmount ->
                    if (dragAmount > 50f && !isHudMinimized) {
                        change.consume()
                        isHudMinimized = true
                    } else if (dragAmount < -50f && isHudMinimized) {
                        change.consume()
                        isHudMinimized = false
                    }
                }
            },
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // --- 1. FULL SCREEN RECORDING PREVIEW BACKGROUND ---
            Image(
                painter = painterResource(id = mountainThumbRes),
                contentDescription = "Screen content being recorded",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle dark overlay to ensure controls pop clearly
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            // Active Recording Red Border indicator
            if (recorderState == RecorderState.RECORDING) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.5.dp, AccentRed.copy(alpha = 0.7f * pulseAlpha))
                )
            }

            // --- 2. TOP BAR & FLOATING TOP PILL ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button (Top Left)
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141418).copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFF2C2C34), CircleShape)
                        .testTag("btn_recording_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to FlowRec",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Top Floating Pill [ • REC 0:00  ❚❚  ■ ] (As shown in screenshot top bar)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141418).copy(alpha = 0.92f))
                        .border(1.dp, Color(0xFF2E2E38), RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("floating_top_pill"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Pulsing red dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (recorderState == RecorderState.RECORDING) AccentRed.copy(alpha = pulseAlpha)
                                    else Color(0xFFFFB300)
                                )
                        )

                        Text(
                            text = if (recorderState == RecorderState.PAUSED) "PAUSED $formattedDuration" else "REC  $formattedDuration",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        // Mini inline pause icon
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF26262E))
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
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Mini inline stop icon (red)
                        Box(
                            modifier = Modifier
                                .size(26.dp)
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
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Quick Mic Toggle (Top Right)
                IconButton(
                    onClick = {
                        micMuted = !micMuted
                        Toast.makeText(
                            context,
                            if (micMuted) "Microphone muted" else "Microphone recording active",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141418).copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFF2C2C34), CircleShape)
                ) {
                    Icon(
                        imageVector = if (micMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = "Mic Toggle",
                        tint = if (micMuted) Color(0xFF8E8E98) else Color(0xFF64B5F6),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // --- 3. FACECAM PIP PREVIEW (Optional overlay) ---
            if (showFacecam) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 80.dp, end = 16.dp)
                        .size(width = 80.dp, height = 60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E2028).copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Facecam PiP",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Facecam",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            // --- 4. BOTTOM FLOATING DOCK (Exact UI from Screenshot) ---
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                AnimatedVisibility(
                    visible = !isHudMinimized,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    // Floating Rounded Dark Capsule Dock
                    Box(
                        modifier = Modifier
                            .shadow(24.dp, RoundedCornerShape(32.dp), spotColor = Color.Black)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFF141418).copy(alpha = 0.95f))
                            .border(1.dp, Color(0xFF2E2E38), RoundedCornerShape(32.dp))
                            .padding(horizontal = 28.dp, vertical = 14.dp)
                            .testTag("floating_bottom_dock")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(36.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Pause / Resume Button
                            DockActionButton(
                                label = if (recorderState == RecorderState.PAUSED) "Resume" else "Pause",
                                icon = if (recorderState == RecorderState.PAUSED) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                iconTint = Color.White,
                                btnBgColor = Color(0xFF26262E),
                                onClick = {
                                    if (recorderState == RecorderState.RECORDING) {
                                        viewModel.recorderEngine.pauseRecording()
                                    } else {
                                        viewModel.recorderEngine.resumeRecording()
                                    }
                                },
                                tag = "dock_btn_pause"
                            )

                            // 2. Stop Button (Red Stop Square)
                            DockActionButton(
                                label = "Stop",
                                icon = Icons.Filled.Stop,
                                iconTint = AccentRed,
                                btnBgColor = Color(0xFF26262E),
                                onClick = {
                                    viewModel.recorderEngine.stopRecording("Screen Recording") { newProject ->
                                        viewModel.onRecordingFinished(newProject)
                                        onRecordingComplete()
                                    }
                                },
                                tag = "dock_btn_stop"
                            )

                            // 3. Hide Button (Minimize HUD)
                            DockActionButton(
                                label = "Hide",
                                icon = Icons.Filled.VisibilityOff,
                                iconTint = Color.White,
                                btnBgColor = Color(0xFF26262E),
                                onClick = {
                                    isHudMinimized = true
                                },
                                tag = "dock_btn_hide"
                            )
                        }
                    }
                }

                // Minimized Floating Pill Indicator (When Dock is Hidden)
                AnimatedVisibility(
                    visible = isHudMinimized,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color.Black)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF141418).copy(alpha = 0.92f))
                            .border(1.dp, Color(0xFF2E2E38), RoundedCornerShape(24.dp))
                            .clickable { isHudMinimized = false }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                            .testTag("dock_minimized_bubble"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AccentRed.copy(alpha = pulseAlpha))
                            )
                            Text(
                                text = "Show FlowRec Dock ($formattedDuration)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockActionButton(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    btnBgColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(tag)
            .padding(vertical = 2.dp, horizontal = 4.dp)
    ) {
        // Circular Button
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(btnBgColor)
                .border(1.dp, Color(0xFF383842), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Label Below
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Color.White
        )
    }
}


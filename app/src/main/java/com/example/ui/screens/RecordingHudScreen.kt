package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecorderState
import com.example.ui.components.formatSeconds
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentRed
import com.example.ui.viewmodel.FlowRecViewModel
import kotlin.math.sin

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

    var showFacecam by remember { mutableStateOf(true) }
    var micMuted by remember { mutableStateOf(!config.recordMicrophone) }

    // Intercept hardware/system back button to safely return to previous screen
    BackHandler {
        onBackClick()
    }

    val formattedDuration = formatSeconds(durationSeconds)

    // Pulsing REC animation
    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    // Screen scanline / touch cursor animation
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_progress"
    )

    // Audio waveform animation
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0D0D11)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // --- TOP APP BAR WITH BACK BUTTON ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("btn_recording_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to FlowRec",
                            tint = Color.White
                        )
                    }

                    Column {
                        Text(
                            text = "Live Recording Screen",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = if (recorderState == RecorderState.PAUSED) "Recording is Paused" else "Capturing Device Screen",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF9E9EA6)
                        )
                    }
                }

                // Live Status Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (recorderState == RecorderState.RECORDING)
                                AccentRed.copy(alpha = 0.2f)
                            else Color(0xFF2C2C32)
                        )
                        .border(
                            1.dp,
                            if (recorderState == RecorderState.RECORDING) AccentRed.copy(alpha = 0.6f) else Color(0xFF44444C),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("recording_status_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (recorderState == RecorderState.RECORDING) AccentRed.copy(alpha = pulseAlpha)
                                    else Color.Yellow
                                )
                        )
                        Text(
                            text = if (recorderState == RecorderState.PAUSED) "PAUSED  $formattedDuration" else "REC  $formattedDuration",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            // --- MAIN SCROLLABLE CONTENT (Screen Monitor & Stats) ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- LIVE SCREEN RECORDING MONITOR (Device Screen Frame) ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = AccentRed.copy(alpha = 0.3f))
                        .testTag("screen_recording_monitor_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF16161B)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(
                            listOf(
                                if (recorderState == RecorderState.RECORDING) AccentRed.copy(alpha = 0.8f) else Color(0xFF383842),
                                Color(0xFF24242C)
                            )
                        ),
                        width = 1.5.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Monitor Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhoneAndroid,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "SCREEN FEED MONITOR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFFB0B0BA)
                                )
                            }

                            Text(
                                text = "${config.resolution.label} • ${config.frameRate.label}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = AccentBlue
                            )
                        }

                        // Simulated Screen Canvas Display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9.5f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F1015))
                                .border(1.dp, Color(0xFF282832), RoundedCornerShape(14.dp))
                        ) {
                            // Dynamic Animated Screen Representation
                            Canvas(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val w = size.width
                                val h = size.height

                                // Draw subtle background grid
                                val gridSpacing = 28f
                                var x = 0f
                                while (x < w) {
                                    drawLine(
                                        color = Color(0xFF1E202C),
                                        start = Offset(x, 0f),
                                        end = Offset(x, h),
                                        strokeWidth = 1f
                                    )
                                    x += gridSpacing
                                }
                                var y = 0f
                                while (y < h) {
                                    drawLine(
                                        color = Color(0xFF1E202C),
                                        start = Offset(0f, y),
                                        end = Offset(w, y),
                                        strokeWidth = 1f
                                    )
                                    y += gridSpacing
                                }

                                // Animated scanning beam across the screen
                                val scanY = scanProgress * h
                                drawLine(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            AccentBlue.copy(alpha = 0.35f),
                                            Color.Transparent
                                        ),
                                        startY = scanY - 30f,
                                        endY = scanY + 30f
                                    ),
                                    start = Offset(0f, scanY),
                                    end = Offset(w, scanY),
                                    strokeWidth = 6f
                                )

                                // Draw live audio waveform at the bottom of the screen
                                if (recorderState == RecorderState.RECORDING && !micMuted) {
                                    val waveY = h - 24f
                                    val points = 32
                                    for (i in 0 until points - 1) {
                                        val x1 = (i.toFloat() / points) * w
                                        val x2 = ((i + 1).toFloat() / points) * w
                                        val offset1 = sin(i * 0.6f + wavePhase) * 12f
                                        val offset2 = sin((i + 1) * 0.6f + wavePhase) * 12f
                                        drawLine(
                                            color = AccentBlue.copy(alpha = 0.7f),
                                            start = Offset(x1, waveY + offset1),
                                            end = Offset(x2, waveY + offset2),
                                            strokeWidth = 2.5f,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }

                                // Simulated cursor touch point moving across the screen
                                val cursorX = w * (0.3f + 0.4f * sin(scanProgress * 6.28f).toFloat())
                                val cursorY = h * (0.4f + 0.3f * sin(scanProgress * 3.14f).toFloat())

                                drawCircle(
                                    color = AccentBlue.copy(alpha = 0.25f),
                                    radius = 18f,
                                    center = Offset(cursorX, cursorY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 5f,
                                    center = Offset(cursorX, cursorY)
                                )
                            }

                            // Simulated App Screen Interface mockup inside monitor
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top mini status bar of recorded device
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "FlowRec Live Feed",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFFB4B4C0)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (recorderState == RecorderState.RECORDING) AccentRed else Color.Gray)
                                        )
                                        Text(
                                            text = formattedDuration,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }

                                // Center graphic watermark / active recording indicator
                                Column(
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (recorderState == RecorderState.RECORDING) "CAPTURING SCREEN IN HIGH DEFINITION" else "RECORDING PAUSED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.2.sp
                                        ),
                                        color = if (recorderState == RecorderState.RECORDING) Color(0xFF00E5FF) else Color(0xFFFFD54F)
                                    )
                                    Text(
                                        text = "Touch actions, window audio & display are being saved",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp
                                        ),
                                        color = Color(0xFF7E8090)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            // Facecam / Webcam PiP Preview in top-right of the recording monitor
                            if (showFacecam) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .size(width = 68.dp, height = 50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E2028))
                                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
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
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Facecam",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- LIVE RECORDING STATS & TELEMETRY ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF181820)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(Color(0xFF2C2C38), Color(0xFF1C1C24)))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(
                            label = "FPS",
                            value = "${config.frameRate.fps}",
                            subtext = "Stable"
                        )
                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFF2A2A34)))
                        StatItem(
                            label = "Audio",
                            value = if (micMuted) "Muted" else "Mic On",
                            subtext = if (micMuted) "Silent" else "48 kHz"
                        )
                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFF2A2A34)))
                        StatItem(
                            label = "Quality",
                            value = config.resolution.label,
                            subtext = "MP4 / H.264"
                        )
                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFF2A2A34)))
                        StatItem(
                            label = "Est. Size",
                            value = "${((durationSeconds * 1.8f) + 4.5f).toInt()} MB",
                            subtext = "Storage OK"
                        )
                    }
                }

                // --- LIVE TOGGLE TOOLS (Facecam & Mic Quick Actions) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Facecam PiP Toggle
                    QuickToolChip(
                        title = if (showFacecam) "Facecam ON" else "Facecam OFF",
                        icon = if (showFacecam) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                        isActive = showFacecam,
                        onClick = { showFacecam = !showFacecam },
                        modifier = Modifier.weight(1f)
                    )

                    // Mic Audio Toggle
                    QuickToolChip(
                        title = if (micMuted) "Mic Muted" else "Mic Recording",
                        icon = if (micMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        isActive = !micMuted,
                        onClick = {
                            micMuted = !micMuted
                            Toast.makeText(
                                context,
                                if (micMuted) "Microphone audio muted" else "Microphone audio enabled",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Helpful notice explaining background recording
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF141924)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF1E3A5F), Color(0xFF172030)))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Tap the Back button (<) anytime to navigate the app, or press Home to capture other applications. Recording continues seamlessly.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            ),
                            color = Color(0xFFB0C4DE)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // --- BOTTOM PRIMARY RECORDING CONTROLS ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF121217),
                tonalElevation = 8.dp,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(listOf(Color(0xFF282834), Color(0xFF121217)))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Pause / Resume Button
                    ControlActionColumn(
                        label = if (recorderState == RecorderState.PAUSED) "Resume" else "Pause",
                        icon = if (recorderState == RecorderState.PAUSED) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        btnColor = Color(0xFF252530),
                        iconTint = Color.White,
                        onClick = {
                            if (recorderState == RecorderState.RECORDING) {
                                viewModel.recorderEngine.pauseRecording()
                            } else {
                                viewModel.recorderEngine.resumeRecording()
                            }
                        },
                        tag = "btn_hud_pause_resume"
                    )

                    // 2. Stop & Save Button (Large Red Center Action)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                viewModel.recorderEngine.stopRecording("Screen Recording") { newProject ->
                                    viewModel.onRecordingFinished(newProject)
                                    onRecordingComplete()
                                }
                            }
                            .testTag("btn_hud_stop_save")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(AccentRed)
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Stop Recording",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Stop & Save",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = AccentRed
                        )
                    }

                    // 3. Back to App / Minimize Button
                    ControlActionColumn(
                        label = "Back to App",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        btnColor = Color(0xFF252530),
                        iconTint = Color(0xFFB0B0BA),
                        onClick = onBackClick,
                        tag = "btn_hud_back_to_app"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    subtext: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF888894)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            color = Color.White
        )
        Text(
            text = subtext,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp
            ),
            color = Color(0xFF6E6E7A)
        )
    }
}

@Composable
private fun QuickToolChip(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) Color(0xFF1E2638) else Color(0xFF181820))
            .border(
                1.dp,
                if (isActive) Color(0xFF3B82F6).copy(alpha = 0.6f) else Color(0xFF282832),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) Color(0xFF60A5FA) else Color(0xFF8E8E98),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = if (isActive) Color.White else Color(0xFF9E9EA8)
            )
        }
    }
}

@Composable
private fun ControlActionColumn(
    label: String,
    icon: ImageVector,
    btnColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(btnColor)
                .border(1.dp, Color(0xFF383844), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
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
            color = iconTint
        )
    }
}


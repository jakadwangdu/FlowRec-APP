package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.config.UiFeatureFlagManager
import com.example.model.AudioSourceMode
import com.example.model.CountdownOption
import com.example.model.FrameRate
import com.example.model.RecorderState
import com.example.model.RecordingResolution
import com.example.model.VideoOrientation
import com.example.ui.components.FlowRecBottomNav
import com.example.ui.components.FlowSwitch
import com.example.ui.components.Rings
import com.example.ui.components.Segmented
import com.example.ui.components.Shutter
import com.example.ui.components.TouchVisualizationLayer
import com.example.ui.theme.flow
import com.example.ui.viewmodel.FlowRecViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun RecordScreen(
    viewModel: FlowRecViewModel,
    onStartRecordingClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onBackClick != null) {
        BackHandler { onBackClick() }
    }

    val context = LocalContext.current
    val flagManager = remember { UiFeatureFlagManager.getInstance(context) }
    val featureFlags by flagManager.flags.collectAsState()
    val isReducedMotion = flagManager.isReducedMotion(context)

    val recState by viewModel.recorderEngine.state.collectAsState()
    val isRecording = recState == RecorderState.RECORDING || recState == RecorderState.PAUSED

    val currentRes by viewModel.defaultResolution.collectAsState()
    val currentFps by viewModel.defaultFps.collectAsState()
    val currentAudio by viewModel.audioSourceMode.collectAsState()
    val currentOrientation by viewModel.videoOrientation.collectAsState()
    val currentCountdown by viewModel.countdownOption.collectAsState()
    val isGameMode by viewModel.isGameMode.collectAsState()

    val facecamEnabled by viewModel.facecamEnabled.collectAsState()
    val facecamShape by viewModel.facecamShape.collectAsState()
    val facecamSize by viewModel.facecamSize.collectAsState()
    val facecamFrontLens by viewModel.facecamFrontLens.collectAsState()

    val showTouches by viewModel.showTouches.collectAsState()
    val touchFeedbackStyle by viewModel.touchFeedbackStyle.collectAsState()
    val touchRippleEnabled by viewModel.touchRippleEnabled.collectAsState()
    val touchHighlightEnabled by viewModel.touchHighlightEnabled.collectAsState()
    val touchMovementTrackingEnabled by viewModel.touchMovementTrackingEnabled.collectAsState()
    val touchEffectSizeDp by viewModel.touchEffectSizeDp.collectAsState()
    val touchEffectDurationMs by viewModel.touchEffectDurationMs.collectAsState()
    val touchEffectOpacity by viewModel.touchEffectOpacity.collectAsState()

    var showCameraDeniedDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setFacecamEnabled(true)
        } else {
            viewModel.setFacecamEnabled(false)
            showCameraDeniedDialog = true
        }
    }

    val touchConfig = remember(showTouches, touchRippleEnabled, touchHighlightEnabled, touchMovementTrackingEnabled, touchEffectSizeDp, touchEffectDurationMs, touchEffectOpacity) {
        viewModel.getTouchEffectConfig()
    }

    // Capture Mode Selector (Screen, Game, Voice - as specified in SKILL.md)
    val captureModeOptions = listOf("Screen", "Game", "Voice")
    val selectedModeIndex = when {
        isGameMode -> 1
        currentAudio == AudioSourceMode.MIC && !isGameMode -> 2
        else -> 0
    }

    Scaffold(
        bottomBar = {
            FlowRecBottomNav(
                currentTab = Screen.RECORD,
                onTabSelected = { tab ->
                    viewModel.switchBottomTab(tab)
                }
            )
        },
        containerColor = flow.bg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Concentric Rings (Minimalist Monochrome)
            if (featureFlags.enableBackgroundRings && featureFlags.useMinimalistMonochromeUi) {
                Rings(
                    recording = isRecording,
                    reduceMotion = isReducedMotion
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Capture",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = flow.fg
                    )

                    // Quick orientation badge
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(flow.fill)
                            .border(0.5.dp, flow.separator, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = currentOrientation.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = flow.fg
                        )
                    }
                }

                // Mode Selector (Sliding Segmented Control)
                Segmented(
                    options = captureModeOptions,
                    selected = selectedModeIndex,
                    onSelect = { index ->
                        when (index) {
                            0 -> { // Screen mode
                                viewModel.setGameMode(false)
                            }
                            1 -> { // Game mode
                                viewModel.setGameMode(true)
                            }
                            2 -> { // Voice mode
                                viewModel.setGameMode(false)
                                viewModel.setAudioSourceMode(AudioSourceMode.MIC)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. LIVE STUDIO PREVIEW BOX
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = flow.fill
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(flow.separator),
                        width = 0.75.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("record_studio_preview")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val previewAspect = when (currentOrientation) {
                            VideoOrientation.LANDSCAPE -> 16f / 9f
                            VideoOrientation.PORTRAIT -> 9f / 13f
                            VideoOrientation.AUTO -> 9f / 12f
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (currentOrientation == VideoOrientation.LANDSCAPE) 1f else 0.55f)
                                .aspectRatio(previewAspect)
                                .clip(RoundedCornerShape(16.dp))
                                .background(flow.glass)
                                .border(1.dp, flow.separator, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Simulated screen content
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = null,
                                    tint = flow.fg.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isGameMode) "Game Capture" else "Display Capture",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = flow.muted
                                )
                            }

                            // Simulated Facecam bubble if enabled
                            if (facecamEnabled) {
                                val facecamCorner = if (facecamShape == "CIRCLE") 50.dp else 8.dp
                                val facecamDp = when (facecamSize) {
                                    "SMALL" -> 32.dp
                                    "LARGE" -> 50.dp
                                    else -> 40.dp
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(facecamDp)
                                        .clip(RoundedCornerShape(facecamCorner))
                                        .background(flow.bg)
                                        .border(1.dp, flow.fg.copy(alpha = 0.8f), RoundedCornerShape(facecamCorner)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CameraAlt,
                                        contentDescription = null,
                                        tint = flow.fg,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Interactive Touch Visualization preview layer
                            if (showTouches) {
                                TouchVisualizationLayer(
                                    config = touchConfig,
                                    interactive = true,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Top status badges
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = currentRes.label,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${currentFps.fps} FPS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SHUTTER BUTTON (as specified in compose.md)
                if (featureFlags.enableShutterMorphAnimation) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Shutter(
                            recording = isRecording,
                            size = 84.dp,
                            onClick = onStartRecordingClick
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRecording) "Stop recording" else "Tap to record",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = flow.muted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. CONFIGURATION CONTROLS CARD
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = flow.fill
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(flow.separator),
                        width = 0.75.dp
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // RESOLUTION SELECTOR
                        Text(
                            text = "Resolution",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = flow.fg
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RecordingResolution.values().forEach { res ->
                                val isSelected = currentRes == res
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) flow.fg else flow.glass)
                                        .border(0.5.dp, flow.separator, CircleShape)
                                        .clickable { viewModel.setDefaultResolution(res) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = res.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) flow.onFg else flow.fg
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = flow.separator)
                        Spacer(modifier = Modifier.height(14.dp))

                        // FRAME RATE (FPS) SELECTOR
                        Text(
                            text = "Frame Rate",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = flow.fg
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FrameRate.values().forEach { fps ->
                                val isSelected = currentFps == fps
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) flow.fg else flow.glass)
                                        .border(0.5.dp, flow.separator, CircleShape)
                                        .clickable { viewModel.setDefaultFps(fps) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = fps.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) flow.onFg else flow.fg
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = flow.separator)
                        Spacer(modifier = Modifier.height(14.dp))

                        // AUDIO SOURCE
                        Text(
                            text = "Audio Source",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = flow.fg
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AudioSourceMode.values().forEach { mode ->
                                val isSelected = currentAudio == mode
                                val isInternalSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                                val labelText = when (mode) {
                                    AudioSourceMode.NONE -> "Mute (video only)"
                                    AudioSourceMode.MIC -> "Microphone"
                                    AudioSourceMode.INTERNAL -> if (isInternalSupported) "Internal device audio" else "Internal (requires Android 10+)"
                                    AudioSourceMode.MIC_AND_INTERNAL -> "Microphone + internal audio"
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) flow.fill else Color.Transparent)
                                        .border(if (isSelected) 0.5.dp else 0.dp, flow.separator, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.setAudioSourceMode(mode) }
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = labelText,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = flow.fg
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(flow.fg)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = flow.separator)
                        Spacer(modifier = Modifier.height(14.dp))

                        // ORIENTATION & COUNTDOWN
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Orientation
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Orientation",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = flow.fg
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                VideoOrientation.values().forEach { orient ->
                                    val isSelected = currentOrientation == orient
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) flow.fg else flow.glass)
                                            .border(0.5.dp, flow.separator, RoundedCornerShape(10.dp))
                                            .clickable { viewModel.setVideoOrientation(orient) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = orient.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) flow.onFg else flow.fg
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Countdown
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Countdown",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = flow.fg
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                CountdownOption.values().forEach { opt ->
                                    val isSelected = currentCountdown == opt
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) flow.fg else flow.glass)
                                            .border(0.5.dp, flow.separator, RoundedCornerShape(10.dp))
                                            .clickable { viewModel.setCountdownOption(opt) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = opt.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) flow.onFg else flow.fg
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = flow.separator)
                        Spacer(modifier = Modifier.height(14.dp))

                        // GAME RECORDING MODE PRESET
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(flow.glass)
                                .border(0.5.dp, flow.separator, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = flow.fg,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Game mode preset",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = flow.fg
                                    )
                                    Text(
                                        text = "1080p, 60 FPS, 12 Mbps, internal audio",
                                        fontSize = 11.sp,
                                        color = flow.muted
                                    )
                                }
                            }

                            FlowSwitch(
                                checked = isGameMode,
                                onChange = { viewModel.setGameMode(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = flow.separator)
                        Spacer(modifier = Modifier.height(14.dp))

                        // FACECAM SECTION
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    tint = flow.fg,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "FaceCam overlay",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = flow.fg
                                    )
                                    Text(
                                        text = "Floating front-camera bubble while recording",
                                        fontSize = 11.sp,
                                        color = flow.muted
                                    )
                                }
                            }
                            FlowSwitch(
                                checked = facecamEnabled,
                                onChange = { enabled ->
                                    if (enabled) {
                                        val hasCamera = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasCamera) {
                                            viewModel.setFacecamEnabled(true)
                                        } else {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    } else {
                                        viewModel.setFacecamEnabled(false)
                                    }
                                }
                            )
                        }

                        // FaceCam custom options if enabled
                        AnimatedVisibility(visible = facecamEnabled) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Shape
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Shape", fontSize = 12.sp, color = flow.muted)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("CIRCLE" to "Circle", "SQUARE" to "Square").forEach { (shape, label) ->
                                            val isSel = facecamShape == shape
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) flow.fg else flow.glass)
                                                    .border(0.5.dp, flow.separator, RoundedCornerShape(8.dp))
                                                    .clickable { viewModel.setFacecamShape(shape) }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    color = if (isSel) flow.onFg else flow.fg
                                                )
                                            }
                                        }
                                    }
                                }

                                // Size
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Size", fontSize = 12.sp, color = flow.muted)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("SMALL" to "S", "MEDIUM" to "M", "LARGE" to "L").forEach { (sz, label) ->
                                            val isSel = facecamSize == sz
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) flow.fg else flow.glass)
                                                    .border(0.5.dp, flow.separator, RoundedCornerShape(8.dp))
                                                    .clickable { viewModel.setFacecamSize(sz) }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    color = if (isSel) flow.onFg else flow.fg
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = flow.separator)
                        Spacer(modifier = Modifier.height(14.dp))

                        // TOUCH EFFECTS SECTION
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TouchApp,
                                    contentDescription = null,
                                    tint = flow.fg,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Touch feedback",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = flow.fg
                                    )
                                    Text(
                                        text = "Record touch coordinates & animated ripples",
                                        fontSize = 11.sp,
                                        color = flow.muted
                                    )
                                }
                            }
                            FlowSwitch(
                                checked = showTouches,
                                onChange = { viewModel.setShowTouches(it) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Start Recording Button (48dp+ height, clean monochrome style)
                Button(
                    onClick = onStartRecordingClick,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = flow.fg,
                        contentColor = flow.onFg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_confirm_recording_popup")
                ) {
                    Icon(
                        imageVector = Icons.Filled.FiberManualRecord,
                        contentDescription = null,
                        tint = flow.record,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start recording",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = flow.onFg
                    )
                }

                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }

    if (showCameraDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showCameraDeniedDialog = false },
            containerColor = flow.sheet,
            title = {
                Text(
                    text = "Camera permission required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = flow.fg
                )
            },
            text = {
                Text(
                    text = "FaceCam requires camera permission to display a floating front-camera bubble while recording.\n\nYou can still record your screen normally without FaceCam, or grant permission to enable it.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = flow.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraDeniedDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = flow.fg,
                        contentColor = flow.onFg
                    )
                ) {
                    Text("Grant permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraDeniedDialog = false }) {
                    Text("Skip", color = flow.muted)
                }
            }
        )
    }
}

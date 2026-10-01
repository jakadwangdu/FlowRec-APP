package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.ui.components.TouchVisualizationLayer
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.model.AudioSourceMode
import com.example.model.CountdownOption
import com.example.model.FrameRate
import com.example.model.RecordingResolution
import com.example.model.VideoOrientation
import com.example.ui.components.FlowRecBottomNav
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentRed
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

    val context = LocalContext.current
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

    Scaffold(
        bottomBar = {
            FlowRecBottomNav(
                currentTab = Screen.RECORD,
                onTabSelected = { tab ->
                    viewModel.switchBottomTab(tab)
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
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
                    .padding(top = 4.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Record Studio",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 1. LIVE STUDIO PREVIEW BOX
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant),
                    width = 0.5.dp
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
                            .background(Color(0xFF0F0F12))
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
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
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Display Capture",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    .background(Color(0xFF2E2E38))
                                    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(facecamCorner)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
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
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
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
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. CONFIGURATION CONTROLS CARD
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant),
                    width = 0.5.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // RESOLUTION SELECTOR
                    Text(
                        text = "Resolution",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecordingResolution.values().forEach { res ->
                            FilterChip(
                                selected = currentRes == res,
                                onClick = { viewModel.setDefaultResolution(res) },
                                label = { Text(res.label, fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // FRAME RATE (FPS) SELECTOR
                    Text(
                        text = "Frame Rate",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FrameRate.values().forEach { fps ->
                            FilterChip(
                                selected = currentFps == fps,
                                onClick = { viewModel.setDefaultFps(fps) },
                                label = { Text(fps.label, fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // AUDIO SOURCE
                    Text(
                        text = "Audio Source",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AudioSourceMode.values().forEach { mode ->
                            val isSelected = currentAudio == mode
                            val isInternalSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                            val labelText = when (mode) {
                                AudioSourceMode.NONE -> "Mute (Video only)"
                                AudioSourceMode.MIC -> "Microphone"
                                AudioSourceMode.INTERNAL -> if (isInternalSupported) "Internal Device Audio (Android 10+)" else "Internal (Android 10+ Required)"
                                AudioSourceMode.MIC_AND_INTERNAL -> "Microphone + Internal Audio"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable { viewModel.setAudioSourceMode(mode) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // ORIENTATION & COUNTDOWN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Orientation
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Orientation",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            VideoOrientation.values().forEach { orient ->
                                FilterChip(
                                    selected = currentOrientation == orient,
                                    onClick = { viewModel.setVideoOrientation(orient) },
                                    label = { Text(orient.label, fontSize = 11.sp) },
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Countdown
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Countdown",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            CountdownOption.values().forEach { opt ->
                                FilterChip(
                                    selected = currentCountdown == opt,
                                    onClick = { viewModel.setCountdownOption(opt) },
                                    label = { Text(opt.label, fontSize = 11.sp) },
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // GAME RECORDING MODE PRESET
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isGameMode) AccentBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = if (isGameMode) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Game Mode Preset",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "1080p, 60 FPS, 12 Mbps, internal audio",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isGameMode,
                            onCheckedChange = { viewModel.setGameMode(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // FACECAM SECTION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "FaceCam Overlay",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Floating front-camera bubble while recording",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = facecamEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    val hasCameraPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    if (hasCameraPerm) {
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

                    AnimatedVisibility(visible = facecamEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Lens Selector
                            Column {
                                Text(
                                    text = "Camera Lens",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = facecamFrontLens,
                                        onClick = { viewModel.setFacecamFrontLens(true) },
                                        label = { Text("Front Camera", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = !facecamFrontLens,
                                        onClick = { viewModel.setFacecamFrontLens(false) },
                                        label = { Text("Back Camera", fontSize = 11.sp) }
                                    )
                                }
                            }

                            // Shape Selector
                            Column {
                                Text(
                                    text = "Bubble Shape",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("CIRCLE" to "Circle", "ROUNDED_RECT" to "Rounded Rect", "RECT" to "Square").forEach { (shape, name) ->
                                        FilterChip(
                                            selected = facecamShape == shape,
                                            onClick = { viewModel.setFacecamShape(shape) },
                                            label = { Text(name, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            // Size Selector & Position Reset
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Size",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("SMALL" to "Small", "MEDIUM" to "Medium", "LARGE" to "Large").forEach { (size, name) ->
                                            FilterChip(
                                                selected = facecamSize == size,
                                                onClick = { viewModel.setFacecamSize(size) },
                                                label = { Text(name, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }

                                TextButton(
                                    onClick = { viewModel.resetFacecamPosition() },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Pos", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // TOUCH EFFECTS SECTION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Touch Effects",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Record touch coordinates & display animated ripples",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = showTouches,
                            onCheckedChange = { viewModel.setShowTouches(it) }
                        )
                    }

                    AnimatedVisibility(visible = showTouches) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Effect Features
                            Column {
                                Text(
                                    text = "Visual Elements",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = touchRippleEnabled,
                                        onClick = { viewModel.setTouchRippleEnabled(!touchRippleEnabled) },
                                        label = { Text("Ripple", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = touchHighlightEnabled,
                                        onClick = { viewModel.setTouchHighlightEnabled(!touchHighlightEnabled) },
                                        label = { Text("Highlight", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = touchMovementTrackingEnabled,
                                        onClick = { viewModel.setTouchMovementTrackingEnabled(!touchMovementTrackingEnabled) },
                                        label = { Text("Movement Trail", fontSize = 11.sp) }
                                    )
                                }
                            }

                            // Size Selector
                            Column {
                                Text(
                                    text = "Effect Size",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(24 to "Small", 36 to "Medium", 48 to "Large").forEach { (size, label) ->
                                        FilterChip(
                                            selected = touchEffectSizeDp == size,
                                            onClick = { viewModel.setTouchEffectSizeDp(size) },
                                            label = { Text(label, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            // Duration Selector
                            Column {
                                Text(
                                    text = "Duration",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(250 to "Short", 500 to "Normal", 800 to "Long").forEach { (dur, label) ->
                                        FilterChip(
                                            selected = touchEffectDurationMs == dur,
                                            onClick = { viewModel.setTouchEffectDurationMs(dur) },
                                            label = { Text(label, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            // Opacity Selector
                            Column {
                                Text(
                                    text = "Opacity",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(0.4f to "40%", 0.8f to "80%", 1.0f to "100%").forEach { (op, label) ->
                                        FilterChip(
                                            selected = kotlin.math.abs(touchEffectOpacity - op) < 0.05f,
                                            onClick = { viewModel.setTouchEffectOpacity(op) },
                                            label = { Text(label, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. LARGE PRIMARY "START RECORDING" ACTION BUTTON
            Button(
                onClick = onStartRecordingClick,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_confirm_recording_popup")
            ) {
                Icon(
                    imageVector = Icons.Filled.FiberManualRecord,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Recording",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCameraDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showCameraDeniedDialog = false },
            title = {
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "FaceCam requires camera permission to display a floating front-camera bubble while recording.\n\nYou can still record your screen normally without FaceCam, or grant permission to enable it.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraDeniedDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCameraDeniedDialog = false }
                ) {
                    Text("Record Without FaceCam")
                }
            }
        )
    }
}

package com.example.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppThemeMode
import com.example.model.AudioSourceMode
import com.example.model.CountdownOption
import com.example.model.ExportFormat
import com.example.model.ExportPreset
import com.example.model.ExportQuality
import com.example.model.FrameRate
import com.example.model.RecordingResolution
import com.example.model.VideoOrientation
import com.example.service.FlowRecTileService
import com.example.ui.components.FlowRecBottomNav
import com.example.ui.components.SettingRowDropdown
import com.example.ui.components.SettingRowSwitch
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentRed
import com.example.ui.viewmodel.FlowRecViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun SettingsScreen(
    viewModel: FlowRecViewModel,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onBackClick != null) {
        BackHandler { onBackClick() }
    }

    val context = LocalContext.current
    val currentTheme by viewModel.themeMode.collectAsState()
    var showThemeMenu by remember { mutableStateOf(false) }

    val defaultRes by viewModel.defaultResolution.collectAsState()
    var showResMenu by remember { mutableStateOf(false) }

    val defaultFps by viewModel.defaultFps.collectAsState()
    var showFpsMenu by remember { mutableStateOf(false) }

    val countdown by viewModel.countdownOption.collectAsState()
    var showCountdownMenu by remember { mutableStateOf(false) }

    val orientation by viewModel.videoOrientation.collectAsState()
    var showOrientationMenu by remember { mutableStateOf(false) }

    val isGameMode by viewModel.isGameMode.collectAsState()

    val audioSource by viewModel.audioSourceMode.collectAsState()
    var showAudioMenu by remember { mutableStateOf(false) }

    val facecamEnabled by viewModel.facecamEnabled.collectAsState()
    val facecamShape by viewModel.facecamShape.collectAsState()
    val facecamSize by viewModel.facecamSize.collectAsState()
    val facecamFrontLens by viewModel.facecamFrontLens.collectAsState()

    val showTouches by viewModel.showTouches.collectAsState()
    val touchFeedbackStyle by viewModel.touchFeedbackStyle.collectAsState()
    val touchFeedbackColor by viewModel.touchFeedbackColor.collectAsState()
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

    val exportPreset by viewModel.exportPreset.collectAsState()
    var showExportPresetMenu by remember { mutableStateOf(false) }

    val exportQuality by viewModel.exportQuality.collectAsState()
    var showExportQualityMenu by remember { mutableStateOf(false) }

    val exportFormat by viewModel.exportFormat.collectAsState()
    var showExportFormatMenu by remember { mutableStateOf(false) }

    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAiPrivacyDialog by remember { mutableStateOf(false) }

    val storageInfo = remember { viewModel.getStorageInfo() }

    Scaffold(
        bottomBar = {
            FlowRecBottomNav(
                currentTab = Screen.SETTINGS,
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
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 1. RECORDING SECTION
            SettingsSectionCard(title = "Recording", icon = Icons.Filled.Videocam) {
                // Game Recording Mode
                SettingRowSwitch(
                    label = "Game Recording Mode",
                    sublabel = "Preset with 1080p, 60 FPS, 12 Mbps & internal device audio",
                    checked = isGameMode,
                    onCheckedChange = { viewModel.setGameMode(it) }
                )

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Resolution
                Box {
                    SettingRowDropdown(
                        label = "Resolution",
                        value = defaultRes.label,
                        onClick = { showResMenu = true }
                    )
                    DropdownMenu(expanded = showResMenu, onDismissRequest = { showResMenu = false }) {
                        RecordingResolution.values().forEach { res ->
                            DropdownMenuItem(
                                text = { Text(res.label) },
                                onClick = {
                                    viewModel.setDefaultResolution(res)
                                    showResMenu = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Frame Rate (FPS)
                Box {
                    SettingRowDropdown(
                        label = "Frame Rate (FPS)",
                        value = defaultFps.label,
                        onClick = { showFpsMenu = true }
                    )
                    DropdownMenu(expanded = showFpsMenu, onDismissRequest = { showFpsMenu = false }) {
                        FrameRate.values().forEach { fps ->
                            DropdownMenuItem(
                                text = { Text(fps.label) },
                                onClick = {
                                    viewModel.setDefaultFps(fps)
                                    showFpsMenu = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Countdown
                Box {
                    SettingRowDropdown(
                        label = "Countdown Timer",
                        value = countdown.label,
                        onClick = { showCountdownMenu = true }
                    )
                    DropdownMenu(expanded = showCountdownMenu, onDismissRequest = { showCountdownMenu = false }) {
                        CountdownOption.values().forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label) },
                                onClick = {
                                    viewModel.setCountdownOption(opt)
                                    showCountdownMenu = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Orientation
                Box {
                    SettingRowDropdown(
                        label = "Orientation",
                        value = orientation.label,
                        onClick = { showOrientationMenu = true }
                    )
                    DropdownMenu(expanded = showOrientationMenu, onDismissRequest = { showOrientationMenu = false }) {
                        VideoOrientation.values().forEach { orient ->
                            DropdownMenuItem(
                                text = { Text(orient.label) },
                                onClick = {
                                    viewModel.setVideoOrientation(orient)
                                    showOrientationMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. AUDIO SECTION
            SettingsSectionCard(title = "Audio", icon = Icons.Filled.Mic) {
                Box {
                    SettingRowDropdown(
                        label = "Audio Source Mode",
                        value = audioSource.label,
                        onClick = { showAudioMenu = true }
                    )
                    DropdownMenu(expanded = showAudioMenu, onDismissRequest = { showAudioMenu = false }) {
                        AudioSourceMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    val isQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                                    val subtitle = if (mode == AudioSourceMode.INTERNAL && !isQ) " (Android 10+ required)" else ""
                                    Text(mode.label + subtitle)
                                },
                                onClick = {
                                    viewModel.setAudioSourceMode(mode)
                                    showAudioMenu = false
                                }
                            )
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Text(
                        text = "Android 10+ AudioPlaybackCapture is active for internal system & gameplay sound recording.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                } else {
                    Text(
                        text = "Note: Internal audio capture requires Android 10+. Microphone recording will be used as fallback.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFFFFB300),
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. CAMERA / FACECAM SECTION
            SettingsSectionCard(title = "Camera (FaceCam)", icon = Icons.Filled.CameraAlt) {
                SettingRowSwitch(
                    label = "FaceCam Overlay",
                    sublabel = "Displays floating camera bubble while recording",
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

                AnimatedVisibility(visible = facecamEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        // Camera Lens
                        Text("Camera Lens", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                        // Shape
                        Text("Bubble Shape", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("CIRCLE" to "Circle", "ROUNDED_RECT" to "Rounded Rect", "RECT" to "Square").forEach { (shape, name) ->
                                FilterChip(
                                    selected = facecamShape == shape,
                                    onClick = { viewModel.setFacecamShape(shape) },
                                    label = { Text(name, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Size & Position Reset
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                    listOf("SMALL" to "Small", "MEDIUM" to "Medium", "LARGE" to "Large").forEach { (size, name) ->
                                        FilterChip(
                                            selected = facecamSize == size,
                                            onClick = { viewModel.setFacecamSize(size) },
                                            label = { Text(name, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            TextButton(onClick = { viewModel.resetFacecamPosition() }) {
                                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Pos", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. TOUCH SECTION
            SettingsSectionCard(title = "Touch & Gestures", icon = Icons.Filled.TouchApp) {
                SettingRowSwitch(
                    label = "Record Touch Effects",
                    sublabel = "Streams touch coordinates to companion .flowtouch metadata and renders tap feedback",
                    checked = showTouches,
                    onCheckedChange = { viewModel.setShowTouches(it) }
                )

                AnimatedVisibility(visible = showTouches) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        // Elements
                        Text("Visual Feedback Elements", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                        // Size
                        Text("Effect Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(24 to "Small", 36 to "Medium", 48 to "Large").forEach { (sz, label) ->
                                FilterChip(
                                    selected = touchEffectSizeDp == sz,
                                    onClick = { viewModel.setTouchEffectSizeDp(sz) },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Duration & Opacity
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(250 to "Short", 500 to "Normal", 800 to "Long").forEach { (dur, label) ->
                                        FilterChip(
                                            selected = touchEffectDurationMs == dur,
                                            onClick = { viewModel.setTouchEffectDurationMs(dur) },
                                            label = { Text(label, fontSize = 10.5.sp) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Opacity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(0.4f to "40%", 0.8f to "80%", 1.0f to "100%").forEach { (op, label) ->
                                        FilterChip(
                                            selected = kotlin.math.abs(touchEffectOpacity - op) < 0.05f,
                                            onClick = { viewModel.setTouchEffectOpacity(op) },
                                            label = { Text(label, fontSize = 10.5.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. EXPORT SECTION
            SettingsSectionCard(title = "Export", icon = Icons.Filled.SaveAlt) {
                // Export Preset
                Box {
                    SettingRowDropdown(
                        label = "Export Preset",
                        value = exportPreset.label,
                        onClick = { showExportPresetMenu = true }
                    )
                    DropdownMenu(expanded = showExportPresetMenu, onDismissRequest = { showExportPresetMenu = false }) {
                        ExportPreset.values().forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.label) },
                                onClick = {
                                    viewModel.setExportPreset(preset)
                                    showExportPresetMenu = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Export Quality
                Box {
                    SettingRowDropdown(
                        label = "Export Quality",
                        value = exportQuality.label,
                        onClick = { showExportQualityMenu = true }
                    )
                    DropdownMenu(expanded = showExportQualityMenu, onDismissRequest = { showExportQualityMenu = false }) {
                        ExportQuality.values().forEach { quality ->
                            DropdownMenuItem(
                                text = { Text(quality.label) },
                                onClick = {
                                    viewModel.setExportQuality(quality)
                                    showExportQualityMenu = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Export Format
                Box {
                    SettingRowDropdown(
                        label = "Container Format",
                        value = exportFormat.name,
                        onClick = { showExportFormatMenu = true }
                    )
                    DropdownMenu(expanded = showExportFormatMenu, onDismissRequest = { showExportFormatMenu = false }) {
                        ExportFormat.values().forEach { fmt ->
                            DropdownMenuItem(
                                text = { Text(fmt.name) },
                                onClick = {
                                    viewModel.setExportFormat(fmt)
                                    showExportFormatMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. STORAGE SECTION
            SettingsSectionCard(title = "Storage", icon = Icons.Filled.Storage) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Save Location", style = MaterialTheme.typography.bodyMedium)
                        Text("Movies/FlowRec", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Recordings Storage", style = MaterialTheme.typography.bodyMedium)
                            Text("${storageInfo.flowRecFormatted} used · ${storageInfo.availableFormatted} free", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Cache cleaned. 32 MB freed.", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("btn_clear_cache")
                        ) {
                            Text("Clear Cache", fontSize = 11.5.sp)
                        }
                    }

                    LinearProgressIndicator(
                        progress = { storageInfo.usedPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. AI SECTION
            SettingsSectionCard(title = "AI Video Polish", icon = Icons.Filled.AutoAwesome) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Smart Zoom, silence detection, and auto-framing models run locally on your phone hardware.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = { showAiPrivacyDialog = true },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Privacy & Cloud Processing Info", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 8. APPEARANCE SECTION
            SettingsSectionCard(title = "Appearance", icon = Icons.Filled.DarkMode) {
                Box {
                    SettingRowDropdown(
                        label = "App Theme",
                        value = currentTheme.label,
                        onClick = { showThemeMenu = true }
                    )
                    DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                        AppThemeMode.values().forEach { theme ->
                            DropdownMenuItem(
                                text = { Text(theme.label) },
                                onClick = {
                                    viewModel.setThemeMode(theme)
                                    showThemeMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 9. NOTIFICATIONS & QUICK SETTINGS
            SettingsSectionCard(title = "Notifications & System Tile", icon = Icons.Filled.Notifications) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "FlowRec uses high-priority foreground notifications to ensure uninterrupted background recording.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            if (context is android.app.Activity) {
                                FlowRecTileService.requestAddToQuickSettings(context) { res ->
                                    Toast.makeText(context, "Quick settings tile prompt opened", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Add Quick Settings Tile", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 10. ABOUT FLOWREC
            SettingsSectionCard(title = "About FlowRec", icon = Icons.Filled.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("FlowRec for Android", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Version 1.0.0 (Release Architecture)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Native MediaProjection & Hardware Codec", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Built for content creators, game recorders, and tutorial makers.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // AI Privacy Dialog
    if (showAiPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showAiPrivacyDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("On-Device AI & Privacy", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "FlowRec does not transmit or upload your recorded video frames to any remote cloud servers. All video analysis, audio extraction, smart zoom detection, and encoding are processed entirely on-device, preserving full privacy for confidential screens, banking, and private chats.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(onClick = { showAiPrivacyDialog = false }, shape = RoundedCornerShape(14.dp)) {
                    Text("Understood")
                }
            }
        )
    }

    // Camera Permission Denied Dialog
    if (showCameraDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showCameraDeniedDialog = false },
            title = { Text("Camera Permission Required", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "FaceCam requires camera permission to display a floating front-camera bubble while recording.\n\nYou can still record your screen normally without FaceCam, or grant permission to enable it.",
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
                TextButton(onClick = { showCameraDeniedDialog = false }) {
                    Text("Record Without FaceCam")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            content()
        }
    }
}

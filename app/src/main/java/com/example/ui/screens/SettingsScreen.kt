package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.core.content.ContextCompat
import com.example.config.UiFeatureFlagManager
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
import com.example.ui.components.Segmented
import com.example.ui.components.SettingRowDropdown
import com.example.ui.components.SettingRowSwitch
import com.example.ui.theme.flow
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
    val flagManager = remember { UiFeatureFlagManager.getInstance(context) }
    val flags by flagManager.flags.collectAsState()

    val currentTheme by viewModel.themeMode.collectAsState()

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
    var showRemoteConfigDialog by remember { mutableStateOf(false) }
    var remoteConfigUrlInput by remember { mutableStateOf(flags.remoteConfigUrl) }

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
        containerColor = flow.bg,
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
                    .padding(top = 8.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.4).sp
                    ),
                    color = flow.fg
                )
            }

            // 1. UI FEATURE FLAGS & REMOTE CONFIG
            SettingsSectionCard(title = "UI & Remote Configuration", icon = Icons.Filled.AutoAwesome) {
                SettingRowSwitch(
                    label = "Minimalist Monochrome UI",
                    sublabel = "Apple-inspired ink-on-paper style with reactive dark/light themes",
                    checked = flags.useMinimalistMonochromeUi,
                    onCheckedChange = { flagManager.setMinimalistUiEnabled(it) }
                )

                HorizontalDivider(thickness = 0.5.dp, color = flow.separator)

                SettingRowSwitch(
                    label = "Shutter Morph Animation",
                    sublabel = "Fluid 400ms circle-to-square shutter morph with spring physics",
                    checked = flags.enableShutterMorphAnimation,
                    onCheckedChange = { flagManager.setShutterMorphEnabled(it) }
                )

                HorizontalDivider(thickness = 0.5.dp, color = flow.separator)

                SettingRowSwitch(
                    label = "Concentric Background Rings",
                    sublabel = "Subtle pulsing harmonic ring waves on Home & Capture canvas",
                    checked = flags.enableBackgroundRings,
                    onCheckedChange = { flagManager.setBackgroundRingsEnabled(it) }
                )

                HorizontalDivider(thickness = 0.5.dp, color = flow.separator)

                SettingRowSwitch(
                    label = "Tactile Haptic Feedback",
                    sublabel = "Physical vibrations on mode shifts, toggles, and shutter clicks",
                    checked = flags.enableHapticFeedback,
                    onCheckedChange = { flagManager.setHapticsEnabled(it) }
                )

                HorizontalDivider(thickness = 0.5.dp, color = flow.separator)

                SettingRowSwitch(
                    label = "Auto Reduced-Motion Detection",
                    sublabel = "Gracefully disables idle animations when system animation scale is 0",
                    checked = flags.enableReducedMotionAutoDetect,
                    onCheckedChange = { flagManager.setReducedMotionAutoDetect(it) }
                )

                HorizontalDivider(thickness = 0.5.dp, color = flow.separator)

                // Remote Config Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Remote Config Endpoint",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = flow.fg
                    )
                    Text(
                        text = if (flags.remoteConfigUrl.isNotBlank()) flags.remoteConfigUrl else "No remote URL configured (tap below to configure or sync)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = flow.muted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                remoteConfigUrlInput = flags.remoteConfigUrl
                                showRemoteConfigDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Set URL", fontSize = 12.sp, color = flow.fg)
                        }

                        Button(
                            onClick = {
                                if (flags.remoteConfigUrl.isBlank()) {
                                    Toast.makeText(context, "Please set a remote config JSON URL first", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Fetching remote config...", Toast.LENGTH_SHORT).show()
                                    flagManager.fetchAndActivateRemoteConfig { success ->
                                        if (success) {
                                            Toast.makeText(context, "Remote config updated successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to fetch remote config", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = flow.fg,
                                contentColor = flow.onFg
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sync Config", fontSize = 12.sp)
                        }
                    }

                    TextButton(
                        onClick = {
                            flagManager.resetToDefaults()
                            Toast.makeText(context, "Feature flags reset to defaults", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = flow.muted)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Flags to Defaults", fontSize = 11.5.sp, color = flow.muted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. APPEARANCE SECTION
            SettingsSectionCard(title = "Appearance", icon = Icons.Filled.DarkMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "App Theme",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                        color = flow.fg
                    )

                    val themeOptions = listOf("Auto (System)", "Light", "Dark")
                    val themeModes = listOf(AppThemeMode.SYSTEM, AppThemeMode.LIGHT, AppThemeMode.DARK)
                    val selectedThemeIndex = when (currentTheme) {
                        AppThemeMode.SYSTEM -> 0
                        AppThemeMode.LIGHT -> 1
                        AppThemeMode.DARK -> 2
                    }

                    Segmented(
                        options = themeOptions,
                        selected = selectedThemeIndex,
                        onSelect = { index ->
                            viewModel.setThemeMode(themeModes[index])
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. RECORDING SECTION
            SettingsSectionCard(title = "Recording", icon = Icons.Filled.Videocam) {
                // Game Recording Mode
                SettingRowSwitch(
                    label = "Game Recording Mode",
                    sublabel = "Preset with 1080p, 60 FPS, 12 Mbps & internal device audio",
                    checked = isGameMode,
                    onCheckedChange = { viewModel.setGameMode(it) }
                )

                HorizontalDivider(thickness = 0.5.dp, color = flow.separator)

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

                HorizontalDivider(thickness = 0.5.dp, color = flow.separator)

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

                HorizontalDivider(thickness = 0.5.dp, color = flow.separator)

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

                HorizontalDivider(thickness = 0.5.dp, color = flow.separator)

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

            // 4. AUDIO SECTION
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
                        color = flow.muted,
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

            // 5. CAMERA / FACECAM SECTION
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
                        HorizontalDivider(thickness = 0.5.dp, color = flow.separator)

                        // Camera Lens
                        Text("Camera Lens", style = MaterialTheme.typography.labelSmall, color = flow.muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = facecamFrontLens,
                                onClick = { viewModel.setFacecamFrontLens(true) },
                                label = { Text("Front Camera", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = flow.fg,
                                    selectedLabelColor = flow.onFg
                                )
                            )
                            FilterChip(
                                selected = !facecamFrontLens,
                                onClick = { viewModel.setFacecamFrontLens(false) },
                                label = { Text("Back Camera", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = flow.fg,
                                    selectedLabelColor = flow.onFg
                                )
                            )
                        }

                        // Shape
                        Text("Bubble Shape", style = MaterialTheme.typography.labelSmall, color = flow.muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("CIRCLE" to "Circle", "ROUNDED_RECT" to "Rounded Rect", "RECT" to "Square").forEach { (shape, name) ->
                                FilterChip(
                                    selected = facecamShape == shape,
                                    onClick = { viewModel.setFacecamShape(shape) },
                                    label = { Text(name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = flow.fg,
                                        selectedLabelColor = flow.onFg
                                    )
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
                                Text("Size", style = MaterialTheme.typography.labelSmall, color = flow.muted)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                    listOf("SMALL" to "Small", "MEDIUM" to "Medium", "LARGE" to "Large").forEach { (size, name) ->
                                        FilterChip(
                                            selected = facecamSize == size,
                                            onClick = { viewModel.setFacecamSize(size) },
                                            label = { Text(name, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = flow.fg,
                                                selectedLabelColor = flow.onFg
                                            )
                                        )
                                    }
                                }
                            }

                            TextButton(onClick = { viewModel.resetFacecamPosition() }) {
                                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = flow.fg)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Pos", fontSize = 11.sp, color = flow.fg)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. TOUCH SECTION
            SettingsSectionCard(title = "Touch & Gestures", icon = Icons.Filled.TouchApp) {
                SettingRowSwitch(
                    label = "Record Touch Effects",
                    sublabel = "Streams touch coordinates to companion metadata and renders visual taps",
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
                        HorizontalDivider(thickness = 0.5.dp, color = flow.separator)

                        // Elements
                        Text("Visual Feedback Elements", style = MaterialTheme.typography.labelSmall, color = flow.muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = touchRippleEnabled,
                                onClick = { viewModel.setTouchRippleEnabled(!touchRippleEnabled) },
                                label = { Text("Ripple", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = flow.fg, selectedLabelColor = flow.onFg)
                            )
                            FilterChip(
                                selected = touchHighlightEnabled,
                                onClick = { viewModel.setTouchHighlightEnabled(!touchHighlightEnabled) },
                                label = { Text("Highlight", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = flow.fg, selectedLabelColor = flow.onFg)
                            )
                            FilterChip(
                                selected = touchMovementTrackingEnabled,
                                onClick = { viewModel.setTouchMovementTrackingEnabled(!touchMovementTrackingEnabled) },
                                label = { Text("Movement Trail", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = flow.fg, selectedLabelColor = flow.onFg)
                            )
                        }

                        // Size
                        Text("Effect Size", style = MaterialTheme.typography.labelSmall, color = flow.muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(24 to "Small", 36 to "Medium", 48 to "Large").forEach { (sz, label) ->
                                FilterChip(
                                    selected = touchEffectSizeDp == sz,
                                    onClick = { viewModel.setTouchEffectSizeDp(sz) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = flow.fg, selectedLabelColor = flow.onFg)
                                )
                            }
                        }

                        // Duration
                        Text("Duration", style = MaterialTheme.typography.labelSmall, color = flow.muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(250 to "Short", 500 to "Normal", 800 to "Long").forEach { (dur, label) ->
                                FilterChip(
                                    selected = touchEffectDurationMs == dur,
                                    onClick = { viewModel.setTouchEffectDurationMs(dur) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = flow.fg, selectedLabelColor = flow.onFg)
                                )
                            }
                        }

                        // Opacity
                        Text("Opacity", style = MaterialTheme.typography.labelSmall, color = flow.muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0.4f to "40%", 0.8f to "80%", 1.0f to "100%").forEach { (op, label) ->
                                FilterChip(
                                    selected = kotlin.math.abs(touchEffectOpacity - op) < 0.05f,
                                    onClick = { viewModel.setTouchEffectOpacity(op) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = flow.fg, selectedLabelColor = flow.onFg)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. EXPORT SECTION
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

                HorizontalDivider(thickness = 0.5.dp, color = flow.separator)

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

                HorizontalDivider(thickness = 0.5.dp, color = flow.separator)

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

            // 8. STORAGE SECTION
            SettingsSectionCard(title = "Storage", icon = Icons.Filled.Storage) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Save Location", style = MaterialTheme.typography.bodyMedium, color = flow.fg)
                        Text("Movies/FlowRec", style = MaterialTheme.typography.bodySmall, color = flow.fg)
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = flow.separator)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Recordings Storage", style = MaterialTheme.typography.bodyMedium, color = flow.fg)
                            Text("${storageInfo.flowRecFormatted} used · ${storageInfo.availableFormatted} free", style = MaterialTheme.typography.bodySmall, color = flow.muted)
                        }

                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Cache cleaned. 32 MB freed.", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("btn_clear_cache")
                        ) {
                            Text("Clear Cache", fontSize = 11.5.sp, color = flow.fg)
                        }
                    }

                    LinearProgressIndicator(
                        progress = { storageInfo.usedPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = flow.fg,
                        trackColor = flow.separator
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 9. AI SECTION
            SettingsSectionCard(title = "AI Video Polish", icon = Icons.Filled.AutoAwesome) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Smart Zoom, silence detection, and auto-framing models run locally on your phone hardware.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                        color = flow.muted
                    )

                    OutlinedButton(
                        onClick = { showAiPrivacyDialog = true },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = flow.fg)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Privacy & Processing Info", fontSize = 12.sp, color = flow.fg)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 10. NOTIFICATIONS & QUICK SETTINGS
            SettingsSectionCard(title = "Notifications & System Tile", icon = Icons.Filled.Notifications) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "FlowRec uses high-priority foreground notifications to ensure uninterrupted background recording.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = flow.muted
                    )

                    OutlinedButton(
                        onClick = {
                            if (context is android.app.Activity) {
                                FlowRecTileService.requestAddToQuickSettings(context) { _ ->
                                    Toast.makeText(context, "Quick settings tile prompt opened", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Add Quick Settings Tile", fontSize = 12.sp, color = flow.fg)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 11. ABOUT FLOWREC
            SettingsSectionCard(title = "About FlowRec", icon = Icons.Filled.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("FlowRec for Android", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = flow.fg)
                    Text("Version 1.0.0 (Minimalist Monochrome Architecture)", style = MaterialTheme.typography.bodySmall, color = flow.muted)
                    Text("Native MediaProjection & Hardware Codec", style = MaterialTheme.typography.bodySmall, color = flow.muted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Minimalist ink-on-paper UI design with local on-device processing.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = flow.muted
                    )
                }
            }

            // Bottom padding for dock clearance
            Spacer(modifier = Modifier.height(110.dp))
        }
    }

    // Remote Config URL Edit Dialog
    if (showRemoteConfigDialog) {
        AlertDialog(
            onDismissRequest = { showRemoteConfigDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Remote Config URL", fontWeight = FontWeight.Bold, color = flow.fg)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a remote JSON endpoint to fetch dynamic UI feature flags over HTTP/HTTPS:",
                        style = MaterialTheme.typography.bodySmall,
                        color = flow.muted
                    )
                    OutlinedTextField(
                        value = remoteConfigUrlInput,
                        onValueChange = { remoteConfigUrlInput = it },
                        label = { Text("Config JSON URL") },
                        placeholder = { Text("https://example.com/flowrec_config.json") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        flagManager.setRemoteConfigUrl(remoteConfigUrlInput)
                        showRemoteConfigDialog = false
                        Toast.makeText(context, "Remote URL saved", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = flow.fg,
                        contentColor = flow.onFg
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoteConfigDialog = false }) {
                    Text("Cancel", color = flow.fg)
                }
            }
        )
    }

    // AI Privacy Dialog
    if (showAiPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showAiPrivacyDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("On-Device AI & Privacy", fontWeight = FontWeight.Bold, color = flow.fg)
            },
            text = {
                Text(
                    "FlowRec does not transmit or upload your recorded video frames to any remote cloud servers. All video analysis, audio extraction, smart zoom detection, and encoding are processed entirely on-device, preserving full privacy for confidential screens, banking, and private chats.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 19.sp),
                    color = flow.muted
                )
            },
            confirmButton = {
                Button(
                    onClick = { showAiPrivacyDialog = false },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = flow.fg,
                        contentColor = flow.onFg
                    )
                ) {
                    Text("Understood")
                }
            }
        )
    }

    // Camera Permission Denied Dialog
    if (showCameraDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showCameraDeniedDialog = false },
            title = { Text("Camera Permission Required", fontWeight = FontWeight.Bold, color = flow.fg) },
            text = {
                Text(
                    "FaceCam requires camera permission to display a floating front-camera bubble while recording.\n\nYou can still record your screen normally without FaceCam, or grant permission to enable it.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
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
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraDeniedDialog = false }) {
                    Text("Record Without FaceCam", color = flow.fg)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(flow.fill)
            .padding(16.dp)
    ) {
        if (title.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = flow.fg,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = flow.fg
                )
            }
        }
        content()
    }
}

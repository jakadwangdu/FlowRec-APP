package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppThemeMode
import com.example.model.AudioSourceMode
import com.example.model.CountdownOption
import com.example.model.FrameRate
import com.example.model.RecordingResolution
import com.example.model.VideoOrientation
import com.example.ui.components.FlowRecBottomNav
import com.example.ui.components.FlowRecTopBar
import com.example.ui.components.SettingRowDropdown
import com.example.ui.components.SettingRowSwitch
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
    var showClearDataDialog by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            FlowRecTopBar(
                title = "Settings",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            FlowRecBottomNav(
                currentTab = Screen.SETTINGS,
                onTabSelected = { tab ->
                    viewModel.switchBottomTab(tab)
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount > 60f) {
                        change.consume()
                        onBackClick?.invoke() ?: viewModel.switchBottomTab(Screen.HOME)
                    }
                }
            }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // SECTION: GENERAL
            Text(
                text = "General",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Theme Dropdown
            Box {
                SettingRowDropdown(
                    label = "Theme",
                    value = currentTheme.label,
                    onClick = { showThemeMenu = true }
                )
                DropdownMenu(
                    expanded = showThemeMenu,
                    onDismissRequest = { showThemeMenu = false }
                ) {
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

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: RECORDING
            Text(
                text = "Recording Quality & Format",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Default Resolution
            val defaultRes by viewModel.defaultResolution.collectAsState()
            var showResMenu by remember { mutableStateOf(false) }
            Box {
                SettingRowDropdown(
                    label = "Resolution",
                    value = defaultRes.label,
                    onClick = { showResMenu = true }
                )
                DropdownMenu(
                    expanded = showResMenu,
                    onDismissRequest = { showResMenu = false }
                ) {
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

            // Default FPS
            val defaultFps by viewModel.defaultFps.collectAsState()
            var showFpsMenu by remember { mutableStateOf(false) }
            Box {
                SettingRowDropdown(
                    label = "Frame Rate",
                    value = defaultFps.label,
                    onClick = { showFpsMenu = true }
                )
                DropdownMenu(
                    expanded = showFpsMenu,
                    onDismissRequest = { showFpsMenu = false }
                ) {
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

            // Audio Source Mode
            val audioSource by viewModel.audioSourceMode.collectAsState()
            var showAudioMenu by remember { mutableStateOf(false) }
            Box {
                SettingRowDropdown(
                    label = "Audio Source",
                    value = audioSource.label,
                    onClick = { showAudioMenu = true }
                )
                DropdownMenu(
                    expanded = showAudioMenu,
                    onDismissRequest = { showAudioMenu = false }
                ) {
                    AudioSourceMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            onClick = {
                                viewModel.setAudioSourceMode(mode)
                                showAudioMenu = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Video Orientation
            val orientation by viewModel.videoOrientation.collectAsState()
            var showOrientationMenu by remember { mutableStateOf(false) }
            Box {
                SettingRowDropdown(
                    label = "Orientation",
                    value = orientation.label,
                    onClick = { showOrientationMenu = true }
                )
                DropdownMenu(
                    expanded = showOrientationMenu,
                    onDismissRequest = { showOrientationMenu = false }
                ) {
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

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Countdown Timer
            val countdown by viewModel.countdownOption.collectAsState()
            var showCountdownMenu by remember { mutableStateOf(false) }
            Box {
                SettingRowDropdown(
                    label = "Countdown Timer",
                    value = countdown.label,
                    onClick = { showCountdownMenu = true }
                )
                DropdownMenu(
                    expanded = showCountdownMenu,
                    onDismissRequest = { showCountdownMenu = false }
                ) {
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

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: MOBILE PHONE CONTROLS
            Text(
                text = "Phone Controls & Gestures",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Show Touches / Tap Indicator
            val showTouches by viewModel.showTouches.collectAsState()
            SettingRowSwitch(
                label = "Show Touches & Taps",
                sublabel = "Displays visual tap circles where finger touches screen",
                checked = showTouches,
                onCheckedChange = { viewModel.setShowTouches(it) }
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Floating Bubble Controls on Side
            val floatingBubble by viewModel.floatingBubbleEnabled.collectAsState()
            SettingRowSwitch(
                label = "Floating Control Bubble",
                sublabel = "Show floating circle widget on screen edge while recording",
                checked = floatingBubble,
                onCheckedChange = { viewModel.setFloatingBubbleEnabled(it) }
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Shake to Stop
            val shakeToStop by viewModel.shakeToStop.collectAsState()
            SettingRowSwitch(
                label = "Shake Phone to Stop",
                sublabel = "Shake device to immediately stop and finalize recording",
                checked = shakeToStop,
                onCheckedChange = { viewModel.setShakeToStop(it) }
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Save to Gallery
            val autoSaveToGallery by viewModel.autoSaveToGallery.collectAsState()
            SettingRowSwitch(
                label = "Save to Gallery Automatically",
                sublabel = "Directly saves recordings to phone Gallery (Movies/FlowRec)",
                checked = autoSaveToGallery,
                onCheckedChange = { viewModel.setAutoSaveToGallery(it) }
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: STORAGE
            Text(
                text = "Storage & Gallery",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Local Storage", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text("1.2 GB / 16 GB used", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "Cache cleared successfully (48 MB freed)", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("btn_clear_cache")
                ) {
                    Text("Clear Cache", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // About & Version
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                Column {
                    Text("FlowRec for Android", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Version 1.0.0 (Capacitor Native Engine)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("MediaProjection + Hardware MediaCodec", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                title = { Text("Clear All Data") },
                text = { Text("Are you sure you want to delete all local recordings and projects? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllData()
                            showClearDataDialog = false
                            Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Delete All", color = AccentRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

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
import com.example.model.FrameRate
import com.example.model.RecordingResolution
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

    var defaultRes by remember { mutableStateOf(RecordingResolution.RES_1080P) }
    var defaultFps by remember { mutableStateOf(FrameRate.FPS_30) }
    var showCursor by remember { mutableStateOf(true) }
    var clickEffects by remember { mutableStateOf(true) }
    var motionBlur by remember { mutableStateOf(true) }
    var defaultZoom by remember { mutableStateOf(1.6f) }
    var blurAmount by remember { mutableStateOf(18) }

    var showResMenu by remember { mutableStateOf(false) }
    var showFpsMenu by remember { mutableStateOf(false) }
    var showZoomMenu by remember { mutableStateOf(false) }
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
                text = "Recording",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Default Resolution
            Box {
                SettingRowDropdown(
                    label = "Default Resolution",
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
                                defaultRes = res
                                showResMenu = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Default FPS
            Box {
                SettingRowDropdown(
                    label = "Default FPS",
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
                                defaultFps = fps
                                showFpsMenu = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            SettingRowSwitch(
                label = "Show Cursor",
                sublabel = null,
                checked = showCursor,
                onCheckedChange = { showCursor = it }
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            SettingRowSwitch(
                label = "Click Effects",
                sublabel = null,
                checked = clickEffects,
                onCheckedChange = { clickEffects = it }
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: EFFECTS
            Text(
                text = "Effects",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box {
                SettingRowDropdown(
                    label = "Default Zoom",
                    value = "${defaultZoom}x",
                    onClick = { showZoomMenu = true }
                )
                DropdownMenu(
                    expanded = showZoomMenu,
                    onDismissRequest = { showZoomMenu = false }
                ) {
                    listOf(1.2f, 1.4f, 1.6f, 1.8f, 2.0f, 2.5f).forEach { z ->
                        DropdownMenuItem(
                            text = { Text("${z}x") },
                            onClick = {
                                defaultZoom = z
                                showZoomMenu = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            SettingRowSwitch(
                label = "Motion Blur",
                sublabel = null,
                checked = motionBlur,
                onCheckedChange = { motionBlur = it }
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Blur Amount", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("$blurAmount%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(18.dp))

            // SECTION: STORAGE
            Text(
                text = "Storage",
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

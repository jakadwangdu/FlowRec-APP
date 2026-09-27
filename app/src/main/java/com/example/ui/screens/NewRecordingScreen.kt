package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CaptureMode
import com.example.model.FrameRate
import com.example.model.RecordingResolution
import com.example.recorder.RecordingConfig
import com.example.ui.components.FlowRecTopBar
import com.example.ui.components.SettingRowDropdown
import com.example.ui.components.SettingRowSwitch
import com.example.ui.viewmodel.FlowRecViewModel

@Composable
fun NewRecordingScreen(
    viewModel: FlowRecViewModel,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    var selectedMode by remember { mutableStateOf(CaptureMode.SCREEN) }
    var selectedRes by remember { mutableStateOf(RecordingResolution.RES_1080P) }
    var selectedFps by remember { mutableStateOf(FrameRate.FPS_30) }
    var systemAudio by remember { mutableStateOf(true) }
    var microphone by remember { mutableStateOf(false) }
    var showCursor by remember { mutableStateOf(true) }
    var clickEffects by remember { mutableStateOf(true) }

    var showResMenu by remember { mutableStateOf(false) }
    var showFpsMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FlowRecTopBar(
                title = "New Recording",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.recorderEngine.updateConfig(
                            RecordingConfig(
                                mode = selectedMode,
                                resolution = selectedRes,
                                frameRate = selectedFps,
                                recordSystemAudio = systemAudio,
                                recordMicrophone = microphone,
                                showCursor = showCursor,
                                clickEffects = clickEffects
                            )
                        )
                        onNextClick()
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_start_recording_setup")
                ) {
                    Text(
                        text = "Start Recording",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    )
                }
            }
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
            Spacer(modifier = Modifier.height(8.dp))

            // Section 1: Select what to capture
            Text(
                text = "Select what to capture",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Mode cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModeCard(
                    title = "Screen",
                    icon = Icons.Filled.SmartDisplay,
                    isSelected = selectedMode == CaptureMode.SCREEN,
                    onClick = { selectedMode = CaptureMode.SCREEN },
                    modifier = Modifier.weight(1f)
                )
                ModeCard(
                    title = "Window",
                    icon = Icons.Filled.DesktopWindows,
                    isSelected = selectedMode == CaptureMode.WINDOW,
                    onClick = { selectedMode = CaptureMode.WINDOW },
                    modifier = Modifier.weight(1f)
                )
                ModeCard(
                    title = "Tab",
                    icon = Icons.Filled.Tab,
                    isSelected = selectedMode == CaptureMode.TAB,
                    onClick = { selectedMode = CaptureMode.TAB },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Section 2: Recording Settings
            Text(
                text = "Recording Settings",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Resolution Selector
            Box {
                SettingRowDropdown(
                    label = "Resolution",
                    value = selectedRes.label,
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
                                selectedRes = res
                                showResMenu = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Frame Rate Selector
            Box {
                SettingRowDropdown(
                    label = "Frame Rate",
                    value = selectedFps.label,
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
                                selectedFps = fps
                                showFpsMenu = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Audio: System Audio
            SettingRowSwitch(
                label = "Audio",
                sublabel = "System Audio",
                checked = systemAudio,
                onCheckedChange = { systemAudio = it }
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Audio: Microphone
            SettingRowSwitch(
                label = "Microphone",
                sublabel = null,
                checked = microphone,
                onCheckedChange = { microphone = it }
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Show Cursor
            SettingRowSwitch(
                label = "Show Cursor",
                sublabel = null,
                checked = showCursor,
                onCheckedChange = { showCursor = it }
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Click Effects
            SettingRowSwitch(
                label = "Click Effects",
                sublabel = null,
                checked = clickEffects,
                onCheckedChange = { clickEffects = it }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant
    val bg = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(if (isSelected) 1.5.dp else 0.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


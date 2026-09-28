package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioSourceMode
import com.example.ui.theme.AccentRed
import com.example.ui.viewmodel.FlowRecViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuickSettingsPanel(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onStartRecordingFromTile: (audioMode: AudioSourceMode, showTouches: Boolean) -> Unit,
    viewModel: FlowRecViewModel
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(tween(250)) + slideInVertically(
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            initialOffsetY = { -it }
        ),
        exit = fadeOut(tween(200)) + slideOutVertically(
            animationSpec = tween(250, easing = FastOutSlowInEasing),
            targetOffsetY = { -it }
        )
    ) {
        val showTouches by viewModel.showTouches.collectAsState()
        val audioSourceMode by viewModel.audioSourceMode.collectAsState()
        val floatingBubble by viewModel.floatingBubbleEnabled.collectAsState()
        val themeMode by viewModel.themeMode.collectAsState()

        var showRecordingOptionsModal by remember { mutableStateOf(false) }
        var selectedAudioMode by remember { mutableStateOf(audioSourceMode) }
        var quickTouchesEnabled by remember { mutableStateOf(showTouches) }
        var brightnessValue by remember { mutableFloatStateOf(0.65f) }

        val timeFormatter = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
        val dateFormatter = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }
        val now = remember { Date() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF16181C).copy(alpha = 0.94f))
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        if (dragAmount < -40f) {
                            change.consume()
                            onDismiss()
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header: Time, Date and Actions (Pencil, Power, Settings)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeFormatter.format(now),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 28.sp,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateFormatter.format(now),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White.copy(alpha = 0.85f)
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { /* Quick settings edit */ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.PowerSettingsNew, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = {
                                onDismiss()
                                viewModel.navigateTo(com.example.ui.viewmodel.Screen.SETTINGS)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Large Dual Pills: Wi-Fi & Bluetooth
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickDualPill(
                        icon = Icons.Filled.Wifi,
                        title = "Wi-Fi",
                        subtitle = "Connected",
                        isActive = true,
                        modifier = Modifier.weight(1f)
                    )
                    QuickDualPill(
                        icon = Icons.Filled.Bluetooth,
                        title = "Bluetooth",
                        subtitle = "Active",
                        isActive = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Tiles Matrix: Screen Recorder (prominent!), Touches, Sound, Dark mode
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF26282E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // 1. SCREEN RECORDER TILE (Target feature from Pic 4)
                            QuickTileItem(
                                icon = Icons.Filled.Videocam,
                                label = "Screen recorder",
                                isActive = true,
                                isAccent = true,
                                onClick = {
                                    showRecordingOptionsModal = true
                                }
                            )

                            // 2. Sound Mode Tile
                            QuickTileItem(
                                icon = Icons.Filled.VolumeUp,
                                label = "Sound",
                                isActive = audioSourceMode != AudioSourceMode.NONE,
                                onClick = {
                                    val next = when (audioSourceMode) {
                                        AudioSourceMode.MIC_AND_SYSTEM -> AudioSourceMode.SYSTEM
                                        AudioSourceMode.SYSTEM -> AudioSourceMode.NONE
                                        AudioSourceMode.NONE -> AudioSourceMode.MIC_AND_SYSTEM
                                    }
                                    viewModel.setAudioSourceMode(next)
                                }
                            )

                            // 3. Show Touches Tile
                            QuickTileItem(
                                icon = Icons.Filled.TouchApp,
                                label = "Show touches",
                                isActive = showTouches,
                                onClick = {
                                    viewModel.setShowTouches(!showTouches)
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // 4. Floating Bubble Tile
                            QuickTileItem(
                                icon = Icons.Filled.Videocam,
                                label = "Float bubble",
                                isActive = floatingBubble,
                                onClick = {
                                    viewModel.setFloatingBubbleEnabled(!floatingBubble)
                                }
                            )

                            // 5. Dark Mode Tile
                            QuickTileItem(
                                icon = Icons.Filled.DarkMode,
                                label = "Dark mode",
                                isActive = themeMode == com.example.model.AppThemeMode.DARK,
                                onClick = {
                                    val next = if (themeMode == com.example.model.AppThemeMode.DARK) {
                                        com.example.model.AppThemeMode.LIGHT
                                    } else {
                                        com.example.model.AppThemeMode.DARK
                                    }
                                    viewModel.setThemeMode(next)
                                }
                            )

                            // 6. Scan QR Code Tile
                            QuickTileItem(
                                icon = Icons.Filled.QrCodeScanner,
                                label = "Scan QR",
                                isActive = false,
                                onClick = {}
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SIM & Network Status card (from Pic 4)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF26282E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Calls", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            Text("SIM 1 • FlowRec", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = Color.White)
                        }
                        HorizontalDivider(
                            modifier = Modifier
                                .height(24.dp)
                                .width(1.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        )
                        Column {
                            Text("Messages", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            Text("SIM 1 • FlowRec", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = Color.White)
                        }
                        HorizontalDivider(
                            modifier = Modifier
                                .height(24.dp)
                                .width(1.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        )
                        Column {
                            Text("Mobile data", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                            Text("Active", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Brightness Slider Card (from Pic 4)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF26282E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BrightnessMedium,
                            contentDescription = "Brightness",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Slider(
                            value = brightnessValue,
                            onValueChange = { brightnessValue = it },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Drag-Up Close Handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.4f))
                    )
                }
            }

            // Samsung One UI style popup when tapping "Screen Recorder" tile
            if (showRecordingOptionsModal) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2024)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF383842)),
                        width = 1.dp
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Text(
                            text = "Start recording with FlowRec?",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Sound",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Audio Radio Options
                        AudioOptionRadio(
                            label = "None",
                            selected = selectedAudioMode == AudioSourceMode.NONE,
                            onClick = { selectedAudioMode = AudioSourceMode.NONE }
                        )
                        AudioOptionRadio(
                            label = "Media sounds",
                            selected = selectedAudioMode == AudioSourceMode.SYSTEM,
                            onClick = { selectedAudioMode = AudioSourceMode.SYSTEM }
                        )
                        AudioOptionRadio(
                            label = "Media sounds and mic",
                            selected = selectedAudioMode == AudioSourceMode.MIC_AND_SYSTEM,
                            onClick = { selectedAudioMode = AudioSourceMode.MIC_AND_SYSTEM }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Show Taps and Touches switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Show taps and touches",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Switch(
                                checked = quickTouchesEnabled,
                                onCheckedChange = { quickTouchesEnabled = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons: Cancel & Start recording
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showRecordingOptionsModal = false },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("Cancel", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    showRecordingOptionsModal = false
                                    onDismiss()
                                    onStartRecordingFromTile(selectedAudioMode, quickTouchesEnabled)
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                            ) {
                                Text("Start recording", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickDualPill(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isActive) Color(0xFFE8EAED) else Color(0xFF26282E))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isActive) Color.Black else Color.White,
            modifier = Modifier.size(22.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                color = if (isActive) Color.Black else Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = if (isActive) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun QuickTileItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isAccent: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (isAccent) AccentRed
                    else if (isActive) Color.White
                    else Color(0xFF383A40)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isAccent) Color.White else if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
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

@Composable
private fun AudioOptionRadio(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color.White,
                unselectedColor = Color.White.copy(alpha = 0.5f)
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

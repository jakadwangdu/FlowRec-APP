package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.ProjectEntity
import com.example.model.AudioSourceMode
import com.example.model.RecorderState
import com.example.ui.components.FlowRecBottomNav
import com.example.ui.components.ProjectItemCard
import com.example.ui.components.StartRecordingDialog
import com.example.ui.components.formatSeconds
import com.example.ui.viewmodel.FlowRecViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun HomeScreen(
    viewModel: FlowRecViewModel,
    projects: List<ProjectEntity>,
    onNewRecordingClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddQuickTileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recState by viewModel.recorderEngine.state.collectAsState()
    val durationSeconds by viewModel.recorderEngine.durationSeconds.collectAsState()

    var showStartRecordingDialog by remember { mutableStateOf(false) }
    var showQuickTileGuideDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                FlowRecBottomNav(
                    currentTab = Screen.HOME,
                    onTabSelected = { tab ->
                        viewModel.switchBottomTab(tab)
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        if (dragAmount < -60f) {
                            change.consume()
                            viewModel.switchBottomTab(Screen.LIBRARY)
                        }
                    }
                }
        ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: FlowRec Brand & Settings Icon
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // FlowRec Logo Icon
                        Image(
                            painter = painterResource(id = R.drawable.ic_flowrec_logo),
                            contentDescription = "FlowRec Logo",
                            modifier = Modifier.size(32.dp)
                        )

                        Text(
                            text = "FlowRec",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showQuickTileGuideDialog = true },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("home_quick_tile_button")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_qs_screen_recorder),
                                contentDescription = "Add to Phone Quick Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        val isDarkTheme = MaterialTheme.colorScheme.primary == Color.White
                        IconButton(
                            onClick = { viewModel.toggleTheme(isDarkTheme) },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("home_theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Active Recording in Progress Banner
            if (recState == RecorderState.RECORDING || recState == RecorderState.PAUSED) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateTo(Screen.RECORDING_HUD) }
                            .testTag("active_recording_banner"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1418)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFFE53935), Color(0xFFFF5252))
                            ),
                            width = 1.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF5252))
                                )
                                Column {
                                    Text(
                                        text = if (recState == RecorderState.PAUSED) "Recording is Paused" else "Recording in Progress...",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Elapsed: ${formatSeconds(durationSeconds)} • Tap to open Live Monitor",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color(0xFFFFCDD2)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open Monitor",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Hero Section: Record. Edit. Share.
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "Record. Edit.\nShare.",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            lineHeight = 38.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Minimal screen recorder with powerful editing features — built for creators.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary CTA: + New Recording
                    Button(
                        onClick = { showStartRecordingDialog = true },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_new_recording")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Recording",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Phone Quick Settings Tile Promo Card (Adds tile to Phone's top panel / Pic 4)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onAddQuickTileClick()
                                showQuickTileGuideDialog = true
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF3B82F6), Color(0xFF60A5FA))
                            ),
                            width = 1.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_qs_screen_recorder),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Add to Phone Quick Settings",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "1-tap record tile in your phone's notification bar",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Section Header: Recent Projects
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Projects",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onViewAllClick)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "View all",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View all projects",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Recent Projects List
            val recentProjects = projects.take(5)
            if (recentProjects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recordings yet. Tap '+ New Recording' to start!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(recentProjects, key = { it.id }) { project ->
                    ProjectItemCard(
                        project = project,
                        onClick = { viewModel.openProject(project) },
                        onEditClick = { viewModel.openEditorForProject(project) },
                        onRename = { newName -> viewModel.renameProject(project, newName) },
                        onDuplicate = { viewModel.duplicateProject(project) },
                        onDelete = { viewModel.deleteProject(project) },
                        onFavoriteToggle = { viewModel.toggleFavorite(project) },
                        showDate = false
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

        // Start Recording Small Confirmation Dialog (Pic 1 user request)
        StartRecordingDialog(
            isOpen = showStartRecordingDialog,
            onDismiss = { showStartRecordingDialog = false },
            onConfirm = { recordMic ->
                showStartRecordingDialog = false
                viewModel.recorderEngine.updateConfig(
                    viewModel.recorderEngine.config.value.copy(recordMicrophone = recordMic)
                )
                onNewRecordingClick()
            }
        )

        // Guide Dialog for adding Screen Recorder into Android / Samsung Quick Settings
        if (showQuickTileGuideDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showQuickTileGuideDialog = false },
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_qs_screen_recorder),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Phone Quick Settings Tile",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Add 'Screen Recorder' directly into your phone's notification panel alongside Wi-Fi and Bluetooth!",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "How to add on your phone:\n1. Pull down your phone's notification bar twice\n2. Tap the ✏️ (Pencil / Edit) icon at top right\n3. Find 'Screen Recorder' and drag it into your active buttons",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showQuickTileGuideDialog = false
                            onAddQuickTileClick()
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Add to Quick Settings")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showQuickTileGuideDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

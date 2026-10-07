package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.config.UiFeatureFlagManager
import com.example.data.entity.ProjectEntity
import com.example.model.RecorderState
import com.example.ui.components.FlowRecBottomNav
import com.example.ui.components.ProjectItemCard
import com.example.ui.components.QualitySheet
import com.example.ui.components.Rings
import com.example.ui.components.formatSeconds
import com.example.ui.theme.flow
import com.example.ui.viewmodel.FlowRecViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun HomeScreen(
    viewModel: FlowRecViewModel,
    projects: List<ProjectEntity>,
    onNewRecordingClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddQuickTileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val flagManager = remember { UiFeatureFlagManager.getInstance(context) }
    val featureFlags by flagManager.flags.collectAsState()
    val isReducedMotion = flagManager.isReducedMotion(context)

    val recState by viewModel.recorderEngine.state.collectAsState()
    val durationSeconds by viewModel.recorderEngine.durationSeconds.collectAsState()

    val currentRes by viewModel.defaultResolution.collectAsState()
    val currentFps by viewModel.defaultFps.collectAsState()
    val currentAudio by viewModel.audioSourceMode.collectAsState()
    val currentOrientation by viewModel.videoOrientation.collectAsState()
    val isGameMode by viewModel.isGameMode.collectAsState()

    var showQuickTileGuideDialog by remember { mutableStateOf(false) }
    var showAiFeatureDialog by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }

    val storageInfo = remember(projects.size) { viewModel.getStorageInfo() }
    val isRecording = recState == RecorderState.RECORDING || recState == RecorderState.PAUSED

    Scaffold(
        bottomBar = {
            FlowRecBottomNav(
                currentTab = Screen.HOME,
                onTabSelected = { tab ->
                    viewModel.switchBottomTab(tab)
                }
            )
        },
        containerColor = flow.bg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Concentric Rings (Minimalist Monochrome - from files/compose.md)
            if (featureFlags.enableBackgroundRings && featureFlags.useMinimalistMonochromeUi) {
                Rings(
                    recording = isRecording,
                    reduceMotion = isReducedMotion
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. BRAND HEADER: Minimalist Greeting + Settings Chip
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_flowrec_logo),
                                contentDescription = "FlowRec Logo",
                                modifier = Modifier.size(36.dp)
                            )

                            Column {
                                Text(
                                    text = "FlowRec",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.5).sp,
                                    color = flow.fg
                                )
                                Text(
                                    text = "Screen recorder & video studio",
                                    fontSize = 12.sp,
                                    color = flow.muted
                                )
                            }
                        }

                        // Settings Chip / Button (48dp touch target)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(flow.fill)
                                .border(0.5.dp, flow.separator, CircleShape)
                                .clickable(onClick = onSettingsClick)
                                .testTag("home_settings_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = flow.fg,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 2. ACTIVE RECORDING IN-PROGRESS BANNER (Shown if recording or paused)
                if (isRecording) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateTo(Screen.RECORDING_HUD) }
                                .testTag("active_recording_banner"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (featureFlags.useMinimalistMonochromeUi) flow.fill else Color(0xFF1E1418)
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(flow.record),
                                width = 1.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(flow.record)
                                    )
                                    Column {
                                        Text(
                                            text = if (recState == RecorderState.PAUSED) "Recording paused" else "REC in progress...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = flow.fg
                                        )
                                        Text(
                                            text = "Elapsed: ${formatSeconds(durationSeconds)} • Tap to open monitor",
                                            fontSize = 12.sp,
                                            color = flow.muted
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Open Monitor",
                                    tint = flow.record,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 3. PRIMARY HERO CARD: Shutter & Start Recording CTA
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = flow.fill
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(flow.separator),
                            width = 0.75.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Record your screen",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = (-0.3).sp,
                                color = flow.fg
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Capture gameplay, tutorials, and apps with crystal-clear audio and zero watermark.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = flow.muted
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Quick Quality & Preset Badges (Tap to open Quality Bottom Sheet)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(flow.glass)
                                    .border(0.5.dp, flow.separator, RoundedCornerShape(14.dp))
                                    .clickable {
                                        if (featureFlags.enableQualityBottomSheet) {
                                            showQualitySheet = true
                                        } else {
                                            viewModel.switchBottomTab(Screen.RECORD)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    QuickSettingBadge(text = currentRes.label)
                                    QuickSettingBadge(text = "${currentFps.fps} FPS")
                                    QuickSettingBadge(text = currentOrientation.label)
                                    if (isGameMode) {
                                        QuickSettingBadge(text = "Game Mode", isHighlight = true)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Tune,
                                        contentDescription = "Quality Options",
                                        tint = flow.fg,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Quality",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = flow.fg
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Big Record CTA Button (48dp+ height, monochrome ink-on-paper with red record dot)
                            Button(
                                onClick = onNewRecordingClick,
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = flow.fg,
                                    contentColor = flow.onFg
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("btn_new_recording")
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
                        }
                    }
                }

                // 4. QUICK ACCESS HUB: Projects Library & Video Editor
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Projects
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.switchBottomTab(Screen.PROJECTS) },
                            colors = CardDefaults.cardColors(
                                containerColor = flow.fill
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(flow.separator),
                                width = 0.5.dp
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(flow.glass)
                                        .border(0.5.dp, flow.separator, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VideoLibrary,
                                        contentDescription = null,
                                        tint = flow.fg,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Library",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = flow.fg
                                )
                                Text(
                                    text = "${projects.size} recordings",
                                    fontSize = 12.sp,
                                    color = flow.muted
                                )
                            }
                        }

                        // Card 2: Video Editor
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.switchBottomTab(Screen.EDITOR) },
                            colors = CardDefaults.cardColors(
                                containerColor = flow.fill
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(flow.separator),
                                width = 0.5.dp
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(flow.glass)
                                        .border(0.5.dp, flow.separator, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        tint = flow.fg,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Studio editor",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = flow.fg
                                )
                                Text(
                                    text = "Timeline & zoom",
                                    fontSize = 12.sp,
                                    color = flow.muted
                                )
                            }
                        }
                    }
                }

                // 5. "MAKE IT FLOW" AI FEATURE SPOTLIGHT ENTRY POINT
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showAiFeatureDialog = true },
                        colors = CardDefaults.cardColors(
                            containerColor = flow.fill
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(flow.separator),
                            width = 0.75.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(flow.glass)
                                        .border(0.5.dp, flow.separator, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = flow.fg,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Make it Flow",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = flow.fg
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(flow.glass)
                                                .border(0.5.dp, flow.separator, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "AI Studio",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = flow.fg
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Smart zoom on clicks, silence cuts & auto-framing",
                                        fontSize = 12.sp,
                                        color = flow.muted
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = flow.muted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 6. STORAGE INFORMATION LINE
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = flow.fill
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(flow.separator),
                            width = 0.5.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Storage,
                                        contentDescription = null,
                                        tint = flow.muted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Device storage",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = flow.fg
                                    )
                                }

                                Text(
                                    text = "${storageInfo.flowRecFormatted} used by FlowRec",
                                    fontSize = 12.sp,
                                    color = flow.fg
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { storageInfo.usedPercentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = flow.fg,
                                trackColor = flow.separator
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${storageInfo.availableFormatted} free",
                                    fontSize = 11.sp,
                                    color = flow.muted
                                )
                                Text(
                                    text = "Total ${storageInfo.totalFormatted}",
                                    fontSize = 11.sp,
                                    color = flow.muted
                                )
                            }
                        }
                    }
                }

                // 7. SYSTEM QUICK SETTINGS TILE PROMO CARD
                item {
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
                            containerColor = flow.fill
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(flow.separator),
                            width = 0.5.dp
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
                                        .background(flow.glass)
                                        .border(0.5.dp, flow.separator, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_qs_screen_recorder),
                                        contentDescription = null,
                                        tint = flow.fg,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Add to Quick Settings",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = flow.fg
                                    )
                                    Text(
                                        text = "1-tap record tile in notification bar",
                                        fontSize = 11.sp,
                                        color = flow.muted
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = flow.fg,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 8. SECTION: RECENT RECORDINGS
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent recordings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = flow.fg
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
                                fontSize = 13.sp,
                                color = flow.fg
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View all projects",
                                tint = flow.fg,
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
                                .clip(RoundedCornerShape(16.dp))
                                .background(flow.fill)
                                .border(0.5.dp, flow.separator, RoundedCornerShape(16.dp))
                                .padding(vertical = 32.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = null,
                                    tint = flow.muted,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No recordings yet",
                                    fontWeight = FontWeight.SemiBold,
                                    color = flow.fg
                                )
                                Text(
                                    text = "Tap 'Start recording' above to capture your screen",
                                    fontSize = 12.sp,
                                    color = flow.muted
                                )
                            }
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
                            showDate = true
                        )
                    }
                }
            }
        }
    }

    // Quality Bottom Sheet (Modal Bottom Sheet with Segmented Controls)
    if (showQualitySheet) {
        QualitySheet(
            onDismiss = { showQualitySheet = false },
            currentResolution = currentRes,
            onResolutionSelected = { viewModel.setDefaultResolution(it) },
            currentFps = currentFps,
            onFpsSelected = { viewModel.setDefaultFps(it) },
            currentAudioSource = currentAudio,
            onAudioSourceSelected = { viewModel.setAudioSourceMode(it) }
        )
    }

    // AI "Make it Flow" Information Dialog
    if (showAiFeatureDialog) {
        AlertDialog(
            onDismissRequest = { showAiFeatureDialog = false },
            shape = RoundedCornerShape(22.dp),
            containerColor = flow.sheet,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(flow.fill)
                        .border(0.5.dp, flow.separator, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = flow.fg,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Make it Flow — AI Video Polish",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = flow.fg
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Turn raw screen captures into cinematic, studio-quality videos with a single tap:",
                        fontSize = 13.sp,
                        color = flow.muted
                    )
                    Text(
                        text = "• Smart Zoom: Automatically detects where your finger taps and smoothly zooms in.\n• Silence Trimmer: Eliminates dead air and pauses from gameplay or voiceovers.\n• Pacing & Framing: Centers dynamic app action smoothly.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = flow.fg
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAiFeatureDialog = false },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = flow.fg,
                        contentColor = flow.onFg
                    )
                ) {
                    Text("Got it")
                }
            }
        )
    }

    // Guide Dialog for adding Screen Recorder into Android Quick Settings
    if (showQuickTileGuideDialog) {
        AlertDialog(
            onDismissRequest = { showQuickTileGuideDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = flow.sheet,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(flow.fill)
                        .border(0.5.dp, flow.separator, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_qs_screen_recorder),
                        contentDescription = null,
                        tint = flow.fg,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Phone Quick Settings tile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = flow.fg
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Add 'Screen Recorder' directly into your phone's notification panel alongside Wi-Fi and Bluetooth!",
                        fontSize = 13.sp,
                        color = flow.muted
                    )
                    Text(
                        text = "How to add on your phone:\n1. Pull down your phone's notification bar twice\n2. Tap the ✏️ (Pencil / Edit) icon at top right\n3. Find 'Screen Recorder' and drag it into your active buttons",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = flow.fg
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQuickTileGuideDialog = false
                        onAddQuickTileClick()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = flow.fg,
                        contentColor = flow.onFg
                    )
                ) {
                    Text("Add to Quick Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickTileGuideDialog = false }) {
                    Text("Close", color = flow.muted)
                }
            }
        )
    }
}

@Composable
private fun QuickSettingBadge(
    text: String,
    isHighlight: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHighlight) flow.fg else flow.fill)
            .border(0.5.dp, flow.separator, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) flow.onFg else flow.fg
        )
    }
}

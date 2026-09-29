package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.ProjectEntity
import com.example.editor.model.AudioTrackConfig
import com.example.editor.model.EditorProjectState
import com.example.editor.model.FaceCamEditorTrack
import com.example.editor.model.ImageOverlay
import com.example.editor.model.TextOverlay
import com.example.editor.model.TouchOverlayConfig
import com.example.editor.model.ZoomKeyframe
import com.example.editor.timeline.TimelineManager
import com.example.ui.components.FlowRecBottomNav
import com.example.ui.components.FlowRecMedia3Player
import com.example.ui.components.formatSeconds
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentRed
import com.example.ui.viewmodel.FlowRecViewModel
import com.example.ui.viewmodel.Screen
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: FlowRecViewModel,
    project: ProjectEntity? = null,
    onBackClick: (() -> Unit)? = null,
    onExportClick: (() -> Unit)? = null,
    onOpenEffects: (() -> Unit)? = null,
    onOpenTimeline: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onBackClick != null) {
        BackHandler { onBackClick() }
    }

    val context = LocalContext.current
    val allProjects by viewModel.allProjects.collectAsState()
    val activeProject = project ?: viewModel.selectedProject.collectAsState().value ?: allProjects.firstOrNull()

    val editorState by viewModel.editorState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val timelinePositionMs by viewModel.timelinePositionMs.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val loadedTouches by viewModel.loadedTouchEvents.collectAsState()
    val activeSegmentId by viewModel.activeSegmentId.collectAsState()
    val activeToolTab by viewModel.activeToolTab.collectAsState()

    // Initialize / Load editor state on first entry for this project
    LaunchedEffect(activeProject?.id) {
        if (activeProject != null) {
            viewModel.loadProjectEditorData(activeProject)
        }
    }

    // Modal sheet states
    var showTextSheet by remember { mutableStateOf(false) }
    var showImageSheet by remember { mutableStateOf(false) }
    var showZoomSheet by remember { mutableStateOf(false) }
    var showFaceCamSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showTrimSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Text Overlay Form State
    var textInput by remember { mutableStateOf("") }
    var textColorHex by remember { mutableStateOf("#FFFFFF") }
    var textBgColorHex by remember { mutableStateOf("#80000000") }
    var textDurationSec by remember { mutableFloatStateOf(4f) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val newImg = ImageOverlay(
                id = UUID.randomUUID().toString(),
                imageUriOrPath = uri.toString(),
                startTimeMs = timelinePositionMs,
                endTimeMs = (timelinePositionMs + 5000L).coerceAtMost(editorState.effectiveDurationMs),
                xPercent = 0.82f,
                yPercent = 0.15f,
                sizePercent = 0.22f,
                opacity = 0.9f
            )
            viewModel.addImageOverlay(newImg)
            Toast.makeText(context, "Logo / Image overlay added", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        bottomBar = {
            FlowRecBottomNav(
                currentTab = Screen.EDITOR,
                onTabSelected = { tab ->
                    viewModel.switchBottomTab(tab)
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (activeProject == null) {
            // EMPTY STATE: No project selected
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "No Video to Edit",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Record your screen or select an existing project from the Projects tab to launch the non-destructive video editor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.switchBottomTab(Screen.RECORD) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Start Recording")
                        }

                        Button(
                            onClick = { viewModel.switchBottomTab(Screen.PROJECTS) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Browse Projects")
                        }
                    }
                }
            }
        } else {
            // ACTIVE REAL VIDEO EDITOR
            val totalTimelineMs = editorState.effectiveDurationMs.coerceAtLeast(1000L)
            val currentFormatted = formatSeconds((timelinePositionMs / 1000L).toInt())
            val totalFormatted = formatSeconds((totalTimelineMs / 1000L).toInt())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. TOP HEADER & WORKSPACE TOOLBAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeProject.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                        Text(
                            text = "${activeProject.resolution} · ${activeProject.fps} FPS · $totalFormatted (Non-destructive)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Undo Button
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = canUndo,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (canUndo) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Redo Button
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = canRedo,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (canRedo) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Export Button
                        Button(
                            onClick = {
                                onExportClick?.invoke() ?: viewModel.navigateTo(Screen.EXPORT_SETTINGS)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // 2. REAL MEDIA3 VIDEO PREVIEW CANVAS (Live Compositing of ExoPlayer + Overlays + FaceCam + Touches)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0A0A0E))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .testTag("editor_media3_player_container")
                ) {
                    FlowRecMedia3Player(
                        videoPath = activeProject.videoPath,
                        thumbnailResName = activeProject.thumbnailResName,
                        isPlaying = isPlaying,
                        onPlayToggle = { viewModel.togglePlayPause() },
                        timelinePositionMs = timelinePositionMs,
                        onTimelinePositionChanged = { viewModel.onPlayerTimelineUpdate(it) },
                        editorState = editorState,
                        playbackSpeed = playbackSpeed,
                        touchEvents = loadedTouches,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Status Badges
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (editorState.touchConfig.enabled) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Touch ON",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White
                                )
                            }
                        }
                        if (playbackSpeed != 1.0f) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentBlue.copy(alpha = 0.85f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. PLAYBACK CONTROLS, TIMECODE & SPEED
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Play / Pause Circle Button
                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("btn_editor_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = "$currentFormatted / $totalFormatted",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Speed Pill Selector Button
                    Button(
                        onClick = { showSpeedSheet = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${playbackSpeed}x", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. MULTI-TRACK REAL EDITOR TIMELINE
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant),
                        width = 0.5.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // A. Time Ruler Markers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val durSec = (totalTimelineMs / 1000L).toInt().coerceAtLeast(1)
                            listOf(0, durSec / 4, durSec / 2, (durSec * 3) / 4, durSec).forEach { s ->
                                Text(
                                    text = formatSeconds(s),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // B. Main Video Clip Segments Track
                        val activeSegments = editorState.segments.filter { !it.isDeleted }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141418))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (activeSegments.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF222228)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No active clips (All deleted)", fontSize = 11.sp, color = Color.Gray)
                                }
                            } else {
                                activeSegments.forEachIndexed { index, seg ->
                                    val weightFraction = (seg.durationMs.toFloat() / totalTimelineMs.toFloat()).coerceIn(0.05f, 1f)
                                    val isSelected = seg.id == activeSegmentId

                                    Box(
                                        modifier = Modifier
                                            .weight(weightFraction)
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color(0xFF282834))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { viewModel.setActiveSegmentId(seg.id) }
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = "Clip ${index + 1} (${formatSeconds((seg.durationMs / 1000L).toInt())})",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // C. Overlays Track (Text & Image chips)
                        if (editorState.textOverlays.isNotEmpty() || editorState.imageOverlays.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF101014))
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                editorState.textOverlays.forEach { textOverlay ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AccentBlue.copy(alpha = 0.25f))
                                            .border(0.5.dp, AccentBlue, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("T: ${textOverlay.text.take(8)}", fontSize = 9.sp, color = AccentBlue)
                                    }
                                }
                                editorState.imageOverlays.forEach { _ ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFEAB308).copy(alpha = 0.25f))
                                            .border(0.5.dp, Color(0xFFEAB308), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("Logo", fontSize = 9.sp, color = Color(0xFFEAB308))
                                    }
                                }
                            }
                        }

                        // D. Zoom Keyframes Track (Markers)
                        if (editorState.zoomKeyframes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF101014))
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Zoom Points:", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                editorState.zoomKeyframes.forEach { kf ->
                                    Text("◆ ${formatSeconds((kf.timeMs / 1000L).toInt())} (${kf.scale}x)", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // E. Scrubbing Slider (Synchronized bidirectionally with Media3 player)
                        Slider(
                            value = timelinePositionMs.toFloat(),
                            onValueChange = { viewModel.seekTimeline(it.toLong()) },
                            valueRange = 0f..totalTimelineMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_timeline_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. EDITING TOOLS PALETTE (Real Interactive Functionality)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SECTION A: Clip Operations (Trim, Split, Delete)
                    Text(
                        text = "Clip Operations",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EditorActionChip(
                            label = "Trim",
                            icon = Icons.Filled.ContentCut,
                            onClick = { showTrimSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                        EditorActionChip(
                            label = "Split",
                            icon = Icons.Filled.ContentCut,
                            onClick = {
                                viewModel.splitAtPlayhead()
                                Toast.makeText(context, "Clip split at $currentFormatted", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        EditorActionChip(
                            label = "Delete",
                            icon = Icons.Filled.Delete,
                            tint = AccentRed,
                            onClick = {
                                if (activeSegmentId != null) {
                                    showDeleteConfirmDialog = true
                                } else {
                                    Toast.makeText(context, "Please tap a clip to select it first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // SECTION B: Overlays & Audio (Text, Image/Logo, Audio)
                    Text(
                        text = "Overlays & Audio",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EditorActionChip(
                            label = "Text",
                            icon = Icons.Filled.TextFields,
                            onClick = { showTextSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                        EditorActionChip(
                            label = "Image/Logo",
                            icon = Icons.Filled.Image,
                            onClick = {
                                // Trigger proper Android image picker
                                imagePickerLauncher.launch("image/*")
                            },
                            modifier = Modifier.weight(1f)
                        )
                        EditorActionChip(
                            label = "Audio",
                            icon = Icons.Filled.Audiotrack,
                            onClick = { showAudioSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // SECTION C: Visual Effects (Zoom Keyframes, FaceCam, Touch)
                    Text(
                        text = "Motion & PIP",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EditorActionChip(
                            label = "Zoom",
                            icon = Icons.Filled.ZoomIn,
                            onClick = { showZoomSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                        EditorActionChip(
                            label = "FaceCam",
                            icon = Icons.Filled.CameraAlt,
                            onClick = { showFaceCamSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                        EditorActionChip(
                            label = "Touch",
                            icon = Icons.Filled.TouchApp,
                            onClick = {
                                val current = editorState.touchConfig
                                viewModel.updateTouchConfig(current.copy(enabled = !current.enabled))
                                Toast.makeText(context, if (!current.enabled) "Touch effects enabled" else "Touch effects hidden", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // =========================================================================
    // MODAL BOTTOM SHEETS & INTERACTIVE DIALOGS FOR EDITING TOOLS
    // =========================================================================

    // 1. TRIM SHEET
    if (showTrimSheet) {
        val activeSeg = editorState.segments.find { it.id == activeSegmentId } ?: editorState.segments.firstOrNull()
        if (activeSeg != null) {
            var trimStart by remember { mutableLongStateOf(activeSeg.sourceStartMs) }
            var trimEnd by remember { mutableLongStateOf(activeSeg.sourceEndMs) }

            ModalBottomSheet(
                onDismissRequest = { showTrimSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Trim Active Clip", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Non-destructively adjusts the start and end in/out points of this segment. The original recording MP4 is not modified.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Start: ${formatSeconds((trimStart / 1000L).toInt())}", fontSize = 12.sp)
                        Text("End: ${formatSeconds((trimEnd / 1000L).toInt())}", fontSize = 12.sp)
                    }

                    // Trim In Slider
                    Text("Trim In (Start)", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = trimStart.toFloat(),
                        onValueChange = { trimStart = it.toLong().coerceAtMost(trimEnd - 300L) },
                        valueRange = 0f..editorState.totalSourceDurationMs.toFloat()
                    )

                    // Trim Out Slider
                    Text("Trim Out (End)", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = trimEnd.toFloat(),
                        onValueChange = { trimEnd = it.toLong().coerceAtLeast(trimStart + 300L) },
                        valueRange = 0f..editorState.totalSourceDurationMs.toFloat()
                    )

                    Button(
                        onClick = {
                            viewModel.applyTrim(activeSeg.id, trimStart, trimEnd)
                            showTrimSheet = false
                            Toast.makeText(context, "Trim applied successfully", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Trim")
                    }
                }
            }
        }
    }

    // 2. TEXT OVERLAY SHEET
    if (showTextSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTextSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add Text Overlay", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Overlay Text") },
                    placeholder = { Text("e.g. Subscribe, Follow, Step 1...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Text Color", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("#FFFFFF" to "White", "#FF3B30" to "Red", "#3B82F6" to "Blue", "#EAB308" to "Yellow").forEach { (hex, name) ->
                        FilterChip(
                            selected = textColorHex == hex,
                            onClick = { textColorHex = hex },
                            label = { Text(name, fontSize = 11.sp) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Duration: ${textDurationSec.toInt()}s", fontSize = 12.sp)
                }
                Slider(
                    value = textDurationSec,
                    onValueChange = { textDurationSec = it },
                    valueRange = 1f..15f
                )

                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val newText = TextOverlay(
                                id = UUID.randomUUID().toString(),
                                text = textInput,
                                startTimeMs = timelinePositionMs,
                                endTimeMs = (timelinePositionMs + (textDurationSec * 1000L).toLong()).coerceAtMost(editorState.effectiveDurationMs),
                                xPercent = 0.5f,
                                yPercent = 0.4f,
                                textColorHex = textColorHex,
                                backgroundColorHex = textBgColorHex
                            )
                            viewModel.addTextOverlay(newText)
                            textInput = ""
                            showTextSheet = false
                            Toast.makeText(context, "Text overlay added at $currentFormatted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = textInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add to Timeline")
                }
            }
        }
    }

    // 3. ZOOM KEYFRAME SHEET
    if (showZoomSheet) {
        var zoomScale by remember { mutableFloatStateOf(1.8f) }
        var focalPos by remember { mutableStateOf("Center") }

        ModalBottomSheet(
            onDismissRequest = { showZoomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Zoom Keyframe at $currentFormatted", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("Creates a smooth cinematic zoom transition focused on a target area during playback.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text("Zoom Scale: ${String.format(java.util.Locale.US, "%.1fx", zoomScale)}", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = zoomScale,
                    onValueChange = { zoomScale = it },
                    valueRange = 1.2f..3.0f
                )

                Text("Focal Focus Area", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Center", "Top", "Bottom", "Left", "Right").forEach { pos ->
                        FilterChip(
                            selected = focalPos == pos,
                            onClick = { focalPos = pos },
                            label = { Text(pos, fontSize = 11.sp) }
                        )
                    }
                }

                Button(
                    onClick = {
                        val (fx, fy) = when (focalPos) {
                            "Top" -> Pair(0.5f, 0.25f)
                            "Bottom" -> Pair(0.5f, 0.75f)
                            "Left" -> Pair(0.25f, 0.5f)
                            "Right" -> Pair(0.75f, 0.5f)
                            else -> Pair(0.5f, 0.5f)
                        }
                        val kf = ZoomKeyframe(
                            timeMs = timelinePositionMs,
                            focalXPercent = fx,
                            focalYPercent = fy,
                            scale = zoomScale
                        )
                        viewModel.addZoomKeyframe(kf)
                        showZoomSheet = false
                        Toast.makeText(context, "Zoom keyframe created at $currentFormatted", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Zoom Keyframe")
                }
            }
        }
    }

    // 4. FACECAM EDITOR SHEET
    if (showFaceCamSheet) {
        val fc = editorState.faceCamTrack
        var enabled by remember { mutableStateOf(fc.enabled) }
        var shape by remember { mutableStateOf(fc.shape) }
        var sizePct by remember { mutableFloatStateOf(fc.sizePercent) }

        ModalBottomSheet(
            onDismissRequest = { showFaceCamSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("FaceCam Overlay Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable FaceCam Track", fontSize = 13.sp)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                Text("Bubble Shape", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("CIRCLE" to "Circle", "ROUNDED_RECT" to "Rounded", "SQUARE" to "Square").forEach { (sh, name) ->
                        FilterChip(
                            selected = shape == sh,
                            onClick = { shape = sh },
                            label = { Text(name, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Bubble Size: ${(sizePct * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = sizePct,
                    onValueChange = { sizePct = it },
                    valueRange = 0.15f..0.45f
                )

                Button(
                    onClick = {
                        viewModel.updateFaceCamTrack(fc.copy(enabled = enabled, shape = shape, sizePercent = sizePct))
                        showFaceCamSheet = false
                        Toast.makeText(context, "FaceCam settings updated", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save FaceCam Settings")
                }
            }
        }
    }

    // 5. AUDIO SHEET
    if (showAudioSheet) {
        val audio = editorState.audioConfig
        var origVol by remember { mutableFloatStateOf(audio.originalAudioVolume) }
        var isMuted by remember { mutableStateOf(audio.originalAudioMuted) }

        ModalBottomSheet(
            onDismissRequest = { showAudioSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Audio & Volume Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mute Original Recording Audio", fontSize = 13.sp)
                    Switch(checked = isMuted, onCheckedChange = { isMuted = it })
                }

                Text("Original Recording Volume: ${(origVol * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = origVol,
                    onValueChange = { origVol = it },
                    valueRange = 0f..2.0f,
                    enabled = !isMuted
                )

                Button(
                    onClick = {
                        viewModel.updateAudioConfig(audio.copy(originalAudioVolume = origVol, originalAudioMuted = isMuted))
                        showAudioSheet = false
                        Toast.makeText(context, "Audio settings updated", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Audio Settings")
                }
            }
        }
    }

    // 6. SPEED SHEET
    if (showSpeedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Playback Speed", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                        FilterChip(
                            selected = playbackSpeed == spd,
                            onClick = {
                                viewModel.setPlaybackSpeed(spd)
                                showSpeedSheet = false
                            },
                            label = { Text("${spd}x", fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    }

    // 7. DELETE CONFIRMATION DIALOG
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Selected Segment?") },
            text = { Text("This will non-destructively remove this segment from the timeline. The original recording file is preserved and can be restored using Undo.") },
            confirmButton = {
                Button(
                    onClick = {
                        activeSegmentId?.let { viewModel.deleteSegment(it) }
                        showDeleteConfirmDialog = false
                        Toast.makeText(context, "Segment deleted (Press Undo to restore)", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Delete Segment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Reusable Editor Tool Action Chip button.
 */
@Composable
private fun EditorActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant),
            width = 0.5.dp
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("editor_chip_${label.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

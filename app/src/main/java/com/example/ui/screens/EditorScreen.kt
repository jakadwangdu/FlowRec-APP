package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.ProjectEntity
import com.example.ui.components.FlowRecTopBar
import com.example.ui.components.FlowRecVideoPlayer
import com.example.ui.components.WaveformCanvas
import com.example.ui.components.formatSeconds
import com.example.ui.theme.AccentBlue
import com.example.ui.viewmodel.EditorTool
import com.example.ui.viewmodel.FlowRecViewModel
import java.io.File

@Composable
fun EditorScreen(
    viewModel: FlowRecViewModel,
    project: ProjectEntity,
    onBackClick: () -> Unit,
    onExportClick: () -> Unit,
    onOpenEffects: () -> Unit,
    onOpenTimeline: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val playheadSec by viewModel.playheadSeconds.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val cursorEnabled by viewModel.cursorEnabled.collectAsState()
    val activeTool by viewModel.editorTool.collectAsState()

    val currentFormatted = formatSeconds(playheadSec)
    val totalFormatted = formatSeconds(project.durationSeconds)

    val isCustomFile = project.thumbnailResName.startsWith("/") || project.thumbnailResName.startsWith("file:")

    Scaffold(
        topBar = {
            FlowRecTopBar(
                title = "Editor",
                onBackClick = onBackClick,
                actions = {
                    Button(
                        onClick = onExportClick,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_editor_export")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.IosShare,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Bottom Tool Tabs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorToolItem(
                        label = "Cursor",
                        icon = Icons.Filled.Mouse,
                        isSelected = activeTool == EditorTool.CURSOR,
                        onClick = {
                            viewModel.setEditorTool(EditorTool.CURSOR)
                            onOpenEffects()
                        }
                    )
                    EditorToolItem(
                        label = "Zoom",
                        icon = Icons.Filled.ZoomIn,
                        isSelected = activeTool == EditorTool.ZOOM,
                        onClick = {
                            viewModel.setEditorTool(EditorTool.ZOOM)
                            onOpenEffects()
                        }
                    )
                    EditorToolItem(
                        label = "Blur",
                        icon = Icons.Filled.BlurOn,
                        isSelected = activeTool == EditorTool.BLUR,
                        onClick = {
                            viewModel.setEditorTool(EditorTool.BLUR)
                            onOpenEffects()
                        }
                    )
                    EditorToolItem(
                        label = "Transition",
                        icon = Icons.Filled.Animation,
                        isSelected = activeTool == EditorTool.TRANSITION,
                        onClick = {
                            viewModel.setEditorTool(EditorTool.TRANSITION)
                            onOpenEffects()
                        }
                    )
                    EditorToolItem(
                        label = "Timeline",
                        icon = Icons.Filled.MoreHoriz,
                        isSelected = activeTool == EditorTool.MORE,
                        onClick = {
                            viewModel.setEditorTool(EditorTool.MORE)
                            onOpenTimeline()
                        }
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Main Video Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            ) {
                FlowRecVideoPlayer(
                    videoPath = project.videoPath,
                    thumbnailResName = project.thumbnailResName,
                    isPlaying = isPlaying,
                    onPlayToggle = { viewModel.togglePlayPause() },
                    modifier = Modifier.fillMaxSize()
                )

                // Effect Badges Overlay (e.g. Smart Zoom 1.6x, Cursor On)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (cursorEnabled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Cursor ON",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color.White
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentBlue.copy(alpha = 0.85f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Zoom ${zoomLevel}x",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timecode & Timeline Zoom Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$currentFormatted / $totalFormatted",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.zoomTimelineIn() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { viewModel.zoomTimelineOut() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Zoom Out", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { viewModel.resetTimelineZoom() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "Reset Zoom", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onOpenTimeline,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Fullscreen, contentDescription = "Expand Timeline", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Video Strip Preview Thumbnails
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (i in 0..4) {
                    if (isCustomFile && File(project.thumbnailResName).exists()) {
                        AsyncImage(
                            model = File(project.thumbnailResName),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        )
                    } else {
                        val thumbResId = remember(project.thumbnailResName) {
                            val res = context.resources.getIdentifier(
                                project.thumbnailResName,
                                "drawable",
                                context.packageName
                            )
                            if (res != 0) res else android.R.drawable.ic_menu_gallery
                        }
                        Image(
                            painter = painterResource(id = thumbResId),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-track Timeline with Waveform Canvas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Track Headers on Left
                Column(
                    modifier = Modifier
                        .width(60.dp)
                        .height(140.dp),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    TrackHeaderLabel("Video")
                    TrackHeaderLabel("Cursor")
                    TrackHeaderLabel("Zoom")
                    TrackHeaderLabel("Effects")
                    TrackHeaderLabel("Audio")
                }

                // Waveform & Keyframe Canvas
                WaveformCanvas(
                    durationSeconds = project.durationSeconds,
                    playheadSeconds = playheadSec,
                    onSeek = { viewModel.setPlayheadSeconds(it) },
                    cursorTrackEnabled = cursorEnabled,
                    zoomTrackEnabled = true,
                    effectsTrackEnabled = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TrackHeaderLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EditorToolItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("editor_tool_${label.lowercase()}")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = tint
        )
    }
}

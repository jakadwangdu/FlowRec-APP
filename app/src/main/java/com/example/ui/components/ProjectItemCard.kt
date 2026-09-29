package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.ProjectEntity
import com.example.ui.theme.AccentRed
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Format bytes to readable string (e.g., 24.5 MB)
 */
fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f GB", bytes.toFloat() / (1024L * 1024L * 1024L))
        bytes >= 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f MB", bytes.toFloat() / (1024L * 1024L))
        bytes >= 1024L -> String.format(Locale.getDefault(), "%d KB", bytes / 1024L)
        else -> "$bytes B"
    }
}

/**
 * List Item View for a Project / Recording
 */
@Composable
fun ProjectItemCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onRename: (String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(project.name) }

    val formattedDuration = formatSeconds(project.durationSeconds)
    val formattedDate = remember(project.createdAt) {
        val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        sdf.format(Date(project.createdAt))
    }
    val formattedSize = remember(project.fileSizeBytes) { formatFileSize(project.fileSizeBytes) }
    val isCustomFile = project.thumbnailResName.startsWith("/") || project.thumbnailResName.startsWith("file:")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("project_item_${project.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Video Thumbnail Box
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val thumbModel: Any = remember(project.thumbnailResName) {
                if (isCustomFile && File(project.thumbnailResName).exists()) {
                    File(project.thumbnailResName)
                } else {
                    val res = context.resources.getIdentifier(
                        project.thumbnailResName,
                        "drawable",
                        context.packageName
                    )
                    if (res != 0) res else com.example.R.drawable.ic_flowrec_logo
                }
            }
            AsyncImage(
                model = thumbModel,
                contentDescription = project.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Duration Pill Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = formattedDuration,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Metadata
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (project.isFavorite) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorite",
                        tint = AccentRed,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Metadata: Resolution, FPS, File size, Date
            val metaParts = mutableListOf<String>()
            metaParts.add("${project.resolution} • ${project.fps}fps")
            if (project.fileSizeBytes > 0) metaParts.add(formattedSize)
            if (showDate) metaParts.add(formattedDate)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = metaParts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (project.touchMetadataPath != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Touch",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (project.facecamEnabled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(com.example.ui.theme.AccentBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Cam",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = com.example.ui.theme.AccentBlue
                        )
                    }
                }
            }
        }

        // More Actions Menu Button
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("project_menu_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Project Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit in Editor") },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onEditClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (project.isFavorite) "Remove Favorite" else "Add to Favorites") },
                    leadingIcon = {
                        Icon(
                            if (project.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (project.isFavorite) AccentRed else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        showMenu = false
                        onFavoriteToggle()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        renameText = project.name
                        showRenameDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Duplicate") },
                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onDuplicate()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = AccentRed) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = AccentRed) },
                    onClick = {
                        showMenu = false
                        showDeleteConfirmDialog = true
                    }
                )
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Project", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            onRename(renameText.trim())
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog (Non-destructive safety)
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Recording?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete \"${project.name}\"? The video file will be permanently removed from your device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = AccentRed, fontWeight = FontWeight.Bold)
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
 * Grid Card View for a Project (Used in Projects 2-column Grid mode)
 */
@Composable
fun ProjectGridCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onRename: (String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(project.name) }

    val formattedDuration = formatSeconds(project.durationSeconds)
    val formattedSize = remember(project.fileSizeBytes) { formatFileSize(project.fileSizeBytes) }
    val isCustomFile = project.thumbnailResName.startsWith("/") || project.thumbnailResName.startsWith("file:")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant),
            width = 0.5.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("project_grid_item_${project.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail Area (16:9 Aspect Ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                val thumbModel: Any = remember(project.thumbnailResName) {
                    if (isCustomFile && File(project.thumbnailResName).exists()) {
                        File(project.thumbnailResName)
                    } else {
                        val res = context.resources.getIdentifier(
                            project.thumbnailResName,
                            "drawable",
                            context.packageName
                        )
                        if (res != 0) res else com.example.R.drawable.ic_flowrec_logo
                    }
                }
                AsyncImage(
                    model = thumbModel,
                    contentDescription = project.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play Icon Center
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Duration Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }

                // Favorite badge if active
                if (project.isFavorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorite",
                            tint = AccentRed,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Info & More Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${project.resolution} · $formattedSize",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (project.touchMetadataPath != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 3.dp, vertical = 0.5.dp)
                            ) {
                                Text(
                                    text = "Touch",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (project.facecamEnabled) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(com.example.ui.theme.AccentBlue.copy(alpha = 0.15f))
                                    .padding(horizontal = 3.dp, vertical = 0.5.dp)
                            ) {
                                Text(
                                    text = "Cam",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = com.example.ui.theme.AccentBlue
                                )
                            }
                        }
                    }
                }

                // Overflow menu button
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit in Editor") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (project.isFavorite) "Remove Favorite" else "Add to Favorites") },
                            leadingIcon = {
                                Icon(
                                    if (project.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (project.isFavorite) AccentRed else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                showMenu = false
                                onFavoriteToggle()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                renameText = project.name
                                showRenameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = AccentRed) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = AccentRed) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Project", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            onRename(renameText.trim())
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Recording?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete \"${project.name}\"? The video file will be permanently removed from your device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = AccentRed, fontWeight = FontWeight.Bold)
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

fun formatSeconds(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
}

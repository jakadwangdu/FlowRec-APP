package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ProjectEntity
import com.example.ui.components.FlowRecBottomNav
import com.example.ui.components.ProjectGridCard
import com.example.ui.components.ProjectItemCard
import com.example.ui.theme.flow
import com.example.ui.viewmodel.FlowRecViewModel
import com.example.ui.viewmodel.ProjectViewMode
import com.example.ui.viewmodel.Screen

enum class ProjectFilterCategory {
    ALL,
    RECORDINGS,
    EXPORTED,
    FAVORITES
}

@Composable
fun ProjectsScreen(
    viewModel: FlowRecViewModel,
    projects: List<ProjectEntity>,
    modifier: Modifier = Modifier
) {
    val viewMode by viewModel.projectsViewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(ProjectFilterCategory.ALL) }

    val filteredProjects = remember(projects, selectedFilter, searchQuery) {
        projects.filter { proj ->
            val matchesFilter = when (selectedFilter) {
                ProjectFilterCategory.ALL -> true
                ProjectFilterCategory.RECORDINGS -> !proj.isExported
                ProjectFilterCategory.EXPORTED -> proj.isExported
                ProjectFilterCategory.FAVORITES -> proj.isFavorite
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                proj.name.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        bottomBar = {
            FlowRecBottomNav(
                currentTab = Screen.PROJECTS,
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
        ) {
            // 1. TOP BAR: Title, Search Toggle, Grid/List Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = flow.fg
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Search Button
                    IconButton(
                        onClick = { isSearchExpanded = !isSearchExpanded },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSearchExpanded) flow.fg else flow.fill)
                            .border(0.5.dp, flow.separator, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = "Search Projects",
                            tint = if (isSearchExpanded) flow.onFg else flow.fg,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Grid / List Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleProjectsViewMode() },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(flow.fill)
                            .border(0.5.dp, flow.separator, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (viewMode == ProjectViewMode.LIST) Icons.Filled.GridView else Icons.Filled.ViewList,
                            contentDescription = "Toggle Grid or List View",
                            tint = flow.fg,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. EXPANDABLE SEARCH TEXT FIELD
            AnimatedVisibility(visible = isSearchExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by recording title...", fontSize = 13.sp, color = flow.muted) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = flow.fg,
                            unfocusedBorderColor = flow.separator,
                            focusedContainerColor = flow.fill,
                            unfocusedContainerColor = flow.fill,
                            focusedTextColor = flow.fg,
                            unfocusedTextColor = flow.fg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }

            // 3. FILTER TABS (All, Recordings, Exported, Favorites)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProjectFilterCategory.values().forEach { cat ->
                    val isSelected = selectedFilter == cat
                    val label = when (cat) {
                        ProjectFilterCategory.ALL -> "All (${projects.size})"
                        ProjectFilterCategory.RECORDINGS -> "Recordings"
                        ProjectFilterCategory.EXPORTED -> "Exported"
                        ProjectFilterCategory.FAVORITES -> "Favorites"
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) flow.fg else flow.fill)
                            .border(0.5.dp, flow.separator, CircleShape)
                            .clickable { selectedFilter = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) flow.onFg else flow.fg
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. CONTENT: LIST OR GRID VIEW
            if (filteredProjects.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(flow.fill)
                                .border(0.5.dp, flow.separator, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Videocam,
                                contentDescription = null,
                                tint = flow.muted,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching recordings" else "No recordings yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = flow.fg
                        )

                        Text(
                            text = if (searchQuery.isNotBlank()) "Try searching for a different keyword" else "Start recording your phone screen or gameplay to create videos.",
                            fontSize = 13.sp,
                            color = flow.muted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { viewModel.switchBottomTab(Screen.RECORD) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = flow.fg,
                                contentColor = flow.onFg
                            )
                        ) {
                            Text("Start recording")
                        }
                    }
                }
            } else {
                if (viewMode == ProjectViewMode.LIST) {
                    // List View
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProjects, key = { it.id }) { project ->
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
                } else {
                    // 2-Column Grid View
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProjects, key = { it.id }) { project ->
                            ProjectGridCard(
                                project = project,
                                onClick = { viewModel.openProject(project) },
                                onEditClick = { viewModel.openEditorForProject(project) },
                                onRename = { newName -> viewModel.renameProject(project, newName) },
                                onDuplicate = { viewModel.duplicateProject(project) },
                                onDelete = { viewModel.deleteProject(project) },
                                onFavoriteToggle = { viewModel.toggleFavorite(project) }
                            )
                        }
                    }
                }
            }
        }
    }
}

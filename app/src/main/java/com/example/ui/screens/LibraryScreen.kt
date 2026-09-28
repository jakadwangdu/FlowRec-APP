package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ProjectEntity
import com.example.model.ProjectFilterOption
import com.example.model.ProjectSortOption
import com.example.ui.components.FlowRecBottomNav
import com.example.ui.components.ProjectItemCard
import com.example.ui.viewmodel.FlowRecViewModel
import com.example.ui.viewmodel.LibraryTab
import com.example.ui.viewmodel.Screen

@Composable
fun LibraryScreen(
    viewModel: FlowRecViewModel,
    projects: List<ProjectEntity>,
    modifier: Modifier = Modifier
) {
    // Back gesture returns to Home tab
    BackHandler {
        viewModel.switchBottomTab(Screen.HOME)
    }

    val selectedTab by viewModel.libraryTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val projectSort by viewModel.projectSort.collectAsState()
    val projectFilter by viewModel.projectFilter.collectAsState()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val hasActiveFilter = projectSort != ProjectSortOption.NEWEST || projectFilter != ProjectFilterOption.ALL

    val filteredProjects = remember(projects, selectedTab, searchQuery, projectSort, projectFilter) {
        val filtered = projects.filter { proj ->
            val matchesTab = when (selectedTab) {
                LibraryTab.ALL -> true
                LibraryTab.VIDEOS -> proj.isExported
                LibraryTab.PROJECTS -> !proj.isExported
            }
            val matchesFilter = when (projectFilter) {
                ProjectFilterOption.ALL -> true
                ProjectFilterOption.FAVORITES -> proj.isFavorite
                ProjectFilterOption.EXPORTED -> proj.isExported
                ProjectFilterOption.RAW -> !proj.isExported
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                proj.name.contains(searchQuery, ignoreCase = true)
            }
            matchesTab && matchesFilter && matchesSearch
        }

        when (projectSort) {
            ProjectSortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
            ProjectSortOption.OLDEST -> filtered.sortedBy { it.createdAt }
            ProjectSortOption.NAME_AZ -> filtered.sortedBy { it.name.lowercase() }
            ProjectSortOption.NAME_ZA -> filtered.sortedByDescending { it.name.lowercase() }
            ProjectSortOption.DURATION_DESC -> filtered.sortedByDescending { it.durationSeconds }
            ProjectSortOption.DURATION_ASC -> filtered.sortedBy { it.durationSeconds }
        }
    }

    Scaffold(
        bottomBar = {
            FlowRecBottomNav(
                currentTab = Screen.LIBRARY,
                onTabSelected = { tab ->
                    viewModel.switchBottomTab(tab)
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isSearchExpanded = !isSearchExpanded },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("btn_library_search")
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Filter & Sort Button with Active Filter Indicator Badge
                        Box {
                            IconButton(
                                onClick = { showFilterMenu = true },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("btn_library_filter")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = "Filter and Sort",
                                    tint = if (hasActiveFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (hasActiveFilter) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }

                            // Interactive Dropdown for Sort and Filter
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                Text(
                                    text = "SORT BY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )

                                ProjectSortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = option.label,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (projectSort == option) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                                if (projectSort == option) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.setProjectSort(option)
                                            showFilterMenu = false
                                        }
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Text(
                                    text = "FILTER BY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )

                                ProjectFilterOption.values().forEach { filterOpt ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = filterOpt.label,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (projectFilter == filterOpt) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                                if (projectFilter == filterOpt) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.setProjectFilter(filterOpt)
                                            showFilterMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            if (isSearchExpanded) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search recordings...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("library_search_input"),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true
                    )
                }
            }

            // Category Tab Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryFilterChip(
                        label = "All",
                        count = projects.size,
                        isSelected = selectedTab == LibraryTab.ALL,
                        onClick = { viewModel.setLibraryTab(LibraryTab.ALL) }
                    )
                    LibraryFilterChip(
                        label = "Videos",
                        count = projects.count { it.isExported },
                        isSelected = selectedTab == LibraryTab.VIDEOS,
                        onClick = { viewModel.setLibraryTab(LibraryTab.VIDEOS) }
                    )
                    LibraryFilterChip(
                        label = "Projects",
                        count = projects.count { !it.isExported },
                        isSelected = selectedTab == LibraryTab.PROJECTS,
                        onClick = { viewModel.setLibraryTab(LibraryTab.PROJECTS) }
                    )
                }
            }

            // Items List
            if (filteredProjects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No recordings matching \"$searchQuery\"" else "No recordings found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
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

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LibraryFilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = textColor
        )
    }
}

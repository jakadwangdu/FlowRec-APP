package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.entity.ProjectEntity
import com.example.ui.viewmodel.FlowRecViewModel

/**
 * Backward-compatible wrapper pointing to ProjectsScreen
 */
@Composable
fun LibraryScreen(
    viewModel: FlowRecViewModel,
    projects: List<ProjectEntity>,
    modifier: Modifier = Modifier
) {
    ProjectsScreen(
        viewModel = viewModel,
        projects = projects,
        modifier = modifier
    )
}

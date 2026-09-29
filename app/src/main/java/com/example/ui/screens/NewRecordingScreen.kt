package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.FlowRecViewModel

/**
 * Backward-compatible wrapper for RecordScreen
 */
@Composable
fun NewRecordingScreen(
    viewModel: FlowRecViewModel,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RecordScreen(
        viewModel = viewModel,
        onStartRecordingClick = onNextClick,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

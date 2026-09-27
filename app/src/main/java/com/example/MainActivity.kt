package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.model.AppThemeMode
import com.example.ui.screens.CountdownScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.EffectsPanelScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NewRecordingScreen
import com.example.ui.screens.ProjectDetailsScreen
import com.example.ui.screens.RecordingHudScreen
import com.example.ui.screens.ScreenSelectionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.screens.VideoReadyScreen
import com.example.ui.theme.FlowRecTheme
import com.example.ui.viewmodel.FlowRecViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: FlowRecViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            FlowRecTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FlowRecApp(
                        activity = this@MainActivity,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun FlowRecApp(
    activity: ComponentActivity,
    viewModel: FlowRecViewModel
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val projects by viewModel.allProjects.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()

    DisposableEffect(Unit) {
        viewModel.recorderEngine.onExternalStopListener = { project ->
            viewModel.onRecordingFinished(project)
        }
        onDispose {
            viewModel.recorderEngine.onExternalStopListener = null
        }
    }

    // Permission launchers
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(activity, "Microphone audio disabled", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Notification permission result handled by system */ }

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.recorderEngine.setProjectionPermission(result.resultCode, result.data)
            viewModel.navigateTo(Screen.COUNTDOWN)
        } else {
            Toast.makeText(activity, "Screen recording permission was not granted", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestScreenCaptureAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (viewModel.recorderEngine.config.value.recordMicrophone) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        val projectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (projectionManager != null) {
            try {
                projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
            } catch (e: Exception) {
                Toast.makeText(activity, "Could not launch screen capture: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(activity, "MediaProjection service unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            Screen.HOME -> {
                HomeScreen(
                    viewModel = viewModel,
                    projects = projects,
                    onNewRecordingClick = { viewModel.navigateTo(Screen.NEW_RECORDING) },
                    onViewAllClick = { viewModel.switchBottomTab(Screen.LIBRARY) },
                    onSettingsClick = { viewModel.navigateTo(Screen.SETTINGS) }
                )
            }

            Screen.NEW_RECORDING -> {
                NewRecordingScreen(
                    viewModel = viewModel,
                    onBackClick = { viewModel.navigateBack() },
                    onNextClick = { viewModel.navigateTo(Screen.SCREEN_SELECTION) }
                )
            }

            Screen.SCREEN_SELECTION -> {
                ScreenSelectionScreen(
                    viewModel = viewModel,
                    onBackClick = { viewModel.navigateBack() },
                    onNextClick = { requestScreenCaptureAndStart() }
                )
            }

            Screen.COUNTDOWN -> {
                CountdownScreen(
                    viewModel = viewModel,
                    onCancel = { viewModel.navigateBack() },
                    onCountdownFinished = { viewModel.navigateTo(Screen.RECORDING_HUD) }
                )
            }

            Screen.RECORDING_HUD -> {
                RecordingHudScreen(
                    viewModel = viewModel,
                    onRecordingComplete = { /* Handled in engine callback */ }
                )
            }

            Screen.LIBRARY -> {
                LibraryScreen(
                    viewModel = viewModel,
                    projects = projects
                )
            }

            Screen.PROJECT_DETAILS -> {
                selectedProject?.let { proj ->
                    ProjectDetailsScreen(
                        viewModel = viewModel,
                        project = proj,
                        onBackClick = { viewModel.navigateBack() },
                        onEditClick = { viewModel.openEditorForProject(proj) }
                    )
                } ?: HomeScreen(
                    viewModel = viewModel,
                    projects = projects,
                    onNewRecordingClick = { viewModel.navigateTo(Screen.NEW_RECORDING) },
                    onViewAllClick = { viewModel.switchBottomTab(Screen.LIBRARY) },
                    onSettingsClick = { viewModel.navigateTo(Screen.SETTINGS) }
                )
            }

            Screen.EDITOR -> {
                selectedProject?.let { proj ->
                    EditorScreen(
                        viewModel = viewModel,
                        project = proj,
                        onBackClick = { viewModel.navigateBack() },
                        onExportClick = { viewModel.navigateTo(Screen.EXPORT_SETTINGS) },
                        onOpenEffects = { viewModel.navigateTo(Screen.EFFECTS) },
                        onOpenTimeline = { viewModel.navigateTo(Screen.TIMELINE) }
                    )
                } ?: HomeScreen(
                    viewModel = viewModel,
                    projects = projects,
                    onNewRecordingClick = { viewModel.navigateTo(Screen.NEW_RECORDING) },
                    onViewAllClick = { viewModel.switchBottomTab(Screen.LIBRARY) },
                    onSettingsClick = { viewModel.navigateTo(Screen.SETTINGS) }
                )
            }

            Screen.EFFECTS -> {
                EffectsPanelScreen(
                    viewModel = viewModel,
                    onBackClick = { viewModel.navigateBack() },
                    onNavigateToRecording = { viewModel.navigateTo(Screen.NEW_RECORDING) },
                    onNavigateToExport = { viewModel.navigateTo(Screen.EXPORT_SETTINGS) }
                )
            }

            Screen.TIMELINE -> {
                selectedProject?.let { proj ->
                    TimelineScreen(
                        viewModel = viewModel,
                        project = proj,
                        onBackClick = { viewModel.navigateBack() }
                    )
                }
            }

            Screen.EXPORT_SETTINGS -> {
                selectedProject?.let { proj ->
                    ExportScreen(
                        viewModel = viewModel,
                        project = proj,
                        onBackClick = { viewModel.navigateBack() },
                        onExportFinished = { viewModel.navigateTo(Screen.VIDEO_READY) }
                    )
                }
            }

            Screen.VIDEO_READY -> {
                VideoReadyScreen(
                    viewModel = viewModel,
                    project = selectedProject,
                    onOpenFile = {
                        val proj = selectedProject
                        if (proj != null) {
                            viewModel.openProject(proj)
                        } else {
                            viewModel.switchBottomTab(Screen.LIBRARY)
                        }
                    },
                    onViewInLibrary = {
                        viewModel.switchBottomTab(Screen.LIBRARY)
                    }
                )
            }

            Screen.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { viewModel.navigateBack() }
                )
            }
        }
    }
}

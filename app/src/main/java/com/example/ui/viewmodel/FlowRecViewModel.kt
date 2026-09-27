package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FlowRecDatabase
import com.example.data.entity.ProjectEntity
import com.example.model.AppThemeMode
import com.example.model.ExportFormat
import com.example.model.ExportPreset
import com.example.model.ExportQuality
import com.example.model.RecordingResolution
import com.example.recorder.RecorderEngine
import com.example.recorder.RecordingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class Screen {
    HOME,
    NEW_RECORDING,
    SCREEN_SELECTION,
    COUNTDOWN,
    RECORDING_HUD,
    LIBRARY,
    PROJECT_DETAILS,
    EDITOR,
    EFFECTS,
    TIMELINE,
    EXPORT_SETTINGS,
    VIDEO_READY,
    SETTINGS
}

enum class LibraryTab {
    ALL,
    VIDEOS,
    PROJECTS
}

enum class EditorTool {
    CURSOR,
    ZOOM,
    BLUR,
    TRANSITION,
    MORE
}

class FlowRecViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FlowRecDatabase.getDatabase(application, viewModelScope)
    private val dao = db.projectDao()

    val recorderEngine = RecorderEngine(application, viewModelScope)

    val allProjects: StateFlow<List<ProjectEntity>> = dao.getAllProjectsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getAllProjects()
            if (existing.isEmpty()) {
                FlowRecDatabase.prepopulateProjects(dao)
            }
        }
    }

    // Screen navigation stack
    private val _navigationStack = MutableStateFlow(listOf(Screen.HOME))
    val currentScreen: StateFlow<Screen> = MutableStateFlow(Screen.HOME).apply {
        viewModelScope.launch {
            _navigationStack.collect { stack ->
                value = stack.lastOrNull() ?: Screen.HOME
            }
        }
    }

    // Active bottom navigation tab
    private val _bottomNavTab = MutableStateFlow(Screen.HOME)
    val bottomNavTab: StateFlow<Screen> = _bottomNavTab.asStateFlow()

    // Selected project for details/editor/export
    private val _selectedProject = MutableStateFlow<ProjectEntity?>(null)
    val selectedProject: StateFlow<ProjectEntity?> = _selectedProject.asStateFlow()

    // Library tab & search
    private val _libraryTab = MutableStateFlow(LibraryTab.ALL)
    val libraryTab: StateFlow<LibraryTab> = _libraryTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // SharedPreferences for persistent app settings
    private val prefs = application.getSharedPreferences("flowrec_settings", Context.MODE_PRIVATE)

    // Theme Mode (Default is SYSTEM: follows phone's Dark / Light mode)
    private val _themeMode = MutableStateFlow(
        try {
            val saved = prefs.getString("app_theme", AppThemeMode.SYSTEM.name)
            AppThemeMode.valueOf(saved ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // Editor state
    private val _editorTool = MutableStateFlow(EditorTool.CURSOR)
    val editorTool: StateFlow<EditorTool> = _editorTool.asStateFlow()

    private val _playheadSeconds = MutableStateFlow(84) // 01:24
    val playheadSeconds: StateFlow<Int> = _playheadSeconds.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _timelineZoomLevel = MutableStateFlow(1.0f)
    val timelineZoomLevel: StateFlow<Float> = _timelineZoomLevel.asStateFlow()

    // Active effects edit state
    private val _cursorEnabled = MutableStateFlow(true)
    val cursorEnabled: StateFlow<Boolean> = _cursorEnabled.asStateFlow()

    private val _cursorStyle = MutableStateFlow("DEFAULT")
    val cursorStyle: StateFlow<String> = _cursorStyle.asStateFlow()

    private val _cursorSize = MutableStateFlow(120)
    val cursorSize: StateFlow<Int> = _cursorSize.asStateFlow()

    private val _clickZoomEnabled = MutableStateFlow(true)
    val clickZoomEnabled: StateFlow<Boolean> = _clickZoomEnabled.asStateFlow()

    private val _zoomLevel = MutableStateFlow(1.6f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    private val _zoomDurationMs = MutableStateFlow(350)
    val zoomDurationMs: StateFlow<Int> = _zoomDurationMs.asStateFlow()

    private val _zoomEasing = MutableStateFlow("Smooth")
    val zoomEasing: StateFlow<String> = _zoomEasing.asStateFlow()

    private val _motionBlurEnabled = MutableStateFlow(true)
    val motionBlurEnabled: StateFlow<Boolean> = _motionBlurEnabled.asStateFlow()

    private val _blurAmount = MutableStateFlow(18)
    val blurAmount: StateFlow<Int> = _blurAmount.asStateFlow()

    // Export state
    private val _exportPreset = MutableStateFlow(ExportPreset.YOUTUBE_1080P)
    val exportPreset: StateFlow<ExportPreset> = _exportPreset.asStateFlow()

    private val _exportFormat = MutableStateFlow(ExportFormat.MP4)
    val exportFormat: StateFlow<ExportFormat> = _exportFormat.asStateFlow()

    private val _exportQuality = MutableStateFlow(ExportQuality.HIGH)
    val exportQuality: StateFlow<ExportQuality> = _exportQuality.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private var playbackJob: Job? = null
    private var exportJob: Job? = null

    // Navigation Methods
    fun navigateTo(screen: Screen) {
        val currentStack = _navigationStack.value.toMutableList()
        if (screen == Screen.HOME || screen == Screen.LIBRARY || screen == Screen.SETTINGS) {
            _bottomNavTab.value = screen
        }
        currentStack.add(screen)
        _navigationStack.value = currentStack
    }

    fun switchBottomTab(screen: Screen) {
        _bottomNavTab.value = screen
        _navigationStack.value = listOf(screen)
    }

    fun navigateBack(): Boolean {
        val currentStack = _navigationStack.value.toMutableList()
        return if (currentStack.size > 1) {
            currentStack.removeAt(currentStack.lastIndex)
            _navigationStack.value = currentStack
            val newTop = currentStack.last()
            if (newTop == Screen.HOME || newTop == Screen.LIBRARY || newTop == Screen.SETTINGS) {
                _bottomNavTab.value = newTop
            }
            true
        } else {
            false
        }
    }

    fun openProject(project: ProjectEntity) {
        _selectedProject.value = project
        _cursorEnabled.value = project.cursorEnabled
        _cursorStyle.value = project.cursorStyle
        _cursorSize.value = project.cursorSizePercent
        _clickZoomEnabled.value = project.clickZoomEnabled
        _zoomLevel.value = project.zoomLevel
        _zoomDurationMs.value = project.zoomDurationMs
        _zoomEasing.value = project.zoomEasing
        _motionBlurEnabled.value = project.motionBlurEnabled
        _blurAmount.value = project.blurAmountPercent
        _playheadSeconds.value = (project.durationSeconds * 0.3f).toInt()
        navigateTo(Screen.PROJECT_DETAILS)
    }

    fun openEditorForProject(project: ProjectEntity) {
        _selectedProject.value = project
        _cursorEnabled.value = project.cursorEnabled
        _cursorStyle.value = project.cursorStyle
        _cursorSize.value = project.cursorSizePercent
        _clickZoomEnabled.value = project.clickZoomEnabled
        _zoomLevel.value = project.zoomLevel
        _zoomDurationMs.value = project.zoomDurationMs
        _zoomEasing.value = project.zoomEasing
        _motionBlurEnabled.value = project.motionBlurEnabled
        _blurAmount.value = project.blurAmountPercent
        _playheadSeconds.value = (project.durationSeconds * 0.3f).toInt()
        navigateTo(Screen.EDITOR)
    }

    fun setLibraryTab(tab: LibraryTab) {
        _libraryTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("app_theme", mode.name).apply()
    }

    fun toggleTheme(isCurrentlyDark: Boolean) {
        val nextMode = if (isCurrentlyDark) AppThemeMode.LIGHT else AppThemeMode.DARK
        setThemeMode(nextMode)
    }

    fun setEditorTool(tool: EditorTool) {
        _editorTool.value = tool
    }

    fun setPlayheadSeconds(seconds: Int) {
        val maxDuration = _selectedProject.value?.durationSeconds ?: 300
        _playheadSeconds.value = seconds.coerceIn(0, maxDuration)
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
        if (_isPlaying.value) {
            playbackJob?.cancel()
            playbackJob = viewModelScope.launch(Dispatchers.Default) {
                val maxDuration = _selectedProject.value?.durationSeconds ?: 300
                while (_isPlaying.value) {
                    delay(1000L)
                    val next = _playheadSeconds.value + 1
                    if (next >= maxDuration) {
                        _playheadSeconds.value = 0
                        _isPlaying.value = false
                    } else {
                        _playheadSeconds.value = next
                    }
                }
            }
        } else {
            playbackJob?.cancel()
        }
    }

    fun zoomTimelineIn() {
        _timelineZoomLevel.value = (_timelineZoomLevel.value + 0.25f).coerceAtMost(3.0f)
    }

    fun zoomTimelineOut() {
        _timelineZoomLevel.value = (_timelineZoomLevel.value - 0.25f).coerceAtLeast(0.5f)
    }

    fun resetTimelineZoom() {
        _timelineZoomLevel.value = 1.0f
    }

    // Effects mutations
    fun setCursorEnabled(enabled: Boolean) {
        _cursorEnabled.value = enabled
        saveCurrentProjectEffects()
    }

    fun setCursorStyle(style: String) {
        _cursorStyle.value = style
        saveCurrentProjectEffects()
    }

    fun setCursorSize(size: Int) {
        _cursorSize.value = size
        saveCurrentProjectEffects()
    }

    fun setClickZoomEnabled(enabled: Boolean) {
        _clickZoomEnabled.value = enabled
        saveCurrentProjectEffects()
    }

    fun setZoomLevel(level: Float) {
        _zoomLevel.value = level
        saveCurrentProjectEffects()
    }

    fun setZoomDuration(durationMs: Int) {
        _zoomDurationMs.value = durationMs
        saveCurrentProjectEffects()
    }

    fun setZoomEasing(easing: String) {
        _zoomEasing.value = easing
        saveCurrentProjectEffects()
    }

    fun setMotionBlurEnabled(enabled: Boolean) {
        _motionBlurEnabled.value = enabled
        saveCurrentProjectEffects()
    }

    fun setBlurAmount(amount: Int) {
        _blurAmount.value = amount
        saveCurrentProjectEffects()
    }

    private fun saveCurrentProjectEffects() {
        val proj = _selectedProject.value ?: return
        val updated = proj.copy(
            cursorEnabled = _cursorEnabled.value,
            cursorStyle = _cursorStyle.value,
            cursorSizePercent = _cursorSize.value,
            clickZoomEnabled = _clickZoomEnabled.value,
            zoomLevel = _zoomLevel.value,
            zoomDurationMs = _zoomDurationMs.value,
            zoomEasing = _zoomEasing.value,
            motionBlurEnabled = _motionBlurEnabled.value,
            blurAmountPercent = _blurAmount.value
        )
        _selectedProject.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateProject(updated)
        }
    }

    // Exporting
    fun setExportPreset(preset: ExportPreset) {
        _exportPreset.value = preset
    }

    fun setExportFormat(format: ExportFormat) {
        _exportFormat.value = format
    }

    fun setExportQuality(quality: ExportQuality) {
        _exportQuality.value = quality
    }

    fun startExport(onCompleted: () -> Unit) {
        _isExporting.value = true
        _exportProgress.value = 0f
        exportJob?.cancel()
        exportJob = viewModelScope.launch(Dispatchers.Default) {
            for (i in 1..100) {
                delay(30L)
                _exportProgress.value = i / 100f
            }
            _isExporting.value = false
            _selectedProject.value?.let { proj ->
                val updated = proj.copy(isExported = true)
                _selectedProject.value = updated
                dao.updateProject(updated)
            }
            viewModelScope.launch(Dispatchers.Main) {
                onCompleted()
                navigateTo(Screen.VIDEO_READY)
            }
        }
    }

    // Project Actions
    fun toggleFavorite(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.toggleFavorite(project.id)
            if (_selectedProject.value?.id == project.id) {
                _selectedProject.value = _selectedProject.value?.copy(isFavorite = !project.isFavorite)
            }
        }
    }

    fun renameProject(project: ProjectEntity, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.renameProject(project.id, newName)
            if (_selectedProject.value?.id == project.id) {
                _selectedProject.value = _selectedProject.value?.copy(name = newName)
            }
        }
    }

    fun duplicateProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val duplicate = project.copy(
                id = UUID.randomUUID().toString(),
                name = "${project.name} (Copy)",
                createdAt = System.currentTimeMillis()
            )
            dao.insertProject(duplicate)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteProject(project)
            if (_selectedProject.value?.id == project.id) {
                _selectedProject.value = null
            }
        }
        if (_selectedProject.value?.id == project.id) {
            navigateBack()
        }
    }

    fun onRecordingFinished(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertProject(project)
            _selectedProject.value = project
            viewModelScope.launch(Dispatchers.Main) {
                openEditorForProject(project)
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllProjects()
        }
    }
}

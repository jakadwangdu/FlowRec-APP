package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FlowRecDatabase
import com.example.data.entity.ProjectEntity
import com.example.model.AppThemeMode
import com.example.model.AudioSourceMode
import com.example.model.CountdownOption
import com.example.model.ExportFormat
import com.example.model.ExportPreset
import com.example.model.ExportQuality
import com.example.model.FrameRate
import com.example.model.RecordingResolution
import com.example.model.VideoOrientation
import com.example.model.ProjectFilterOption
import com.example.model.ProjectSortOption
import com.example.recorder.RecorderEngine
import com.example.recorder.RecordingConfig
import com.example.util.GalleryExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
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
    TOUCH,
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
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteDemoProjects()
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

    // Library Sorting & Filtering
    private val _projectSort = MutableStateFlow(ProjectSortOption.NEWEST)
    val projectSort: StateFlow<ProjectSortOption> = _projectSort.asStateFlow()

    private val _projectFilter = MutableStateFlow(ProjectFilterOption.ALL)
    val projectFilter: StateFlow<ProjectFilterOption> = _projectFilter.asStateFlow()

    // Dynamic Tap/Click Zoom on Screen
    private val _zoomFocalPoint = MutableStateFlow(Pair(0.5f, 0.5f))
    val zoomFocalPoint: StateFlow<Pair<Float, Float>> = _zoomFocalPoint.asStateFlow()

    private val _isZoomActive = MutableStateFlow(false)
    val isZoomActive: StateFlow<Boolean> = _isZoomActive.asStateFlow()

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

    // Mobile Phone Screen Recording Settings
    private val _showTouches = MutableStateFlow(prefs.getBoolean("show_touches", true))
    val showTouches: StateFlow<Boolean> = _showTouches.asStateFlow()

    private val _floatingBubbleEnabled = MutableStateFlow(prefs.getBoolean("floating_bubble", true))
    val floatingBubbleEnabled: StateFlow<Boolean> = _floatingBubbleEnabled.asStateFlow()

    private val _shakeToStop = MutableStateFlow(prefs.getBoolean("shake_to_stop", false))
    val shakeToStop: StateFlow<Boolean> = _shakeToStop.asStateFlow()

    private val _autoSaveToGallery = MutableStateFlow(prefs.getBoolean("auto_save_gallery", true))
    val autoSaveToGallery: StateFlow<Boolean> = _autoSaveToGallery.asStateFlow()

    private val _audioSourceMode = MutableStateFlow(
        try {
            AudioSourceMode.valueOf(prefs.getString("audio_source_mode", AudioSourceMode.MIC_AND_SYSTEM.name) ?: AudioSourceMode.MIC_AND_SYSTEM.name)
        } catch (e: Exception) {
            AudioSourceMode.MIC_AND_SYSTEM
        }
    )
    val audioSourceMode: StateFlow<AudioSourceMode> = _audioSourceMode.asStateFlow()

    private val _videoOrientation = MutableStateFlow(
        try {
            VideoOrientation.valueOf(prefs.getString("video_orientation", VideoOrientation.AUTO.name) ?: VideoOrientation.AUTO.name)
        } catch (e: Exception) {
            VideoOrientation.AUTO
        }
    )
    val videoOrientation: StateFlow<VideoOrientation> = _videoOrientation.asStateFlow()

    private val _countdownOption = MutableStateFlow(
        try {
            CountdownOption.valueOf(prefs.getString("countdown_option", CountdownOption.SEC_3.name) ?: CountdownOption.SEC_3.name)
        } catch (e: Exception) {
            CountdownOption.SEC_3
        }
    )
    val countdownOption: StateFlow<CountdownOption> = _countdownOption.asStateFlow()

    private val _defaultResolution = MutableStateFlow(
        try {
            RecordingResolution.valueOf(prefs.getString("default_res", RecordingResolution.RES_1080P.name) ?: RecordingResolution.RES_1080P.name)
        } catch (e: Exception) {
            RecordingResolution.RES_1080P
        }
    )
    val defaultResolution: StateFlow<RecordingResolution> = _defaultResolution.asStateFlow()

    private val _defaultFps = MutableStateFlow(
        try {
            FrameRate.valueOf(prefs.getString("default_fps", FrameRate.FPS_30.name) ?: FrameRate.FPS_30.name)
        } catch (e: Exception) {
            FrameRate.FPS_30
        }
    )
    val defaultFps: StateFlow<FrameRate> = _defaultFps.asStateFlow()

    fun setShowTouches(enabled: Boolean) {
        _showTouches.value = enabled
        prefs.edit().putBoolean("show_touches", enabled).apply()
        syncRecorderConfig()
    }

    fun setFloatingBubbleEnabled(enabled: Boolean) {
        _floatingBubbleEnabled.value = enabled
        prefs.edit().putBoolean("floating_bubble", enabled).apply()
        syncRecorderConfig()
    }

    fun setShakeToStop(enabled: Boolean) {
        _shakeToStop.value = enabled
        prefs.edit().putBoolean("shake_to_stop", enabled).apply()
        syncRecorderConfig()
    }

    fun setAutoSaveToGallery(enabled: Boolean) {
        _autoSaveToGallery.value = enabled
        prefs.edit().putBoolean("auto_save_gallery", enabled).apply()
        syncRecorderConfig()
    }

    fun setAudioSourceMode(mode: AudioSourceMode) {
        _audioSourceMode.value = mode
        prefs.edit().putString("audio_source_mode", mode.name).apply()
        syncRecorderConfig()
    }

    fun setVideoOrientation(orientation: VideoOrientation) {
        _videoOrientation.value = orientation
        prefs.edit().putString("video_orientation", orientation.name).apply()
        syncRecorderConfig()
    }

    fun setCountdownOption(option: CountdownOption) {
        _countdownOption.value = option
        prefs.edit().putString("countdown_option", option.name).apply()
        syncRecorderConfig()
    }

    fun setDefaultResolution(res: RecordingResolution) {
        _defaultResolution.value = res
        prefs.edit().putString("default_res", res.name).apply()
        syncRecorderConfig()
    }

    fun setDefaultFps(fps: FrameRate) {
        _defaultFps.value = fps
        prefs.edit().putString("default_fps", fps.name).apply()
        syncRecorderConfig()
    }

    private fun syncRecorderConfig() {
        val audioMode = _audioSourceMode.value
        recorderEngine.updateConfig(
            RecordingConfig(
                resolution = _defaultResolution.value,
                frameRate = _defaultFps.value,
                audioSource = audioMode,
                recordSystemAudio = audioMode != AudioSourceMode.NONE,
                recordMicrophone = audioMode == AudioSourceMode.MIC_AND_SYSTEM,
                showTouches = _showTouches.value,
                floatingBubbleEnabled = _floatingBubbleEnabled.value,
                autoSaveToGallery = _autoSaveToGallery.value,
                orientation = _videoOrientation.value,
                countdownOption = _countdownOption.value,
                shakeToStop = _shakeToStop.value
            )
        )
    }

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

    // Phone Touch Feedback & Mobile Effects state
    private val _touchFeedbackStyle = MutableStateFlow("RIPPLE") // RIPPLE, GLOW, TARGET, POINTER
    val touchFeedbackStyle: StateFlow<String> = _touchFeedbackStyle.asStateFlow()

    private val _touchFeedbackColor = MutableStateFlow("#FF4444") // Red, Cyan, Yellow, White
    val touchFeedbackColor: StateFlow<String> = _touchFeedbackColor.asStateFlow()

    // Mobile Facecam Overlay
    private val _facecamEnabled = MutableStateFlow(false)
    val facecamEnabled: StateFlow<Boolean> = _facecamEnabled.asStateFlow()

    private val _facecamShape = MutableStateFlow("CIRCLE") // CIRCLE, RECT
    val facecamShape: StateFlow<String> = _facecamShape.asStateFlow()

    private val _facecamSize = MutableStateFlow("MEDIUM") // SMALL, MEDIUM, LARGE
    val facecamSize: StateFlow<String> = _facecamSize.asStateFlow()

    // Screen Brush & Annotation
    private val _brushEnabled = MutableStateFlow(false)
    val brushEnabled: StateFlow<Boolean> = _brushEnabled.asStateFlow()

    private val _brushColor = MutableStateFlow("#00E5FF")
    val brushColor: StateFlow<String> = _brushColor.asStateFlow()

    private val _brushThickness = MutableStateFlow(6)
    val brushThickness: StateFlow<Int> = _brushThickness.asStateFlow()

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
        } else if (currentStack.isNotEmpty() && currentStack.last() != Screen.HOME) {
            _bottomNavTab.value = Screen.HOME
            _navigationStack.value = listOf(Screen.HOME)
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

    fun setProjectSort(sort: ProjectSortOption) {
        _projectSort.value = sort
    }

    fun setProjectFilter(filter: ProjectFilterOption) {
        _projectFilter.value = filter
    }

    fun setZoomFocalPoint(x: Float, y: Float) {
        _zoomFocalPoint.value = Pair(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
    }

    fun toggleZoomActive() {
        _isZoomActive.value = !_isZoomActive.value
    }

    fun setZoomActive(active: Boolean) {
        _isZoomActive.value = active
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

    fun setTouchFeedbackStyle(style: String) {
        _touchFeedbackStyle.value = style
        _cursorStyle.value = style
        saveCurrentProjectEffects()
    }

    fun setTouchFeedbackColor(color: String) {
        _touchFeedbackColor.value = color
    }

    fun setFacecamEnabled(enabled: Boolean) {
        _facecamEnabled.value = enabled
    }

    fun setFacecamShape(shape: String) {
        _facecamShape.value = shape
    }

    fun setFacecamSize(size: String) {
        _facecamSize.value = size
    }

    fun setBrushEnabled(enabled: Boolean) {
        _brushEnabled.value = enabled
    }

    fun setBrushColor(color: String) {
        _brushColor.value = color
    }

    fun setBrushThickness(thickness: Int) {
        _brushThickness.value = thickness
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
        exportJob = viewModelScope.launch(Dispatchers.IO) {
            for (i in 1..65) {
                delay(20L)
                _exportProgress.value = i / 100f
            }

            val proj = _selectedProject.value
            var savedGalleryPath: String? = null

            if (proj != null) {
                var sourceFile: File? = if (proj.videoPath.isNotBlank() && !proj.videoPath.startsWith("content:")) {
                    File(proj.videoPath)
                } else null

                if (sourceFile == null || !sourceFile.exists()) {
                    val recordingsDir = File(getApplication<Application>().filesDir, "recordings")
                    val candidates = recordingsDir.listFiles { f -> f.extension == "mp4" }
                    sourceFile = candidates?.maxByOrNull { it.lastModified() }
                }

                if (sourceFile != null && sourceFile.exists()) {
                    val uri = GalleryExporter.saveVideoToGallery(getApplication(), sourceFile, proj.name)
                    if (uri != null) {
                        savedGalleryPath = uri.toString()
                    }
                }
            }

            for (i in 66..100) {
                delay(15L)
                _exportProgress.value = i / 100f
            }

            _isExporting.value = false
            if (proj != null) {
                val updated = proj.copy(
                    isExported = true,
                    videoPath = savedGalleryPath ?: proj.videoPath
                )
                _selectedProject.value = updated
                dao.updateProject(updated)
            }

            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    getApplication(),
                    "Video exported & saved to Gallery (Movies/FlowRec)",
                    Toast.LENGTH_LONG
                ).show()
                onCompleted()
                navigateTo(Screen.VIDEO_READY)
            }
        }
    }

    fun saveProjectToGallery(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            var sourceFile: File? = if (project.videoPath.isNotBlank() && !project.videoPath.startsWith("content:")) {
                File(project.videoPath)
            } else null

            if (sourceFile == null || !sourceFile.exists()) {
                val recordingsDir = File(getApplication<Application>().filesDir, "recordings")
                val candidates = recordingsDir.listFiles { f -> f.extension == "mp4" }
                sourceFile = candidates?.maxByOrNull { it.lastModified() }
            }

            if (sourceFile != null && sourceFile.exists()) {
                val uri = GalleryExporter.saveVideoToGallery(getApplication(), sourceFile, project.name)
                if (uri != null) {
                    val updated = project.copy(isExported = true, videoPath = uri.toString())
                    dao.updateProject(updated)
                    if (_selectedProject.value?.id == project.id) {
                        _selectedProject.value = updated
                    }
                    viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Saved to Gallery (Movies/FlowRec)", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
            }
            viewModelScope.launch(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Could not locate recorded video file", Toast.LENGTH_SHORT).show()
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

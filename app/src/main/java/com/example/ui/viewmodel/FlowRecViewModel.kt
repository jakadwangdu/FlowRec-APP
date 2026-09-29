package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
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
import java.io.FileOutputStream
import java.util.UUID
import com.example.recorder.touch.TouchEffectConfig
import com.example.editor.model.AudioTrackConfig
import com.example.editor.model.EditorProjectState
import com.example.editor.model.FaceCamEditorTrack
import com.example.editor.model.ImageOverlay
import com.example.editor.model.TextOverlay
import com.example.editor.model.TimelineSegment
import com.example.editor.model.TouchOverlayConfig
import com.example.editor.model.ZoomKeyframe
import com.example.editor.history.EditorHistoryManager
import com.example.editor.timeline.TimelineManager
import com.example.recorder.touch.FlowTouchEvent
import com.example.recorder.touch.TouchTracker

enum class Screen {
    HOME,
    RECORD,
    PROJECTS,
    EDITOR,
    SETTINGS,
    NEW_RECORDING,
    SCREEN_SELECTION,
    COUNTDOWN,
    RECORDING_HUD,
    LIBRARY,
    PROJECT_DETAILS,
    EFFECTS,
    TIMELINE,
    EXPORT_SETTINGS,
    VIDEO_READY
}

enum class ProjectViewMode {
    LIST,
    GRID
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
            scanAndRecoverOrphanedRecordings()
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
        AudioSourceMode.fromString(
            prefs.getString("audio_source_mode", AudioSourceMode.MIC_AND_INTERNAL.name) ?: AudioSourceMode.MIC_AND_INTERNAL.name
        )
    )
    val audioSourceMode: StateFlow<AudioSourceMode> = _audioSourceMode.asStateFlow()

    private val _isGameMode = MutableStateFlow(prefs.getBoolean("is_game_mode", false))
    val isGameMode: StateFlow<Boolean> = _isGameMode.asStateFlow()

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

    fun setGameMode(enabled: Boolean) {
        _isGameMode.value = enabled
        prefs.edit().putBoolean("is_game_mode", enabled).apply()
        if (enabled) {
            applyGameRecordingPreset()
        } else {
            syncRecorderConfig()
        }
    }

    fun applyGameRecordingPreset() {
        _isGameMode.value = true
        _defaultResolution.value = RecordingResolution.RES_1080P
        _defaultFps.value = FrameRate.FPS_60
        _audioSourceMode.value = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            AudioSourceMode.MIC_AND_INTERNAL
        } else {
            AudioSourceMode.MIC
        }
        _videoOrientation.value = VideoOrientation.AUTO
        prefs.edit()
            .putBoolean("is_game_mode", true)
            .putString("default_res", RecordingResolution.RES_1080P.name)
            .putString("default_fps", FrameRate.FPS_60.name)
            .putString("audio_source_mode", _audioSourceMode.value.name)
            .putString("video_orientation", VideoOrientation.AUTO.name)
            .apply()
        syncRecorderConfig()
    }

    private fun syncRecorderConfig() {
        val audioMode = _audioSourceMode.value
        val isGame = _isGameMode.value
        recorderEngine.updateConfig(
            RecordingConfig(
                resolution = _defaultResolution.value,
                frameRate = if (isGame) FrameRate.FPS_60 else _defaultFps.value,
                audioSource = audioMode,
                recordSystemAudio = audioMode == AudioSourceMode.INTERNAL || audioMode == AudioSourceMode.MIC_AND_INTERNAL,
                recordMicrophone = audioMode == AudioSourceMode.MIC || audioMode == AudioSourceMode.MIC_AND_INTERNAL,
                showTouches = _showTouches.value,
                floatingBubbleEnabled = _floatingBubbleEnabled.value,
                autoSaveToGallery = _autoSaveToGallery.value,
                orientation = _videoOrientation.value,
                countdownOption = _countdownOption.value,
                isGameMode = isGame,
                shakeToStop = _shakeToStop.value,
                showCursor = _cursorEnabled.value,
                clickEffects = _clickZoomEnabled.value,
                facecamEnabled = _facecamEnabled.value,
                facecamShape = _facecamShape.value,
                facecamSize = _facecamSize.value,
                facecamFrontLens = _facecamFrontLens.value,
                touchTrackingEnabled = _showTouches.value,
                touchRippleEnabled = _touchRippleEnabled.value,
                touchHighlightEnabled = _touchHighlightEnabled.value,
                touchMovementTrackingEnabled = _touchMovementTrackingEnabled.value,
                touchEffectSizeDp = _touchEffectSizeDp.value,
                touchEffectDurationMs = _touchEffectDurationMs.value,
                touchEffectOpacity = _touchEffectOpacity.value
            )
        )
    }

    // Phase 3: Real Video Editor State & Engine
    private val editorHistory = EditorHistoryManager()

    private val _editorState = MutableStateFlow(EditorProjectState.createDefault("", 0L))
    val editorState: StateFlow<EditorProjectState> = _editorState.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _timelinePositionMs = MutableStateFlow(0L)
    val timelinePositionMs: StateFlow<Long> = _timelinePositionMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _loadedTouchEvents = MutableStateFlow<List<FlowTouchEvent>>(emptyList())
    val loadedTouchEvents: StateFlow<List<FlowTouchEvent>> = _loadedTouchEvents.asStateFlow()

    private val _activeSegmentId = MutableStateFlow<String?>(null)
    val activeSegmentId: StateFlow<String?> = _activeSegmentId.asStateFlow()

    private val _activeToolTab = MutableStateFlow<String?>(null)
    val activeToolTab: StateFlow<String?> = _activeToolTab.asStateFlow()

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
    private val _facecamEnabled = MutableStateFlow(prefs.getBoolean("facecam_enabled", false))
    val facecamEnabled: StateFlow<Boolean> = _facecamEnabled.asStateFlow()

    private val _facecamShape = MutableStateFlow(prefs.getString("facecam_shape", "CIRCLE") ?: "CIRCLE") // CIRCLE, ROUNDED_RECT, RECT
    val facecamShape: StateFlow<String> = _facecamShape.asStateFlow()

    private val _facecamSize = MutableStateFlow(prefs.getString("facecam_size", "MEDIUM") ?: "MEDIUM") // SMALL, MEDIUM, LARGE
    val facecamSize: StateFlow<String> = _facecamSize.asStateFlow()

    private val _facecamFrontLens = MutableStateFlow(prefs.getBoolean("facecam_front_lens", true))
    val facecamFrontLens: StateFlow<Boolean> = _facecamFrontLens.asStateFlow()

    // Touch Feedback & Tracking Controls (Phase 2)
    private val _touchRippleEnabled = MutableStateFlow(prefs.getBoolean("touch_ripple", true))
    val touchRippleEnabled: StateFlow<Boolean> = _touchRippleEnabled.asStateFlow()

    private val _touchHighlightEnabled = MutableStateFlow(prefs.getBoolean("touch_highlight", true))
    val touchHighlightEnabled: StateFlow<Boolean> = _touchHighlightEnabled.asStateFlow()

    private val _touchMovementTrackingEnabled = MutableStateFlow(prefs.getBoolean("touch_movement", false))
    val touchMovementTrackingEnabled: StateFlow<Boolean> = _touchMovementTrackingEnabled.asStateFlow()

    private val _touchEffectSizeDp = MutableStateFlow(prefs.getInt("touch_size_dp", 36))
    val touchEffectSizeDp: StateFlow<Int> = _touchEffectSizeDp.asStateFlow()

    private val _touchEffectDurationMs = MutableStateFlow(prefs.getInt("touch_duration_ms", 500))
    val touchEffectDurationMs: StateFlow<Int> = _touchEffectDurationMs.asStateFlow()

    private val _touchEffectOpacity = MutableStateFlow(prefs.getFloat("touch_opacity", 0.8f))
    val touchEffectOpacity: StateFlow<Float> = _touchEffectOpacity.asStateFlow()

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

    init {
        syncRecorderConfig()
    }

    // Projects Grid / List View Mode
    private val _projectsViewMode = MutableStateFlow(
        try {
            ProjectViewMode.valueOf(prefs.getString("projects_view_mode", ProjectViewMode.LIST.name) ?: ProjectViewMode.LIST.name)
        } catch (_: Exception) {
            ProjectViewMode.LIST
        }
    )
    val projectsViewMode: StateFlow<ProjectViewMode> = _projectsViewMode.asStateFlow()

    fun toggleProjectsViewMode() {
        val next = if (_projectsViewMode.value == ProjectViewMode.LIST) ProjectViewMode.GRID else ProjectViewMode.LIST
        _projectsViewMode.value = next
        prefs.edit().putString("projects_view_mode", next.name).apply()
    }

    data class StorageInfo(
        val flowRecBytes: Long,
        val availableBytes: Long,
        val totalBytes: Long,
        val flowRecFormatted: String,
        val availableFormatted: String,
        val totalFormatted: String,
        val usedPercentage: Float
    )

    fun getStorageInfo(): StorageInfo {
        var flowRecBytes = 0L
        try {
            val recDir = File(getApplication<Application>().filesDir, "recordings")
            if (recDir.exists()) {
                flowRecBytes = recDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            }
        } catch (_: Exception) {}

        var availableBytes = 8L * 1024 * 1024 * 1024
        var totalBytes = 64L * 1024 * 1024 * 1024
        try {
            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            totalBytes = stat.blockCountLong * stat.blockSizeLong
        } catch (_: Exception) {}

        val usedPercentage = if (totalBytes > 0) {
            ((totalBytes - availableBytes).toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else 0.5f

        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f GB", bytes.toFloat() / (1024 * 1024 * 1024))
                bytes >= 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes.toFloat() / (1024 * 1024))
                else -> String.format(java.util.Locale.getDefault(), "%d KB", bytes / 1024)
            }
        }

        return StorageInfo(
            flowRecBytes = flowRecBytes,
            availableBytes = availableBytes,
            totalBytes = totalBytes,
            flowRecFormatted = formatBytes(flowRecBytes),
            availableFormatted = formatBytes(availableBytes),
            totalFormatted = formatBytes(totalBytes),
            usedPercentage = usedPercentage
        )
    }

    // Navigation Methods
    fun navigateTo(screen: Screen) {
        val currentStack = _navigationStack.value.toMutableList()
        val mappedTab = when (screen) {
            Screen.HOME -> Screen.HOME
            Screen.RECORD, Screen.NEW_RECORDING -> Screen.RECORD
            Screen.PROJECTS, Screen.LIBRARY -> Screen.PROJECTS
            Screen.EDITOR -> Screen.EDITOR
            Screen.SETTINGS -> Screen.SETTINGS
            else -> null
        }
        if (mappedTab != null) {
            _bottomNavTab.value = mappedTab
        }
        currentStack.add(screen)
        _navigationStack.value = currentStack
    }

    fun switchBottomTab(screen: Screen) {
        val mappedTab = when (screen) {
            Screen.HOME -> Screen.HOME
            Screen.RECORD, Screen.NEW_RECORDING -> Screen.RECORD
            Screen.PROJECTS, Screen.LIBRARY -> Screen.PROJECTS
            Screen.EDITOR -> Screen.EDITOR
            Screen.SETTINGS -> Screen.SETTINGS
            else -> screen
        }
        _bottomNavTab.value = mappedTab
        _navigationStack.value = listOf(mappedTab)
    }

    fun navigateBack() {
        val currentStack = _navigationStack.value.toMutableList()
        if (currentStack.size > 1) {
            currentStack.removeAt(currentStack.lastIndex)
            _navigationStack.value = currentStack
            val newTop = currentStack.last()
            val mappedTab = when (newTop) {
                Screen.HOME -> Screen.HOME
                Screen.RECORD, Screen.NEW_RECORDING -> Screen.RECORD
                Screen.PROJECTS, Screen.LIBRARY -> Screen.PROJECTS
                Screen.EDITOR -> Screen.EDITOR
                Screen.SETTINGS -> Screen.SETTINGS
                else -> null
            }
            if (mappedTab != null) {
                _bottomNavTab.value = mappedTab
            }
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
        _playheadSeconds.value = 0
        _timelinePositionMs.value = 0L
        _isPlaying.value = false
        loadProjectEditorData(project)
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
        val maxDuration = (_editorState.value.effectiveDurationMs / 1000L).toInt().coerceAtLeast(1)
        _playheadSeconds.value = seconds.coerceIn(0, maxDuration)
        _timelinePositionMs.value = (seconds * 1000L).coerceIn(0L, _editorState.value.effectiveDurationMs)
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun onPlayerTimelineUpdate(timelineMs: Long) {
        _timelinePositionMs.value = timelineMs
        _playheadSeconds.value = (timelineMs / 1000L).toInt()
    }

    fun seekTimeline(timelineMs: Long) {
        val totalMs = _editorState.value.effectiveDurationMs
        val clamped = timelineMs.coerceIn(0L, totalMs)
        _timelinePositionMs.value = clamped
        _playheadSeconds.value = (clamped / 1000L).toInt()
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun setActiveSegmentId(id: String?) {
        _activeSegmentId.value = id
    }

    fun setActiveToolTab(tab: String?) {
        _activeToolTab.value = tab
    }

    fun applyTrim(segmentId: String, newStartMs: Long, newEndMs: Long) {
        editorHistory.pushState(_editorState.value)
        _editorState.value = TimelineManager.trimSegment(_editorState.value, segmentId, newStartMs, newEndMs)
        updateUndoRedoFlags()
        saveCurrentProjectEditorData()
    }

    fun splitAtPlayhead() {
        editorHistory.pushState(_editorState.value)
        _editorState.value = TimelineManager.splitAtTimelineTime(_editorState.value, _timelinePositionMs.value)
        updateUndoRedoFlags()
        saveCurrentProjectEditorData()
    }

    fun deleteSegment(segmentId: String) {
        editorHistory.pushState(_editorState.value)
        _editorState.value = TimelineManager.deleteSegment(_editorState.value, segmentId)
        updateUndoRedoFlags()
        saveCurrentProjectEditorData()
    }

    fun undo() {
        val prev = editorHistory.undo(_editorState.value)
        if (prev != null) {
            _editorState.value = prev
            updateUndoRedoFlags()
            saveCurrentProjectEditorData()
        }
    }

    fun redo() {
        val next = editorHistory.redo(_editorState.value)
        if (next != null) {
            _editorState.value = next
            updateUndoRedoFlags()
            saveCurrentProjectEditorData()
        }
    }

    fun addTextOverlay(textOverlay: TextOverlay) {
        editorHistory.pushState(_editorState.value)
        val updated = _editorState.value.textOverlays + textOverlay
        _editorState.value = _editorState.value.copy(textOverlays = updated)
        updateUndoRedoFlags()
        saveCurrentProjectEditorData()
    }

    fun updateTextOverlay(textOverlay: TextOverlay) {
        editorHistory.pushState(_editorState.value)
        val updated = _editorState.value.textOverlays.map { if (it.id == textOverlay.id) textOverlay else it }
        _editorState.value = _editorState.value.copy(textOverlays = updated)
        updateUndoRedoFlags()
        saveCurrentProjectEditorData()
    }

    fun deleteTextOverlay(id: String) {
        editorHistory.pushState(_editorState.value)
        val updated = _editorState.value.textOverlays.filter { it.id != id }
        _editorState.value = _editorState.value.copy(textOverlays = updated)
        updateUndoRedoFlags()
        saveCurrentProjectEditorData()
    }

    fun addImageOverlay(imageOverlay: ImageOverlay) {
        editorHistory.pushState(_editorState.value)
        val updated = _editorState.value.imageOverlays + imageOverlay
        _editorState.value = _editorState.value.copy(imageOverlays = updated)
        updateUndoRedoFlags()
        saveCurrentProjectEditorData()
    }

    fun updateImageOverlay(imageOverlay: ImageOverlay) {
        editorHistory.pushState(_editorState.value)
        val updated = _editorState.value.imageOverlays.map { if (it.id == imageOverlay.id) imageOverlay else it }
        _editorState.value = _editorState.value.copy(imageOverlays = updated)
        updateUndoRedoFlags()
        saveCurrentProjectEditorData()
    }

    fun deleteImageOverlay(id: String) {
        editorHistory.pushState(_editorState.value)
        val updated = _editorState.value.imageOverlays.filter { it.id != id }
        _editorState.value = _editorState.value.copy(imageOverlays = updated)
        updateUndoRedoFlags()
        saveCurrentProjectEditorData()
    }

    fun addZoomKeyframe(zoomKeyframe: ZoomKeyframe) {
        editorHistory.pushState(_editorState.value)
        val updated = _editorState.value.zoomKeyframes + zoomKeyframe
        _editorState.value = _editorState.value.copy(zoomKeyframes = updated)
        updateUndoRedoFlags()
        saveCurrentProjectEditorData()
    }

    fun deleteZoomKeyframe(id: String) {
        editorHistory.pushState(_editorState.value)
        val updated = _editorState.value.zoomKeyframes.filter { it.id != id }
        _editorState.value = _editorState.value.copy(zoomKeyframes = updated)
        updateUndoRedoFlags()
        saveCurrentProjectEditorData()
    }

    fun updateFaceCamTrack(faceCamTrack: FaceCamEditorTrack) {
        editorHistory.pushState(_editorState.value)
        _editorState.value = _editorState.value.copy(faceCamTrack = faceCamTrack)
        updateUndoRedoFlags()
        saveCurrentProjectEditorData()
    }

    fun updateAudioConfig(audioConfig: AudioTrackConfig) {
        editorHistory.pushState(_editorState.value)
        _editorState.value = _editorState.value.copy(audioConfig = audioConfig)
        updateUndoRedoFlags()
        saveCurrentProjectEditorData()
    }

    fun updateTouchConfig(touchConfig: TouchOverlayConfig) {
        editorHistory.pushState(_editorState.value)
        _editorState.value = _editorState.value.copy(touchConfig = touchConfig)
        updateUndoRedoFlags()
        saveCurrentProjectEditorData()
    }

    private fun updateUndoRedoFlags() {
        _canUndo.value = editorHistory.canUndo
        _canRedo.value = editorHistory.canRedo
    }

    fun loadProjectEditorData(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val srcDurMs = (project.durationSeconds * 1000L).coerceAtLeast(1000L)
            var loadedState: EditorProjectState? = null

            // 1. Try reading from project.editorDataJson
            if (!project.editorDataJson.isNullOrBlank()) {
                loadedState = EditorProjectState.fromJson(project.editorDataJson)
            }

            // 2. Try reading from companion <video>.flowedit file
            if (loadedState == null && project.videoPath.isNotBlank()) {
                try {
                    val editFile = File(project.videoPath.substringBeforeLast(".") + ".flowedit")
                    if (editFile.exists() && editFile.length() > 0) {
                        loadedState = EditorProjectState.fromJson(editFile.readText())
                    }
                } catch (_: Exception) {}
            }

            // 3. Fallback to default
            val state = loadedState ?: EditorProjectState.createDefault(project.id, srcDurMs)

            // 4. Load .flowtouch companion events if present
            var touchEvents: List<FlowTouchEvent> = emptyList()
            if (project.videoPath.isNotBlank()) {
                val touchFile = if (project.touchMetadataPath != null && File(project.touchMetadataPath).exists()) {
                    File(project.touchMetadataPath)
                } else {
                    File(project.videoPath.substringBeforeLast(".") + ".flowtouch")
                }
                if (touchFile.exists()) {
                    val meta = TouchTracker.readMetadata(touchFile)
                    if (meta != null) {
                        touchEvents = meta.events
                    }
                }
            }

            // 5. Check .flowcam companion file
            var facecamTrack = state.faceCamTrack
            if (project.facecamEnabled && !facecamTrack.enabled && project.videoPath.isNotBlank()) {
                val camFile = if (project.facecamMetadataPath != null && File(project.facecamMetadataPath).exists()) {
                    File(project.facecamMetadataPath)
                } else {
                    File(project.videoPath.substringBeforeLast(".") + ".flowcam")
                }
                if (camFile.exists()) {
                    try {
                        val txt = camFile.readText()
                        val shape = txt.substringAfter("\"shape\":\"", "CIRCLE").substringBefore("\"")
                        val size = txt.substringAfter("\"size\":\"", "MEDIUM").substringBefore("\"")
                        val szPct = when (size.uppercase()) {
                            "SMALL" -> 0.18f
                            "LARGE" -> 0.32f
                            else -> 0.25f
                        }
                        facecamTrack = FaceCamEditorTrack(enabled = true, shape = shape, sizePercent = szPct)
                    } catch (_: Exception) {}
                } else {
                    facecamTrack = FaceCamEditorTrack(enabled = true)
                }
            }

            val finalState = state.copy(faceCamTrack = facecamTrack)

            viewModelScope.launch(Dispatchers.Main) {
                _editorState.value = finalState
                _loadedTouchEvents.value = touchEvents
                _activeSegmentId.value = finalState.segments.firstOrNull()?.id
                editorHistory.clear()
                updateUndoRedoFlags()
                _timelinePositionMs.value = 0L
                _playheadSeconds.value = 0
            }
        }
    }

    fun saveCurrentProjectEditorData() {
        val proj = _selectedProject.value ?: return
        val currentState = _editorState.value
        viewModelScope.launch(Dispatchers.IO) {
            val json = currentState.toJson()

            // Save to companion .flowedit file
            if (proj.videoPath.isNotBlank()) {
                try {
                    val editFile = File(proj.videoPath.substringBeforeLast(".") + ".flowedit")
                    editFile.writeText(json)
                } catch (e: Exception) {
                    Log.w("FlowRecViewModel", "Error saving .flowedit companion file: ${e.message}")
                }
            }

            // Save into Room Database
            val updated = proj.copy(
                editorDataJson = json,
                durationSeconds = (currentState.effectiveDurationMs / 1000L).toInt().coerceAtLeast(1)
            )
            dao.updateProject(updated)
            _selectedProject.value = updated
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

    fun setTouchRippleEnabled(enabled: Boolean) {
        _touchRippleEnabled.value = enabled
        prefs.edit().putBoolean("touch_ripple", enabled).apply()
        syncRecorderConfig()
    }

    fun setTouchHighlightEnabled(enabled: Boolean) {
        _touchHighlightEnabled.value = enabled
        prefs.edit().putBoolean("touch_highlight", enabled).apply()
        syncRecorderConfig()
    }

    fun setTouchMovementTrackingEnabled(enabled: Boolean) {
        _touchMovementTrackingEnabled.value = enabled
        prefs.edit().putBoolean("touch_movement", enabled).apply()
        syncRecorderConfig()
    }

    fun setTouchEffectSizeDp(sizeDp: Int) {
        _touchEffectSizeDp.value = sizeDp
        prefs.edit().putInt("touch_size_dp", sizeDp).apply()
        syncRecorderConfig()
    }

    fun setTouchEffectDurationMs(durationMs: Int) {
        _touchEffectDurationMs.value = durationMs
        prefs.edit().putInt("touch_duration_ms", durationMs).apply()
        syncRecorderConfig()
    }

    fun setTouchEffectOpacity(opacity: Float) {
        _touchEffectOpacity.value = opacity
        prefs.edit().putFloat("touch_opacity", opacity).apply()
        syncRecorderConfig()
    }

    fun getTouchEffectConfig(): TouchEffectConfig {
        return TouchEffectConfig(
            enabled = _showTouches.value,
            rippleEnabled = _touchRippleEnabled.value,
            highlightEnabled = _touchHighlightEnabled.value,
            movementTrackingEnabled = _touchMovementTrackingEnabled.value,
            sizeDp = _touchEffectSizeDp.value,
            durationMs = _touchEffectDurationMs.value,
            opacity = _touchEffectOpacity.value,
            colorHex = _touchFeedbackColor.value
        )
    }

    fun setFacecamEnabled(enabled: Boolean) {
        _facecamEnabled.value = enabled
        prefs.edit().putBoolean("facecam_enabled", enabled).apply()
        syncRecorderConfig()
    }

    fun setFacecamShape(shape: String) {
        _facecamShape.value = shape
        prefs.edit().putString("facecam_shape", shape).apply()
        syncRecorderConfig()
    }

    fun setFacecamSize(size: String) {
        _facecamSize.value = size
        prefs.edit().putString("facecam_size", size).apply()
        syncRecorderConfig()
    }

    fun setFacecamFrontLens(isFront: Boolean) {
        _facecamFrontLens.value = isFront
        prefs.edit().putBoolean("facecam_front_lens", isFront).apply()
        syncRecorderConfig()
    }

    fun resetFacecamPosition() {
        try {
            com.example.recorder.camera.FloatingFacecamManager.getInstance(getApplication()).resetPosition()
        } catch (_: Exception) {}
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
            val duplicateId = UUID.randomUUID().toString()
            var dupVideoPath = project.videoPath
            var dupTouchPath = project.touchMetadataPath
            var dupCamPath = project.facecamMetadataPath

            try {
                if (project.videoPath.isNotBlank()) {
                    val origFile = File(project.videoPath)
                    if (origFile.exists()) {
                        val dupFile = File(origFile.parentFile, "rec_${duplicateId}.mp4")
                        origFile.copyTo(dupFile, overwrite = true)
                        dupVideoPath = dupFile.absolutePath

                        val origTouch = File(project.videoPath.substringBeforeLast(".") + ".flowtouch")
                        if (origTouch.exists()) {
                            val dupTouch = File(origFile.parentFile, "rec_${duplicateId}.flowtouch")
                            origTouch.copyTo(dupTouch, overwrite = true)
                            dupTouchPath = dupTouch.absolutePath
                        }

                        val origCam = File(project.videoPath.substringBeforeLast(".") + ".flowcam")
                        if (origCam.exists()) {
                            val dupCam = File(origFile.parentFile, "rec_${duplicateId}.flowcam")
                            origCam.copyTo(dupCam, overwrite = true)
                            dupCamPath = dupCam.absolutePath
                        }

                        val origEdit = File(project.videoPath.substringBeforeLast(".") + ".flowedit")
                        if (origEdit.exists()) {
                            val dupEdit = File(origFile.parentFile, "rec_${duplicateId}.flowedit")
                            origEdit.copyTo(dupEdit, overwrite = true)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("FlowRecViewModel", "Error duplicating files: ${e.message}")
            }

            val duplicate = project.copy(
                id = duplicateId,
                name = "${project.name} (Copy)",
                videoPath = dupVideoPath,
                touchMetadataPath = dupTouchPath,
                facecamMetadataPath = dupCamPath,
                editorDataJson = project.editorDataJson,
                createdAt = System.currentTimeMillis()
            )
            dao.insertProject(duplicate)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (project.videoPath.isNotBlank()) {
                    val vidFile = File(project.videoPath)
                    if (vidFile.exists()) vidFile.delete()

                    val touchFile = File(project.videoPath.substringBeforeLast(".") + ".flowtouch")
                    if (touchFile.exists()) touchFile.delete()

                    val camFile = File(project.videoPath.substringBeforeLast(".") + ".flowcam")
                    if (camFile.exists()) camFile.delete()

                    val editFile = File(project.videoPath.substringBeforeLast(".") + ".flowedit")
                    if (editFile.exists()) editFile.delete()
                }
                if (project.touchMetadataPath != null) {
                    val tf = File(project.touchMetadataPath)
                    if (tf.exists()) tf.delete()
                }
                if (project.thumbnailResName.startsWith("/")) {
                    val thumbFile = File(project.thumbnailResName)
                    if (thumbFile.exists()) thumbFile.delete()
                }
            } catch (e: Exception) {
                Log.w("FlowRecViewModel", "Error deleting companion files: ${e.message}")
            }
            dao.deleteProject(project)
            if (_selectedProject.value?.id == project.id) {
                _selectedProject.value = null
            }
        }
        if (_selectedProject.value?.id == project.id) {
            navigateBack()
        }
    }

    fun openProjectById(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val proj = dao.getProjectById(id)
            if (proj != null) {
                viewModelScope.launch(Dispatchers.Main) {
                    openEditorForProject(proj)
                }
            }
        }
    }

    fun stopRecording() {
        recorderEngine.stopRecording()
    }

    private suspend fun scanAndRecoverOrphanedRecordings() {
        try {
            val recordingsDir = File(getApplication<Application>().filesDir, "recordings")
            if (!recordingsDir.exists()) return
            val files = recordingsDir.listFiles { f -> f.extension == "mp4" && f.length() > 0 } ?: return
            val existingProjects = dao.getAllProjects()
            val existingPaths = existingProjects.map { it.videoPath }.toSet()

            for (file in files) {
                if (!existingPaths.contains(file.absolutePath)) {
                    var durationSec = 1
                    var thumbPath = "thumb_mountain"
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(file.absolutePath)
                        val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        if (durMs > 500) {
                            durationSec = (durMs / 1000L).toInt().coerceAtLeast(1)
                        }
                        val frame = retriever.getFrameAtTime(500_000) ?: retriever.getFrameAtTime(0)
                        if (frame != null) {
                            val thumbsDir = File(getApplication<Application>().filesDir, "thumbnails")
                            thumbsDir.mkdirs()
                            val thumbFile = File(thumbsDir, "thumb_${file.nameWithoutExtension}.png")
                            val fos = FileOutputStream(thumbFile)
                            frame.compress(Bitmap.CompressFormat.PNG, 90, fos)
                            fos.flush()
                            fos.close()
                            thumbPath = thumbFile.absolutePath
                        }
                        retriever.release()
                    } catch (e: Exception) {
                        Log.w("FlowRecViewModel", "Recover metadata error: ${e.message}")
                    }

                    val recoveredProject = ProjectEntity(
                        id = UUID.randomUUID().toString(),
                        name = "Screen Recording",
                        durationSeconds = durationSec,
                        resolution = "1080p",
                        fps = 30,
                        fileSizeBytes = file.length(),
                        thumbnailResName = thumbPath,
                        videoPath = file.absolutePath,
                        createdAt = file.lastModified(),
                        isFavorite = false,
                        isExported = false,
                        cursorEnabled = true,
                        clickZoomEnabled = true
                    )
                    dao.insertProject(recoveredProject)
                    Log.i("FlowRecViewModel", "Recovered orphaned recording: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.w("FlowRecViewModel", "Error recovering recordings: ${e.message}")
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

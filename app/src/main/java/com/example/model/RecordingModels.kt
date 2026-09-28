package com.example.model

enum class CaptureMode(val label: String, val description: String) {
    SCREEN("Screen", "Record full device screen"),
    WINDOW("Window", "Record specific application"),
    TAB("Tab", "Record browser tab or view")
}

enum class RecordingResolution(val label: String, val width: Int, val height: Int) {
    RES_720P("720p", 1280, 720),
    RES_1080P("1080p", 1920, 1080),
    RES_NATIVE("Device Native", 0, 0)
}

enum class FrameRate(val label: String, val fps: Int) {
    FPS_24("24 FPS", 24),
    FPS_30("30 FPS", 30),
    FPS_60("60 FPS", 60)
}

enum class CursorStyle(val label: String) {
    DEFAULT("Pointer"),
    TARGET("Target"),
    MAGNIFIER("Magnifier"),
    RING("Halo Ring")
}

enum class ZoomEasing(val label: String) {
    SMOOTH("Smooth"),
    LINEAR("Linear"),
    EASE_IN("Ease In"),
    SPRING("Spring")
}

enum class ExportPreset(val label: String, val resLabel: String, val fps: Int) {
    YOUTUBE_1080P("YouTube (1080p - 30fps)", "1080p", 30),
    INSTAGRAM_REEL("Instagram Reel (9:16)", "1080x1920", 30),
    TWITTER_HD("Twitter / X HD (720p)", "720p", 30),
    CUSTOM_4K("Custom 4K (2160p - 60fps)", "4K", 60)
}

enum class ExportFormat(val label: String, val extension: String) {
    MP4("MP4", "mp4"),
    GIF("Animated GIF", "gif"),
    WEBM("ProRes WebM", "webm")
}

enum class ExportQuality(val label: String, val bitrate: String) {
    BALANCED("Balanced", "4 Mbps"),
    HIGH("High (Recommended)", "8 Mbps"),
    MAXIMUM("Maximum Quality", "16 Mbps")
}

enum class AppThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class AudioSourceMode(val label: String) {
    NONE("No Sound (Mute)"),
    SYSTEM("Media Sounds Only"),
    MIC_AND_SYSTEM("Media Sounds and Mic")
}

enum class VideoOrientation(val label: String) {
    AUTO("Auto Detect"),
    PORTRAIT("Portrait (9:16)"),
    LANDSCAPE("Landscape (16:9)")
}

enum class CountdownOption(val label: String, val seconds: Int) {
    OFF("Off (Instant)", 0),
    SEC_3("3 Seconds", 3),
    SEC_5("5 Seconds", 5)
}

enum class RecorderState {
    IDLE,
    PREPARING,
    COUNTDOWN,
    RECORDING,
    PAUSED,
    STOPPING,
    PROCESSING,
    COMPLETED,
    ERROR
}

enum class ProjectSortOption(val label: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    NAME_AZ("Name (A to Z)"),
    NAME_ZA("Name (Z to A)"),
    DURATION_DESC("Longest Duration"),
    DURATION_ASC("Shortest Duration")
}

enum class ProjectFilterOption(val label: String) {
    ALL("All Files"),
    FAVORITES("Favorites Only"),
    EXPORTED("Exported Videos"),
    RAW("Raw Recordings")
}


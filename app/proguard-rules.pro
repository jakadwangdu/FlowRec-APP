# FlowRec Android Production ProGuard & R8 Optimization Rules
# Phase 7: Production Release Hardening

# Preserve source file names and line numbers for crash debugging
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# -----------------------------------------------------------------------------
# FlowRec Domain Models, Companion File Schemas & Serialization
# Protects .flowedit, .flowtouch, .flowcam, .flowai, and database entities
# -----------------------------------------------------------------------------
-keep class com.example.data.entity.** { *; }
-keep class com.example.data.database.** { *; }
-keep class com.example.data.dao.** { *; }
-keep class com.example.editor.model.** { *; }
-keep class com.example.editor.export.** { *; }
-keep class com.example.recorder.touch.** { *; }
-keep class com.example.ai.model.** { *; }
-keep class com.example.model.** { *; }

# Moshi / JSON reflection
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.JsonClass <fields>;
}
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# -----------------------------------------------------------------------------
# Android Architecture Components (Lifecycle & Room)
# -----------------------------------------------------------------------------
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-dontwarn androidx.room.paging.**

-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# -----------------------------------------------------------------------------
# AndroidX Media3 / ExoPlayer & Codec Pipelines
# -----------------------------------------------------------------------------
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# -----------------------------------------------------------------------------
# CameraX Pipelines (FaceCam)
# -----------------------------------------------------------------------------
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# -----------------------------------------------------------------------------
# Foreground Services, Quick Settings Tile & Providers
# -----------------------------------------------------------------------------
-keep class com.example.service.** { *; }
-keep class com.example.MainActivity { *; }

# -----------------------------------------------------------------------------
# Image Loading (Coil)
# -----------------------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**

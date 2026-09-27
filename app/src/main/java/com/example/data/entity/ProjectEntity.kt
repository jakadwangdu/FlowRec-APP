package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val durationSeconds: Int,
    val resolution: String = "1080p",
    val fps: Int = 30,
    val fileSizeBytes: Long = 125_000_000L,
    val thumbnailResName: String = "thumb_mountain",
    val videoPath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isExported: Boolean = false,
    val format: String = "MP4",
    // Editor Effects state
    val cursorEnabled: Boolean = true,
    val cursorStyle: String = "DEFAULT",
    val cursorSizePercent: Int = 120,
    val clickZoomEnabled: Boolean = true,
    val zoomLevel: Float = 1.6f,
    val zoomDurationMs: Int = 350,
    val zoomEasing: String = "SMOOTH",
    val motionBlurEnabled: Boolean = true,
    val blurAmountPercent: Int = 18,
    val transition: String = "Smooth"
)

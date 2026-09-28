package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ProjectDao
import com.example.data.entity.ProjectEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(entities = [ProjectEntity::class], version = 2, exportSchema = false)
abstract class FlowRecDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: FlowRecDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): FlowRecDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlowRecDatabase::class.java,
                    "flowrec_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun prepopulateProjects(dao: ProjectDao) {
            val now = System.currentTimeMillis()
            val day = 86400000L
            val initialProjects = listOf(
                ProjectEntity(
                    id = "demo_product_demo",
                    name = "Product Demo",
                    durationSeconds = 272, // 4:32
                    resolution = "1080p",
                    fps = 30,
                    fileSizeBytes = 129 * 1024 * 1024L,
                    thumbnailResName = "thumb_mountain",
                    createdAt = now - (1 * day),
                    isFavorite = true,
                    isExported = true,
                    format = "MP4",
                    cursorEnabled = true,
                    cursorStyle = "DEFAULT",
                    cursorSizePercent = 120,
                    clickZoomEnabled = true,
                    zoomLevel = 1.6f,
                    zoomDurationMs = 350,
                    zoomEasing = "SMOOTH",
                    motionBlurEnabled = true,
                    blurAmountPercent = 18,
                    transition = "Smooth"
                ),
                ProjectEntity(
                    id = "demo_code_walkthrough",
                    name = "Code Walkthrough",
                    durationSeconds = 372, // 6:12
                    resolution = "1080p",
                    fps = 30,
                    fileSizeBytes = 184 * 1024 * 1024L,
                    thumbnailResName = "thumb_code",
                    createdAt = now - (3 * day),
                    isFavorite = false,
                    isExported = true,
                    format = "MP4",
                    cursorEnabled = true,
                    cursorStyle = "TARGET",
                    cursorSizePercent = 100,
                    clickZoomEnabled = true,
                    zoomLevel = 2.0f,
                    zoomDurationMs = 400,
                    zoomEasing = "SPRING",
                    motionBlurEnabled = false,
                    blurAmountPercent = 0,
                    transition = "Instant"
                ),
                ProjectEntity(
                    id = "demo_app_ui_showcase",
                    name = "App UI Showcase",
                    durationSeconds = 225, // 3:45
                    resolution = "1080p",
                    fps = 60,
                    fileSizeBytes = 112 * 1024 * 1024L,
                    thumbnailResName = "thumb_appui",
                    createdAt = now - (5 * day),
                    isFavorite = true,
                    isExported = false,
                    format = "MP4",
                    cursorEnabled = true,
                    cursorStyle = "MAGNIFIER",
                    cursorSizePercent = 140,
                    clickZoomEnabled = true,
                    zoomLevel = 1.8f,
                    zoomDurationMs = 300,
                    zoomEasing = "SMOOTH",
                    motionBlurEnabled = true,
                    blurAmountPercent = 25,
                    transition = "Smooth"
                ),
                ProjectEntity(
                    id = "demo_landing_page",
                    name = "Landing Page",
                    durationSeconds = 138, // 2:18
                    resolution = "1080p",
                    fps = 30,
                    fileSizeBytes = 76 * 1024 * 1024L,
                    thumbnailResName = "thumb_mountain",
                    createdAt = now - (7 * day),
                    isFavorite = false,
                    isExported = true,
                    format = "MP4"
                ),
                ProjectEntity(
                    id = "demo_tutorial",
                    name = "Tutorial",
                    durationSeconds = 260, // 4:20
                    resolution = "1080p",
                    fps = 30,
                    fileSizeBytes = 145 * 1024 * 1024L,
                    thumbnailResName = "thumb_code",
                    createdAt = now - (9 * day),
                    isFavorite = false,
                    isExported = false,
                    format = "MP4"
                )
            )
            dao.insertProjects(initialProjects)
        }
    }
}

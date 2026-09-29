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

@Database(entities = [ProjectEntity::class], version = 4, exportSchema = false)
abstract class FlowRecDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: FlowRecDatabase? = null

        fun getDatabase(context: Context): FlowRecDatabase {
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

        fun getDatabase(context: Context, scope: CoroutineScope): FlowRecDatabase {
            return getDatabase(context)
        }

        suspend fun prepopulateProjects(dao: ProjectDao) {
            // No dummy projects prepopulated. Keep only user's real screen recordings.
        }
    }
}


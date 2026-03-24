package org.example.desktop_app.data.datasource.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [VideoHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
package org.example.desktop_app.data.datasource.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_history")
data class VideoHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val url: String,
    val savedPath: String?,
    val duration: String?,
    val size: String,
    val status: String, // Stored as a raw String in the DB
    val progress: Int = 0,
    val errorMessage: String? = null,
    val thumbnailPath: String? = null,
    val downloadDateMillis: Long
)
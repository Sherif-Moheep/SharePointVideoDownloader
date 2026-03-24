package org.example.desktop_app.data.datasource.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM video_history ORDER BY downloadDateMillis DESC")
    fun getAllHistory(): Flow<List<VideoHistoryEntity>>

    @Upsert
    suspend fun upsertVideo(video: VideoHistoryEntity)

    @Query("SELECT * FROM video_history WHERE id = :id")
    suspend fun getVideoById(id: Long): VideoHistoryEntity?
}
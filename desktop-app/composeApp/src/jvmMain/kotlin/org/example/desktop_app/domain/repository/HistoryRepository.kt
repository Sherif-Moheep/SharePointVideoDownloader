package org.example.desktop_app.domain.repository

import kotlinx.coroutines.flow.Flow
import org.example.desktop_app.domain.models.VideoHistory

interface HistoryRepository {
    fun getDownloadHistory(): Flow<List<VideoHistory>>

    suspend fun upsertVideo(video: VideoHistory)

    suspend fun getVideoById(id: Long): VideoHistory?
}
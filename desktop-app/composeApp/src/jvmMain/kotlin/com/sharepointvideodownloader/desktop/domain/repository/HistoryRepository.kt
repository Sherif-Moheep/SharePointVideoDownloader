package com.sharepointvideodownloader.desktop.domain.repository

import kotlinx.coroutines.flow.Flow
import com.sharepointvideodownloader.desktop.domain.models.VideoHistory

interface HistoryRepository {
    fun getDownloadHistory(): Flow<List<VideoHistory>>

    suspend fun upsertVideo(video: VideoHistory)

    suspend fun getVideoById(id: Long): VideoHistory?
}
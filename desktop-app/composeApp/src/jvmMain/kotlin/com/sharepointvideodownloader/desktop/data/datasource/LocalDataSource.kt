package com.sharepointvideodownloader.desktop.data.datasource

import kotlinx.coroutines.flow.Flow
import com.sharepointvideodownloader.desktop.data.datasource.room.HistoryDao
import com.sharepointvideodownloader.desktop.data.datasource.room.VideoHistoryEntity

class LocalDataSource(
    private val historyDao: HistoryDao
) {
    fun getAllHistory(): Flow<List<VideoHistoryEntity>> {
        return historyDao.getAllHistory()
    }

    suspend fun upsertVideo(video: VideoHistoryEntity) {
        historyDao.upsertVideo(video = video)
    }

    suspend fun getVideoById(id: Long): VideoHistoryEntity? {
        return historyDao.getVideoById(id)
    }
}
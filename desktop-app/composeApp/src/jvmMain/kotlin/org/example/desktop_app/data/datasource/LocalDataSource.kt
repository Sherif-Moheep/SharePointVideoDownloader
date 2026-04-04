package org.example.desktop_app.data.datasource

import kotlinx.coroutines.flow.Flow
import org.example.desktop_app.data.datasource.room.HistoryDao
import org.example.desktop_app.data.datasource.room.VideoHistoryEntity

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
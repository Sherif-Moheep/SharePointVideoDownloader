package org.example.desktop_app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.desktop_app.data.datasource.LocalDataSource
import org.example.desktop_app.data.datasource.room.VideoHistoryEntity
import org.example.desktop_app.data.mappers.toDomain
import org.example.desktop_app.domain.models.VideoHistory
import org.example.desktop_app.domain.repository.HistoryRepository

class HistoryRepositoryImpl(
    private val localDataSource: LocalDataSource
): HistoryRepository {

    override fun getDownloadHistory(): Flow<List<VideoHistory>> {
        return localDataSource.getAllHistory().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun upsertVideo(video: VideoHistory) {
        val entity = VideoHistoryEntity(
            id = video.id,
            name = video.name,
            url = video.url,
            savedPath = video.savedPath,
            duration = video.duration,
            size = video.size,
            status = video.status.name,
            progress = video.progress,
            errorMessage = video.errorMessage,
            thumbnailPath = video.thumbnailPath,
            downloadDateMillis = video.downloadDate
        )

        localDataSource.upsertVideo(entity)
    }

    override suspend fun getVideoById(id: Long): VideoHistory? {
        return localDataSource.getVideoById(id)?.toDomain()
    }
}
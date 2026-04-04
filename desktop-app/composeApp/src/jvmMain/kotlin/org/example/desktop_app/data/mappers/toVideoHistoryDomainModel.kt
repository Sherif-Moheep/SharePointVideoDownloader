package org.example.desktop_app.data.mappers

import org.example.desktop_app.data.datasource.room.VideoHistoryEntity
import org.example.desktop_app.domain.models.DownloadStatus
import org.example.desktop_app.domain.models.VideoHistory

fun VideoHistoryEntity.toDomain(): VideoHistory {
    return VideoHistory(
        id = this.id,
        name = this.name,
        url = this.url,
        savedPath = this.savedPath,
        duration = this.duration,
        size = this.size,
        status = this.status.toDownloadStatus(),
        progress = this.progress,
        errorMessage = this.errorMessage,
        thumbnailPath = this.thumbnailPath,
        downloadDate = this.downloadDateMillis
    )
}

// Helper to safely parse the status string
private fun String.toDownloadStatus(): DownloadStatus {
    return try {
        DownloadStatus.valueOf(this.uppercase())
    } catch (_: IllegalArgumentException) {
        DownloadStatus.UNKNOWN
    }
}
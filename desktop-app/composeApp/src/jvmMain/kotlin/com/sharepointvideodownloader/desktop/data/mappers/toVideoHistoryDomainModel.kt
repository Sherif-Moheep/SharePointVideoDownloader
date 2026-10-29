package com.sharepointvideodownloader.desktop.data.mappers

import com.sharepointvideodownloader.desktop.data.datasource.room.VideoHistoryEntity
import com.sharepointvideodownloader.desktop.domain.models.DownloadStatus
import com.sharepointvideodownloader.desktop.domain.models.VideoHistory

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
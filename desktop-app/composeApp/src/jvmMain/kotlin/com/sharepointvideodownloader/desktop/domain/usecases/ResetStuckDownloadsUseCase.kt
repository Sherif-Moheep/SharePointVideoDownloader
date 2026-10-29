package com.sharepointvideodownloader.desktop.domain.usecases

import kotlinx.coroutines.flow.first
import com.sharepointvideodownloader.desktop.domain.models.DownloadStatus
import com.sharepointvideodownloader.desktop.domain.repository.HistoryRepository

class ResetStuckDownloadsUseCase(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke() {
        val currentHistory = historyRepository.getDownloadHistory().first()

        val stuckVideos = currentHistory.filter { it.status == DownloadStatus.DOWNLOADING }

        stuckVideos.forEach { video ->
            historyRepository.upsertVideo(video.copy(status = DownloadStatus.CANCELLED))
        }
    }
}
package org.example.desktop_app.domain.usecases

import kotlinx.coroutines.flow.first
import org.example.desktop_app.domain.models.DownloadStatus
import org.example.desktop_app.domain.repository.HistoryRepository

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
package org.example.desktop_app.domain.usecases

import kotlinx.coroutines.flow.first
import org.example.desktop_app.domain.models.DownloadStatus
import org.example.desktop_app.domain.repository.HistoryRepository

class ResetStuckDownloadsUseCase(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke() {
        // Grab the current history list
        val currentHistory = historyRepository.getDownloadHistory().first()

        // Find and cancel stuck videos
        val stuckVideos = currentHistory.filter { it.status == DownloadStatus.DOWNLOADING }

        // Flip them all to CANCELLED
        stuckVideos.forEach { video ->
            historyRepository.upsertVideo(video.copy(status = DownloadStatus.CANCELLED))
        }
    }
}
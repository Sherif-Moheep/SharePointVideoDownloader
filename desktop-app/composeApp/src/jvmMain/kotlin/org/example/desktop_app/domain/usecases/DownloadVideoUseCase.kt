package org.example.desktop_app.domain.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.example.desktop_app.domain.models.DownloadStatus
import org.example.desktop_app.domain.repository.HistoryRepository
import org.example.desktop_app.domain.repository.VideoDownloadRepository

class DownloadVideoUseCase(
    private val historyRepository: HistoryRepository,
    private val videoDownloadRepository: VideoDownloadRepository
) {
    suspend operator fun invoke(videoId: Long) = coroutineScope {
        val video = historyRepository.getVideoById(videoId) ?: return@coroutineScope

        // 1. Mark as downloading
        val downloadingVideo = video.copy(status = DownloadStatus.DOWNLOADING)
        historyRepository.upsertVideo(downloadingVideo)

        try {
            // 2. Start download
            val finalPath = videoDownloadRepository.downloadVideo(
                videoName = video.name,
                url = video.url,
                onProgress = { currentProgress ->
                    // 3. Fire progress updates concurrently
                    launch(Dispatchers.IO) {
                        historyRepository.upsertVideo(
                            downloadingVideo.copy(progress = currentProgress)
                        )
                    }
                }
            )

            // 4. Mark as completed
            historyRepository.upsertVideo(
                downloadingVideo.copy(
                    status = DownloadStatus.COMPLETED,
                    progress = 100,
                    savedPath = finalPath
                )
            )
        } catch (e: Exception) {
            // 5. Handle failure
            historyRepository.upsertVideo(
                downloadingVideo.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.message
                )
            )
        }
    }
}
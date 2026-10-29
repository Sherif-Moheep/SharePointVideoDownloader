package com.sharepointvideodownloader.desktop.domain.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.sharepointvideodownloader.desktop.domain.models.DownloadStatus
import com.sharepointvideodownloader.desktop.domain.repository.HistoryRepository
import com.sharepointvideodownloader.desktop.domain.repository.VideoDownloadRepository

class DownloadVideoUseCase(
    private val historyRepository: HistoryRepository,
    private val videoDownloadRepository: VideoDownloadRepository
) {
    suspend operator fun invoke(videoId: Long) = coroutineScope {
        val video = historyRepository.getVideoById(videoId) ?: return@coroutineScope

        val downloadingVideo = video.copy(status = DownloadStatus.DOWNLOADING)
        historyRepository.upsertVideo(downloadingVideo)

        try {
            val finalPath = videoDownloadRepository.downloadVideo(
                videoName = video.name,
                url = video.url,
                onProgress = { currentProgress ->
                    launch(Dispatchers.IO) {
                        historyRepository.upsertVideo(
                            downloadingVideo.copy(progress = currentProgress)
                        )
                    }
                }
            )

            // Extract the actual filename from the final path
            val finalFileName = finalPath.substringAfterLast('\\').substringAfterLast('/')

            historyRepository.upsertVideo(
                downloadingVideo.copy(
                    name = finalFileName,
                    status = DownloadStatus.COMPLETED,
                    progress = 100,
                    savedPath = finalPath
                )
            )
        } catch (e: Exception) {
            historyRepository.upsertVideo(
                downloadingVideo.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.message
                )
            )
        }
    }
}
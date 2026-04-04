package org.example.desktop_app.domain.usecases

import org.example.desktop_app.domain.models.VideoHistory
import org.example.desktop_app.domain.repository.ChromeRepository
import org.example.desktop_app.domain.repository.HistoryRepository

class ProcessNewVideoJsonUseCase(
    private val chromeRepository: ChromeRepository,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(rawJson: String): Result<Unit> {
        if (rawJson.isBlank()) return Result.failure(Exception("Empty JSON"))

        return chromeRepository.processVideoData(rawJson).map { videoMessage ->
            val newVideo = VideoHistory(
                id = 0L,
                name = videoMessage.title,
                url = videoMessage.url,
                size = videoMessage.size,
                downloadDate = System.currentTimeMillis()
            )
            historyRepository.upsertVideo(newVideo)
        }
    }
}
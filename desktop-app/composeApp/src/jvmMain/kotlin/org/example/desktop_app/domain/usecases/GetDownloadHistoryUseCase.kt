package org.example.desktop_app.domain.usecases

import kotlinx.coroutines.flow.Flow
import org.example.desktop_app.domain.models.VideoHistory
import org.example.desktop_app.domain.repository.HistoryRepository

class GetDownloadHistoryUseCase (
    private val historyRepository: HistoryRepository
) {
    operator fun invoke() : Flow<List<VideoHistory>> {
        return historyRepository.getDownloadHistory()
    }
}
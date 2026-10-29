package com.sharepointvideodownloader.desktop.domain.usecases

import kotlinx.coroutines.flow.Flow
import com.sharepointvideodownloader.desktop.domain.models.VideoHistory
import com.sharepointvideodownloader.desktop.domain.repository.HistoryRepository

class GetDownloadHistoryUseCase (
    private val historyRepository: HistoryRepository
) {
    operator fun invoke() : Flow<List<VideoHistory>> {
        return historyRepository.getDownloadHistory()
    }
}
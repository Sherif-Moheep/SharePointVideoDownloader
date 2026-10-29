package com.sharepointvideodownloader.desktop.domain.repository

import com.sharepointvideodownloader.desktop.domain.models.VideoMessage

interface ChromeRepository {
    suspend fun processVideoData(rawJson: String): Result<VideoMessage>
}
package org.example.desktop_app.domain.repository

import org.example.desktop_app.domain.models.VideoMessage

interface ChromeRepository {
    suspend fun processVideoData(rawJson: String): Result<VideoMessage>
}
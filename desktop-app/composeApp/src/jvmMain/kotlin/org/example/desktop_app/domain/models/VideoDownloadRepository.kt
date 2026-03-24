package org.example.desktop_app.domain.models

interface VideoDownloadRepository {
    suspend fun downloadVideo(
        videoName: String,
        url: String,
        onProgress: (Int) -> Unit
    ): String
}
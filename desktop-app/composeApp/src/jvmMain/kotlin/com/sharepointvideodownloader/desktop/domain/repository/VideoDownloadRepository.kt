package com.sharepointvideodownloader.desktop.domain.repository

interface VideoDownloadRepository {
    suspend fun downloadVideo(
        videoName: String,
        url: String,
        onProgress: (Int) -> Unit
    ): String
}
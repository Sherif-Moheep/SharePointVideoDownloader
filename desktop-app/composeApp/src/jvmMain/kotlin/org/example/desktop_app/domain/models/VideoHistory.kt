package org.example.desktop_app.domain.models

data class VideoHistory(
    val id: Long = 0L,
    val name: String,
    val url: String,
    val savedPath: String? = null,
    val duration: String? = null,
    val size: String,
    val status: DownloadStatus = DownloadStatus.READY,
    val progress: Int = 0,
    val errorMessage: String? = null,
    val thumbnailPath: String? = null,
    val downloadDate: Long
)
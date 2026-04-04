package org.example.desktop_app.presentation.models

import org.example.desktop_app.domain.models.DownloadStatus

data class UiVideoHistory(
    val id: Long,
    val name: String,
    val savedPath: String? = null,
    val size: String = "",
    val status: DownloadStatus,
    val progress: Int = 0,
    val downloadDate: String = ""
)
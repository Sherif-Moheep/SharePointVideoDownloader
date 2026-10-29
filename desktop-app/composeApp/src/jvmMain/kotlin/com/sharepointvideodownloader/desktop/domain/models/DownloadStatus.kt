package com.sharepointvideodownloader.desktop.domain.models

enum class DownloadStatus {
    READY,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED,
    UNKNOWN
}
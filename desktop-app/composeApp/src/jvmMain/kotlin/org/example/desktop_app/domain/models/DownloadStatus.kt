package org.example.desktop_app.domain.models

enum class DownloadStatus {
    READY,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED,
    UNKNOWN
}
package com.sharepointvideodownloader.desktop.presentation.mappers

import com.sharepointvideodownloader.desktop.domain.models.VideoHistory
import com.sharepointvideodownloader.desktop.presentation.models.UiVideoHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun VideoHistory.toUiModel(): UiVideoHistory {
    return UiVideoHistory(
        id = this.id,
        name = this.name,
        savedPath = this.savedPath,
        size = this.size,
        status = this.status,
        progress = this.progress,
        downloadDate = formatTimestamp(this.downloadDate)
    )
}

private fun formatTimestamp(millis: Long): String {
    return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(millis))
}
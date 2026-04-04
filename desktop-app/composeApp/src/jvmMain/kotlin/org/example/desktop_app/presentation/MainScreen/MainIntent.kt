package org.example.desktop_app.presentation.MainScreen

sealed interface MainIntent {
    data class StartDownload(val videoId: Long) : MainIntent
    data class NewVideoReceived(val rawJson: String) : MainIntent
    data class OpenFileInExplorer(val path: String): MainIntent
}
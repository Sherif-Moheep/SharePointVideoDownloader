package org.example.desktop_app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.desktop_app.data.util.FileManager.openFileInExplorer
import org.example.desktop_app.domain.models.DownloadStatus
import org.example.desktop_app.domain.models.VideoDownloadRepository
import org.example.desktop_app.domain.models.VideoHistory
import org.example.desktop_app.domain.repository.ChromeRepository
import org.example.desktop_app.domain.repository.HistoryRepository
import org.example.desktop_app.presentation.mappers.toUiModel

class MainViewModel(
    private val chromeRepository: ChromeRepository,
    private val historyRepository: HistoryRepository,
    private val videoDownloadRepository: VideoDownloadRepository
) : ViewModel() {

    private val _state = MutableStateFlow<MainState>(MainState.Loading)
    val state: StateFlow<MainState> = _state.asStateFlow()

    private val _effect = Channel<MainEffect>()
    val effect = _effect.receiveAsFlow()


    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Grab the very first list from the database directly (ignores the UI state)
            val currentHistory = historyRepository.getDownloadHistory().first()

            // Find any videos that were interrupted when the app last closed/crashed
            val stuckVideos = currentHistory.filter { it.status == DownloadStatus.DOWNLOADING }

            // Flip them all to CANCELLED
            stuckVideos.forEach { video ->
                historyRepository.upsertVideo(video.copy(status = DownloadStatus.CANCELLED))
            }
        }
        // Listen to your Room Database Flow forever
        viewModelScope.launch {
            historyRepository.getDownloadHistory().collect { historyList ->
                val uiList = historyList.map{ it.toUiModel() }
                _state.value = MainState.Dashboard(downloads = uiList)
            }
        }
    }

    fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.NewVideoReceived -> handleNewVideoJson(rawJson =  intent.rawJson)
            is MainIntent.StartDownload -> startDownload(videoId = intent.videoId)
            is MainIntent.OpenFileInExplorer -> openDownloadedFile(filePath = intent.path)
//            is MainIntent.CancelAllDownloads -> cancelAllDownloads()
        }
    }

    private fun handleNewVideoJson(rawJson: String) {
        if (rawJson.isBlank()) return

        viewModelScope.launch {
            chromeRepository.processVideoData(rawJson).onSuccess { videoMessage ->
                val newVideo = VideoHistory(
                    id = 0L,
                    name = videoMessage.title,
                    url = videoMessage.url,
//                    savedPath = "",
//                    duration = "Unknown",
                    size = videoMessage.size,
                    downloadDate = System.currentTimeMillis()
                )

                historyRepository.upsertVideo(newVideo)

            }.onFailure { error ->
                val currentState = _state.value
                if (currentState is MainState.Dashboard) {
                    _state.value = currentState.copy(errorMessage = error.message ?: "Extension Error")
                }
            }
        }
    }

    private fun startDownload(videoId: Long) {
        viewModelScope.launch {
            val currentState = _state.value

            if (currentState is MainState.Dashboard) {
                val targetVideo = historyRepository.getVideoById(videoId) ?: return@launch

                targetVideo.let { video ->

                    // 1. Update status to DOWNLOADING to disable UI buttons
                    val downloadingVideo = video.copy(status = DownloadStatus.DOWNLOADING)
                    historyRepository.upsertVideo(downloadingVideo)

                    try {
                        // 2. Start yt-dlp via the repository
                        val finalPath = videoDownloadRepository.downloadVideo(
                            videoName = video.name,
                            url = video.url,
                            onProgress = { currentProgress ->
                                // 3. This block fires rapidly as yt-dlp outputs progress.
                                // Update Room DB, which instantly updates the UI progress bar!
                                viewModelScope.launch(Dispatchers.IO) {
                                    val updatingVideo = downloadingVideo.copy(progress = currentProgress)
                                    historyRepository.upsertVideo(video = updatingVideo)
                                }
                            }
                        )

                        // 4. Success! yt-dlp finished. Save the final file path and mark 100%.
                        val finishedVideo = downloadingVideo.copy(
                            status = DownloadStatus.COMPLETED,
                            progress = 100,
                            savedPath = finalPath
                        )
                        historyRepository.upsertVideo(finishedVideo)

                    } catch (e: Exception) {
                        // 5. Something broke (network error, yt-dlp crash, etc.)
                        val failedVideo = downloadingVideo.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = e.message
                        )
                        historyRepository.upsertVideo(failedVideo)
                    }
                }
            }
        }
    }

    private fun openDownloadedFile(filePath: String?) {
        if (filePath.isNullOrBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val success = openFileInExplorer(filePath)

            if (!success) {
                // If it failed, tell the UI to show a toast
                _effect.send(MainEffect.ShowToast("File missing. It may have been moved or deleted."))
            }
        }

    }
}
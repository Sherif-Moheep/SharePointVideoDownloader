package org.example.desktop_app.presentation.mainScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import desktop_app.composeapp.generated.resources.Res
import desktop_app.composeapp.generated.resources.file_missing
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.example.desktop_app.domain.usecases.MainUseCases
import org.example.desktop_app.presentation.mappers.toUiModel
import org.jetbrains.compose.resources.getString

class MainViewModel(
    private val mainUseCases: MainUseCases
) : ViewModel() {

    private val _state = MutableStateFlow<MainState>(MainState.Loading)
    val state: StateFlow<MainState> = _state.asStateFlow()

    private val _effect = Channel<MainEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            mainUseCases.resetStuckDownloads()
        }
        // Listen to your Room Database Flow forever
        viewModelScope.launch(Dispatchers.IO) {
            mainUseCases.getDownloadHistory().collect { historyList ->
                    val uiList = historyList.map{ it.toUiModel() }
                    _state.value = MainState.Dashboard(downloads = uiList.toPersistentList())

            }
        }
    }

    fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.NewVideoReceived -> handleNewVideoJson(rawJson =  intent.rawJson)
            is MainIntent.StartDownload -> startDownload(videoId = intent.videoId)
            is MainIntent.OpenFileInExplorer -> openDownloadedFile(filePath = intent.path)
        }
    }

    private fun handleNewVideoJson(rawJson: String) {
        if (rawJson.isBlank()) return

        viewModelScope.launch {
            mainUseCases.processNewVideoJson(rawJson = rawJson).onFailure { error ->
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
                viewModelScope.launch {
                    mainUseCases.downloadVideo(
                        videoId = videoId
                    )
                }
            }
        }
    }

    private fun openDownloadedFile(filePath: String?) {
        if (filePath.isNullOrBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val success = mainUseCases.openFile(
                filePath = filePath
            )

            if (!success) {
                // If it failed, tell the UI to show a toast
                _effect.send(MainEffect.ShowToast(getString(Res.string.file_missing)))
            }
        }
    }
}
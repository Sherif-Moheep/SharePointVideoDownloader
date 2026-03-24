package org.example.desktop_app.presentation

import org.example.desktop_app.presentation.models.UiVideoHistory

sealed class MainState {
    data object Loading : MainState() // Only used for the first split-second of app launch
    data class Dashboard(
        val downloads: List<UiVideoHistory> = emptyList(),
        val errorMessage: String? = null
    ) : MainState()
}
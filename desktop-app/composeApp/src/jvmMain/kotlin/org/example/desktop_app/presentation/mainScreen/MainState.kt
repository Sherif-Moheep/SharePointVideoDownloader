package org.example.desktop_app.presentation.mainScreen

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.example.desktop_app.presentation.models.UiVideoHistory

sealed interface MainState {
    data object Loading : MainState // Only used for the first split-second of app launch
    @Stable
    data class Dashboard(
        val downloads: ImmutableList<UiVideoHistory> = persistentListOf(),
        val errorMessage: String? = null
    ): MainState
}
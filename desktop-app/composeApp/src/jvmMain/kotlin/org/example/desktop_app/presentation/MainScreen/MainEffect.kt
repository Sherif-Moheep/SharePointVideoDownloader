package org.example.desktop_app.presentation.MainScreen

sealed interface MainEffect {
    data class ShowToast(val message: String) : MainEffect
}
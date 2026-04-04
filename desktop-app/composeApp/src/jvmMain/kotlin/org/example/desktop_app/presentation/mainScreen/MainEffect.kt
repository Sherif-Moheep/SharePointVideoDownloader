package org.example.desktop_app.presentation.mainScreen

sealed interface MainEffect {
    data class ShowToast(val message: String) : MainEffect
}
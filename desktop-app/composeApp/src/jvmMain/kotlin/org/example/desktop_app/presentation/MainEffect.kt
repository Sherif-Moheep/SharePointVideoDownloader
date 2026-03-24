package org.example.desktop_app.presentation

sealed interface MainEffect {
    data class ShowToast(val message: String) : MainEffect
}
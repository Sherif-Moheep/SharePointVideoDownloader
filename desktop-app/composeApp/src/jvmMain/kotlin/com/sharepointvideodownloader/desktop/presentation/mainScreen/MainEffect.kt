package com.sharepointvideodownloader.desktop.presentation.mainScreen

sealed interface MainEffect {
    data class ShowToast(val message: String) : MainEffect
}
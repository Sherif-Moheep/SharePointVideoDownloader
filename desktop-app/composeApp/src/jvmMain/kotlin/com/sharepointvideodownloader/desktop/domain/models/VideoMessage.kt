package com.sharepointvideodownloader.desktop.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class VideoMessage(
    val url: String,
    val title: String,
    val size: String
)
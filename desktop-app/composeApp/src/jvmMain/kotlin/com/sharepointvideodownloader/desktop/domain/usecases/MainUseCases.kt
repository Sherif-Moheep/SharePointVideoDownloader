package com.sharepointvideodownloader.desktop.domain.usecases

data class MainUseCases(
    val getDownloadHistory: GetDownloadHistoryUseCase,
    val resetStuckDownloads: ResetStuckDownloadsUseCase,
    val processNewVideoJson: ProcessNewVideoJsonUseCase,
    val downloadVideo: DownloadVideoUseCase,
    val openFile: OpenFileUseCase
)

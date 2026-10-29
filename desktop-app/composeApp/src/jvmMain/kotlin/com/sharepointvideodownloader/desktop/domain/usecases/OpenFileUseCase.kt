package com.sharepointvideodownloader.desktop.domain.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sharepointvideodownloader.desktop.data.util.FileManager.openFileInExplorer

class OpenFileUseCase {
    suspend operator fun invoke(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            openFileInExplorer(filePath)
        }
    }
}
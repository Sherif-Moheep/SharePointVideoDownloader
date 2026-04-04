package org.example.desktop_app.domain.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.desktop_app.data.util.FileManager.openFileInExplorer

class OpenFileUseCase {
    suspend operator fun invoke(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            openFileInExplorer(filePath)
        }
    }
}
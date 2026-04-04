package org.example.desktop_app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.example.desktop_app.domain.models.VideoMessage
import org.example.desktop_app.domain.repository.ChromeRepository

class ChromeRepositoryImpl : ChromeRepository {

    private val jsonConfig = Json { ignoreUnknownKeys = true }

    override suspend fun processVideoData(rawJson: String): Result<VideoMessage> = withContext(Dispatchers.IO) {
        try {
            if (rawJson.isBlank()) {
                return@withContext Result.failure(Exception("Received empty data from Chrome."))
            }

            val video = jsonConfig.decodeFromString<VideoMessage>(rawJson)
            Result.success(video)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse data: ${e.message}"))
        }
    }
}
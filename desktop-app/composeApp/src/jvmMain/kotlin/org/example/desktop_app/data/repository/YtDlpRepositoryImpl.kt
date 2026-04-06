package org.example.desktop_app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.example.desktop_app.data.util.BinaryExtractor
import org.example.desktop_app.data.util.FileManager
import org.example.desktop_app.domain.repository.VideoDownloadRepository

const val DEBUG = false

class YtDlpRepositoryImpl : VideoDownloadRepository {

    override suspend fun downloadVideo(
        videoName: String,
        url: String,
        onProgress: (Int) -> Unit
    ): String = withContext(Dispatchers.IO) {

        val targetFile = FileManager.getOutputFile(videoName)
        val numberOfFragments = 4
        val userAgentValue = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        val ytDlpPath = BinaryExtractor.getOrExtractYtDlp()
        val ffmpegPath = BinaryExtractor.getOrExtractFfmpeg()

        val command = mutableListOf(
            ytDlpPath,
            "--newline",
            "--ffmpeg-location", ffmpegPath,
            "--no-playlist",
            "--concurrent-fragments", numberOfFragments.toString(),
            "--retry-sleep", "fragment:exp=1:20",
            "--user-agent", userAgentValue,
            "-o", targetFile.absolutePath,
            cleanUrl(url).trim()
        )

        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(true) // Merge errors with standard output
        val process = processBuilder.start()

        // Regex to catch yt-dlp's progress output like "[download]  25.3% of..."
        try {
            // Regex to strictly catch the percentage number: e.g., "[download]  25.3%"
            val progressRegex = Regex("""\[download\]\s+([\d.]+)%""")
            var isAudioPhase = false

            process.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {

                    // Ensure we stop reading if the coroutine is cancelled
                    ensureActive()

                    val currentLine = line ?: ""
                    if (DEBUG) println("yt-dlp: $currentLine") // Keep for debugging ()

                    // 1. Detect if we switched to downloading the Audio track
                    if (currentLine.contains("[download] Destination:") && (currentLine.contains(".m4a") || currentLine.contains(".webm"))) {
                        isAudioPhase = true
                    }

                    // 2. Detect if we are in the final ffmpeg merging phase
                    if (currentLine.contains("[Merger] Merging formats")) {
                        onProgress(99)
                        continue
                    }

                    // 3. Parse the percentage and apply the +100 logic
                    val matchResult = progressRegex.find(currentLine)
                    if (matchResult != null) {
                        val rawPercent = matchResult.groupValues[1].toFloatOrNull() ?: 0f

                        // Add 100 if we are in the audio phase
                        val totalScale = if (isAudioPhase) {
                            rawPercent + 100f
                        } else {
                            rawPercent
                        }

                        // Divide by 2 to convert the 0-200 scale back to a 0-100 integer for your UI
                        val finalPercentInt = (totalScale / 2f).toInt()

                        onProgress(finalPercentInt)
                    }
                }
            }

            val exitCode = process.waitFor()

            if (exitCode == 0 && targetFile.exists()) {
                return@withContext targetFile.absolutePath
            } else {
                throw Exception("yt-dlp failed with exit code $exitCode")
            }

        } finally {
            // 4. Critical Cleanup: Destroy the yt-dlp process if the coroutine fails or is cancelled
            process.destroy()
        }
    }

    private fun cleanUrl(url: String): String{
        val anchorDash = "format=dash"
        return if (url.contains(anchorDash)) {
            url.substringBefore(anchorDash) + anchorDash
        } else url
    }
}
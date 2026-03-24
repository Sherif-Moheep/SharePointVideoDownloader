package org.example.desktop_app.data.util

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object BinaryExtractor {

    // Gets the OS temp directory (e.g., C:\Users\YourName\AppData\Local\Temp\)
    private val tempDir = System.getProperty("java.io.tmpdir")
    private val appTempDir = File(tempDir, "FdmAppBinaries").apply { mkdirs() }

    fun getOrExtractYtDlp(): String {
        return extractBinary("bin/yt-dlp.exe", "yt-dlp.exe").absolutePath
    }

    fun getOrExtractFfmpeg(): String {
        return extractBinary("bin/ffmpeg.exe", "ffmpeg.exe").absolutePath
    }

    private fun extractBinary(resourcePath: String, outputName: String): File {
        val targetFile = File(appTempDir, outputName)

        // If it already exists and has a file size, skip extraction to save time!
        if (targetFile.exists() && targetFile.length() > 0) {
            return targetFile
        }

        // Read the .exe from the compiled app resources
        val inputStream: InputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
            ?: throw Exception("Could not find $resourcePath in app resources! Check your spelling and folder structure.")

        // Copy it to the temp folder
        Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        inputStream.close()

        // Make sure the OS allows us to execute it
        targetFile.setExecutable(true)

        return targetFile
    }
}
package org.example.desktop_app.data.util

import java.io.File

object FileManager {

    private const val DEBUG = false

    // 1. Define the specific folder name
    private const val APP_FOLDER_NAME = "SharePoint Downloads"


    // Gets the path to the app's specific download folder.
    // It automatically creates the folder if it doesn't exist yet.
    fun getDownloadDirectory(): File {
        // Gets "C:\Users\Name" (Win) or "/Users/Name" (Mac)
        val userHome = System.getProperty("user.home")

        // Target: ~/Downloads/SharePoint Downloads
        val downloadDir = File(userHome, "Downloads${File.separator}$APP_FOLDER_NAME")

        // If the folder doesn't exist, create it!
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }

        return downloadDir
    }


    // Generates a safe file path for a specific video.
    fun getOutputFile(videoName: String): File {
        val directory = getDownloadDirectory()
        val safeFileName = sanitizeFileName(videoName)

        return File(directory, safeFileName)
    }

    // Helper to strip illegal characters (like : / \ ? * < > |) from video titles
    // so the OS doesn't throw an error when creating the file.
    private fun sanitizeFileName(name: String): String {
        val illegalChars = "[\\\\/:*?\"<>|]".toRegex()
        return name.replace(illegalChars, "_").trim()
    }

    fun openFileInExplorer(filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) return false

        val file = File(filePath)
        if (!file.exists()) {
            if (DEBUG) println("File does not exist: $filePath")
            return false
        }

        val os = System.getProperty("os.name").lowercase()

        try {
            when {
                os.contains("win") -> {
                    // Windows: Opens Explorer and selects the specific file
                    Runtime.getRuntime().exec(arrayOf("explorer.exe", "/select,", file.absolutePath))
                }
                os.contains("mac") -> {
                    // macOS: Opens Finder and selects the specific file
                    Runtime.getRuntime().exec(arrayOf("open", "-R", file.absolutePath))
                }
                else -> {
                    // Linux / Fallback: Just open the parent directory
                    java.awt.Desktop.getDesktop().open(file.parentFile)
                }
            }
            return true
        } catch (e: Exception) {
            if (DEBUG) println("Failed to open file explorer: ${e.message}")
            return false
        }
    }
}
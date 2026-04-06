package org.example.desktop_app.data.util

import java.io.File

object ChromeExtensionInstaller {

    private const val DEBUG = false

    fun installNativeMessagingHost(extensionId: String) {
        try {
            val appDataPath = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
            val appFolder = File(appDataPath, "SharePointDownloader")

            if (!appFolder.exists()) {
                appFolder.mkdirs()
            }

            val manifestFile = File(appFolder, "com.sharepoint.downloader.json")

            val appExePath = System.getProperty("jpackage.app-path")
                ?: (System.getProperty("user.dir") + "\\SharePoint Video Downloader.exe")

            val escapedExePath = appExePath.replace("\\", "\\\\")

            val jsonContent = """
                {
                  "name": "com.sharepoint.downloader",
                  "description": "Native Host for SharePoint Downloader",
                  "path": "$escapedExePath",
                  "type": "stdio",
                  "allowed_origins": [
                    "chrome-extension://$extensionId/"
                  ]
                }
            """.trimIndent()

            manifestFile.writeText(jsonContent)

            val regKey = "HKCU\\Software\\Google\\Chrome\\NativeMessagingHosts\\com.sharepoint.downloader"

            val command = listOf(
                "reg", "add", regKey,
                "/ve",
                "/t", "REG_SZ",
                "/d", manifestFile.absolutePath,
                "/f"
            )

            val exitCode = ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
                .waitFor()

            if (DEBUG) {
                if (exitCode == 0) println("Native messaging manifest installed successfully!")
                else println("Registry injection failed with exit code: $exitCode")
            }

        } catch (e: Exception) {
            if (DEBUG) println("Failed to install Chrome manifest: ${e.message}")
            e.printStackTrace()
        }
    }
}
package org.example.desktop_app.presentation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import desktop_app.composeapp.generated.resources.Res
import desktop_app.composeapp.generated.resources.app_icon
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.example.desktop_app.data.util.ChromeMessageDecoder
import org.example.desktop_app.di.appModule
import org.example.desktop_app.domain.models.DownloadStatus
import org.example.desktop_app.presentation.MainScreen.MainState
import org.example.desktop_app.presentation.MainScreen.MainViewModel
import org.example.desktop_app.presentation.components.ExitWarningDialog
import org.example.desktop_app.presentation.theme.AppTheme
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext.startKoin
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.BindException
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.system.exitProcess

const val INSTANCE_PORT = 49152

// A global flow to pass messages from the OS layer up to the Compose UI layer
private val _incomingJsonFlow = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 10)
val incomingJsonFlow = _incomingJsonFlow.asSharedFlow()

fun main(args: Array<String>) {
    try {
        // 1. HOST ATTEMPT: Try to claim the port
        val serverSocket = ServerSocket(INSTANCE_PORT)

        // Start a background thread to handle incoming data forever
        thread(isDaemon = true) {
            // A. If launched by Chrome, read the very first message from System.in
            val launchedByChrome = args.any { it.startsWith("chrome-extension://") }
            if (launchedByChrome) {
                ChromeMessageDecoder.readStream(System.`in`)?.let { _incomingJsonFlow.tryEmit(it) }
            }

            // B. Listen for future clicks sent by Proxy instances
            while (true) {
                try {
                    val client = serverSocket.accept()
                    val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
                    val proxyJson = reader.readLine()
                    if (!proxyJson.isNullOrBlank()) {
                        _incomingJsonFlow.tryEmit(proxyJson)
                    }
                    client.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 2. LAUNCH UI: Only the Host process makes it here
        startKoin { modules(appModule) }

        application {
            val viewModel = koinInject<MainViewModel>()
            val state by viewModel.state.collectAsState()

            // Safely check for active downloads
            val hasActiveDownloads = if (state is MainState.Dashboard) {
                (state as MainState.Dashboard).downloads.any { it.status == DownloadStatus.DOWNLOADING }
            } else false

            var showExitWarning by remember { mutableStateOf(false) }

            Window(
                onCloseRequest = {
                    if (hasActiveDownloads) {
                        showExitWarning = true // Intercept the close request!
                    } else {
                        exitApplication() // Safe to close normally
                    }
                },
                title = "SharePoint Video Downloader",
                icon = painterResource(Res.drawable.app_icon),
                alwaysOnTop = true
            ) {

                MainScreen(viewModel = viewModel)

                // 3. THE SAFE-EXIT DIALOG
                if (showExitWarning) {
                    AppTheme {
                        ExitWarningDialog(
                            onDismiss = { showExitWarning = false },
                            onConfirmExit = { exitApplication() }
                        )
                    }
                }
            }
        }

    } catch (e: BindException) {
        // 3. PROXY INSTANCE: Port is taken, which means the app is already running!

        // Read the message Chrome just sent us via System.in
        val newJsonMessage = ChromeMessageDecoder.readStream(System.`in`)

        if (!newJsonMessage.isNullOrBlank()) {
            try {
                // Forward the JSON to the Host instance over the local socket
                Socket("localhost", INSTANCE_PORT).use { socket ->
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println(newJsonMessage) // Send with a newline so reader.readLine() catches it
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        // 1. Create a simple JSON response confirming the hand-off
        val responseBytes = "{\"status\": \"forwarded\"}".toByteArray(Charsets.UTF_8)

        // 2. Write the 4-byte length header (Chrome strictly requires little-endian format)
        System.out.write(
            byteArrayOf(
                responseBytes.size.toByte(),
                (responseBytes.size shr 8).toByte(),
                (responseBytes.size shr 16).toByte(),
                (responseBytes.size shr 24).toByte()
            )
        )

        // 3. Write the actual JSON payload and flush the stream
        System.out.write(responseBytes)
        System.out.flush()

        // NOW you can safely kill this redundant process
        exitProcess(0)
    }
}
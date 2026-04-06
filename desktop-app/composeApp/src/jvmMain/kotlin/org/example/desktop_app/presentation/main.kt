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
import org.example.desktop_app.data.util.ChromeExtensionInstaller
import org.example.desktop_app.data.util.ChromeMessageDecoder
import org.example.desktop_app.di.appModule
import org.example.desktop_app.domain.models.DownloadStatus
import org.example.desktop_app.presentation.mainScreen.MainState
import org.example.desktop_app.presentation.mainScreen.MainViewModel
import org.example.desktop_app.presentation.components.ExitWarningDialog
import org.example.desktop_app.presentation.mainScreen.MainScreen
import org.example.desktop_app.presentation.theme.AppTheme
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext.startKoin
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.io.PrintWriter
import java.net.BindException
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private const val INSTANCE_PORT = 49152
private const val extensionId = "nbipmgebklaiigmhoflnjpoifnlbblbk"

// A global flow to pass messages from the OS layer up to the Compose UI layer
private val _incomingJsonFlow = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 10)
val incomingJsonFlow = _incomingJsonFlow.asSharedFlow()

fun main(args: Array<String>) {

    val originalOut = System.out // saving the System.out to a variable
    System.setOut(System.err) // redirects all print statements to System.err, which chrome native messaging ignores
    try {
        // 1. HOST ATTEMPT: Try to claim the port
        val serverSocket = ServerSocket(INSTANCE_PORT)

        // Configures the host.json file
        ChromeExtensionInstaller.installNativeMessagingHost(extensionId = extensionId)

        // Start a background thread to handle incoming data forever
        thread(isDaemon = true) {
            // A. If launched by Chrome, read the very first message from System.in
            val launchedByChrome = args.any { it.startsWith("chrome-extension://") }
            if (launchedByChrome) {
                ChromeMessageDecoder.readStream(System.`in`)?.let { _incomingJsonFlow.tryEmit(it) }

                sendChromeReply(statusMessage = "launched_and_received", originalOut = originalOut)
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

    } catch (_: BindException) {
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

        sendChromeReply(statusMessage = "forwarded", originalOut = originalOut)

        exitProcess(0)
    }
}

private fun sendChromeReply(statusMessage: String, originalOut: PrintStream) {
    // Wrap the message in a simple JSON object
    val responseBytes = "{\"status\": \"$statusMessage\"}".toByteArray(Charsets.UTF_8)

    // Write the 4-byte length header using the pristine, original stream
    originalOut.write(
        byteArrayOf(
            responseBytes.size.toByte(),
            (responseBytes.size shr 8).toByte(),
            (responseBytes.size shr 16).toByte(),
            (responseBytes.size shr 24).toByte()
        )
    )

    // Write the actual JSON payload and flush the stream
    originalOut.write(responseBytes)
    originalOut.flush()
}
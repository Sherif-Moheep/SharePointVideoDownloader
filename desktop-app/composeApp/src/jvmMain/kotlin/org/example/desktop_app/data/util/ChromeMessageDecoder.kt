package org.example.desktop_app.data.util

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ChromeMessageDecoder {
    fun readStream(inputStream: InputStream): String? {
        val lengthBytes = ByteArray(4)
        if (inputStream.read(lengthBytes, 0, 4) < 4) return null

        // Chrome sends the message length as a 32-bit integer in native byte order
        val length = ByteBuffer.wrap(lengthBytes).order(ByteOrder.nativeOrder()).int

        // Safety check to prevent massive allocations
        if (length <= 0 || length > 10 * 1024 * 1024) return null

        val contentBytes = ByteArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val read = inputStream.read(contentBytes, totalRead, length - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return String(contentBytes, Charsets.UTF_8)
    }
}
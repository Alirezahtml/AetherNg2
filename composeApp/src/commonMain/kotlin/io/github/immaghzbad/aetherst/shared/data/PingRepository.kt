package io.github.immaghzbad.aetherst.shared.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import kotlin.time.Duration.Companion.milliseconds

object PingRepository {
    private val _pingState = MutableStateFlow(PingState())
    val pingState: StateFlow<PingState> = _pingState.asStateFlow()

    private val mutex = kotlinx.coroutines.sync.Mutex()

    private const val PING_HOST = "1.1.1.1"
    private const val PING_PORT = 80
    private const val SAMPLES = 5
    private const val CONNECT_TIMEOUT_MS = 5000
    private const val READ_TIMEOUT_MS = 5000

    suspend fun runPing(socksHost: String = "127.0.0.1", socksPort: Int = 1819, useProxy: Boolean = true) {
        if (!mutex.tryLock()) return

        try {
            _pingState.value = _pingState.value.copy(isPinging = true, error = null)

            withContext(Dispatchers.Default) {
                val samples = mutableListOf<Long>()

                for (attempt in 1..SAMPLES) {
                    val rtt = measureRtt(socksHost, socksPort, useProxy)
                    if (rtt > 0) {
                        samples.add(rtt)
                    }
                    if (attempt < SAMPLES) {
                        delay(400.milliseconds)
                    }
                }

                if (samples.isNotEmpty()) {
                    val sorted = samples.sorted()
                    val median = sorted[sorted.size / 2]
                    _pingState.value = PingState(ms = median, isPinging = false)
                    LogRepository.i("Ping: ${median}ms (samples: ${samples.joinToString()}, proxy: $useProxy)", "Ping")
                } else {
                    _pingState.value = PingState(ms = -1, isPinging = false, error = "Timeout")
                }
            }
        } finally {
            mutex.unlock()
        }
    }

    private fun measureRtt(socksHost: String, socksPort: Int, useProxy: Boolean): Long {
        return try {
            val socket = if (useProxy) {
                val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(socksHost, socksPort))
                Socket(proxy)
            } else {
                Socket()
            }

            socket.use { s ->
                s.soTimeout = READ_TIMEOUT_MS
                s.connect(InetSocketAddress(PING_HOST, PING_PORT), CONNECT_TIMEOUT_MS)

                val request = "GET /cdn-cgi/trace HTTP/1.1\r\nHost: 1.1.1.1\r\nConnection: close\r\n\r\n"
                val output = s.getOutputStream()
                val input = s.getInputStream()

                val startTime = System.nanoTime()
                output.write(request.toByteArray())
                output.flush()

                val buffer = ByteArray(1024)
                var totalRead = 0
                while (totalRead < buffer.size) {
                    val read = input.read(buffer, totalRead, buffer.size - totalRead)
                    if (read == -1) break
                    totalRead += read
                    if (totalRead >= 4) {
                        val tail = String(buffer, totalRead - 4, 4)
                        if (tail.contains("\r\n\r\n")) break
                    }
                }

                val endTime = System.nanoTime()
                (endTime - startTime) / 1_000_000
            }
        } catch (_: Exception) {
            -1
        }
    }

    fun reset() {
        _pingState.value = PingState()
    }
}

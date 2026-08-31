package io.github.immaghzbad.aetherst.shared.data

import io.github.immaghzbad.aetherst.shared.core.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import kotlinx.serialization.json.*
import java.net.InetSocketAddress
import java.net.Proxy
import kotlin.time.Duration.Companion.milliseconds

object IpInfoRepository {
    private val _ipInfo = MutableStateFlow(IpInfo())
    val ipInfo: StateFlow<IpInfo> = _ipInfo.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = kotlinx.coroutines.sync.Mutex()

    suspend fun fetchIpInfo(socksHost: String = "127.0.0.1", socksPort: Int = 1819, useProxy: Boolean = true) {
        if (!mutex.tryLock()) return

        try {
            _ipInfo.value = _ipInfo.value.copy(isLoading = true)

            withContext(Dispatchers.Default) {
                if (!useProxy) {
                    LogRepository.i("Querying public IP (direct)...", "IpWhois")
                    val result = fetchParallelDirect()
                    if (result != null) {
                        _ipInfo.value = result
                        LogRepository.i("Direct IP: ${result.ip} (${result.country})", "IpWhois")
                        return@withContext
                    }
                } else {
                    delay(2500.milliseconds)

                    for (attempt in 1..3) {
                        LogRepository.i("Querying public IP via tunnel ($socksHost:$socksPort) attempt $attempt...", "IpWhois")

                        val result = fetchParallelViaProxy(socksHost, socksPort)
                        if (result != null) {
                            _ipInfo.value = result
                            LogRepository.i("Tunnel IP: ${result.ip} (${result.country})", "IpWhois")
                            return@withContext
                        }

                        if (attempt < 3) {
                            delay(2000.milliseconds)
                        }
                    }

                    LogRepository.w("SOCKS proxy lookup failed, falling back to direct...", "IpWhois")
                    val fallback = fetchParallelDirect()
                    if (fallback != null) {
                        _ipInfo.value = fallback
                        LogRepository.i("Fallback direct IP: ${fallback.ip} (${fallback.country})", "IpWhois")
                        return@withContext
                    }
                }

                LogRepository.w("${if (useProxy) "SOCKS proxy" else "Direct"} IP lookup failed after all attempts.", "IpWhois")
                _ipInfo.value = _ipInfo.value.copy(
                    isLoading = false,
                    error = if (useProxy) "Proxy Lookup Failed" else "Direct Lookup Failed"
                )
            }
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun fetchParallelDirect(): IpInfo? = coroutineScope {
        val sources = listOf(
            async { tryDirectIpSb() },
            async { tryDirectIpify() },
            async { tryDirectIpinfoIo() },
            async { tryDirectIfconfig() }
        )
        for (future in sources) {
            val result = future.await()
            if (result != null) {
                sources.forEach { it.cancel() }
                return@coroutineScope result
            }
        }
        null
    }

    private suspend fun fetchParallelViaProxy(socksHost: String, socksPort: Int): IpInfo? = coroutineScope {
        val sources = listOf(
            async { tryViaProxyIpSb(socksHost, socksPort) },
            async { tryViaProxyIpify(socksHost, socksPort) },
            async { tryViaProxyIfconfig(socksHost, socksPort) },
            async { tryViaProxyIpinfoIo(socksHost, socksPort) },
            async { tryViaProxyAmazon(socksHost, socksPort) }
        )
        for (future in sources) {
            val result = future.await()
            if (result != null) {
                sources.forEach { it.cancel() }
                return@coroutineScope result
            }
        }
        null
    }

    private fun tryViaProxyIpSb(socksHost: String, socksPort: Int): IpInfo? {
        return try {
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(socksHost, socksPort))
            val client = NetworkClient.instance.newBuilder()
                .proxy(proxy)
                .connectTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder()
                .url("https://api.ip.sb/geoip")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return null
                    val root = json.parseToJsonElement(jsonStr).jsonObject
                    val ip = root["ip"]?.jsonPrimitive?.content ?: ""
                    val country = root["country"]?.jsonPrimitive?.content ?: "Unknown"
                    val countryCode = root["country_code"]?.jsonPrimitive?.content ?: ""

                    if (ip.isNotEmpty()) {
                        LogRepository.i("Geo-data (ip.sb): $ip ($country)", "IpWhois")
                        IpInfo(ip, country, countryCode, getFlagEmoji(countryCode), false)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            LogRepository.w("ip.sb via SOCKS error: ${e.message}", "IpWhois")
            null
        }
    }

    private fun tryViaProxyIpify(socksHost: String, socksPort: Int): IpInfo? {
        return try {
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(socksHost, socksPort))
            val client = NetworkClient.instance.newBuilder()
                .proxy(proxy)
                .connectTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder()
                .url("https://api.ipify.org?format=json")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return null
                    val root = json.parseToJsonElement(jsonStr).jsonObject
                    val ip = root["ip"]?.jsonPrimitive?.content ?: ""

                    if (ip.isNotEmpty()) {
                        LogRepository.i("IP via ipify: $ip", "IpWhois")
                        IpInfo(ip, "Unknown", "", getFlagEmoji(""), false)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            LogRepository.w("ipify via SOCKS error: ${e.message}", "IpWhois")
            null
        }
    }

    private fun tryViaProxyIfconfig(socksHost: String, socksPort: Int): IpInfo? {
        return try {
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(socksHost, socksPort))
            val client = NetworkClient.instance.newBuilder()
                .proxy(proxy)
                .connectTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder()
                .url("https://ifconfig.me/all.json")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return null
                    val root = json.parseToJsonElement(jsonStr).jsonObject
                    val ip = root["ip_addr"]?.jsonPrimitive?.content ?: ""
                    val countryCode = root["country_code"]?.jsonPrimitive?.content ?: ""

                    if (ip.isNotEmpty()) {
                        LogRepository.i("IP via ifconfig: $ip", "IpWhois")
                        IpInfo(ip, "Unknown", countryCode, getFlagEmoji(countryCode), false)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            LogRepository.w("ifconfig.me via SOCKS error: ${e.message}", "IpWhois")
            null
        }
    }

    private fun tryViaProxyAmazon(socksHost: String, socksPort: Int): IpInfo? {
        return try {
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(socksHost, socksPort))
            val client = NetworkClient.instance.newBuilder()
                .proxy(proxy)
                .connectTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder()
                .url("https://checkip.amazonaws.com")
                .header("User-Agent", "curl/7.64.1")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val ip = response.body?.string()?.trim() ?: ""
                    if (ip.isNotEmpty()) {
                        LogRepository.i("IP via Amazon: $ip", "IpWhois")
                        IpInfo(ip, "Unknown", "", getFlagEmoji(""), false)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            LogRepository.w("Amazon IP check failed: ${e.message}", "IpWhois")
            null
        }
    }

    private fun tryViaProxyIpinfoIo(socksHost: String, socksPort: Int): IpInfo? {
        return try {
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(socksHost, socksPort))
            val client = NetworkClient.instance.newBuilder()
                .proxy(proxy)
                .connectTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder()
                .url("https://ipinfo.io/json")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return null
                    val root = json.parseToJsonElement(jsonStr).jsonObject
                    val ip = root["ip"]?.jsonPrimitive?.content ?: ""
                    val country = root["country"]?.jsonPrimitive?.content ?: "Unknown"
                    val countryCode = root["country"]?.jsonPrimitive?.content ?: ""

                    if (ip.isNotEmpty()) {
                        LogRepository.i("IP via ipinfo.io: $ip ($country)", "IpWhois")
                        IpInfo(ip, country, countryCode, getFlagEmoji(countryCode), false)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            LogRepository.w("ipinfo.io via SOCKS error: ${e.message}", "IpWhois")
            null
        }
    }

    private fun tryDirectIpSb(): IpInfo? {
        return try {
            val client = NetworkClient.instance.newBuilder()
                .connectTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
            val request = Request.Builder().url("https://api.ip.sb/geoip").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return null
                    val root = json.parseToJsonElement(jsonStr).jsonObject
                    val ip = root["ip"]?.jsonPrimitive?.content ?: ""
                    if (ip.isNotEmpty()) {
                        val country = root["country"]?.jsonPrimitive?.content ?: "Unknown"
                        val countryCode = root["country_code"]?.jsonPrimitive?.content ?: ""
                        IpInfo(ip, country, countryCode, getFlagEmoji(countryCode), false)
                    } else null
                } else null
            }
        } catch (_: Exception) { null }
    }

    private fun tryDirectIpify(): IpInfo? {
        return try {
            val client = NetworkClient.instance.newBuilder()
                .connectTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
            val request = Request.Builder().url("https://api.ipify.org?format=json").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return null
                    val root = json.parseToJsonElement(jsonStr).jsonObject
                    val ip = root["ip"]?.jsonPrimitive?.content ?: ""
                    if (ip.isNotEmpty()) IpInfo(ip, "Unknown", "", getFlagEmoji(""), false) else null
                } else null
            }
        } catch (_: Exception) { null }
    }

    private fun tryDirectIpinfoIo(): IpInfo? {
        return try {
            val client = NetworkClient.instance.newBuilder()
                .connectTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
            val request = Request.Builder().url("https://ipinfo.io/json").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return null
                    val root = json.parseToJsonElement(jsonStr).jsonObject
                    val ip = root["ip"]?.jsonPrimitive?.content ?: ""
                    val country = root["country"]?.jsonPrimitive?.content ?: "Unknown"
                    val countryCode = root["country"]?.jsonPrimitive?.content ?: ""
                    if (ip.isNotEmpty()) IpInfo(ip, country, countryCode, getFlagEmoji(countryCode), false) else null
                } else null
            }
        } catch (_: Exception) { null }
    }

    private fun tryDirectIfconfig(): IpInfo? {
        return try {
            val client = NetworkClient.instance.newBuilder()
                .connectTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(8000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
            val request = Request.Builder().url("https://ifconfig.me/all.json").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return null
                    val root = json.parseToJsonElement(jsonStr).jsonObject
                    val ip = root["ip_addr"]?.jsonPrimitive?.content ?: ""
                    val countryCode = root["country_code"]?.jsonPrimitive?.content ?: ""
                    if (ip.isNotEmpty()) IpInfo(ip, "Unknown", countryCode, getFlagEmoji(countryCode), false) else null
                } else null
            }
        } catch (_: Exception) { null }
    }

    fun reset() { _ipInfo.value = IpInfo() }

    private fun getFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2) return "\uD83C\uDF10"
        val firstLetter = countryCode[0].uppercaseChar().code - 'A'.code + 0x1F1E6
        val secondLetter = countryCode[1].uppercaseChar().code - 'A'.code + 0x1F1E6
        return codePointToString(firstLetter) + codePointToString(secondLetter)
    }

    private fun codePointToString(codePoint: Int): String {
        return if (codePoint <= 0xFFFF) {
            codePoint.toChar().toString()
        } else {
            val high = ((codePoint - 0x10000) shr 10) + 0xD800
            val low = ((codePoint - 0x10000) and 0x3FF) + 0xDC00
            high.toChar().toString() + low.toChar().toString()
        }
    }
}

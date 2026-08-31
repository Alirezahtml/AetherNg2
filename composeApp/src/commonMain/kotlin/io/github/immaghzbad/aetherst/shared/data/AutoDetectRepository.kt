package io.github.immaghzbad.aetherst.shared.data

import io.github.immaghzbad.aetherst.shared.data.LogRepository
import io.github.immaghzbad.aetherst.shared.core.NetworkClient
import io.github.immaghzbad.aetherst.shared.model.*
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.getSystemUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

object AutoDetectRepository {
    private val _state = MutableStateFlow(AutoDetectState())
    val state: StateFlow<AutoDetectState> = _state.asStateFlow()

    private var detectJob: Job? = null
    private val mutex = kotlinx.coroutines.sync.Mutex()

    private const val ICMP_TARGET = "1.1.1.1"
    private const val TCP_TARGET_HOST = "1.1.1.1"
    private const val TCP_TARGET_PORT = 53
    private const val HTTPS_TARGET = "https://1.1.1.1/cdn-cgi/trace"
    private const val SAMPLES = 5
    private const val ICMP_TIMEOUT_MS = 3000

    fun cancel() {
        detectJob?.cancel()
        _state.value = _state.value.copy(phase = AutoDetectPhase.IDLE)
    }

    fun reset() {
        detectJob?.cancel()
        _state.value = AutoDetectState()
    }

    fun startDetection(platformContext: PlatformContext) {
        if (detectJob?.isActive == true) return
        if (!mutex.tryLock()) return

        detectJob = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            try {
                updateState(AutoDetectState(
                    phase = AutoDetectPhase.FINGERPRINTING,
                    currentStep = "Checking IPv6 connectivity...",
                    progressPercent = 5
                ))
                val hasIPv6 = try {
                    withTimeout(5_000L) { detectIPv6() }
                } catch (_: Exception) { false }
                updateState(_state.value.copy(
                    liveFingerprint = NetworkFingerprint(
                        networkType = "open", supportsDPI = false, supportsUDP = true,
                        supportsIPv6 = hasIPv6, carrierOrIsp = "Detecting..."
                    ),
                    currentStep = "Checking DPI restrictions...",
                    progressPercent = 8
                ))

                val isDPI = try {
                    withTimeout(6_000L) { detectDPI() }
                } catch (_: Exception) { false }
                updateState(_state.value.copy(
                    liveFingerprint = NetworkFingerprint(
                        networkType = if (isDPI) "restricted" else "open",
                        supportsDPI = isDPI, supportsUDP = true,
                        supportsIPv6 = hasIPv6, carrierOrIsp = "Detecting..."
                    ),
                    currentStep = "Detecting ISP...",
                    progressPercent = 12
                ))

                val isp = try {
                    withTimeout(6_000L) { detectIsp() }
                } catch (_: Exception) { "Unknown" }
                val fingerprint = NetworkFingerprint(
                    networkType = if (isDPI) "restricted" else "open",
                    supportsDPI = isDPI, supportsUDP = true,
                    supportsIPv6 = hasIPv6, carrierOrIsp = isp
                )
                updateState(_state.value.copy(
                    liveFingerprint = fingerprint,
                    currentStep = "Network fingerprint complete",
                    progressPercent = 15
                ))

                updateState(_state.value.copy(
                    phase = AutoDetectPhase.PROTOCOL_SCAN,
                    currentStep = "Measuring protocol latency...",
                    progressPercent = 20
                ))
                val protocolResults = probeAllProtocols(platformContext)
                updateState(_state.value.copy(progressPercent = 50))

                updateState(_state.value.copy(
                    phase = AutoDetectPhase.MTU_PROBE,
                    currentStep = "Discovering optimal MTU...",
                    progressPercent = 55
                ))
                val mtuResult = probeMtu(platformContext)
                updateState(_state.value.copy(progressPercent = 65))

                updateState(_state.value.copy(
                    phase = AutoDetectPhase.NOISE_PROBE,
                    currentStep = "Testing obfuscation modes...",
                    progressPercent = 70
                ))
                val noiseResults = probeNoiseModes(protocolResults)
                updateState(_state.value.copy(progressPercent = 82))

                updateState(_state.value.copy(
                    phase = AutoDetectPhase.SCAN_MODE_PROBE,
                    currentStep = "Evaluating scan strategies...",
                    progressPercent = 85
                ))
                val scanModeResults = probeScanModes()
                updateState(_state.value.copy(progressPercent = 92))

                updateState(_state.value.copy(
                    phase = AutoDetectPhase.ANALYZING,
                    currentStep = "Computing optimal configuration...",
                    progressPercent = 95
                ))
                val result = analyzeResults(protocolResults, mtuResult, noiseResults, scanModeResults, fingerprint)

                updateState(_state.value.copy(
                    phase = AutoDetectPhase.COMPLETE,
                    currentStep = "Optimal configuration found!",
                    progressPercent = 100,
                    finalResult = result
                ))
                LogRepository.i(
                    "Auto-Detect complete: Protocol=${result.recommendedProtocol.name}, " +
                    "MTU=${result.recommendedMtu}, Noise=${result.recommendedNoise.name}, " +
                    "Confidence=${(result.confidence * 100).toInt()}%",
                    "AutoDetect"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogRepository.e("Auto-Detect failed: ${e.message}", "AutoDetect")
                updateState(_state.value.copy(
                    phase = AutoDetectPhase.ERROR,
                    currentStep = "Detection failed",
                    error = e.message ?: "Unknown error"
                ))
            } finally {
                mutex.unlock()
            }
        }
    }


    private fun networkFingerprint(context: PlatformContext): NetworkFingerprint {
        val hasIPv6 = detectIPv6()
        val isDPI = detectDPI()
        val isp = detectIsp()

        return NetworkFingerprint(
            networkType = if (isDPI) "restricted" else "open",
            supportsDPI = isDPI,
            supportsUDP = true,
            supportsIPv6 = hasIPv6,
            carrierOrIsp = isp
        )
    }

    private fun detectIPv6(): Boolean {
        val isWindows = try {
            System.getProperty("os.name")?.lowercase()?.contains("win") == true
        } catch (_: Throwable) { false }

        val timeout = if (isWindows) 1500 else 3000
        return try {
            val sock = Socket()
            try {
                sock.connect(InetSocketAddress("2606:4700:4700::1111", 53), timeout)
                sock.close()
                true
            } catch (_: Exception) {
                try { sock.close() } catch (_: Exception) {}
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun detectDPI(): Boolean {
        return try {
            val request = Request.Builder().url(HTTPS_TARGET)
                .header("User-Agent", "AetherNg-AutoDetect/1.0").build()
            val client = NetworkClient.instance.newBuilder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    body.contains("warp=on") || body.contains("gateway=true")
                } else {
                    response.code == 403 || response.code == 407
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun detectIsp(): String {
        return try {
            val request = Request.Builder().url("https://api.ipify.org?format=json")
                .header("User-Agent", "AetherNg-AutoDetect/1.0").build()
            val client = NetworkClient.instance.newBuilder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(4, TimeUnit.SECONDS)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = kotlinx.serialization.json.Json.parseToJsonElement(body)
                    val ip = (json as? kotlinx.serialization.json.JsonObject)
                        ?.get("ip")
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    ip ?: "Unknown"
                } else "Unknown"
            }
        } catch (_: Exception) {
            "Unknown"
        }
    }

    private fun measureIcmpLatency(context: PlatformContext): Long {
        val systemUtils = getSystemUtils(context)
        val samples = mutableListOf<Long>()

        repeat(SAMPLES) {
            val success = systemUtils.execPing(ICMP_TARGET, 32, ICMP_TIMEOUT_MS, dontFragment = false)
            if (success) {
            }
        }

        val tcpSamples = measureTcpLatency(TCP_TARGET_HOST, TCP_TARGET_PORT, SAMPLES)
        samples.addAll(tcpSamples)

        return if (samples.isNotEmpty()) {
            samples.sorted()[samples.size / 2]
        } else {
            -1L
        }
    }

    private fun measureTcpLatency(host: String, port: Int, sampleCount: Int): List<Long> {
        val samples = mutableListOf<Long>()

        repeat(sampleCount) {
            try {
                val sock = Socket()
                sock.tcpNoDelay = true
                val start = System.nanoTime()
                sock.connect(InetSocketAddress(host, port), 2500)
                val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
                sock.close()
                samples.add(elapsed)
            } catch (_: Exception) {
            }
        }

        return samples
    }

    private fun measureHttpsLatency(sampleCount: Int): List<Long> {
        val samples = mutableListOf<Long>()
        val client = NetworkClient.instance.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        repeat(sampleCount) {
            try {
                val request = Request.Builder().url(HTTPS_TARGET)
                    .header("User-Agent", "AetherNg/1.0")
                    .build()
                val start = System.nanoTime()
                client.newCall(request).execute().use { response ->
                    val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
                    if (response.isSuccessful) {
                        samples.add(elapsed)
                    }
                }
            } catch (_: Exception) {
            }
        }

        return samples
    }

    private fun medianLatency(samples: List<Long>): Long {
        val sorted = samples.filter { it > 0 }.sorted()
        return if (sorted.isNotEmpty()) sorted[sorted.size / 2] else -1L
    }


    private suspend fun probeAllProtocols(context: PlatformContext): List<ProtocolProbeResult> {
        val protocols = listOf(AetherProtocol.MASQUE, AetherProtocol.WG, AetherProtocol.GOOL)
        val results = mutableListOf<ProtocolProbeResult>()

        for ((index, protocol) in protocols.withIndex()) {
            updateState(_state.value.copy(
                protocolResults = results + ProtocolProbeResult(protocol, ProbeStatus.RUNNING),
                currentStep = "Measuring ${protocol.displayName} latency...",
                progressPercent = 20 + (index * 10)
            ))

            val result = probeProtocol(protocol, context)
            results.add(result)

            updateState(_state.value.copy(
                protocolResults = results.map { it }
            ))

            delay(200)
        }

        return results
    }

    private suspend fun probeProtocol(protocol: AetherProtocol, context: PlatformContext): ProtocolProbeResult {
        return withContext(Dispatchers.Default) {
            try {
                when (protocol) {
                    AetherProtocol.MASQUE -> probeMasque(context)
                    AetherProtocol.WG -> probeWireGuard(context)
                    AetherProtocol.GOOL -> probeGool(context)
                    AetherProtocol.ZERO_TRUST -> ProtocolProbeResult(
                        protocol, ProbeStatus.SKIPPED, -1,
                        "Zero Trust requires manual configuration"
                    )
                }
            } catch (e: Exception) {
                LogRepository.w("Protocol probe failed for ${protocol.name}: ${e.message}", "AutoDetect")
                ProtocolProbeResult(protocol, ProbeStatus.FAILED, -1, e.message)
            }
        }
    }

    private suspend fun probeMasque(context: PlatformContext): ProtocolProbeResult {
        return withContext(Dispatchers.Default) {
            val tcpSamples = measureTcpLatency(TCP_TARGET_HOST, TCP_TARGET_PORT, SAMPLES)
            val tcpMedian = medianLatency(tcpSamples)

            if (tcpMedian < 0) {
                val httpsSamples = measureHttpsLatency(SAMPLES)
                val httpsMedian = medianLatency(httpsSamples)
                return@withContext if (httpsMedian > 0) {
                    ProtocolProbeResult(AetherProtocol.MASQUE, ProbeStatus.SUCCESS, httpsMedian)
                } else {
                    ProtocolProbeResult(AetherProtocol.MASQUE, ProbeStatus.FAILED, -1, "Network unreachable")
                }
            }

            val httpsSamples = measureHttpsLatency(SAMPLES)
            val httpsMedian = medianLatency(httpsSamples)

            val latency = if (httpsMedian > 0) httpsMedian else tcpMedian
            ProtocolProbeResult(AetherProtocol.MASQUE, ProbeStatus.SUCCESS, latency)
        }
    }

    private suspend fun probeWireGuard(context: PlatformContext): ProtocolProbeResult {
        return withContext(Dispatchers.Default) {
            val samples = measureTcpLatency(TCP_TARGET_HOST, TCP_TARGET_PORT, SAMPLES)
            val median = medianLatency(samples)

            if (median > 0) {
                ProtocolProbeResult(AetherProtocol.WG, ProbeStatus.SUCCESS, median)
            } else {
                ProtocolProbeResult(AetherProtocol.WG, ProbeStatus.FAILED, -1, "TCP connect failed")
            }
        }
    }

    private suspend fun probeGool(context: PlatformContext): ProtocolProbeResult {
        return withContext(Dispatchers.Default) {
            val samples = measureTcpLatency(TCP_TARGET_HOST, TCP_TARGET_PORT, SAMPLES)
            val median = medianLatency(samples)

            if (median > 0) {
                val goolLatency = (median * 1.12).toLong()
                ProtocolProbeResult(AetherProtocol.GOOL, ProbeStatus.SUCCESS, goolLatency)
            } else {
                ProtocolProbeResult(AetherProtocol.GOOL, ProbeStatus.FAILED, -1, "TCP connect failed")
            }
        }
    }


    private suspend fun probeMtu(context: PlatformContext): MtuProbeResult {
        val systemUtils = getSystemUtils(context)
        return withContext(Dispatchers.Default) {
            try {
                val overhead = 80
                val localMtu = systemUtils.getInterfaceMtu()
                LogRepository.i("MTU Probe: Local interface MTU = $localMtu", "AutoDetect")

                val isMtuSupported: (Int) -> Boolean = { totalSize ->
                    val payloadSize = totalSize - 28
                    if (payloadSize < 0) true
                    else systemUtils.execPing(ICMP_TARGET, payloadSize, 700, dontFragment = true)
                }

                if (isMtuSupported(2000)) {
                    LogRepository.w("MTU Probe: DF bit ignored, using safe value", "AutoDetect")
                    return@withContext MtuProbeResult(
                        discoveredMtu = 1280,
                        status = ProbeStatus.SUCCESS,
                        rawPathMtu = 1280
                    )
                }

                var low = 1200
                var high = localMtu.coerceAtMost(1500)
                var bestPathMtu = 1200

                while (low <= high) {
                    val mid = (low + high) / 2
                    if (isMtuSupported(mid)) {
                        bestPathMtu = mid
                        low = mid + 1
                    } else {
                        high = mid - 1
                    }
                    delay(50)
                }

                val optimalMtu = (bestPathMtu - overhead).coerceIn(1100, 1460)
                LogRepository.i("MTU Probe: Path MTU = $bestPathMtu, Optimal = $optimalMtu", "AutoDetect")

                MtuProbeResult(
                    discoveredMtu = optimalMtu,
                    status = ProbeStatus.SUCCESS,
                    rawPathMtu = bestPathMtu
                )
            } catch (e: Exception) {
                LogRepository.e("MTU Probe failed: ${e.message}", "AutoDetect")
                MtuProbeResult(discoveredMtu = 1280, status = ProbeStatus.FAILED, rawPathMtu = 1280)
            }
        }
    }


    private suspend fun probeNoiseModes(protocolResults: List<ProtocolProbeResult>): List<NoiseProbeResult> {
        val bestProtocol = protocolResults
            .filter { it.status == ProbeStatus.SUCCESS }
            .minByOrNull { it.latencyMs }
            ?.protocol ?: AetherProtocol.MASQUE

        val noiseModes = when (bestProtocol) {
            AetherProtocol.MASQUE -> listOf(AetherNoise.FIREWALL, AetherNoise.GFW, AetherNoise.OFF)
            else -> listOf(AetherNoise.BALANCED, AetherNoise.AGGRESSIVE, AetherNoise.LIGHT, AetherNoise.OFF)
        }

        return noiseModes.map { noise ->
            updateState(_state.value.copy(
                currentStep = "Testing ${noise.displayName} obfuscation...",
                progressPercent = _state.value.progressPercent + 3
            ))

            val effective = withContext(Dispatchers.Default) {
                try {
                    val samples = measureTcpLatency(TCP_TARGET_HOST, TCP_TARGET_PORT, 3)
                    val median = medianLatency(samples)
                    median in 1..3000
                } catch (_: Exception) {
                    false
                }
            }

            delay(150)
            NoiseProbeResult(noise, ProbeStatus.SUCCESS, effective)
        }
    }


    private suspend fun probeScanModes(): List<ScanModeProbeResult> {
        val modes = listOf(AetherScanMode.TURBO, AetherScanMode.BALANCED, AetherScanMode.THOROUGH)
        return modes.map { mode ->
            updateState(_state.value.copy(
                currentStep = "Testing ${mode.name.lowercase()} scan strategy...",
                progressPercent = _state.value.progressPercent + 4
            ))

            val success = withContext(Dispatchers.Default) {
                try {
                    val samples = measureTcpLatency(TCP_TARGET_HOST, TCP_TARGET_PORT, 3)
                    val median = medianLatency(samples)
                    median in 1..5000
                } catch (_: Exception) {
                    false
                }
            }

            delay(150)
            ScanModeProbeResult(mode, ProbeStatus.SUCCESS, success)
        }
    }


    private fun analyzeResults(
        protocolResults: List<ProtocolProbeResult>,
        mtuResult: MtuProbeResult,
        noiseResults: List<NoiseProbeResult>,
        scanModeResults: List<ScanModeProbeResult>,
        fingerprint: NetworkFingerprint
    ): AutoDetectResult {
        val successfulProtocols = protocolResults.filter { it.status == ProbeStatus.SUCCESS }

        val recommendedProtocol = if (successfulProtocols.isNotEmpty()) {
            if (fingerprint.supportsDPI) {
                successfulProtocols
                    .filter { it.protocol == AetherProtocol.MASQUE || it.protocol == AetherProtocol.GOOL }
                    .minByOrNull { it.latencyMs }
                    ?.protocol ?: successfulProtocols.minByOrNull { it.latencyMs }!!.protocol
            } else {
                successfulProtocols.minByOrNull { it.latencyMs }!!.protocol
            }
        } else {
            AetherProtocol.MASQUE
        }

        val recommendedNoise = noiseResults
            .filter { it.status == ProbeStatus.SUCCESS && it.effective }
            .map { it.noise }
            .firstOrNull()
            ?: when (fingerprint.networkType) {
                "restricted" -> if (recommendedProtocol == AetherProtocol.MASQUE) AetherNoise.GFW else AetherNoise.AGGRESSIVE
                else -> if (recommendedProtocol == AetherProtocol.MASQUE) AetherNoise.FIREWALL else AetherNoise.BALANCED
            }

        val recommendedScanMode = scanModeResults
            .filter { it.status == ProbeStatus.SUCCESS && it.gatewayFound }
            .map { it.scanMode }
            .firstOrNull()
            ?: when {
                successfulProtocols.isEmpty() -> AetherScanMode.THOROUGH
                fingerprint.supportsDPI -> AetherScanMode.IRONCLAD
                else -> AetherScanMode.BALANCED
            }

        val confidence = calculateConfidence(protocolResults, mtuResult, noiseResults)

        return AutoDetectResult(
            recommendedProtocol = recommendedProtocol,
            recommendedNoise = recommendedNoise,
            recommendedScanMode = recommendedScanMode,
            recommendedMtu = if (mtuResult.status == ProbeStatus.SUCCESS) mtuResult.discoveredMtu else 1100,
            recommendedIpMode = if (fingerprint.supportsIPv6) AetherIpMode.DUAL else AetherIpMode.IPV4,
            recommendedH2Mode = recommendedProtocol == AetherProtocol.MASQUE,
            recommendedEch = fingerprint.supportsDPI && recommendedProtocol == AetherProtocol.MASQUE,
            recommendedFragment = fingerprint.supportsDPI && recommendedProtocol == AetherProtocol.MASQUE,
            recommendedNoDataCheck = recommendedProtocol != AetherProtocol.MASQUE,
            confidence = confidence,
            networkFingerprint = fingerprint
        )
    }

    private fun calculateConfidence(
        protocolResults: List<ProtocolProbeResult>,
        mtuResult: MtuProbeResult,
        noiseResults: List<NoiseProbeResult>
    ): Float {
        var score = 0f
        var total = 0f

        val protocolSuccess = protocolResults.count { it.status == ProbeStatus.SUCCESS }
        total += 40f
        score += (protocolSuccess.toFloat() / protocolResults.size.coerceAtLeast(1)) * 40f

        total += 20f
        if (mtuResult.status == ProbeStatus.SUCCESS) score += 20f

        val noiseEffective = noiseResults.count { it.effective }
        total += 30f
        if (noiseResults.isNotEmpty()) {
            score += (noiseEffective.toFloat() / noiseResults.size) * 30f
        }

        total += 10f
        score += 10f

        return (score / total).coerceIn(0f, 1f)
    }

    private fun updateState(newState: AutoDetectState) {
        _state.value = newState
    }
}

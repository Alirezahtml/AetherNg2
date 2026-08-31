package io.github.immaghzbad.aetherst.shared.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.getAppInfoProvider
import io.github.immaghzbad.aetherst.platform.getSettings
import io.github.immaghzbad.aetherst.platform.getSystemUtils
import io.github.immaghzbad.aetherst.platform.getVpnController
import io.github.immaghzbad.aetherst.platform.isDesktop
import io.github.immaghzbad.aetherst.shared.core.ConnectionController
import io.github.immaghzbad.aetherst.shared.data.AetherConfigRepository
import io.github.immaghzbad.aetherst.shared.data.IpInfo
import io.github.immaghzbad.aetherst.shared.data.IpInfoRepository
import io.github.immaghzbad.aetherst.shared.data.LogRepository
import io.github.immaghzbad.aetherst.shared.data.PingRepository
import io.github.immaghzbad.aetherst.shared.data.PingState
import io.github.immaghzbad.aetherst.shared.model.AetherConfig
import io.github.immaghzbad.aetherst.shared.model.AetherProtocol
import io.github.immaghzbad.aetherst.shared.model.AppInfo
import io.github.immaghzbad.aetherst.shared.model.ConnectionMode
import io.github.immaghzbad.aetherst.shared.model.ConnectionStatus
import io.github.immaghzbad.aetherst.shared.model.LogEntry
import io.github.immaghzbad.aetherst.shared.model.RoutingMode
import io.github.immaghzbad.aetherst.shared.model.RoutingRule
import io.github.immaghzbad.aetherst.shared.model.TunnelEngine
import io.github.immaghzbad.aetherst.shared.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AetherViewModel(platformContext: PlatformContext) : ViewModel() {
    private val repository = AetherConfigRepository.getInstance(getSettings(platformContext))
    private val vpnController = getVpnController(platformContext)
    private val systemUtils = getSystemUtils(platformContext)
    private val appInfoProvider = getAppInfoProvider(platformContext)

    val config: StateFlow<AetherConfig> = repository.config
    val isOnboardingComplete: StateFlow<Boolean> = repository.isOnboardingComplete
    val connectionStatus: StateFlow<ConnectionStatus> = ConnectionController.status
    val elapsedSeconds: StateFlow<Long> = ConnectionController.elapsedSeconds
    val sessionTraffic = ConnectionController.sessionTraffic
    val isWaitingForLoginCode = ConnectionController.isWaitingForCode
    val logs: StateFlow<List<LogEntry>> = LogRepository.logs
    val ipInfo: StateFlow<IpInfo> = IpInfoRepository.ipInfo
    val pingState: StateFlow<PingState> = PingRepository.pingState

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isBatteryOptimized = MutableStateFlow(value = false)
    val isBatteryOptimized: StateFlow<Boolean> = _isBatteryOptimized.asStateFlow()

    val appVersion: String = systemUtils.getAppVersion()

    private val _importConflictRules = MutableStateFlow<List<RoutingRule>?>(null)
    val importConflictRules: StateFlow<List<RoutingRule>?> = _importConflictRules.asStateFlow()

    private val _importErrorMessage = MutableStateFlow<String?>(null)
    val importErrorMessage: StateFlow<String?> = _importErrorMessage.asStateFlow()

    private val _scrollToZeroTrust = MutableStateFlow(false)
    val scrollToZeroTrust: StateFlow<Boolean> = _scrollToZeroTrust.asStateFlow()

    private val _isOptimizingMtu = MutableStateFlow(false)
    val isOptimizingMtu: StateFlow<Boolean> = _isOptimizingMtu.asStateFlow()

    private val _crashLog = MutableStateFlow<String?>(null)
    val crashLog: StateFlow<String?> = _crashLog.asStateFlow()

    data class ToastState(val message: String, val isError: Boolean = false)
    private val _toastState = MutableStateFlow<ToastState?>(null)
    val toastState: StateFlow<ToastState?> = _toastState.asStateFlow()

    init {
        val settings = getSettings(platformContext)
        LogRepository.initialize(settings)
        io.github.immaghzbad.aetherst.shared.data.SpeedTestRepository.initialize(settings)
        LogRepository.i("Initializing Aether Ng Multiplatform UI...", "AetherSystem")
        ConnectionController.getInstance(platformContext)
        observeConnectionStatus()
        checkBatteryOptimizationStatus()
        checkLastCrash()
        loadInstalledApps()
        if (isDesktop) checkForUpdates()
    }

    private fun checkLastCrash() {
        viewModelScope.launch {
            val log = withContext(Dispatchers.Default) {
                systemUtils.readLastCrashLog()
            }
            _crashLog.value = log
        }
    }

    fun toggleVpn(onPermissionRequired: () -> Unit) {
        val currentState = connectionStatus.value
        if (currentState == ConnectionStatus.STOPPING) return

        val cfg = config.value
        if (cfg.protocol == AetherProtocol.ZERO_TRUST) {
            if (cfg.teamName.isEmpty() || cfg.accessEmail.isEmpty()) {
                showToast("Please complete Zero Trust settings", true)
                _scrollToZeroTrust.value = true
                return
            }
        }

        try {
            if ((currentState == ConnectionStatus.STOPPED) || (currentState == ConnectionStatus.ERROR)) {
                if (cfg.connectionMode == ConnectionMode.TUNNEL) {
                    if (vpnController.prepareVpn(onPermissionRequired)) {
                        ConnectionController.markStatus(ConnectionStatus.STARTING)
                        vpnController.startVpn()
                    }
                } else {
                    ConnectionController.markStatus(ConnectionStatus.STARTING)
                    vpnController.startProxy()
                }
            } else {
                ConnectionController.markStatus(ConnectionStatus.STOPPING)
                if (cfg.connectionMode == ConnectionMode.TUNNEL) {
                    vpnController.stopVpn()
                } else {
                    vpnController.stopProxy()
                }
            }
        } catch (exception: Exception) {
            LogRepository.e("[UI] Connection toggle failed: ${exception.message}")
            ConnectionController.markStatus(ConnectionStatus.ERROR)
        }
    }

    fun prepareVpn(onPermissionRequired: () -> Unit): Boolean {
        return vpnController.prepareVpn(onPermissionRequired)
    }

    fun updateConfig(newConfig: AetherConfig) {
        val oldConfig = config.value
        repository.updateConfig(newConfig)
        
        val needsRestart = oldConfig.connectionMode != newConfig.connectionMode ||
                oldConfig.tunnelAllApps != newConfig.tunnelAllApps ||
                oldConfig.protocol != newConfig.protocol ||
                oldConfig.ipMode != newConfig.ipMode ||
                oldConfig.mtu != newConfig.mtu ||
                oldConfig.tunnelEngine != newConfig.tunnelEngine ||
                oldConfig.ipv6Leak != newConfig.ipv6Leak ||
                oldConfig.socksPort != newConfig.socksPort ||
                oldConfig.socksHost != newConfig.socksHost

        if (needsRestart) {
            restartConnection()
        }
    }

    private fun restartConnection() {
        val state = connectionStatus.value
        if (state == ConnectionStatus.STOPPED || state == ConnectionStatus.ERROR || state == ConnectionStatus.STOPPING) return

        viewModelScope.launch {
            val oldCfg = config.value
            if (oldCfg.connectionMode == ConnectionMode.TUNNEL) vpnController.stopVpn() else vpnController.stopProxy()

            withTimeoutOrNull(5.seconds) {
                connectionStatus.first { it == ConnectionStatus.STOPPED || it == ConnectionStatus.ERROR }
                true
            }

            delay(500.milliseconds)

            val newCfg = config.value
            if (newCfg.connectionMode == ConnectionMode.TUNNEL) {
                if (vpnController.prepareVpn {}) {
                    vpnController.startVpn()
                }
            } else {
                vpnController.startProxy()
            }
        }
    }

    fun updateTunnelEngine(engine: TunnelEngine) {
        val current = config.value
        if (current.tunnelEngine == engine) return
        updateConfig(current.copy(tunnelEngine = engine))
        restartConnection()
    }

    fun updateAppSplitTunnelingMode(packageName: String, modeOrdinal: Int) {
        val current = config.value
        val excluded = current.excludedPackages.toMutableSet()
        val blocked = current.blockedPackages.toMutableSet()

        excluded.remove(packageName)
        blocked.remove(packageName)

        when (modeOrdinal) {
            1 -> excluded.add(packageName)
            2 -> blocked.add(packageName)
        }

        updateConfig(current.copy(excludedPackages = excluded, blockedPackages = blocked))
        restartConnection()
    }

    fun addRoutingRule(pattern: String, mode: RoutingMode) {
        val current = config.value
        if (current.routingRules.any { it.pattern == pattern }) return

        val newList = current.routingRules + RoutingRule(pattern, mode)
        LogRepository.i("Routing rule added: $pattern ($mode)")
        updateConfig(current.copy(routingRules = newList))
        restartConnection()
    }

    fun removeRoutingRule(pattern: String) {
        val current = config.value
        val newList = current.routingRules.filter { it.pattern != pattern }
        if (newList.size == current.routingRules.size) return

        LogRepository.i("Routing rule removed: $pattern")
        updateConfig(current.copy(routingRules = newList))
        restartConnection()
    }

    fun updateRoutingRuleMode(pattern: String, mode: RoutingMode) {
        val current = config.value
        val newList = current.routingRules.map {
            if (it.pattern == pattern) it.copy(mode = mode) else it
        }
        if (newList == current.routingRules) return

        LogRepository.i("Routing rule updated: $pattern -> $mode")
        updateConfig(current.copy(routingRules = newList))
        restartConnection()
    }

    fun clearAllRoutingRules() {
        val current = config.value
        if (current.routingRules.isEmpty()) return
        LogRepository.i("All routing rules cleared")
        updateConfig(current.copy(routingRules = emptyList()))
        restartConnection()
    }

    fun resetAllSettings() {
        repository.resetToDefaults()
        restartConnection()
    }

    fun optimizeMtu() {
        if (_isOptimizingMtu.value) return
        _isOptimizingMtu.value = true
        
        viewModelScope.launch {
            showToast("Starting precision MTU discovery...")
            
            var probeResult: Int? = null
            var dfIgnored = false

            withContext(Dispatchers.Default) {
                try {
                    val currentProtocol = config.value.protocol
                    val overhead = when (currentProtocol) {
                        AetherProtocol.WG, AetherProtocol.GOOL -> 80
                        AetherProtocol.MASQUE -> 60
                        else -> 40
                    }

                    val localMtu = systemUtils.getInterfaceMtu()
                    LogRepository.i("Step 1: Local interface reports MTU: $localMtu", "MTUProbe")

                    val isMtuSupported: (Int) -> Boolean = { totalSize ->
                        val payloadSize = totalSize - 28
                        if (payloadSize < 0) true
                        else {
                            val targets = listOf("1.1.1.1", "8.8.8.8", "9.9.9.9")
                            targets.any { target ->
                                systemUtils.execPing(target, payloadSize, 700, dontFragment = true)
                            }
                        }
                    }

                    LogRepository.i("Step 2: Validating network DF constraint...", "MTUProbe")
                    if (isMtuSupported(2000)) {
                        LogRepository.w("WARNING: Network is ignoring DF bit. Results may be inaccurate.", "MTUProbe")
                        dfIgnored = true
                        probeResult = 1280 + overhead
                        return@withContext
                    }

                    LogRepository.i("Step 3: Probing path MTU via binary search...", "MTUProbe")
                    var low = 1200
                    var high = localMtu.coerceAtMost(1500)
                    var bestPathMtu = 1200

                    while (low <= high) {
                        val mid = (low + high) / 2
                        if (isMtuSupported(mid)) {
                            bestPathMtu = mid
                            low = mid + 1
                            LogRepository.d("Probe success at $mid bytes", "MTUProbe")
                        } else {
                            high = mid - 1
                            LogRepository.d("Probe failed at $mid bytes", "MTUProbe")
                        }
                        delay(50.milliseconds)
                    }

                    LogRepository.i("Final Discovered Path MTU: $bestPathMtu", "MTUProbe")
                    probeResult = (bestPathMtu - overhead).coerceIn(1100, 1460)
                } catch (e: Exception) {
                    LogRepository.e("MTU Optimization failed: ${e.message}", "MTUProbe")
                }
            }

            _isOptimizingMtu.value = false

            probeResult?.let { finalResult ->
                val current = config.value
                val message = when {
                    dfIgnored -> "DF bit ignored by ISP. Used safe MTU: $finalResult"
                    finalResult >= 1420 -> "High-speed path detected. Applied MTU: $finalResult"
                    else -> "Optimal MTU for your path: $finalResult"
                }
                showToast(message)
                updateConfig(current.copy(mtu = finalResult))
            } ?: run {
                showToast("MTU probe failed, using safe default", true)
                updateConfig(config.value.copy(mtu = 1280))
            }
        }
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val info = withContext(Dispatchers.Default) {
                    val request = Request.Builder()
                        .url("https://raw.githubusercontent.com/immaghzbad/AetherST/refs/heads/main/updatewin.json")
                        .build()

                    io.github.immaghzbad.aetherst.shared.core.NetworkClient.instance.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val jsonStr = response.body?.string() ?: return@withContext null
                            val element = Json.parseToJsonElement(jsonStr).jsonObject
                            UpdateInfo(
                                version = element["version"]?.jsonPrimitive?.content ?: "",
                                versionCode = element["version_code"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1,
                                isBeta = element["is_beta"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false,
                                changelog = element["changelog"]?.jsonPrimitive?.content ?: "",
                                releaseUrl = element["release_url"]?.jsonPrimitive?.content ?: ""
                            )
                        } else null
                    }
                }
                if (info != null && info.version != appVersion) {
                    _updateInfo.value = info
                }
            } catch (e: Exception) {
                LogRepository.w("Update check failed: ${e.message}")
            }
        }
    }

    fun clearCrashLog() {
        _crashLog.value = null
        viewModelScope.launch(Dispatchers.Default) {
            systemUtils.clearCrashLog()
        }
    }
    fun clearImportError() { _importErrorMessage.value = null }
    fun onZeroTrustScrolled() { _scrollToZeroTrust.value = false }
    fun dismissUpdate() { _updateInfo.value = null }
    fun cancelImport() { _importConflictRules.value = null }
    fun applyPreset(presetId: String) { repository.applyPreset(presetId) }

    fun applyAutoDetectResult(result: io.github.immaghzbad.aetherst.shared.model.AutoDetectResult) {
        val oldConfig = config.value
        val newConfig = oldConfig.copy(
            presetId = "custom",
            protocol = result.recommendedProtocol,
            noise = result.recommendedNoise,
            scanMode = result.recommendedScanMode,
            mtu = if (result.recommendedMtu > 0) result.recommendedMtu else oldConfig.mtu,
            ipMode = result.recommendedIpMode,
            h2Mode = result.recommendedH2Mode,
            echEnabled = result.recommendedEch,
            h2Fragment = result.recommendedFragment,
            fragmentSize = "16-32",
            fragmentDelay = "2-10",
            noDataCheck = result.recommendedNoDataCheck
        )
        repository.applyDetectedConfig(newConfig)

        val needsRestart = oldConfig.connectionMode != newConfig.connectionMode ||
                oldConfig.tunnelAllApps != newConfig.tunnelAllApps ||
                oldConfig.protocol != newConfig.protocol ||
                oldConfig.ipMode != newConfig.ipMode ||
                oldConfig.mtu != newConfig.mtu ||
                oldConfig.tunnelEngine != newConfig.tunnelEngine ||
                oldConfig.ipv6Leak != newConfig.ipv6Leak ||
                oldConfig.socksPort != newConfig.socksPort ||
                oldConfig.socksHost != newConfig.socksHost

        if (needsRestart) {
            restartConnection()
        }
        showToast("Auto-Detect configuration applied!")
    }

    private var toastJob: Job? = null

    fun showToast(message: String, isError: Boolean = false) {
        toastJob?.cancel()
        _toastState.value = ToastState(message, isError)
        toastJob = viewModelScope.launch {
            delay(5000.milliseconds)
            _toastState.value = null
        }
    }

    fun submitLoginCode(code: String) {
        vpnController.submitLoginCode(code)
    }

    fun refreshIpInfo() {
        viewModelScope.launch {
            val state = connectionStatus.value
            if (state == ConnectionStatus.RUNNING) {
                val cfg = config.value
                IpInfoRepository.fetchIpInfo(cfg.socksHost, cfg.socksPort.toIntOrNull() ?: 1819, useProxy = true)
            } else {
                IpInfoRepository.fetchIpInfo(useProxy = false)
            }
        }
    }

    fun refreshPing() {
        viewModelScope.launch {
            val state = connectionStatus.value
            if (state == ConnectionStatus.RUNNING) {
                val cfg = config.value
                PingRepository.runPing(cfg.socksHost, cfg.socksPort.toIntOrNull() ?: 1819, useProxy = true)
            } else {
                PingRepository.reset()
            }
        }
    }

    fun checkBatteryOptimizationStatus() {
        viewModelScope.launch(Dispatchers.Default) {
            val optimized = runCatching { systemUtils.isBatteryOptimized() }.getOrDefault(true)
            _isBatteryOptimized.value = optimized
        }
    }

    fun requestBatteryOptimization() {
        systemUtils.requestBatteryOptimization()
    }

    fun requestNotificationPermission() {
        systemUtils.requestNotificationPermission()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.Default) {
            delay(1500.milliseconds)
            val apps = appInfoProvider.getInstalledApps()
            _installedApps.value = apps
        }
    }

    private fun observeConnectionStatus() {
        viewModelScope.launch {
            connectionStatus.collect { state ->
                when (state) {
                    ConnectionStatus.RUNNING -> {
                        val cfg = config.value
                        val host = cfg.socksHost
                        val port = cfg.socksPort.toIntOrNull() ?: 1819
                        viewModelScope.launch { IpInfoRepository.fetchIpInfo(host, port, useProxy = true) }
                        viewModelScope.launch { PingRepository.runPing(host, port, useProxy = true) }
                    }
                    ConnectionStatus.STOPPED -> {
                        viewModelScope.launch { IpInfoRepository.fetchIpInfo(useProxy = false) }
                        PingRepository.reset()
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearLogs() { LogRepository.clear() }
    
    fun copyToClipboard(text: String) {
        systemUtils.copyToClipboard(text)
        showToast("Copied to clipboard")
    }

    fun copyLogs() {
        val allLogs = logs.value.joinToString("\n") { "[${it.timestamp}] [${it.level.name}] [${it.tag}] ${it.message}" }
        copyToClipboard(allLogs)
    }

    fun shareLogs() {
        val allLogs = logs.value.joinToString("\n") { "[${it.timestamp}] [${it.level.name}] [${it.tag}] ${it.message}" }
        systemUtils.shareFile("AetherNg_Logs.txt", allLogs)
    }

    fun cleanRoutingPattern(input: String): String {
        var pattern = input.trim()
        if (pattern.startsWith("http://", ignoreCase = true)) pattern = pattern.substring(7)
        if (pattern.startsWith("https://", ignoreCase = true)) pattern = pattern.substring(8)
        while (pattern.endsWith("/")) pattern = pattern.dropLast(1)
        return pattern
    }

    fun isValidRoutingPattern(pattern: String): Boolean {
        if (pattern.isEmpty()) return false
        if (pattern.startsWith("regexp:")) return true
        val regex = Regex("^[a-zA-Z0-9.*:\\-/]+$")
        return regex.matches(pattern)
    }

    fun resolveConflict(rules: List<RoutingRule>, replace: Boolean) {
        _importConflictRules.value = null
        applyImport(rules, merge = !replace)
    }

    private fun applyImport(newRules: List<RoutingRule>, merge: Boolean) {
        val current = config.value
        val finalRules = if (merge) {
            val existingPatterns = current.routingRules.mapTo(mutableSetOf()) { it.pattern.lowercase() }
            current.routingRules + newRules.filter { it.pattern.lowercase() !in existingPatterns }
        } else {
            val newPatterns = newRules.mapTo(mutableSetOf()) { it.pattern.lowercase() }
            current.routingRules.filter { it.pattern.lowercase() !in newPatterns } + newRules
        }
        updateConfig(current.copy(routingRules = finalRules))
        restartConnection()
    }

    
    fun exportFullBackup() {
        val json = repository.getFullConfigJson()
        systemUtils.exportFile("AetherNg_Backup.astf", json) { success ->
            if (success) {
                showToast("Backup exported successfully", false)
            } else {
                showToast("Failed to export backup", true)
            }
        }
    }

    fun importFullBackup() {
        systemUtils.importFile { content ->
            if (content != null) {
                if (repository.restoreFullConfig(content)) {
                    showToast("Configuration restored", false)
                    restartConnection()
                } else {
                    showToast("Invalid backup file", true)
                }
            }
        }
    }

    fun exportRoutingRules() {
        try {
            val json = Json.encodeToString(config.value.routingRules)
            systemUtils.exportFile("AetherNg_Rules.astf", json) { success ->
                if (success) {
                    showToast("Routing rules exported", false)
                } else {
                    showToast("Failed to export rules", true)
                }
            }
        } catch (e: Exception) {
            showToast("Export error: ${e.message}", true)
        }
    }

    fun importRoutingRules() {
        systemUtils.importFile { content ->
            if (content != null) {
                try {
                    val rules = Json.decodeFromString<List<RoutingRule>>(content)
                    if (rules.isEmpty()) {
                        showToast("No rules found in file", true)
                        return@importFile
                    }
                    _importConflictRules.value = rules
                } catch (_: Exception) {
                    showToast("Invalid rules file", true)
                }
            }
        }
    }

    fun importInternalRoutingRules(assetName: String) {
        val content = systemUtils.readInternalAsset(assetName)
        if (content == null) {
            showToast("Failed to load internal rules", true)
            return
        }

        try {
            val rules = if (assetName.endsWith(".astb")) {
                val lines = content.lines().filter { it.isNotBlank() }
                val parsed = mutableListOf<RoutingRule>()
                var i = 0
                while (i + 1 < lines.size) {
                    val modeStr = lines[i+1].trim().removePrefix("-").uppercase()
                    val mode = when (modeStr) {
                        "TUNNEL" -> RoutingMode.TUNNEL
                        "DIRECT" -> RoutingMode.DIRECT
                        "BLOCK" -> RoutingMode.BLOCK
                        else -> RoutingMode.TUNNEL
                    }
                    parsed.add(RoutingRule(lines[i].trim(), mode))
                    i += 2
                }
                parsed
            } else {
                Json.decodeFromString<List<RoutingRule>>(content)
            }

            if (rules.isEmpty()) {
                showToast("No rules found in asset", true)
                return
            }
            _importConflictRules.value = rules
        } catch (e: Exception) {
            showToast("Failed to parse internal rules", true)
            LogRepository.e("Internal import error: ${e.message}")
        }
    }
}

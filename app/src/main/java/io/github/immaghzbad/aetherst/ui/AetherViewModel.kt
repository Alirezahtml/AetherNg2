package io.github.immaghzbad.aetherst.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import java.io.File
import android.net.VpnService
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.immaghzbad.aetherst.BuildConfig
import io.github.immaghzbad.aetherst.shared.data.*
import io.github.immaghzbad.aetherst.shared.model.*
import io.github.immaghzbad.aetherst.core.ConnectionController
import io.github.immaghzbad.aetherst.core.NetworkClient
import io.github.immaghzbad.aetherst.service.AetherProxyService
import io.github.immaghzbad.aetherst.service.AetherVpnService
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.getSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

class AetherViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val repository = AetherConfigRepository.getInstance(getSettings(PlatformContext(appContext)))
    private val lastToggleAt = AtomicLong(0)

    val config: StateFlow<AetherConfig> = repository.config
    val isOnboardingComplete: StateFlow<Boolean> = repository.isOnboardingComplete
    private val _connectionStatus = MutableStateFlow(ConnectionController.lastKnownStatus)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    val elapsedSeconds: StateFlow<Long> = ConnectionController.elapsedSeconds
    private val _sessionTraffic = MutableStateFlow(SessionTraffic())
    val sessionTraffic: StateFlow<SessionTraffic> = _sessionTraffic.asStateFlow()

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

    private val _importConflictRules = MutableStateFlow<List<RoutingRule>?>(null)
    val importConflictRules: StateFlow<List<RoutingRule>?> = _importConflictRules.asStateFlow()

    private val _importErrorMessage = MutableStateFlow<String?>(null)
    val importErrorMessage: StateFlow<String?> = _importErrorMessage.asStateFlow()

    private val _scrollToZeroTrust = MutableStateFlow(value = false)
    val scrollToZeroTrust: StateFlow<Boolean> = _scrollToZeroTrust.asStateFlow()

    data class ToastState(val message: String, val isError: Boolean = false)
    private val _toastState = MutableStateFlow<ToastState?>(null)
    val toastState: StateFlow<ToastState?> = _toastState.asStateFlow()

    private val _isOptimizingMtu = MutableStateFlow(value = false)
    val isOptimizingMtu: StateFlow<Boolean> = _isOptimizingMtu.asStateFlow()

    private val _crashLog = MutableStateFlow<String?>(null)
    val crashLog: StateFlow<String?> = _crashLog.asStateFlow()

    init {
        LogRepository.initialize(getSettings(PlatformContext(appContext)))
        ConnectionController.getInstance(appContext)
        
        viewModelScope.launch {
            ConnectionController.status.collect { status ->
                _connectionStatus.value = status
            }
        }

        viewModelScope.launch {
            ConnectionController.sessionTraffic.collect { traffic ->
                _sessionTraffic.value = traffic
            }
        }
        
        val statusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val statusName = intent?.getStringExtra("status")
                if (statusName != null) {
                    try {
                        val newStatus = ConnectionStatus.valueOf(statusName)
                        _connectionStatus.value = newStatus
                    } catch (_: Exception) {}
                } else {
                    _connectionStatus.value = ConnectionController.lastKnownStatus
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(statusReceiver, IntentFilter(ConnectionController.ACTION_STATUS_CHANGED), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(statusReceiver, IntentFilter(ConnectionController.ACTION_STATUS_CHANGED))
        }

        loadInstalledApps()
        checkForUpdates()
        observeConnectionStatus()
        checkBatteryOptimizationStatus()
        checkLastCrash()
    }

    private fun checkLastCrash() {
        viewModelScope.launch {
            val file = File(appContext.cacheDir, "last_crash.log")
            if (file.exists()) {
                val log = file.readText()
                if (log.isNotEmpty()) {
                    _crashLog.value = log
                }
            }
        }
    }

    fun clearCrashLog() {
        val file = File(appContext.cacheDir, "last_crash.log")
        if (file.exists()) file.delete()
        _crashLog.value = null
    }

    fun toggleVpn(context: Context, onPermissionRequired: () -> Unit) {
        val now = SystemClock.elapsedRealtime()

        while (true) {
            val previous = lastToggleAt.get()
            if ((now - previous) < 450L) return
            if (lastToggleAt.compareAndSet(previous, now)) break
        }

        val config = repository.config.value
        if (config.protocol == AetherProtocol.ZERO_TRUST) {
            if (config.teamName.isEmpty() || config.accessEmail.isEmpty()) {
                showToast("Please complete Zero Trust settings", isError = true)
                _scrollToZeroTrust.value = true
                return
            }
        }

        val currentState = connectionStatus.value
        if (currentState == ConnectionStatus.STOPPING) return

        try {
            if ((currentState == ConnectionStatus.STOPPED) || (currentState == ConnectionStatus.ERROR)) {
                if (config.connectionMode == ConnectionMode.TUNNEL) {
                    val prepareIntent = VpnService.prepare(context)
                    if (prepareIntent != null) {
                        onPermissionRequired()
                        return
                    }
                    AetherVpnService.startVpn(context)
                } else {
                    AetherProxyService.startProxy(context)
                }
            } else {
                if (config.connectionMode == ConnectionMode.TUNNEL) {
                    AetherVpnService.stopVpn(context)
                } else {
                    AetherProxyService.stopProxy(context)
                }
            }
        } catch (exception: Exception) {
            LogRepository.e("[UI] Connection toggle failed: ${exception.localizedMessage}")
        }
    }

    fun updateConfig(newConfig: AetherConfig) {
        val oldConfig = repository.config.value
        repository.updateConfig(newConfig)
        
        val needsRestart = (oldConfig.connectionMode != newConfig.connectionMode) ||
                (oldConfig.tunnelAllApps != newConfig.tunnelAllApps) ||
                (oldConfig.protocol != newConfig.protocol) ||
                (oldConfig.ipMode != newConfig.ipMode) ||
                (oldConfig.mtu != newConfig.mtu) ||
                (oldConfig.tunnelEngine != newConfig.tunnelEngine) ||
                (oldConfig.ipv6Leak != newConfig.ipv6Leak) ||
                (oldConfig.socksPort != newConfig.socksPort) ||
                (oldConfig.socksHost != newConfig.socksHost) ||
                (oldConfig.shareHotspot != newConfig.shareHotspot) ||
                (oldConfig.httpProxyEnabled != newConfig.httpProxyEnabled)

        if (needsRestart) {
            if (oldConfig.connectionMode != newConfig.connectionMode) {
                switchMode()
            } else {
                restartVpnIfActive()
            }
        }
    }

    private fun switchMode() {
        val state = connectionStatus.value
        if (state == ConnectionStatus.STOPPED || state == ConnectionStatus.ERROR || state == ConnectionStatus.STOPPING) return

        restartVpnIfActive()
    }

    fun updateTunnelEngine(engine: TunnelEngine) {
        val current = config.value
        if (current.tunnelEngine == engine) return

        updateConfig(current.copy(tunnelEngine = engine))
        restartVpnIfActive()
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

        val newExcluded = excluded.toSet()
        val newBlocked = blocked.toSet()

        if ((newExcluded == current.excludedPackages) && (newBlocked == current.blockedPackages)) return

        updateConfig(
            current.copy(
                excludedPackages = newExcluded,
                blockedPackages = newBlocked,
            )
        )

        restartVpnIfActive()
    }

    fun addRoutingRule(pattern: String, mode: RoutingMode) {
        val current = config.value
        if (current.routingRules.any { it.pattern == pattern }) return

        val newList = current.routingRules + RoutingRule(pattern, mode)
        LogRepository.i("Routing rule added: $pattern ($mode)")
        updateConfig(current.copy(routingRules = newList))
        restartVpnIfActive()
    }

    fun removeRoutingRule(pattern: String) {
        val current = config.value
        val newList = current.routingRules.filter { it.pattern != pattern }
        if (newList.size == current.routingRules.size) return

        LogRepository.i("Routing rule removed: $pattern")
        updateConfig(current.copy(routingRules = newList))
        restartVpnIfActive()
    }

    fun updateRoutingRuleMode(pattern: String, mode: RoutingMode) {
        val current = config.value
        val newList = current.routingRules.map {
            if (it.pattern == pattern) it.copy(mode = mode) else it
        }
        if (newList == current.routingRules) return

        LogRepository.i("Routing rule updated: $pattern -> $mode")
        updateConfig(current.copy(routingRules = newList))
        restartVpnIfActive()
    }

    fun clearAllRoutingRules() {
        val current = config.value
        if (current.routingRules.isEmpty()) return
        LogRepository.i("All routing rules cleared")
        updateConfig(current.copy(routingRules = emptyList()))
        restartVpnIfActive()
    }

    fun resetAllSettings() {
        repository.resetToDefaults()
        restartVpnIfActive()
    }

    fun optimizeMtu() {
        if (_isOptimizingMtu.value) return
        _isOptimizingMtu.value = true
        
        viewModelScope.launch {
            showToast("Probing network for optimal MTU...")
            
            val result = withContext(Dispatchers.IO) {
                try {
                    val overhead = when (config.value.protocol) {
                        AetherProtocol.WG, AetherProtocol.GOOL -> 80
                        AetherProtocol.MASQUE -> 60
                        else -> 40
                    }

                    val isMtuSupported: (Int) -> Boolean = { size ->
                        val payloadSize = size - 28
                        val targets = listOf("1.1.1.1", "8.8.8.8")
                        var reachable = false
                        for (target in targets) {
                            try {
                                val proc = Runtime.getRuntime().exec("ping -c 2 -s $payloadSize -W 1 $target")
                                if (proc.waitFor() == 0) {
                                    reachable = true
                                    break
                                }
                            } catch (_: Exception) {}
                        }
                        reachable
                    }

                    var low = 1280
                    var high = 1500
                    var bestUnderlyingMtu = 1280

                    while (low <= high) {
                        val mid = (low + high) / 2
                        if (isMtuSupported(mid)) {
                            bestUnderlyingMtu = mid
                            low = mid + 1
                        } else {
                            high = mid - 1
                        }
                        delay(50.milliseconds)
                    }

                    (bestUnderlyingMtu - overhead).coerceIn(1100, 1420)
                } catch (_: Exception) {
                    null
                } finally {
                    _isOptimizingMtu.value = false
                }
            }

            if (result != null) {
                val current = config.value
                if (current.mtu == result) {
                    showToast("Current MTU is already optimal ($result)")
                } else {
                    updateConfig(current.copy(mtu = result))
                    showToast("Optimal MTU discovered and applied: $result")
                    restartVpnIfActive()
                }
            } else {
                showToast("MTU probe failed, using safe default", true)
                updateConfig(config.value.copy(mtu = 1280))
            }
        }
    }

    fun clearImportError() {
        _importErrorMessage.value = null
    }

    fun onZeroTrustScrolled() {
        _scrollToZeroTrust.value = false
    }

    fun submitLoginCode(code: String) {
        ConnectionController.getInstance(appContext).submitLoginCode(code)
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

    private fun normalizeImportedRoutingPattern(input: String): String {
        val cleaned = cleanRoutingPattern(input)
        return if (cleaned.startsWith("regexp:", ignoreCase = true)) {
            "regexp:${cleaned.substringAfter(':')}"
        } else {
            cleaned.lowercase(Locale.ROOT)
        }
    }

    private fun isValidImportedRoutingPattern(pattern: String): Boolean {
        if (!isValidRoutingPattern(pattern)) return false
        if (!pattern.startsWith("regexp:", ignoreCase = true)) return true
        val expression = pattern.substringAfter(':')
        return expression.isNotEmpty() && runCatching { Regex(expression) }.isSuccess
    }

    private fun routingPatternKey(pattern: String): String {
        val cleaned = cleanRoutingPattern(pattern)
        return if (cleaned.startsWith("regexp:", ignoreCase = true)) {
            "regexp:${cleaned.substringAfter(':')}"
        } else {
            cleaned.lowercase(Locale.ROOT)
        }
    }

    fun exportRoutingRules(context: Context) {
        viewModelScope.launch {
            val rules = config.value.routingRules
            if (rules.isEmpty()) return@launch

            val content = StringBuilder()
            rules.forEach { rule ->
                content.append(rule.pattern).append("\n")
                content.append("-").append(rule.mode.name.lowercase()).append("\n")
            }

            val timestamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
            val filename = "backup-routing-$timestamp.astb"
            val file = File(context.cacheDir, filename)
            file.writeText(content.toString())

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Aether Ng Routing Backup").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun importRoutingRules(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                var fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst()) cursor.getString(nameIndex) else null
                }
                
                if (fileName == null) {
                    fileName = uri.path?.let { File(it).name } ?: "file"
                }

                if (!fileName.lowercase().endsWith(".astb")) {
                    _importErrorMessage.value = "Invalid file type. Please use .astb"
                    return@launch
                }

                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@launch
                val lines = content.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
                
                if (lines.size % 2 != 0) {
                    _importErrorMessage.value = "Invalid format: Incomplete pairs"
                    return@launch
                }

                val importedRulesByPattern = linkedMapOf<String, RoutingRule>()
                for (i in lines.indices step 2) {
                    val rawPattern = lines[i]
                    val modeLine = lines[i + 1]

                    if (rawPattern.startsWith("-")) {
                        _importErrorMessage.value = "Invalid format: Pattern expected at line ${i + 1}"
                        return@launch
                    }
                    val pattern = normalizeImportedRoutingPattern(rawPattern)
                    if (!isValidImportedRoutingPattern(pattern)) {
                        _importErrorMessage.value = "Invalid pattern at line ${i + 1}"
                        return@launch
                    }
                    if (!modeLine.startsWith("-")) {
                        _importErrorMessage.value = "Invalid format: Mode prefix (-) missing at line ${i + 2}"
                        return@launch
                    }

                    val mode = when (val modeStr = modeLine.substring(1).lowercase()) {
                        "direct" -> RoutingMode.DIRECT
                        "block" -> RoutingMode.BLOCK
                        "tunnel" -> RoutingMode.TUNNEL
                        else -> {
                            _importErrorMessage.value = "Invalid format: Unknown mode '$modeStr'"
                            return@launch
                        }
                    }
                    importedRulesByPattern[routingPatternKey(pattern)] = RoutingRule(pattern, mode)
                }

                val newRules = importedRulesByPattern.values.toList()
                if (newRules.isEmpty()) {
                    _importErrorMessage.value = "Backup file is empty"
                    return@launch
                }

                val currentRules = config.value.routingRules
                val currentPatternKeys = currentRules.mapTo(mutableSetOf()) { routingPatternKey(it.pattern) }
                val hasConflict = newRules.any { routingPatternKey(it.pattern) in currentPatternKeys }

                if (hasConflict) {
                    _importConflictRules.value = newRules
                } else {
                    applyImport(newRules, merge = true)
                }
            } catch (e: Exception) {
                LogRepository.e("Import failed: ${e.localizedMessage}")
                _importErrorMessage.value = "Import failed: Check file content"
            }
        }
    }

    fun exportFullBackup(context: Context) {
        viewModelScope.launch {
            val json = repository.getFullConfigJson()
            val timestamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
            val filename = "full-backup-$timestamp.astf"
            val file = File(context.cacheDir, filename)
            file.writeText(json)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Aether Ng Full Backup").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun importFullBackup(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@launch
                if (repository.restoreFullConfig(json)) {
                    showToast("Full backup restored successfully")
                    restartVpnIfActive()
                } else {
                    showToast("Invalid backup file", true)
                }
            } catch (e: Exception) {
                showToast("Import failed: ${e.localizedMessage}", true)
            }
        }
    }

    fun resolveConflict(rules: List<RoutingRule>, replace: Boolean) {
        _importConflictRules.value = null
        applyImport(rules, merge = !replace)
    }

    fun cancelImport() {
        _importConflictRules.value = null
    }

    private fun applyImport(newRules: List<RoutingRule>, merge: Boolean) {
        val current = config.value
        val finalRules = if (merge) {
            val existingPatterns = current.routingRules.mapTo(mutableSetOf()) { routingPatternKey(it.pattern) }
            current.routingRules + newRules.filter { routingPatternKey(it.pattern) !in existingPatterns }
        } else {
            val newPatterns = newRules.mapTo(mutableSetOf()) { routingPatternKey(it.pattern) }
            current.routingRules.filter { routingPatternKey(it.pattern) !in newPatterns } + newRules
        }
        updateConfig(current.copy(routingRules = finalRules))
        restartVpnIfActive()
    }

    fun applyPreset(presetId: String) {
        repository.applyPreset(presetId)
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

    fun clearLogs() {
        LogRepository.clear()
    }

    fun copyLogs(context: Context) {
        val allLogs = logs.value.joinToString("\n") { "[${it.timestamp}] [${it.level}] [${it.tag}] ${it.message}" }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Aether Logs", allLogs)
        clipboard.setPrimaryClip(clip)
    }

    fun checkBatteryOptimizationStatus() {
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        _isBatteryOptimized.value = pm?.isIgnoringBatteryOptimizations(appContext.packageName) ?: true
    }

    private fun restartVpnIfActive() {
        val state = connectionStatus.value
        if (state == ConnectionStatus.STOPPED || state == ConnectionStatus.ERROR || state == ConnectionStatus.STOPPING) return

        viewModelScope.launch {
            val config = repository.config.value
            if (config.connectionMode == ConnectionMode.TUNNEL) {
                AetherVpnService.stopVpn(appContext)
            } else {
                AetherProxyService.stopProxy(appContext)
            }

            val stopped = withTimeoutOrNull(3500.milliseconds) {
                connectionStatus.first { it == ConnectionStatus.STOPPED || it == ConnectionStatus.ERROR }
                true
            } == true

            if (!stopped) return@launch

            delay(300.milliseconds)

            if (config.connectionMode == ConnectionMode.PROXY_ONLY || VpnService.prepare(appContext) == null) {
                if (config.connectionMode == ConnectionMode.TUNNEL) {
                    AetherVpnService.startVpn(appContext)
                } else {
                    AetherProxyService.startProxy(appContext)
                }
            }
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
                        LogRepository.i("[Health] Fetching public IP via SOCKS5 ($host:$port)", "UI")
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

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = appContext.packageManager
                val myPkg = appContext.packageName

                pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .asSequence()
                    .filter { it.packageName != myPkg && hasInternetPermission(it) }
                    .map { app ->
                        AppInfo(
                            name = pm.getApplicationLabel(app).toString(),
                            packageName = app.packageName,
                            icon = null,
                            isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        )
                    }
                    .sortedBy { it.name.lowercase() }
                    .toList()
            }

            _installedApps.value = apps
        }
    }

    private fun hasInternetPermission(app: ApplicationInfo): Boolean = runCatching {
        val info = appContext.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
        info.requestedPermissions?.contains("android.permission.INTERNET") == true
    }.getOrDefault(false)

    private fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val info = withContext(Dispatchers.IO) {
                    val request = okhttp3.Request.Builder()
                        .url("https://raw.githubusercontent.com/immaghzbad/AetherST/refs/heads/main/update.json")
                        .build()

                    NetworkClient.instance.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val jsonStr = response.body?.string() ?: return@withContext null
                            val json = JSONObject(jsonStr)

                            UpdateInfo(
                                version = json.getString("version"),
                                versionCode = json.getInt("version_code"),
                                isBeta = json.getBoolean("is_beta"),
                                changelog = json.getString("changelog"),
                                releaseUrl = json.getString("release_url")
                            )
                        } else {
                            null
                        }
                    }
                }

                if (info != null) {
                    val currentVersion = BuildConfig.VERSION_NAME

                    if (info.version != currentVersion) {
                        _updateInfo.value = info
                    }
                }
            } catch (e: Exception) {
                LogRepository.w("Update check failed: ${e.localizedMessage}")
            }
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }

    fun importInternalRoutingRules(assetName: String) {
        viewModelScope.launch {
            try {
                val content = appContext.assets.open(assetName).bufferedReader().use { it.readText() }
                val rules = if (assetName.endsWith(".astb")) {
                    val lines = content.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
                    val parsed = mutableListOf<RoutingRule>()
                    var i = 0
                    while (i + 1 < lines.size) {
                        val mode = when (lines[i+1].trim().removePrefix("-").uppercase()) {
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
                    return@launch
                }
                
                val currentRules = config.value.routingRules
                val currentPatternKeys = currentRules.mapTo(mutableSetOf()) { routingPatternKey(it.pattern) }
                val hasConflict = rules.any { routingPatternKey(it.pattern) in currentPatternKeys }

                if (hasConflict) {
                    _importConflictRules.value = rules
                } else {
                    applyImport(rules, merge = true)
                }
            } catch (e: Exception) {
                showToast("Failed to load internal rules", true)
                LogRepository.e("Internal import error: ${e.message}")
            }
        }
    }
}

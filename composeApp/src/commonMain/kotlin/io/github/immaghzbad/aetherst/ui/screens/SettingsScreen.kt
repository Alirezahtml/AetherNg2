package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.immaghzbad.aetherst.platform.isDesktop
import io.github.immaghzbad.aetherst.shared.core.NetworkUtils
import io.github.immaghzbad.aetherst.shared.model.*
import io.github.immaghzbad.aetherst.shared.ui.localization.LocalAppStrings

import io.github.immaghzbad.aetherst.shared.ui.theme.*

enum class SettingsPage(val title: String) {
    PRESETS("Configuration Profiles"),
    CONNECTION("Connection & Tunneling"),
    PROTOCOL("Protocol & Transport"),
    ZEROTRUST("Cloudflare Zero Trust"),
    NETWORK("Network Parameters"),
    SECURITY("Security & Reliability"),
    DIAGNOSTICS("Diagnostics & Core"),
    SYSTEM("System & Maintenance"),
    HEV_ENGINE("HEV Engine")
}

@Composable
fun SettingsScreen(
    config: AetherConfig,
    isBatteryOptimized: Boolean,
    scrollToSection: Boolean = false,
    onSectionScrolled: () -> Unit = {},
    onUpdateConfig: (AetherConfig) -> Unit,
    onUpdateTunnelEngine: (TunnelEngine) -> Unit,
    onApplyPreset: (String) -> Unit,
    onOpenSplitTunneling: () -> Unit,
    onOpenRoutingRules: () -> Unit,
    onOpenAutoDetect: () -> Unit = {},
    onOpenSpeedTest: () -> Unit = {},
    onRequestBatteryOptimization: () -> Unit,
    onResetAll: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOptimizeMtu: () -> Unit,
    onCopy: (String) -> Unit = {},
    isOptimizingMtu: Boolean = false,
    onShowToast: (String, Boolean) -> Unit = { _, _ -> },
    initialPage: SettingsPage? = null,
    onSubPageClosed: () -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
) {
    var currentPage by remember { mutableStateOf<SettingsPage?>(initialPage) }

    if (currentPage != null) {
        SettingsSubPage(
            page = currentPage!!,
            config = config,
            isBatteryOptimized = isBatteryOptimized,
            onBack = {
                currentPage = null
                onSubPageClosed()
            },
            onUpdateConfig = onUpdateConfig,
            onUpdateTunnelEngine = onUpdateTunnelEngine,
            onApplyPreset = onApplyPreset,
            onOpenAutoDetect = onOpenAutoDetect,
            onOpenSplitTunneling = onOpenSplitTunneling,
            onOpenRoutingRules = onOpenRoutingRules,
            onRequestBatteryOptimization = onRequestBatteryOptimization,
            onResetAll = onResetAll,
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup,
            onOptimizeMtu = onOptimizeMtu,
            onCopy = onCopy,
            isOptimizingMtu = isOptimizingMtu,
            onShowToast = onShowToast,
            bottomContentPadding = bottomContentPadding
        )
        return
    }

    val isAndroid = remember { try { Class.forName("android.os.Build"); true } catch(_: Throwable) { false } }
    val strings = LocalAppStrings.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(IosBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomContentPadding + 12.dp, top = if (isDesktop) 12.dp else 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(strings.settingsTitle, fontWeight = FontWeight.Bold, color = IosTextPrimary, fontSize = 26.sp, lineHeight = 30.sp)
                Text(strings.settingsSubtitle, color = IosSecondaryLabel, fontSize = 12.sp)
            }
        }
        item {
            IosGroupCard {
                Column {
                    IosActionRow(icon = Icons.Default.Speed, iconBg = Color(0xFFFF9500), title = strings.catSpeedTest, subtitle = strings.catSpeedTestSub, onClick = onOpenSpeedTest)
                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp)
                    IosActionRow(icon = Icons.Default.Radar, iconBg = Color(0xFF007AFF), title = strings.catAutoDetect, subtitle = strings.catAutoDetectSub, onClick = onOpenAutoDetect)
                }
            }
        }
        item { CategoryCard(icon = Icons.Default.Tune, iconBg = Color(0xFF8E8E93), title = strings.catPresets, subtitle = strings.catPresetsSub, onClick = { currentPage = SettingsPage.PRESETS }) }
        item { CategoryCard(icon = Icons.Default.VpnLock, iconBg = Color(0xFF34C759), title = strings.catConnection, subtitle = strings.catConnectionSub, onClick = { currentPage = SettingsPage.CONNECTION }) }
        item { CategoryCard(icon = Icons.Default.Shield, iconBg = IosActiveBlue, title = strings.catProtocol, subtitle = strings.catProtocolSub, onClick = { currentPage = SettingsPage.PROTOCOL }) }
        if (config.protocol == AetherProtocol.ZERO_TRUST) {
            item { CategoryCard(icon = Icons.Default.Business, iconBg = Color(0xFF5856D6), title = strings.catZeroTrust, subtitle = strings.catZeroTrustSub, onClick = { currentPage = SettingsPage.ZEROTRUST }) }
        }
        item { CategoryCard(icon = Icons.Default.Language, iconBg = IosActiveBlue, title = strings.catNetwork, subtitle = strings.catNetworkSub, onClick = { currentPage = SettingsPage.NETWORK }) }
        item { CategoryCard(icon = Icons.Default.Lock, iconBg = Color(0xFFFF3B30), title = strings.catSecurity, subtitle = strings.catSecuritySub, onClick = { currentPage = SettingsPage.SECURITY }) }
        item { CategoryCard(icon = Icons.Default.BugReport, iconBg = Color(0xFF64D2FF), title = strings.catDiagnostics, subtitle = strings.catDiagnosticsSub, onClick = { currentPage = SettingsPage.DIAGNOSTICS }) }
        if (isAndroid) {
            item { CategoryCard(icon = Icons.Default.Memory, iconBg = Color(0xFFAF52DE), title = strings.catHevEngine, subtitle = strings.catHevEngineSub, onClick = { currentPage = SettingsPage.HEV_ENGINE }) }
        }
        item { CategoryCard(icon = Icons.Default.Settings, iconBg = Color(0xFF8E8E93), title = strings.catSystem, subtitle = strings.catSystemSub, onClick = { currentPage = SettingsPage.SYSTEM }) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CategoryCard(icon: ImageVector, iconBg: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        border = BorderStroke(1.dp, AppTheme.colors.cardBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosIconBadge(icon = icon, backgroundColor = iconBg)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = IosTextPrimary, fontSize = 15.sp)
                Text(subtitle, color = IosSecondaryLabel, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = IosSecondaryLabel, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingsSubPage(page: SettingsPage, config: AetherConfig, isBatteryOptimized: Boolean, onBack: () -> Unit, onUpdateConfig: (AetherConfig) -> Unit, onUpdateTunnelEngine: (TunnelEngine) -> Unit, onApplyPreset: (String) -> Unit, onOpenAutoDetect: () -> Unit, onOpenSplitTunneling: () -> Unit, onOpenRoutingRules: () -> Unit, onRequestBatteryOptimization: () -> Unit, onResetAll: () -> Unit, onExportBackup: () -> Unit, onImportBackup: () -> Unit, onOptimizeMtu: () -> Unit, onCopy: (String) -> Unit, isOptimizingMtu: Boolean, onShowToast: (String, Boolean) -> Unit, bottomContentPadding: Dp) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showAdvancedZt by remember { mutableStateOf(false) }
    val isAndroid = remember { try { Class.forName("android.os.Build"); true } catch(_: Throwable) { false } }
    val focusManager = LocalFocusManager.current

    io.github.immaghzbad.aetherst.shared.ui.components.PlatformBackHandler(enabled = true, onBack = onBack)

    Column(modifier = Modifier.fillMaxSize().background(IosBackground).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { focusManager.clearFocus() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = if (isDesktop) 12.dp else 36.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = IosActiveBlue, modifier = Modifier.size(24.dp)) }
            Text(page.title, fontWeight = FontWeight.Bold, color = IosTextPrimary, fontSize = 20.sp)
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomContentPadding + 12.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (page) {
                SettingsPage.PRESETS -> item { PresetPage(config, onApplyPreset, onShowToast) }
                SettingsPage.CONNECTION -> item { ConnectionPage(config, isAndroid, onUpdateConfig, onUpdateTunnelEngine, onOpenSplitTunneling, onOpenRoutingRules) }
                SettingsPage.PROTOCOL -> item { ProtocolPage(config, onUpdateConfig, onOptimizeMtu, isOptimizingMtu) }
                SettingsPage.ZEROTRUST -> item { ZeroTrustPage(config, showAdvancedZt, { showAdvancedZt = it }, onUpdateConfig) }
                SettingsPage.NETWORK -> item { NetworkPage(config, isAndroid, onUpdateConfig, onCopy) }
                SettingsPage.SECURITY -> item { SecurityPage(config, isAndroid, isBatteryOptimized, onUpdateConfig, onRequestBatteryOptimization) }
                SettingsPage.DIAGNOSTICS -> item { DiagnosticsPage(config, onUpdateConfig) }
                SettingsPage.HEV_ENGINE -> item { HevEnginePage(config, onUpdateConfig) }
                SettingsPage.SYSTEM -> item { SystemPage(config, isAndroid, onUpdateConfig, onExportBackup, onImportBackup, { showResetDialog = true }) }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
    if (showResetDialog) IosConfirmationDialog(title = "Reset All Settings?", message = "This will restore all protocols, engine tweaks, and security settings to their factory defaults. This action cannot be undone.", confirmText = "Reset Everything", confirmColor = Color(0xFFFF3B30), onConfirm = { onResetAll(); showResetDialog = false; onShowToast("System restored to defaults", false) }, onDismiss = { showResetDialog = false })
}

@Composable private fun PresetPage(config: AetherConfig, onApplyPreset: (String) -> Unit, onShowToast: (String, Boolean) -> Unit) {
    IosGroupCard { Column {
        IosPresetItem(icon = Icons.Default.Tune, iconBg = Color(0xFF8E8E93), title = "Custom Manual Tweaks", subtitle = "Your own independent configuration", isActive = config.presetId == "custom", onClick = { onApplyPreset("custom"); onShowToast("Applied manual configuration", false) })
        divider(); IosPresetItem(icon = Icons.Default.Lock, iconBg = Color(0xFF5856D6), title = "Bypass UDP / TLS", subtitle = "MASQUE + H2 Fallback + Fragmentation", isActive = config.presetId == "bypass_udp", onClick = { onApplyPreset("bypass_udp"); onShowToast("Applied UDP/TLS Bypass preset", false) })
        divider(); IosPresetItem(icon = Icons.Default.Shield, iconBg = IosActiveBlue, title = "Ironclad Stealth", subtitle = "MASQUE + GFW Noise + Ironclad Probe", isActive = config.presetId == "ironclad_stealth", onClick = { onApplyPreset("ironclad_stealth"); onShowToast("Applied Ironclad Stealth preset", false) })
        divider(); IosPresetItem(icon = Icons.Default.Bolt, iconBg = Color(0xFFFF9500), title = "Turbo Speed", subtitle = "WireGuard + Balanced Noise + Turbo Scan", isActive = config.presetId == "turbo_wg", onClick = { onApplyPreset("turbo_wg"); onShowToast("Applied Turbo Speed preset", false) })
    } }
}

@Composable private fun ConnectionPage(config: AetherConfig, isAndroid: Boolean, onUpdateConfig: (AetherConfig) -> Unit, onUpdateTunnelEngine: (TunnelEngine) -> Unit, onOpenSplitTunneling: () -> Unit, onOpenRoutingRules: () -> Unit) {
    IosGroupCard { Column {
        val opts = if (isAndroid) listOf("Tunnel", "Proxy Only") else if (isDesktop) listOf("TUN Mode (Global)", "System Proxy", "Proxy Only") else listOf("TUN Mode (Global)", "System Proxy", "Proxy Only")
        IosPickerRow(icon = Icons.Default.VpnLock, iconBg = Color(0xFF34C759), title = "Connection Mode", value = when { config.connectionMode == ConnectionMode.TUNNEL -> if (isAndroid) "Tunnel" else "TUN Mode (Global)"; config.connectionMode == ConnectionMode.SYSTEM_PROXY -> "System Proxy"; else -> "Proxy Only" }, options = opts, onOptionSelected = { val m = if (isAndroid) { if (it == 0) ConnectionMode.TUNNEL else ConnectionMode.PROXY_ONLY } else if (isDesktop) { when (it) { 0 -> ConnectionMode.TUNNEL; 1 -> ConnectionMode.SYSTEM_PROXY; else -> ConnectionMode.PROXY_ONLY } } else { when (it) { 0 -> ConnectionMode.TUNNEL; 1 -> ConnectionMode.SYSTEM_PROXY; else -> ConnectionMode.PROXY_ONLY } }; onUpdateConfig(config.copy(connectionMode = m)) })
        if (config.connectionMode == ConnectionMode.TUNNEL) {
            divider(); IosPickerRow(icon = Icons.Default.VpnLock, iconBg = Color(0xFF5856D6), title = "Tunnel Engine", value = config.tunnelEngine.displayName, options = TunnelEngine.entries.map { it.displayName }, onOptionSelected = { onUpdateTunnelEngine(TunnelEngine.entries[it]) })
            if (!isDesktop) {
                divider(); IosSwitchRow(icon = Icons.Default.AllInclusive, iconBg = IosActiveBlue, title = "Tunnel Whole Device", subtitle = "Route all application traffic through VPN", checked = config.tunnelAllApps, onCheckedChange = { onUpdateConfig(config.copy(tunnelAllApps = it)) }, testTag = "switch_tunnel_all"); divider(); IosPickerRow(icon = Icons.Default.Tune, iconBg = Color(0xFF5856D6), title = "Split Tunneling", value = if (config.tunnelAllApps) "All Apps Tunneled" else "${config.excludedPackages.size + config.blockedPackages.size} Apps", options = emptyList(), onOptionSelected = {}, onClickOverride = onOpenSplitTunneling, enabled = !config.tunnelAllApps)
            }
            divider()
        }
        IosPickerRow(icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = IosActiveBlue, title = "Domain & IP Routing", value = "${config.routingRules.size} Rules", options = emptyList(), onOptionSelected = {}, onClickOverride = onOpenRoutingRules)
        if (isAndroid) { divider(); IosSwitchRow(icon = Icons.Default.Share, iconBg = Color(0xFFAF52DE), title = "Share via Hotspot", subtitle = "Allow other devices to connect to proxy", checked = config.shareHotspot, onCheckedChange = { onUpdateConfig(config.copy(shareHotspot = it)) }, testTag = "switch_share_hotspot"); if (config.shareHotspot) HotspotInfo(config) }
    } }
}

@Composable private fun HotspotInfo(config: AetherConfig) {
    Column(modifier = Modifier.fillMaxWidth().background(IosGroupBg.copy(alpha = 0.4f)).padding(14.dp)) {
        var localIp by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) { localIp = NetworkUtils.getLocalIpAddress() }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { Icon(if (localIp != null) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (localIp != null) IosActiveGreen else Color(0xFFFF9500), modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(if (localIp != null) "Hotspot Active" else "Hotspot Inactive", color = if (localIp != null) IosActiveGreen else Color(0xFFFF9500), fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            IconButton(onClick = { localIp = NetworkUtils.getLocalIpAddress() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Refresh, null, tint = IosActiveBlue, modifier = Modifier.size(18.dp)) }
        }
        if (localIp != null) { Spacer(modifier = Modifier.height(10.dp)); Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color.Black.copy(alpha = 0.3f)) { Column(modifier = Modifier.padding(12.dp)) { Text("PROXY ADDRESS", color = IosSecondaryLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp); Spacer(modifier = Modifier.height(6.dp)); Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("$localIp:${config.socksPort}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) } } } }
    }
}

@Composable private fun ProtocolPage(config: AetherConfig, onUpdateConfig: (AetherConfig) -> Unit, onOptimizeMtu: () -> Unit, isOptimizingMtu: Boolean) {
    IosGroupCard { Column {
        IosPickerRow(icon = Icons.Default.VpnLock, iconBg = IosActiveBlue, title = "Transport Protocol", value = config.protocol.displayName, options = AetherProtocol.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(protocol = AetherProtocol.entries[it])) })
        if (config.protocol == AetherProtocol.MASQUE) { divider(); IosSwitchRow(icon = Icons.Default.Http, iconBg = IosActiveBlue, title = "HTTP/2 Fallback Mode", subtitle = "Force MASQUE over TCP/TLS instead of QUIC", checked = config.h2Mode, onCheckedChange = { onUpdateConfig(config.copy(h2Mode = it)) }, testTag = "switch_h2_mode"); divider(); IosSwitchRow(icon = Icons.Default.VerticalSplit, iconBg = Color(0xFF5856D6), title = "Packet Fragmentation", subtitle = "Bypass SNI filters (H2 mode only)", checked = config.h2Fragment, onCheckedChange = { onUpdateConfig(config.copy(h2Fragment = it)) }, testTag = "switch_fragment"); if (config.h2Fragment) { IosInputFieldRow(icon = Icons.Default.Straighten, iconBg = IosSecondaryLabel, label = "Fragment Size (Bytes)", value = config.fragmentSize, onValueChange = { onUpdateConfig(config.copy(fragmentSize = it)) }, placeholder = "16-32", testTag = "fragment_size_input"); divider(); IosInputFieldRow(icon = Icons.Default.Timer, iconBg = IosSecondaryLabel, label = "Fragment Delay (ms)", value = config.fragmentDelay, onValueChange = { onUpdateConfig(config.copy(fragmentDelay = it)) }, placeholder = "2-10", testTag = "fragment_delay_input"); divider() }; IosSwitchRow(icon = Icons.Default.EnhancedEncryption, iconBg = IosActiveGreen, title = "Encrypted Client Hello (ECH)", subtitle = "Hide SNI from network observers", checked = config.echEnabled, onCheckedChange = { onUpdateConfig(config.copy(echEnabled = it)) }, testTag = "switch_ech_enabled"); divider() }
        IosSwitchRow(icon = Icons.Default.DataUsage, iconBg = Color(0xFFFF9500), title = "Disable Data Verification", subtitle = "Skip waiting for initial packet exchange", checked = config.noDataCheck, onCheckedChange = { onUpdateConfig(config.copy(noDataCheck = it)) }, testTag = "switch_no_data_check"); divider()
        val availNoise = if (config.protocol == AetherProtocol.MASQUE) listOf(AetherNoise.FIREWALL, AetherNoise.GFW, AetherNoise.OFF) else listOf(AetherNoise.BALANCED, AetherNoise.AGGRESSIVE, AetherNoise.LIGHT, AetherNoise.OFF)
        IosPickerRow(icon = Icons.Default.Tune, iconBg = Color(0xFFAF52DE), title = "Bypass Obfuscation", value = config.noise.displayName.substringBefore(" ("), options = availNoise.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(noise = availNoise[it])) }); divider()
        IosPickerRow(icon = Icons.Default.NetworkCheck, iconBg = Color(0xFFFF9500), title = "Speed Strategy", value = config.scanMode.name.lowercase().replaceFirstChar { it.uppercase() }, options = AetherScanMode.entries.map { "${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }} (${it.description})" }, onOptionSelected = { onUpdateConfig(config.copy(scanMode = AetherScanMode.entries[it])) }); divider()
        IosPickerRow(icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = Color(0xFF5856D6), title = "Network Stack", value = config.ipMode.rawValue, options = AetherIpMode.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(ipMode = AetherIpMode.entries[it])) }); divider()
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.Bottom) { Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.Default.Tune, backgroundColor = IosActiveGreen); Spacer(modifier = Modifier.width(12.dp)); IosInputField(label = "Custom MTU Size", value = config.mtu.toString(), onValueChange = { onUpdateConfig(config.copy(mtu = it.toIntOrNull() ?: 1100)) }, modifier = Modifier.weight(1f), placeholder = "1100", keyboardType = KeyboardType.Number, testTag = "mtu_input") }; Spacer(modifier = Modifier.width(8.dp)); Button(onClick = onOptimizeMtu, enabled = !isOptimizingMtu, modifier = Modifier.height(46.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = IosActiveBlue.copy(alpha = 0.15f), contentColor = IosActiveBlue, disabledContainerColor = IosActiveBlue.copy(alpha = 0.05f), disabledContentColor = IosActiveBlue.copy(alpha = 0.3f)), contentPadding = PaddingValues(horizontal = 16.dp)) { if (isOptimizingMtu) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = IosActiveBlue, strokeWidth = 2.dp) else Text("Optimize", fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
    } }
}

@Composable private fun ZeroTrustPage(config: AetherConfig, showAdvanced: Boolean, onToggleAdvanced: (Boolean) -> Unit, onUpdateConfig: (AetherConfig) -> Unit) {
    IosGroupCard { Column {
        IosInputFieldRow(icon = Icons.Default.Business, iconBg = Color(0xFF5856D6), label = "Organization Team Name", value = config.teamName, onValueChange = { onUpdateConfig(config.copy(teamName = it)) }, placeholder = "e.g. my-org", testTag = "zt_team_input"); divider()
        IosInputFieldRow(icon = Icons.Default.Language, iconBg = IosActiveBlue, label = "Cloudflare Access Email", value = config.accessEmail, onValueChange = { onUpdateConfig(config.copy(accessEmail = it)) }, placeholder = "user@example.com", testTag = "zt_email_input"); divider()
        IosSwitchRow(icon = Icons.Default.Shield, iconBg = IosActiveGreen, title = "Gateway Filtering Proxy", subtitle = "Route via org Gateway for filtering & logs", checked = config.useGateway, onCheckedChange = { onUpdateConfig(config.copy(useGateway = it)) }, testTag = "switch_zt_gateway"); divider()
        Row(modifier = Modifier.fillMaxWidth().clickable { onToggleAdvanced(!showAdvanced) }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.Default.Lock, backgroundColor = IosSecondaryLabel); Spacer(modifier = Modifier.width(12.dp)); Text("Advanced Authentication", fontWeight = FontWeight.Medium, color = IosTextPrimary, fontSize = 15.sp) }; Icon(if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = IosSecondaryLabel, modifier = Modifier.size(18.dp)) }
        AnimatedVisibility(visible = showAdvanced, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) { Column(modifier = Modifier.fillMaxWidth().background(IosGroupBg.copy(alpha = 0.4f)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { IosInputField(label = "Access Client ID", value = config.accessId, onValueChange = { onUpdateConfig(config.copy(accessId = it)) }, placeholder = "Required for Service Tokens", testTag = "zt_access_id"); IosInputField(label = "Access Client Secret", value = config.accessSecret, onValueChange = { onUpdateConfig(config.copy(accessSecret = it)) }, placeholder = "Required for Service Tokens", testTag = "zt_access_secret"); IosInputField(label = "Manual JWT Access Token", value = config.accessToken, onValueChange = { onUpdateConfig(config.copy(accessToken = it)) }, placeholder = "Optional overrides auth", testTag = "zt_access_token") } }
    } }
}

@Composable private fun NetworkPage(config: AetherConfig, isAndroid: Boolean, onUpdateConfig: (AetherConfig) -> Unit, onCopy: (String) -> Unit) {
    IosGroupCard { Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.Default.Language, backgroundColor = IosActiveBlue); Spacer(modifier = Modifier.width(12.dp)); IosInputField(label = "SOCKS5 Host", value = config.socksHost, onValueChange = { onUpdateConfig(config.copy(socksHost = it)) }, modifier = Modifier.weight(1f), placeholder = "127.0.0.1", testTag = "socks_host_input"); Spacer(modifier = Modifier.width(10.dp)); IosInputField(label = "SOCKS Port", value = config.socksPort, onValueChange = { onUpdateConfig(config.copy(socksPort = it)) }, modifier = Modifier.width(75.dp), placeholder = "1819", keyboardType = KeyboardType.Number, testTag = "socks_port_input"); Spacer(modifier = Modifier.width(8.dp)); IosInputField(label = "HTTP Port", value = config.httpPort, onValueChange = { onUpdateConfig(config.copy(httpPort = it)) }, modifier = Modifier.width(75.dp), placeholder = "1820", keyboardType = KeyboardType.Number, testTag = "http_port_input") }
        divider(); IosSwitchRow(icon = Icons.Default.Http, iconBg = IosActiveBlue, title = "Internal HTTP Proxy", subtitle = "Expose an HTTP CONNECT proxy alongside SOCKS5", checked = config.httpProxyEnabled, onCheckedChange = { onUpdateConfig(config.copy(httpProxyEnabled = it)) }, testTag = "switch_http_proxy_enabled"); divider()
        IosInputFieldRow(icon = Icons.Default.Code, iconBg = IosSecondaryLabel, label = "TLS Key Groups", value = config.tlsGroups, onValueChange = { onUpdateConfig(config.copy(tlsGroups = it)) }, placeholder = "P-256:X25519:P-384", testTag = "tls_groups_input"); divider()
        if (isAndroid) { IosInputFieldRow(icon = Icons.Default.Dns, iconBg = IosActiveBlue, label = "Tunnel DNS Servers", value = config.dnsList, onValueChange = { onUpdateConfig(config.copy(dnsList = it.replace(Regex("\\s*,\\s*"), ","))) }, placeholder = "1.1.1.1,1.0.0.1", testTag = "dns_list_input"); divider() }
        IosInputFieldRow(icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = Color(0xFF5856D6), label = "Forced Peer IP", value = config.peer, onValueChange = { onUpdateConfig(config.copy(peer = it)) }, placeholder = "e.g. 1.2.3.4:443", testTag = "peer_input"); divider()
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.Default.Bolt, backgroundColor = Color(0xFFFF9500)); Spacer(modifier = Modifier.width(12.dp)); IosInputField(label = "Keepalive (Secs)", value = config.keepalive.toString(), onValueChange = { onUpdateConfig(config.copy(keepalive = it.toIntOrNull() ?: 5)) }, placeholder = "5", keyboardType = KeyboardType.Number, testTag = "keepalive_input") }; Spacer(modifier = Modifier.width(12.dp)); Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosInputField(label = "Validation (Secs)", value = config.validateSecs.toString(), onValueChange = { onUpdateConfig(config.copy(validateSecs = it.toIntOrNull() ?: 10)) }, placeholder = "10", keyboardType = KeyboardType.Number, testTag = "validate_secs_input") } }
    } }
}

@Composable private fun SecurityPage(config: AetherConfig, isAndroid: Boolean, isBatteryOptimized: Boolean, onUpdateConfig: (AetherConfig) -> Unit, onRequestBatteryOptimization: () -> Unit) {
    IosGroupCard { Column {
        IosSwitchRow(icon = Icons.Default.VpnLock, iconBg = Color(0xFF5856D6), title = "Strict Kill Switch", subtitle = "Prevent any leak even during manual stop", checked = config.strictKillSwitch, onCheckedChange = { onUpdateConfig(config.copy(strictKillSwitch = it)) }, testTag = "switch_strict_kill_switch"); divider()
        IosSwitchRow(icon = Icons.Default.Lock, iconBg = Color(0xFFFF3B30), title = "Kill Switch", subtitle = "Block traffic when VPN is disconnected", checked = config.killSwitch, onCheckedChange = { onUpdateConfig(config.copy(killSwitch = it)) }, testTag = "switch_kill_switch"); divider()
        IosSwitchRow(icon = Icons.Default.Security, iconBg = Color(0xFF5856D6), title = "IPv6 Leak Protection", subtitle = "Force all IPv6 traffic through tunnel", checked = config.ipv6Leak, onCheckedChange = { onUpdateConfig(config.copy(ipv6Leak = it)) }, testTag = "switch_ipv6_leak"); divider()
        IosSwitchRow(icon = Icons.Default.Restore, iconBg = IosActiveGreen, title = "Smart Reconnect", subtitle = "Attempt auto-recovery on network failure", checked = config.smartReconnect, onCheckedChange = { onUpdateConfig(config.copy(smartReconnect = it)) }, testTag = "switch_smart_reconnect")
        if (config.smartReconnect) {
            divider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    IosIconBadge(icon = Icons.Default.Repeat, backgroundColor = IosSecondaryLabel)
                    Spacer(modifier = Modifier.width(12.dp))
                    IosInputField(
                        label = "Max Retries",
                        value = config.reconnectRetryLimit.toString(),
                        onValueChange = { onUpdateConfig(config.copy(reconnectRetryLimit = it.toIntOrNull() ?: 10)) },
                        placeholder = "10",
                        keyboardType = KeyboardType.Number,
                        testTag = "reconnect_limit_input"
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    IosInputField(
                        label = "Delay (Secs)",
                        value = config.reconnectSecs.toString(),
                        onValueChange = { onUpdateConfig(config.copy(reconnectSecs = it.toIntOrNull() ?: 2)) },
                        placeholder = "2",
                        keyboardType = KeyboardType.Number,
                        testTag = "reconnect_secs_input"
                    )
                }
            }
        }
        divider(); IosSwitchRow(icon = Icons.Default.Sync, iconBg = IosActiveGreen, title = "Cloudflare Reprovision", subtitle = "Auto-register fresh device on identity loss", checked = config.reprovision, onCheckedChange = { onUpdateConfig(config.copy(reprovision = it)) }, testTag = "switch_reprovision")
        if (isAndroid) { divider(); IosSwitchRow(icon = Icons.Default.BatteryAlert, iconBg = Color(0xFFFF3B30), title = "Battery Optimization", subtitle = "Allow Aether Ng to run without restrictions", checked = isBatteryOptimized, enabled = !isBatteryOptimized, onCheckedChange = { if (it) onRequestBatteryOptimization() }, testTag = "switch_battery_opt") }
    } }
}

@Composable private fun DiagnosticsPage(config: AetherConfig, onUpdateConfig: (AetherConfig) -> Unit) {
    IosGroupCard { Column {
        IosPickerRow(icon = Icons.Default.BugReport, iconBg = Color(0xFF64D2FF), title = "App System Logging", value = config.appLogLevel.displayName.substringBefore(" ("), options = AetherLogLevel.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(appLogLevel = AetherLogLevel.entries[it])) }); divider()
        IosPickerRow(icon = Icons.Default.VpnLock, iconBg = IosSecondaryLabel, title = "Aether Core Logging", value = config.coreLogLevel.displayName.substringBefore(" ("), options = AetherLogLevel.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(coreLogLevel = AetherLogLevel.entries[it])) }); divider()
        IosPickerRow(icon = Icons.Default.Speed, iconBg = IosActiveGreen, title = "Core Performance Profile", value = config.perfProfile.displayName, options = AetherPerfProfile.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(perfProfile = AetherPerfProfile.entries[it])) }); divider()
        IosInputFieldRow(icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = Color(0xFFAF52DE), label = "Upstream Proxy URL", value = config.upstreamProxy, onValueChange = { onUpdateConfig(config.copy(upstreamProxy = it)) }, placeholder = "socks5://host:port", testTag = "upstream_proxy_input"); divider()
        IosSwitchRow(icon = Icons.AutoMirrored.Filled.Rule, iconBg = IosActiveBlue, title = "Domain Sniffing", subtitle = "Sniff SNI/Host for domain routing rules", checked = config.routeSniffing, onCheckedChange = { onUpdateConfig(config.copy(routeSniffing = it)) }, testTag = "switch_route_sniffing")
        if (config.routeSniffing) { divider(); IosInputFieldRow(icon = Icons.Default.Timer, iconBg = IosSecondaryLabel, label = "Sniffing Timeout (ms)", value = config.sniffingTimeoutMs.toString(), onValueChange = { onUpdateConfig(config.copy(sniffingTimeoutMs = it.toIntOrNull() ?: 100)) }, placeholder = "100", keyboardType = KeyboardType.Number, testTag = "sniffing_timeout_input") }
        divider(); IosSwitchRow(icon = Icons.Default.Restore, iconBg = IosActiveGreen, title = "Quick Reconnect Strategy", subtitle = "Optimize session recovery timing", checked = config.quickReconnect, onCheckedChange = { onUpdateConfig(config.copy(quickReconnect = it)) }, testTag = "switch_quick_reconnect"); divider()
        IosSwitchRow(icon = Icons.Default.Block, iconBg = Color(0xFFFF3B30), title = "Strict Profile Lock", subtitle = "Disable fallback to other profiles", checked = config.noProfileRetry, onCheckedChange = { onUpdateConfig(config.copy(noProfileRetry = it)) }, testTag = "switch_no_profile_retry")
    } }
}

@Composable private fun HevEnginePage(config: AetherConfig, onUpdateConfig: (AetherConfig) -> Unit) {
    val hevLevels = listOf("error", "warn", "info", "debug")
    val levelLabels = mapOf("error" to "Error", "warn" to "Warn (Default)", "info" to "Info", "debug" to "Debug (Verbose)")
    val currentLevel = if (config.hevLogLevel in hevLevels) config.hevLogLevel else "warn"

    IosGroupCard { Column {
        IosPickerRow(
            icon = Icons.Default.BugReport,
            iconBg = Color(0xFFAF52DE),
            title = "HEV Log Level",
            value = levelLabels[currentLevel] ?: "Warn (Default)",
            options = hevLevels.map { levelLabels[it]!! },
            onOptionSelected = { index -> onUpdateConfig(config.copy(hevLogLevel = hevLevels[index])) }
        ); divider()
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                IosIconBadge(icon = Icons.Default.Timer, backgroundColor = IosActiveBlue); Spacer(modifier = Modifier.width(12.dp))
                IosInputField(label = "Connect Timeout (ms)", value = config.hevConnectTimeoutMs.toString(), onValueChange = { onUpdateConfig(config.copy(hevConnectTimeoutMs = it.toIntOrNull()?.coerceIn(500, 120000) ?: 5000)) }, modifier = Modifier.weight(1f), placeholder = "5000", keyboardType = KeyboardType.Number, testTag = "hev_connect_timeout_input")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                IosIconBadge(icon = Icons.Default.SwapHoriz, backgroundColor = IosActiveGreen); Spacer(modifier = Modifier.width(12.dp))
                IosInputField(label = "RW Timeout (ms)", value = config.hevReadWriteTimeoutMs.toString(), onValueChange = { onUpdateConfig(config.copy(hevReadWriteTimeoutMs = it.toIntOrNull()?.coerceIn(1000, 600000) ?: 60000)) }, modifier = Modifier.weight(1f), placeholder = "60000", keyboardType = KeyboardType.Number, testTag = "hev_rw_timeout_input")
            }
        }
        divider()
        IosInputFieldRow(icon = Icons.Default.Layers, iconBg = Color(0xFF5856D6), label = "Max Sessions (0 = Unlimited)", value = config.hevMaxSessionCount.toString(), onValueChange = { onUpdateConfig(config.copy(hevMaxSessionCount = it.toIntOrNull()?.coerceIn(0, 200000) ?: 0)) }, placeholder = "0", keyboardType = KeyboardType.Number, testTag = "hev_max_sessions_input"); divider()
        IosInputFieldRow(icon = Icons.Default.Storage, iconBg = Color(0xFFFF9500), label = "MapDNS Cache Size", value = config.hevMapdnsCacheSize.toString(), onValueChange = { onUpdateConfig(config.copy(hevMapdnsCacheSize = it.toIntOrNull()?.coerceIn(100, 1000000) ?: 10000)) }, placeholder = "10000", keyboardType = KeyboardType.Number, testTag = "hev_mapdns_cache_input")
    } }
    Spacer(modifier = Modifier.height(8.dp))
    IosGroupCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text("About HEV Engine", fontWeight = FontWeight.Bold, color = IosTextPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("These values configure the native HEV tun2socks engine used in Tunnel mode on Android. Changes apply after the VPN reconnects. Timeouts are clamped to safe ranges; Max Sessions 0 means unlimited.", color = IosSecondaryLabel, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable private fun SystemPage(config: AetherConfig, isAndroid: Boolean, onUpdateConfig: (AetherConfig) -> Unit, onExportBackup: () -> Unit, onImportBackup: () -> Unit, onResetClick: () -> Unit) {
    val themeOptions = listOf("System Default", "Dark Mode", "Light Mode")
    val currentThemeIndex = when (config.themeMode) {
        AppThemeMode.SYSTEM -> 0
        AppThemeMode.DARK -> 1
        AppThemeMode.LIGHT -> 2
    }
    val langOptions = listOf("System Default / پیش‌فرض", "English", "فارسی (Persian)")
    val currentLangIndex = when (config.appLanguage) {
        AppLanguage.SYSTEM -> 0
        AppLanguage.ENGLISH -> 1
        AppLanguage.PERSIAN -> 2
    }
    val strings = LocalAppStrings.current
    IosGroupCard { Column {
        IosPickerRow(
            icon = Icons.Default.Palette,
            iconBg = Color(0xFFAF52DE),
            title = strings.themeTitle,
            value = themeOptions[currentThemeIndex],
            options = themeOptions,
            onOptionSelected = { index ->
                val newMode = when (index) {
                    0 -> AppThemeMode.SYSTEM
                    1 -> AppThemeMode.DARK
                    2 -> AppThemeMode.LIGHT
                    else -> AppThemeMode.SYSTEM
                }
                onUpdateConfig(config.copy(themeMode = newMode))
            }
        ); divider()
        IosPickerRow(
            icon = Icons.Default.Language,
            iconBg = IosActiveBlue,
            title = strings.labelLanguageTitle,
            value = langOptions[currentLangIndex],
            options = langOptions,
            onOptionSelected = { index ->
                val newLang = when (index) {
                    0 -> AppLanguage.SYSTEM
                    1 -> AppLanguage.ENGLISH
                    2 -> AppLanguage.PERSIAN
                    else -> AppLanguage.SYSTEM
                }
                onUpdateConfig(config.copy(appLanguage = newLang))
            }
        ); divider()
        IosSwitchRow(
            icon = Icons.Default.NetworkPing,
            iconBg = Color(0xFF34C759),
            title = strings.settingsShowLivePing,
            subtitle = strings.settingsShowLivePingSub,
            checked = config.showLivePingWidget,
            onCheckedChange = { onUpdateConfig(config.copy(showLivePingWidget = it)) },
            testTag = "switch_show_live_ping"
        ); divider()
        IosSwitchRow(
            icon = Icons.Default.ShowChart,
            iconBg = IosActiveBlue,
            title = strings.settingsShowTrafficChart,
            subtitle = strings.settingsShowTrafficChartSub,
            checked = config.showTrafficChartWidget,
            onCheckedChange = { onUpdateConfig(config.copy(showTrafficChartWidget = it)) },
            testTag = "switch_show_traffic_chart"
        ); divider()
        IosActionRow(icon = Icons.Default.CloudUpload, iconBg = Color(0xFF5856D6), title = "Full Configuration Backup", subtitle = "Export all settings to .astf file", onClick = onExportBackup); divider()
        IosActionRow(icon = Icons.Default.CloudDownload, iconBg = IosActiveGreen, title = "Restore from Backup", subtitle = "Import settings from an .astf file", onClick = onImportBackup); divider()
        IosActionRow(icon = Icons.Default.DeleteForever, iconBg = Color(0xFFFF3B30), title = "Reset to Factory Defaults", subtitle = "Wipe all custom tweaks and restart", onClick = onResetClick, titleColor = Color(0xFFFF3B30))
    } }
}

@Composable private fun divider() = HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp, modifier = Modifier.padding(start = 50.dp))

@Composable fun IosConfirmationDialog(title: String, message: String, confirmText: String, confirmColor: Color = Color.White, onConfirm: () -> Unit, onDismiss: () -> Unit, scaleFactor: Float = 1f) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onConfirm, modifier = Modifier.height(44.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = confirmColor, contentColor = Color.White)) { Text(confirmText, fontWeight = FontWeight.Bold, fontSize = 14.sp) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = IosSecondaryLabel, fontWeight = FontWeight.Bold) } }, title = { Text(title, color = IosTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) }, text = { Text(message, color = IosSecondaryLabel, fontSize = 14.sp, lineHeight = 20.sp) }, containerColor = IosCardBg, shape = RoundedCornerShape(20.dp), modifier = Modifier.border(1.dp, IosCardBorder, RoundedCornerShape(20.dp)))
}

@Composable fun IosPresetItem(icon: ImageVector, iconBg: Color, title: String, subtitle: String, isActive: Boolean, onClick: () -> Unit, scaleFactor: Float = 1f) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = icon, backgroundColor = iconBg, scaleFactor = scaleFactor); Spacer(modifier = Modifier.width(12.dp)); Column { Text(title, fontWeight = FontWeight.Medium, color = IosTextPrimary, fontSize = 15.sp); Text(subtitle, color = IosSecondaryLabel, fontSize = 11.sp) } }; if (isActive) Text("Active", fontWeight = FontWeight.Bold, color = IosActiveGreen, fontSize = 11.sp) else Icon(Icons.Default.ChevronRight, null, tint = IosSecondaryLabel, modifier = Modifier.size(18.dp)) }
}

@Composable fun IosSectionHeader(title: String, scaleFactor: Float = 1f) { Text(text = title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = IosSecondaryLabel, fontSize = 11.sp, letterSpacing = 0.5.sp, modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)) }

@Composable fun IosGroupCard(content: @Composable () -> Unit) { Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = IosCardBg), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) { content() } }

@Composable fun IosIconBadge(icon: ImageVector, backgroundColor: Color, scaleFactor: Float = 1f) { Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(backgroundColor), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) } }

@Composable fun IosSwitchRow(icon: ImageVector, iconBg: Color, title: String, subtitle: String? = null, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit, testTag: String, scaleFactor: Float = 1f) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = icon, backgroundColor = iconBg, scaleFactor = scaleFactor); Spacer(modifier = Modifier.width(12.dp)); Column { Text(title, fontWeight = FontWeight.Medium, color = IosTextPrimary.copy(alpha = if (enabled) 1f else 0.5f), fontSize = 15.sp); if (!subtitle.isNullOrEmpty()) Text(subtitle, color = IosSecondaryLabel.copy(alpha = if (enabled) 1f else 0.5f), fontSize = 11.sp) } }; Spacer(modifier = Modifier.width(8.dp)); Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled, modifier = Modifier.testTag(testTag).graphicsLayer { scaleX = scaleFactor * 0.9f; scaleY = scaleFactor * 0.9f }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IosActiveGreen, checkedBorderColor = Color.Transparent, uncheckedThumbColor = Color.White, uncheckedTrackColor = IosInactiveTrack, uncheckedBorderColor = Color.Transparent, disabledCheckedTrackColor = IosActiveGreen.copy(alpha = 0.5f), disabledCheckedThumbColor = Color.White.copy(alpha = 0.8f))) }
}

@Composable fun IosPickerRow(icon: ImageVector, iconBg: Color, title: String, value: String, options: List<String>, onOptionSelected: (Int) -> Unit, scaleFactor: Float = 1f, onClickOverride: (() -> Unit)? = null, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    Box { Row(modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { if (onClickOverride != null) onClickOverride() else expanded = true }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = icon, backgroundColor = iconBg, scaleFactor = scaleFactor); Spacer(modifier = Modifier.width(12.dp)); Text(title, fontWeight = FontWeight.Medium, color = if (enabled) IosTextPrimary else IosTextPrimary.copy(alpha = 0.35f), fontSize = 15.sp) }; Row(verticalAlignment = Alignment.CenterVertically) { Text(value, color = if (enabled) IosSecondaryLabel else IosSecondaryLabel.copy(alpha = 0.35f), maxLines = 1, fontSize = 13.sp); Spacer(modifier = Modifier.width(4.dp)); Icon(Icons.Default.ChevronRight, null, tint = if (enabled) IosSecondaryLabel else IosSecondaryLabel.copy(alpha = 0.35f), modifier = Modifier.size(18.dp)) } }; DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(IosGroupBg)) { options.forEachIndexed { index, option -> DropdownMenuItem(text = { Text(option, color = IosTextPrimary, fontSize = 14.sp) }, onClick = { onOptionSelected(index); expanded = false }) } } }
}

@Composable fun IosActionRow(icon: ImageVector, iconBg: Color, title: String, subtitle: String? = null, onClick: () -> Unit, scaleFactor: Float = 1f, titleColor: Color = IosTextPrimary) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = icon, backgroundColor = iconBg, scaleFactor = scaleFactor); Spacer(modifier = Modifier.width(12.dp)); Column { Text(title, fontWeight = FontWeight.Medium, color = titleColor, fontSize = 15.sp); if (subtitle != null) Text(subtitle, color = IosSecondaryLabel, fontSize = 11.sp) } }; Icon(Icons.Default.ChevronRight, null, tint = IosSecondaryLabel, modifier = Modifier.size(18.dp)) }
}

@Composable fun IosInputField(label: String, value: String, onValueChange: (String) -> Unit, testTag: String, modifier: Modifier = Modifier, placeholder: String = "", keyboardType: KeyboardType = KeyboardType.Text, scaleFactor: Float = 1f) {
    val focusManager = LocalFocusManager.current; var isFocused by remember { mutableStateOf(false) }
    Column(modifier = modifier) { Text(label, color = IosSecondaryLabel, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp)); BasicTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().height(46.dp).background(IosGroupBg, RoundedCornerShape(10.dp)).border(1.dp, if (isFocused) IosActiveBlue else Color.Transparent, RoundedCornerShape(10.dp)).onFocusChanged { isFocused = it.isFocused }.testTag(testTag), textStyle = MaterialTheme.typography.bodyMedium.copy(color = IosTextPrimary, fontSize = 14.sp), keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }), singleLine = true, cursorBrush = SolidColor(IosActiveBlue), decorationBox = { innerTextField -> Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) { if (value.isEmpty()) Text(placeholder, color = IosSecondaryLabel, fontSize = 13.sp); innerTextField() } }) }
}

@Composable fun IosInputFieldRow(icon: ImageVector, iconBg: Color, label: String, value: String, onValueChange: (String) -> Unit, placeholder: String = "", keyboardType: KeyboardType = KeyboardType.Text, testTag: String, scaleFactor: Float = 1f) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = icon, backgroundColor = iconBg, scaleFactor = scaleFactor); Spacer(modifier = Modifier.width(12.dp)); IosInputField(label = label, value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f), placeholder = placeholder, keyboardType = keyboardType, testTag = testTag, scaleFactor = scaleFactor) }
}

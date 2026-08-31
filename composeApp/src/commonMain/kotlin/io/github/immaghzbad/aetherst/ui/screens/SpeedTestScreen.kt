package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.text.style.TextOverflow
import io.github.immaghzbad.aetherst.platform.isDesktop
import io.github.immaghzbad.aetherst.shared.data.SpeedTestRepository
import io.github.immaghzbad.aetherst.shared.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val IosCardBg = Color(0xFF1C1C1E)
private val IosGroupBg = Color(0xFF2C2C2E)
private val IosSecondaryLabel = Color(0xFF8E8E93)
private val IosActiveGreen = Color(0xFF34C759)
private val IosActiveBlue = Color(0xFF007AFF)
private val IosErrorRed = Color(0xFFFF3B30)
private val IosAmber = Color(0xFFFF9500)
private val IosPurple = Color(0xFFAF52DE)

@Composable
fun SpeedTestScreen(
    onBack: () -> Unit,
    onCopy: (String) -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
    connectionStatus: ConnectionStatus = ConnectionStatus.STOPPED,
    config: AetherConfig = AetherConfig()
) {
    val state by SpeedTestRepository.state.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showServerUnavailable by remember { mutableStateOf(false) }
    var checkingServer by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isAndroid = remember { try { Class.forName("android.os.Build"); true } catch(_: Throwable) { false } }
    val navBarPadding = if (isAndroid) WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() else 0.dp
    val effectiveBottomPadding = if (bottomContentPadding > 0.dp) bottomContentPadding else navBarPadding

    val isRunning = connectionStatus == ConnectionStatus.RUNNING
    val socksHost = config.socksHost
    val socksPort = config.socksPort.toIntOrNull() ?: 1819

    DisposableEffect(Unit) {
        onDispose { SpeedTestRepository.cancelTest() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = (8 * 1f).dp,
                    end = (12 * 1f).dp,
                    top = if (isDesktop) 12.dp else 36.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                SpeedTestRepository.cancelTest()
                onBack()
            }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Speed Test",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 26.sp,
                    lineHeight = 30.sp
                )
                Text(
                    text = "Measure your internet connection performance",
                    style = MaterialTheme.typography.bodySmall,
                    color = IosSecondaryLabel,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = { showSettings = !showSettings }, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (showSettings) Icons.Default.ExpandLess else Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = IosActiveBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                bottom = 12.dp,
                top = 4.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showSettings) {
                item { SettingsPanel(state.config, { SpeedTestRepository.updateConfig(it) }) }
            }

            if (state.phase == SpeedTestPhase.PING || state.phase == SpeedTestPhase.DOWNLOAD || state.phase == SpeedTestPhase.UPLOAD) {
                item { ProgressCard(state) }
            }

            if (state.phase == SpeedTestPhase.COMPLETE) {
                item { ResultsCard(state.result, state.config) }
                item { ResultDetailsCard(state.result, state.config, onCopy) }
            }

            if (state.phase == SpeedTestPhase.ERROR) {
                item { ErrorCard(state.error) }
            }
        }

        val showGuide = state.phase == SpeedTestPhase.IDLE || state.phase == SpeedTestPhase.COMPLETE ||
            state.phase == SpeedTestPhase.ERROR || state.phase == SpeedTestPhase.CANCELLED

        AnimatedVisibility(
            visible = showGuide,
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (state.phase) {
                        SpeedTestPhase.COMPLETE -> "Done! Tap below to re-test."
                        SpeedTestPhase.ERROR -> "Failed. Try again or change server."
                        SpeedTestPhase.CANCELLED -> "Cancelled. Tap to restart."
                        else -> "Test your internet — download, upload, ping & jitter."
                    },
                    color = IosSecondaryLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(
                    start = 16.dp, end = 16.dp,
                    bottom = effectiveBottomPadding + 12.dp,
                    top = 8.dp
                )
        ) {
            if (state.phase == SpeedTestPhase.IDLE || state.phase == SpeedTestPhase.COMPLETE ||
                state.phase == SpeedTestPhase.ERROR || state.phase == SpeedTestPhase.CANCELLED) {
                Button(
                    onClick = {
                        if (checkingServer) return@Button
                        checkingServer = true
                        scope.launch(Dispatchers.IO) {
                            val throughProxy = isRunning
                            val needsCheck = state.config.selectedServer != SpeedTestServer.CLOUDFLARE
                            val reachable = !needsCheck || SpeedTestRepository.checkServerReachable(
                                state.config.selectedServer, state.config,
                                proxyHost = socksHost, proxyPort = socksPort, throughProxy = throughProxy
                            )
                            checkingServer = false
                            if (reachable) {
                                SpeedTestRepository.reset()
                                SpeedTestRepository.startTest(
                                    proxyHost = socksHost, proxyPort = socksPort, throughProxy = throughProxy
                                )
                            } else {
                                showServerUnavailable = true
                            }
                        }
                    },
                    enabled = !checkingServer,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (state.phase) {
                            SpeedTestPhase.COMPLETE -> IosActiveGreen
                            SpeedTestPhase.ERROR -> IosErrorRed
                            else -> IosActiveBlue
                        },
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        when (state.phase) {
                            SpeedTestPhase.COMPLETE -> Icons.Default.Refresh
                            SpeedTestPhase.ERROR -> Icons.Default.Refresh
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        when (state.phase) {
                            SpeedTestPhase.COMPLETE -> "Re-Test"
                            SpeedTestPhase.ERROR -> "Retry"
                            else -> if (checkingServer) "Checking server..." else "Start Speed Test"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { SpeedTestRepository.cancelTest() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IosErrorRed),
                    border = BorderStroke(1.5.dp, IosErrorRed.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Cancel Test", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showServerUnavailable) {
        ServerUnavailableDialog(
            serverName = state.config.selectedServer.displayName,
            onSwitchAndStart = {
                showServerUnavailable = false
                showSettings = false
                SpeedTestRepository.updateConfig(state.config.copy(selectedServer = SpeedTestServer.CLOUDFLARE))
                SpeedTestRepository.reset()
                SpeedTestRepository.startTest(
                    proxyHost = socksHost, proxyPort = socksPort, throughProxy = isRunning
                )
            },
            onDismiss = { showServerUnavailable = false }
        )
    }
}


@Composable
private fun ServerUnavailableDialog(
    serverName: String,
    onSwitchAndStart: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            val textScale = (maxWidth / 360.dp).coerceIn(0.75f, 1f)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = IosAmber,
                        modifier = Modifier.size((38 * textScale).dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Server Unavailable",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (17 * textScale).sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "The $serverName server is currently unreachable. Would you like to switch to Cloudflare and start the speed test?",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = (13 * textScale).sp,
                        lineHeight = (18 * textScale).sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onSwitchAndStart,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IosActiveBlue),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            "Switch to Cloudflare & Start",
                            fontWeight = FontWeight.Bold,
                            fontSize = (13 * textScale).sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text(
                            "Cancel",
                            color = IosSecondaryLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = (13 * textScale).sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun SettingsPanel(config: SpeedTestConfig, onUpdate: (SpeedTestConfig) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, null, tint = IosActiveBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("SERVER", color = IosSecondaryLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpeedTestServer.entries.forEach { server ->
                    val selected = config.selectedServer == server
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onUpdate(config.copy(selectedServer = server)) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) IosActiveBlue else IosGroupBg,
                        border = if (selected) BorderStroke(1.5.dp, IosActiveBlue) else BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                when (server) {
                                    SpeedTestServer.CLOUDFLARE -> Icons.Default.Cloud
                                    SpeedTestServer.OFAKIN -> Icons.Default.Storage
                                    SpeedTestServer.CUSTOM -> Icons.Default.Language
                                },
                                null,
                                tint = if (selected) Color.White else IosSecondaryLabel,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                server.displayName,
                                color = if (selected) Color.White else IosSecondaryLabel,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                server.description,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 8.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                lineHeight = 10.sp
                            )
                        }
                    }
                }
            }

            if (config.selectedServer == SpeedTestServer.CUSTOM) {
                Spacer(modifier = Modifier.height(10.dp))
                val focusManager = LocalFocusManager.current
                var urlFocused by remember { mutableStateOf(false) }
                BasicTextField(
                    value = config.customServerUrl,
                    onValueChange = { onUpdate(config.copy(customServerUrl = it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .border(
                            1.dp,
                            if (urlFocused) IosActiveBlue else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(10.dp)
                        )
                        .onFocusChanged { urlFocused = it.isFocused },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true,
                    cursorBrush = SolidColor(IosActiveBlue),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                            if (config.customServerUrl.isEmpty()) Text("https://your-server.example.com", color = IosSecondaryLabel, fontSize = 12.sp)
                            innerTextField()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Must support /__down?bytes=N and /__up endpoints", color = IosSecondaryLabel, fontSize = 9.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("DISPLAY UNIT", color = IosSecondaryLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(3.dp)
            ) {
                listOf(
                    "Bytes (MB/s)" to false,
                    "Bits (Mb/s)" to true
                ).forEach { (label, isBits) ->
                    val selected = config.showBits == isBits
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) IosActiveBlue else Color.Transparent)
                            .clickable { onUpdate(config.copy(showBits = isBits)) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (selected) Color.White else IosSecondaryLabel,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("TEST SIZE", color = IosSecondaryLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Download", color = IosSecondaryLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(3.dp)
                    ) {
                        listOf(10, 25, 50).forEach { size ->
                            val selected = config.downloadSizeMb == size
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) IosActiveBlue else Color.Transparent)
                                    .clickable { onUpdate(config.copy(downloadSizeMb = size)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${size}MB",
                                    color = if (selected) Color.White else IosSecondaryLabel,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Upload", color = IosSecondaryLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(3.dp)
                    ) {
                        listOf(5, 10, 20).forEach { size ->
                            val selected = config.uploadSizeMb == size
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) IosActiveBlue else Color.Transparent)
                                    .clickable { onUpdate(config.copy(uploadSizeMb = size)) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${size}MB",
                                    color = if (selected) Color.White else IosSecondaryLabel,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("PARALLEL DOWNLOAD STREAMS", color = IosSecondaryLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("More streams measure faster connections more accurately", color = IosSecondaryLabel.copy(alpha = 0.7f), fontSize = 9.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(3.dp)
            ) {
                listOf(1, 2, 3, 4, 5, 6).forEach { streams ->
                    val selected = config.downloadStreams == streams
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) IosActiveBlue else Color.Transparent)
                            .clickable { onUpdate(config.copy(downloadStreams = streams)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$streams",
                            color = if (selected) Color.White else IosSecondaryLabel,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AUTO UNIT", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Scale unit automatically (KB/s → MB/s → GB/s)", color = IosSecondaryLabel, fontSize = 10.sp)
                }
                Switch(
                    checked = config.autoUnit,
                    onCheckedChange = { onUpdate(config.copy(autoUnit = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = IosActiveGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = IosGroupBg
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("WARM-UP PINGS DISCARDED", color = IosSecondaryLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = IosActiveBlue.copy(alpha = 0.15f)
                ) {
                    Text(
                        "${config.pingWarmup}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = IosActiveBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("First samples include connection warm-up — discarding them improves accuracy", color = IosSecondaryLabel.copy(alpha = 0.7f), fontSize = 9.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = config.pingWarmup.toFloat(),
                onValueChange = { onUpdate(config.copy(pingWarmup = it.toInt())) },
                valueRange = 0f..5f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = IosActiveBlue,
                    activeTrackColor = IosActiveBlue,
                    inactiveTrackColor = IosGroupBg
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PING SAMPLES", color = IosSecondaryLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = IosActiveBlue.copy(alpha = 0.15f)
                ) {
                    Text(
                        "${config.pingSamples}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = IosActiveBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = config.pingSamples.toFloat(),
                onValueChange = { onUpdate(config.copy(pingSamples = it.toInt())) },
                valueRange = 5f..50f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = IosActiveBlue,
                    activeTrackColor = IosActiveBlue,
                    inactiveTrackColor = IosGroupBg
                )
            )
        }
    }
}


@Composable
private fun ProgressCard(state: SpeedTestState) {
    val phaseColor = when (state.phase) {
        SpeedTestPhase.PING -> IosAmber
        SpeedTestPhase.DOWNLOAD -> IosActiveGreen
        SpeedTestPhase.UPLOAD -> IosPurple
        else -> IosActiveBlue
    }
    val phaseName = when (state.phase) {
        SpeedTestPhase.PING -> "PING & JITTER"
        SpeedTestPhase.DOWNLOAD -> "DOWNLOAD"
        SpeedTestPhase.UPLOAD -> "UPLOAD"
        else -> "TESTING"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        border = BorderStroke(1.dp, phaseColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(phaseColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(phaseName, color = phaseColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.8.sp)
                }
                Text("${(state.progress * 100).toInt()}%", color = phaseColor, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = phaseColor,
                trackColor = IosGroupBg
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (state.phase) {
                SpeedTestPhase.PING -> LivePingStats(state)
                SpeedTestPhase.DOWNLOAD -> LiveDownloadStats(state, state.config)
                SpeedTestPhase.UPLOAD -> LiveUploadStats(state, state.config)
                else -> {}
            }

            if (state.currentStep.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.currentStep, color = IosSecondaryLabel, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LivePingStats(state: SpeedTestState) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(IosGroupBg)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CURRENT", color = IosSecondaryLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text(
                    if (state.livePingMs >= 0) "${state.livePingMs}" else "—",
                    color = when {
                        state.livePingMs < 0 -> IosSecondaryLabel
                        state.livePingMs < 50 -> IosActiveGreen
                        state.livePingMs < 100 -> IosAmber
                        else -> IosErrorRed
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
                Text("ms", color = IosSecondaryLabel, fontSize = 10.sp)
            }
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.08f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SAMPLES", color = IosSecondaryLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text("${state.livePingCount}", color = IosActiveBlue, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Text("of ${state.config.pingSamples}", color = IosSecondaryLabel, fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniStat("MIN", if (state.livePingMin >= 0) "${state.livePingMin}ms" else "—", IosActiveGreen, Modifier.weight(1f))
            MiniStat("AVG", if (state.livePingAvg >= 0) "${"%.1f".format(state.livePingAvg)}ms" else "—", IosActiveBlue, Modifier.weight(1f))
            MiniStat("MAX", if (state.livePingMax >= 0) "${state.livePingMax}ms" else "—", IosErrorRed, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LiveDownloadStats(state: SpeedTestState, config: SpeedTestConfig) {
    val formatSpeed = if (config.showBits) SpeedTestRepository::formatBitsPerSecond else SpeedTestRepository::formatBytesPerSecond
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(IosGroupBg)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SPEED", color = IosSecondaryLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text(
                    formatSpeed(state.liveDownloadBps),
                    color = IosActiveGreen,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
            }
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.08f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DOWNLOADED", color = IosSecondaryLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text(
                    SpeedTestRepository.formatBytes(state.liveDownloadTotal),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniStat("ELAPSED", "${state.livePhaseElapsed}s", IosActiveGreen, Modifier.weight(1f))
            MiniStat("SPEED", formatSpeed(state.liveDownloadBps), IosActiveBlue, Modifier.weight(1f))
            MiniStat("TOTAL", SpeedTestRepository.formatBytes(state.liveDownloadTotal), IosAmber, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LiveUploadStats(state: SpeedTestState, config: SpeedTestConfig) {
    val formatSpeed = if (config.showBits) SpeedTestRepository::formatBitsPerSecond else SpeedTestRepository::formatBytesPerSecond
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(IosGroupBg)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SPEED", color = IosSecondaryLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text(
                    formatSpeed(state.liveUploadBps),
                    color = IosPurple,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
            }
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.08f)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("UPLOADED", color = IosSecondaryLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text(
                    SpeedTestRepository.formatBytes(state.liveUploadTotal),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniStat("ELAPSED", "${state.livePhaseElapsed}s", IosPurple, Modifier.weight(1f))
            MiniStat("SPEED", formatSpeed(state.liveUploadBps), IosActiveBlue, Modifier.weight(1f))
            MiniStat("TOTAL", SpeedTestRepository.formatBytes(state.liveUploadTotal), IosAmber, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = IosSecondaryLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}




@Composable
private fun ResultsCard(result: SpeedTestResult, config: SpeedTestConfig) {
    val infiniteTransition = rememberInfiniteTransition(label = "resultGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = IosActiveGreen.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        border = BorderStroke(1.5.dp, IosActiveGreen.copy(alpha = glowAlpha))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Test Complete", color = IosActiveGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Server: ${result.serverName}", color = IosSecondaryLabel, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpeedGauge(
                    label = "DOWNLOAD",
                    display = speedDisplay(result.downloadBps, config),
                    color = IosActiveGreen
                )
                SpeedGauge(
                    label = "UPLOAD",
                    display = speedDisplay(result.uploadBps, config),
                    color = IosPurple
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(IosGroupBg)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PING", color = IosSecondaryLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${"%.1f".format(result.pingMs)}",
                        color = if (result.pingMs < 50) IosActiveGreen else if (result.pingMs < 100) IosAmber else IosErrorRed,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    )
                    Text("ms", color = IosSecondaryLabel, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(50.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("JITTER", color = IosSecondaryLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${"%.1f".format(result.jitterMs)}",
                        color = if (result.jitterMs < 5) IosActiveGreen else if (result.jitterMs < 15) IosAmber else IosErrorRed,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    )
                    Text("ms", color = IosSecondaryLabel, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            ConnectionVerdict(result)
        }
    }
}

private data class SpeedVerdict(
    val grade: String,
    val gradeColor: Color,
    val useCases: List<String>
)

private fun connectionVerdict(result: SpeedTestResult): SpeedVerdict {
    val mbps = result.downloadMbps
    val (grade, color) = when {
        mbps >= 100 -> "Excellent" to IosActiveGreen
        mbps >= 50 -> "Great" to IosActiveGreen
        mbps >= 25 -> "Good" to IosActiveBlue
        mbps >= 10 -> "Fair" to IosAmber
        mbps >= 5 -> "Slow" to IosAmber
        else -> "Very Slow" to IosErrorRed
    }
    val useCases = buildList {
        if (mbps >= 25) add("4K streaming")
        else if (mbps >= 10) add("HD streaming")
        else if (mbps >= 5) add("SD video")
        if (result.pingMs < 60 && result.jitterMs < 10 && mbps >= 10) add("Online gaming")
        else if (result.pingMs < 100 && result.jitterMs < 20) add("Casual gaming")
        if (mbps >= 3) add("Video calls")
        else add("Voice calls")
        if (mbps < 5) add(0, "Basic browsing")
    }.distinct()
    return SpeedVerdict(grade, color, useCases)
}

@Composable
private fun ConnectionVerdict(result: SpeedTestResult) {
    val verdict = connectionVerdict(result)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(verdict.gradeColor.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Star, null, tint = verdict.gradeColor, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    verdict.grade,
                    color = verdict.gradeColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "connection",
                    color = IosSecondaryLabel,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                verdict.useCases.joinToString(" • "),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (result.pingMs > 120 || result.jitterMs > 25) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Warning, null, tint = IosAmber, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SpeedGauge(label: String, display: Pair<String, String>, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = IosSecondaryLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            display.first,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp
        )
        if (display.second.isNotEmpty()) {
            Text(display.second, color = IosSecondaryLabel, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private fun speedDisplay(bps: Double, config: SpeedTestConfig): Pair<String, String> {
    return if (config.autoUnit) {
        val formatted = if (config.showBits)
            SpeedTestRepository.formatBitsPerSecond(bps)
        else
            SpeedTestRepository.formatBytesPerSecond(bps)
        val parts = formatted.split(" ")
        (parts.getOrNull(0) ?: formatted) to (parts.getOrNull(1) ?: "")
    } else {
        val value = if (config.showBits) bps * 8.0 / (1024.0 * 1024.0) else bps / (1024.0 * 1024.0)
        "%.2f".format(value) to if (config.showBits) "Mb/s" else "MB/s"
    }
}


@Composable
private fun ResultDetailsCard(result: SpeedTestResult, config: SpeedTestConfig, onCopy: (String) -> Unit) {
    val detailText = buildString {
        appendLine("Speed Test Results")
        appendLine("Server: ${result.serverName}")
        appendLine("─────────────────")
        appendLine("Download: ${if (config.showBits) SpeedTestRepository.formatBitsPerSecond(result.downloadBps) else SpeedTestRepository.formatBytesPerSecond(result.downloadBps)}")
        appendLine("Upload: ${if (config.showBits) SpeedTestRepository.formatBitsPerSecond(result.uploadBps) else SpeedTestRepository.formatBytesPerSecond(result.uploadBps)}")
        appendLine("Ping: ${"%.1f".format(result.pingMs)} ms")
        appendLine("Jitter: ${"%.1f".format(result.jitterMs)} ms")
        appendLine("Samples: ${result.pingSamples.size}")
        if (result.pingSamples.isNotEmpty()) {
            appendLine("─────────────────")
            appendLine("Min: ${result.pingSamples.minOrNull() ?: 0}ms")
            appendLine("Avg: ${"%.1f".format(result.pingSamples.average())}ms")
            appendLine("Max: ${result.pingSamples.maxOrNull() ?: 0}ms")
            appendLine("Med: ${result.pingSamples.sorted().let { it[it.size / 2] }}ms")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Detailed Results", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                IconButton(
                    onClick = { onCopy(detailText) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, "Copy results", tint = IosActiveBlue, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            DetailRow("Download Speed", speedDisplay(result.downloadBps, config).let { (v, u) -> if (u.isEmpty()) v else "$v $u" })
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 6.dp))
            DetailRow("Upload Speed", speedDisplay(result.uploadBps, config).let { (v, u) -> if (u.isEmpty()) v else "$v $u" })
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 6.dp))
            DetailRow("Ping (Median)", "${"%.1f".format(result.pingMs)} ms")
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 6.dp))
            DetailRow("Jitter (StdDev)", "${"%.1f".format(result.jitterMs)} ms")
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 6.dp))
            DetailRow("Ping Samples", "${result.pingSamples.size} measurements")
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 6.dp))
            DetailRow("Server", result.serverName)

            if (result.pingSamples.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text("PING SAMPLES", color = IosSecondaryLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("MIN", color = IosSecondaryLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${result.pingSamples.minOrNull() ?: 0}ms", color = IosActiveGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("AVG", color = IosSecondaryLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${"%.0f".format(result.pingSamples.average())}ms", color = IosActiveBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("MAX", color = IosSecondaryLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${result.pingSamples.maxOrNull() ?: 0}ms", color = IosErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("MED", color = IosSecondaryLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("${result.pingSamples.sorted().let { it[it.size / 2] }}ms", color = IosAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = IosSecondaryLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}


@Composable
private fun ErrorCard(error: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = IosErrorRed.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Error, null, tint = IosErrorRed, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Test Failed", color = IosErrorRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(error ?: "Unknown error", color = IosSecondaryLabel, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

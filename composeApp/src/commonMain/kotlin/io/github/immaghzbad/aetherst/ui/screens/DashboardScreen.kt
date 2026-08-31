package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.getSettings
import io.github.immaghzbad.aetherst.platform.getSystemUtils
import io.github.immaghzbad.aetherst.platform.isDesktop
import io.github.immaghzbad.aetherst.shared.data.IpInfo
import io.github.immaghzbad.aetherst.shared.data.PingState
import io.github.immaghzbad.aetherst.shared.model.AetherConfig
import io.github.immaghzbad.aetherst.shared.model.AetherProtocol
import io.github.immaghzbad.aetherst.shared.model.AppLanguage
import io.github.immaghzbad.aetherst.shared.model.ConnectionMode
import io.github.immaghzbad.aetherst.shared.model.ConnectionStatus
import io.github.immaghzbad.aetherst.shared.model.SessionTraffic
import io.github.immaghzbad.aetherst.shared.ui.localization.LocalAppStrings
import io.github.immaghzbad.aetherst.shared.ui.theme.AppTheme
import io.github.immaghzbad.aetherst.ui.components.LivePingSignalVisualizer
import io.github.immaghzbad.aetherst.ui.components.LiveTrafficSpeedChartWidget
import kotlinx.coroutines.launch

private val IosCardBg @Composable get() = AppTheme.colors.glassCardBg
private val IosGroupBg @Composable get() = AppTheme.colors.glassGroupBg
private val IosSecondaryLabel @Composable get() = AppTheme.colors.textSecondary
private val IosTextPrimary @Composable get() = AppTheme.colors.textPrimary
private val IosActiveGreen = Color(0xFF34C759)
private val IosActiveBlue = Color(0xFF007AFF)
private val IosScanningAmber = Color(0xFFFF9500)
private val IosErrorRed = Color(0xFFFF3B30)

@Composable
fun DashboardScreen(
    config: AetherConfig,
    connectionStatus: ConnectionStatus,
    elapsedSeconds: Long,
    sessionTraffic: SessionTraffic,
    ipInfo: IpInfo = IpInfo(),
    pingState: PingState = PingState(),
    appVersion: String = "1.0.0",
    onToggleVpn: () -> Unit,
    onUpdateProtocol: (AetherProtocol) -> Unit,
    onRefreshIpInfo: () -> Unit = {},
    onRefreshPing: () -> Unit = {},
    onCopy: (String) -> Unit = {},
    onOpenSettingsToZeroTrust: () -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
    platformContext: PlatformContext? = null
) {
    var showProxyOverlay by remember { mutableStateOf(true) }
    var showAdminRequiredDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var supportDialogAuto by remember { mutableStateOf(true) }
    val uriHandler = LocalUriHandler.current
    val settings = platformContext?.let { getSettings(it) }
    val strings = LocalAppStrings.current

    LaunchedEffect(Unit) {
        if (settings != null && !settings.getBoolean("support_dialog_dismissed", false)) {
            supportDialogAuto = true
            showSupportDialog = true
        }
    }
    val systemUtils = platformContext?.let { getSystemUtils(it) }

    LaunchedEffect(connectionStatus) {
        if (connectionStatus != ConnectionStatus.RUNNING) {
            showProxyOverlay = true
        }
    }

    val isDark = AppTheme.colors.isDark
    val bgColors = if (isDark) {
        listOf(
            Color(0xFF0C101A),
            Color(0xFF07090F),
            Color(0xFF030408)
        )
    } else {
        listOf(
            Color(0xFFF7F9FC),
            Color(0xFFEEF2F8),
            Color(0xFFE4EAF4)
        )
    }
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = bgColors))
    ) {
        val screenWidth = this.maxWidth
        val screenHeight = this.maxHeight
        val scaleFactor = (screenWidth.value / 411f).coerceIn(0.7f, 1.1f)
        val isCompactHeight = screenHeight < 640.dp
        val isVeryCompactHeight = screenHeight < 580.dp
        val horizontalPadding = if (screenWidth < 360.dp) 12.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = bottomContentPadding + 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
        ) {
            Column(
                modifier = Modifier.padding(top = if (isDesktop) 12.dp else 36.dp),
                verticalArrangement = Arrangement.spacedBy((14 * scaleFactor).dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = strings.tunnelTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = IosTextPrimary,
                            fontSize = (26 * scaleFactor).sp,
                            lineHeight = (30 * scaleFactor).sp
                        )
                        Text(
                            text = if (config.connectionMode == ConnectionMode.TUNNEL) strings.tunnelSubtitleTunnel else strings.tunnelSubtitleProxy,
                            style = MaterialTheme.typography.bodySmall,
                            color = IosSecondaryLabel,
                            fontSize = (12 * scaleFactor).sp,
                            lineHeight = (16 * scaleFactor).sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (config.connectionMode == ConnectionMode.PROXY_ONLY && connectionStatus == ConnectionStatus.RUNNING) {
                            IconButton(
                                onClick = { showProxyOverlay = true },
                                modifier = Modifier.size((32 * scaleFactor).dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Proxy Info",
                                    tint = IosActiveBlue,
                                    modifier = Modifier.size((22 * scaleFactor).dp)
                                )
                            }
                            Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = IosGroupBg,
                            border = BorderStroke(1.dp, AppTheme.colors.cardBorder),
                            modifier = Modifier.clickable {
                                supportDialogAuto = false
                                showSupportDialog = true
                            }
                        ) {
                            Text(
                                text = "v$appVersion",
                                modifier = Modifier.padding(horizontal = (12 * scaleFactor).dp, vertical = (6 * scaleFactor).dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = IosActiveBlue,
                                fontSize = (10 * scaleFactor).sp
                            )
                        }
                    }
                }

                IosStatusHeroCard(
                    connectionStatus = connectionStatus,
                    elapsedSeconds = elapsedSeconds,
                    sessionTraffic = sessionTraffic,
                    config = config,
                    ipInfo = ipInfo,
                    pingState = pingState,
                    onRefreshIpInfo = onRefreshIpInfo,
                    onRefreshPing = onRefreshPing,
                    onCopy = onCopy,
                    hideConfigChips = isCompactHeight,
                    scaleFactor = scaleFactor
                )

                if (!isVeryCompactHeight && config.showLivePingWidget) {
                    LivePingSignalVisualizer(
                        pingState = pingState,
                        connectionStatus = connectionStatus,
                        onRefreshPing = onRefreshPing,
                        scaleFactor = scaleFactor
                    )
                }

                if (!isVeryCompactHeight && connectionStatus == ConnectionStatus.ERROR) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = IosErrorRed.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, IosErrorRed.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = IosErrorRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                strings.connFailedMsg,
                                color = IosErrorRed,
                                fontSize = (11 * scaleFactor).sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 84.dp)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val isWindows = remember { try { System.getProperty("os.name")?.lowercase()?.contains("win") == true } catch (_: Throwable) { false } }
                val isAndroid = remember { !isDesktop }
                val handleToggle: () -> Boolean = {
                    if (config.protocol == AetherProtocol.ZERO_TRUST && connectionStatus == ConnectionStatus.STOPPED) {
                        onOpenSettingsToZeroTrust()
                        false
                    } else if (isWindows && config.connectionMode == ConnectionMode.TUNNEL && systemUtils?.isAdministrator() == false) {
                        showAdminRequiredDialog = true
                        false
                    } else {
                        onToggleVpn()
                        true
                    }
                }
                if ((isDesktop && isWindows) || isAndroid) {
                    WindowsSwipeSwitch(
                        connectionStatus = connectionStatus,
                        onToggle = handleToggle,
                        onAdminCancelResetKey = if (showAdminRequiredDialog) 1 else 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    val minDim = if (screenWidth < screenHeight) screenWidth else screenHeight
                    val buttonSize = (minDim * 0.35f).coerceIn(100.dp, 160.dp)
                    IosPowerButton(
                        connectionStatus = connectionStatus,
                        onToggle = { handleToggle().let {} },
                        size = buttonSize
                    )
                }
            }

            if (!isVeryCompactHeight) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy((10 * scaleFactor).dp)
                ) {
                    if (config.showTrafficChartWidget) {
                        LiveTrafficSpeedChartWidget(
                            sessionTraffic = sessionTraffic,
                            connectionStatus = connectionStatus,
                            scaleFactor = scaleFactor
                        )
                    }

                    IosProtocolSegmentedControl(
                        selectedProtocol = config.protocol,
                        onProtocolSelected = onUpdateProtocol,
                        enabled = connectionStatus == ConnectionStatus.STOPPED || connectionStatus == ConnectionStatus.ERROR,
                        scaleFactor = scaleFactor
                    )
                }
            }
        }

        val offsetY = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(showProxyOverlay) {
            if (showProxyOverlay) {
                offsetY.snapTo(0f)
            }
        }

        AnimatedVisibility(
            visible = config.connectionMode == ConnectionMode.PROXY_ONLY && connectionStatus == ConnectionStatus.RUNNING && showProxyOverlay,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 36.dp)
                .graphicsLayer { translationY = offsetY.value }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetY.value < -100f) {
                                    showProxyOverlay = false
                                } else {
                                    offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                            }
                        },
                        onVerticalDrag = { _, dragAmount ->
                            scope.launch {
                                offsetY.snapTo((offsetY.value + dragAmount).coerceAtMost(20f))
                            }
                        }
                    )
                }
        ) {
            ProxyOverlayPill(
                host = config.socksHost,
                socksPort = config.socksPort,
                httpPort = config.httpPort,
                onHide = { showProxyOverlay = false },
                onCopy = onCopy,
                scaleFactor = scaleFactor
            )
        }

        if (showAdminRequiredDialog) {
            AdminRequiredDialog(
                onRelaunch = {
                    showAdminRequiredDialog = false
                    systemUtils?.relaunchAsAdmin()
                },
                onDismiss = { showAdminRequiredDialog = false },
                scaleFactor = scaleFactor
            )
        }

        if (showSupportDialog) {
            SupportDialog(
                autoShow = supportDialogAuto,
                onJoin = {
                    settings?.putBoolean("support_dialog_dismissed", true)
                    showSupportDialog = false
                    uriHandler.openUri(TelegramChannelUrl)
                },
                onSkip = {
                    settings?.putBoolean("support_dialog_dismissed", true)
                    showSupportDialog = false
                },
                onCancel = { showSupportDialog = false },
                scaleFactor = scaleFactor
            )
        }
    }
}

private const val TelegramChannelUrl = "https://t.me/aliem_061"

@Composable
private fun SupportDialog(
    autoShow: Boolean,
    onJoin: () -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit,
    scaleFactor: Float
) {
    val strings = LocalAppStrings.current
    Dialog(
        onDismissRequest = { if (autoShow) onSkip() else onCancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = IosCardBg),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding((20 * scaleFactor).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        strings.labelSupportTitle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (18 * scaleFactor).sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height((10 * scaleFactor).dp))
                    Text(
                        strings.labelSupportMessage,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = (13 * scaleFactor).sp,
                        lineHeight = (18 * scaleFactor).sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height((20 * scaleFactor).dp))
                    Button(
                        onClick = onJoin,
                        modifier = Modifier.fillMaxWidth().height((48 * scaleFactor).dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IosActiveBlue, contentColor = Color.White)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            strings.labelSupportJoin,
                            fontWeight = FontWeight.Bold,
                            fontSize = (14 * scaleFactor).sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
                    TextButton(
                        onClick = { if (autoShow) onSkip() else onCancel() },
                        modifier = Modifier.fillMaxWidth().height((42 * scaleFactor).dp)
                    ) {
                        Text(
                            if (autoShow) strings.labelSupportDismiss else strings.actionCancel,
                            color = IosSecondaryLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = (13 * scaleFactor).sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminRequiredDialog(
    onRelaunch: () -> Unit,
    onDismiss: () -> Unit,
    scaleFactor: Float = 1f
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = (24 * scaleFactor).dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = (340 * scaleFactor).dp)
                    .fillMaxWidth()
                    .clickable(enabled = false) { },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding((24 * scaleFactor).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size((64 * scaleFactor).dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size((32 * scaleFactor).dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height((20 * scaleFactor).dp))
                    
                    Text(
                        text = "Admin Privileges Required",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = (20 * scaleFactor).sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height((12 * scaleFactor).dp))
                    
                    Text(
                        text = "TUN Mode requires administrator rights to create a virtual network interface. Please relaunch the app as Administrator.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = (14 * scaleFactor).sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height((32 * scaleFactor).dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
                    ) {
                        Button(
                            onClick = onRelaunch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((52 * scaleFactor).dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF007AFF),
                                contentColor = Color.White
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FlashOn, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Relaunch as Admin",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (15 * scaleFactor).sp
                                )
                            }
                        }
                        
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((52 * scaleFactor).dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium,
                                fontSize = (15 * scaleFactor).sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProxyOverlayPill(
    host: String,
    socksPort: String,
    httpPort: String,
    onHide: () -> Unit,
    onCopy: (String) -> Unit,
    scaleFactor: Float
) {
    val socksAddress = "$host:$socksPort"
    val httpAddress = "$host:$httpPort"

    Surface(
        modifier = Modifier
            .widthIn(max = 400.dp)
            .padding(horizontal = 8.dp)
            .shadow(24.dp, RoundedCornerShape(20.dp), spotColor = IosActiveBlue.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1C1C1E).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(IosActiveBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Dns, null, tint = IosActiveBlue, modifier = Modifier.size(20.dp))
            }
            
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ProxyCopyRow(
                    label = "SOCKS5",
                    address = socksAddress,
                    onCopy = {
                        onCopy(socksAddress)
                    },
                    scaleFactor = scaleFactor
                )
                ProxyCopyRow(
                    label = "HTTP",
                    address = httpAddress,
                    onCopy = {
                        onCopy(httpAddress)
                    },
                    scaleFactor = scaleFactor
                )
            }

            VerticalDivider(modifier = Modifier.height(36.dp), thickness = 1.dp, color = Color.White.copy(alpha = 0.1f))

            IconButton(
                onClick = onHide,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Close, null, tint = IosSecondaryLabel, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ProxyCopyRow(
    label: String,
    address: String,
    onCopy: () -> Unit,
    scaleFactor: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCopy() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall,
                color = IosActiveBlue,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (9 * scaleFactor).sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (12 * scaleFactor).sp,
                maxLines = 1
            )
        }
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy",
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size((14 * scaleFactor).dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosStatusHeroCard(
    connectionStatus: ConnectionStatus,
    elapsedSeconds: Long,
    sessionTraffic: SessionTraffic,
    config: AetherConfig,
    ipInfo: IpInfo = IpInfo(),
    pingState: PingState = PingState(),
    onRefreshIpInfo: () -> Unit = {},
    onRefreshPing: () -> Unit = {},
    onCopy: (String) -> Unit = {},
    hideConfigChips: Boolean = false,
    scaleFactor: Float = 1f
) {
    val strings = LocalAppStrings.current
    val statusColor by animateColorAsState(
        targetValue = when (connectionStatus) {
            ConnectionStatus.RUNNING -> IosActiveGreen
            ConnectionStatus.STARTING, ConnectionStatus.VALIDATING, ConnectionStatus.RECONNECTING, ConnectionStatus.STOPPING -> IosScanningAmber
            ConnectionStatus.ERROR -> IosErrorRed
            ConnectionStatus.STOPPED -> IosSecondaryLabel
        },
        label = "statusColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("status_hero_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        border = BorderStroke(1.dp, AppTheme.colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
                .padding((14 * scaleFactor).dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size((7 * scaleFactor).dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width((5 * scaleFactor).dp))
                        Text(
                            text = when (connectionStatus) {
                                ConnectionStatus.RUNNING -> if (config.connectionMode == ConnectionMode.TUNNEL) strings.statusProtectedConnected else strings.statusProxyActive
                                ConnectionStatus.STARTING -> strings.statusFindingServers
                                ConnectionStatus.VALIDATING -> strings.statusEstablishingLink
                                ConnectionStatus.RECONNECTING -> strings.statusReconnectingUpper
                                ConnectionStatus.STOPPING -> strings.statusStoppingUpper
                                ConnectionStatus.ERROR -> strings.statusErrorUpper
                                ConnectionStatus.STOPPED -> strings.statusReadyToConnect
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = statusColor,
                            fontSize = (8.5 * scaleFactor).sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = IosGroupBg,
                        border = BorderStroke(1.dp, AppTheme.colors.cardBorder)
                    ) {
                        val protocolText = if (config.protocol == AetherProtocol.MASQUE) {
                            if (config.h2Mode) "MASQUE (H2)" else "MASQUE (H3)"
                        } else {
                            config.protocol.displayName
                        }
                        Text(
                            text = protocolText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IosActiveBlue,
                            fontSize = (8.5 * scaleFactor).sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height((10 * scaleFactor).dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = formatTime(elapsedSeconds),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = IosTextPrimary,
                            fontSize = (28 * scaleFactor).sp
                        )
                    }

                    if (connectionStatus == ConnectionStatus.RUNNING) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onRefreshPing() }
                                .padding(2.dp)
                        ) {
                            if (pingState.isPinging) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size((11 * scaleFactor).dp),
                                    color = IosActiveBlue,
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Ping",
                                    tint = if (pingState.error != null) IosErrorRed else IosActiveBlue,
                                    modifier = Modifier.size((15 * scaleFactor).dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = when {
                                    pingState.isPinging -> "..."
                                    pingState.error != null -> "TIMEOUT"
                                    pingState.ms >= 0 -> "${pingState.ms}ms"
                                    else -> "PING"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (pingState.error != null) IosErrorRed else IosActiveBlue,
                                fontSize = (12 * scaleFactor).sp
                            )
                        }
                    } else {
                        Text(
                            text = if (connectionStatus == ConnectionStatus.RECONNECTING) "RETRY" else strings.labelNoUplink,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (connectionStatus == ConnectionStatus.RECONNECTING) IosScanningAmber else IosSecondaryLabel,
                            modifier = Modifier.clickable { onRefreshPing() },
                            fontSize = (10 * scaleFactor).sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height((8 * scaleFactor).dp))

                if (connectionStatus == ConnectionStatus.RUNNING) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(IosGroupBg)
                            .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TrafficValue(
                            label = strings.labelUploadUpper,
                            value = formatTrafficBytes(sessionTraffic.uploadedBytes),
                            speed = sessionTraffic.uploadSpeedBps,
                            color = IosActiveBlue,
                            alignment = Alignment.Start,
                            modifier = Modifier.weight(1f),
                            scaleFactor = scaleFactor
                        )
                        TrafficValue(
                            label = strings.labelDownloadUpper,
                            value = formatTrafficBytes(sessionTraffic.downloadedBytes),
                            speed = sessionTraffic.downloadSpeedBps,
                            color = IosActiveGreen,
                            alignment = Alignment.End,
                            modifier = Modifier.weight(1f),
                            scaleFactor = scaleFactor
                        )
                    }
                }

                Spacer(modifier = Modifier.height((8 * scaleFactor).dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .combinedClickable(
                            onClick = { onRefreshIpInfo() },
                            onLongClick = {
                                if (ipInfo.ip.isNotEmpty()) {
                                    onCopy(ipInfo.ip)
                                }
                            }
                        ),
                    color = IosGroupBg,
                    border = BorderStroke(1.dp, AppTheme.colors.cardBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (ipInfo.countryCode.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = IosActiveBlue.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = ipInfo.countryCode.uppercase(),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = IosActiveBlue,
                                        fontSize = (10 * scaleFactor).sp
                                    )
                                }
                            } else {
                                Text(
                                    text = "🌐",
                                    fontSize = (16 * scaleFactor).sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = when {
                                        ipInfo.country.isNotEmpty() -> if (ipInfo.countryCode.isNotEmpty()) "${ipInfo.country} (${ipInfo.countryCode})" else ipInfo.country
                                        ipInfo.isLoading -> "Wait..."
                                        ipInfo.error != null -> "Error"
                                        else -> "Unknown"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = IosTextPrimary,
                                    fontSize = (11 * scaleFactor).sp
                                )
                            Text(
                                text = when {
                                    ipInfo.ip.isNotEmpty() -> ipInfo.ip
                                    ipInfo.isLoading -> strings.labelLocatingIp
                                    ipInfo.error != null -> strings.labelCouldNotFindIp
                                    else -> strings.labelShowPublicIp
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    ipInfo.error != null -> IosErrorRed
                                    ipInfo.isLoading -> IosScanningAmber
                                    else -> IosSecondaryLabel
                                },
                                fontSize = (9 * scaleFactor).sp
                            )
                            }
                        }

                        if (ipInfo.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size((12 * scaleFactor).dp),
                                color = IosActiveBlue,
                                strokeWidth = 1.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = IosSecondaryLabel,
                                modifier = Modifier.size((12 * scaleFactor).dp)
                            )
                        }
                    }
                }

                if (!hideConfigChips) {
                    Spacer(modifier = Modifier.height((10 * scaleFactor).dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(IosGroupBg)
                            .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(10.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        IosConfigChip(label = strings.labelBypass, value = config.noise.displayName.split(" ")[0], scaleFactor = scaleFactor)
                        IosConfigChip(label = strings.labelSpeedUpper, value = config.scanMode.name.take(6), scaleFactor = scaleFactor)
                        IosConfigChip(label = strings.labelNetworkUpper, value = config.ipMode.rawValue, scaleFactor = scaleFactor)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrafficValue(label: String, value: String, speed: Double = 0.0, color: Color, alignment: Alignment.Horizontal, modifier: Modifier = Modifier, scaleFactor: Float = 1f) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = IosSecondaryLabel,
            fontSize = (8 * scaleFactor).sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = (12 * scaleFactor).sp
        )
        if (speed > 0) {
            Text(
                text = formatSpeedValue(speed),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color.copy(alpha = 0.7f),
                fontSize = (8 * scaleFactor).sp
            )
        }
    }
}

@Composable
fun IosConfigChip(label: String, value: String, scaleFactor: Float = 1f) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = IosSecondaryLabel, fontSize = (8 * scaleFactor).sp, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = IosTextPrimary, fontSize = (10 * scaleFactor).sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosPowerButton(
    connectionStatus: ConnectionStatus,
    onToggle: () -> Unit,
    size: Dp = 140.dp
) {
    val isConnected = connectionStatus == ConnectionStatus.RUNNING
    val isWorking = connectionStatus == ConnectionStatus.STARTING ||
                    connectionStatus == ConnectionStatus.VALIDATING ||
                    connectionStatus == ConnectionStatus.RECONNECTING ||
                    connectionStatus == ConnectionStatus.STOPPING
    val isError = connectionStatus == ConnectionStatus.ERROR
    val canToggle = connectionStatus != ConnectionStatus.STOPPING

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "refinedGlow")

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isWorking) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathingScale"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else if (isWorking) breathingScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "buttonScale"
    )

    val cornerRadiusPercent by animateFloatAsState(
        targetValue = if (isConnected || isWorking) 0.28f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cornerRadius"
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            isConnected -> IosActiveGreen
            isWorking -> IosScanningAmber
            isError -> IosErrorRed
            else -> IosActiveBlue
        },
        animationSpec = tween(durationMillis = 600),
        label = "buttonColor"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = if (isConnected) 1.8f else 1.5f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.02f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .size(size * 2.5f),
        contentAlignment = Alignment.Center
    ) {
        if (isWorking || isConnected) {
            val pulseColor = buttonColor.copy(alpha = 0.45f)
            val glowShape = RoundedCornerShape(size * cornerRadiusPercent)

            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = glowScale
                        scaleY = glowScale
                        alpha = glowAlpha
                    }
                    .background(pulseColor, glowShape)
            )

            if (isConnected) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .graphicsLayer {
                            scaleX = glowScale * 0.75f
                            scaleY = glowScale * 0.75f
                            alpha = glowAlpha * 1.8f
                        }
                        .background(pulseColor, glowShape)
                )
            }
        }

        Surface(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                }
                .shadow(
                    elevation = if (isPressed) 6.dp else 24.dp,
                    shape = RoundedCornerShape(size * cornerRadiusPercent),
                    ambientColor = buttonColor.copy(alpha = 0.6f),
                    spotColor = buttonColor
                )
                .clip(RoundedCornerShape(size * cornerRadiusPercent))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = canToggle,
                    onClick = {
                        scope.launch { onToggle() }
                    }
                ),
            color = buttonColor,
            tonalElevation = 14.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.45f)
                )
            }
        }
    }
}

@Composable
fun WindowsSwipeSwitch(
    connectionStatus: ConnectionStatus,
    onToggle: () -> Boolean,
    onAdminCancelResetKey: Int = 0,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isConnected = connectionStatus == ConnectionStatus.RUNNING
    val isWorking = connectionStatus == ConnectionStatus.STARTING ||
            connectionStatus == ConnectionStatus.VALIDATING ||
            connectionStatus == ConnectionStatus.RECONNECTING ||
            connectionStatus == ConnectionStatus.STOPPING
    val isError = connectionStatus == ConnectionStatus.ERROR
    val canSwipe = connectionStatus != ConnectionStatus.STOPPING
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val trackColor by animateColorAsState(
        targetValue = when {
            isConnected -> IosActiveGreen
            isWorking -> IosScanningAmber
            isError -> IosErrorRed
            else -> IosGroupBg
        }, label = "trackColor"
    )
    val text = when {
        isWorking -> when (connectionStatus) {
            ConnectionStatus.STARTING -> strings.statusConnecting
            ConnectionStatus.VALIDATING -> strings.statusValidating
            ConnectionStatus.RECONNECTING -> strings.statusReconnecting
            ConnectionStatus.STOPPING -> strings.statusStopping
            ConnectionStatus.RUNNING, ConnectionStatus.STOPPED, ConnectionStatus.ERROR -> strings.statusConnecting
        }
        isConnected -> strings.swipeToDisconnect
        isError -> strings.swipeToRetry
        else -> strings.swipeToConnect
    }
    val hintTransition = rememberInfiniteTransition(label = "hint")
    val hintShift by hintTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hintShift"
    )
    val dotTransition = rememberInfiniteTransition(label = "dots")
    val dotPhase by dotTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "dotPhase"
    )
    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier = modifier
                .widthIn(min = 280.dp, max = 360.dp)
                .height(72.dp)
                .shadow(14.dp, RoundedCornerShape(36.dp), spotColor = trackColor.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(36.dp))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
        val maxWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxWidth.toPx() }
        val thumbSize = 56.dp
        val thumbPx = with(androidx.compose.ui.platform.LocalDensity.current) { thumbSize.toPx() }
        val horizontalPadding = 8.dp
        val paddingPx = with(androidx.compose.ui.platform.LocalDensity.current) { horizontalPadding.toPx() }
        val maxDrag = (maxWidthPx - thumbPx - paddingPx * 2).coerceAtLeast(0f)
        val dragFraction = when {
            !canSwipe || maxDrag == 0f -> 0f
            isConnected -> (1f - offsetX.value / maxDrag).coerceIn(0f, 1f)
            isWorking -> (offsetX.value / maxDrag).coerceIn(0f, 1f)
            else -> 0f
        }
        val isDisconnectDrag = (isConnected || isWorking) && isDragging && dragFraction > 0.05f
        val effectiveTrackColor = if (isDisconnectDrag) lerp(trackColor, IosErrorRed, dragFraction) else trackColor

        LaunchedEffect(isConnected, isWorking, maxDrag) {
            if (isDragging) return@LaunchedEffect
            if (isWorking) {
                offsetX.snapTo(if (isConnected) maxDrag else 0f)
            } else {
                offsetX.animateTo(
                    targetValue = if (isConnected) maxDrag else 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                )
            }
        }
        LaunchedEffect(onAdminCancelResetKey) {
            if (!isConnected && !isWorking && offsetX.value != 0f && !isDragging) {
                offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(36.dp))
                .background(effectiveTrackColor.copy(alpha = if (isConnected || isDisconnectDrag) 1f else 0.95f))
                .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(36.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isDisconnectDrag) strings.releaseToDisconnect else text,
                color = if (isConnected || isDisconnectDrag || isWorking || isError) Color.White else IosTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 72.dp)
            )
        }

        val hintOffset = if (!isDragging && !isWorking) {
            if (!isConnected) hintShift else -hintShift
        } else 0f
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = horizontalPadding)
                .offset { androidx.compose.ui.unit.IntOffset((offsetX.value + hintOffset).toInt(), 0) }
                .size(thumbSize)
                .shadow(8.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(isConnected, isWorking, canSwipe, maxDrag) {
                        if (!canSwipe) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                scope.launch {
                                    val threshold = if (isWorking) maxDrag * 0.25f else maxDrag * 0.5f
                                    val shouldTrigger = if (isWorking) {
                                        if (!isConnected) offsetX.value > threshold else offsetX.value < maxDrag - threshold
                                    } else {
                                        if (!isConnected) offsetX.value > threshold else offsetX.value < threshold
                                    }
                                    if (shouldTrigger) {
                                        val success = onToggle()
                                        if (success) {
                                            if (isWorking) {
                                                offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                                            } else {
                                                offsetX.animateTo(
                                                    if (!isConnected) maxDrag else 0f,
                                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                                )
                                            }
                                        } else {
                                            offsetX.animateTo(
                                                if (isConnected) maxDrag else 0f,
                                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                            )
                                        }
                                    } else {
                                        offsetX.animateTo(
                                            if (isConnected) maxDrag else 0f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                scope.launch {
                                    offsetX.animateTo(
                                        if (isConnected) maxDrag else 0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                    )
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    val next = (offsetX.value + dragAmount).coerceIn(0f, maxDrag)
                                    offsetX.snapTo(next)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isWorking && !isDragging) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = effectiveTrackColor,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isConnected || isWorking) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = IosActiveBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        val hintElementColor = if (isConnected || isDisconnectDrag || isWorking || isError) Color.White else IosActiveBlue
        if (!isWorking && !isConnected) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
                    .graphicsLayer { translationX = if (!isDragging) hintShift * 0.6f else 0f },
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { idx ->
                    val alpha = 0.3f + ((dotPhase + idx * 0.33f) % 1f) * 0.7f
                    Box(
                        modifier = Modifier
                            .padding(start = if (idx == 0) 0.dp else 3.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(hintElementColor.copy(alpha = alpha.coerceIn(0.3f, 1f)))
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = hintElementColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp).padding(start = 4.dp)
                )
            }
        }
        if (!isWorking && isConnected) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
                    .graphicsLayer { translationX = if (!isDragging) -hintShift * 0.6f else 0f },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp).padding(end = 4.dp)
                )
                repeat(3) { idx ->
                    val alpha = 0.3f + ((1f - dotPhase + idx * 0.33f) % 1f) * 0.7f
                    Box(
                        modifier = Modifier
                            .padding(end = if (idx == 2) 0.dp else 3.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = alpha.coerceIn(0.3f, 1f)))
                    )
                }
            }
        }
    }
    }
}

@Composable
fun IosProtocolSegmentedControl(
    selectedProtocol: AetherProtocol,
    onProtocolSelected: (AetherProtocol) -> Unit,
    enabled: Boolean = true,
    scaleFactor: Float = 1f
) {
    Column {
        Text(
            text = "CONNECTION PROTOCOL",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = IosSecondaryLabel,
            fontSize = (9 * scaleFactor).sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = IosCardBg,
            shadowElevation = 8.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IosCardBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AetherProtocol.entries.forEach { proto ->
                    val selected = proto == selectedProtocol
                    val bg by animateColorAsState(
                        targetValue = if (selected) IosActiveBlue else Color.Transparent,
                        animationSpec = tween(250), label = "protoBg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (selected) Color.White else IosSecondaryLabel,
                        animationSpec = tween(250), label = "protoText"
                    )
                    val label = if (proto == AetherProtocol.ZERO_TRUST) "Z-TRUST" else proto.displayName.split(" ")[0].uppercase()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((36 * scaleFactor).dp)
                            .clip(RoundedCornerShape(50))
                            .background(bg)
                            .shadow(
                                elevation = if (selected) 10.dp else 0.dp,
                                shape = RoundedCornerShape(50),
                                spotColor = IosActiveBlue.copy(alpha = 0.4f),
                                ambientColor = IosActiveBlue.copy(alpha = 0.3f)
                            )
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = enabled) { onProtocolSelected(proto) }
                            .graphicsLayer { alpha = if (enabled || selected) 1f else 0.45f }
                            .testTag("protocol_${proto.rawValue}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.22f),
                                                Color.White.copy(alpha = 0.06f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = textColor,
                            fontSize = (10 * scaleFactor).sp,
                            letterSpacing = 0.3.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    
    fun pad(n: Long) = if (n < 10) "0$n" else n.toString()
    return "${pad(h)}:${pad(m)}:${pad(s)}"
}

private fun formatTrafficBytes(bytes: Long): String {
    val safeBytes = bytes.coerceAtLeast(0)
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    var value = safeBytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    
    
    val roundedValue = (value * 100).toLong() / 100.0
    return if (unitIndex == 0) {
        "$safeBytes ${units[unitIndex]}"
    } else {
        "$roundedValue ${units[unitIndex]}"
    }
}

private fun formatSpeedValue(bytesPerSec: Double): String {
    return when {
        bytesPerSec >= 1024.0 * 1024.0 * 1024.0 * 1024.0 -> "${"%.1f".format(bytesPerSec / (1024.0 * 1024.0 * 1024.0 * 1024.0))} TB/s"
        bytesPerSec >= 1024.0 * 1024.0 * 1024.0 -> "${"%.1f".format(bytesPerSec / (1024.0 * 1024.0 * 1024.0))} GB/s"
        bytesPerSec >= 1024.0 * 1024.0 -> "${"%.1f".format(bytesPerSec / (1024.0 * 1024.0))} MB/s"
        bytesPerSec >= 1024.0 -> "${"%.0f".format(bytesPerSec / 1024.0)} KB/s"
        else -> "${"%.0f".format(bytesPerSec)} B/s"
    }
}

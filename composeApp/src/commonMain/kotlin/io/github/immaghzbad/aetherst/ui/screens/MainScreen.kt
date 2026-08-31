package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.isDesktop
import io.github.immaghzbad.aetherst.platform.getSystemUtils
import io.github.immaghzbad.aetherst.platform.getDeviceModel
import io.github.immaghzbad.aetherst.platform.getOsVersion
import io.github.immaghzbad.aetherst.shared.desktop.TrayState
import io.github.immaghzbad.aetherst.shared.ui.AetherViewModel
import io.github.immaghzbad.aetherst.shared.ui.OnboardingViewModel
import io.github.immaghzbad.aetherst.shared.ui.components.IosToast
import io.github.immaghzbad.aetherst.shared.ui.components.PlatformBackHandler
import io.github.immaghzbad.aetherst.shared.ui.localization.LocalAppStrings
import io.github.immaghzbad.aetherst.shared.ui.theme.AppTheme
import io.github.immaghzbad.aetherst.shared.ui.theme.IosActiveBlue
import io.github.immaghzbad.aetherst.shared.ui.theme.IosSecondaryLabel
import io.github.immaghzbad.aetherst.shared.ui.theme.IosTextPrimary
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private val BarContentHeight = 90.dp
private val ButtonSize = 56.dp
private val ButtonCenterY = 20.dp
private val CircleGap = 6.dp
private val BarTopY = 20.dp
private val ItemBottomPadding = 12.dp

@Composable
fun MainScreen(viewModel: AetherViewModel, onboardingViewModel: OnboardingViewModel, platformContext: PlatformContext) {
    val isOnboardingComplete by viewModel.isOnboardingComplete.collectAsState()
    val onboardingState by onboardingViewModel.state.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    val crashLog by viewModel.crashLog.collectAsState()
    val toastState by viewModel.toastState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkBatteryOptimizationStatus()
    }

    LaunchedEffect(isOnboardingComplete) {
        while (true) {
            kotlinx.coroutines.delay(2000.milliseconds)
            viewModel.checkBatteryOptimizationStatus()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = this.maxWidth
        val scaleFactor = (screenWidth.value / 411f).coerceIn(0.7f, 1.1f)

        if (!isOnboardingComplete) {
            OnboardingScreen(
                state = onboardingState,
                onGetStarted = { onboardingViewModel.moveToNextStep() },
                onRetryRegistration = { onboardingViewModel.startProtocolTests() },
                onCancelRegistration = { onboardingViewModel.cancelTests() },
                onUpdateScanMode = { onboardingViewModel.updateScanMode(it) },
                onRequestVpnPermission = {
                    viewModel.prepareVpn { }
                    onboardingViewModel.onPermissionRequested()
                },
                onRequestNotificationPermission = {
                    viewModel.requestNotificationPermission()
                    onboardingViewModel.onPermissionRequested()
                },
                onRequestBatteryOptimization = {
                    viewModel.requestBatteryOptimization()
                    onboardingViewModel.onPermissionRequested()
                },
                onFinish = onboardingViewModel::moveToNextStep
            )
        } else if (crashLog != null) {
            CrashReportScreen(
                crashLog = crashLog!!,
                appVersion = viewModel.appVersion,
                platformName = if (isDesktop) "Windows Desktop" else "Android",
                deviceModel = try {
                    getDeviceModel()
                } catch (_: Exception) { "Unknown" },
                osVersion = try {
                    getOsVersion()
                } catch (_: Exception) { "Unknown" },
                onRestart = { viewModel.clearCrashLog() },
                onCopy = { viewModel.copyToClipboard(it) },
                onShare = { viewModel.shareLogs() },
                onShowToast = { viewModel.showToast(it) }
            )
        } else if (updateInfo != null) {
            UpdateScreen(
                info = updateInfo!!,
                onDismiss = { viewModel.dismissUpdate() },
                scaleFactor = scaleFactor
            )
        } else {
            DashboardContent(viewModel, scaleFactor, platformContext)
        }

        IosToast(
            message = toastState?.message,
            isError = toastState?.isError ?: false,
            scaleFactor = scaleFactor
        )
    }
}

@Composable
private fun DashboardContent(viewModel: AetherViewModel, scaleFactor: Float, platformContext: PlatformContext) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSplitTunneling by remember { mutableStateOf(false) }
    var showAutoDetect by remember { mutableStateOf(false) }
    var showSpeedTest by remember { mutableStateOf(false) }
    var openZeroTrustSettings by remember { mutableStateOf(false) }
    var showTrayAdminDialog by remember { mutableStateOf(false) }

    val config by viewModel.config.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val sessionTraffic by viewModel.sessionTraffic.collectAsState()
    val ipInfo by viewModel.ipInfo.collectAsState()
    val pingState by viewModel.pingState.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val isBatteryOptimized by viewModel.isBatteryOptimized.collectAsState()
    val importConflictRules by viewModel.importConflictRules.collectAsState()
    val importErrorMessage by viewModel.importErrorMessage.collectAsState()
    val isOptimizingMtu by viewModel.isOptimizingMtu.collectAsState()
    val isWaitingForLoginCode by viewModel.isWaitingForLoginCode.collectAsState()
    val scrollToZeroTrust by viewModel.scrollToZeroTrust.collectAsState()
    var showRoutingRules by remember { mutableStateOf(false) }

    val trayNavigateKey by TrayState.navigateToSettings.collectAsState()
    val trayAdminKey by TrayState.adminDialogRequest.collectAsState()
    LaunchedEffect(trayNavigateKey) {
        if (trayNavigateKey != 0L) {
            selectedTab = 1
            showSplitTunneling = false
            showRoutingRules = false
            showAutoDetect = false
            showSpeedTest = false
        }
    }
    LaunchedEffect(trayAdminKey) {
        if (trayAdminKey != 0L) {
            showTrayAdminDialog = true
        }
    }
    LaunchedEffect(scrollToZeroTrust) {
        if (scrollToZeroTrust) {
            selectedTab = 1
            showSplitTunneling = false
            showRoutingRules = false
        }
    }

    LaunchedEffect(openZeroTrustSettings) {
        if (openZeroTrustSettings) {
            selectedTab = 1
            showSplitTunneling = false
            showRoutingRules = false
            showAutoDetect = false
            showSpeedTest = false
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != 1 && openZeroTrustSettings) {
            openZeroTrustSettings = false
        }
    }

    PlatformBackHandler(enabled = showSplitTunneling || showRoutingRules || showAutoDetect || showSpeedTest) {
        showSplitTunneling = false
        showRoutingRules = false
        showAutoDetect = false
        showSpeedTest = false
    }

    val saveableStateHolder = rememberSaveableStateHolder()

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val totalNavBarHeight = BarContentHeight + navBarPadding

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            val targetScreen =
                if (showSpeedTest) 102 else if (showAutoDetect) 101 else if (showRoutingRules) 100 else if (showSplitTunneling) 99 else selectedTab
            AnimatedContent(
                targetState = targetScreen,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val duration = 250
                    val easing = FastOutSlowInEasing
                    if (targetState > initialState) {
                        (slideInHorizontally(
                            animationSpec = tween(
                                duration,
                                easing = easing
                            )
                        ) { it } + fadeIn(animationSpec = tween(duration, easing = easing)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(
                                        duration,
                                        easing = easing
                                    )
                                ) { -it / 2 } + fadeOut(
                                    animationSpec = tween(
                                        duration,
                                        easing = easing
                                    )
                                ))
                    } else {
                        (slideInHorizontally(
                            animationSpec = tween(
                                duration,
                                easing = easing
                            )
                        ) { -it } + fadeIn(animationSpec = tween(duration, easing = easing)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(
                                        duration,
                                        easing = easing
                                    )
                                ) { it / 2 } + fadeOut(
                                    animationSpec = tween(
                                        duration,
                                        easing = easing
                                    )
                                ))
                    }
                },
                label = "screen_transition"
            ) { tab ->
                Box(modifier = Modifier.fillMaxSize().graphicsLayer {
                    clip = false
                }) {
                    saveableStateHolder.SaveableStateProvider(tab) {
                        when (tab) {
                            0 -> DashboardScreen(
                                config = config,
                                connectionStatus = connectionStatus,
                                elapsedSeconds = elapsedSeconds,
                                sessionTraffic = sessionTraffic,
                                ipInfo = ipInfo,
                                pingState = pingState,
                                onToggleVpn = { viewModel.toggleVpn { } },
                                onUpdateProtocol = { proto ->
                                    viewModel.updateConfig(
                                        config.copy(
                                            protocol = proto
                                        )
                                    )
                                },
                                onRefreshIpInfo = { viewModel.refreshIpInfo() },
                                onRefreshPing = { viewModel.refreshPing() },
                                onCopy = { viewModel.copyToClipboard(it) },
                                onOpenSettingsToZeroTrust = { openZeroTrustSettings = true },
                                appVersion = viewModel.appVersion,
                                bottomContentPadding = totalNavBarHeight,
                                platformContext = platformContext
                            )

                            1 -> SettingsScreen(
                                config = config,
                                isBatteryOptimized = isBatteryOptimized,
                                scrollToSection = scrollToZeroTrust,
                                onSectionScrolled = { viewModel.onZeroTrustScrolled() },
                                onUpdateConfig = { viewModel.updateConfig(it) },
                                onUpdateTunnelEngine = { viewModel.updateTunnelEngine(it) },
                                onApplyPreset = { preset -> viewModel.applyPreset(preset) },
                                onOpenSplitTunneling = { showSplitTunneling = true },
                                onOpenRoutingRules = { showRoutingRules = true },
                                onOpenAutoDetect = { showAutoDetect = true },
                                onOpenSpeedTest = { showSpeedTest = true },
                                onResetAll = { viewModel.resetAllSettings() },
                                onExportBackup = { viewModel.exportFullBackup() },
                                onImportBackup = { viewModel.importFullBackup() },
                                onOptimizeMtu = { viewModel.optimizeMtu() },
                                onCopy = { viewModel.copyToClipboard(it) },
                                isOptimizingMtu = isOptimizingMtu,
                                onRequestBatteryOptimization = { viewModel.requestBatteryOptimization() },
                                onShowToast = { msg: String, err: Boolean -> viewModel.showToast(msg, err) },
                                initialPage = if (openZeroTrustSettings) SettingsPage.ZEROTRUST else null,
                                onSubPageClosed = { openZeroTrustSettings = false },
                                bottomContentPadding = totalNavBarHeight
                            )

                            2 -> LogsScreen(
                                viewModel = viewModel,
                                onShowToast = { msg: String, err: Boolean -> viewModel.showToast(msg, err) },
                                bottomContentPadding = totalNavBarHeight
                            )

                            3 -> AboutUsScreen(
                                appVersion = viewModel.appVersion,
                                bottomContentPadding = totalNavBarHeight
                            )
                            99 -> SplitTunnelingScreen(
                                apps = installedApps,
                                excludedPackages = config.excludedPackages,
                                blockedPackages = config.blockedPackages,
                                tunnelAllApps = config.tunnelAllApps,
                                onUpdateMode = { pkg, mode ->
                                    viewModel.updateAppSplitTunnelingMode(
                                        pkg,
                                        mode
                                    )
                                },
                                onBack = { showSplitTunneling = false },
                                scaleFactor = scaleFactor
                            )

                            102 -> SpeedTestScreen(
                                onBack = { showSpeedTest = false },
                                onCopy = { viewModel.copyToClipboard(it) },
                                bottomContentPadding = 0.dp,
                                connectionStatus = connectionStatus,
                                config = config
                            )

                            101 -> AutoDetectScreen(
                                onBack = { showAutoDetect = false },
                                onApplyResult = { result ->
                                    viewModel.applyAutoDetectResult(result)
                                    showAutoDetect = false
                                },
                                platformContext = platformContext,
                                bottomContentPadding = 0.dp
                            )

                            100 -> RoutingRulesScreen(
                                rules = config.routingRules,
                                importConflictRules = importConflictRules,
                                importErrorMessage = importErrorMessage,
                                onAddRule = { pattern, mode ->
                                    viewModel.addRoutingRule(
                                        pattern,
                                        mode
                                    )
                                },
                                onRemoveRule = { pattern -> viewModel.removeRoutingRule(pattern) },
                                onUpdateMode = { pattern, mode ->
                                    viewModel.updateRoutingRuleMode(
                                        pattern,
                                        mode
                                    )
                                },
                                onClearAllRules = { viewModel.clearAllRoutingRules() },
                                onCleanPattern = { viewModel.cleanRoutingPattern(it) },
                                onValidatePattern = { viewModel.isValidRoutingPattern(it) },
                                onExportRules = { viewModel.exportRoutingRules() },
                                onImportRules = { viewModel.importRoutingRules() },
                                onImportInternalRules = { viewModel.importInternalRoutingRules(it) },
                                onResolveConflict = { rules, replace ->
                                    viewModel.resolveConflict(
                                        rules,
                                        replace
                                    )
                                },
                                onCancelImport = { viewModel.cancelImport() },
                                onClearImportError = { viewModel.clearImportError() },
                                onShowToast = { msg: String, err: Boolean -> viewModel.showToast(msg, err) },
                                onBack = { showRoutingRules = false },
                                scaleFactor = scaleFactor
                            )
                        }
                    }
                }
            }
        }
        if (!showSplitTunneling && !showRoutingRules && !showAutoDetect && !showSpeedTest) {
            CurvedNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                scaleFactor = scaleFactor,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (isWaitingForLoginCode) {
            ZeroTrustLoginDialog(
                onSubmit = { viewModel.submitLoginCode(it) },
                onDismiss = { viewModel.submitLoginCode("") },
                scaleFactor = scaleFactor
            )
        }
        if (showTrayAdminDialog) {
            AdminRequiredDialog(
                onRelaunch = {
                    showTrayAdminDialog = false
                    getSystemUtils(platformContext).relaunchAsAdmin()
                },
                onDismiss = { showTrayAdminDialog = false },
                scaleFactor = scaleFactor
            )
        }
    }
}

@Composable
fun ZeroTrustLoginDialog(
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    scaleFactor: Float
) {
    var code by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

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
                    indication = null
                ) { focusManager.clearFocus() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width((320 * scaleFactor).dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(AppTheme.colors.cardBg)
                    .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(28.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(IosActiveBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = IosActiveBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Zero Trust Login",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = IosTextPrimary,
                    fontSize = (20 * scaleFactor).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A one-time code was sent to your email. Please enter it below to authorize this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IosSecondaryLabel,
                    textAlign = TextAlign.Center,
                    fontSize = (13 * scaleFactor).sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                BasicTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(AppTheme.colors.groupBg, RoundedCornerShape(14.dp))
                        .border(1.dp, AppTheme.colors.cardBorder, RoundedCornerShape(14.dp)),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        color = IosActiveBlue,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 8.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (code.length == 6) onSubmit(code)
                    }),
                    cursorBrush = SolidColor(IosActiveBlue),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (code.isEmpty()) {
                                Text(
                                    "000000",
                                    color = IosSecondaryLabel.copy(alpha = 0.3f),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 8.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", color = IosSecondaryLabel, fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = { if (code.length == 6) onSubmit(code) },
                        enabled = code.length == 6,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IosActiveBlue
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Verify", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurvedNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    scaleFactor: Float,
    modifier: Modifier = Modifier
) {
    val isAndroid = try { Class.forName("android.os.Build"); true } catch(_: Throwable) { false }
    val navBarPadding = if (isAndroid) WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() else 0.dp
    val isDark = AppTheme.colors.isDark
    val navActiveColor = AppTheme.colors.activeBlue
    val navInactiveColor = AppTheme.colors.textSecondary

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = modifier.fillMaxWidth().background(Color.Transparent)) {
            val scaledBarHeight = (BarContentHeight.value * scaleFactor).dp + navBarPadding
            val scaledButtonSize = (ButtonSize.value * scaleFactor).dp
            val scaledButtonCenterY = (ButtonCenterY.value * scaleFactor).dp
            val scaledCircleGap = (CircleGap.value * scaleFactor).dp
            val scaledBarTopY = (BarTopY.value * scaleFactor).dp
            val scaledItemBottomPadding = (ItemBottomPadding.value * scaleFactor).dp + navBarPadding

            val strings = LocalAppStrings.current
            val tabs = listOf(
                strings.tabDashboard to Icons.Default.Dashboard,
                strings.tabSettings to Icons.Default.Settings,
                strings.tabLogs to Icons.Default.Code,
                strings.tabAbout to Icons.Default.Info
            )
            val tabCount = tabs.size
            var barWidthPx by remember { mutableIntStateOf(0) }

            val indicatorOffset by animateFloatAsState(
                targetValue = selectedTab.toFloat(),
                animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
                label = "indicatorOffset"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scaledBarHeight)
                    .onSizeChanged { barWidthPx = it.width }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = (15 * scaleFactor).dp,
                            spotColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0x33000000)
                        )
                ) {
                    val tabWidth = size.width / tabCount
                    val centerX = (indicatorOffset * tabWidth) + (tabWidth / 2)
                    val barTop = scaledBarTopY.toPx()
                    val notchBottom =
                        scaledButtonCenterY.toPx() + (scaledButtonSize.toPx() / 2f) + scaledCircleGap.toPx()
                    val shoulderWidth = (45.dp.toPx() * scaleFactor)

                    val barShape = Path().apply {
                        moveTo(0f, barTop)
                        lineTo(centerX - shoulderWidth, barTop)

                        cubicTo(
                            centerX - (40.dp.toPx() * scaleFactor),
                            barTop,
                            centerX - (38.dp.toPx() * scaleFactor),
                            barTop + (2.dp.toPx() * scaleFactor),
                            centerX - (35.dp.toPx() * scaleFactor),
                            barTop + (10.dp.toPx() * scaleFactor)
                        )
                        cubicTo(
                            centerX - (28.dp.toPx() * scaleFactor),
                            barTop + (26.dp.toPx() * scaleFactor),
                            centerX - (20.dp.toPx() * scaleFactor),
                            notchBottom,
                            centerX,
                            notchBottom
                        )
                        cubicTo(
                            centerX + (20.dp.toPx() * scaleFactor),
                            notchBottom,
                            centerX + (28.dp.toPx() * scaleFactor),
                            barTop + (26.dp.toPx() * scaleFactor),
                            centerX + (35.dp.toPx() * scaleFactor),
                            barTop + (10.dp.toPx() * scaleFactor)
                        )
                        cubicTo(
                            centerX + (38.dp.toPx() * scaleFactor),
                            barTop + (2.dp.toPx() * scaleFactor),
                            centerX + (40.dp.toPx() * scaleFactor),
                            barTop,
                            centerX + shoulderWidth,
                            barTop
                        )

                        lineTo(size.width, barTop)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(
                        path = barShape,
                        brush = Brush.verticalGradient(
                            if (isDark) {
                                listOf(
                                    Color(0xFF1E2433).copy(alpha = 0.95f),
                                    Color(0xFF10141F).copy(alpha = 0.98f)
                                )
                            } else {
                                listOf(
                                    Color(0xFFFFFFFF).copy(alpha = 0.98f),
                                    Color(0xFFF1F4F9).copy(alpha = 0.98f)
                                )
                            }
                        ),
                        style = Fill
                    )
                    drawPath(
                        path = barShape,
                        color = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0x1F000000),
                        style = Stroke(width = (1 * scaleFactor).dp.toPx())
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(scaledButtonSize + (scaledCircleGap * 2))
                        .offset {
                            val tabWidth = barWidthPx.toFloat() / tabCount
                            val outerSize = scaledButtonSize.toPx() + scaledCircleGap.toPx() * 2f
                            IntOffset(
                                (indicatorOffset * tabWidth + (tabWidth / 2) - (outerSize / 2f)).roundToInt(),
                                (scaledButtonCenterY.toPx() - outerSize / 2f).roundToInt()
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val iconScale by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                        label = "iconScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(scaledButtonSize)
                            .shadow(
                                elevation = (16 * scaleFactor).dp,
                                shape = CircleShape,
                                spotColor = navActiveColor.copy(alpha = 0.8f)
                            )
                            .background(navActiveColor, CircleShape)
                            .border(
                                width = (1.5 * scaleFactor).dp,
                                color = Color.White.copy(alpha = 0.35f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tabs[selectedTab].second,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size((28 * scaleFactor).dp)
                                .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(scaledBarHeight)
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.Bottom
                ) {
                    tabs.forEachIndexed { index, (label, icon) ->
                        val isSelected = selectedTab == index

                        val contentAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.8f,
                            label = "contentAlpha"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onTabSelected(index) }
                                .padding(bottom = scaledItemBottomPadding),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.graphicsLayer(alpha = contentAlpha)
                            ) {
                                if (!isSelected) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = navInactiveColor.copy(alpha = 0.9f),
                                        modifier = Modifier.size((24 * scaleFactor).dp)
                                    )
                                    Spacer(modifier = Modifier.height((6 * scaleFactor).dp))
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = (10 * scaleFactor).sp,
                                    color = if (isSelected) navActiveColor else navInactiveColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.immaghzbad.aetherst.platform.isDesktop
import io.github.immaghzbad.aetherst.shared.model.*
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onGetStarted: () -> Unit,
    onRetryRegistration: () -> Unit,
    onCancelRegistration: () -> Unit,
    onUpdateScanMode: (AetherScanMode) -> Unit,
    onRequestVpnPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onFinish: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenWidth = this.maxWidth
        val scaleFactor = (screenWidth.value / 411f).coerceIn(0.7f, 1.1f)
        val horizontalPadding = (24 * scaleFactor).dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = (24 * scaleFactor).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            OnboardingHeader(scaleFactor)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = state.currentStep,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                    },
                    label = "step_transition"
                ) { step ->
                    when (step) {
                        OnboardingStep.WELCOME -> WelcomeStep(onGetStarted, scaleFactor)
                        OnboardingStep.PROTOCOL_TEST -> ProtocolTestStep(
                            state,
                            onRetryRegistration,
                            onCancelRegistration,
                            onUpdateScanMode,
                            onFinish,
                            scaleFactor
                        )
                        OnboardingStep.VPN_PERMISSION -> VpnPermissionStep(state, onRequestVpnPermission, scaleFactor)
                        OnboardingStep.NOTIFICATION_PERMISSION -> NotificationPermissionStep(state, onRequestNotificationPermission, scaleFactor)
                        OnboardingStep.BATTERY_OPTIMIZATION -> BatteryOptimizationStep(state, onRequestBatteryOptimization, onFinish, scaleFactor)
                        OnboardingStep.SUCCESS -> SuccessStep(onFinish, scaleFactor)
                        else -> Box(Modifier.fillMaxSize())
                    }
                }
            }

            OnboardingFooter(state.currentStep, scaleFactor)
        }
    }
}

@Composable
private fun OnboardingHeader(scaleFactor: Float) {
    val slogans = listOf(
        "Privacy at Warp Speed",
        "Beyond Boundaries, Beyond Limits",
        "Invisible, Untraceable, Unstoppable",
        "The Future of Secure Networking",
        "Your Digital Shield in the Shadows",
        "Encryption Without Compromise",
        "Defying Censorship, Ensuring Freedom",
        "Secure, Free, and Ad-free",
        "Secure Your Connection Instantly",
        "Total Freedom for Every User",
        "High-Performance Proxy Engine",
        "Advanced Protection Against Tracking",
        "Seamless Access to Global Content",
        "Reliable Security for Your Data",
        "Experience a Truly Open Internet",
        "Optimized for Low-Latency Browsing",
        "Your Trusted Companion for Privacy",
        "Fast, Secure, and Reliable"
    )
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000.milliseconds)
            index = (index + 1) % slogans.size
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height((48 * scaleFactor).dp))
        Text(
            text = "Aether Ng",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = (32 * scaleFactor).sp
        )
        Box(modifier = Modifier.height((24 * scaleFactor).dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = slogans[index],
                transitionSpec = {
                    (slideInVertically { it } + fadeIn(tween(600))) togetherWith
                            (slideOutVertically { -it } + fadeOut(tween(600)))
                },
                label = "slogan_animation"
            ) { slogan ->
                Text(
                    text = slogan,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8E8E93),
                    textAlign = TextAlign.Center,
                    fontSize = (14 * scaleFactor).sp
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(onGetStarted: () -> Unit, scaleFactor: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Aether Ng",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontSize = (28 * scaleFactor).sp
        )
        Spacer(modifier = Modifier.height((16 * scaleFactor).dp))
        Text(
            text = "Let’s prepare your secure connection in a few quick steps.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF8E8E93),
            textAlign = TextAlign.Center,
            fontSize = (16 * scaleFactor).sp
        )
        Spacer(modifier = Modifier.height((48 * scaleFactor).dp))
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().height((56 * scaleFactor).dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF), contentColor = Color.White)
        ) {
            Text("Get Started", fontSize = (18 * scaleFactor).sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun ProtocolTestStep(
    state: OnboardingState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onUpdateScanMode: (AetherScanMode) -> Unit,
    onContinue: () -> Unit,
    scaleFactor: Float
) {
    val allowedModes = listOf(AetherScanMode.TURBO, AetherScanMode.BALANCED, AetherScanMode.STEALTH, AetherScanMode.IRONCLAD)
    val allDone = !state.isProcessing && state.protocolResults.all {
        it.status == ProtocolTestStatus.CONNECTED ||
        it.status == ProtocolTestStatus.FAILED ||
        it.status == ProtocolTestStatus.TIMED_OUT ||
        it.status == ProtocolTestStatus.CANCELLED
    }
    val anySuccess = state.protocolResults.any { it.status == ProtocolTestStatus.CONNECTED }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Preparing Your Connection",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontSize = (22 * scaleFactor).sp
        )
        Spacer(modifier = Modifier.height((24 * scaleFactor).dp))

        SelectorLabel(scaleFactor)
        AetherScanModeSelector(
            selected = state.selectedScanMode,
            allowedModes = allowedModes,
            enabled = !state.isProcessing,
            onSelect = onUpdateScanMode,
            scaleFactor = scaleFactor
        )

        Spacer(modifier = Modifier.height((32 * scaleFactor).dp))

        state.protocolResults.forEach { result ->
            ProtocolRow(result.protocol.displayName, result.status, state.activeProtocol == result.protocol, scaleFactor)
            Spacer(modifier = Modifier.height((12 * scaleFactor).dp))
        }

        if (state.error != null) {
            Spacer(modifier = Modifier.height((16 * scaleFactor).dp))
            Text(text = state.error, color = Color(0xFFFF3B30), fontSize = (12 * scaleFactor).sp, textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height((32 * scaleFactor).dp))

        if (state.isProcessing) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E), contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height((56 * scaleFactor).dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancel Test", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (16 * scaleFactor).sp)
            }
        } else if (allDone && anySuccess) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height((56 * scaleFactor).dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759), contentColor = Color.White)
            ) {
                Text("Continue", fontWeight = FontWeight.Bold, color = Color.White, fontSize = (16 * scaleFactor).sp)
            }
        } else {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height((56 * scaleFactor).dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF), contentColor = Color.White)
            ) {
                Text(
                    text = if (state.error != null) "Try Again" else "Start Connection Test",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = (16 * scaleFactor).sp
                )
            }
        }
    }
}

@Composable
private fun SelectorLabel(scaleFactor: Float) {
    Text(
        text = "SCAN MODE",
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF8E8E93),
        fontSize = (11 * scaleFactor).sp,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = (8 * scaleFactor).dp)
    )
}

@Composable
private fun AetherScanModeSelector(
    selected: AetherScanMode,
    allowedModes: List<AetherScanMode>,
    enabled: Boolean,
    onSelect: (AetherScanMode) -> Unit,
    scaleFactor: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1C1C1E)).padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        allowedModes.forEach { mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFF007AFF) else Color.Transparent)
                    .clickable(enabled = enabled) { onSelect(mode) }
                    .padding(vertical = (8 * scaleFactor).dp),
                contentAlignment = Alignment.Center
            ) {
                val label = when(mode) {
                    AetherScanMode.TURBO -> "Turbo"
                    AetherScanMode.BALANCED -> "Balanced"
                    AetherScanMode.STEALTH -> "Stealth"
                    AetherScanMode.IRONCLAD -> "Ironclad"
                    else -> mode.name
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else Color(0xFF8E8E93),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = (12 * scaleFactor).sp
                )
            }
        }
    }
}

@Composable
private fun ProtocolRow(name: String, status: ProtocolTestStatus, isActive: Boolean, scaleFactor: Float) {
    Surface(
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding((16 * scaleFactor).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = (16 * scaleFactor).sp)
                if (isActive) {
                    Text(
                        text = when (status) {
                            ProtocolTestStatus.PREPARING -> "Preparing engine..."
                            ProtocolTestStatus.REGISTERING -> "Registering account..."
                            ProtocolTestStatus.IDENTITY_READY -> "Identity verified"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF007AFF),
                        fontSize = (11 * scaleFactor).sp
                    )
                }
            }

            when (status) {
                ProtocolTestStatus.WAITING -> Text("Waiting", color = Color(0xFF8E8E93), style = MaterialTheme.typography.labelSmall, fontSize = (11 * scaleFactor).sp)
                ProtocolTestStatus.CONNECTED -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF34C759), modifier = Modifier.size((20 * scaleFactor).dp))
                ProtocolTestStatus.FAILED, ProtocolTestStatus.TIMED_OUT -> Icon(Icons.Default.Error, null, tint = Color(0xFFFF3B30), modifier = Modifier.size((20 * scaleFactor).dp))
                ProtocolTestStatus.CANCELLED -> Text("Cancelled", color = Color(0xFF8E8E93), style = MaterialTheme.typography.labelSmall, fontSize = (11 * scaleFactor).sp)
                else -> CircularProgressIndicator(modifier = Modifier.size((20 * scaleFactor).dp), strokeWidth = 2.dp, color = Color(0xFF007AFF))
            }
        }
    }
}

@Composable
private fun PermissionStepWrapper(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    buttonLabel: String,
    state: OnboardingState,
    onRequest: () -> Unit,
    onSkip: (() -> Unit)?,
    scaleFactor: Float
) {
    val isVerifying = state.isVerifyingPermission
    val justGranted = state.permissionJustGranted

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size((72 * scaleFactor).dp).clip(CircleShape).background(Color(0xFF007AFF).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { icon() }
        Spacer(modifier = Modifier.height((24 * scaleFactor).dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = (24 * scaleFactor).sp)
        Spacer(modifier = Modifier.height((16 * scaleFactor).dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8E8E93), textAlign = TextAlign.Center, fontSize = (14 * scaleFactor).sp)
        Spacer(modifier = Modifier.height((40 * scaleFactor).dp))
        when {
            justGranted -> GrantedIndicator(scaleFactor)
            isVerifying -> VerifyingIndicator(scaleFactor)
            else -> {
                Button(onClick = onRequest, modifier = Modifier.fillMaxWidth().height((56 * scaleFactor).dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF), contentColor = Color.White)) {
                    Text(if (state.error != null) "Try Again" else buttonLabel, fontWeight = FontWeight.Bold, color = Color.White, fontSize = (16 * scaleFactor).sp)
                }
            }
        }
        if (state.error != null && !isVerifying && !justGranted) {
            Spacer(modifier = Modifier.height((16 * scaleFactor).dp))
            Text(state.error, color = Color(0xFFFF9500), fontSize = (13 * scaleFactor).sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
        }
        if (onSkip != null && !isVerifying && !justGranted) {
            Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
            TextButton(onClick = onSkip) { Text("Not Now", color = Color.White, fontSize = (14 * scaleFactor).sp) }
        }
    }
}

@Composable
private fun GrantedIndicator(scaleFactor: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size((56 * scaleFactor).dp))
        Spacer(modifier = Modifier.height((20 * scaleFactor).dp))
        Text("Access Granted", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF34C759), fontWeight = FontWeight.SemiBold, fontSize = (16 * scaleFactor).sp)
        Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
        Text("Continuing to next step...", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8E8E93), textAlign = TextAlign.Center, fontSize = (13 * scaleFactor).sp)
    }
}

@Composable
private fun VerifyingIndicator(scaleFactor: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "verifying")
    val alpha by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse), label = "pulse_alpha")
    val scale by infiniteTransition.animateFloat(initialValue = 0.85f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse_scale")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size((56 * scaleFactor).dp).graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale).clip(CircleShape).background(Color(0xFF007AFF).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size((28 * scaleFactor).dp), strokeWidth = 3.dp, color = Color(0xFF007AFF))
        }
        Spacer(modifier = Modifier.height((20 * scaleFactor).dp))
        Text("Verifying...", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold, fontSize = (16 * scaleFactor).sp)
        Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
        Text("Please complete the action, then return to the app.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8E8E93), textAlign = TextAlign.Center, fontSize = (13 * scaleFactor).sp)
    }
}

@Composable
private fun VpnPermissionStep(state: OnboardingState, onRequest: () -> Unit, scaleFactor: Float) {
    PermissionStepWrapper(icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size((32 * scaleFactor).dp)) }, title = "Allow VPN Access", description = "Aether Ng needs VPN permission to create a secure tunnel. Your current connection remains untouched for now.", buttonLabel = "Allow Access", state = state, onRequest = onRequest, onSkip = null, scaleFactor = scaleFactor)
}

@Composable
private fun NotificationPermissionStep(state: OnboardingState, onRequest: () -> Unit, scaleFactor: Float) {
    PermissionStepWrapper(icon = { Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size((32 * scaleFactor).dp)) }, title = "Stay Informed", description = "Enable notifications to see tunnel status and important updates.", buttonLabel = "Enable Notifications", state = state, onRequest = onRequest, onSkip = null, scaleFactor = scaleFactor)
}

@Composable
private fun BatteryOptimizationStep(state: OnboardingState, onRequest: () -> Unit, onSkip: () -> Unit, scaleFactor: Float) {
    PermissionStepWrapper(icon = { Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size((32 * scaleFactor).dp)) }, title = "Unrestricted Background Service", description = "To ensure a stable and persistent tunnel connection, please disable battery optimizations for Aether Ng.", buttonLabel = "Disable Restrictions", state = state, onRequest = onRequest, onSkip = onSkip, scaleFactor = scaleFactor)
}

@Composable
private fun SuccessStep(onFinish: () -> Unit, scaleFactor: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF34C759), modifier = Modifier.size((80 * scaleFactor).dp))
        Spacer(modifier = Modifier.height((24 * scaleFactor).dp))
        Text("Setup Complete", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = (24 * scaleFactor).sp)
        Spacer(modifier = Modifier.height((16 * scaleFactor).dp))
        Text(
            text = "Aether Ng is ready to protect your connection. You can now enter the dashboard and start the tunnel.",
            color = Color(0xFF8E8E93),
            textAlign = TextAlign.Center,
            fontSize = (14 * scaleFactor).sp
        )
        Spacer(modifier = Modifier.height((48 * scaleFactor).dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height((56 * scaleFactor).dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759), contentColor = Color.White)
        ) {
            Text("Start Secure Journey", fontWeight = FontWeight.Bold, color = Color.White, fontSize = (16 * scaleFactor).sp)
        }
    }
}

@Composable
private fun OnboardingFooter(currentStep: OnboardingStep, scaleFactor: Float) {
    Row(
        modifier = Modifier.padding(bottom = (32 * scaleFactor).dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val visibleSteps = if (isDesktop) {
            OnboardingStep.entries.filter {
                it != OnboardingStep.VPN_PERMISSION &&
                it != OnboardingStep.NOTIFICATION_PERMISSION &&
                it != OnboardingStep.BATTERY_OPTIMIZATION &&
                it != OnboardingStep.COMPLETED
            }
        } else {
            OnboardingStep.entries.filter { it != OnboardingStep.COMPLETED }
        }

        visibleSteps.forEach { step ->
            val isSelected = step == currentStep
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "indicator_width"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF007AFF) else Color(0xFF2C2C2E),
                animationSpec = tween(400),
                label = "indicator_color"
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

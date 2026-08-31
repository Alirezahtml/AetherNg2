package io.github.immaghzbad.aetherst.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.immaghzbad.aetherst.shared.data.PingState
import io.github.immaghzbad.aetherst.shared.model.ConnectionStatus
import io.github.immaghzbad.aetherst.shared.ui.localization.LocalAppStrings
import kotlin.math.sin

private val IosCardBg = Color(0xD9161B27)
private val IosGroupBg = Color(0x99232B3D)
private val IosSecondaryLabel = Color(0xFF8E95A5)
private val SignalGreen = Color(0xFF34C759)
private val SignalAmber = Color(0xFFFF9500)
private val SignalRed = Color(0xFFFF3B30)
private val SignalBlue = Color(0xFF007AFF)

@Composable
fun LivePingSignalVisualizer(
    pingState: PingState,
    connectionStatus: ConnectionStatus,
    onRefreshPing: () -> Unit,
    modifier: Modifier = Modifier,
    scaleFactor: Float = 1f
) {
    val strings = LocalAppStrings.current
    val isRunning = connectionStatus == ConnectionStatus.RUNNING
    val isValidating = connectionStatus == ConnectionStatus.VALIDATING || connectionStatus == ConnectionStatus.STARTING
    val isActive = isRunning || isValidating

    // Calculate latency status and color
    val pingMs = pingState.ms
    val (statusColor, statusText, signalBars) = when {
        !isActive -> Triple(IosSecondaryLabel, strings.latencyIdle, 1)
        pingState.isPinging -> Triple(SignalBlue, strings.statusValidating, 3)
        pingMs in 1..85 -> Triple(SignalGreen, strings.latencyOptimal, 4)
        pingMs in 86..180 -> Triple(SignalAmber, strings.latencyGood, 3)
        pingMs > 180 -> Triple(SignalRed, strings.latencyHigh, 2)
        pingState.error != null -> Triple(SignalRed, strings.statusErrorUpper, 1)
        else -> Triple(SignalGreen, strings.latencyOptimal, 3)
    }

    val animatedStatusColor by animateColorAsState(targetValue = statusColor, label = "ping_color")

    // Wave animation phases
    val infiniteTransition = rememberInfiniteTransition(label = "signal_waves")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive) (Math.PI * 2).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRunning) 1800 else 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_ping_signal_card"),
        shape = RoundedCornerShape((18 * scaleFactor).dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            animatedStatusColor.copy(alpha = if (isActive) 0.12f else 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding((12 * scaleFactor).dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy((10 * scaleFactor).dp)) {
                // Header row: Title + Signal Quality Badge + Test Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size((8 * scaleFactor).dp)
                                .clip(CircleShape)
                                .background(animatedStatusColor)
                        )
                        Spacer(modifier = Modifier.width((6 * scaleFactor).dp))
                        Text(
                            text = strings.livePingTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = (10.5 * scaleFactor).sp,
                            letterSpacing = 0.6.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((6 * scaleFactor).dp)
                    ) {
                        // Signal bars indicator
                        SignalBarsView(
                            activeBars = if (isActive) signalBars else 1,
                            barColor = animatedStatusColor,
                            scaleFactor = scaleFactor
                        )

                        // Tap to test / refresh button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IosGroupBg,
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !pingState.isPinging && isActive) {
                                    onRefreshPing()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (pingState.isPinging) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size((11 * scaleFactor).dp),
                                        color = SignalBlue,
                                        strokeWidth = 1.5.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Ping",
                                        tint = animatedStatusColor,
                                        modifier = Modifier.size((12 * scaleFactor).dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = animatedStatusColor,
                                    fontSize = (9 * scaleFactor).sp
                                )
                            }
                        }
                    }
                }

                // Middle row: Wave canvas visualizer + Ping metric readout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape((10 * scaleFactor).dp))
                        .background(IosGroupBg)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape((10 * scaleFactor).dp))
                        .padding(horizontal = (10 * scaleFactor).dp, vertical = (8 * scaleFactor).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Metric readout (Latency & estimated Jitter)
                    Column(modifier = Modifier.weight(1.1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (isActive && pingMs > 0) "$pingMs" else if (isActive && pingState.isPinging) "..." else "--",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isActive && pingMs > 0) animatedStatusColor else Color.White.copy(alpha = 0.5f),
                                fontSize = (22 * scaleFactor).sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ms",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = IosSecondaryLabel,
                                fontSize = (11 * scaleFactor).sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // Jitter and Stability indicator
                        val jitterMs = if (isActive && pingMs > 0) (pingMs * 0.08f).toInt().coerceAtLeast(1) else 0
                        Text(
                            text = if (isActive && pingMs > 0) "${strings.labelJitter}: ~${jitterMs}ms" else "${strings.labelLatency}: ${strings.latencyIdle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = IosSecondaryLabel,
                            fontSize = (10 * scaleFactor).sp
                        )
                    }

                    // Sine Wave / Oscilloscope Canvas
                    Box(
                        modifier = Modifier
                            .weight(1.4f)
                            .height((36 * scaleFactor).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val w = size.width
                            val h = size.height
                            val midY = h / 2f

                            if (isActive) {
                                // Draw 2 harmonized waves for rich signal depth
                                val path1 = Path()
                                val path2 = Path()

                                val amplitude = (h * 0.32f) * if (isRunning) 1f else 0.4f
                                val frequency = 2.2f

                                for (x in 0..w.toInt() step 2) {
                                    val xNorm = x / w
                                    val y1 = midY + sin(xNorm * Math.PI * 2 * frequency + phase).toFloat() * amplitude
                                    val y2 = midY + sin(xNorm * Math.PI * 2 * (frequency * 1.5f) - phase * 1.2f).toFloat() * (amplitude * 0.6f)

                                    if (x == 0) {
                                        path1.moveTo(0f, y1)
                                        path2.moveTo(0f, y2)
                                    } else {
                                        path1.lineTo(x.toFloat(), y1)
                                        path2.lineTo(x.toFloat(), y2)
                                    }
                                }

                                // Secondary wave (fainter)
                                drawPath(
                                    path = path2,
                                    color = animatedStatusColor.copy(alpha = 0.35f),
                                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Primary wave
                                drawPath(
                                    path = path1,
                                    color = animatedStatusColor.copy(alpha = 0.9f),
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            } else {
                                // Subtle flat heartbeat line with small center blip
                                val path = Path()
                                val blipX = w * 0.5f
                                path.moveTo(0f, midY)
                                path.lineTo(blipX - 16f, midY)
                                path.lineTo(blipX - 8f, midY - 6f)
                                path.lineTo(blipX, midY + 6f)
                                path.lineTo(blipX + 8f, midY - 3f)
                                path.lineTo(blipX + 16f, midY)
                                path.lineTo(w, midY)

                                drawPath(
                                    path = path,
                                    color = IosSecondaryLabel.copy(alpha = 0.35f),
                                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalBarsView(
    activeBars: Int,
    barColor: Color,
    scaleFactor: Float = 1f
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((2.5 * scaleFactor).dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height((14 * scaleFactor).dp)
    ) {
        val heights = listOf(4.dp, 7.dp, 10.dp, 13.dp)
        heights.forEachIndexed { index, h ->
            val isActive = index < activeBars
            Box(
                modifier = Modifier
                    .width((3 * scaleFactor).dp)
                    .height(h * scaleFactor)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isActive) barColor else Color.White.copy(alpha = 0.15f))
            )
        }
    }
}

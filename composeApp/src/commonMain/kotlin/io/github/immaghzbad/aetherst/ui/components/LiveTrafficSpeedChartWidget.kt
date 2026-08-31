package io.github.immaghzbad.aetherst.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.immaghzbad.aetherst.shared.model.ConnectionStatus
import io.github.immaghzbad.aetherst.shared.model.SessionTraffic
import io.github.immaghzbad.aetherst.shared.ui.localization.LocalAppStrings
import kotlinx.coroutines.delay

private val IosCardBg = Color(0xD9161B27)
private val IosGroupBg = Color(0x99232B3D)
private val IosSecondaryLabel = Color(0xFF8E95A5)
private val DownloadColor = Color(0xFF34C759)
private val DownloadCyan = Color(0xFF30D158)
private val UploadColor = Color(0xFF007AFF)

private const val MAX_HISTORY_POINTS = 16

@Composable
fun LiveTrafficSpeedChartWidget(
    sessionTraffic: SessionTraffic,
    connectionStatus: ConnectionStatus,
    modifier: Modifier = Modifier,
    scaleFactor: Float = 1f
) {
    val strings = LocalAppStrings.current
    val isRunning = connectionStatus == ConnectionStatus.RUNNING

    // Rolling history for download and upload speeds
    val downloadHistory = remember { mutableStateListOf<Float>().apply { repeat(MAX_HISTORY_POINTS) { add(0f) } } }
    val uploadHistory = remember { mutableStateListOf<Float>().apply { repeat(MAX_HISTORY_POINTS) { add(0f) } } }

    // Periodic updates to record smooth speed graph points
    LaunchedEffect(isRunning, sessionTraffic.downloadSpeedBps, sessionTraffic.uploadSpeedBps) {
        val currentDown = if (isRunning) sessionTraffic.downloadSpeedBps.toFloat().coerceAtLeast(0f) else 0f
        val currentUp = if (isRunning) sessionTraffic.uploadSpeedBps.toFloat().coerceAtLeast(0f) else 0f

        if (downloadHistory.size >= MAX_HISTORY_POINTS) {
            downloadHistory.removeAt(0)
        }
        downloadHistory.add(currentDown)

        if (uploadHistory.size >= MAX_HISTORY_POINTS) {
            uploadHistory.removeAt(0)
        }
        uploadHistory.add(currentUp)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_traffic_chart_card"),
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
                            DownloadColor.copy(alpha = if (isRunning) 0.08f else 0.03f),
                            Color.Transparent
                        )
                    )
                )
                .padding((12 * scaleFactor).dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)) {
                // Header row: Title + Live Speed Badges (Down & Up)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = if (isRunning) DownloadCyan else IosSecondaryLabel,
                            modifier = Modifier.size((14 * scaleFactor).dp)
                        )
                        Spacer(modifier = Modifier.width((6 * scaleFactor).dp))
                        Text(
                            text = strings.speedChartTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = (10.5 * scaleFactor).sp,
                            letterSpacing = 0.6.sp
                        )
                    }

                    // Live Speed indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Download badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = DownloadColor,
                                modifier = Modifier.size((12 * scaleFactor).dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = formatSpeedShort(sessionTraffic.downloadSpeedBps, isRunning),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isRunning) DownloadColor else IosSecondaryLabel,
                                fontSize = (9.5 * scaleFactor).sp
                            )
                        }

                        // Upload badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = UploadColor,
                                modifier = Modifier.size((12 * scaleFactor).dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = formatSpeedShort(sessionTraffic.uploadSpeedBps, isRunning),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isRunning) UploadColor else IosSecondaryLabel,
                                fontSize = (9.5 * scaleFactor).sp
                            )
                        }
                    }
                }

                // Live Speed Wave Canvas (Chart)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((48 * scaleFactor).dp)
                        .clip(RoundedCornerShape((10 * scaleFactor).dp))
                        .background(IosGroupBg.copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape((10 * scaleFactor).dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height
                        if (w <= 0 || h <= 0) return@Canvas

                        val maxSpeed = maxOf(
                            downloadHistory.maxOrNull() ?: 1f,
                            uploadHistory.maxOrNull() ?: 1f,
                            100_000f // 100 KB/s baseline minimum for smooth visual scale
                        )

                        // Draw Download wave & filled area
                        drawSpeedCurve(
                            points = downloadHistory,
                            maxVal = maxSpeed,
                            strokeColor = DownloadColor,
                            fillColor = DownloadColor.copy(alpha = 0.22f),
                            w = w,
                            h = h
                        )

                        // Draw Upload wave & filled area
                        drawSpeedCurve(
                            points = uploadHistory,
                            maxVal = maxSpeed,
                            strokeColor = UploadColor,
                            fillColor = UploadColor.copy(alpha = 0.15f),
                            w = w,
                            h = h
                        )
                    }
                }

                // Bottom row: Total Sent & Received metrics
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(IosGroupBg)
                        .border(0.8.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total Received
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(DownloadColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${strings.labelTotalRecv}: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = IosSecondaryLabel,
                            fontSize = (9.5 * scaleFactor).sp
                        )
                        Text(
                            text = formatBytesDisplay(sessionTraffic.downloadedBytes),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = (10 * scaleFactor).sp
                        )
                    }

                    // Total Sent
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(UploadColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${strings.labelTotalSent}: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = IosSecondaryLabel,
                            fontSize = (9.5 * scaleFactor).sp
                        )
                        Text(
                            text = formatBytesDisplay(sessionTraffic.uploadedBytes),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = (10 * scaleFactor).sp
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpeedCurve(
    points: List<Float>,
    maxVal: Float,
    strokeColor: Color,
    fillColor: Color,
    w: Float,
    h: Float
) {
    if (points.size < 2) return

    val stepX = w / (points.size - 1)
    val strokePath = Path()
    val fillPath = Path()

    fun getY(value: Float): Float {
        val normalized = (value / maxVal).coerceIn(0f, 1f)
        return h - (normalized * (h * 0.85f)) - (h * 0.05f)
    }

    val firstY = getY(points[0])
    strokePath.moveTo(0f, firstY)
    fillPath.moveTo(0f, h)
    fillPath.lineTo(0f, firstY)

    for (i in 0 until points.size - 1) {
        val x0 = i * stepX
        val y0 = getY(points[i])
        val x1 = (i + 1) * stepX
        val y1 = getY(points[i + 1])

        val cx = (x0 + x1) / 2f
        strokePath.cubicTo(cx, y0, cx, y1, x1, y1)
        fillPath.cubicTo(cx, y0, cx, y1, x1, y1)
    }

    fillPath.lineTo(w, h)
    fillPath.close()

    // Draw translucent gradient fill
    drawPath(path = fillPath, color = fillColor)

    // Draw glowing bezier line
    drawPath(
        path = strokePath,
        color = strokeColor,
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun formatSpeedShort(bytesPerSec: Double, isRunning: Boolean): String {
    if (!isRunning || bytesPerSec <= 0.0) return "0.0 KB/s"
    val kb = bytesPerSec / 1024.0
    return if (kb < 1024) {
        val rounded = ((kb * 10).toLong()) / 10.0
        "$rounded KB/s"
    } else {
        val mb = kb / 1024.0
        val rounded = ((mb * 10).toLong()) / 10.0
        "$rounded MB/s"
    }
}

private fun formatBytesDisplay(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "${((gb * 100).toLong()) / 100.0} GB"
        mb >= 1.0 -> "${((mb * 10).toLong()) / 10.0} MB"
        kb >= 1.0 -> "${((kb * 10).toLong()) / 10.0} KB"
        else -> "$bytes B"
    }
}

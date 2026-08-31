package io.github.immaghzbad.aetherst

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.UninstallCleanup
import io.github.immaghzbad.aetherst.platform.getSettings
import io.github.immaghzbad.aetherst.platform.getSystemUtils
import io.github.immaghzbad.aetherst.shared.App
import io.github.immaghzbad.aetherst.shared.core.ConnectionController
import io.github.immaghzbad.aetherst.shared.core.Elevation
import io.github.immaghzbad.aetherst.shared.core.NetworkHealer
import io.github.immaghzbad.aetherst.shared.data.AetherConfigRepository
import io.github.immaghzbad.aetherst.shared.desktop.AetherTray
import io.github.immaghzbad.aetherst.shared.desktop.TrayActions
import io.github.immaghzbad.aetherst.shared.desktop.TrayState
import io.github.immaghzbad.aetherst.shared.model.ConnectionMode
import io.github.immaghzbad.aetherst.shared.core.SingleInstanceLock
import io.github.immaghzbad.aetherst.shared.model.ConnectionStatus
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

private var lockSocket: ServerSocket? = null
private var requestShowWindow: (() -> Unit)? = null

private fun sweepChildProcesses() {
    runCatching {
        ProcessBuilder("taskkill", "/F", "/T", "/IM", "aether.exe")
            .redirectErrorStream(true).start().waitFor(5, TimeUnit.SECONDS)
    }
    runCatching {
        ProcessBuilder("taskkill", "/F", "/T", "/IM", "hev-socks5-tunnel.exe")
            .redirectErrorStream(true).start().waitFor(5, TimeUnit.SECONDS)
    }
}

private fun cleanTempFiles() {
    try {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "AetherST")
        if (tempDir.exists()) tempDir.walkBottomUp().forEach { it.delete() }
    } catch (_: Exception) {}
}

fun main() {
    NetworkHealer.heal()

    var bound = false
    for (attempt in 0..7) {
        try {
            val s = ServerSocket(18195)
            lockSocket = s
            SingleInstanceLock.socket = s
            bound = true
            break
        } catch (_: Exception) {
            if (attempt >= 7) break
            try { Thread.sleep(350) } catch (_: Exception) {}
        }
    }
    if (!bound) {
        runCatching {
            java.net.Socket("127.0.0.1", 18195).use { socket ->
                socket.getOutputStream().write(1)
                socket.getOutputStream().flush()
            }
        }
        exitProcess(0)
    }

    Thread({
        while (true) {
            val client = try { lockSocket?.accept() ?: break } catch (_: Exception) { break }
            runCatching { client.close() }
            try { requestShowWindow?.invoke() } catch (_: Exception) {}
        }
    }, "Aether-SingleInstance").apply { isDaemon = true }.start()

    Runtime.getRuntime().addShutdownHook(
        Thread {
            SingleInstanceLock.release()
            runCatching { lockSocket?.close() }
            AetherTray.uninstall()
            sweepChildProcesses()
            cleanTempFiles()
        }
    )

    application {
        val viewModelStoreOwner = remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }

        var isVisible by remember { mutableStateOf(true) }
        var showCloseDialog by remember { mutableStateOf(false) }

        val windowState = rememberWindowState(
            width = 432.dp,
            height = 784.dp,
            position = WindowPosition.Aligned(Alignment.Center)
        )

        remember {
            requestShowWindow = {
                isVisible = true
                windowState.isMinimized = false
            }
            true
        }

        val trayActions = remember {
            TrayActions(
                onShowWindow = {
                    isVisible = true
                    windowState.isMinimized = false
                },
                onToggleConnection = {
                    val context = PlatformContext()
                    val currentStatus = ConnectionController.status.value
                    if (currentStatus == ConnectionStatus.RUNNING ||
                        currentStatus == ConnectionStatus.RECONNECTING) {
                        ConnectionController.getImpl(context).stop()
                    } else {
                        val config = AetherConfigRepository.getInstance(getSettings(context)).config.value
                        val isAdmin = getSystemUtils(context).isAdministrator()
                        if (config.connectionMode == ConnectionMode.TUNNEL && !isAdmin) {
                            isVisible = true
                            windowState.isMinimized = false
                            TrayState.requestAdminDialog()
                        } else {
                            ConnectionController.getImpl(context).start()
                        }
                    }
                },
                onOpenSettings = {
                    isVisible = true
                    windowState.isMinimized = false
                    TrayState.requestSettings()
                },
                onOpenRouting = {
                    isVisible = true
                    windowState.isMinimized = false
                },
                onExit = {
                    val context = PlatformContext()
                    ConnectionController.getImpl(context).stop()
                    AetherTray.uninstall()
                    cleanTempFiles()
                    sweepChildProcesses()
                    SingleInstanceLock.release()
                    runCatching { lockSocket?.close() }
                    lockSocket = null
                    exitApplication()
                }
            )
        }

        LaunchedEffect(trayActions) {
            AetherTray.install(trayActions)
        }

        LaunchedEffect(Unit) {
            ConnectionController.status.collect { status ->
                AetherTray.setConnectionState(
                    status == io.github.immaghzbad.aetherst.shared.model.ConnectionStatus.RUNNING ||
                    status == io.github.immaghzbad.aetherst.shared.model.ConnectionStatus.RECONNECTING
                )
            }
        }

        if (isVisible) {
            Window(
                onCloseRequest = {
                    showCloseDialog = true
                },
                title = "Aether Ng Tunnel",
                state = windowState,
                resizable = false,
                undecorated = true,
                transparent = true,
                visible = isVisible
            ) {
                CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            WindowDraggableArea {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .background(Color(0xFF1C1C1E))
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color(0xFF007AFF), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "A",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "Aether Ng Tunnel",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .clickable { windowState.isMinimized = true }
                                                .background(Color.White.copy(alpha = 0.05f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Minimize",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .clickable { showCloseDialog = true }
                                                .background(Color(0xFFFF3B30).copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = Color(0xFFFF3B30),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                App(PlatformContext())
                            }
                        }
                        if (showCloseDialog) {
                            val traySupported = remember(showCloseDialog) { AetherTray.isSupported() && AetherTray.isInstalled() }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable(enabled = false) {},
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth().padding(16.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Close Aether Ng?",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (traySupported) "Hide to system tray or exit completely?" else "Do you want to exit Aether Ng Tunnel?",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (traySupported) {
                                                Button(
                                                    onClick = {
                                                        showCloseDialog = false
                                                        isVisible = false
                                                    },
                                                    modifier = Modifier.weight(1f).height(46.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF), contentColor = Color.White)
                                                ) {
                                                    Text("Hide to Tray", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 13.sp)
                                                }
                                            }
                                            Button(
                                                onClick = {
                                                    showCloseDialog = false
                                                    val context = PlatformContext()
                                                    ConnectionController.getImpl(context).stop()
                                                    AetherTray.uninstall()
                                                    cleanTempFiles()
                                                    sweepChildProcesses()
                                                    SingleInstanceLock.release()
                                                    runCatching { lockSocket?.close() }
                                                    lockSocket = null
                                                    exitApplication()
                                                },
                                                modifier = Modifier.weight(if (traySupported) 1f else 1f).height(46.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30), contentColor = Color.White)
                                            ) {
                                                Text("Exit", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        TextButton(
                                            onClick = { showCloseDialog = false },
                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Cancel", color = Color.White.copy(alpha = 0.6f), fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

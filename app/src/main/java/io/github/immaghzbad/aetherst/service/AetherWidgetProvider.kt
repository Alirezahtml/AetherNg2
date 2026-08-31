package io.github.immaghzbad.aetherst.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.graphics.toColorInt
import android.widget.RemoteViews
import io.github.immaghzbad.aetherst.R
import io.github.immaghzbad.aetherst.core.ConnectionController
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.getSettings
import io.github.immaghzbad.aetherst.shared.data.AetherConfigRepository
import io.github.immaghzbad.aetherst.shared.model.AetherProtocol
import io.github.immaghzbad.aetherst.shared.model.ConnectionMode
import io.github.immaghzbad.aetherst.shared.model.ConnectionStatus

class AetherWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "io.github.immaghzbad.aetherst.WIDGET_TOGGLE"
        const val ACTION_CHANGE_PROTOCOL = "io.github.immaghzbad.aetherst.WIDGET_CHANGE_PROTOCOL"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, AetherWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            val status = ConnectionController.status.value
            val config = AetherConfigRepository.getInstance(getSettings(PlatformContext(context))).config.value

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, status, config)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            status: ConnectionStatus,
            config: io.github.immaghzbad.aetherst.shared.model.AetherConfig
        ) {
            val views = RemoteViews(context.packageName, R.layout.aether_widget)

            val statusText = when (status) {
                ConnectionStatus.RUNNING -> when (config.connectionMode) {
                    ConnectionMode.TUNNEL -> "VPN Connected"
                    ConnectionMode.PROXY_ONLY -> {
                        if (config.httpProxyEnabled) {
                            "Proxy Active \u2022 SOCKS5 :${config.socksPort} \u2022 HTTP :${config.httpPort}"
                        } else {
                            "Proxy Active \u2022 SOCKS5 :${config.socksPort}"
                        }
                    }
                    ConnectionMode.SYSTEM_PROXY -> "Proxy Active \u2022 HTTP :${config.httpPort}"
                }
                ConnectionStatus.STARTING, ConnectionStatus.VALIDATING -> "Connecting..."
                ConnectionStatus.RECONNECTING -> "Reconnecting..."
                ConnectionStatus.STOPPING -> "Disconnecting..."
                ConnectionStatus.ERROR -> "Error"
                else -> "Disconnected"
            }

            val statusColor = when (status) {
                ConnectionStatus.RUNNING -> "#34C759".toColorInt()
                ConnectionStatus.STARTING, ConnectionStatus.VALIDATING, ConnectionStatus.RECONNECTING -> "#FF9500".toColorInt()
                ConnectionStatus.ERROR -> "#FF3B30".toColorInt()
                else -> "#8E8E93".toColorInt()
            }

            val buttonRes = when (status) {
                ConnectionStatus.RUNNING -> R.drawable.widget_button_green
                ConnectionStatus.STARTING, ConnectionStatus.VALIDATING, ConnectionStatus.RECONNECTING -> R.drawable.widget_button_orange
                else -> R.drawable.widget_button_blue
            }

            views.setTextViewText(R.id.widget_status, statusText)
            views.setTextColor(R.id.widget_status, statusColor)
            views.setImageViewResource(R.id.widget_button, android.R.drawable.ic_lock_power_off)
            views.setInt(R.id.widget_button, "setBackgroundResource", buttonRes)

            setupProtocolButton(context, views, R.id.proto_masque, AetherProtocol.MASQUE, config.protocol)
            setupProtocolButton(context, views, R.id.proto_wire, AetherProtocol.WG, config.protocol)
            setupProtocolButton(context, views, R.id.proto_gool, AetherProtocol.GOOL, config.protocol)

            val toggleIntent = Intent(context, AetherWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
            }
            val togglePending = PendingIntent.getBroadcast(
                context, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_button_container, togglePending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun setupProtocolButton(
            context: Context,
            views: RemoteViews,
            viewId: Int,
            protocol: AetherProtocol,
            currentProtocol: AetherProtocol
        ) {
            val isActive = protocol == currentProtocol
            val bgRes = if (isActive) R.drawable.widget_protocol_active_bg else R.drawable.widget_protocol_inactive_bg
            val textColor = if (isActive) "#007AFF".toColorInt() else "#8E8E93".toColorInt()
            
            views.setInt(viewId, "setBackgroundResource", bgRes)
            views.setTextColor(viewId, textColor)

            val intent = Intent(context, AetherWidgetProvider::class.java).apply {
                action = ACTION_CHANGE_PROTOCOL
                putExtra("protocol", protocol.name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, protocol.ordinal + 10, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(viewId, pendingIntent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val status = ConnectionController.status.value
        val config = AetherConfigRepository.getInstance(getSettings(PlatformContext(context))).config.value
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, status, config)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val repository = AetherConfigRepository.getInstance(getSettings(PlatformContext(context)))
        
        when (intent.action) {
            ACTION_TOGGLE -> {
                if (!repository.isOnboardingComplete.value) {
                    val launchIntent = Intent(context, io.github.immaghzbad.aetherst.MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(launchIntent)
                    return
                }

                val config = repository.config.value

                if (config.connectionMode == ConnectionMode.TUNNEL) {
                    if (android.net.VpnService.prepare(context) != null) {
                        val launchIntent = Intent(context, io.github.immaghzbad.aetherst.MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(launchIntent)
                        return
                    }
                }

                val status = ConnectionController.status.value
                if (status == ConnectionStatus.STARTING || status == ConnectionStatus.VALIDATING) {
                    return
                }

                if (status == ConnectionStatus.RUNNING || status == ConnectionStatus.RECONNECTING) {
                    if (config.connectionMode == ConnectionMode.TUNNEL) {
                        AetherVpnService.stopVpn(context)
                    } else {
                        AetherProxyService.stopProxy(context)
                    }
                } else {
                    if (config.connectionMode == ConnectionMode.TUNNEL) {
                        AetherVpnService.startVpn(context)
                    } else {
                        AetherProxyService.startProxy(context)
                    }
                }
            }
            ACTION_CHANGE_PROTOCOL -> {
                val protocolName = intent.getStringExtra("protocol") ?: return
                val nextProtocol = AetherProtocol.valueOf(protocolName)
                val currentConfig = repository.config.value

                if (currentConfig.protocol != nextProtocol) {
                    repository.updateConfig(currentConfig.copy(protocol = nextProtocol))

                    val status = ConnectionController.status.value
                    if (status == ConnectionStatus.RUNNING || status == ConnectionStatus.RECONNECTING) {
                        if (currentConfig.connectionMode == ConnectionMode.TUNNEL) {
                            AetherVpnService.stopVpn(context)
                            AetherVpnService.startVpn(context)
                        } else {
                            AetherProxyService.stopProxy(context)
                            AetherProxyService.startProxy(context)
                        }
                    }

                    updateAllWidgets(context)
                }
            }
        }
    }
}

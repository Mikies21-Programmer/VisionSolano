package com.example.visionsolano.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionsolano.data.model.ConnectionStatus
import com.example.visionsolano.data.model.DeviceState
import com.example.visionsolano.data.model.SystemMode
import com.example.visionsolano.ui.components.CameraViewport
import com.example.visionsolano.ui.components.DeviceStatusCard
import com.example.visionsolano.ui.components.EventItemCard
import com.example.visionsolano.ui.components.FpgaAnalysisPanel
import com.example.visionsolano.ui.components.SurveillanceIcons
import com.example.visionsolano.ui.theme.AlertRed
import com.example.visionsolano.ui.theme.CardBorderColor
import com.example.visionsolano.ui.theme.CyberCyan
import com.example.visionsolano.ui.theme.DarkBackground
import com.example.visionsolano.ui.theme.DarkSurface
import com.example.visionsolano.ui.theme.DarkSurfaceVariant
import com.example.visionsolano.ui.theme.StatusGreen
import com.example.visionsolano.ui.theme.TextPrimary
import com.example.visionsolano.ui.theme.TextSecondary
import com.example.visionsolano.ui.theme.WarningAmber
import com.example.visionsolano.viewmodel.SurveillanceViewModel

@Composable
fun DashboardScreen(
    viewModel: SurveillanceViewModel,
    onNavigateToCamera: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val systemStatus by viewModel.systemStatus.collectAsState()
    val fpgaAnalysis by viewModel.fpgaAnalysis.collectAsState()
    val recentEvents by viewModel.filteredEvents.collectAsState()
    val isStreamPaused by viewModel.isStreamPaused.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val snapshotFeedback by viewModel.snapshotFeedback.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(snapshotFeedback) {
        snapshotFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnapshotFeedback()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Main ESP-CAM Camera Card
            CameraViewport(
                isPaused = isStreamPaused,
                onTogglePause = { viewModel.toggleStreamPause() },
                onCapture = { viewModel.takeSnapshot() },
                onFullscreen = onNavigateToCamera,
                onSettings = onNavigateToSettings,
                isRecording = isRecording,
                fps = systemStatus.fps,
                resolution = systemStatus.resolution,
                connectionStatusText = if (systemStatus.connectionState == com.example.visionsolano.data.model.ConnectionState.Connected) "ENLACE ESP32 OK" else systemStatus.connectionState.label,
                streamUrl = viewModel.getStreamUrl(),
                isSimulated = systemStatus.isSimulated
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Section: Estado del sistema
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ESTADO DEL SISTEMA",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    if (systemStatus.isSimulated) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(WarningAmber.copy(alpha = 0.15f))
                                .border(1.dp, WarningAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SIMULADO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WarningAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Badge de versión de protocolo
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "PROTOCOLO v${systemStatus.protocolVersion}.0",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // 2x2 Grid of Status Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DeviceStatusCard(
                    deviceTitle = "FPGA (Nexys 4)",
                    statusLabel = systemStatus.fpgaState.label,
                    statusColor = if (systemStatus.fpgaState == DeviceState.ONLINE) StatusGreen else WarningAmber,
                    icon = SurveillanceIcons.Chip,
                    extraInfo = "Decisión FSM determinista",
                    modifier = Modifier.weight(1f)
                )

                DeviceStatusCard(
                    deviceTitle = "ESP32-S3",
                    statusLabel = if (systemStatus.esp32Online) "ONLINE" else "OFFLINE",
                    statusColor = if (systemStatus.esp32Online) StatusGreen else WarningAmber,
                    icon = SurveillanceIcons.Videocam,
                    extraInfo = if (systemStatus.cameraOnline) "Cámara OV2640 OK" else "Cámara Desconectada",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DeviceStatusCard(
                    deviceTitle = "Vigilancia",
                    statusLabel = systemStatus.systemMode.label,
                    statusColor = if (systemStatus.systemMode == SystemMode.ACTIVO) StatusGreen else TextSecondary,
                    icon = SurveillanceIcons.Shield,
                    extraInfo = "Modo Centinela Activo",
                    modifier = Modifier.weight(1f)
                )

                DeviceStatusCard(
                    deviceTitle = "mDNS / Enlace",
                    statusLabel = systemStatus.connectionState.label,
                    statusColor = systemStatus.connectionState.getColor(),
                    icon = SurveillanceIcons.Refresh,
                    extraInfo = systemStatus.discoveredDevice?.hostName ?: "Buscando servicio...",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fila de telemetría de latido y último contacto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (systemStatus.heartbeatAge < 3000L) StatusGreen else AlertRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Heartbeat: ${if (systemStatus.heartbeatAge > 0) "${systemStatus.heartbeatAge}ms" else "<1s"}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                Text(
                    text = if (systemStatus.isSimulated) {
                        "[SIMULADO] Latencia: ${systemStatus.latencyMs} ms | Wi-Fi: ${systemStatus.wifiSignalDbm} dBm"
                    } else {
                        "Latencia: ${systemStatus.latencyMs} ms | Wi-Fi: ${systemStatus.wifiSignalDbm} dBm"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (systemStatus.isSimulated) WarningAmber else (if (systemStatus.latencyMs < 20) StatusGreen else WarningAmber),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Section: Análisis FPGA
            FpgaAnalysisPanel(
                analysis = fpgaAnalysis,
                onSimulateToggle = {
                    if (fpgaAnalysis.movementDetected) {
                        viewModel.resetFpgaThreat()
                    } else {
                        viewModel.simulateFpgaThreat()
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Section: Alertas recientes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ALERTAS RECIENTES",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )

                Text(
                    text = "${recentEvents.size} Eventos",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recent 4 events list
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentEvents.take(4).forEach { event ->
                    EventItemCard(
                        event = event,
                        onClick = onNavigateToEvents
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button: Ver todas las alertas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { onNavigateToEvents() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Ver todas las alertas",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = SurveillanceIcons.History,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

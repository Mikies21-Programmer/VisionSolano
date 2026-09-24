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
                isRecording = isRecording
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Section: Estado del sistema
            Text(
                text = "ESTADO DEL SISTEMA",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            // 2x2 Grid of Status Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DeviceStatusCard(
                    deviceTitle = "FPGA",
                    statusLabel = systemStatus.fpgaState.label,
                    statusColor = if (systemStatus.fpgaState == DeviceState.ONLINE) StatusGreen else WarningAmber,
                    icon = SurveillanceIcons.Chip,
                    extraInfo = "Procesamiento Lógico OK",
                    modifier = Modifier.weight(1f)
                )

                DeviceStatusCard(
                    deviceTitle = "ESP-CAM",
                    statusLabel = systemStatus.espCamState.label,
                    statusColor = if (systemStatus.espCamState == DeviceState.ONLINE) StatusGreen else WarningAmber,
                    icon = SurveillanceIcons.Videocam,
                    extraInfo = "Sensor OV2640 Listo",
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
                    deviceTitle = "Conexión",
                    statusLabel = systemStatus.connectionStatus.label,
                    statusColor = when (systemStatus.connectionStatus) {
                        ConnectionStatus.CONECTADO -> StatusGreen
                        ConnectionStatus.NO_CONECTADO -> WarningAmber
                        ConnectionStatus.ENLAZANDO -> CyberCyan
                    },
                    icon = SurveillanceIcons.Info,
                    extraInfo = "Esperando Enlace HW",
                    modifier = Modifier.weight(1f)
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

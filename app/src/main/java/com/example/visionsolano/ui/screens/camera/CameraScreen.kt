package com.example.visionsolano.ui.screens.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionsolano.ui.components.StatusBadge
import com.example.visionsolano.ui.components.SurveillanceIcons
import com.example.visionsolano.ui.theme.AlertRed
import com.example.visionsolano.ui.theme.CardBorderColor
import com.example.visionsolano.ui.theme.CyberCyan
import com.example.visionsolano.ui.theme.DarkBackground
import com.example.visionsolano.ui.theme.DarkSurface
import com.example.visionsolano.ui.theme.DarkSurfaceElevated
import com.example.visionsolano.ui.theme.DarkSurfaceVariant
import com.example.visionsolano.ui.theme.HudGrid
import com.example.visionsolano.ui.theme.HudReticle
import com.example.visionsolano.ui.theme.StatusGreen
import com.example.visionsolano.ui.theme.TextPrimary
import com.example.visionsolano.ui.theme.TextSecondary
import com.example.visionsolano.ui.theme.WarningAmber
import com.example.visionsolano.viewmodel.SurveillanceViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CameraScreen(
    viewModel: SurveillanceViewModel,
    modifier: Modifier = Modifier
) {
    val isStreamPaused by viewModel.isStreamPaused.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingSeconds by viewModel.recordingSeconds.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val isNightVision by viewModel.isNightVisionActive.collectAsState()
    val isFlashlight by viewModel.isFlashlightActive.collectAsState()
    val isGridVisible by viewModel.isGridVisible.collectAsState()
    val snapshotFeedback by viewModel.snapshotFeedback.collectAsState()
    val systemStatus by viewModel.systemStatus.collectAsState()
    val streamUrl = viewModel.getStreamUrl()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var isFullscreenSimulated by remember { mutableStateOf(false) }
    var flashVisible by remember { mutableStateOf(false) }

    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        }
    }

    LaunchedEffect(snapshotFeedback) {
        snapshotFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnapshotFeedback()
        }
    }

    val recTransition = rememberInfiniteTransition(label = "camRecPulse")
    val recAlpha by recTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "camRecAlpha"
    )

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ESP-CAM LIVE",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(
                            text = if (isStreamPaused) "PAUSADO" else "LIVE",
                            accentColor = if (isStreamPaused) WarningAmber else AlertRed,
                            isPulsing = !isStreamPaused
                        )
                    }
                    Text(
                        text = "Canal de Video Primario | Sensor ${systemStatus.discoveredDevice?.camera?.uppercase() ?: "OV2640"}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Wi-Fi: ${systemStatus.wifiSignalDbm} dBm",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = StatusGreen,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Massive Video Frame Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isFullscreenSimulated) 16f / 12f else 16f / 10f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isNightVision) Color(0xFF04140D) else DarkBackground)
                    .border(
                        1.dp,
                        if (isRecording) AlertRed.copy(alpha = 0.7f) else CardBorderColor,
                        RoundedCornerShape(16.dp)
                    )
            ) {
                // Tactical HUD Canvas
                if (isGridVisible) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cornerLen = 34f
                        val strokeW = 3f

                        val reticleColor = if (isNightVision) StatusGreen.copy(alpha = 0.7f) else HudReticle
                        val gridColor = if (isNightVision) StatusGreen.copy(alpha = 0.2f) else HudGrid

                        // 4 Corners
                        drawLine(reticleColor, Offset(16f, 16f), Offset(16f + cornerLen, 16f), strokeW)
                        drawLine(reticleColor, Offset(16f, 16f), Offset(16f, 16f + cornerLen), strokeW)

                        drawLine(reticleColor, Offset(w - 16f, 16f), Offset(w - 16f - cornerLen, 16f), strokeW)
                        drawLine(reticleColor, Offset(w - 16f, 16f), Offset(w - 16f, 16f + cornerLen), strokeW)

                        drawLine(reticleColor, Offset(16f, h - 16f), Offset(16f + cornerLen, h - 16f), strokeW)
                        drawLine(reticleColor, Offset(16f, h - 16f), Offset(16f, h - 16f - cornerLen), strokeW)

                        drawLine(reticleColor, Offset(w - 16f, h - 16f), Offset(w - 16f - cornerLen, h - 16f), strokeW)
                        drawLine(reticleColor, Offset(w - 16f, h - 16f), Offset(w - 16f, h - 16f - cornerLen), strokeW)

                        // Center reticle
                        val cx = w / 2f
                        val cy = h / 2f
                        drawCircle(reticleColor, radius = 24f * zoomLevel, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                        drawLine(gridColor, Offset(cx - 30f, cy), Offset(cx + 30f, cy), 1.5f)
                        drawLine(gridColor, Offset(cx, cy - 30f), Offset(cx, cy + 30f), 1.5f)
                    }
                }

                // Top stream telemetry badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isRecording) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AlertRed.copy(alpha = 0.25f))
                                    .border(1.dp, AlertRed, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AlertRed.copy(alpha = recAlpha))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val mins = recordingSeconds / 60
                                val secs = recordingSeconds % 60
                                Text(
                                    text = "REC %02d:%02d".format(mins, secs),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AlertRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isStreamPaused) "PAUSA" else "${systemStatus.fps} FPS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isStreamPaused) WarningAmber else StatusGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = systemStatus.resolution,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        if (systemStatus.isSimulated) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(WarningAmber.copy(alpha = 0.2f))
                                    .border(1.dp, WarningAmber.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
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

                    // Timestamp
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentTime,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Center placeholder
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (isNightVision) StatusGreen.copy(alpha = 0.3f) else CyberCyan.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.5.dp, if (isNightVision) StatusGreen else CyberCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SurveillanceIcons.Videocam,
                            contentDescription = "Sensor",
                            tint = if (isNightVision) StatusGreen else CyberCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "ESP-CAM LIVE VIEW",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isNightVision) "Modo Infrarrojo / Visión Nocturna" else if (isStreamPaused) "Flujo detenido" else "Stream: $streamUrl",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isNightVision) StatusGreen else TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                // Bottom badges inside frame
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ZOOM: ${zoomLevel.toInt()}x",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (zoomLevel > 1f) WarningAmber else TextPrimary
                                )
                            )
                        }

                        if (isFlashlight) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(WarningAmber.copy(alpha = 0.25f))
                                    .border(1.dp, WarningAmber, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "FLASH LED ENCENDIDO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = WarningAmber,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "SOCKET: STANDBY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = StatusGreen,
                                fontSize = 9.sp
                            )
                        )
                    }
                }

                // Shutter flash
                if (flashVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.8f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Tactical Controls
            Text(
                text = "CONTROLES DE CÁMARA",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row: Captura, Grabación, Pausa, Pantalla Completa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Snapshot Button
                CameraDeckButton(
                    title = "Captura",
                    subtitle = "Instantánea",
                    icon = SurveillanceIcons.Camera,
                    accentColor = CyberCyan,
                    onClick = {
                        flashVisible = true
                        viewModel.takeSnapshot()
                    },
                    modifier = Modifier.weight(1f)
                )

                // Recording Button
                CameraDeckButton(
                    title = if (isRecording) "Detener" else "Grabar",
                    subtitle = if (isRecording) "Grabando..." else "Video local",
                    icon = SurveillanceIcons.FiberManualRecord,
                    accentColor = if (isRecording) AlertRed else TextPrimary,
                    isActive = isRecording,
                    onClick = { viewModel.toggleRecording() },
                    modifier = Modifier.weight(1f)
                )

                // Pause Button
                CameraDeckButton(
                    title = if (isStreamPaused) "Reanudar" else "Pausa",
                    subtitle = "Stream",
                    icon = if (isStreamPaused) SurveillanceIcons.Play else SurveillanceIcons.Pause,
                    accentColor = WarningAmber,
                    isActive = isStreamPaused,
                    onClick = { viewModel.toggleStreamPause() },
                    modifier = Modifier.weight(1f)
                )

                // Fullscreen Toggle
                CameraDeckButton(
                    title = if (isFullscreenSimulated) "Restaurar" else "Expandir",
                    subtitle = "Vista",
                    icon = SurveillanceIcons.Fullscreen,
                    accentColor = CyberCyan,
                    isActive = isFullscreenSimulated,
                    onClick = { isFullscreenSimulated = !isFullscreenSimulated },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Zoom Selector Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NIVEL DE ZOOM ÓPTICO/DIGITAL",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1f, 2f, 4f).forEach { level ->
                        val isSelected = zoomLevel == level
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberCyan else DarkSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) CyberCyan else CardBorderColor,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.setZoomLevel(level) }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${level.toInt()}x",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isSelected) DarkBackground else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auxiliary Toggles: Visión Nocturna, Flashlight, HUD Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AuxiliaryToggleCard(
                    title = "Visión Nocturna",
                    subtitle = if (isNightVision) "Activa (Sim)" else "Desactivada",
                    isActive = isNightVision,
                    activeColor = StatusGreen,
                    onClick = { viewModel.toggleNightVision() },
                    modifier = Modifier.weight(1f)
                )

                AuxiliaryToggleCard(
                    title = "Flash LED",
                    subtitle = if (isFlashlight) "Encendido (Sim)" else "Apagado",
                    isActive = isFlashlight,
                    activeColor = WarningAmber,
                    onClick = { viewModel.toggleFlashlight() },
                    modifier = Modifier.weight(1f)
                )

                AuxiliaryToggleCard(
                    title = "Retícula HUD",
                    subtitle = if (isGridVisible) "Visible" else "Oculta",
                    isActive = isGridVisible,
                    activeColor = CyberCyan,
                    onClick = { viewModel.toggleGrid() },
                    modifier = Modifier.weight(1f)
                )
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

    LaunchedEffect(flashVisible) {
        if (flashVisible) {
            delay(120)
            flashVisible = false
        }
    }
}

@Composable
private fun CameraDeckButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.18f) else DarkSurfaceVariant)
            .border(
                1.dp,
                if (isActive) accentColor else CardBorderColor,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) accentColor else TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isActive) accentColor else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontSize = 9.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AuxiliaryToggleCard(
    title: String,
    subtitle: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.12f) else DarkSurfaceVariant)
            .border(
                1.dp,
                if (isActive) activeColor.copy(alpha = 0.4f) else CardBorderColor,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isActive) activeColor else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontSize = 9.sp
                ),
                maxLines = 1
            )
        }
    }
}

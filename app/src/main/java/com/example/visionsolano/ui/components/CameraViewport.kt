package com.example.visionsolano.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.visionsolano.ui.theme.AlertRed
import com.example.visionsolano.ui.theme.CardBorderColor
import com.example.visionsolano.ui.theme.CyberCyan
import com.example.visionsolano.ui.theme.DarkBackground
import com.example.visionsolano.ui.theme.DarkSurface
import com.example.visionsolano.ui.theme.DarkSurfaceVariant
import com.example.visionsolano.ui.theme.HudGrid
import com.example.visionsolano.ui.theme.HudReticle
import com.example.visionsolano.ui.theme.StatusGreen
import com.example.visionsolano.ui.theme.TextPrimary
import com.example.visionsolano.ui.theme.TextSecondary
import com.example.visionsolano.ui.theme.WarningAmber
import kotlinx.coroutines.delay

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CameraViewport(
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onCapture: () -> Unit,
    onFullscreen: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    showDetailedControls: Boolean = true,
    isRecording: Boolean = false,
    zoomLevel: Float = 1f,
    fps: Int = 30,
    resolution: String = "640 x 480",
    connectionStatusText: String = "ENLACE ESP-CAM OK",
    streamUrl: String = "",
    isSimulated: Boolean = false
) {
    // Live simulated timestamp
    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        }
    }

    // Shutter flash effect
    var flashVisible by remember { mutableStateOf(false) }

    // Pulsing REC badge transition
    val transition = rememberInfiniteTransition(label = "recPulse")
    val recAlpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, CardBorderColor, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        // Video Viewport Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(14.dp))
                .background(DarkBackground)
                .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
        ) {
            // HUD reticle and corner brackets
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cornerLen = 28f
                val strokeW = 3f

                // Top-Left corner
                drawLine(HudReticle, Offset(16f, 16f), Offset(16f + cornerLen, 16f), strokeW)
                drawLine(HudReticle, Offset(16f, 16f), Offset(16f, 16f + cornerLen), strokeW)

                // Top-Right corner
                drawLine(HudReticle, Offset(w - 16f, 16f), Offset(w - 16f - cornerLen, 16f), strokeW)
                drawLine(HudReticle, Offset(w - 16f, 16f), Offset(w - 16f, 16f + cornerLen), strokeW)

                // Bottom-Left corner
                drawLine(HudReticle, Offset(16f, h - 16f), Offset(16f + cornerLen, h - 16f), strokeW)
                drawLine(HudReticle, Offset(16f, h - 16f), Offset(16f, h - 16f - cornerLen), strokeW)

                // Bottom-Right corner
                drawLine(HudReticle, Offset(w - 16f, h - 16f), Offset(w - 16f - cornerLen, h - 16f), strokeW)
                drawLine(HudReticle, Offset(w - 16f, h - 16f), Offset(w - 16f, h - 16f - cornerLen), strokeW)

                // Central crosshair
                val cx = w / 2f
                val cy = h / 2f
                val crossLen = 14f
                drawLine(HudGrid, Offset(cx - crossLen, cy), Offset(cx + crossLen, cy), 1.5f)
                drawLine(HudGrid, Offset(cx, cy - crossLen), Offset(cx, cy + crossLen), 1.5f)
            }

            // Top HUD elements
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // REC or PAUSE indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPaused) WarningAmber else AlertRed.copy(alpha = recAlpha)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPaused) "PAUSA" else if (isRecording) "REC ●" else if (isSimulated) "EN VIVO [SIMULADO]" else "EN VIVO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isPaused) WarningAmber else AlertRed,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                // Live Timestamp & ESP-CAM indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
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

            // Center: Simulated camera placeholder
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(CyberCyan.copy(alpha = 0.2f), Color.Transparent)
                            )
                        )
                        .border(1.dp, HudReticle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SurveillanceIcons.Videocam,
                        contentDescription = "Cámara",
                        tint = CyberCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isSimulated) "TRANSMISIÓN ESP-CAM (SIMULADA)" else "TRANSMISIÓN ESP-CAM",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (isPaused) "Transmisión pausada por usuario" else "Esperando transmisión...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                )
            }

            // Bottom overlay inside video frame (Resolution, FPS, Zoom, Connection)
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
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = resolution,
                            style = MaterialTheme.typography.labelSmall.copy(color = CyberCyan)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isPaused) "0 FPS" else "$fps FPS",
                            style = MaterialTheme.typography.labelSmall.copy(color = StatusGreen)
                        )
                    }

                    if (zoomLevel > 1f) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${zoomLevel.toInt()}x",
                                style = MaterialTheme.typography.labelSmall.copy(color = WarningAmber)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(StatusGreen)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = connectionStatusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = StatusGreen,
                            fontSize = 9.sp
                        )
                    )
                }
            }

            // Snapshot flash effect
            if (flashVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.75f))
                )
            }
        }

        // Bottom Controls Bar (Captura, Pantalla completa, Pausa, Configuración)
        if (showDetailedControls) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CameraControlButton(
                    text = "Captura",
                    icon = SurveillanceIcons.Camera,
                    onClick = {
                        flashVisible = true
                        onCapture()
                    },
                    modifier = Modifier.weight(1f)
                )

                CameraControlButton(
                    text = if (isPaused) "Reanudar" else "Pausa",
                    icon = if (isPaused) SurveillanceIcons.Play else SurveillanceIcons.Pause,
                    onClick = onTogglePause,
                    isActive = isPaused,
                    activeColor = WarningAmber,
                    modifier = Modifier.weight(1f)
                )

                CameraControlButton(
                    text = "Completa",
                    icon = SurveillanceIcons.Fullscreen,
                    onClick = onFullscreen,
                    modifier = Modifier.weight(1f)
                )

                CameraControlButton(
                    text = "Ajustes",
                    icon = SurveillanceIcons.Settings,
                    onClick = onSettings,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Auto-dismiss snapshot flash
    LaunchedEffect(flashVisible) {
        if (flashVisible) {
            delay(120)
            flashVisible = false
        }
    }
}

@Composable
private fun CameraControlButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    activeColor: Color = CyberCyan
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.16f) else DarkSurface)
            .border(
                1.dp,
                if (isActive) activeColor.copy(alpha = 0.5f) else CardBorderColor,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (isActive) activeColor else TextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isActive) activeColor else TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

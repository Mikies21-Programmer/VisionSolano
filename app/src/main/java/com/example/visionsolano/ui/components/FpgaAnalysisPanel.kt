package com.example.visionsolano.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionsolano.data.model.FpgaAnalysis
import com.example.visionsolano.data.model.ThreatLevel
import com.example.visionsolano.ui.theme.AlertRed
import com.example.visionsolano.ui.theme.CardBorderColor
import com.example.visionsolano.ui.theme.CyberCyan
import com.example.visionsolano.ui.theme.DarkSurface
import com.example.visionsolano.ui.theme.DarkSurfaceVariant
import com.example.visionsolano.ui.theme.StatusGreen
import com.example.visionsolano.ui.theme.TextPrimary
import com.example.visionsolano.ui.theme.TextSecondary
import com.example.visionsolano.ui.theme.WarningAmber

@Composable
fun FpgaAnalysisPanel(
    analysis: FpgaAnalysis,
    onSimulateToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isThreatActive = analysis.threatLevel != ThreatLevel.BAJO

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceVariant)
            .border(
                width = 1.dp,
                color = if (isThreatActive) AlertRed.copy(alpha = 0.6f) else CardBorderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SurveillanceIcons.Chip,
                            contentDescription = "FPGA",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Análisis FPGA",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (analysis.isSimulated) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(WarningAmber.copy(alpha = 0.15f))
                                        .border(1.dp, WarningAmber.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "SIMULADO",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = WarningAmber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (analysis.isSimulated) "Telemetría simulada (Desarrollo / Mock)" else "Decisiones por Hardware en Tiempo Real",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                            maxLines = 1
                        )
                    }
                }

                // Interactive simulation trigger button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isThreatActive) AlertRed.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.12f))
                        .border(
                            1.dp,
                            if (isThreatActive) AlertRed.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSimulateToggle() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isThreatActive) "Restablecer" else "Simular",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isThreatActive) AlertRed else CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Grid of decision metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricMiniCard(
                    title = "Estado actual",
                    value = analysis.currentState,
                    highlightColor = if (isThreatActive) AlertRed else StatusGreen,
                    modifier = Modifier.weight(1f)
                )

                MetricMiniCard(
                    title = "Movimiento",
                    value = if (analysis.movementDetected) "SÍ" else "NO",
                    highlightColor = if (analysis.movementDetected) AlertRed else StatusGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricMiniCard(
                    title = "Evento detectado",
                    value = analysis.eventDetected,
                    highlightColor = if (isThreatActive) WarningAmber else TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                MetricMiniCard(
                    title = "Nivel de alerta",
                    value = analysis.threatLevel.label,
                    highlightColor = analysis.threatLevel.getColor(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Threat level indicator bar
            ThreatLevelIndicator(
                threatLevel = analysis.threatLevel,
                threatPercentage = analysis.threatPercentage
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Telemetry line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Latencia HW: ${analysis.processingTimeMs} ms",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
                Text(
                    text = "Confianza: ${(analysis.confidenceScore * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
                Text(
                    text = "Sensores: ${analysis.activeSensorsCount}/4 OK",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }
        }
    }
}

@Composable
private fun MetricMiniCard(
    title: String,
    value: String,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    letterSpacing = 0.5.sp,
                    fontSize = 9.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = highlightColor,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

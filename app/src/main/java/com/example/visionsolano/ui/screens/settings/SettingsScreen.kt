package com.example.visionsolano.ui.screens.settings

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionsolano.data.model.SurveillanceConfig
import com.example.visionsolano.ui.components.StatusBadge
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
fun SettingsScreen(
    viewModel: SurveillanceViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val testConnectionStatus by viewModel.connectionTestStatus.collectAsState()

    var espCamIp by remember(config.espCamIp) { mutableStateOf(config.espCamIp) }
    var espCamPort by remember(config.espCamPort) { mutableStateOf(config.espCamPort.toString()) }
    var fpgaIp by remember(config.fpgaBridgeIp) { mutableStateOf(config.fpgaBridgeIp) }
    var fpgaPort by remember(config.fpgaPort) { mutableStateOf(config.fpgaPort.toString()) }

    var surveillanceActive by remember(config.surveillanceActive) { mutableStateOf(config.surveillanceActive) }
    var motionDetectionActive by remember(config.motionDetectionActive) { mutableStateOf(config.motionDetectionActive) }
    var sensitivity by remember(config.fpgaSensitivity) { mutableFloatStateOf(config.fpgaSensitivity) }

    var immediateAlerts by remember(config.immediateNotifications) { mutableStateOf(config.immediateNotifications) }
    var soundAlarm by remember(config.audioAlarmOnHighThreat) { mutableStateOf(config.audioAlarmOnHighThreat) }
    var autoSave by remember(config.autoSaveSnapshots) { mutableStateOf(config.autoSaveSnapshots) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Text(
                text = "CONFIGURACIÓN",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            )
            Text(
                text = "Ajustes de enlace, hardware de vigilancia y aplicación",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // CATEGORY 1: CONEXIÓN
            SettingsCategoryCard(title = "CONEXIÓN", icon = SurveillanceIcons.Refresh) {
                Text(
                    text = "CÁMARA (ESP-CAM)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CustomSurveillanceField(
                        label = "Dirección IP",
                        value = espCamIp,
                        onValueChange = { espCamIp = it },
                        modifier = Modifier.weight(2f)
                    )

                    CustomSurveillanceField(
                        label = "Puerto",
                        value = espCamPort,
                        onValueChange = { espCamPort = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "PUENTE FPGA (UART / TCP SOCKET)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CustomSurveillanceField(
                        label = "IP / Host",
                        value = fpgaIp,
                        onValueChange = { fpgaIp = it },
                        modifier = Modifier.weight(2f)
                    )

                    CustomSurveillanceField(
                        label = "Puerto",
                        value = fpgaPort,
                        onValueChange = { fpgaPort = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Test Connection Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberCyan.copy(alpha = 0.12f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { viewModel.testConnection() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = SurveillanceIcons.Refresh,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Probar Conexión con Hardware",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (testConnectionStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .border(1.dp, StatusGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = testConnectionStatus ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(color = StatusGreen)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CATEGORY 2: VIGILANCIA
            SettingsCategoryCard(title = "VIGILANCIA", icon = SurveillanceIcons.Shield) {
                // Activar Vigilancia
                SettingsSwitchRow(
                    title = "Activar Vigilancia Continua",
                    subtitle = "Habilita el modo centinela y monitoreo continuo",
                    checked = surveillanceActive,
                    onCheckedChange = {
                        surveillanceActive = it
                        viewModel.toggleSurveillance(it)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Detección de Movimiento
                SettingsSwitchRow(
                    title = "Detección de Movimiento",
                    subtitle = "Análisis de flujo óptico en FPGA",
                    checked = motionDetectionActive,
                    onCheckedChange = { motionDetectionActive = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Sensibilidad Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sensibilidad de Detección FPGA",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "${(sensitivity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = CardBorderColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CATEGORY 3: APLICACIÓN
            SettingsCategoryCard(title = "APLICACIÓN", icon = SurveillanceIcons.Settings) {
                SettingsSwitchRow(
                    title = "Notificaciones Inmediatas",
                    subtitle = "Alertar al detectar anomalías de alta prioridad",
                    checked = immediateAlerts,
                    onCheckedChange = { immediateAlerts = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSwitchRow(
                    title = "Alarma Sonora",
                    subtitle = "Emitir tono de alerta en amenazas críticas",
                    checked = soundAlarm,
                    onCheckedChange = { soundAlarm = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSwitchRow(
                    title = "Almacenamiento Local",
                    subtitle = "Guardar capturas y registros en la memoria del dispositivo",
                    checked = autoSave,
                    onCheckedChange = { autoSave = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CATEGORY 4: INFORMACIÓN DEL SISTEMA
            SettingsCategoryCard(title = "INFORMACIÓN DEL SISTEMA", icon = SurveillanceIcons.Info) {
                InfoItemRow(label = "Aplicación", value = "VisionSolano")
                InfoItemRow(label = "Versión de Software", value = config.firmwareVersion)
                InfoItemRow(label = "Hardware Target", value = config.hardwareTarget)
                InfoItemRow(label = "Estado de Integración", value = "Prototipo Visual y Arquitectura Lista")
                InfoItemRow(label = "Protocolo de Video", value = "MJPEG / RTSP (Preparado)")
                InfoItemRow(label = "Protocolo de Decisiones", value = "FPGA Serial Bridge (Preparado)")
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberCyan,
                checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkSurface
            )
        )
    }
}

@Composable
private fun CustomSurveillanceField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberCyan,
            unfocusedBorderColor = CardBorderColor,
            focusedLabelColor = CyberCyan,
            unfocusedLabelColor = TextSecondary,
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    )
}

@Composable
private fun InfoItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

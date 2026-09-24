package com.example.visionsolano.ui.screens.events

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionsolano.data.model.EventPriority
import com.example.visionsolano.data.model.SecurityEvent
import com.example.visionsolano.ui.components.EventItemCard
import com.example.visionsolano.ui.components.StatusBadge
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
import com.example.visionsolano.viewmodel.SurveillanceViewModel

@Composable
fun EventsScreen(
    viewModel: SurveillanceViewModel,
    modifier: Modifier = Modifier
) {
    val events by viewModel.filteredEvents.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    var selectedEventForDialog by remember { mutableStateOf<SecurityEvent?>(null) }

    val filterOptions = listOf("Todos", "FPGA", "ESP-CAM", "Prioridad Alta")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HISTORIAL DE EVENTOS",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "Auditoría de Seguridad y Registro FPGA",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }

                StatusBadge(
                    text = "${events.size} REGISTROS",
                    accentColor = CyberCyan
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) CyberCyan else DarkSurfaceVariant)
                            .border(
                                1.dp,
                                if (isSelected) CyberCyan else CardBorderColor,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.setFilter(filter) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isSelected) DarkBackground else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Events List
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = SurveillanceIcons.Shield,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No se encontraron eventos para el filtro seleccionado",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(events, key = { it.id }) { event ->
                        EventItemCard(
                            event = event,
                            onClick = { selectedEventForDialog = event }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Event Detail Dialog
        selectedEventForDialog?.let { event ->
            AlertDialog(
                onDismissRequest = { selectedEventForDialog = null },
                containerColor = DarkSurface,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detalle del Evento",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        StatusBadge(
                            text = event.priority.label,
                            accentColor = event.priority.getColor()
                        )
                    }
                },
                text = {
                    Column {
                        DetailLine(label = "ID de Evento", value = event.id)
                        DetailLine(label = "Título", value = event.title)
                        DetailLine(label = "Origen", value = event.source.label)
                        DetailLine(label = "Hora Registrada", value = event.time)
                        DetailLine(label = "Descripción", value = event.description)

                        if (event.sensorPayload != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "TELEMETRÍA / ANÁLISIS DE SEÑAL:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = event.sensorPayload,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedEventForDialog = null }) {
                        Text(
                            text = "Cerrar",
                            style = MaterialTheme.typography.labelLarge.copy(color = CyberCyan)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
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

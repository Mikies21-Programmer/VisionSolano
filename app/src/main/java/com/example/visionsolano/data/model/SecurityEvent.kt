package com.example.visionsolano.data.model

import androidx.compose.ui.graphics.Color
import com.example.visionsolano.ui.theme.AlertRed
import com.example.visionsolano.ui.theme.CyberCyan
import com.example.visionsolano.ui.theme.StatusGreen
import com.example.visionsolano.ui.theme.WarningAmber

enum class EventSource(val label: String) {
    FPGA("FPGA"),
    ESP_CAM("ESP-CAM"),
    SISTEMA("SISTEMA")
}

enum class EventPriority(val label: String) {
    BAJA("PRIORIDAD BAJA"),
    MEDIA("PRIORIDAD MEDIA"),
    ALTA("PRIORIDAD ALTA");

    fun getColor(): Color = when (this) {
        BAJA -> StatusGreen
        MEDIA -> WarningAmber
        ALTA -> AlertRed
    }
}

data class SecurityEvent(
    val id: String,
    val title: String,
    val description: String,
    val time: String,
    val source: EventSource,
    val priority: EventPriority,
    val sensorPayload: String? = null
)

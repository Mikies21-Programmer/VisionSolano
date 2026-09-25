package com.example.visionsolano.data.model

import androidx.compose.ui.graphics.Color
import com.example.visionsolano.ui.theme.AlertRed
import com.example.visionsolano.ui.theme.CyberCyan
import com.example.visionsolano.ui.theme.StatusGreen
import com.example.visionsolano.ui.theme.WarningAmber

/**
 * Estados del ciclo de vida de conexión y vinculación entre VisionSolano y los dispositivos periféricos.
 */
enum class ConnectionState(val label: String) {
    Searching("BUSCANDO DISPOSITIVO"),
    Discovered("DISPOSITIVO DETECTADO"),
    Connecting("ENLAZANDO..."),
    Connected("CONECTADO"),
    Disconnected("SIN CONEXIÓN"),
    Timeout("TIEMPO AGOTADO"),
    IncompatibleProtocol("PROTOCOLO INCOMPATIBLE");

    fun getColor(): Color = when (this) {
        Connected -> StatusGreen
        Discovered, Connecting -> CyberCyan
        Searching -> WarningAmber
        Disconnected -> AlertRed
        Timeout -> WarningAmber
        IncompatibleProtocol -> AlertRed
    }
}

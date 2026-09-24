package com.example.visionsolano.data.model

enum class DeviceState(val label: String) {
    ONLINE("ONLINE"),
    OFFLINE("OFFLINE"),
    STANDBY("STANDBY")
}

enum class SystemMode(val label: String) {
    ACTIVO("ACTIVO"),
    INACTIVO("INACTIVO"),
    ALERTA("ALERTA")
}

enum class ConnectionStatus(val label: String) {
    CONECTADO("CONECTADO"),
    NO_CONECTADO("NO CONECTADO"),
    ENLAZANDO("ENLAZANDO...")
}

data class SystemStatus(
    val fpgaState: DeviceState = DeviceState.ONLINE,
    val espCamState: DeviceState = DeviceState.ONLINE,
    val systemMode: SystemMode = SystemMode.ACTIVO,
    val connectionStatus: ConnectionStatus = ConnectionStatus.NO_CONECTADO,
    val latencyMs: Int = 8,
    val uptimeHours: String = "04:18:22",
    val wifiSignalDbm: Int = -54,
    val powerSupplyVoltage: Float = 12.1f
)

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

/**
 * Estado integral de telemetría y salud del sistema VisionSolano.
 */
data class SystemStatus(
    // Estados de hardware y subsistemas
    val fpgaState: DeviceState = DeviceState.ONLINE,
    val espCamState: DeviceState = DeviceState.ONLINE,
    val systemMode: SystemMode = SystemMode.ACTIVO,
    val connectionStatus: ConnectionStatus = ConnectionStatus.NO_CONECTADO,
    val connectionState: ConnectionState = ConnectionState.Disconnected,

    // Métricas de enlace y red
    val latencyMs: Int = 8,
    val uptimeHours: String = "04:18:22",
    val wifiSignalDbm: Int = -54,
    val powerSupplyVoltage: Float = 12.1f,

    // Campos de ciclo de vida, descubrimiento y latido (Heartbeat)
    val lastContact: Long = System.currentTimeMillis(),
    val heartbeatAge: Long = 0L,
    val esp32Online: Boolean = true,
    val cameraOnline: Boolean = true,
    val protocolVersion: Int = 1,

    // Métricas del sensor óptico (dinámicas según /api/status o mock)
    val fps: Int = 30,
    val resolution: String = "640 x 480",

    // Dispositivo actualmente descubierto/asociado
    val discoveredDevice: VisionSolanoDevice? = null,

    // Indicador explícito de telemetría simulada (Mock/desarrollo)
    val isSimulated: Boolean = false
)

package com.example.visionsolano.data.repository

import com.example.visionsolano.data.model.ConnectionState
import com.example.visionsolano.data.model.ConnectionStatus
import com.example.visionsolano.data.model.DeviceState
import com.example.visionsolano.data.model.EventPriority
import com.example.visionsolano.data.model.EventSource
import com.example.visionsolano.data.model.FpgaAnalysis
import com.example.visionsolano.data.model.SecurityEvent
import com.example.visionsolano.data.model.SurveillanceConfig
import com.example.visionsolano.data.model.SystemMode
import com.example.visionsolano.data.model.SystemStatus
import com.example.visionsolano.data.model.ThreatLevel
import com.example.visionsolano.data.model.VisionSolanoDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface SurveillanceRepository {
    val systemStatus: Flow<SystemStatus>
    val fpgaAnalysis: Flow<FpgaAnalysis>
    val securityEvents: Flow<List<SecurityEvent>>
    val config: Flow<SurveillanceConfig>

    fun updateConfig(config: SurveillanceConfig)
    fun simulateThreatDetection()
    fun resetThreatToNormal()
    fun addSecurityEvent(event: SecurityEvent)
    fun toggleSurveillance(active: Boolean)

    // Métodos preparados para el ciclo de vida de descubrimiento
    fun startDiscovery() {}
    fun stopDiscovery() {}
}

class MockSurveillanceRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : SurveillanceRepository {

    // Dispositivo Mock predeterminado
    // ACLARACIÓN: 192.168.1.50 es únicamente un dato de prueba/mock, NO una IP fija del producto.
    private val mockDiscoveredDevice = VisionSolanoDevice.createMockDevice()

    private val _systemStatus = MutableStateFlow(
        SystemStatus(
            fpgaState = DeviceState.ONLINE,
            espCamState = DeviceState.ONLINE,
            systemMode = SystemMode.ACTIVO,
            connectionStatus = ConnectionStatus.CONECTADO,
            connectionState = ConnectionState.Connected,
            latencyMs = 8,
            uptimeHours = "03:42:15",
            wifiSignalDbm = -54,
            powerSupplyVoltage = 12.2f,
            lastContact = System.currentTimeMillis(),
            heartbeatAge = 0L,
            esp32Online = true,
            cameraOnline = true,
            protocolVersion = 1,
            fps = 30,
            resolution = "640 x 480",
            discoveredDevice = mockDiscoveredDevice,
            isSimulated = true
        )
    )
    override val systemStatus: Flow<SystemStatus> = _systemStatus.asStateFlow()

    private val _fpgaAnalysis = MutableStateFlow(
        FpgaAnalysis(
            currentState = "SIN AMENAZA",
            movementDetected = false,
            eventDetected = "NINGUNO",
            threatLevel = ThreatLevel.BAJO,
            threatPercentage = 0.08f,
            confidenceScore = 0.98f,
            processingTimeMs = 1.2f,
            activeSensorsCount = 4,
            lastAnalysisTimestamp = "10:25",
            isSimulated = true
        )
    )
    override val fpgaAnalysis: Flow<FpgaAnalysis> = _fpgaAnalysis.asStateFlow()

    private val _securityEvents = MutableStateFlow(
        listOf(
            SecurityEvent(
                id = "EVT-1006",
                title = "SIN AMENAZA",
                description = "Patrón normal detectado en perímetro",
                time = "10:25",
                source = EventSource.FPGA,
                priority = EventPriority.BAJA,
                sensorPayload = "Sector B-1 | Ruido térmico normal | Varianza < 0.02"
            ),
            SecurityEvent(
                id = "EVT-1005",
                title = "MOVIMIENTO DETECTADO",
                description = "Análisis realizado por FPGA",
                time = "10:24",
                source = EventSource.FPGA,
                priority = EventPriority.MEDIA,
                sensorPayload = "Diferencia de cuadros por hardware | Umbral 78% | Sensor óptico 1"
            ),
            SecurityEvent(
                id = "EVT-1004",
                title = "Sin amenazas detectadas",
                description = "Escaneo continuo de cuadrante A",
                time = "10:24",
                source = EventSource.FPGA,
                priority = EventPriority.BAJA,
                sensorPayload = "Validación por red lógica programable"
            ),
            SecurityEvent(
                id = "EVT-1003",
                title = "Vigilancia activa",
                description = "Subsistema de monitoreo en modo centinela",
                time = "10:23",
                source = EventSource.SISTEMA,
                priority = EventPriority.BAJA,
                sensorPayload = "Modo: Centinela | FPS: 30 | Watchdog activo"
            ),
            SecurityEvent(
                id = "EVT-1002",
                title = "ESP-CAM conectada",
                description = "Canal de video simulado inicializado",
                time = "10:22",
                source = EventSource.ESP_CAM,
                priority = EventPriority.BAJA,
                sensorPayload = "Buffer de video listo | Resolución: 640x480"
            ),
            SecurityEvent(
                id = "EVT-1001",
                title = "Sistema iniciado",
                description = "Arranque de plataforma VisionSolano",
                time = "10:21",
                source = EventSource.SISTEMA,
                priority = EventPriority.BAJA,
                sensorPayload = "Kernel y subsistemas de telemetría inicializados"
            )
        )
    )
    override val securityEvents: Flow<List<SecurityEvent>> = _securityEvents.asStateFlow()

    private val _config = MutableStateFlow(SurveillanceConfig())
    override val config: Flow<SurveillanceConfig> = _config.asStateFlow()

    private var discoverySimulationJob: Job? = null

    override fun updateConfig(config: SurveillanceConfig) {
        _config.value = config
    }

    override fun simulateThreatDetection() {
        _fpgaAnalysis.update {
            it.copy(
                currentState = "MOVIMIENTO DETECTADO",
                movementDetected = true,
                eventDetected = "INTRUSIÓN EN CUADRANTE",
                threatLevel = ThreatLevel.ALTO,
                threatPercentage = 0.85f,
                confidenceScore = 0.94f,
                processingTimeMs = 1.8f,
                lastAnalysisTimestamp = "10:26"
            )
        }
        addSecurityEvent(
            SecurityEvent(
                id = "EVT-${System.currentTimeMillis() % 10000}",
                title = "MOVIMIENTO DETECTADO",
                description = "Análisis realizado por FPGA - Detección activa",
                time = "10:26",
                source = EventSource.FPGA,
                priority = EventPriority.ALTA,
                sensorPayload = "Alerta generada por lógica combinacional FPGA"
            )
        )
    }

    override fun resetThreatToNormal() {
        _fpgaAnalysis.update {
            it.copy(
                currentState = "SIN AMENAZA",
                movementDetected = false,
                eventDetected = "NINGUNO",
                threatLevel = ThreatLevel.BAJO,
                threatPercentage = 0.06f,
                confidenceScore = 0.99f,
                processingTimeMs = 1.1f,
                lastAnalysisTimestamp = "10:27"
            )
        }
    }

    override fun addSecurityEvent(event: SecurityEvent) {
        _securityEvents.update { listOf(event) + it }
    }

    override fun toggleSurveillance(active: Boolean) {
        _systemStatus.update {
            it.copy(systemMode = if (active) SystemMode.ACTIVO else SystemMode.INACTIVO)
        }
        _config.update {
            it.copy(surveillanceActive = active)
        }
    }

    override fun startDiscovery() {
        discoverySimulationJob?.cancel()
        discoverySimulationJob = scope.launch {
            _systemStatus.update {
                it.copy(
                    connectionState = ConnectionState.Searching,
                    connectionStatus = ConnectionStatus.ENLAZANDO,
                    espCamState = DeviceState.STANDBY
                )
            }
            delay(1200)
            _systemStatus.update {
                it.copy(
                    connectionState = ConnectionState.Discovered,
                    discoveredDevice = mockDiscoveredDevice
                )
            }
            delay(800)
            _systemStatus.update {
                it.copy(
                    connectionState = ConnectionState.Connected,
                    connectionStatus = ConnectionStatus.CONECTADO,
                    espCamState = DeviceState.ONLINE,
                    lastContact = System.currentTimeMillis(),
                    heartbeatAge = 0L
                )
            }
        }
    }

    override fun stopDiscovery() {
        discoverySimulationJob?.cancel()
    }
}

package com.example.visionsolano.data.repository

import com.example.visionsolano.data.model.ConnectionState
import com.example.visionsolano.data.model.ConnectionStatus
import com.example.visionsolano.data.model.DeviceState
import com.example.visionsolano.data.model.FpgaAnalysis
import com.example.visionsolano.data.model.SecurityEvent
import com.example.visionsolano.data.model.SurveillanceConfig
import com.example.visionsolano.data.model.SystemMode
import com.example.visionsolano.data.model.SystemStatus
import com.example.visionsolano.data.model.ThreatLevel
import com.example.visionsolano.data.model.VisionSolanoDevice
import com.example.visionsolano.data.network.DiscoveryState
import com.example.visionsolano.data.network.ESP32Api
import com.example.visionsolano.data.network.NetworkConfig
import com.example.visionsolano.data.network.VisionSolanoDiscoveryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Repositorio de red preparado para la comunicación real con la ESP32-S3 y la Nexys 4.
 *
 * Implementa [SurveillanceRepository] manteniendo la misma interfaz conceptual que
 * [MockSurveillanceRepository], permitiendo una transición transparente en la UI.
 */
class VisionSolanoNetworkRepository(
    private val discoveryManager: VisionSolanoDiscoveryManager = VisionSolanoDiscoveryManager(),
    private val esp32Api: ESP32Api = ESP32Api(useMockResponses = true),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : SurveillanceRepository {

    private val _systemStatus = MutableStateFlow(
        SystemStatus(
            fpgaState = DeviceState.ONLINE,
            espCamState = DeviceState.STANDBY,
            systemMode = SystemMode.ACTIVO,
            connectionStatus = ConnectionStatus.NO_CONECTADO,
            connectionState = ConnectionState.Disconnected,
            latencyMs = 0,
            uptimeHours = "00:00:00",
            wifiSignalDbm = -54,
            powerSupplyVoltage = 12.0f,
            lastContact = 0L,
            heartbeatAge = 0L,
            esp32Online = false,
            cameraOnline = false,
            protocolVersion = NetworkConfig.EXPECTED_PROTOCOL_VERSION,
            fps = 0,
            resolution = "640 x 480",
            discoveredDevice = null,
            isSimulated = false
        )
    )
    override val systemStatus: Flow<SystemStatus> = _systemStatus.asStateFlow()

    private val _fpgaAnalysis = MutableStateFlow(
        FpgaAnalysis(
            currentState = "SIN AMENAZA",
            movementDetected = false,
            eventDetected = "NINGUNO",
            threatLevel = ThreatLevel.BAJO,
            threatPercentage = 0.05f,
            confidenceScore = 0.95f,
            processingTimeMs = 1.0f,
            activeSensorsCount = 4,
            lastAnalysisTimestamp = "--:--",
            isSimulated = false
        )
    )
    override val fpgaAnalysis: Flow<FpgaAnalysis> = _fpgaAnalysis.asStateFlow()

    private val _securityEvents = MutableStateFlow<List<SecurityEvent>>(emptyList())
    override val securityEvents: Flow<List<SecurityEvent>> = _securityEvents.asStateFlow()

    private val _config = MutableStateFlow(SurveillanceConfig())
    override val config: Flow<SurveillanceConfig> = _config.asStateFlow()

    private var heartbeatJob: Job? = null

    init {
        // Observar cambios en el proceso de descubrimiento mDNS
        scope.launch {
            discoveryManager.discoveryState.collect { dState ->
                handleDiscoveryStateChange(dState)
            }
        }

        scope.launch {
            discoveryManager.discoveredDevice.collect { device ->
                if (device != null) {
                    onDeviceDiscovered(device)
                }
            }
        }
    }

    private fun handleDiscoveryStateChange(state: DiscoveryState) {
        val mappedConnectionState = when (state) {
            DiscoveryState.IDLE -> ConnectionState.Disconnected
            DiscoveryState.SEARCHING -> ConnectionState.Searching
            DiscoveryState.DISCOVERING -> ConnectionState.Connecting
            DiscoveryState.FOUND -> ConnectionState.Discovered
            DiscoveryState.CONNECTING -> ConnectionState.Connecting
            DiscoveryState.CONNECTED -> ConnectionState.Connected
            DiscoveryState.TIMEOUT -> ConnectionState.Timeout
            DiscoveryState.NOT_FOUND -> ConnectionState.Disconnected
            DiscoveryState.INCOMPATIBLE -> ConnectionState.IncompatibleProtocol
        }

        _systemStatus.update {
            it.copy(
                connectionState = mappedConnectionState,
                connectionStatus = if (mappedConnectionState == ConnectionState.Connected) {
                    ConnectionStatus.CONECTADO
                } else if (mappedConnectionState == ConnectionState.Connecting || mappedConnectionState == ConnectionState.Searching) {
                    ConnectionStatus.ENLAZANDO
                } else {
                    ConnectionStatus.NO_CONECTADO
                }
            )
        }
    }

    private fun onDeviceDiscovered(device: VisionSolanoDevice) {
        _systemStatus.update {
            it.copy(
                discoveredDevice = device,
                espCamState = DeviceState.ONLINE,
                esp32Online = true,
                cameraOnline = true,
                connectionState = ConnectionState.Connected,
                connectionStatus = ConnectionStatus.CONECTADO,
                lastContact = System.currentTimeMillis(),
                heartbeatAge = 0L,
                protocolVersion = device.protocolVersion
            )
        }
        startHeartbeat(device)
    }

    /**
     * Bucle de latido (Heartbeat):
     * - Intervalo: 1 segundo
     * - Tiempo de agotamiento (Timeout): 3 segundos
     * - Si se pierde el contacto: muestra "SIN CONEXIÓN".
     * - Android NO toma decisiones de alarma (no activa ni desactiva sirenas).
     */
    private fun startHeartbeat(device: VisionSolanoDevice) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(NetworkConfig.HEARTBEAT_INTERVAL_MS)

                val result = esp32Api.getStatus(device.targetHost, device.port)
                val now = System.currentTimeMillis()

                if (result.isSuccess) {
                    val status = result.getOrThrow()
                    _systemStatus.update {
                        it.copy(
                            lastContact = now,
                            heartbeatAge = 0L,
                            esp32Online = status.esp32Online,
                            cameraOnline = status.cameraOnline,
                            fps = status.fps,
                            resolution = status.resolution,
                            wifiSignalDbm = status.wifiSignalDbm,
                            espCamState = if (status.cameraOnline) DeviceState.ONLINE else DeviceState.STANDBY,
                            connectionState = ConnectionState.Connected,
                            connectionStatus = ConnectionStatus.CONECTADO
                        )
                    }
                } else {
                    val currentStatus = _systemStatus.value
                    val age = now - currentStatus.lastContact
                    _systemStatus.update { it.copy(heartbeatAge = age) }

                    // Timeout estricto de 3 segundos
                    if (age >= NetworkConfig.HEARTBEAT_TIMEOUT_MS) {
                        _systemStatus.update {
                            it.copy(
                                connectionState = ConnectionState.Disconnected,
                                connectionStatus = ConnectionStatus.NO_CONECTADO,
                                espCamState = DeviceState.OFFLINE,
                                esp32Online = false,
                                cameraOnline = false
                            )
                        }
                    }
                }
            }
        }
    }

    override fun startDiscovery() {
        discoveryManager.startDiscovery()
    }

    override fun stopDiscovery() {
        discoveryManager.stopDiscovery()
        heartbeatJob?.cancel()
    }

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
                lastAnalysisTimestamp = "10:26"
            )
        }
    }

    override fun resetThreatToNormal() {
        _fpgaAnalysis.update {
            it.copy(
                currentState = "SIN AMENAZA",
                movementDetected = false,
                eventDetected = "NINGUNO",
                threatLevel = ThreatLevel.BAJO,
                threatPercentage = 0.05f
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
}

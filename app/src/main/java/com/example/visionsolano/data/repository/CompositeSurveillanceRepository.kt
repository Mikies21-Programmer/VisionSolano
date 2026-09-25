package com.example.visionsolano.data.repository

import com.example.visionsolano.data.model.ConnectionState
import com.example.visionsolano.data.model.FpgaAnalysis
import com.example.visionsolano.data.model.SecurityEvent
import com.example.visionsolano.data.model.SurveillanceConfig
import com.example.visionsolano.data.model.SystemStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Repositorio compuesto que integra [VisionSolanoNetworkRepository] y [MockSurveillanceRepository].
 *
 * Arquitectura del sistema:
 * - [MockSurveillanceRepository]: fallback y desarrollo (telemetría marcada como simulada).
 * - [VisionSolanoNetworkRepository]: backend real activo cuando existe un dispositivo descubierto
 *   o cuando se ejecuta un proceso activo de descubrimiento en la LAN.
 * - La interfaz de usuario (UI) interactúa exclusivamente con [SurveillanceRepository] sin
 *   conocer qué repositorio subyacente está activo en cada momento.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CompositeSurveillanceRepository(
    val mockRepository: MockSurveillanceRepository = MockSurveillanceRepository(),
    val networkRepository: VisionSolanoNetworkRepository = VisionSolanoNetworkRepository(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : SurveillanceRepository {

    /**
     * Determina reactivamente si el backend de red debe suministrar la telemetría.
     * Es verdadero cuando hay un dispositivo real descubierto y conectado, o mientras
     * se encuentra en un estado activo de búsqueda o enlace.
     */
    private val isNetworkActiveFlow: Flow<Boolean> = networkRepository.systemStatus.flatMapLatest { netStatus ->
        val isActive = netStatus.discoveredDevice != null ||
            netStatus.connectionState == ConnectionState.Connected ||
            netStatus.connectionState == ConnectionState.Connecting ||
            netStatus.connectionState == ConnectionState.Searching ||
            netStatus.connectionState == ConnectionState.Discovered

        flowOf(isActive)
    }

    override val systemStatus: Flow<SystemStatus> = isNetworkActiveFlow.flatMapLatest { isNetworkActive ->
        if (isNetworkActive) {
            networkRepository.systemStatus
        } else {
            mockRepository.systemStatus
        }
    }

    override val fpgaAnalysis: Flow<FpgaAnalysis> = isNetworkActiveFlow.flatMapLatest { isNetworkActive ->
        if (isNetworkActive) {
            networkRepository.fpgaAnalysis
        } else {
            mockRepository.fpgaAnalysis
        }
    }

    override val securityEvents: Flow<List<SecurityEvent>> = isNetworkActiveFlow.flatMapLatest { isNetworkActive ->
        if (isNetworkActive) {
            combine(networkRepository.securityEvents, mockRepository.securityEvents) { netEvents, mockEvents ->
                if (netEvents.isNotEmpty()) netEvents else mockEvents
            }
        } else {
            mockRepository.securityEvents
        }
    }

    override val config: Flow<SurveillanceConfig> = networkRepository.config

    override fun updateConfig(config: SurveillanceConfig) {
        mockRepository.updateConfig(config)
        networkRepository.updateConfig(config)
    }

    override fun simulateThreatDetection() {
        mockRepository.simulateThreatDetection()
        networkRepository.simulateThreatDetection()
    }

    override fun resetThreatToNormal() {
        mockRepository.resetThreatToNormal()
        networkRepository.resetThreatToNormal()
    }

    override fun addSecurityEvent(event: SecurityEvent) {
        mockRepository.addSecurityEvent(event)
        networkRepository.addSecurityEvent(event)
    }

    override fun toggleSurveillance(active: Boolean) {
        mockRepository.toggleSurveillance(active)
        networkRepository.toggleSurveillance(active)
    }

    override fun startDiscovery() {
        networkRepository.startDiscovery()
    }

    override fun stopDiscovery() {
        networkRepository.stopDiscovery()
    }
}

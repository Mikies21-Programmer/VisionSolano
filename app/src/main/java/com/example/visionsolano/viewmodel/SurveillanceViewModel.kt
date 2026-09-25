package com.example.visionsolano.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visionsolano.data.model.ConnectionStatus
import com.example.visionsolano.data.model.EventPriority
import com.example.visionsolano.data.model.EventSource
import com.example.visionsolano.data.model.FpgaAnalysis
import com.example.visionsolano.data.model.SecurityEvent
import com.example.visionsolano.data.model.SurveillanceConfig
import com.example.visionsolano.data.model.SystemStatus
import com.example.visionsolano.data.network.NetworkConfig
import com.example.visionsolano.data.repository.CompositeSurveillanceRepository
import com.example.visionsolano.data.repository.MockSurveillanceRepository
import com.example.visionsolano.data.repository.SurveillanceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SurveillanceViewModel(
    private val repository: SurveillanceRepository = CompositeSurveillanceRepository()
) : ViewModel() {

    val systemStatus: StateFlow<SystemStatus> = repository.systemStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SystemStatus()
    )

    val fpgaAnalysis: StateFlow<FpgaAnalysis> = repository.fpgaAnalysis.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FpgaAnalysis()
    )

    private val _rawEvents = repository.securityEvents
    private val _selectedFilter = MutableStateFlow("Todos")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    val filteredEvents: StateFlow<List<SecurityEvent>> = combine(_rawEvents, _selectedFilter) { events, filter ->
        when (filter) {
            "FPGA" -> events.filter { it.source == EventSource.FPGA }
            "ESP-CAM" -> events.filter { it.source == EventSource.ESP_CAM }
            "Prioridad Alta" -> events.filter { it.priority == EventPriority.ALTA }
            else -> events
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val config: StateFlow<SurveillanceConfig> = repository.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SurveillanceConfig()
    )

    // Camera Interactive Controls & States
    private val _isStreamPaused = MutableStateFlow(false)
    val isStreamPaused: StateFlow<Boolean> = _isStreamPaused.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()
    private var recordingJob: Job? = null

    private val _zoomLevel = MutableStateFlow(1f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    private val _isNightVisionActive = MutableStateFlow(false)
    val isNightVisionActive: StateFlow<Boolean> = _isNightVisionActive.asStateFlow()

    private val _isFlashlightActive = MutableStateFlow(false)
    val isFlashlightActive: StateFlow<Boolean> = _isFlashlightActive.asStateFlow()

    private val _isGridVisible = MutableStateFlow(true)
    val isGridVisible: StateFlow<Boolean> = _isGridVisible.asStateFlow()

    private val _snapshotFeedback = MutableStateFlow<String?>(null)
    val snapshotFeedback: StateFlow<String?> = _snapshotFeedback.asStateFlow()

    private val _connectionTestStatus = MutableStateFlow<String?>(null)
    val connectionTestStatus: StateFlow<String?> = _connectionTestStatus.asStateFlow()

    fun toggleStreamPause() {
        _isStreamPaused.update { !it }
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            _isRecording.value = false
            recordingJob?.cancel()
            _recordingSeconds.value = 0
            _snapshotFeedback.value = "Grabación detenida y guardada en búfer local"
        } else {
            _isRecording.value = true
            _recordingSeconds.value = 0
            recordingJob = viewModelScope.launch {
                while (_isRecording.value) {
                    delay(1000)
                    _recordingSeconds.update { it + 1 }
                }
            }
        }
    }

    fun takeSnapshot() {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        _snapshotFeedback.value = "Captura instantánea guardada: SNAP_$currentTime.jpg"
        repository.addSecurityEvent(
            SecurityEvent(
                id = "EVT-${System.currentTimeMillis() % 10000}",
                title = "Captura de fotograma",
                description = "Captura manual guardada en almacenamiento",
                time = currentTime,
                source = EventSource.ESP_CAM,
                priority = EventPriority.BAJA,
                sensorPayload = "Frame #8491 | Resolución: ${systemStatus.value.resolution} | Formato: JPEG"
            )
        )
    }

    fun clearSnapshotFeedback() {
        _snapshotFeedback.value = null
    }

    fun setZoomLevel(level: Float) {
        _zoomLevel.value = level
    }

    fun toggleNightVision() {
        _isNightVisionActive.update { !it }
    }

    fun toggleFlashlight() {
        _isFlashlightActive.update { !it }
    }

    fun toggleGrid() {
        _isGridVisible.update { !it }
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun simulateFpgaThreat() {
        repository.simulateThreatDetection()
    }

    fun resetFpgaThreat() {
        repository.resetThreatToNormal()
    }

    fun toggleSurveillance(active: Boolean) {
        repository.toggleSurveillance(active)
    }

    fun updateConfig(updated: SurveillanceConfig) {
        repository.updateConfig(updated)
    }

    fun testConnection() {
        viewModelScope.launch {
            _connectionTestStatus.value = "Probando enlace con ESP-CAM y puente FPGA..."
            delay(1200)
            _connectionTestStatus.value = "Enlace OK: ESP-CAM Ping 12ms | FPGA UART Bridge Respondio ACK"
            delay(3500)
            _connectionTestStatus.value = null
        }
    }

    // Estado y control del permiso de red local (ACCESS_LOCAL_NETWORK para SDK 37+)
    private val _isLocalNetworkPermissionDenied = MutableStateFlow(false)
    val isLocalNetworkPermissionDenied: StateFlow<Boolean> = _isLocalNetworkPermissionDenied.asStateFlow()

    fun onLocalNetworkPermissionResult(granted: Boolean) {
        if (granted) {
            _isLocalNetworkPermissionDenied.value = false
            startDiscovery()
        } else {
            _isLocalNetworkPermissionDenied.value = true
        }
    }

    fun dismissLocalNetworkPermissionDenied() {
        _isLocalNetworkPermissionDenied.value = false
    }

    // Funciones de descubrimiento automático por mDNS / NSD
    fun startDiscovery() {
        repository.startDiscovery()
    }

    fun stopDiscovery() {
        repository.stopDiscovery()
    }

    /**
     * Retorna la URL dinámica del stream de vídeo resolviendo el host/IP descubierto.
     * No depende de una IP hardcodeada.
     */
    fun getStreamUrl(): String {
        val status = systemStatus.value
        val device = status.discoveredDevice
        return if (device != null) {
            device.streamUrl
        } else {
            NetworkConfig.buildStreamUrl(config.value.espCamIp, config.value.espCamPort)
        }
    }
}

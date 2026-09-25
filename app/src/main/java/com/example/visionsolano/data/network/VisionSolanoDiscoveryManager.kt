package com.example.visionsolano.data.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.visionsolano.data.model.VisionSolanoDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.nio.charset.StandardCharsets

/**
 * Estados del proceso de descubrimiento por NSD (Network Service Discovery / mDNS).
 */
enum class DiscoveryState {
    IDLE,
    SEARCHING,
    DISCOVERING,
    FOUND,
    CONNECTING,
    CONNECTED,
    TIMEOUT,
    NOT_FOUND,
    INCOMPATIBLE
}

/**
 * Administrador de descubrimiento de servicios de red mediante mDNS / NSD.
 *
 * Busca servicios de tipo `_visionsolano._tcp`, resuelve IPs, puertos y atributos TXT.
 */
class VisionSolanoDiscoveryManager(
    private val context: Context? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val TAG = "VS_DiscoveryManager"

    private val _discoveryState = MutableStateFlow(DiscoveryState.IDLE)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    private val _discoveredDevice = MutableStateFlow<VisionSolanoDevice?>(null)
    val discoveredDevice: StateFlow<VisionSolanoDevice?> = _discoveredDevice.asStateFlow()

    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var timeoutJob: Job? = null

    init {
        try {
            nsdManager = context?.getSystemService(Context.NSD_SERVICE) as? NsdManager
        } catch (e: Exception) {
            Log.w(TAG, "NsdManager no disponible en este contexto: ${e.message}")
        }
    }

    /**
     * Inicia la búsqueda activa de la ESP32 en la LAN mediante mDNS.
     */
    fun startDiscovery(timeoutMs: Long = NetworkConfig.DISCOVERY_TIMEOUT_MS) {
        stopDiscovery()

        _discoveryState.value = DiscoveryState.SEARCHING
        _discoveredDevice.value = null

        // Programar temporizador de tiempo agotado (Timeout)
        timeoutJob = scope.launch {
            delay(timeoutMs)
            if (_discoveryState.value == DiscoveryState.SEARCHING ||
                _discoveryState.value == DiscoveryState.DISCOVERING
            ) {
                _discoveryState.value = DiscoveryState.TIMEOUT
                stopDiscovery()
            }
        }

        val manager = nsdManager
        if (manager == null) {
            Log.i(TAG, "Contexto de NSD no disponible, operando en modo simulación/espera.")
            return
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String?) {
                Log.d(TAG, "Búsqueda NSD iniciada para: $regType")
                _discoveryState.value = DiscoveryState.SEARCHING
            }

            override fun onServiceFound(service: NsdServiceInfo?) {
                Log.d(TAG, "Servicio detectado: ${service?.serviceName}")
                if (service?.serviceType?.contains(NetworkConfig.SERVICE_TYPE_SEARCH) == true) {
                    _discoveryState.value = DiscoveryState.DISCOVERING
                    resolveService(service)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo?) {
                Log.d(TAG, "Servicio perdido: ${service?.serviceName}")
                if (_discoveredDevice.value?.hostName == service?.serviceName) {
                    _discoveredDevice.value = null
                    _discoveryState.value = DiscoveryState.NOT_FOUND
                }
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(TAG, "Búsqueda NSD detenida.")
            }

            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Fallo al iniciar descubrimiento: código $errorCode")
                _discoveryState.value = DiscoveryState.NOT_FOUND
                stopDiscovery()
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Fallo al detener descubrimiento: código $errorCode")
            }
        }

        try {
            manager.discoverServices(
                NetworkConfig.SERVICE_TYPE_SEARCH,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error invocando discoverServices", e)
            _discoveryState.value = DiscoveryState.NOT_FOUND
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val manager = nsdManager ?: return

        manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.w(TAG, "Fallo resolviendo servicio: código $errorCode")
            }

            override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                if (resolvedInfo == null) return
                scope.launch {
                    processResolvedService(resolvedInfo)
                }
            }
        })
    }

    /**
     * Procesa la información resuelta del servicio e interpreta sus atributos TXT.
     */
    fun processResolvedService(serviceInfo: NsdServiceInfo) {
        val host: InetAddress? = serviceInfo.host
        val ipAddress = host?.hostAddress.orEmpty()
        val port = serviceInfo.port

        // Parsear registros TXT desde atributos
        val attributes = serviceInfo.attributes
        val txtMap = mutableMapOf<String, String>()
        for ((key, valueBytes) in attributes) {
            if (valueBytes != null) {
                txtMap[key] = String(valueBytes, StandardCharsets.UTF_8)
            }
        }

        // Obtener el hostname real resuelto según contrato mDNS, sin inventarlo de serviceName
        val hostName = extractHostName(
            host = host,
            serviceName = serviceInfo.serviceName,
            txtMap = txtMap
        )

        val parseResult = parseTxtRecords(txtMap, hostName, ipAddress, port)
        if (parseResult.isSuccess) {
            val device = parseResult.getOrThrow()
            _discoveredDevice.value = device
            _discoveryState.value = DiscoveryState.FOUND
            timeoutJob?.cancel()
        } else {
            val exception = parseResult.exceptionOrNull()
            Log.w(TAG, "Dispositivo no compatible: ${exception?.message}")
            _discoveryState.value = DiscoveryState.INCOMPATIBLE
        }
    }

    /**
     * Detiene el proceso de búsqueda NSD.
     */
    fun stopDiscovery() {
        timeoutJob?.cancel()
        val listener = discoveryListener
        if (listener != null) {
            try {
                nsdManager?.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.w(TAG, "Error deteniendo discovery: ${e.message}")
            }
            discoveryListener = null
        }
    }

    /**
     * Activa el modo de prueba/simulación para entornos sin hardware real conectado.
     */
    fun simulateMockDiscovery(
        device: VisionSolanoDevice = VisionSolanoDevice.createMockDevice(),
        state: DiscoveryState = DiscoveryState.FOUND
    ) {
        stopDiscovery()
        _discoveredDevice.value = device
        _discoveryState.value = state
    }

    fun setDiscoveryState(state: DiscoveryState) {
        _discoveryState.value = state
    }

    companion object {
        /**
         * Resuelve y extrae el hostname mDNS real a partir de la resolución NSD y atributos del servicio.
         *
         * Reglas del contrato:
         * 1. No inventa un hostname a partir de un serviceName arbitrario (Instance Name de DNS-SD).
         * 2. Si el host resuelto (InetAddress) contiene un hostname mDNS válido (.local), se utiliza directamente.
         * 3. Si el host resuelto contiene un nombre de host sin dominio (ej. "visionsolano-esp32"),
         *    se cualifica dentro del dominio mDNS (".local").
         * 4. Si el registro TXT especifica explícitamente "hostname" o "host", se utiliza.
         * 5. Si el serviceName ya termina explícitamente en ".local" (ej. "visionsolano-esp32.local"), se respeta.
         * 6. De lo contrario, se adhiere al contrato mDNS esperado: NetworkConfig.EXPECTED_HOSTNAME ("visionsolano-esp32.local").
         */
        fun extractHostName(
            host: InetAddress?,
            serviceName: String? = null,
            txtMap: Map<String, String> = emptyMap()
        ): String {
            val ipAddress = host?.hostAddress.orEmpty()
            val rawHostName = host?.hostName?.trim().orEmpty()
            val canonicalHostName = try { host?.canonicalHostName?.trim().orEmpty() } catch (_: Exception) { "" }

            // 1. Hostname de InetAddress si es un dominio mDNS .local y no es la IP numérica
            if (rawHostName.isNotBlank() && rawHostName != ipAddress && rawHostName.endsWith(".local", ignoreCase = true)) {
                return rawHostName.lowercase()
            }

            // 2. Canonical hostname si termina en .local y no es la IP numérica
            if (canonicalHostName.isNotBlank() && canonicalHostName != ipAddress && canonicalHostName.endsWith(".local", ignoreCase = true)) {
                return canonicalHostName.lowercase()
            }

            // 3. Hostname de InetAddress si es una etiqueta válida sin puntos (ej: "visionsolano-esp32")
            if (rawHostName.isNotBlank() && rawHostName != ipAddress && !rawHostName.contains(".")) {
                return "$rawHostName.local".lowercase()
            }

            // 4. Atributo TXT explícito anunciado por el dispositivo (ej: hostname=visionsolano-esp32.local)
            val txtHostname = txtMap[NetworkConfig.KEY_HOSTNAME]?.trim()
                ?: txtMap["hostname"]?.trim()
                ?: txtMap["host"]?.trim()
            if (!txtHostname.isNullOrBlank()) {
                return if (txtHostname.endsWith(".local", ignoreCase = true)) {
                    txtHostname.lowercase()
                } else {
                    "$txtHostname.local".lowercase()
                }
            }

            // 5. Si el serviceName contiene explícitamente la extensión .local
            val cleanServiceName = serviceName?.trim().orEmpty()
            if (cleanServiceName.endsWith(".local", ignoreCase = true)) {
                return cleanServiceName.lowercase()
            }

            // 6. Contrato base definido para VisionSolano
            return NetworkConfig.EXPECTED_HOSTNAME
        }

        /**
         * Función pura para parsear y validar registros TXT de mDNS.
         * Desacoplada de Android para pruebas unitarias limpias.
         */
        fun parseTxtRecords(
            txtMap: Map<String, String>,
            hostName: String = NetworkConfig.EXPECTED_HOSTNAME,
            ipAddress: String = "",
            port: Int = NetworkConfig.HTTP_PORT
        ): Result<VisionSolanoDevice> {
            val role = txtMap[NetworkConfig.KEY_ROLE] ?: NetworkConfig.EXPECTED_ROLE
            val camera = txtMap[NetworkConfig.KEY_CAMERA] ?: NetworkConfig.EXPECTED_CAMERA
            val deviceId = txtMap[NetworkConfig.KEY_DEVICE_ID] ?: "1"

            val protocolVersionStr = txtMap[NetworkConfig.KEY_PROTOCOL_VERSION]
            val protocolVersion = protocolVersionStr?.toIntOrNull() ?: NetworkConfig.EXPECTED_PROTOCOL_VERSION

            val apiVersionStr = txtMap[NetworkConfig.KEY_API_VERSION]
            val apiVersion = apiVersionStr?.toIntOrNull() ?: NetworkConfig.EXPECTED_API_VERSION

            // Validar compatibilidad de protocolo
            if (protocolVersion != NetworkConfig.EXPECTED_PROTOCOL_VERSION) {
                return Result.failure(
                    IllegalArgumentException(
                        "Versión de protocolo incompatible: se esperaba ${NetworkConfig.EXPECTED_PROTOCOL_VERSION}, recibido $protocolVersion"
                    )
                )
            }

            return Result.success(
                VisionSolanoDevice(
                    deviceId = deviceId,
                    hostName = hostName,
                    ipAddress = ipAddress,
                    port = port,
                    role = role,
                    protocolVersion = protocolVersion,
                    apiVersion = apiVersion,
                    camera = camera,
                    discoveredAt = System.currentTimeMillis()
                )
            )
        }
    }
}

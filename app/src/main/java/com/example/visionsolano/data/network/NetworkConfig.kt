package com.example.visionsolano.data.network

/**
 * Centralización de rutas, puertos y especificaciones de red para VisionSolano.
 *
 * Evita la duplicación de URLs y endpoints hardcodeados en pantallas y repositorios.
 */
object NetworkConfig {
    // Protocolo y puertos estándar
    const val HTTP_PORT = 80
    const val STREAM_PATH = "/stream"
    const val STATUS_PATH = "/api/status"
    const val CONTROL_PATH = "/api/control"

    // Parámetros de descubrimiento mDNS / NSD (Network Service Discovery)
    const val SERVICE_TYPE = "_visionsolano._tcp."
    const val SERVICE_TYPE_SEARCH = "_visionsolano._tcp"
    const val EXPECTED_HOSTNAME = "visionsolano-esp32.local"

    // Claves de registros TXT mDNS esperados
    const val KEY_DEVICE_ID = "device_id"
    const val KEY_ROLE = "role"
    const val KEY_PROTOCOL_VERSION = "protocol_version"
    const val KEY_API_VERSION = "api_version"
    const val KEY_CAMERA = "camera"
    const val KEY_HOSTNAME = "hostname"

    // Valores esperados en los registros TXT
    const val EXPECTED_ROLE = "esp32"
    const val EXPECTED_PROTOCOL_VERSION = 1
    const val EXPECTED_API_VERSION = 1
    const val EXPECTED_CAMERA = "ov2640"

    // Tiempos para latidos y descubrimiento (en milisegundos)
    const val HEARTBEAT_INTERVAL_MS = 1000L
    const val HEARTBEAT_TIMEOUT_MS = 3000L
    const val DISCOVERY_TIMEOUT_MS = 8000L

    /**
     * Construye la URL para el stream de vídeo MJPEG.
     * Ejemplo: http://192.168.1.50:80/stream o http://visionsolano-esp32.local:80/stream
     */
    fun buildStreamUrl(hostOrIp: String, port: Int = HTTP_PORT): String {
        val cleanHost = cleanHost(hostOrIp)
        return "http://$cleanHost:$port$STREAM_PATH"
    }

    /**
     * Construye la URL para la consulta de estado y telemetría.
     * Ejemplo: http://192.168.1.50:80/api/status
     */
    fun buildStatusUrl(hostOrIp: String, port: Int = HTTP_PORT): String {
        val cleanHost = cleanHost(hostOrIp)
        return "http://$cleanHost:$port$STATUS_PATH"
    }

    /**
     * Construye la URL para el envío de comandos de control.
     * Ejemplo: http://192.168.1.50:80/api/control
     */
    fun buildControlUrl(hostOrIp: String, port: Int = HTTP_PORT): String {
        val cleanHost = cleanHost(hostOrIp)
        return "http://$cleanHost:$port$CONTROL_PATH"
    }

    private fun cleanHost(host: String): String {
        return host.removePrefix("http://")
            .removePrefix("https://")
            .substringBefore(":")
            .substringBefore("/")
            .trim()
    }
}

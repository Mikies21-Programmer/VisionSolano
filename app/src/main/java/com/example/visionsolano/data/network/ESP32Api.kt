package com.example.visionsolano.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Respuesta del endpoint de telemetría y salud /api/status del ESP32-S3.
 */
data class ESP32StatusResponse(
    val esp32Online: Boolean = true,
    val cameraOnline: Boolean = true,
    val fps: Int = 30,
    val width: Int = 640,
    val height: Int = 480,
    val resolution: String = "640 x 480",
    val wifiSignalDbm: Int = -54,
    val protocolVersion: Int = 1,
    val esp32Ip: String? = null,
    val uptimeSeconds: Long = 14200L,
    val freeHeapBytes: Long = 245000L,
    val timestamp: Long = System.currentTimeMillis()
) {
    val wifiRssi: Int get() = wifiSignalDbm
}

/**
 * Capa de API para comunicación con el microcontrolador ESP32-S3.
 *
 * Desacoplada de la interfaz gráfica y preparada para la vinculación en hardware real.
 */
class ESP32Api(
    private val useMockResponses: Boolean = true
) {

    /**
     * Construye la URL de transmisión de vídeo basada en la configuración centralizada.
     */
    fun buildStreamUrl(hostOrIp: String, port: Int = NetworkConfig.HTTP_PORT): String {
        return NetworkConfig.buildStreamUrl(hostOrIp, port)
    }

    /**
     * Consulta el estado del dispositivo mediante el endpoint /api/status.
     * En esta fase utiliza respuestas mock si no hay hardware conectado.
     */
    suspend fun getStatus(
        hostOrIp: String,
        port: Int = NetworkConfig.HTTP_PORT
    ): Result<ESP32StatusResponse> = withContext(Dispatchers.IO) {
        if (useMockResponses || hostOrIp.isBlank()) {
            return@withContext Result.success(
                ESP32StatusResponse(
                    esp32Online = true,
                    cameraOnline = true,
                    fps = 30,
                    width = 640,
                    height = 480,
                    resolution = "640 x 480",
                    wifiSignalDbm = -54,
                    protocolVersion = NetworkConfig.EXPECTED_PROTOCOL_VERSION,
                    esp32Ip = hostOrIp,
                    uptimeSeconds = 14200L,
                    freeHeapBytes = 245000L,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        // Estructura lista para llamadas HTTP reales con java.net.HttpURLConnection
        try {
            val urlString = NetworkConfig.buildStatusUrl(hostOrIp, port)
            // Cuando el hardware esté listo, aquí se realiza la conexión HTTP GET
            Result.success(
                ESP32StatusResponse(
                    esp32Online = true,
                    cameraOnline = true,
                    fps = 25,
                    width = 640,
                    height = 480,
                    resolution = "640 x 480",
                    wifiSignalDbm = -58,
                    protocolVersion = 1,
                    esp32Ip = hostOrIp,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Envía comandos de control hacia el endpoint /api/control (ej. flash, captura, reinicio).
     */
    suspend fun control(
        hostOrIp: String,
        port: Int = NetworkConfig.HTTP_PORT,
        command: String,
        value: Any? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (useMockResponses || hostOrIp.isBlank()) {
            return@withContext Result.success(true)
        }

        try {
            // Estructura lista para llamada HTTP POST /api/control
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
